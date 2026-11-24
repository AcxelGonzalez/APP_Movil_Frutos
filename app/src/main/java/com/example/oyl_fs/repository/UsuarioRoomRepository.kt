package com.example.oyl_fs.repository

import androidx.room.withTransaction
import android.content.Context
import android.database.sqlite.SQLiteConstraintException

import com.example.oyl_fs.data.local.HashContrasena
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.UsuarioEntity
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.model.ErroresPerfil
import com.example.oyl_fs.model.ValidacionPerfil
import com.example.oyl_fs.util.ValidacionContacto

import java.util.Locale
import java.util.UUID

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Datos necesarios para registrar un cliente.
 *
 * El formulario enviará este modelo
 * al repositorio en la próxima etapa.
 */
data class DatosRegistroCliente(
    val alias: String,
    val nombre: String,
    val correo: String,
    val clave: String,
    val apellido: String = "",
    val rut: String = "",
    val telefono: String = "",
    val direccion: String = ""
)

/**
 * Resultado controlado del registro.
 */
sealed class ResultadoRegistro {

    data class Exito(
        val usuarioId: String
    ) : ResultadoRegistro()

    data class Error(
        val mensaje: String
    ) : ResultadoRegistro()
}

/**
 * Datos mínimos entregados después
 * de autenticar correctamente.
 *
 * No contiene hashes ni contraseñas.
 */
data class ClienteAutenticado(
    val id: String,
    val alias: String,
    val nombre: String
)

/**
 * Repositorio de cuentas de clientes.
 *
 * Utiliza Room para conservar usuarios.
 * El administrador continúa utilizando
 * la cuenta de demostración preconfigurada.
 */
