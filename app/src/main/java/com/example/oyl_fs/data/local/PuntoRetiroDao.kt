package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PuntoRetiroDao {
    @Query("SELECT * FROM puntos_retiro ORDER BY orden ASC, id ASC")
    fun observar(): Flow<List<PuntoRetiroEntity>>

    @Query("SELECT * FROM puntos_retiro ORDER BY orden ASC, id ASC")
    suspend fun listar(): List<PuntoRetiroEntity>

    @Query("SELECT COUNT(*) FROM puntos_retiro")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarIniciales(puntos: List<PuntoRetiroEntity>)

    @Transaction
    suspend fun inicializarSiVacio(puntos: List<PuntoRetiroEntity>) {
        if (contar() == 0) insertarIniciales(puntos)
    }
}
