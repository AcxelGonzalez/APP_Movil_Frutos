package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.network.mensajeApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.oyl_fs.repository.DatosRegistroCliente
import com.example.oyl_fs.repository.ResultadoRegistro
import com.example.oyl_fs.repository.UsuarioRoomRepository

import java.util.Locale
import com.example.oyl_fs.util.ValidacionContacto

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Datos que recibe el formulario de registro.
 */
data class FormularioRegistro(
    val alias: String,
    val nombre: String,
    val correo: String,
    val rut: String,
    val telefono: String,
    val clave: String,
    val confirmacion: String
)

/**
 * Errores individuales de cada campo.
 */
data class ErroresRegistro(
    val alias: String? = null,
    val nombre: String? = null,
    val correo: String? = null,
    val rut: String? = null,
    val telefono: String? = null,
    val clave: String? = null,
    val confirmacion: String? = null,
    val general: String? = null
) {
    fun hayErrores(): Boolean = listOf(
        alias, nombre, correo, rut, telefono,
        clave, confirmacion, general
    ).any { it != null }
}

data class RegistroUiState(
    val procesando: Boolean = false,
    val errores: ErroresRegistro = ErroresRegistro()
)

enum class CampoRegistro {
    ALIAS, NOMBRE, CORREO, RUT,
    TELEFONO, CLAVE, CONFIRMACION
}

/**
 * Validaciones independientes de Compose.
 * Se pueden comprobar mediante pruebas JVM.
 */
object ValidacionRegistro {

    fun validar(datos: FormularioRegistro): ErroresRegistro {

        val alias = datos.alias.trim()
            .lowercase(Locale.ROOT)

        val correo = datos.correo.trim()
            .lowercase(Locale.ROOT)

        val aliasValido =
            Regex("^[a-z0-9._-]{3,24}$")
                .matches(alias)

        val correoValido =
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                .matches(correo)

        return ErroresRegistro(
            alias = when {
                !aliasValido ->
                    "Usa entre 3 y 24 caracteres: letras, números, punto, guion o guion bajo."
                alias in setOf("admin", "cliente", "cliente-demo") ->
                    "Este alias está reservado."
                else -> null
            },

            nombre = if (datos.nombre.isBlank()) {
                "Ingresa tu nombre completo."
            } else com.example.oyl_fs.util.ValidacionLongitud.errorNombrePerfil(datos.nombre),

            correo = if (!correoValido) {
                "Ingresa un correo electrónico válido."
            } else null,

            rut = if (!ValidacionContacto.rutValido(datos.rut)) {
                "Ingresa un RUT válido, por ejemplo 12.345.678-5."
            } else null,

            telefono = if (
                datos.telefono.isNotBlank() &&
                !ValidacionContacto.telefonoValido(datos.telefono)
            ) {
                "Ingresa un teléfono válido."
            } else null,

            clave = com.example.oyl_fs.util.ValidacionContrasena.errorRegistro(datos.clave),

            confirmacion = if (
                datos.confirmacion != datos.clave
            ) {
                "Las contraseñas no coinciden."
            } else null
        )
    }

}

/**
 * Valida el registro. MainActivity utiliza ms-usuarios y complementos locales;
 * las pruebas de compatibilidad pueden utilizar UsuarioRoomRepository.
 */
