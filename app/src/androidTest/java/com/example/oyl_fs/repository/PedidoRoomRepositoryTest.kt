package com.example.oyl_fs.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.oyl_fs.data.local.OylDatabase

import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que los pedidos se almacenen
 * correctamente en SQLite.
 */
@RunWith(AndroidJUnit4::class)
class PedidoRoomRepositoryTest {

    @Test
    fun guardarYRecuperarPedidoPersistente() =
        runBlocking {

            val contexto = InstrumentationRegistry
                .getInstrumentation()
                .targetContext

            val nombreBase =
                "oyl_pedidos_repositorio_prueba.db"

            contexto.deleteDatabase(nombreBase)

            // Base exclusiva para esta prueba.
            fun crearBase(): OylDatabase {
                return Room.databaseBuilder(
                    contexto,
                    OylDatabase::class.java,
                    nombreBase
                ).build()
            }

            var base = crearBase()

            try {

                val repositorio =
                    PedidoRoomRepository(base)

                val pedido = Pedido(
                    id = "PED-PRUEBA-001",
                    usuarioId = "cliente-prueba",
                    fecha = 1700000000000L,

                    productos = listOf(
                        DetallePedido(
                            productoId = "1",
                            nombreProducto = "Almendras",
                            cantidad = 2,
                            precioUnitario = 6990
                        )
                    ),

                    total = 13980,
                    estado =
                        EstadoPedido.PENDIENTE_VALIDACION
                )

                // 1. Guardar pedido y detalles.
                repositorio.guardarPedido(pedido)

                assertEquals(
                    1,
                    repositorio.contarPedidos()
                )

                // 2. Recuperar su información.
                val recuperado =
                    repositorio.obtenerPedidoPorId(
                        "PED-PRUEBA-001"
                    )

                assertEquals(
                    pedido,
                    recuperado
                )

                // 3. Comprobar filtro por cliente.
                assertEquals(
                    1,
                    repositorio
                        .observarPedidosUsuario(
                            "cliente-prueba"
                        )
                        .first()
                        .size
                )

                assertTrue(
                    repositorio
                        .observarPedidosUsuario(
                            "otro-cliente"
                        )
                        .first()
                        .isEmpty()
                )

                // 4. Actualizar el estado.
                assertTrue(
                    repositorio.actualizarEstadoPedido(
                        "PED-PRUEBA-001",
                        EstadoPedido.CONFIRMADO
                    )
                )

                // 5. Cerrar y abrir nuevamente.
                base.close()
                base = crearBase()

                val repositorioRecuperado =
                    PedidoRoomRepository(base)

                // El pedido debe continuar guardado.
                val pedidoPersistente =
                    repositorioRecuperado
                        .obtenerPedidoPorId(
                            "PED-PRUEBA-001"
                        )

                assertNotNull(pedidoPersistente)

                assertEquals(
                    EstadoPedido.CONFIRMADO,
                    pedidoPersistente?.estado
                )

                assertEquals(
                    1,
                    pedidoPersistente?.productos?.size
                )

                assertEquals(
                    13980,
                    pedidoPersistente?.total
                )

            } finally {

                base.close()

                // Solo elimina la base de pruebas.
                contexto.deleteDatabase(nombreBase)
            }
        }
}
