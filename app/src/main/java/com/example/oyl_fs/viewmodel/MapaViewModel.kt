package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import com.example.oyl_fs.model.PuntoRetiro
import com.example.oyl_fs.repository.PuntoRetiroRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MapaUiState(
    val puntos: List<PuntoRetiro> = emptyList(),
    val seleccionado: PuntoRetiro? = null
) {

    val tienda: PuntoRetiro? get() = puntos.firstOrNull { it.esTienda }
}

class MapaViewModel : ViewModel() {

    private val repo = PuntoRetiroRepository

    private val _uiState = MutableStateFlow(
        MapaUiState(puntos = repo.obtenerPuntos())
    )
    val uiState: StateFlow<MapaUiState> = _uiState.asStateFlow()

    fun seleccionar(punto: PuntoRetiro) {
        _uiState.update { it.copy(seleccionado = punto) }
    }

    fun cerrarDetalle() {
        _uiState.update { it.copy(seleccionado = null) }
    }
}