package com.example.oyl_fs.data.local

import androidx.room.*

// Un resultado por cuenta hasta que se cierre explícitamente; no descartar incertidumbre.
@Entity(tableName = "movimientos_stock_locales")
data class MovimientoStockLocalEntity(
    @PrimaryKey val actorId: String,
    val clave: String, val productoId: Int, val cambio: Int, val contenido: String,
    val estado: String, val stockAnterior: Int?, val stockResultante: Int?, val mensaje: String?
)
@Dao
interface MovimientoStockLocalDao {
    @Query("SELECT * FROM movimientos_stock_locales WHERE actorId = :actorId")
    suspend fun obtener(actorId: String): MovimientoStockLocalEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(fila: MovimientoStockLocalEntity)
    @Query("UPDATE movimientos_stock_locales SET estado=:estado, stockAnterior=:antes, stockResultante=:despues, mensaje=:mensaje WHERE actorId=:actorId AND clave=:clave")
    suspend fun actualizar(actorId: String, clave: String, estado: String, antes: Int?, despues: Int?, mensaje: String?)
    @Query("DELETE FROM movimientos_stock_locales WHERE actorId=:actorId AND clave=:clave AND estado IN ('APLICADA','RECHAZADA')")
    suspend fun cerrar(actorId: String, clave: String)
}
