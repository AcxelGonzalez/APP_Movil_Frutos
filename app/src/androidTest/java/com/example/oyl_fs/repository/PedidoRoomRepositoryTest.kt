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


    @Test
    fun compraYStockSeGuardanEnUnaSolaTransaccion() =
        runBlocking {

            val contexto = InstrumentationRegistry
                .getInstrumentation()
                .targetContext

            val nombreBase = "oyl_compra_atomica_prueba.db"

            contexto.deleteDatabase(nombreBase)

            fun crearBase(): OylDatabase {
                return Room.databaseBuilder(
                    contexto,
                    OylDatabase::class.java,
                    nombreBase
                )
                    .addCallback(
                        OylDatabase.crearCallbackInicializacion(
                            contexto
                        )
                    )
                    .build()
            }

            var base = crearBase()

            try {

                var repo = PedidoRoomRepository(base)
                var productosDao = base.productoDao()

                val almendras = productosDao.obtenerPorId(1)!!
                val nueces = productosDao.obtenerPorId(2)!!

                fun crearPedido(
                    id: String,
                    cantidadAlmendras: Int,
                    cantidadNueces: Int
                ): Pedido {

                    val detalles = listOf(
                        DetallePedido(
                            productoId = "1",
                            nombreProducto = almendras.nombre,
                            cantidad = cantidadAlmendras,
                            precioUnitario = almendras.precio
                        ),
                        DetallePedido(
                            productoId = "2",
                            nombreProducto = nueces.nombre,
                            cantidad = cantidadNueces,
                            precioUnitario = nueces.precio
                        )
                    )

                    val total =
                        cantidadAlmendras * almendras.precio +
                                cantidadNueces * nueces.precio

                    return Pedido(
                        id = id,
                        usuarioId = "cliente-prueba",
                        fecha = System.currentTimeMillis(),
                        productos = detalles,
                        total = total,
                        estado = EstadoPedido.PENDIENTE_VALIDACION
                    )
                }

                // CASO 1:
                // Comprar dos almendras y tres nueces.
                val compraValida = crearPedido(
                    id = "PED-COMPRA-001",
                    cantidadAlmendras = 2,
                    cantidadNueces = 3
                )

                assertTrue(
                    repo.registrarCompraConStock(compraValida)
                )

                assertEquals(1, repo.contarPedidos())

                assertNotNull(
                    repo.obtenerPedidoPorId("PED-COMPRA-001")
                )

                assertEquals(
                    almendras.stock - 2,
                    productosDao.obtenerPorId(1)?.stock
                )

                assertEquals(
                    nueces.stock - 3,
                    productosDao.obtenerPorId(2)?.stock
                )

                // CASO 2:
                // La segunda compra solicita más nueces
                // de las disponibles.
                val compraSinStock = crearPedido(
                    id = "PED-COMPRA-002",
                    cantidadAlmendras = 1,
                    cantidadNueces = nueces.stock + 100
                )

                assertFalse(
                    repo.registrarCompraConStock(compraSinStock)
                )

                // No se debe guardar el pedido fallido.
                assertNull(
                    repo.obtenerPedidoPorId("PED-COMPRA-002")
                )

                assertEquals(1, repo.contarPedidos())

                // Tampoco debe descontarse parcialmente
                // el inventario.
                assertEquals(
                    almendras.stock - 2,
                    productosDao.obtenerPorId(1)?.stock
                )

                assertEquals(
                    nueces.stock - 3,
                    productosDao.obtenerPorId(2)?.stock
                )

                // CASO 3:
                // Reutilizar el ID de un pedido existente.
                // SQLite debe rechazarlo sin alterar stock.
                val compraDuplicada = crearPedido(
                    id = "PED-COMPRA-001",
                    cantidadAlmendras = 1,
                    cantidadNueces = 1
                )

                val errorDuplicado = runCatching {
                    repo.registrarCompraConStock(
                        compraDuplicada
                    )
                }.exceptionOrNull()

                assertNotNull(errorDuplicado)

                assertEquals(1, repo.contarPedidos())

                assertEquals(
                    almendras.stock - 2,
                    productosDao.obtenerPorId(1)?.stock
                )

                // CASO 4:
                // Cerrar y reabrir la misma base SQLite.
                base.close()

                base = crearBase()
                repo = PedidoRoomRepository(base)
                productosDao = base.productoDao()

                // Pedido y stock deben persistir.
                assertNotNull(
                    repo.obtenerPedidoPorId("PED-COMPRA-001")
                )

                assertEquals(
                    almendras.stock - 2,
                    productosDao.obtenerPorId(1)?.stock
                )

                assertEquals(
                    nueces.stock - 3,
                    productosDao.obtenerPorId(2)?.stock
                )

            } finally {

                base.close()
                contexto.deleteDatabase(nombreBase)
            }
        }

}
