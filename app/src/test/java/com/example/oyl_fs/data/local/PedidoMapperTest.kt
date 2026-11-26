package com.example.oyl_fs.data.local

import com.example.oyl_fs.model.*
import org.junit.Assert.*
import org.junit.Test

class PedidoMapperTest {
    private val pedido = Pedido("PED-021", "cliente-a", 1700000000000, listOf(
        DetallePedido("3", "Almendras", 2, 1000), DetallePedido("8", "Semillas", 1, 500)
    ), 2500, EstadoPedido.CONFIRMADO)

    @Test fun conservaIdentidadImportesDetalleYEntregaEnAmbasModalidades() {
        val entregas = listOf(
            EntregaPedido(ModalidadEntrega.DOMICILIO, "Contacto", "912345678", "Calle ficticia 100"),
            EntregaPedido(ModalidadEntrega.RETIRO, "Contacto", "912345678", "Local ficticio", "R01", "Retiro ficticio")
        )
        for (entrega in entregas) {
            val original = pedido.copy(entrega = entrega)
            val detalles = original.aEntidadesDetalle()
            assertTrue(detalles.all { it.pedidoId == original.id })
            assertEquals(original, PedidoConDetalles(original.aEntidad(), detalles).aModeloPedido())
        }
    }

    @Test fun pedidosAntiguosSinEntregaNoInventanContactoNiDomicilio() {
        assertNull(pedido.aEntidad().modalidadEntrega)
        assertEquals(pedido, PedidoConDetalles(pedido.aEntidad(), pedido.aEntidadesDetalle()).aModeloPedido())
        val antigua = pedido.aEntidad().copy(modalidadEntrega = "DOMICILIO")
        val entrega = requireNotNull(PedidoConDetalles(antigua, emptyList()).aModeloPedido().entrega)
        assertEquals("", entrega.nombreContacto)
        assertEquals("", entrega.telefonoContacto)
        assertEquals("", entrega.direccion)
        assertEquals("", entrega.puntoRetiroId)
        assertEquals("", entrega.puntoRetiroNombre)
    }

    @Test fun estadosYModalidadesDesconocidasNoSeConviertenEnUnPedidoValido() {
        for (entidad in listOf(pedido.aEntidad().copy(estado = "INVENTADO"),
            pedido.aEntidad().copy(modalidadEntrega = "INVENTADA"))) {
            try { PedidoConDetalles(entidad, emptyList()).aModeloPedido(); fail("Debe rechazar el valor desconocido") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun copiaLocalDelPerfilConservaSuPropietarioRolYTodosLosCampos() {
        val local = PerfilRemotoEntity(9, true, "Administrador", "admin@example.test", "912345678",
            "Local ficticio", "12.345.678-5", "Apellido")
        assertEquals(PerfilUsuario("9", true, "Administrador", "Apellido", "12.345.678-5",
            "admin@example.test", "912345678", "Local ficticio"), local.aPerfil())
    }
}
