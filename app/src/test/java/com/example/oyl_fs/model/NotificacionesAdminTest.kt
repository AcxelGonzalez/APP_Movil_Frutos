package com.example.oyl_fs.model

import org.junit.Assert.*
import org.junit.Test

class NotificacionesAdminTest {
    private fun producto(stock: Int, minimo: Int = 5) = ProductoInfo(
        1, "Almendras", "OYL001", "Frutos secos", "Descripción", "500 g",
        6990, stock, minimo, true
    )
    private fun pedido(estado: EstadoPedido, fecha: Long = 1L) =
        Pedido("PED-1", "cliente-1", fecha, emptyList(), 6990, estado)

    @Test fun sinDatosNoSeInventanAvisos() {
        assertTrue(crearNotificacionesAdmin(emptyList(), emptyList()).isEmpty())
    }
    @Test fun stockEnElMinimoGeneraAlertaYStockSuficienteNo() {
        assertEquals(1, crearNotificacionesAdmin(emptyList(), listOf(producto(5))).size)
        assertTrue(crearNotificacionesAdmin(emptyList(), listOf(producto(6))).isEmpty())
    }
    @Test fun agotadoTieneTituloPropioYDestinoDeInventario() {
        val aviso = crearNotificacionesAdmin(emptyList(), listOf(producto(0))).single()
        assertEquals("Producto agotado", aviso.titulo)
        assertEquals(DestinoNotificacion.INVENTARIO, aviso.destino)
    }
    @Test fun soloPedidosPendientesGeneranAviso() {
        EstadoPedido.values().forEach { estado ->
            assertEquals(if (estado == EstadoPedido.PENDIENTE_VALIDACION) 1 else 0,
                crearNotificacionesAdmin(listOf(pedido(estado)), emptyList()).size)
        }
    }
    @Test fun cambiosDeStockProducenUnNuevoAvisoSinRepetirElAnterior() {
        val anterior = crearNotificacionesAdmin(emptyList(), listOf(producto(4))).single()
        val nuevo = crearNotificacionesAdmin(emptyList(), listOf(producto(3))).single()
        assertNotEquals(anterior.id, nuevo.id)
        assertEquals(anterior.id,
            crearNotificacionesAdmin(emptyList(), listOf(producto(4))).single().id)
    }
    @Test fun cuandoSeResuelveLaCondicionDesapareceLaAlerta() {
        assertTrue(crearNotificacionesAdmin(listOf(pedido(EstadoPedido.CONFIRMADO)),
            listOf(producto(10))).isEmpty())
    }
}
