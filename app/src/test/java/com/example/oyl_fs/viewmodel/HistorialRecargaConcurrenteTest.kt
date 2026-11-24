package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.repository.PedidoRemoteRepository
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HistorialRecargaConcurrenteTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = mockk<PedidoRemoteRepository>()
    private val almacen = ViewModelStore()
    private lateinit var vm: HistorialViewModel
    private val original = Pedido("PED-001","9",1,emptyList(),1000,EstadoPedido.PENDIENTE_VALIDACION)
    private val confirmado = original.copy(estado = EstadoPedido.CONFIRMADO)
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        coEvery { api.listarTodos() } returns Result.success(listOf(original))
        vm = HistorialViewModel(remoto = true, api = api)
        almacen.put("historial",vm)
        vm.configurarSesion("7",true)
        dispatcher.scheduler.runCurrent()
    }
    @After fun limpiar() { almacen.clear(); dispatcher.scheduler.runCurrent(); clearAllMocks(); Dispatchers.resetMain() }

    @Test fun actualizarMismaSesionNoDescartaExitoNiPermiteQueLecturaAntiguaLoRevierta() = runTest {
        val escritura = CompletableDeferred<Result<Pedido>>()
        coEvery { api.cambiarEstado(original.id,EstadoPedido.CONFIRMADO) } coAnswers { escritura.await() }
        val operacion = async { vm.cambiarEstadoPedidoPersistente(original.id,EstadoPedido.CONFIRMADO) }
        runCurrent()
        var antigua: Continuation<Result<List<Pedido>>>? = null
        val nueva = CompletableDeferred<Result<List<Pedido>>>()
        var lecturas = 0
        coEvery { api.listarTodos() } coAnswers {
            if (++lecturas == 1) suspendCoroutine { antigua = it } else nueva.await()
        }
        vm.configurarSesion("7",true)
        runCurrent()
        escritura.complete(Result.success(confirmado))
        runCurrent()
        assertTrue(operacion.await())
        assertEquals(EstadoPedido.CONFIRMADO,vm.pedidosParaAdmin.value.single().estado)
        assertTrue(vm.cargando.value)
        antigua!!.resume(Result.success(listOf(original)))
        runCurrent()
        assertEquals(EstadoPedido.CONFIRMADO,vm.pedidosParaAdmin.value.single().estado)
        assertTrue(vm.cargando.value)
        nueva.complete(Result.success(listOf(confirmado)))
        runCurrent()
        assertFalse(vm.cargando.value)
        coVerify(exactly = 1) { api.cambiarEstado(any(),any()) }
    }

    @Test fun falloDeRecargaPosteriorNoConvierteLaEscrituraConfirmadaEnFallo() = runTest {
        coEvery { api.cambiarEstado(any(),any()) } returns Result.success(confirmado)
        coEvery { api.listarTodos() } returns Result.failure(IOException("Sin conexión simulada"))
        assertTrue(vm.cambiarEstadoPedidoPersistente(original.id,EstadoPedido.CONFIRMADO))
        runCurrent()
        assertEquals(confirmado,vm.pedidosParaAdmin.value.single())
        assertNotNull(vm.error.value)
        assertFalse(vm.cargando.value)
    }

    @Test fun respuestaDeAdministradorAnteriorNoSePublicaEnLaCuentaSiguiente() = runTest {
        val respuesta = CompletableDeferred<Result<Pedido>>()
        coEvery { api.cambiarEstado(any(),any()) } coAnswers { respuesta.await() }
        coEvery { api.listarPorUsuario(8) } returns Result.success(emptyList())
        val operacion = async { vm.cambiarEstadoPedidoPersistente(original.id,EstadoPedido.CONFIRMADO) }
        runCurrent()
        vm.configurarSesion("8",false)
        runCurrent()
        respuesta.complete(Result.success(confirmado))
        runCurrent()
        assertFalse(operacion.await())
        assertTrue(vm.pedidosParaAdmin.value.isEmpty())
        assertNull(vm.error.value)
    }
}
