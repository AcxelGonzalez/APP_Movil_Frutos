package com.example.oyl_fs.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.model.DetallePedido
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.Pedido
import com.example.oyl_fs.repository.PedidoRoomRepository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistorialRoomViewModelTest {

    @Test
    fun historialObservaPedidosDeRoom() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val nombreBase = "oyl_historial_vm_prueba.db"

        contexto.deleteDatabase(nombreBase)

        val base = Room.databaseBuilder(
            contexto,
            OylDatabase::class.java,
            nombreBase
        ).build()

        val store = ViewModelStore()

        try {

            val repo = PedidoRoomRepository(base)

            val vm = ViewModelProvider(
                store,
                object : ViewModelProvider.Factory {

                    override fun <T : ViewModel> create(
                        modelClass: Class<T>
                    ): T {

                        require(
                            modelClass.isAssignableFrom(
                                HistorialViewModel::class.java
                            )
                        )

                        @Suppress("UNCHECKED_CAST")
                        return HistorialViewModel(repo) as T
                    }
                }
            )[HistorialViewModel::class.java]

            // Registrar un pedido de prueba en SQLite.
            val pedido = Pedido(
                id = "PED-HISTORIAL-TEST",
                usuarioId = "cliente-prueba",
                fecha = System.currentTimeMillis(),
                productos = listOf(
                    DetallePedido(
                        productoId = "1",
                        nombreProducto = "Almendras",
                        cantidad = 1,
                        precioUnitario = 6990
                    )
                ),
                total = 6990,
                estado = EstadoPedido.PENDIENTE_VALIDACION
            )

            repo.guardarPedido(pedido)

            // El ViewModel debe observar el pedido.
            val listaAdmin = withTimeout(10000L) {
                vm.pedidosParaAdmin.first { lista ->
                    lista.any { it.id == pedido.id }
                }
            }

            assertEquals(1, listaAdmin.size)

            // Configurar el historial del cliente.
            vm.configurarSesion(
                usuarioId = "cliente-prueba",
                esAdmin = false
            )

            val visibles = withTimeout(10000L) {
                vm.pedidosVisibles.first { lista ->
                    lista.any { it.id == pedido.id }
                }
            }

            assertEquals(1, visibles.size)

            // El último pedido debe estar disponible.
            assertEquals(
                pedido.id,
                vm.observarUltimoPedidoCliente(
                    "cliente-prueba"
                ).first()?.id
            )

            // Un cliente no puede cambiar estados.
            assertFalse(
                vm.cambiarEstadoPedidoPersistente(
                    pedido.id,
                    EstadoPedido.CONFIRMADO
                )
            )

            // El administrador sí puede.
            vm.configurarSesion(
                usuarioId = "admin-demo",
                esAdmin = true
            )

            assertTrue(
                vm.cambiarEstadoPedidoPersistente(
                    pedido.id,
                    EstadoPedido.CONFIRMADO
                )
            )

            val actualizado = withTimeout(10000L) {
                vm.pedidosParaAdmin.first { lista ->
                    lista.any {
                        it.id == pedido.id &&
                                it.estado == EstadoPedido.CONFIRMADO
                    }
                }
            }

            assertEquals(
                EstadoPedido.CONFIRMADO,
                actualizado.first().estado
            )

        } finally {

            // Detener la observación del ViewModel
            // antes de cerrar la base de pruebas.
            store.clear()
            base.close()
            contexto.deleteDatabase(nombreBase)
        }
    }
}
