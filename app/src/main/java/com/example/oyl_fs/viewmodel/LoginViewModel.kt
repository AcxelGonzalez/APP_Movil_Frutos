package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.oyl_fs.repository.UsuarioRoomRepository

import java.util.Locale

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Identidad obtenida después de autenticar.
 *
 * El identificador se utilizará posteriormente
 * para separar carritos, pedidos y perfiles.
 */
data class SesionIdentificada(
    val usuarioId: String,
    val nombre: String,
    val esAdmin: Boolean,
    val esDemo: Boolean
)

/**
 * Resultado de la comprobación de credenciales.
 */
sealed class ResultadoLogin {

    data class Exito(
        val sesion: SesionIdentificada
    ) : ResultadoLogin()

    data class Error(
        val mensaje: String
    ) : ResultadoLogin()
}

/**
 * Estado visual del formulario Login.
 */
data class LoginUiState(
    val procesando: Boolean = false,
    val error: String? = null
)

/**
 * Autenticación local de OYL Frutos Secos.
 *
 * Conserva las cuentas académicas admin
 * y cliente, pero también comprueba los
 * usuarios registrados mediante Room.
 */
class LoginViewModel(
    private val usuarios: UsuarioRoomRepository
) : ViewModel() {

    private val _estado =
        MutableStateFlow(LoginUiState())

    val estado = _estado.asStateFlow()

    fun limpiarError() {
        _estado.value = _estado.value.copy(
            error = null
        )
    }

    /**
     * Comprueba las credenciales y devuelve
     * la identidad correspondiente.
     *
     * No guarda ni activa una sesión todavía.
     */
    suspend fun autenticarCredenciales(
        usuario: String,
        contrasena: String
    ): ResultadoLogin {

        val alias = usuario.trim()
            .lowercase(Locale.ROOT)

        if (alias.isBlank() || contrasena.isEmpty()) {
            return ResultadoLogin.Error(
                "Completa usuario y contraseña."
            )
        }

        // Administrador de demostración.
        if (alias == "admin" && contrasena == "admin") {

            return ResultadoLogin.Exito(
                SesionIdentificada(
                    usuarioId = "admin-demo",
                    nombre = "Administrador",
                    esAdmin = true,
                    esDemo = true
                )
            )
        }

        // Cliente académico de demostración.
        if (alias == "cliente" && contrasena == "cliente") {

            return ResultadoLogin.Exito(
                SesionIdentificada(
                    usuarioId = "cliente-demo",
                    nombre = "Cliente",
                    esAdmin = false,
                    esDemo = true
                )
            )
        }

        // Clientes registrados mediante Room.
        val autenticado = usuarios.autenticarCliente(
            alias = alias,
            clave = contrasena
        ) ?: return ResultadoLogin.Error(
            "Usuario o contraseña incorrectos."
        )

        return ResultadoLogin.Exito(
            SesionIdentificada(
                usuarioId = autenticado.id,
                nombre = autenticado.nombre,
                esAdmin = false,
                esDemo = false
            )
        )
    }

    /**
     * Versión que utilizará LoginScreen
     * cuando conectemos la interfaz.
     */
    fun iniciarSesion(
        usuario: String,
        contrasena: String,
        onExito: (SesionIdentificada) -> Unit
    ) {

        if (_estado.value.procesando) return

        _estado.value = LoginUiState(
            procesando = true
        )

        viewModelScope.launch {

            try {

                when (
                    val resultado = autenticarCredenciales(
                        usuario,
                        contrasena
                    )
                ) {

                    is ResultadoLogin.Exito -> {
                        _estado.value = LoginUiState()
                        onExito(resultado.sesion)
                    }

                    is ResultadoLogin.Error -> {
                        _estado.value = LoginUiState(
                            error = resultado.mensaje
                        )
                    }
                }

            } catch (e: CancellationException) {
                throw e

            } catch (_: Exception) {
                _estado.value = LoginUiState(
                    error = "No se pudo comprobar la cuenta."
                )
            } finally {
                _estado.value = _estado.value.copy(
                    procesando = false
                )
            }
        }
    }
}
