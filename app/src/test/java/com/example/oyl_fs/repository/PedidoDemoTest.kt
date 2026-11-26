package com.example.oyl_fs.repository

import com.example.oyl_fs.viewmodel.ProductoViewModel
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprueba que los pedidos ficticios utilicen
 * productos existentes del catálogo.
 */
class PedidoDemoTest {

    @Test
    fun pedidosDemoCoincidenConCatalogo() {

        val productos = ProductoViewModel()
            .productos.value
            .associateBy { it.id.toString() }

        val pedidos = PedidoRepository().pedidos.value

        assertEquals(4, pedidos.size)

        pedidos.forEach { pedido ->

            assertTrue(pedido.productos.isNotEmpty())

            pedido.productos.forEach { detalle ->

                val producto = productos[detalle.productoId]

                assertNotNull(
                    "No existe el producto ${detalle.productoId}",
                    producto
                )

                assertEquals(
                    producto!!.nombre,
                    detalle.nombreProducto
                )

                assertEquals(
                    producto.precio,
                    detalle.precioUnitario
                )
            }

            // El total debe coincidir con la suma
            // de los subtotales de sus productos.
            assertEquals(
                pedido.productos.sumOf { it.subtotal },
                pedido.total
            )
        }
    }
}
