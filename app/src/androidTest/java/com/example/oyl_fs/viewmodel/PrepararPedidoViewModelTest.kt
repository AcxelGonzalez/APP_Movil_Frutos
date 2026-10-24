
package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.util.textoQrProducto

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas de la lógica de preparación QR.
 *
 * No requieren cámara, emulador ni SQLite.
 */
class PrepararPedidoViewModelTest {

    private fun crearPedido(
        id: String = "PED-QR-TEST",
        estado: EstadoPedido = EstadoPedido.CONFIRMADO,
        detalles: List<DetallePedido> = listOf(
            DetallePedido("1", "Almendras", 2, 6990)
        )
    ): Pedido {

        return Pedido(
            id = id,
            usuarioId = "cliente-prueba",
            fecha = 1700000000000L,
            productos = detalles,
            // El total no interviene en la verificación QR.
            total = 0,
            estado = estado
        )
    }

    @Test
    fun agrupaProductosRepetidosCorrectamente() {

        val vm = PrepararPedidoViewModel()

        val pedido = crearPedido(
            detalles = listOf(
                DetallePedido("1", "Almendras", 2, 6990),
                DetallePedido("1", "Almendras", 3, 6990),
                DetallePedido("2", "Nueces", 1, 5990)
            )
        )

        vm.cargarPedido(pedido)

        val productos = vm.productosPreparacion.value

        assertEquals(2, productos.size)

        assertEquals(
            5,
            productos.first {
                it.productoId == "1"
            }.cantidad
        )

        assertEquals(
            1,
            productos.first {
                it.productoId == "2"
            }.cantidad
        )
    }

    @Test
    fun exigeTodasLasUnidadesAgrupadas() {

        val vm = PrepararPedidoViewModel()

        vm.cargarPedido(
            crearPedido(
                detalles = listOf(
                    DetallePedido("1", "Almendras", 2, 6990),
                    DetallePedido("1", "Almendras", 3, 6990),
                    DetallePedido("2", "Nueces", 1, 5990)
                )
            )
        )

        repeat(4) {
            vm.validarQr(textoQrProducto(1))
        }

        assertFalse(vm.pedidoCompleto())

        vm.validarQr(textoQrProducto(1))

        assertEquals(
            5,
            vm.cantidadesEscaneadas.value["1"]
        )

        // Aún falta verificar la nuez.
        assertFalse(vm.pedidoCompleto())

        vm.validarQr(textoQrProducto(2))

        assertTrue(vm.pedidoCompleto())
    }

    @Test
    fun noPermiteEscanearMasUnidades() {

        val vm = PrepararPedidoViewModel()

        vm.cargarPedido(crearPedido())

        repeat(3) {
            vm.validarQr(textoQrProducto(1))
        }

        // Solamente se requerían dos unidades.
        assertEquals(
            2,
            vm.cantidadesEscaneadas.value["1"]
        )

        assertTrue(vm.pedidoCompleto())

        assertTrue(
            vm.mensaje.value.contains(
                "Ya se verificaron"
            )
        )
    }

    @Test
    fun rechazaCodigosInvalidosYProductosAjenos() {

        val vm = PrepararPedidoViewModel()

        vm.cargarPedido(crearPedido())

        vm.validarQr("CODIGO-INVALIDO")

        assertTrue(
            vm.cantidadesEscaneadas.value.isEmpty()
        )

        assertTrue(
            vm.mensaje.value.contains("no es válido")
        )

        vm.validarQr(textoQrProducto(99))

        assertTrue(
            vm.cantidadesEscaneadas.value.isEmpty()
        )

        assertTrue(
            vm.mensaje.value.contains(
                "no pertenece"
            )
        )
    }

    @Test
    fun pedidosNoConfirmadosNoSePreparan() {

        val estados = listOf(
            EstadoPedido.PENDIENTE_VALIDACION,
            EstadoPedido.PREPARADO,
            EstadoPedido.ENTREGADO
        )

        estados.forEach { estado ->

            val vm = PrepararPedidoViewModel()

            vm.cargarPedido(
                crearPedido(estado = estado)
            )

            vm.validarQr(textoQrProducto(1))

            assertTrue(
                vm.cantidadesEscaneadas.value.isEmpty()
            )

            assertFalse(vm.pedidoCompleto())
        }
    }

    @Test
    fun cambiarDePedidoReiniciaLasVerificaciones() {

        val vm = PrepararPedidoViewModel()

        vm.cargarPedido(
            crearPedido(id = "PED-UNO")
        )

        vm.validarQr(textoQrProducto(1))

        assertEquals(
            1,
            vm.cantidadesEscaneadas.value["1"]
        )

        vm.cargarPedido(
            crearPedido(id = "PED-DOS")
        )

        assertTrue(
            vm.cantidadesEscaneadas.value.isEmpty()
        )

        assertFalse(vm.pedidoCompleto())
    }

    @Test
    fun cambioDeEstadoInvalidaPreparacionAnterior() {

        val vm = PrepararPedidoViewModel()

        val pedido = crearPedido()

        vm.cargarPedido(pedido)

        repeat(2) {
            vm.validarQr(textoQrProducto(1))
        }

        assertTrue(vm.pedidoCompleto())

        // Simular el nuevo estado recibido de Room.
        vm.cargarPedido(
            pedido.copy(
                estado = EstadoPedido.PREPARADO
            )
        )

        assertFalse(vm.pedidoCompleto())

        assertTrue(
            vm.cantidadesEscaneadas.value.isEmpty()
        )
    }

    @Test
    fun cantidadesInvalidasNoPermitenPreparar() {

        val vm = PrepararPedidoViewModel()

        vm.cargarPedido(
            crearPedido(
                detalles = listOf(
                    DetallePedido(
                        "1", "Almendras", 0, 6990
                    )
                )
            )
        )

        assertTrue(
            vm.productosPreparacion.value.isEmpty()
        )

        assertFalse(vm.pedidoCompleto())
    }

    @Test
    fun erroresDelEscanerSeMuestran() {

        val vm = PrepararPedidoViewModel()

        vm.informarFalloEscaneo(
            "Escaneo cancelado."
        )

        assertEquals(
            "Escaneo cancelado.",
            vm.mensaje.value
        )
    }
}
