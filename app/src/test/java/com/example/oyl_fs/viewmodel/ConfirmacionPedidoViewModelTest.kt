package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.model.*
import com.example.oyl_fs.repository.CompraRemotaRepository
import com.example.oyl_fs.repository.PedidoRoomRepository
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmacionPedidoViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val remoto = mockk<CompraRemotaRepository>()
    private val local = mockk<PedidoRoomRepository>()
    private val almacen = ViewModelStore()
    private lateinit var vm: ConfirmacionPedidoViewModel
    private val items = listOf(ItemCarrito("4", "Almendras", "500 g", 1000, 2, 8))
    private val entrega = EntregaPedido(ModalidadEntrega.DOMICILIO, "Cliente ficticio", "912345678", "Calle ficticia 100")
    private val pedido = Pedido("PED-045", "7", 1700000000000,
        listOf(DetallePedido("4", "Almendras", 2, 1000)), 2000, EstadoPedido.PENDIENTE_VALIDACION, entrega)

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        vm = nuevo()
    }

    private fun nuevo(usarLocal: Boolean = false): ConfirmacionPedidoViewModel =
        ConfirmacionPedidoViewModel(mockk<Application>(), if (usarLocal) local else null, remoto).also {
            almacen.put(if (usarLocal) "local" else "remoto", it)
        }

    @After fun limpiar() {
        almacen.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
        clearAllMocks()
    }

    private fun iniciar() { vm.iniciar("7") }
    private fun confirmar() { vm.confirmar("7", items, entrega) }

    @Test fun estadoInicialSinComprobanteYConstructorDeAndroidDisponible() {
        assertEquals(ConfirmacionPedidoUiState(), vm.estado.value)
        assertNotNull(ConfirmacionPedidoViewModel::class.java.getConstructor(Application::class.java))
    }

    @Test fun sinUsuarioActivoOConOtroPropietarioNoEnviaPedidos() = runTest {
        vm.confirmar("", items, entrega)
        confirmar()
        iniciar()
        vm.confirmar("8", items, entrega)
        vm.confirmar("", items, entrega)
        runCurrent()
        coVerify(exactly = 0) { remoto.confirmar(any(), any(), any()) }
        coVerify(exactly = 0) { local.registrarCompraDesdeCarrito(any(), any(), any()) }
        assertEquals(ConfirmacionPedidoUiState("7"), vm.estado.value)
    }

    @Test fun bloqueaPulsacionesAntesYDuranteElEnvioYTrasRecibirElComprobante() = runTest {
        iniciar()
        val respuesta = CompletableDeferred<CompraRemotaRepository.Confirmada>()
        coEvery { remoto.confirmar("7", items, entrega) } coAnswers { respuesta.await() }
        confirmar()
        assertTrue(vm.estado.value.procesando)
        confirmar()
        runCurrent()
        confirmar()
        runCurrent()
        coVerify(exactly = 1) { remoto.confirmar("7", items, entrega) }
        respuesta.complete(CompraRemotaRepository.Confirmada(pedido))
        runCurrent()
        confirmar()
        runCurrent()
        coVerify(exactly = 1) { remoto.confirmar(any(), any(), any()) }
        assertEquals(pedido, vm.estado.value.pedido)
        assertFalse(vm.estado.value.procesando)
    }

    @Test fun conservaLaSeleccionTomadaAlPulsarAunqueCambieLaListaDelLlamador() = runTest {
        iniciar()
        val seleccion = items.toMutableList()
        val enviada = slot<List<ItemCarrito>>()
        coEvery { remoto.confirmar("7", capture(enviada), entrega) } returns CompraRemotaRepository.Confirmada(pedido)
        vm.confirmar("7", seleccion, entrega)
        seleccion.clear()
        runCurrent()
        assertEquals(items, enviada.captured)
        assertEquals(pedido, vm.estado.value.pedido)
    }

    @Test fun procesamientoImpideLimpiarOCambiarDeCuentaHastaRecibirRespuesta() = runTest {
        iniciar()
        val respuesta = CompletableDeferred<CompraRemotaRepository.Confirmada>()
        coEvery { remoto.confirmar(any(), any(), any()) } coAnswers { respuesta.await() }
        confirmar()
        runCurrent()
        vm.limpiar()
        vm.iniciar("8")
        assertEquals("7", vm.estado.value.usuarioId)
        assertTrue(vm.estado.value.procesando)
        respuesta.complete(CompraRemotaRepository.Confirmada(pedido))
        runCurrent()
        assertEquals("7", vm.estado.value.usuarioId)
        assertEquals(pedido, vm.estado.value.pedido)
        vm.iniciar("8")
        assertEquals(ConfirmacionPedidoUiState("8"), vm.estado.value)
    }

    @Test fun muestraElComprobanteRealDevueltoSinReconstruirSuNumeroNiImportes() = runTest {
        iniciar()
        val confirmado = pedido.copy(id = "PED-987", total = 2400, fecha = 1800000000000)
        coEvery { remoto.confirmar("7", items, entrega) } returns CompraRemotaRepository.Confirmada(confirmado)
        confirmar()
        runCurrent()
        assertEquals(ConfirmacionPedidoUiState("7", pedido = confirmado), vm.estado.value)
        coVerify(exactly = 0) { local.registrarCompraDesdeCarrito(any(), any(), any()) }
    }

    @Test fun advertenciaLocalNoConvierteUnPedidoConfirmadoEnUnFalloNiPermiteReenviarlo() = runTest {
        iniciar()
        val advertencia = "El pedido fue registrado, pero no se pudieron guardar los datos de entrega."
        coEvery { remoto.confirmar(any(), any(), any()) } returns CompraRemotaRepository.Confirmada(pedido, advertencia)
        confirmar()
        runCurrent()
        assertEquals(pedido, vm.estado.value.pedido)
        assertEquals(advertencia, vm.estado.value.error)
        assertFalse(vm.estado.value.procesando)
        confirmar()
        runCurrent()
        coVerify(exactly = 1) { remoto.confirmar(any(), any(), any()) }
    }

    @Test fun volverALaMismaCuentaConservaSuComprobanteYCambiarDeCuentaLoLimpia() = runTest {
        iniciar()
        coEvery { remoto.confirmar(any(), any(), any()) } returns CompraRemotaRepository.Confirmada(pedido)
        confirmar()
        runCurrent()
        vm.iniciar("7")
        assertEquals(pedido, vm.estado.value.pedido)
        vm.iniciar("8")
        assertEquals(ConfirmacionPedidoUiState("8"), vm.estado.value)
        vm.iniciar("")
        assertEquals(ConfirmacionPedidoUiState(), vm.estado.value)
    }

    @Test fun limpiarFueraDelEnvioConservaPropietarioYPermiteUnaCompraPosterior() = runTest {
        iniciar()
        coEvery { remoto.confirmar(any(), any(), any()) } returns CompraRemotaRepository.Confirmada(pedido)
        confirmar()
        runCurrent()
        vm.limpiar()
        assertEquals(ConfirmacionPedidoUiState("7"), vm.estado.value)
        coEvery { remoto.confirmar(any(), any(), any()) } returns CompraRemotaRepository.Confirmada(pedido.copy(id = "PED-046"))
        confirmar()
        runCurrent()
        assertEquals("PED-046", vm.estado.value.pedido?.id)
        coVerify(exactly = 2) { remoto.confirmar(any(), any(), any()) }
    }

    @Test fun validacionRechazadaNoProduceComprobanteYElSiguienteIntentoLimpiaElError() = runTest {
        iniciar()
        coEvery { remoto.confirmar(any(), any(), any()) } throws IllegalArgumentException("Revisa el stock de Almendras")
        confirmar()
        runCurrent()
        assertNull(vm.estado.value.pedido)
        assertFalse(vm.estado.value.procesando)
        assertEquals("Revisa el stock de Almendras", vm.estado.value.error)
        coEvery { remoto.confirmar(any(), any(), any()) } returns CompraRemotaRepository.Confirmada(pedido)
        confirmar()
        assertTrue(vm.estado.value.procesando)
        assertNull(vm.estado.value.error)
        runCurrent()
        assertEquals(pedido, vm.estado.value.pedido)
    }

    @Test fun desconexionNoInventaUnPedidoNiReintentaAutomaticamente() = runTest {
        iniciar()
        coEvery { remoto.confirmar(any(), any(), any()) } throws IOException("Detalle técnico")
        confirmar()
        advanceUntilIdle()
        assertNull(vm.estado.value.pedido)
        assertFalse(vm.estado.value.procesando)
        assertEquals("La conexión se interrumpió. Revisa tu conexión e inténtalo nuevamente.", vm.estado.value.error)
        coVerify(exactly = 1) { remoto.confirmar(any(), any(), any()) }
    }

    @Test fun respuestaInciertaConservaLaAdvertenciaDeRevisarMisPedidos() = runTest {
        iniciar()
        val aviso = "No se pudo confirmar la respuesta. Revisa Mis pedidos antes de volver a comprar."
        coEvery { remoto.confirmar(any(), any(), any()) } throws IllegalStateException(aviso)
        confirmar()
        runCurrent()
        assertEquals(aviso, vm.estado.value.error)
        assertNull(vm.estado.value.pedido)
        coVerify(exactly = 1) { remoto.confirmar(any(), any(), any()) }
    }

    @Test fun cancelarLaCorrutinaNoGeneraUnErrorDeNegocioNiUnComprobanteFicticio() = runTest {
        iniciar()
        coEvery { remoto.confirmar(any(), any(), any()) } throws CancellationException("Cancelada")
        confirmar()
        runCurrent()
        assertNull(vm.estado.value.error)
        assertNull(vm.estado.value.pedido)
        coVerify(exactly = 1) { remoto.confirmar(any(), any(), any()) }
    }

    @Test fun destruirElViewModelCancelaElEnvioPendienteSinPublicarUnResultado() = runTest {
        iniciar()
        var cancelado = false
        coEvery { remoto.confirmar(any(), any(), any()) } coAnswers {
            try { awaitCancellation() } finally { cancelado = true }
        }
        confirmar()
        runCurrent()
        almacen.clear()
        runCurrent()
        assertTrue(cancelado)
        assertNull(vm.estado.value.pedido)
        assertNull(vm.estado.value.error)
    }

    @Test fun compatibilidadRoomUsaSoloElRepositorioLocalYConservaElComprobante() = runTest {
        val modelo = nuevo(usarLocal = true)
        coEvery { local.registrarCompraDesdeCarrito("7", items, entrega) } returns pedido
        modelo.iniciar("7")
        modelo.confirmar("7", items, entrega)
        runCurrent()
        assertEquals(ConfirmacionPedidoUiState("7", pedido = pedido), modelo.estado.value)
        coVerify(exactly = 0) { remoto.confirmar(any(), any(), any()) }
    }

    @Test fun rechazoDeRoomMuestraComoCorregirDatosStockOPrecio() = runTest {
        val modelo = nuevo(usarLocal = true)
        coEvery { local.registrarCompraDesdeCarrito(any(), any(), any()) } returns null
        modelo.iniciar("7")
        modelo.confirmar("7", items, entrega)
        runCurrent()
        assertNull(modelo.estado.value.pedido)
        assertFalse(modelo.estado.value.procesando)
        assertEquals("No se pudo confirmar: revisa los datos y el stock. Si cambió un precio, quita el producto del carrito y agrégalo nuevamente.", modelo.estado.value.error)
    }

    @Test fun excepcionDeRoomEsLegibleYNoExponeDetallesDelAlmacenamiento() = runTest {
        val modelo = nuevo(usarLocal = true)
        coEvery { local.registrarCompraDesdeCarrito(any(), any(), any()) } throws IOException("Error SQLite privado")
        modelo.iniciar("7")
        modelo.confirmar("7", items, entrega)
        runCurrent()
        assertEquals("No se pudo guardar el pedido. Inténtalo nuevamente.", modelo.estado.value.error)
        assertNull(modelo.estado.value.pedido)
        assertFalse(modelo.estado.value.procesando)
    }
}
