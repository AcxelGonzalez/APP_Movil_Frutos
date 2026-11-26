package com.example.oyl_fs.data.local
import androidx.room.*

/** Marcador confirmado en la misma transacción que la limpieza del carrito. */
@Entity(tableName = "compras_aplicadas")
data class CompraAplicadaEntity(@PrimaryKey val clave: String, val usuarioId: String)
@Dao
interface CompraAplicadaDao {
    @Query("SELECT EXISTS(SELECT 1 FROM compras_aplicadas WHERE clave = :clave AND usuarioId = :usuarioId)")
    suspend fun existe(clave: String, usuarioId: String): Boolean
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardar(compra: CompraAplicadaEntity)
}