class RegistroViewModel(
    private val repository: UsuarioRoomRepository,
    private val complementos: com.example.oyl_fs.repository.DatosLocalesRemotosRepository? = null
) : ViewModel() {

    val usaBackend: Boolean get() = complementos != null

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado = _estado.asStateFlow()

    fun limpiarError(campo: CampoRegistro) {

        _estado.update { actual ->
            val errores = actual.errores

            val nuevos = when (campo) {
                CampoRegistro.ALIAS ->
                    errores.copy(alias = null, general = null)
                CampoRegistro.NOMBRE ->
                    errores.copy(nombre = null, general = null)
                CampoRegistro.CORREO ->
                    errores.copy(correo = null, general = null)
                CampoRegistro.RUT ->
                    errores.copy(rut = null, general = null)
                CampoRegistro.TELEFONO ->
                    errores.copy(telefono = null, general = null)
                CampoRegistro.CLAVE ->
                    errores.copy(clave = null, general = null)
                CampoRegistro.CONFIRMACION ->
                    errores.copy(confirmacion = null, general = null)
            }

            actual.copy(errores = nuevos)
        }
    }

    fun reiniciar() {
        if (!_estado.value.procesando) {
            _estado.value = RegistroUiState()
        }
    }

    /**
     * Valida el formulario y solicita el registro.
     *
     * La navegación solo se ejecuta después
     * de confirmar el registro en el repositorio configurado.
     */
    fun registrar(
        formulario: FormularioRegistro,
        onRegistroExitoso: () -> Unit
    ) {

        if (_estado.value.procesando) return

        var errores = ValidacionRegistro.validar(formulario)
        if (complementos != null) {
            if (formulario.alias.trim().length > 20) errores = errores.copy(alias = "El alias debe tener entre 3 y 20 caracteres")
            if (formulario.telefono.isBlank()) errores = errores.copy(telefono = "Ingresa tu teléfono")
        }

        if (errores.hayErrores()) {
            _estado.value = RegistroUiState(
                errores = errores
            )
            return
        }

        _estado.value = RegistroUiState(
            procesando = true
        )

        viewModelScope.launch {
            try {

                if (complementos != null) {
                    val usuario = com.example.oyl_fs.repository.SesionRemotaRepository.registrar(
                        com.example.oyl_fs.network.RegistroRequest(
                            nombre = formulario.nombre.trim(), alias = formulario.alias.trim().lowercase(Locale.ROOT),
                            correo = formulario.correo.trim().lowercase(Locale.ROOT), telefono = formulario.telefono.trim(),
                            contrasena = formulario.clave
                        )
                    ).getOrThrow()
                    try {
                        complementos.guardarPerfil(usuario, formulario.rut.trim())
                    } catch (e: CancellationException) { throw e
                    } catch (e: Exception) {
                        _estado.value = RegistroUiState(errores = ErroresRegistro(general = "La cuenta ya fue creada. Inicia sesión y vuelve a ingresar tu RUT en el perfil."))
                        return@launch
                    }
                    _estado.value = RegistroUiState()
                    onRegistroExitoso()
                    return@launch
                }
                val resultado = repository.registrarCliente(
                    DatosRegistroCliente(
                        alias = formulario.alias.trim(),
                        nombre = formulario.nombre.trim(),
                        correo = formulario.correo.trim(),
                        rut = formulario.rut.trim(),
                        telefono = formulario.telefono.trim(),
                        clave = formulario.clave
                    )
                )

                when (resultado) {

                    is ResultadoRegistro.Exito -> {
                        _estado.value = RegistroUiState()
                        onRegistroExitoso()
                    }

                    is ResultadoRegistro.Error -> {
                        val mensaje = resultado.mensaje

                        val erroresRepositorio = when {

                            mensaje.contains(
                                "RUT",
                                ignoreCase = true
                            ) -> ErroresRegistro(
                                rut = mensaje
                            )

                            mensaje.contains(
                                "alias", ignoreCase = true
                            ) && !mensaje.contains(
                                "correo", ignoreCase = true
                            ) -> ErroresRegistro(alias = mensaje)

                            mensaje.contains(
                                "correo", ignoreCase = true
                            ) && !mensaje.contains(
                                "alias", ignoreCase = true
                            ) -> ErroresRegistro(correo = mensaje)

                            else -> ErroresRegistro(
                                general = mensaje
                            )
                        }

                        _estado.update {
                            it.copy(errores = erroresRepositorio)
                        }
                    }
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _estado.update {
                    it.copy(
                        errores = ErroresRegistro(
                            general = if (complementos != null) e.mensajeApi() else "No se pudo guardar la cuenta. Intenta nuevamente."
                        )
                    )
                }

            } finally {
                _estado.update {
                    it.copy(procesando = false)
                }
            }
        }
    }
}
