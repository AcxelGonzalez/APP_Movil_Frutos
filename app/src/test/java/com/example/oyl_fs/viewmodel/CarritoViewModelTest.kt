package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.DetallePedidoDto
import com.example.oyl_fs.network.PedidoDto
import com.example.oyl_fs.network.PedidosApi
import com.example.oyl_fs.repository.CarritoRepository
import com.example.oyl_fs.repository.PedidoRemoteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/*
Revisa el carrito del cliente: que sume bien el total, que no deje
agregar más unidades de las que hay en stock, que al bajar la cantidad
a cero el producto desaparezca, y que al confirmar la compra el pedido
llegue al servidor y el carrito quede vacío.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CarritoViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val cliente = "cliente-prueba"

    private val almendras = producto(1, "Almendras", stock = 5)

    @Before
    fun prepararHiloPrincipal() {
        Dispatchers.setMain(dispatcher)
        CarritoRepository.vaciar(cliente)
    }

    @After
    fun liberarHiloPrincipal() {
        Dispatchers.resetMain()
    }

    @Test
    fun sinClienteIdentificadoNoSePuedeAgregarNada() = runTest(dispatcher) {
        val vm = CarritoViewModel()

        val agregado = vm.agregarProducto(almendras)

        assertFalse(agregado)
        assertEquals("No se ha identificado al cliente", vm.mensaje.value)
    }

    @Test
    fun alAgregarUnProductoSeCalculaElTotal() = runTest(dispatcher) {
        val vm = carrito()

        val agregado = vm.agregarProducto(almendras, cantidad = 2)

        assertTrue(agregado)
        assertEquals(2, vm.uiState.value.cantidadProductos)
        assertEquals(13980L, vm.uiState.value.total)
        assertFalse(vm.uiState.value.estaVacio)
    }

    @Test
    fun noSePuedeAgregarMasUnidadesQueElStock() = runTest(dispatcher) {
        val vm = carrito()

        val agregado = vm.agregarProducto(almendras, cantidad = 6)

        assertFalse(agregado)
        assertEquals("La cantidad supera las unidades disponibles", vm.mensaje.value)
        assertTrue(vm.uiState.value.estaVacio)
    }

    @Test
    fun unProductoAgotadoNoSePuedeAgregar() = runTest(dispatcher) {
        val vm = carrito()

        val agregado = vm.agregarProducto(almendras.copy(stock = 0))

        assertFalse(agregado)
        assertTrue(vm.uiState.value.estaVacio)
    }

    @Test
    fun aumentarRespetaElStockDisponible() = runTest(dispatcher) {
        val vm = carrito()
        vm.agregarProducto(almendras, cantidad = 5)

        vm.aumentar(vm.uiState.value.items.single())

        assertEquals(5, vm.uiState.value.cantidadProductos)
        assertEquals("Solo hay 5 unidades de Almendras", vm.mensaje.value)
    }

    @Test
    fun disminuirHastaCeroQuitaElProducto() = runTest(dispatcher) {
        val vm = carrito()
        vm.agregarProducto(almendras, cantidad = 1)

        vm.disminuir(vm.uiState.value.items.single())

        assertTrue(vm.uiState.value.estaVacio)
    }

    @Test
    fun quitarUnProductoAvisaAlUsuario() = runTest(dispatcher) {
        val vm = carrito()
        vm.agregarProducto(almendras, cantidad = 2)

        vm.quitar(vm.uiState.value.items.single())

        assertTrue(vm.uiState.value.estaVacio)
        assertEquals("Almendras se quitó del carrito", vm.mensaje.value)
    }

    @Test
    fun unCarritoVacioNoSePuedeConfirmar() = runTest(dispatcher) {
        val vm = carrito()

        assertFalse(vm.validarAntesDeConfirmar())
        assertEquals("Tu carrito está vacío", vm.mensaje.value)
    }

    @Test
    fun siElProductoDesapareceDelCatalogoSuStockQuedaEnCero() = runTest(dispatcher) {
        val vm = carrito()
        vm.agregarProducto(almendras, cantidad = 2)

        vm.sincronizarConCatalogo(cliente, emptyList())

        assertEquals(0, vm.uiState.value.items.single().stockDisponible)
        assertFalse(vm.validarAntesDeConfirmar())
    }

    @Test
    fun alConfirmarLaCompraElPedidoSeEnviaYElCarritoQuedaVacio() = runTest(dispatcher) {
        val api: PedidosApi = mockk()
        PedidoRemoteRepository.api = api
        coEvery { api.crear(any()) } returns pedidoCreado()

        val vm = carrito()
        vm.agregarProducto(almendras, cantidad = 2)

        var confirmado = false
        vm.enviarPedido(usuarioId = 7L) { confirmado = true }

        assertTrue(confirmado)
        assertTrue(vm.uiState.value.estaVacio)
        assertEquals("Pedido #15 creado correctamente", vm.mensaje.value)
        assertFalse(vm.enviando.value)
    }

    @Test
    fun elAvisoSeLimpiaDespuesDeMostrarlo() = runTest(dispatcher) {
        val vm = carrito()
        vm.agregarProducto(almendras)

        vm.mensajeMostrado()

        assertNull(vm.mensaje.value)
    }

    // Crea el ViewModel, lo asocia al cliente y deja alguien escuchando
    // el carrito: sin un observador activo, uiState no se actualiza.
    private fun kotlinx.coroutines.test.TestScope.carrito(): CarritoViewModel {
        val vm = CarritoViewModel()
        vm.iniciar(cliente)
        backgroundScope.launch { vm.uiState.collect {} }
        return vm
    }

    private fun producto(id: Int, nombre: String, stock: Int) = ProductoInfo(
        id = id,
        nombre = nombre,
        codigo = "OYL00$id",
        categoria = "Frutos secos",
        descripcion = "Bolsa de 500 g",
        presentacion = "500 g",
        precio = 6990,
        stock = stock,
        stockMinimo = 2,
        disponible = true
    )

    private fun pedidoCreado() = PedidoDto(
        id = 15L,
        usuarioId = 7L,
        fecha = "2026-11-23T19:00:00",
        estado = "PENDIENTE_VALIDACION",
        total = 13980L,
        detalles = listOf(
            DetallePedidoDto(1L, "Almendras", 6990, 2, 13980L)
        )
    )
}