package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.repository.SesionRemotaRepository
import com.example.oyl_fs.repository.SesionActiva
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
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
    private val repo = SesionRemotaRepository.also { it.configurar(application) }
    private val mutex = Mutex()
    private val _estado = MutableStateFlow(SesionUiState())
    val estado = _estado.asStateFlow()
    private var idVisible: String? = null
    private var vencimiento: Job? = null

    init {
        viewModelScope.launch {
            repo.invalidacion.collect { aviso ->
                if (aviso != null) mutex.withLock {
                    if (idVisible == aviso.idSesion) {
                        idVisible = null; vencimiento?.cancel()
                        _estado.value = SesionUiState(restaurando = false, error = aviso.mensaje)
                    }
                }
            }
        }
        viewModelScope.launch {
            mutex.withLock {
                try {
                    repo.restaurar()
                    aplicar(repo.sesionActual(), repo.mensajeRestauracion)
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) {
                    aplicar(null, "No se pudo recuperar la sesión. Inicia sesión nuevamente.")
                }
            }
        }
    }

    private fun aplicar(sesion: SesionActiva?, error: String? = null) {
        idVisible = sesion?.idSesion
        _estado.value = SesionUiState(sesion?.identidad, restaurando = false, error = error)
        vencimiento?.cancel()
        if (sesion != null) vencimiento = viewModelScope.launch {
            delay((sesion.venceEnMillis - System.currentTimeMillis()).coerceAtLeast(0))
            repo.expirarSiCoincide(sesion.idSesion)
        }
    }

    fun verificarVigencia() {
        val id = idVisible ?: return
        viewModelScope.launch { repo.expirarSiCoincide(id) }
    }

    fun iniciar(sesion: SesionIdentificada) = cambiar("No se pudo guardar la sesión") { repo.guardar(sesion) }
    fun cerrar() = cambiar("No se pudo cerrar la sesión") { repo.cerrar() }

    private fun cambiar(error: String, accion: suspend () -> Unit) {
        if (_estado.value.procesando || _estado.value.restaurando) return
        _estado.value = _estado.value.copy(procesando = true, error = null)
        viewModelScope.launch {
            mutex.withLock {
                try {
                    accion()
                    aplicar(repo.sesionActual())
                } catch (e: CancellationException) { throw e
                } catch (_: Exception) {
                    aplicar(repo.sesionActual(), error)
                }
            }
        }
    }
}
