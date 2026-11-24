package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.repository.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelUnitTest {
    private val dispatcher=StandardTestDispatcher()
    private val store=ViewModelStore()
    private val avisos=MutableStateFlow<InvalidacionSesion?>(null)
    private val identidad=SesionIdentificada("7","Cliente",false,false)
    private var actual:SesionActiva?=null
    private fun vm()=SesionViewModel(mockk<Application>()).also { store.put("vm",it) }
    @Before fun preparar() {
        Dispatchers.setMain(dispatcher);mockkObject(SesionRemotaRepository)
        every { SesionRemotaRepository.configurar(any(),any()) } just Runs
        every { SesionRemotaRepository.invalidacion } returns avisos
        every { SesionRemotaRepository.sesionActual() } answers { actual }
        every { SesionRemotaRepository.mensajeRestauracion } returns null
        coEvery { SesionRemotaRepository.restaurar() } returns null
        coEvery { SesionRemotaRepository.expirarSiCoincide(any()) } returns Unit
    }
    @After fun limpiar() { store.clear();unmockkAll();Dispatchers.resetMain() }
    @Test fun restauracionFinalizaYLoginLogoutPublicanEstadoConfirmado() = runTest {
        val vm=vm();assertTrue(vm.estado.value.restaurando)
        vm.iniciar(identidad);coVerify(exactly=0) { SesionRemotaRepository.guardar(any()) }
        runCurrent();assertFalse(vm.estado.value.restaurando)
        coEvery { SesionRemotaRepository.guardar(identidad) } coAnswers { actual=SesionActiva(identidad,"s7",System.currentTimeMillis()+60000) }
        vm.iniciar(identidad);runCurrent();assertEquals(identidad,vm.estado.value.sesion)
        vm.verificarVigencia();runCurrent();coVerify { SesionRemotaRepository.expirarSiCoincide("s7") }
        coEvery { SesionRemotaRepository.cerrar() } coAnswers { actual=null }
        vm.cerrar();runCurrent();assertNull(vm.estado.value.sesion)
        store.clear()
    }
    @Test fun invalidacionAntiguaNoBorraSesionActualYVencimientoSeConsulta() = runTest {
        actual=SesionActiva(identidad,"actual",System.currentTimeMillis()+1000)
        val vm=vm();runCurrent()
        avisos.value=InvalidacionSesion("antigua","Anterior");runCurrent();assertEquals(identidad,vm.estado.value.sesion)
        advanceTimeBy(1100);runCurrent();coVerify { SesionRemotaRepository.expirarSiCoincide("actual") }
        avisos.value=InvalidacionSesion("actual","Venció");runCurrent()
        assertNull(vm.estado.value.sesion);assertEquals("Venció",vm.estado.value.error)
        store.clear()
    }
    @Test fun fallosDeRestauracionYGuardadoSeInformanSinInventarSesion() = runTest {
        coEvery { SesionRemotaRepository.restaurar() } throws IOException()
        val vm=vm();runCurrent();assertNotNull(vm.estado.value.error);assertFalse(vm.estado.value.restaurando)
        coEvery { SesionRemotaRepository.guardar(any()) } throws IOException()
        vm.iniciar(identidad);runCurrent();assertEquals("No se pudo guardar la sesión",vm.estado.value.error)
        coEvery { SesionRemotaRepository.cerrar() } throws IOException()
        vm.cerrar();runCurrent();assertEquals("No se pudo cerrar la sesión",vm.estado.value.error)
        vm.verificarVigencia();store.clear()
    }
    @Test fun noDuplicaAccionMientrasSeGuarda() = runTest {
        val terminado=CompletableDeferred<Unit>()
        coEvery { SesionRemotaRepository.guardar(any()) } coAnswers { terminado.await() }
        val vm=vm();runCurrent();vm.iniciar(identidad);runCurrent()
        assertTrue(vm.estado.value.procesando);vm.cerrar()
        terminado.complete(Unit);runCurrent();assertFalse(vm.estado.value.procesando)
        coVerify(exactly=0) { SesionRemotaRepository.cerrar() }
        store.clear()
    }
}
