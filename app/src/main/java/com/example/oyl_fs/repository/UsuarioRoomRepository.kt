package com.example.oyl_fs.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException

import com.example.oyl_fs.data.local.HashContrasena
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.UsuarioEntity

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
    database: OylDatabase
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

        if (datos.clave.length !in 8..128) {
            return ResultadoRegistro.Error(
                "La contraseña debe tener entre 8 y 128 caracteres."
            )
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

            dao.insertar(usuario)

            ResultadoRegistro.Exito(
                usuarioId = usuario.id
            )

        } catch (_: SQLiteConstraintException) {

            // Protege también frente a dos registros
            // simultáneos con el mismo alias o correo.
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
