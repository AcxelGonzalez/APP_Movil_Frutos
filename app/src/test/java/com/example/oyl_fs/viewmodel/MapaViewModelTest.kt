package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.repository.PuntoRetiroRepository
import org.junit.Assert.*
import org.junit.Test

class MapaViewModelTest {
    @Test fun consultaDePuntosYSeleccionConservanDatosParaElRetiro() {
        val vm = MapaViewModel()
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
        assertEquals(retiro, PuntoRetiroRepository.buscarPorId(retiro.id))
        assertNull(PuntoRetiroRepository.buscarPorId("NO-EXISTE"))
        vm.cerrarDetalle()
        assertNull(vm.uiState.value.seleccionado)
        assertEquals(puntos, vm.uiState.value.puntos)
        assertNull(MapaUiState().tienda)
    }
}
