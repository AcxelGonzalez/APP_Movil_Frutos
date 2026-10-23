package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

import kotlinx.coroutines.flow.Flow

/**
 * DAO encargado de las operaciones
 * de usuarios almacenados en SQLite.
 *
 * Permite registrar, consultar y
 * actualizar clientes locales.
 */
@Dao
interface UsuarioDao {

    /**
     * Inserta un nuevo cliente.
     *
     * ABORT impide reemplazar usuarios
     * existentes cuando hay conflictos.
     */
    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertar(
        usuario: UsuarioEntity
    )

    /**
     * Busca un cliente mediante su ID.
     *
     * Se utilizará para recuperar el perfil
     * correspondiente a una sesión.
     */
    @Query(
        """
        SELECT * FROM usuarios
        WHERE id = :usuarioId
        LIMIT 1
        """
    )
    suspend fun obtenerPorId(
        usuarioId: String
    ): UsuarioEntity?

    /**
     * Busca un cliente por su alias
     * previamente normalizado.
     *
     * Se utilizará durante el Login.
     */
    @Query(
        """
        SELECT * FROM usuarios
        WHERE aliasNormalizado = :aliasNormalizado
        LIMIT 1
        """
    )
    suspend fun obtenerPorAlias(
        aliasNormalizado: String
    ): UsuarioEntity?

    /**
     * Comprueba si un alias ya existe.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM usuarios
            WHERE aliasNormalizado = :aliasNormalizado
        )
        """
    )
    suspend fun existeAlias(
        aliasNormalizado: String
    ): Boolean

    /**
     * Comprueba si un correo ya existe.
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM usuarios
            WHERE correoNormalizado = :correoNormalizado
        )
        """
    )
    suspend fun existeCorreo(
        correoNormalizado: String
    ): Boolean

    /**
     * Observa los datos de un usuario.
     *
     * Flow permite que Perfil se actualice
     * cuando cambie la información local.
     */
    @Query(
        """
        SELECT * FROM usuarios
        WHERE id = :usuarioId
        LIMIT 1
        """
    )
    fun observarUsuario(
        usuarioId: String
    ): Flow<UsuarioEntity?>

    /**
     * Actualiza la información del usuario.
     *
     * Devuelve 1 si fue actualizado
     * o 0 si el usuario no existe.
     */
    @Update
    suspend fun actualizar(
        usuario: UsuarioEntity
    ): Int

    /**
     * Devuelve la cantidad de clientes
     * registrados en SQLite.
     *
     * Se utilizará en las pruebas.
     */
    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun contarUsuarios(): Int
}
