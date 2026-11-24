package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.oyl_fs.model.PerfilUsuario

/** Copia local del perfil: no contiene credenciales ni modifica la cuenta del servidor. */
@Entity(tableName = "perfiles_remotos")
data class PerfilRemotoEntity(
    @PrimaryKey val usuarioId: Long,
    val esAdmin: Boolean = false,
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val rut: String = "",
    val apellido: String = ""
) {
    fun aPerfil() = PerfilUsuario(usuarioId.toString(), esAdmin, nombre, apellido, rut, correo, telefono, direccion)
}
