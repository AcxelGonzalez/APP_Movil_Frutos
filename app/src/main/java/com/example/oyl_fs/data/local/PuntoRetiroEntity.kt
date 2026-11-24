package com.example.oyl_fs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.oyl_fs.model.PuntoRetiro

@Entity(tableName = "puntos_retiro")
data class PuntoRetiroEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val esTienda: Boolean,
    val direccion: String,
    val horario: String,
    val indicaciones: String,
    val latitud: Double,
    val longitud: Double,
    val orden: Int
)

fun PuntoRetiroEntity.aPuntoRetiro() = PuntoRetiro(
    id, nombre, esTienda, direccion, horario, indicaciones, latitud, longitud
)