class UsuarioRoomRepository(
    private val database: OylDatabase
) {

    constructor(context: Context) : this(
        OylDatabase.obtener(context.applicationContext)
    )

    private val dao = database.usuarioDao()

    /**
     * Registra un cliente con ID único.
     *
     * Valida datos básicos, normaliza alias
     * y correo y protege la contraseña.
     */
    suspend fun registrarCliente(
        datos: DatosRegistroCliente
    ): ResultadoRegistro {

        val alias = datos.alias.trim()
        val nombre = datos.nombre.trim()
        val correo = datos.correo.trim()

        val aliasNormalizado = alias.lowercase(Locale.ROOT)
        val correoNormalizado = correo.lowercase(Locale.ROOT)

        // Representación utilizada para comparar RUT.
        //
        // 12.345.678-5 -> 123456785
        // 12345678-5   -> 123456785
        val rutNormalizado = datos.rut
            .trim()
            .replace(".", "")
            .replace("-", "")
            .replace(" ", "")
            .uppercase(Locale.ROOT)

        // Alias permitido: letras, números,
        // puntos, guiones y guion bajo.
        val patronAlias = Regex("^[a-z0-9._-]{3,24}$")

        if (!patronAlias.matches(aliasNormalizado)) {
            return ResultadoRegistro.Error(
                "El alias debe tener entre 3 y 24 caracteres y no contener espacios."
            )
        }

        // Reservar las cuentas de demostración.
        if (
            aliasNormalizado in setOf(
                "admin",
                "cliente",
                "cliente-demo"
            )
        ) {
            return ResultadoRegistro.Error(
                "Este alias está reservado."
            )
        }

        if (nombre.isBlank()) {
            return ResultadoRegistro.Error(
                "Debes ingresar tu nombre."
            )
        }

        val patronCorreo = Regex(
            "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )

        if (!patronCorreo.matches(correoNormalizado)) {
            return ResultadoRegistro.Error(
                "Ingresa un correo electrónico válido."
            )
        }

        com.example.oyl_fs.util.ValidacionContrasena.errorRegistro(datos.clave)?.let {
            return ResultadoRegistro.Error(it)
        }

        // Validaciones previas para entregar
        // mensajes claros al usuario.
        if (dao.existeAlias(aliasNormalizado)) {
            return ResultadoRegistro.Error(
                "El alias ya está registrado."
            )
        }

        if (dao.existeCorreo(correoNormalizado)) {
            return ResultadoRegistro.Error(
                "El correo ya está registrado."
            )
        }

        /**
         * Impide utilizar el mismo RUT en
         * cuentas de cliente diferentes.
         *
         * Se omite la comprobación cuando está
         * vacío para conservar compatibilidad
         * con las pruebas existentes del repositorio.
         *
         * El formulario visual ya exige un RUT
         * válido y no vacío.
         */
        if (
            rutNormalizado.isNotEmpty() &&
            dao.existeRutNormalizado(rutNormalizado)
        ) {
            return ResultadoRegistro.Error(
                "El RUT ya está registrado."
            )
        }

        // PBKDF2 es costoso; ejecutarlo fuera
        // del hilo principal y del despachador IO.
        val credencial = withContext(Dispatchers.Default) {
            HashContrasena.crear(datos.clave)
        }

        val usuario = UsuarioEntity(
            id = UUID.randomUUID().toString(),
            alias = alias,
            aliasNormalizado = aliasNormalizado,
            nombre = nombre,
            apellido = datos.apellido.trim(),
            rut = datos.rut.trim(),
            correo = correo,
            correoNormalizado = correoNormalizado,
            telefono = datos.telefono.trim(),
            direccion = datos.direccion.trim(),
            claveHash = credencial.hash,
            claveSalt = credencial.salt,
            creadoEn = System.currentTimeMillis()
        )

        return try {

            // La comprobación final y la inserción
            // se realizan en una misma transacción.
            //
            // Evita que dos registros simultáneos
            // mediante este repositorio pasen la
            // comprobación con el mismo RUT.
            val insertado = database.withTransaction {

                val rutOcupado =
                    rutNormalizado.isNotEmpty() &&
                            dao.existeRutNormalizado(
                                rutNormalizado
                            )

                if (rutOcupado) {

                    false

                } else {

                    dao.insertar(usuario)
                    true
                }
            }

            if (insertado) {

                ResultadoRegistro.Exito(
                    usuarioId = usuario.id
                )

            } else {

                ResultadoRegistro.Error(
                    "El RUT ya está registrado."
                )
            }

        } catch (_: SQLiteConstraintException) {

            ResultadoRegistro.Error(
                "El alias o correo ya está registrado."
            )
        }
    }

    /**
     * Comprueba las credenciales de un cliente.
     *
     * Devuelve null si el alias no existe
     * o la contraseña no coincide.
     */
    suspend fun autenticarCliente(
        alias: String,
        clave: String
    ): ClienteAutenticado? {

        val aliasNormalizado =
            alias.trim().lowercase(Locale.ROOT)

        if (aliasNormalizado.isBlank() || clave.isEmpty()) {
            return null
        }

        val usuario = dao.obtenerPorAlias(
            aliasNormalizado
        ) ?: return null

        val coincide = withContext(Dispatchers.Default) {
            HashContrasena.verificar(
                clave = clave,
                hashAlmacenado = usuario.claveHash,
                saltAlmacenado = usuario.claveSalt
            )
        }

        if (!coincide) {
            return null
        }

        return ClienteAutenticado(
            id = usuario.id,
            alias = usuario.alias,
            nombre = usuario.nombre
        )
    }

    /** Actualiza la misma cuenta que utiliza el login, sin modificar sus credenciales. */
    suspend fun actualizarPerfil(perfil: PerfilUsuario): ErroresPerfil? {
        val errores = ValidacionPerfil.validar(perfil)
        if (errores.primerError() != null) return errores
        if (perfil.esAdmin) return ErroresPerfil(general = "El perfil no es válido")
        return try {
            database.withTransaction {
                val anterior = dao.obtenerPorId(perfil.id)
                    ?: return@withTransaction ErroresPerfil(general = "La cuenta ya no existe")
                val correo = perfil.correo.trim()
                val correoNormalizado = correo.lowercase(Locale.ROOT)
                if (correoNormalizado != anterior.correoNormalizado && dao.existeCorreo(correoNormalizado)) {
                    return@withTransaction ErroresPerfil(correo = "El correo ya está registrado")
                }
                val rut = ValidacionContacto.normalizarRut(perfil.rut)
                val rutAnterior = ValidacionContacto.normalizarRut(anterior.rut)
                if (rut.isNotEmpty() && rut != rutAnterior && dao.existeRutNormalizado(rut)) {
                    return@withTransaction ErroresPerfil(rut = "El RUT ya está registrado")
                }
                dao.actualizar(anterior.copy(
                    nombre = perfil.nombre.trim(), apellido = perfil.apellido.trim(),
                    rut = perfil.rut.trim(), correo = correo, correoNormalizado = correoNormalizado,
                    telefono = perfil.telefono.trim(), direccion = perfil.direccion.trim(),
                    perfilPreferenciasMigrado = true
                ))
                null
            }
        } catch (_: SQLiteConstraintException) {
            ErroresPerfil(correo = "El correo ya está registrado")
        }
    }

    /**
     * Recupera los datos de un usuario
     * mediante el ID de su futura sesión.
     */
    suspend fun obtenerUsuario(
        usuarioId: String
    ): UsuarioEntity? {

        if (usuarioId.isBlank()) return null

        return dao.obtenerPorId(usuarioId)
    }
}
