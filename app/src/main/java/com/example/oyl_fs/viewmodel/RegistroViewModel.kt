package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.oyl_fs.repository.DatosRegistroCliente
import com.example.oyl_fs.repository.ResultadoRegistro
import com.example.oyl_fs.repository.UsuarioRoomRepository

import java.util.Locale

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

        val digitosTelefono = datos.telefono
            .filter { it.isDigit() }

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
            } else null,

            correo = if (!correoValido) {
                "Ingresa un correo electrónico válido."
            } else null,

            rut = if (!rutValido(datos.rut)) {
                "Ingresa un RUT válido, por ejemplo 12.345.678-5."
            } else null,

            telefono = if (
                datos.telefono.isNotBlank() &&
                digitosTelefono.length !in 8..11
            ) {
                "Ingresa un teléfono válido."
            } else null,

            clave = if (datos.clave.length !in 8..128) {
                "La contraseña debe tener entre 8 y 128 caracteres."
            } else null,

            confirmacion = if (
                datos.confirmacion != datos.clave
            ) {
                "Las contraseñas no coinciden."
            } else null
        )
    }

    /**
     * Comprueba el dígito verificador del RUT chileno.
     * Admite formatos con y sin puntos o guion.
     */
    private fun rutValido(rut: String): Boolean {

        val limpio = rut.trim()
            .replace(".", "")
            .replace("-", "")
            .uppercase(Locale.ROOT)

        if (!Regex("^\\d{7,8}[\\dK]$").matches(limpio)) {
            return false
        }

        val numero = limpio.dropLast(1)
        val verificador = limpio.last()

        var suma = 0
        var factor = 2

        for (digito in numero.reversed()) {
            suma += digito.digitToInt() * factor
            factor = if (factor == 7) 2 else factor + 1
        }

        val resto = 11 - (suma % 11)

        val esperado = when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> resto.digitToChar()
        }

        return verificador == esperado
    }
}

/**
 * Gestiona la validación y el registro real
 * de clientes mediante UsuarioRoomRepository.
 */
class RegistroViewModel(
    private val repository: UsuarioRoomRepository
) : ViewModel() {

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
     * de confirmar el almacenamiento en SQLite.
     */
    fun registrar(
        formulario: FormularioRegistro,
        onRegistroExitoso: () -> Unit
    ) {

        if (_estado.value.procesando) return

        val errores = ValidacionRegistro.validar(formulario)

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

            } catch (_: Exception) {
                _estado.update {
                    it.copy(
                        errores = ErroresRegistro(
                            general = "No se pudo guardar la cuenta. Intenta nuevamente."
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
