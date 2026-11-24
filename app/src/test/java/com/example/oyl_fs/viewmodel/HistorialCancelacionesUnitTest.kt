package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.model.*
import com.example.oyl_fs.network.CancelacionDto
import com.example.oyl_fs.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialCancelacionesUnitTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private val api=mockk<PedidoRemoteRepository>()
    private val cancelaciones=mockk<CancelacionPedidoRepository>()
    private val local=mockk<PedidoRoomRepository>()
    private val pedido=Pedido("PED-009","7",1,emptyList(),1000,EstadoPedido.PENDIENTE_VALIDACION)
    private val registros=MutableStateFlow(listOf(CancelacionGuardada(pedido,"EN_REVISION",null)))
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        coEvery { api.listarPorUsuario(any()) } returns Result.success(listOf(pedido,pedido.copy(id="PED-010",usuarioId="8")))
        coEvery { api.listarTodos() } returns Result.success(listOf(pedido))
        every { cancelaciones.observar(any()) } returns registros
    }
    @After fun limpiar() { store.clear();unmockkAll();Dispatchers.resetMain() }
    private fun vm()=HistorialViewModel(remoto=true,api=api,cancelacionesRepository=cancelaciones).also { store.put("vm",it);it.configurarSesion("7",false) }
    @Test fun clienteSoloVeLoPropioYCancelacionCompletaActualizaHistorial()=runTest {
        val vm=vm();val observacion=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.pedidosVisibles.collect() }
        runCurrent();assertTrue(vm.admiteCancelaciones);assertEquals(listOf(pedido),vm.pedidosVisibles.value)
        assertEquals(pedido,vm.obtenerPedido(pedido.id));assertEquals(pedido,vm.observarUltimoPedidoCliente("7").first())
        vm.seleccionarFiltro(EstadoPedido.ENTREGADO);runCurrent();assertTrue(vm.pedidosVisibles.value.isEmpty())
        vm.seleccionarFiltro(null)
        coEvery { cancelaciones.iniciar("7",pedido,true) } returns CancelacionDto(9,7,"COMPLETADA","Cancelado")
        coEvery { api.listarPorUsuario(7) } returns Result.success(emptyList())
        vm.gestionarCancelacion(pedido=pedido,continuar=true);runCurrent()
        assertEquals("Cancelado",vm.mensajeCancelacion.value);assertTrue(vm.pedidosParaAdmin.value.isEmpty())
        assertFalse(vm.procesandoCancelacion.value);observacion.cancel()
    }
    @Test fun noCancelaPedidoAjenoNiDuplicaMientrasEspera()=runTest {
        val vm=vm();runCurrent()
        vm.gestionarCancelacion(pedido=pedido.copy(usuarioId="8"),continuar=true)
        coVerify(exactly=0) { cancelaciones.iniciar(any(),any(),any()) }
        val terminado=CompletableDeferred<CancelacionDto>()
        coEvery { cancelaciones.recuperar("7",9,false) } coAnswers { terminado.await() }
        vm.gestionarCancelacion(pedidoId=9,continuar=false);runCurrent()
        vm.gestionarCancelacion(pedidoId=9,continuar=false);vm.cerrarResultadoCancelacion(9)
        assertTrue(vm.procesandoCancelacion.value)
        terminado.complete(CancelacionDto(9,7,"EN_REVISION","Consulta pendiente"));runCurrent()
        coVerify(exactly=1) { cancelaciones.recuperar(any(),any(),any()) }
        assertEquals("Consulta pendiente",vm.mensajeCancelacion.value)
    }
    @Test fun falloDelRegistroBloqueaOperacionesHastaReleerYSoloInformaErroresVigentes()=runTest {
        every { cancelaciones.observar("7") } returns flow { throw IOException() }
        val vm=vm();runCurrent();assertFalse(vm.registroCancelacionesLegible.value)
        vm.gestionarCancelacion(pedido=pedido,continuar=true)
        assertFalse(vm.cambiarEstadoPedidoPersistente(pedido.id,EstadoPedido.CONFIRMADO))
        every { cancelaciones.observar("7") } returns registros
        vm.configurarSesion("7",false);runCurrent();assertTrue(vm.registroCancelacionesLegible.value)
        coEvery { cancelaciones.recuperar(any(),any(),any()) } throws IOException()
        vm.gestionarCancelacion(pedidoId=9,continuar=false);runCurrent();assertNotNull(vm.mensajeCancelacion.value)
        coEvery { cancelaciones.cerrar(any(),any()) } throws IOException()
        vm.cerrarResultadoCancelacion(9);runCurrent();assertFalse(vm.procesandoCancelacion.value)
    }
    @Test fun respuestaDeCancelacionAnteriorNoPublicaMensajesEnOtraCuenta()=runTest {
        val terminado=CompletableDeferred<CancelacionDto>()
        coEvery { cancelaciones.iniciar(any(),any(),any()) } coAnswers { terminado.await() }
        val vm=vm();runCurrent();vm.gestionarCancelacion(pedido=pedido,continuar=true);runCurrent()
        vm.configurarSesion("8",false);runCurrent();terminado.complete(CancelacionDto(9,7,"COMPLETADA","Anterior"));runCurrent()
        assertNull(vm.mensajeCancelacion.value)
    }
    @Test fun historialRoomObservaDatosYDelegaCompraYEstadosSoloParaAdmin()=runTest {
        every { local.pedidos } returns flowOf(listOf(pedido))
        every { local.observarPedidosUsuario("7") } returns flowOf(listOf(pedido))
        val vm=HistorialViewModel(roomRepository=local);store.put("local",vm);vm.configurarSesion("7",false);runCurrent()
        assertFalse(vm.cambiarEstadoPedido(pedido.id,EstadoPedido.CONFIRMADO))
        assertFalse(vm.cambiarEstadoPedidoPersistente(pedido.id,EstadoPedido.CONFIRMADO))
        assertNull(vm.registrarPedido("7",emptyList()))
        assertEquals(pedido,vm.observarUltimoPedidoCliente("7").first())
        coEvery { local.registrarCompraDesdeCarrito("7",any(),any()) } returns pedido
        assertEquals(pedido,vm.registrarCompraPersistente("7",emptyList()))
        vm.configurarSesion("1",true)
        coEvery { local.actualizarEstadoPedido(pedido.id,EstadoPedido.CONFIRMADO) } returns true
        assertTrue(vm.cambiarEstadoPedidoPersistente(pedido.id,EstadoPedido.CONFIRMADO))
    }
}
