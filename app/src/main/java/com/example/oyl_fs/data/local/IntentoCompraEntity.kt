package com.example.oyl_fs.data.local
import androidx.room.*

@Entity(tableName = "intentos_compra")
data class IntentoCompraEntity(@PrimaryKey val usuarioId: String, val clave: String, val contenido: String)

@Dao
interface IntentoCompraDao {
    @Query("SELECT * FROM intentos_compra WHERE usuarioId = :usuarioId")
    suspend fun obtener(usuarioId: String): IntentoCompraEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardar(intento: IntentoCompraEntity)
    @Query("DELETE FROM intentos_compra WHERE usuarioId = :usuarioId AND clave = :clave")
    suspend fun quitar(usuarioId: String, clave: String)
}
