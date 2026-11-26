package com.example.oyl_fs.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cancelaciones_locales", primaryKeys = ["actorId", "pedidoId"])
data class CancelacionLocalEntity(
    val actorId: String,
    val pedidoId: Long,
    val propietarioId: String,
    val contenido: String,
    val estado: String,
    val mensaje: String?
)

@Dao
interface CancelacionLocalDao {
    @Query("SELECT * FROM cancelaciones_locales WHERE actorId = :actorId ORDER BY pedidoId DESC")
    fun observar(actorId: String): Flow<List<CancelacionLocalEntity>>
    @Query("SELECT * FROM cancelaciones_locales WHERE actorId = :actorId AND pedidoId = :pedidoId")
    suspend fun obtener(actorId: String, pedidoId: Long): CancelacionLocalEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(cancelacion: CancelacionLocalEntity)
    @Query("UPDATE cancelaciones_locales SET estado = :estado, mensaje = :mensaje WHERE actorId = :actorId AND pedidoId = :pedidoId")
    suspend fun actualizar(actorId: String, pedidoId: Long, estado: String, mensaje: String?)
    @Query("DELETE FROM cancelaciones_locales WHERE actorId = :actorId AND pedidoId = :pedidoId AND estado IN ('COMPLETADA', 'RECHAZADA')")
    suspend fun cerrar(actorId: String, pedidoId: Long)
}
