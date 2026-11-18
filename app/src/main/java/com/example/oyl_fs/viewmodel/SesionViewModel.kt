package com.example.oyl_fs.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.repository.SesionRepository
import com.example.oyl_fs.repository.UsuarioRoomRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SesionUiState(
    val sesion: SesionIdentificada? = null,
    val restaurando: Boolean = true,
    val procesando: Boolean = false,
    val error: String? = null
)

class SesionViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SesionRepository(
        application.getSharedPreferences("sesion_oyl", Context.MODE_PRIVATE),
        UsuarioRoomRepository(application)
    )
    private val mutex = Mutex()
    private val _estado = MutableStateFlow(SesionUiState())
    val estado = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            mutex.withLock {
                try {
                    _estado.value = SesionUiState(sesion = repo.restaurar(), restaurando = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _estado.value = SesionUiState(restaurando = false, error = "No se pudo recuperar la sesión. Inicia sesión nuevamente.")
                }
            }
        }
    }

    fun iniciar(sesion: SesionIdentificada) = cambiar("No se pudo guardar la sesión") {
        repo.guardar(sesion)
        sesion
    }

    fun cerrar() = cambiar("No se pudo cerrar la sesión") {
        repo.cerrar()
        null
    }

    private fun cambiar(error: String, accion: suspend () -> SesionIdentificada?) {
        if (_estado.value.procesando || _estado.value.restaurando) return
        _estado.value = _estado.value.copy(procesando = true, error = null)
        viewModelScope.launch {
            mutex.withLock {
                try {
                    _estado.value = SesionUiState(sesion = accion(), restaurando = false)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _estado.value = _estado.value.copy(procesando = false, error = error)
                }
            }
        }
    }
}
