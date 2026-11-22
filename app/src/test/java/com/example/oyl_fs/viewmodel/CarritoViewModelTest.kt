package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.repository.CarritoRoomRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CarritoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = mockk<CarritoRoomRepository>()
    private val cuentas = mutableMapOf<String, MutableStateFlow<List<ItemCarrito>>>()
    private val almacen = ViewModelStore()
    private lateinit var vm: CarritoViewModel
    private val producto = ProductoInfo(4, "Almendras", "ALM-4", "Frutos secos", "Prueba", "500 g", 1000, 8, 2, true, 0)
    private val item = ItemCarrito("4", "Almendras", "500 g", 1000, 2, 8)

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        every { repo.observar(any()) } answers { cuenta(firstArg()) }
        vm = CarritoViewModel(mockk<Application>(), repo)
        almacen.put("carrito", vm)
    }

    @After fun limpiar() {
        almacen.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun cuenta(id: String) = cuentas.getOrPut(id) { MutableStateFlow(emptyList()) }
    private fun cargar(id: String = "a", items: List<ItemCarrito> = listOf(item)) {
        cuenta(id).value = items
        vm.iniciar(id)
        dispatcher.scheduler.runCurrent()
    }

    @Test fun conservaElConstructorApplicationQueUsaAndroid() {
        assertNotNull(CarritoViewModel::class.java.getConstructor(Application::class.java))
    }

    @Test fun cambiarCuentaLimpiaEstadoYDetieneLaObservacionAnterior() = runTest {
        cargar()
        assertEquals(listOf(item), vm.uiState.value.items)
        vm.iniciar("a")
        runCurrent()
        verify(exactly = 1) { repo.observar("a") }
        vm.iniciar("b")
        assertTrue(vm.uiState.value.estaVacio)
        runCurrent()
        cuenta("a").value = listOf(item.copy(cantidad = 5))
        cuenta("b").value = listOf(item.copy(cantidad = 1))
        runCurrent()
        assertEquals(1, vm.uiState.value.cantidadProductos)
        vm.iniciar("")
        runCurrent()
        assertTrue(vm.uiState.value.estaVacio)
        assertNull(vm.mensaje.value)
        verify(exactly = 0) { repo.observar("") }
    }

    @Test fun observacionFallidaPermiteReintentarLaMismaCuenta() = runTest {
        every { repo.observar("a") } returns flow { throw IOException("Lectura fallida") }
        vm.iniciar("a")
        runCurrent()
        assertEquals("No se pudo recuperar el carrito", vm.mensaje.value)
        every { repo.observar("a") } returns cuenta("a")
        cargar()
        assertEquals(listOf(item), vm.uiState.value.items)
        assertNull(vm.mensaje.value)
        verify(exactly = 2) { repo.observar("a") }
    }

    @Test fun validacionesImpidenEscribirSinClienteConCantidadInvalidaOProductoInactivo() = runTest {
        assertFalse(vm.agregarProducto(producto))
        assertEquals("No se ha identificado al cliente", vm.mensaje.value)
        cargar()
        for (cantidad in listOf(0, -1, 9)) {
            assertFalse(vm.agregarProducto(producto, cantidad))
            assertEquals("La cantidad supera las unidades disponibles", vm.mensaje.value)
        }
        assertFalse(vm.agregarProducto(producto.copy(disponible = false)))
        assertEquals("Este producto no está disponible", vm.mensaje.value)
        assertFalse(vm.agregarProducto(producto.copy(stock = 0)))
        coVerify(exactly = 0) { repo.agregar(any(), any(), any()) }
    }

    @Test fun agregarEnviaClienteProductoYCantidadYExplicaAceptacionORechazo() = runTest {
        cargar()
        coEvery { repo.agregar("a", producto, 2) } returns true
        assertTrue(vm.agregarProducto(producto, 2))
        assertEquals("Almendras agregado al carrito", vm.mensaje.value)
        coEvery { repo.agregar("a", producto, 1) } returns false
        assertFalse(vm.agregarProducto(producto))
        assertEquals("No hay stock suficiente de Almendras", vm.mensaje.value)
        coVerify(exactly = 1) { repo.agregar("a", producto, 2) }
        coVerify(exactly = 1) { repo.agregar("a", producto, 1) }
    }

    @Test fun falloDeGuardadoInformaElErrorSinModificarElEstadoObservado() = runTest {
        cargar()
        coEvery { repo.agregar(any(), any(), any()) } throws IOException("Escritura fallida")
        assertFalse(vm.agregarProducto(producto))
        assertEquals("No se pudo guardar el carrito", vm.mensaje.value)
        assertEquals(listOf(item), vm.uiState.value.items)
        vm.mensajeMostrado()
        assertNull(vm.mensaje.value)
    }

    @Test fun cancelacionAlAgregarSePropagaSinConvertirseEnErrorDeAlmacenamiento() = runTest {
        cargar()
        coEvery { repo.agregar(any(), any(), any()) } throws CancellationException("Cancelada")
        try { vm.agregarProducto(producto); fail("Debe propagar la cancelación") }
        catch (_: CancellationException) { }
        assertNull(vm.mensaje.value)
    }

    @Test fun aumentarYDisminuirUsanElDeltaCorrectoYSeReflejanPorElFlujo() = runTest {
        cargar()
        coEvery { repo.ajustar("a", 4, any()) } coAnswers {
            cuenta("a").value = listOf(item.copy(cantidad = cuenta("a").value.single().cantidad + thirdArg<Int>()))
            true
        }
        vm.aumentar(item)
        runCurrent()
        assertEquals(3, vm.uiState.value.cantidadProductos)
        vm.disminuir(item)
        runCurrent()
        assertEquals(2, vm.uiState.value.cantidadProductos)
        coVerify(exactly = 1) { repo.ajustar("a", 4, 1) }
        coVerify(exactly = 1) { repo.ajustar("a", 4, -1) }
    }

    @Test fun rechazoYErrorDeAjusteConservanLosArticulosEInformanElMotivo() = runTest {
        cargar()
        coEvery { repo.ajustar(any(), any(), any()) } returns false
        vm.aumentar(item)
        runCurrent()
        assertEquals("No hay unidades disponibles para este cambio", vm.mensaje.value)
        coEvery { repo.ajustar(any(), any(), any()) } throws IOException("Error")
        vm.disminuir(item)
        runCurrent()
        assertEquals("No se pudo actualizar el carrito", vm.mensaje.value)
        assertEquals(listOf(item), vm.uiState.value.items)
    }

    @Test fun ajustePendienteDeOtraCuentaNoMuestraSuErrorEnLaNueva() = runTest {
        cargar()
        val respuesta = CompletableDeferred<Boolean>()
        coEvery { repo.ajustar("a", 4, 1) } coAnswers { respuesta.await() }
        vm.aumentar(item)
        runCurrent()
        cargar("b", emptyList())
        respuesta.complete(false)
        runCurrent()
        assertNull(vm.mensaje.value)
        assertTrue(vm.uiState.value.estaVacio)
    }

    @Test fun quitarEscribeEnLaCuentaCorrectaYElFlujoConfirmaLaEliminacion() = runTest {
        cargar()
        coEvery { repo.quitar("a", 4) } coAnswers { cuenta("a").value = emptyList() }
        vm.quitar(item)
        runCurrent()
        assertTrue(vm.uiState.value.estaVacio)
        assertEquals("Almendras se quitó del carrito", vm.mensaje.value)
        coVerify(exactly = 1) { repo.quitar("a", 4) }
    }

    @Test fun quitarConErrorConservaLosArticulosYNoInformaExito() = runTest {
        cargar()
        coEvery { repo.quitar(any(), any()) } throws IOException("Error")
        vm.quitar(item)
        runCurrent()
        assertEquals("No se pudo quitar el producto", vm.mensaje.value)
        assertEquals(listOf(item), vm.uiState.value.items)
    }

    @Test fun quitarPendienteNoPublicaSuConfirmacionEnOtraCuenta() = runTest {
        cargar()
        val respuesta = CompletableDeferred<Unit>()
        coEvery { repo.quitar("a", 4) } coAnswers { respuesta.await() }
        vm.quitar(item)
        runCurrent()
        cargar("b", emptyList())
        respuesta.complete(Unit)
        runCurrent()
        assertNull(vm.mensaje.value)
        assertTrue(vm.uiState.value.estaVacio)
    }

    @Test fun identificadoresInvalidosNoSeEnvianParaAjustarOQuitar() = runTest {
        cargar()
        vm.aumentar(item.copy(productoId = "no-numerico"))
        vm.quitar(item.copy(productoId = "no-numerico"))
        runCurrent()
        coVerify(exactly = 0) { repo.ajustar(any(), any(), any()) }
        coVerify(exactly = 0) { repo.quitar(any(), any()) }
    }

    @Test fun antesDeConfirmarRechazaVacioYStockInsuficienteYAdmiteUnCarritoValido() = runTest {
        assertFalse(vm.validarAntesDeConfirmar())
        assertEquals("Tu carrito está vacío", vm.mensaje.value)
        cargar(items = listOf(item.copy(stockDisponible = 1)))
        assertFalse(vm.validarAntesDeConfirmar())
        assertEquals("Stock insuficiente: Almendras", vm.mensaje.value)
        cuenta("a").value = listOf(item)
        runCurrent()
        vm.mensajeMostrado()
        assertTrue(vm.validarAntesDeConfirmar())
        assertEquals(2000L, vm.uiState.value.total)
        assertNull(vm.mensaje.value)
    }

    @Test fun agregadoPendienteNoPublicaSuConfirmacionEnOtraCuenta() = runTest {
        comprobarAgregadoTardio(Result.success(true))
    }

    @Test fun rechazoPendienteDeAgregarNoSeMuestraEnOtraCuenta() = runTest {
        comprobarAgregadoTardio(Result.success(false))
    }

    @Test fun excepcionPendienteDeAgregarNoSeMuestraEnOtraCuenta() = runTest {
        comprobarAgregadoTardio(Result.failure(IOException("Escritura fallida")))
    }

    private suspend fun TestScope.comprobarAgregadoTardio(resultado: Result<Boolean>) {
        cargar()
        val respuesta = CompletableDeferred<Boolean>()
        coEvery { repo.agregar("a", producto, 1) } coAnswers { respuesta.await() }
        val agregado = async { vm.agregarProducto(producto) }
        runCurrent()
        cargar("b", emptyList())
        resultado.fold({ respuesta.complete(it) }, { respuesta.completeExceptionally(it) })
        assertEquals(resultado.getOrDefault(false), agregado.await())
        coVerify(exactly = 1) { repo.agregar("a", producto, 1) }
        coVerify(exactly = 0) { repo.agregar("b", any(), any()) }
        assertNull(vm.mensaje.value)
        assertTrue(vm.uiState.value.estaVacio)
    }

    @Test fun cancelacionDeObservacionNoSePresentaComoFalloYPermiteVolverAIniciar() = runTest {
        every { repo.observar("a") } returns flow { throw CancellationException("Cancelada") }
        vm.iniciar("a")
        runCurrent()
        assertNull(vm.mensaje.value)
        assertTrue(vm.uiState.value.estaVacio)
        every { repo.observar("a") } returns cuenta("a")
        cargar()
        assertEquals(listOf(item), vm.uiState.value.items)
    }

    @Test fun cancelacionDeAjusteYEliminacionNoPublicaErroresNiModificaLaLista() = runTest {
        cargar()
        coEvery { repo.ajustar(any(), any(), any()) } throws CancellationException("Cancelada")
        coEvery { repo.quitar(any(), any()) } throws CancellationException("Cancelada")
        vm.aumentar(item)
        vm.quitar(item)
        runCurrent()
        assertEquals(listOf(item), vm.uiState.value.items)
        assertNull(vm.mensaje.value)
    }
}
