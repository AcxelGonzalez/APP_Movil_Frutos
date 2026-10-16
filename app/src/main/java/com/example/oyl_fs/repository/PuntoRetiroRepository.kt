package com.example.oyl_fs.repository

import com.example.oyl_fs.model.PuntoRetiro

object PuntoRetiroRepository {
    private val puntos = listOf(
        PuntoRetiro(
            id = "T01",
            nombre = "OYL Frutos Secos - Tienda principal",
            esTienda = true,
            direccion = "Paseo Ahumada 100, Santiago Centro (referencial)",
            horario = "Lun a Sáb 10:00 - 19:00",
            indicaciones = "A una cuadra de la Plaza de Armas, local 5",
            latitud = -33.4380,
            longitud = -70.6505
        ),
        PuntoRetiro(
            id = "R01",
            nombre = "Punto de retiro Providencia",
            esTienda = false,
            direccion = "Av. Providencia 2100, Providencia (referencial)",
            horario = "Lun a Vie 11:00 - 18:00",
            indicaciones = "Cerca del Metro Los Leones, consultar en recepción",
            latitud = -33.4220,
            longitud = -70.6080
        ),
        PuntoRetiro(
            id = "R02",
            nombre = "Punto de retiro Ñuñoa",
            esTienda = false,
            direccion = "Av. Irarrázaval 3400, Ñuñoa (referencial)",
            horario = "Mar a Sáb 10:00 - 17:00",
            indicaciones = "Frente a la Plaza Ñuñoa",
            latitud = -33.4566,
            longitud = -70.5978
        ),
        PuntoRetiro(
            id = "R03",
            nombre = "Punto de retiro Maipú",
            esTienda = false,
            direccion = "Av. Pajaritos 1800, Maipú (referencial)",
            horario = "Lun a Vie 10:00 - 18:00",
            indicaciones = "Cerca de la Plaza de Maipú, local 12",
            latitud = -33.5106,
            longitud = -70.7572
        )
    )

    fun obtenerPuntos(): List<PuntoRetiro> = puntos

    fun buscarPorId(id: String): PuntoRetiro? = puntos.find { it.id == id }
}