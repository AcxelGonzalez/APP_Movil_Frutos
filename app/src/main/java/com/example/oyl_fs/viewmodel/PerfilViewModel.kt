package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.oyl_fs.network.ActualizarPerfilRequest
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.network.mensajeApi
import com.example.oyl_fs.repository.SesionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// El perfil ya no se guarda en el teléfono: es el usuario que inició sesión
// y los cambios se guardan en el backend (ms-usuarios).
class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    // Usuario con la sesión iniciada (null si no hay sesión)
    val perfil: StateFlow<UsuarioDto?> = SesionRepository.usuario

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Mientras se espera la respuesta del servidor
    private val _guardando = MutableStateFlow(false)
    val guardando: StateFlow<Boolean> = _guardando.asStateFlow()

    fun limpiarError() { _error.value = null }

    // Valida los datos y, si están bien, los guarda en el backend.
    // onExito se ejecuta solo si el servidor aceptó los cambios.
    fun guardarPerfil(
        nombre: String,
        correo: String,
        telefono: String,
        direccion: String,
        onExito: () -> Unit
    ) {
        if (_guardando.value) return

        _error.value = when {
            nombre.isBlank() -> "Escribe el nombre"
            !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(correo.trim()) ->
                "Escribe un correo válido"
            telefono.isNotBlank() &&
                    (telefono.count { it.isDigit() } !in 8..15 ||
                            telefono.any { !it.isDigit() && it !in "+ -()" }) ->
                "Revisa el teléfono"
            else -> null
        }
        if (_error.value != null) return

        _guardando.value = true
        viewModelScope.launch {
            SesionRepository.actualizarPerfil(
                ActualizarPerfilRequest(
                    nombre = nombre.trim(),
                    correo = correo.trim(),
                    telefono = telefono.trim().ifEmpty { null },
                    direccion = direccion.trim().ifEmpty { null }
                )
            )
                .onSuccess {
                    _error.value = null
                    onExito()
                }
                .onFailure { e -> _error.value = e.mensajeApi() }
            _guardando.value = false
        }
    }
}
