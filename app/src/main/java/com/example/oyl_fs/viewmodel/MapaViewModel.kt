package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.model.PuntoRetiro
import com.example.oyl_fs.repository.PuntoRetiroRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MapaUiState(
    val puntos: List<PuntoRetiro> = emptyList(),
    val seleccionado: PuntoRetiro? = null,
    val cargando: Boolean = false,
    val error: String? = null
) {
    val tienda: PuntoRetiro? get() = puntos.firstOrNull { it.esTienda }
}

class MapaViewModel @JvmOverloads constructor(
    application: Application,
    private val repo: PuntoRetiroRepository = PuntoRetiroRepository(OylDatabase.obtener(application))
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MapaUiState(cargando = true))
    val uiState = _uiState.asStateFlow()
    private var observacion: Job? = null

    init { reintentar() }

    fun reintentar() {
        if (observacion?.isActive == true) return
        _uiState.update { it.copy(cargando = true, error = null) }
        observacion = viewModelScope.launch {
            try {
                repo.observarPuntos().collect { puntos ->
                    _uiState.update { actual ->
                        MapaUiState(puntos, puntos.find { it.id == actual.seleccionado?.id })
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value = MapaUiState(error = "No se pudieron leer los puntos de retiro guardados. Intenta nuevamente.")
            }
        }
    }

    fun seleccionar(punto: PuntoRetiro) {
        _uiState.update { it.copy(seleccionado = it.puntos.find { registro -> registro.id == punto.id }) }
    }

    fun cerrarDetalle() {
        _uiState.update { it.copy(seleccionado = null) }
    }
}
