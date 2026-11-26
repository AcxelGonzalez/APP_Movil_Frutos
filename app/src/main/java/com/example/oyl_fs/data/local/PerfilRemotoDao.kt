package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PerfilRemotoDao {
    @Query("SELECT * FROM perfiles_remotos WHERE usuarioId = :usuarioId")
    suspend fun obtener(usuarioId: Long): PerfilRemotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(perfil: PerfilRemotoEntity)
}
