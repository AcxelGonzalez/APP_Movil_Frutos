package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.PerfilRemotoDatabase
import com.example.oyl_fs.model.*
import com.example.oyl_fs.network.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.viewmodel.ProductoViewModel
import java.io.IOException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class IntegracionRemotaRoomTest {
    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var base: OylDatabase
    private lateinit var basePerfiles: PerfilRemotoDatabase
    private lateinit var preferenciasNombre: String
    private lateinit var extras: DatosLocalesRemotosRepository
    private lateinit var productosApi: Catalogo
    private lateinit var productos: ProductoRemoteRepository
    private lateinit var pedidosApi: Pedidos
    private lateinit var carrito: CarritoRoomRepository
    private lateinit var compras: CompraRemotaRepository
    private val entrega = EntregaPedido(ModalidadEntrega.DOMICILIO, "Cliente prueba", "912345678", "Calle ficticia 100")

    private class Catalogo : ProductosApi {
        var lista = listOf(ProductoDto(901, "Almendras", "PRUEBA-901", "Frutos secos", "Prueba", "500 g", 1000, 10, 2, true, null))
        var borrado = 204
        var error: Exception? = null
        override suspend fun listar(): List<ProductoDto> {
            error?.let { throw it }
            return lista
        }
        override suspend fun obtener(id: Long) = lista.first { it.id == id }
        override suspend fun crear(producto: ProductoDto) = producto.copy(id = 902)
        override suspend fun actualizar(id: Long, producto: ProductoDto) = producto.copy(id = id)
        override suspend fun eliminar(id: Long): Response<Unit> = if (borrado == 204) Response.success(Unit) else Response.error(borrado, "{}".toResponseBody())
    }
    private class Pedidos : PedidosApi {
        var envios = 0
        var error: Exception? = null
        var ultima: CrearPedidoRequest? = null
        val pedido = PedidoDto(55, 7, "2026-11-21T10:00:00", "PENDIENTE_VALIDACION", 2000,
            listOf(DetallePedidoDto(901, "Almendras", 1000, 2, 2000)))
        override suspend fun crear(pedido: CrearPedidoRequest): PedidoDto {
            envios++; ultima = pedido; error?.let { throw it }; return this.pedido
        }
        override suspend fun listar() = listOf(pedido)
        override suspend fun listarPorUsuario(usuarioId: Long) = listOf(pedido)
        override suspend fun obtener(id: Long) = pedido
        override suspend fun cambiarEstado(id: Long, request: CambiarEstadoRequest) = pedido.copy(estado = request.estado)
        override suspend fun cancelar(id: Long): Response<Unit> = Response.success(Unit)
    }
    @Before fun preparar() {
        base = Room.inMemoryDatabaseBuilder(contexto, OylDatabase::class.java).build()
        basePerfiles = Room.inMemoryDatabaseBuilder(contexto, PerfilRemotoDatabase::class.java).build()
        preferenciasNombre = "prueba_complementos_" + UUID.randomUUID()
        extras = DatosLocalesRemotosRepository(contexto.getSharedPreferences(preferenciasNombre, Context.MODE_PRIVATE), basePerfiles)
        productosApi = Catalogo()
        productos = ProductoRemoteRepository(contexto, productosApi, base)
        pedidosApi = Pedidos()
        carrito = CarritoRoomRepository(contexto, base)
        compras = CompraRemotaRepository(contexto, base, productos, PedidoRemoteRepository(pedidosApi), extras)
    }
    @After fun limpiar() {
        base.close()
        basePerfiles.close()
        contexto.deleteSharedPreferences(preferenciasNombre)
    }
    private suspend fun seleccionar(): List<ItemCarrito> {
        val producto = productos.listar().single()
        assertTrue(carrito.agregar("7", producto, 2))
        return carrito.observar("7").first()
    }

    @Test fun confirmaEnServidorConservaEntregaYNoDescuentaStockLocalDosVeces() = runBlocking {
        val items = seleccionar()
        val producto = productos.listar().single()
        assertTrue(carrito.agregar("8", producto, 1))
        val resultado = compras.confirmar("7", items, entrega)
        assertEquals("PED-055", resultado.pedido.id)
        assertEquals(1, pedidosApi.envios)
        assertEquals(CrearPedidoRequest(7, listOf(ItemPedidoRequest(901, 2))), pedidosApi.ultima)
        assertTrue(carrito.observar("7").first().isEmpty())
        assertEquals(1, carrito.observar("8").first().single().cantidad)
        assertEquals(10, base.productoDao().obtenerPorId(901)?.stock)
        productosApi.lista = productosApi.lista.map { it.copy(stock = 8) }
        productos.listar()
        assertEquals(8, base.productoDao().obtenerPorId(901)?.stock)
        val nuevaInstancia = DatosLocalesRemotosRepository(contexto.getSharedPreferences(preferenciasNombre, Context.MODE_PRIVATE), basePerfiles)
        assertEquals(entrega, nuevaInstancia.completarPedido(resultado.pedido.copy(entrega = null)).entrega)
        assertNull(nuevaInstancia.completarPedido(resultado.pedido.copy(usuarioId = "8", entrega = null)).entrega)
    }

    @Test fun precioModificadoImpideEnviarYConservaElPrecioAceptadoDelCarrito() = runBlocking {
        val items = seleccionar()
        productosApi.lista = productosApi.lista.map { it.copy(precio = 1200) }
        try { compras.confirmar("7", items, entrega); fail("Debe rechazar el cambio de precio") }
        catch (e: IllegalArgumentException) { assertTrue(e.message.orEmpty().contains("precio")) }
        assertEquals(0, pedidosApi.envios)
        assertEquals(1000, carrito.observar("7").first().single().precio)
        assertEquals(1200, base.productoDao().obtenerPorId(901)?.precio)
    }

    @Test fun rechazoDelServidorConservaElCarritoYNoGuardaUnaEntregaFicticia() = runBlocking {
        val items = seleccionar()
        pedidosApi.error = HttpException(Response.error<Unit>(409, "{}".toResponseBody()))
        try { compras.confirmar("7", items, entrega); fail("Debe rechazar el pedido") }
        catch (_: IllegalStateException) { }
        assertEquals(2, carrito.observar("7").first().single().cantidad)
        val remoto = with(PedidoRemoteRepository(pedidosApi)) { pedidosApi.pedido.aPedido() }
        assertNull(extras.completarPedido(remoto).entrega)
    }

    @Test fun catalogoEliminadoInvalidaStockDelCarritoSinBorrarlo() = runBlocking {
        seleccionar()
        productosApi.lista = emptyList()
        productos.listar()
        val item = carrito.observar("7").first().single()
        assertEquals(0, item.stockDisponible)
        assertEquals(2, item.cantidad)
    }

    @Test fun identificadorFueraDeRangoNoReemplazaUnaCacheValida() = runBlocking {
        productos.listar()
        productosApi.lista = productosApi.lista.map { it.copy(id = Int.MAX_VALUE.toLong() + 1) }
        try { productos.listar(); fail("Debe rechazar el id") } catch (_: IllegalArgumentException) { }
        assertNotNull(base.productoDao().obtenerPorId(901))
    }

    @Test fun falloDeBorradoRemotoNoSePresentaComoProductoInexistente() = runBlocking {
        productosApi.borrado = 503
        try { productos.eliminar(901); fail("Debe propagar el error del servidor") }
        catch (e: HttpException) { assertEquals(503, e.code()) }
        productosApi.borrado = 404
        assertFalse(productos.eliminar(901))
    }

    @Test fun rutYApellidoSonLocalesYSeSeparanPorIdentidadRemota() = runBlocking {
        val usuario = UsuarioDto(7, "Cliente prueba", "prueba", "prueba@example.test", "912345678", rol = "CLIENTE")
        extras.guardarPerfil("7", "12.345.678-5", "Apellido prueba")
        val perfil = extras.perfil(usuario)
        assertEquals("12.345.678-5", perfil.rut)
        assertEquals("Apellido prueba", perfil.apellido)
        assertEquals("", extras.perfil(usuario.copy(id = 8)).rut)
    }

    @Test fun aperturaSinConexionRecuperaCacheYReintentoSustituyeDatosYQuitaAviso() = runBlocking {
        val guardado = productos.listar().single()
        productosApi.error = IOException("Sin conexión de prueba")
        val almacen = ViewModelStore()
        val viewModel = withContext(Dispatchers.Main) {
            ProductoViewModel(remoteRepository = ProductoRemoteRepository(contexto, productosApi, base)).also {
                almacen.put("catalogo", it)
            }
        }
        try {
            withTimeout(5000) { viewModel.cargandoProductos.first { !it } }
            assertEquals(listOf(guardado), viewModel.productos.value)
            assertTrue(viewModel.errorCatalogo.value.orEmpty().contains("Mostrando catálogo guardado"))
            assertEquals(guardado, productos.cargarCache().single())

            productosApi.error = null
            productosApi.lista = productosApi.lista.map { it.copy(precio = 1200, stock = 8) }
            withContext(Dispatchers.Main) { viewModel.refrescar() }
            withTimeout(5000) { viewModel.productos.first { it.singleOrNull()?.precio == 1200 } }
            withTimeout(5000) { viewModel.cargandoProductos.first { !it } }
            assertEquals(8, viewModel.productos.value.single().stock)
            assertNull(viewModel.errorCatalogo.value)
            assertEquals(1200, productos.cargarCache().single().precio)
        } finally {
            withContext(Dispatchers.Main) { almacen.clear() }
        }
    }

    @Test fun aperturaSinConexionNiCacheExplicaLaAusenciaSinInventarProductos() = runBlocking {
        productosApi.error = IOException("Sin conexión de prueba")
        val almacen = ViewModelStore()
        val viewModel = withContext(Dispatchers.Main) {
            ProductoViewModel(remoteRepository = productos).also { almacen.put("catalogo", it) }
        }
        try {
            withTimeout(5000) { viewModel.cargandoProductos.first { !it } }
            assertTrue(viewModel.productos.value.isEmpty())
            assertTrue(viewModel.errorCatalogo.value.orEmpty().contains("No hay catálogo guardado"))
        } finally {
            withContext(Dispatchers.Main) { almacen.clear() }
        }
    }
}
