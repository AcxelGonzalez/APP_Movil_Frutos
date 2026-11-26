package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.oyl_fs.data.local.*
import com.example.oyl_fs.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/** Prueba reglas y coordinación con DAOs simulados; no demuestra atomicidad de SQLite. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RepositoriosRoomUnitTest {
    private val db = mockk<OylDatabase>(relaxed = true)
    private val productos = mockk<ProductoDao>(relaxed = true)
    private val pedidos = mockk<PedidoDao>(relaxed = true)
    private val carrito = mockk<CarritoDao>(relaxed = true)
    private val usuarios = mockk<UsuarioDao>(relaxed = true)
    private val contexto = mockk<Context>(relaxed = true)
    private val producto = ProductoInfo(4,"Almendras","ALM","Frutos secos","Descripción","500 g",1000,10,2,true)
    private val item = ItemCarrito("4","Almendras","500 g",1000,2,10)
    private val pedido = Pedido("PED-QA","7",1,listOf(DetallePedido("4","Almendras",2,1000)),2000,EstadoPedido.PENDIENTE_VALIDACION)
    @Before fun preparar() {
        every { contexto.applicationContext } returns contexto
        every { db.productoDao() } returns productos
        every { db.pedidoDao() } returns pedidos
        every { db.carritoDao() } returns carrito
        every { db.usuarioDao() } returns usuarios
        every { productos.observarProductos() } returns flowOf(listOf(producto.aEntidad()))
        every { pedidos.observarPedidos() } returns flowOf(emptyList())
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
    }
    @After fun limpiar() { unmockkAll() }
    private suspend fun rechaza(bloque: suspend () -> Unit) {
        try { bloque(); fail("Debía rechazar la operación") } catch (_: IllegalArgumentException) { }
    }

    @Test fun catalogoResuelveImagenYGeneraElIdDesdeElDao() = runTest {
        every { contexto.resources.getIdentifier("almendra","drawable",any()) } returns 55
        val entidad = producto.aEntidad("almendra")
        every { productos.observarProductos() } returns flowOf(listOf(entidad))
        coEvery { productos.listarProductos() } returns listOf(entidad)
        coEvery { productos.obtenerPorId(4) } returns entidad
        coEvery { productos.insertar(any()) } returns 4
        coEvery { productos.contar() } returns 1
        val repo = ProductoRoomRepository(contexto,productos)
        assertEquals(55,repo.productos.first().single().imagen)
        assertEquals(55,repo.cargarProductos().single().imagen)
        assertEquals(4,repo.insertarYObtener(producto).id)
        coVerify { productos.insertar(match { it.id == 0 }) }
        assertEquals(1,repo.contar())
    }
    @Test fun actualizarConservaImagenAnteriorYRespetaResultadoDelDao() = runTest {
        coEvery { productos.obtenerPorId(4) } returns producto.aEntidad("anterior")
        coEvery { productos.actualizar(any()) } returnsMany listOf(1,0)
        val repo = ProductoRoomRepository(contexto,productos)
        assertTrue(repo.actualizar(producto))
        coVerify { productos.actualizar(match { it.imagenDrawableNombre == "anterior" }) }
        assertFalse(repo.actualizar(producto))
        coEvery { productos.obtenerPorId(4) } returns null
        assertFalse(repo.actualizar(producto)); assertFalse(repo.eliminar(4))
        assertNull(repo.obtenerPorId(4))
    }
    @Test fun insertarValidaLimitesYNoOcultaFalloDeLectura() = runTest {
        val repo = ProductoRoomRepository(contexto,productos)
        rechaza { repo.insertar(producto.copy(presentacion="x".repeat(256))) }
        rechaza { repo.actualizar(producto.copy(presentacion="x".repeat(256))) }
        coEvery { productos.insertar(any()) } returns 99
        coEvery { productos.obtenerPorId(99) } returns null
        try { repo.insertarYObtener(producto); fail() } catch (_: IllegalStateException) { }
        every { contexto.resources.getResourceEntryName(55) } returns "nueva"
        coEvery { productos.obtenerPorId(4) } returns producto.aEntidad()
        coEvery { productos.eliminar(any()) } returns 1
        assertTrue(repo.eliminar(4))
        repo.insertar(producto.copy(imagen=55))
        coVerify { productos.insertar(match { it.imagenDrawableNombre == "nueva" }) }
    }
    @Test fun descuentoAgrupaUnidadesYRechazaDatosAntesDeEscribir() = runTest {
        val repo = ProductoRoomRepository(contexto,productos)
        assertTrue(repo.descontarStockPorCompra(listOf(item,item.copy(cantidad=1))))
        coVerify(exactly=1) { productos.descontarStockLote(listOf(DescuentoStock(4,3,1000))) }
        for (malos in listOf(emptyList(),listOf(item.copy(productoId="x")),listOf(item.copy(productoId="0")),
            listOf(item.copy(cantidad=0)),listOf(item.copy(precio=0)),listOf(item.copy(precio=Int.MAX_VALUE)),
            listOf(item,item.copy(precio=5)))) assertFalse(repo.descontarStockPorCompra(malos))
        coEvery { productos.descontarStockLote(any()) } throws StockNoDisponibleException()
        assertFalse(repo.descontarStockPorCompra(listOf(item)))
        coEvery { productos.descontarStockLote(any()) } throws IOException()
        try { repo.descontarStockPorCompra(listOf(item)); fail() } catch (_: IOException) { }
    }
    @Test fun guardarPedidoCompruebaDetalleTotalYLuegoCoordinaEscrituras() = runTest {
        val repo = PedidoRoomRepository(db)
        repo.guardarPedido(pedido)
        coVerifyOrder { pedidos.insertarPedido(pedido.aEntidad()); pedidos.insertarDetalles(pedido.aEntidadesDetalle()) }
        for (malo in listOf(pedido.copy(id=""),pedido.copy(usuarioId=""),pedido.copy(productos=emptyList()),
            pedido.copy(total=1),pedido.copy(productos=listOf(pedido.productos.single().copy(cantidad=0))),
            pedido.copy(productos=listOf(pedido.productos.single().copy(precioUnitario=Int.MAX_VALUE)))))
            rechaza { repo.guardarPedido(malo) }
    }
    @Test fun compraAgrupaStockYDescuentaSoloCarritoDelPropietario() = runTest {
        val repo = PedidoRoomRepository(db)
        val doble = pedido.copy(productos=pedido.productos+pedido.productos,total=4000)
        assertTrue(repo.registrarCompraConStock(doble))
        coVerify { productos.descontarStockLote(listOf(DescuentoStock(4,4,1000))) }
        coVerify(exactly=2) { carrito.descontarComprados("7",4,2) }
        coVerify { carrito.quitarVacios("7") }
        coEvery { productos.descontarStockLote(any()) } throws StockNoDisponibleException()
        assertFalse(repo.registrarCompraConStock(pedido))
    }
    @Test fun compraRechazaIdentificadoresPreciosYTotalesInconsistentes() = runTest {
        val repo = PedidoRoomRepository(db)
        for (malo in listOf(pedido.copy(id=""),pedido.copy(usuarioId=""),pedido.copy(productos=emptyList()),
            pedido.copy(total=3),pedido.copy(productos=listOf(pedido.productos.single().copy(productoId="x"))),
            pedido.copy(productos=listOf(pedido.productos.single().copy(productoId="-1"))),
            pedido.copy(productos=listOf(pedido.productos.single().copy(precioUnitario=Int.MAX_VALUE))),
            pedido.copy(productos=pedido.productos+pedido.productos.single().copy(precioUnitario=1),total=2002)))
            rechaza { repo.registrarCompraConStock(malo) }
        coVerify(exactly=0) { pedidos.insertarPedido(any()) }
    }
    @Test fun compraDesdeCarritoValidaEntregaYDevuelvePedidoConTotalSeguro() = runTest {
        val repo = PedidoRoomRepository(db)
        assertNull(repo.registrarCompraDesdeCarrito("",listOf(item)))
        assertNull(repo.registrarCompraDesdeCarrito("7",emptyList()))
        for (malo in listOf(item.copy(productoId="x"),item.copy(productoId="0"),item.copy(nombre=""),
            item.copy(precio=0),item.copy(cantidad=0),item.copy(precio=Int.MAX_VALUE)))
            assertNull(repo.registrarCompraDesdeCarrito("7",listOf(malo)))
        val entrega = EntregaPedido(ModalidadEntrega.DOMICILIO," Cliente ","912345678"," Calle 1 ")
        assertNull(repo.registrarCompraDesdeCarrito("7",listOf(item),entrega.copy(direccion="")))
        val creado = requireNotNull(repo.registrarCompraDesdeCarrito(" 7 ",listOf(item),entrega))
        assertEquals("7",creado.usuarioId); assertEquals(2000,creado.total)
        assertEquals("Calle 1",creado.entrega?.direccion)
        coEvery { productos.descontarStockLote(any()) } throws StockNoDisponibleException()
        assertNull(repo.registrarCompraDesdeCarrito("7",listOf(item)))
    }
    @Test fun estadosExigenElAnteriorCorrespondienteYSeMapeanLasLecturas() = runTest {
        val repo = PedidoRoomRepository(db)
        every { pedidos.observarPedidosUsuario("7") } returns flowOf(listOf(PedidoConDetalles(pedido.aEntidad(),pedido.aEntidadesDetalle())))
        coEvery { pedidos.obtenerPedidoPorId(pedido.id) } returns PedidoConDetalles(pedido.aEntidad(),pedido.aEntidadesDetalle())
        coEvery { pedidos.contarPedidos() } returns 1
        assertEquals(pedido,repo.observarPedidosUsuario("7").first().single())
        assertEquals(pedido,repo.obtenerPedidoPorId(pedido.id)); assertEquals(1,repo.contarPedidos())
        assertTrue(repo.pedidos.first().isEmpty())
        assertFalse(repo.actualizarEstadoPedido("",EstadoPedido.CONFIRMADO))
        assertFalse(repo.actualizarEstadoPedido(pedido.id,EstadoPedido.PENDIENTE_VALIDACION))
        coEvery { pedidos.actualizarEstado(any(),any(),any()) } returns 1
        for ((antes,despues) in listOf("PENDIENTE_VALIDACION" to EstadoPedido.CONFIRMADO,"CONFIRMADO" to EstadoPedido.PREPARADO,"PREPARADO" to EstadoPedido.ENTREGADO)) {
            assertTrue(repo.actualizarEstadoPedido(pedido.id,despues))
            coVerify { pedidos.actualizarEstado(pedido.id,antes,despues.name) }
        }
    }
    @Test fun carritoObservaStockActualSinCambiarElPrecioAceptado() = runTest {
        val linea = CarritoEntity("7",4,"Almendras","500 g",900,2,null,null)
        every { carrito.observar("7") } returns flowOf(listOf(linea))
        val repo=CarritoRoomRepository(contexto,db)
        val actual=repo.observar("7").first().single()
        assertEquals(900,actual.precio);assertEquals(10,actual.stockDisponible)
        every { productos.observarProductos() } returns flowOf(emptyList())
        assertEquals(0,repo.observar("7").first().single().stockDisponible)
    }
    @Test fun carritoAgregarAjustarYQuitarRespetaStockYCantidad() = runTest {
        val repo=CarritoRoomRepository(contexto,db)
        coEvery { productos.obtenerPorId(4) } returns producto.aEntidad()
        coEvery { carrito.obtener("7",4) } returns null
        assertFalse(repo.agregar("",producto,1));assertFalse(repo.agregar("7",producto,0))
        assertTrue(repo.agregar("7",producto,2))
        coVerify { carrito.guardar(match { it.usuarioId=="7" && it.cantidad==2 && it.precio==1000 }) }
        coEvery { carrito.obtener("7",4) } returns CarritoEntity("7",4,"A","500 g",900,10,null,null)
        assertFalse(repo.agregar("7",producto,1));assertFalse(repo.ajustar("7",4,1))
        assertTrue(repo.ajustar("7",4,-1))
        assertFalse(repo.ajustar("",4,1)); assertFalse(repo.ajustar("7",4,2))
        coEvery { carrito.obtener("7",4) } returns CarritoEntity("7",4,"A","500 g",900,1,null,null)
        assertTrue(repo.ajustar("7",4,-1));coVerify { carrito.quitar("7",4) }
        repo.quitar("",4);repo.quitar("7",4)
        coEvery { productos.obtenerPorId(4) } returns producto.copy(disponible=false).aEntidad()
        assertFalse(repo.agregar("7",producto,1));assertFalse(repo.ajustar("7",4,1))
    }
}
