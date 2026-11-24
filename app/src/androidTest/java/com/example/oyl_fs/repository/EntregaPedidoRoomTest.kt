package com.example.oyl_fs.repository

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import com.example.oyl_fs.viewmodel.ConfirmacionPedidoViewModel
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EntregaPedidoRoomTest {
    private val contexto = ApplicationProvider.getApplicationContext<Application>()
    private val nombre = "entrega-prueba-${UUID.randomUUID()}.db"
    private lateinit var base: OylDatabase
    private val retiro = EntregaPedido(ModalidadEntrega.RETIRO, "Cliente ficticio", "+56 9 1234 5678", puntoRetiroId = "T01")
    private val items = listOf(ItemCarrito("1", "Almendras", "500 g", 1000, 2, 10))

    private fun abrirBase() = Room.databaseBuilder(contexto, OylDatabase::class.java, nombre).addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build()
    @Before fun preparar() = runBlocking {
        base = abrirBase()
        base.productoDao().insertar(ProductoEntity(1, "Almendras", "TEST-1", "Frutos secos", "Prueba", "500 g",
            1000, 10, 1, true, "almendras", null))
        base.carritoDao().guardar(CarritoEntity("cliente-a", 1, "Almendras", "500 g", 1000, 2, "almendras", null))
    }
    @After fun limpiar() { base.close(); contexto.deleteDatabase(nombre) }

    @Test fun ambasModalidadesPersistenYNoDependenDelPerfilPosterior() = runBlocking {
        var repo = PedidoRoomRepository(base)
        val primero = repo.registrarCompraDesdeCarrito("cliente-a", items, retiro.copy(puntoRetiroNombre = "Manipulado"))!!
        val domicilio = retiro.copy(modalidad = ModalidadEntrega.DOMICILIO, direccion = "Calle ficticia 200", puntoRetiroNombre = "Tienda anterior")
        val segundo = repo.registrarCompraDesdeCarrito("cliente-a", items, domicilio)!!
        val usuarios = UsuarioRoomRepository(base)
        val id = (usuarios.registrarCliente(DatosRegistroCliente("cliente-prueba", "Nombre original", "prueba@example.test", "Prueba123!")) as ResultadoRegistro.Exito).usuarioId
        val tercero = repo.registrarCompraDesdeCarrito(id, items, retiro)!!
        assertNull(usuarios.actualizarPerfil(PerfilUsuario(id, false, "Otro nombre", correo = "otro@example.test", telefono = "987654321")))
        base.close()
        base = abrirBase()
        repo = PedidoRoomRepository(base)
        assertEquals("OYL Frutos Secos - Tienda principal", repo.obtenerPedidoPorId(primero.id)?.entrega?.puntoRetiroNombre)
        assertEquals(ModalidadEntrega.DOMICILIO, repo.obtenerPedidoPorId(segundo.id)?.entrega?.modalidad)
        assertEquals("", repo.obtenerPedidoPorId(segundo.id)?.entrega?.puntoRetiroId)
        assertEquals("Calle ficticia 200", repo.obtenerPedidoPorId(segundo.id)?.entrega?.direccion)
        assertEquals("Cliente ficticio", repo.obtenerPedidoPorId(tercero.id)?.entrega?.nombreContacto)
        assertEquals(4, base.productoDao().obtenerPorId(1)?.stock)
    }

    @Test fun datosInvalidosNoCreanPedidoNiCambianStockOCarrito() = runBlocking {
        val repo = PedidoRoomRepository(base)
        listOf(retiro.copy(nombreContacto = ""), retiro.copy(telefonoContacto = "abc12345678"),
            retiro.copy(puntoRetiroId = "desconocido"), retiro.copy(modalidad = ModalidadEntrega.DOMICILIO, direccion = " ")).forEach {
            assertNull(repo.registrarCompraDesdeCarrito("cliente-a", items, it))
        }
        assertEquals(0, base.pedidoDao().contarPedidos())
        assertEquals(10, base.productoDao().obtenerPorId(1)?.stock)
        assertEquals(2, base.carritoDao().obtener("cliente-a", 1)?.cantidad)
    }

    @Test fun faltaDeStockReviertePedidoEntregaYCarritoJuntos() = runBlocking {
        base.productoDao().actualizar(base.productoDao().obtenerPorId(1)!!.copy(stock = 1))
        assertNull(PedidoRoomRepository(base).registrarCompraDesdeCarrito("cliente-a", items, retiro))
        assertEquals(0, base.pedidoDao().contarPedidos())
        assertEquals(1, base.productoDao().obtenerPorId(1)?.stock)
        assertEquals(2, base.carritoDao().obtener("cliente-a", 1)?.cantidad)
    }

    @Test fun confirmacionBloqueaPulsacionesDuplicadasYConservaComprobante() = runBlocking {
        val vm = ConfirmacionPedidoViewModel(contexto, PedidoRoomRepository(base))
        vm.iniciar("cliente-a")
        vm.confirmar("cliente-a", items, retiro)
        vm.confirmar("cliente-a", items, retiro)
        val resultado = withTimeout(10_000) { vm.estado.first { it.pedido != null || it.error != null } }
        assertNull(resultado.error)
        assertNotNull(resultado.pedido)
        vm.confirmar("cliente-a", items, retiro)
        assertEquals(1, base.pedidoDao().contarPedidos())
        assertEquals(8, base.productoDao().obtenerPorId(1)?.stock)
        assertNull(base.carritoDao().obtener("cliente-a", 1))
        assertEquals(resultado.pedido?.id, vm.estado.value.pedido?.id)
    }

    @Test fun migracionV4aV5ConservaPedidoAnteriorYCarrito() = runBlocking {
        val pedido = PedidoRoomRepository(base).registrarCompraDesdeCarrito("cliente-antiguo", items)!!
        base.close()
        // Reconstruir únicamente la tabla anterior con sus cinco columnas reales.
        val antigua = SQLiteDatabase.openDatabase(contexto.getDatabasePath(nombre).path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            antigua.execSQL("PRAGMA foreign_keys=OFF")
            antigua.execSQL("CREATE TABLE pedidos_v4 (id TEXT NOT NULL PRIMARY KEY, usuarioId TEXT NOT NULL, fecha INTEGER NOT NULL, total INTEGER NOT NULL, estado TEXT NOT NULL)")
            antigua.execSQL("INSERT INTO pedidos_v4 SELECT id, usuarioId, fecha, total, estado FROM pedidos")
            antigua.execSQL("DROP TABLE pedidos")
            antigua.execSQL("ALTER TABLE pedidos_v4 RENAME TO pedidos")
            antigua.execSQL("CREATE INDEX index_pedidos_usuarioId ON pedidos(usuarioId)")
            antigua.execSQL("CREATE INDEX index_pedidos_fecha ON pedidos(fecha)")
            antigua.execSQL("DROP TABLE room_master_table")
            antigua.version = 4
        } finally { antigua.close() }
        base = abrirBase()
        val recuperado = PedidoRoomRepository(base).obtenerPedidoPorId(pedido.id)!!
        assertEquals(pedido.total, recuperado.total)
        assertEquals(pedido.productos, recuperado.productos)
        assertNull(recuperado.entrega)
        assertEquals(2, base.carritoDao().obtener("cliente-a", 1)?.cantidad)
        assertEquals(8, base.productoDao().obtenerPorId(1)?.stock)
    }
}
