package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import com.example.oyl_fs.repository.PuntoRetiroRepository
import com.example.oyl_fs.data.local.PuntosRetiroIniciales
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapaViewModelTest {
    @Test fun consultaDePuntosYSeleccionConservanDatosParaElRetiro() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val almacen = ViewModelStore()
        val repo = mockk<PuntoRetiroRepository>()
        every { repo.observarPuntos() } returns MutableStateFlow(PuntosRetiroIniciales.puntos)
        try {
            val vm = MapaViewModel(mockk<Application>(), repo)
            almacen.put("mapa", vm)
            runCurrent()
            val puntos = vm.uiState.value.puntos
            assertTrue(puntos.isNotEmpty())
            assertEquals(puntos.size, puntos.map { it.id }.distinct().size)
            assertTrue(puntos.all { it.direccion.isNotBlank() && it.horario.isNotBlank() && it.indicaciones.isNotBlank() })
            assertTrue(puntos.all { it.latitud in -90.0..90.0 && it.longitud in -180.0..180.0 })
            val tienda = requireNotNull(vm.uiState.value.tienda)
            assertTrue(tienda.esTienda)
            val retiro = puntos.first { !it.esTienda }
            vm.seleccionar(retiro)
            assertEquals(retiro, vm.uiState.value.seleccionado)
            assertEquals(tienda, vm.uiState.value.tienda)
            vm.cerrarDetalle()
            assertNull(vm.uiState.value.seleccionado)
            assertEquals(puntos, vm.uiState.value.puntos)
            assertNull(MapaUiState().tienda)
        } finally {
            almacen.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }
}
