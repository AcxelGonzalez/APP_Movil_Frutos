package com.example.oyl_fs.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AvatarPerfilDao {
    @Query("SELECT archivo FROM avatares_perfil WHERE perfilClave = :clave")
    suspend fun obtener(clave: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(avatar: AvatarPerfilEntity)

    @Query("DELETE FROM avatares_perfil WHERE perfilClave = :clave")
    suspend fun quitar(clave: String)
}
