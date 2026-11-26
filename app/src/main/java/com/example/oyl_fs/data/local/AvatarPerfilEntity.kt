package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** La clave distingue cuentas locales y remotas; archivo es un nombre privado, no una ruta externa. */
@Entity(tableName = "avatares_perfil")
data class AvatarPerfilEntity(@PrimaryKey val perfilClave: String, val archivo: String)
