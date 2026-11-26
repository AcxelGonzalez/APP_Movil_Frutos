package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CarritoDao {
    @Query("SELECT * FROM carrito WHERE usuarioId = :usuarioId ORDER BY productoId")
    fun observar(usuarioId: String): Flow<List<CarritoEntity>>

    @Query("SELECT * FROM carrito WHERE usuarioId = :usuarioId AND productoId = :productoId")
    suspend fun obtener(usuarioId: String, productoId: Int): CarritoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(item: CarritoEntity)

    @Query("DELETE FROM carrito WHERE usuarioId = :usuarioId AND productoId = :productoId")
    suspend fun quitar(usuarioId: String, productoId: Int)

    @Query("UPDATE carrito SET cantidad = cantidad - :cantidad WHERE usuarioId = :usuarioId AND productoId = :productoId")
    suspend fun descontarComprados(usuarioId: String, productoId: Int, cantidad: Int)

    @Query("DELETE FROM carrito WHERE usuarioId = :usuarioId AND cantidad <= 0")
    suspend fun quitarVacios(usuarioId: String)
}
