package com.example.oyl_fs.model

data class PuntoRetiro(
    val id: String,
    val nombre: String,
    val esTienda: Boolean,      // true = tienda principal, false = punto de retiro
    val direccion: String,
    val horario: String,        // ej: "Lun a Vie 10:00 - 19:00"
    val indicaciones: String,   // ej: "Frente a la plaza, local 12"
    val latitud: Double,        // norte/sur  (Santiago ≈ -33.4)
    val longitud: Double        // este/oeste (Santiago ≈ -70.6)
)