package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.*
import org.junit.Assert.*
import org.junit.Test

class ValidacionEntregaPedidoTest {
    private val puntos = listOf(PuntoRetiro("T01", "Tienda prueba", true, "Calle prueba 100", "10–18",
        "Referencia", -33.0, -70.0))
    private val retiro = EntregaPedido(ModalidadEntrega.RETIRO, "Cliente prueba", "+56 9 1234 5678", puntoRetiroId = "T01")

    @Test fun retiroRequiereContactoYPuntoExistente() {
        assertFalse(ValidacionEntregaPedido.validar(retiro, puntos).hayErrores())
        val errores = ValidacionEntregaPedido.validar(retiro.copy(nombreContacto = " ", telefonoContacto = "", puntoRetiroId = "otro"), puntos)
        assertNotNull(errores.nombre)
        assertNotNull(errores.telefono)
        assertNotNull(errores.puntoRetiro)
        assertNotNull(ValidacionEntregaPedido.validar(retiro.copy(telefonoContacto = "abc12345678"), puntos).telefono)
    }

    @Test fun domicilioRequiereDireccionPeroNoPuntoDeRetiro() {
        val domicilio = retiro.copy(modalidad = ModalidadEntrega.DOMICILIO, puntoRetiroId = "", direccion = "Calle ficticia 200")
        assertFalse(ValidacionEntregaPedido.validar(domicilio, puntos).hayErrores())
        assertNotNull(ValidacionEntregaPedido.validar(domicilio.copy(direccion = " "), puntos).direccion)
    }

    @Test fun retiroUsaDireccionYNombreOficialesDelPunto() {
        val normalizado = ValidacionEntregaPedido.normalizar(retiro.copy(nombreContacto = " Cliente prueba ",
            direccion = "Dirección falsa enviada", puntoRetiroNombre = "Nombre enviado"), puntos)
        assertEquals("Cliente prueba", normalizado.nombreContacto)
        assertEquals("Tienda prueba", normalizado.puntoRetiroNombre)
        assertEquals("Calle prueba 100", normalizado.direccion)
    }

    @Test fun domicilioDescartaElPuntoSeleccionadoAntesDeCambiarModalidad() {
        val normalizado = ValidacionEntregaPedido.normalizar(retiro.copy(modalidad = ModalidadEntrega.DOMICILIO,
            direccion = " Calle ficticia 200 ", puntoRetiroNombre = "Tienda prueba"), puntos)
        assertEquals("", normalizado.puntoRetiroId)
        assertEquals("", normalizado.puntoRetiroNombre)
        assertEquals("Calle ficticia 200", normalizado.direccion)
    }
}
