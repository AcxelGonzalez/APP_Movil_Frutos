package com.example.oyl_fs.model

import com.example.oyl_fs.util.ValidacionContacto

enum class ModalidadEntrega(val descripcion: String) {
    RETIRO("Retiro en tienda o punto de retiro"),
    DOMICILIO("Entrega a domicilio")
}

// Una copia de los datos acordados al comprar, independiente del perfil futuro.
data class EntregaPedido(
    val modalidad: ModalidadEntrega,
    val nombreContacto: String,
    val telefonoContacto: String,
    val direccion: String = "",
    val puntoRetiroId: String = "",
    val puntoRetiroNombre: String = ""
)

data class ErroresEntrega(
    val nombre: String? = null,
    val telefono: String? = null,
    val direccion: String? = null,
    val puntoRetiro: String? = null
) {
    fun hayErrores() = listOfNotNull(nombre, telefono, direccion, puntoRetiro).isNotEmpty()
}

object ValidacionEntregaPedido {
    fun validar(datos: EntregaPedido, puntos: List<PuntoRetiro>) = ErroresEntrega(
        nombre = if (datos.nombreContacto.isBlank()) "Ingresa el nombre de contacto" else null,
        telefono = if (datos.telefonoContacto.isBlank() || !ValidacionContacto.telefonoValido(datos.telefonoContacto))
            "Ingresa un teléfono válido de 8 a 11 dígitos" else null,
        direccion = if (datos.modalidad == ModalidadEntrega.DOMICILIO && datos.direccion.isBlank())
            "Ingresa la dirección de entrega" else null,
        puntoRetiro = if (datos.modalidad == ModalidadEntrega.RETIRO && puntos.none { it.id == datos.puntoRetiroId })
            "Selecciona un punto de retiro válido" else null
    )

    fun normalizar(datos: EntregaPedido, puntos: List<PuntoRetiro>): EntregaPedido {
        require(!validar(datos, puntos).hayErrores())
        val punto = puntos.find { it.id == datos.puntoRetiroId }
        return datos.copy(
            nombreContacto = datos.nombreContacto.trim(), telefonoContacto = datos.telefonoContacto.trim(),
            direccion = if (datos.modalidad == ModalidadEntrega.RETIRO) punto!!.direccion else datos.direccion.trim(),
            puntoRetiroId = if (datos.modalidad == ModalidadEntrega.RETIRO) punto!!.id else "",
            puntoRetiroNombre = if (datos.modalidad == ModalidadEntrega.RETIRO) punto!!.nombre else ""
        )
    }
}
