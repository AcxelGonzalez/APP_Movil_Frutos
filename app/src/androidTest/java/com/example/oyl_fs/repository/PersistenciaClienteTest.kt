package com.example.oyl_fs.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.ProductoEntity
import com.example.oyl_fs.data.local.aProducto
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.viewmodel.PerfilViewModel
import com.example.oyl_fs.viewmodel.SesionIdentificada
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PersistenciaClienteTest {
    private val contexto = ApplicationProvider.getApplicationContext<Application>()
    private val nombre = "persistencia-cliente-${UUID.randomUUID()}.db"
    private lateinit var base: OylDatabase
    private val preferencias by lazy { contexto.getSharedPreferences(nombre, Context.MODE_PRIVATE) }

    @Before fun abrir() { base = nuevaBase() }

    private fun nuevaBase() = Room.databaseBuilder(contexto, OylDatabase::class.java, nombre).build()

    @After fun limpiar() {
        base.close()
        contexto.deleteDatabase(nombre)
        preferencias.edit().clear().commit()
    }

    private suspend fun producto(stock: Int = 10): ProductoEntity {
        val producto = ProductoEntity(1, "Almendras", "TEST-1", "Frutos secos", "Prueba", "500 g",
            1000, stock, 1, true, "almendras", null)
        base.productoDao().insertar(producto)
        return producto
    }

    private suspend fun registrar(alias: String, rut: String = ""): String {
        val resultado = UsuarioRoomRepository(base).registrarCliente(
            DatosRegistroCliente(alias, "Cliente de prueba", "$alias@example.test", "Prueba123!", rut = rut)
        )
        return (resultado as ResultadoRegistro.Exito).usuarioId
    }

    @Test fun carritosSobrevivenAlReabrirSinMezclarClientes() = runBlocking {
        val producto = producto()
        var repo = CarritoRoomRepository(contexto, base)
        assertTrue(repo.agregar("cliente-a", producto.aProducto(0), 2))
        assertTrue(repo.agregar("cliente-b", producto.aProducto(0), 3))
        assertFalse(repo.agregar("cliente-a", producto.aProducto(0), -1))
        base.close()
        base = nuevaBase()
        repo = CarritoRoomRepository(contexto, base)
        assertEquals(2, repo.observar("cliente-a").first().single().cantidad)
        assertEquals(3, repo.observar("cliente-b").first().single().cantidad)
        assertTrue(repo.observar("otro").first().isEmpty())

        // Un producto desactivado sigue visible en el carrito, pero sin stock comprable.
        base.productoDao().actualizar(producto.copy(disponible = false))
        assertEquals(0, repo.observar("cliente-a").first().single().stockDisponible)
        assertFalse(repo.ajustar("cliente-a", 1, 1))
        assertTrue(repo.ajustar("cliente-a", 1, -1))
        assertEquals(1, repo.observar("cliente-a").first().single().cantidad)
    }

    @Test fun dosAgregadosSimultaneosNoSuperanElLimite() = runBlocking {
        val producto = producto(stock = 4).aProducto(0)
        val repo = CarritoRoomRepository(contexto, base)
        val resultados = listOf(
            async(Dispatchers.IO) { repo.agregar("cliente-a", producto, 3) },
            async(Dispatchers.IO) { repo.agregar("cliente-a", producto, 3) }
        ).awaitAll()
        assertEquals(1, resultados.count { it })
        assertEquals(3, repo.observar("cliente-a").first().single().cantidad)
    }

    @Test fun compraRetiraSoloArticulosCompradosDelCarritoCorrectoYRevierteSiFalla() = runBlocking {
        val producto = producto()
        val carrito = CarritoRoomRepository(contexto, base)
        carrito.agregar("cliente-a", producto.aProducto(0), 4)
        carrito.agregar("cliente-b", producto.aProducto(0), 2)
        val pedidos = PedidoRoomRepository(base)
        val items = listOf(ItemCarrito("1", "Almendras", "500 g", 1000, 3, 10))

        base.productoDao().actualizar(producto.copy(stock = 1))
        assertNull(pedidos.registrarCompraDesdeCarrito("cliente-a", items))
        assertEquals(0, base.pedidoDao().contarPedidos())
        assertEquals(4, carrito.observar("cliente-a").first().single().cantidad)

        base.productoDao().actualizar(producto)
        assertNotNull(pedidos.registrarCompraDesdeCarrito("cliente-a", items))
        assertEquals(7, base.productoDao().obtenerPorId(1)?.stock)
        assertEquals(1, carrito.observar("cliente-a").first().single().cantidad)
        assertEquals(2, carrito.observar("cliente-b").first().single().cantidad)
        base.close()
        base = nuevaBase()
        assertEquals(1, base.pedidoDao().contarPedidos())
        assertEquals(1, CarritoRoomRepository(contexto, base).observar("cliente-a").first().single().cantidad)
    }

    @Test fun perfilActualizaLaCuentaSinCambiarCredencialesYRechazaDuplicados() = runBlocking {
        val idA = registrar("cliente-a", "12.345.678-5")
        registrar("cliente-b", "11.111.111-1")
        var repo = UsuarioRoomRepository(base)
        val anterior = repo.obtenerUsuario(idA)!!
        val perfil = PerfilUsuario(idA, false, "Nombre editado", rut = "12.345.678-5", correo = " NUEVO@example.test ")
        assertNull(repo.actualizarPerfil(perfil))
        assertNotNull(repo.actualizarPerfil(perfil.copy(correo = "CLIENTE-B@example.test"))?.correo)
        assertNotNull(repo.actualizarPerfil(perfil.copy(rut = "11111111-1"))?.rut)
        assertNotNull(repo.actualizarPerfil(perfil.copy(rut = "12345678-9"))?.rut)
        base.close()
        base = nuevaBase()
        repo = UsuarioRoomRepository(base)
        val guardado = repo.obtenerUsuario(idA)!!
        assertEquals("Nombre editado", guardado.nombre)
        assertEquals("nuevo@example.test", guardado.correoNormalizado)
        assertEquals(anterior.claveHash, guardado.claveHash)
        assertEquals(anterior.claveSalt, guardado.claveSalt)
        assertTrue(guardado.perfilPreferenciasMigrado)
        assertEquals("Nombre editado", repo.autenticarCliente("cliente-a", "Prueba123!")?.nombre)
    }

    @Test fun sesionSeRestauraValidaSuIdentidadYElCierrePersiste() = runBlocking {
        val id = registrar("cliente-a")
        var repo = SesionRepository(preferencias, UsuarioRoomRepository(base))
        repo.guardar(SesionIdentificada(id, "Nombre anterior", false, false))
        base.close()
        base = nuevaBase()
        repo = SesionRepository(preferencias, UsuarioRoomRepository(base))
        val restaurada = repo.restaurar()!!
        assertEquals(id, restaurada.usuarioId)
        assertEquals("Cliente de prueba", restaurada.nombre)
        assertFalse(restaurada.esAdmin)
        repo.cerrar()
        assertNull(SesionRepository(preferencias, UsuarioRoomRepository(base)).restaurar())

        // El rol guardado no se usa para conceder permisos al restaurar.
        repo.guardar(SesionIdentificada("cliente-demo", "Otro", true, true))
        assertFalse(repo.restaurar()!!.esAdmin)
        repo.guardar(SesionIdentificada("admin-demo", "Otro", false, true))
        assertTrue(repo.restaurar()!!.esAdmin)
        repo.guardar(SesionIdentificada("usuario-inexistente", "Otro", true, false))
        assertNull(repo.restaurar())
        assertFalse(preferencias.contains("usuarioId"))
    }

    @Test fun preferenciasAntiguasSeIncorporanSoloUnaVezAlPerfilRoom() = runBlocking {
        val id = registrar("cliente-a")
        val repo = UsuarioRoomRepository(base)
        preferencias.edit().putString("$id.nombre", "Perfil anterior")
            .putString("$id.correo", "anterior@example.test").commit()
        val vm = PerfilViewModel(contexto, repo, preferencias)
        vm.activarClienteRegistrado(repo.obtenerUsuario(id)!!)
        assertEquals("Perfil anterior", repo.obtenerUsuario(id)?.nombre)
        assertTrue(vm.guardarPerfil(vm.perfiles.value.first { it.id == id }.copy(nombre = "Perfil nuevo")))
        // Simular otra instancia conservando las preferencias de la versión antigua.
        val siguiente = PerfilViewModel(contexto, repo, preferencias)
        siguiente.activarClienteRegistrado(repo.obtenerUsuario(id)!!)
        assertEquals("Perfil nuevo", siguiente.perfiles.value.first { it.id == id }.nombre)
        assertEquals("Perfil nuevo", repo.obtenerUsuario(id)?.nombre)
    }
}
