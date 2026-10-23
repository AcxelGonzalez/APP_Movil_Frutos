package com.example.oyl_fs.repository

import android.content.Context

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.model.EstadoPedido

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica que los pedidos de demostración
 * se almacenen en Room una única vez,
 * sin modificar el inventario.
 */
@RunWith(AndroidJUnit4::class)
class PedidoDemoSeedTest {

    @Test
    fun pedidosDemoSeGuardanSinDescontarStock() =
        runBlocking {

            val contexto = InstrumentationRegistry
                .getInstrumentation()
                .targetContext

            val nombreBase =
                "oyl_pedidos_demo_seed_test.db"

            val nombrePreferencias =
                "oyl_demo_$nombreBase"

            // Limpiar solamente los datos
            // utilizados por esta prueba.
            contexto.deleteDatabase(nombreBase)

            contexto.getSharedPreferences(
                nombrePreferencias,
                Context.MODE_PRIVATE
            ).edit().clear().commit()

            fun crearBase(): OylDatabase {
                return Room.databaseBuilder(
                    contexto,
                    OylDatabase::class.java,
                    nombreBase
                )
                    .addCallback(
                        OylDatabase
                            .crearCallbackInicializacion(
                                contexto
                            )
                    )
                    .build()
            }

            var base = crearBase()

            try {

                var repositorio =
                    PedidoRoomRepository(base)

                val stockInicial = base.productoDao()
                    .obtenerPorId(1)!!.stock

                // Primera inicialización.
                repositorio
                    .inicializarPedidosDemoUnaVez(contexto)

                assertEquals(
                    4,
                    repositorio.contarPedidos()
                )

                // Verificar el pedido utilizado por QR.
                val pedidoQr =
                    repositorio.obtenerPedidoPorId(
                        "PED-001"
                    )

                assertNotNull(pedidoQr)

                assertEquals(
                    EstadoPedido.CONFIRMADO,
                    pedidoQr?.estado
                )

                assertEquals(
                    2,
                    pedidoQr?.productos?.size
                )

                // No se debe descontar inventario.
                assertEquals(
                    stockInicial,
                    base.productoDao()
                        .obtenerPorId(1)?.stock
                )

                // Segunda inicialización:
                // no debe duplicar los pedidos.
                repositorio
                    .inicializarPedidosDemoUnaVez(contexto)

                assertEquals(
                    4,
                    repositorio.contarPedidos()
                )

                // Cerrar y reabrir SQLite.
                base.close()
                base = crearBase()

                repositorio = PedidoRoomRepository(base)

                repositorio
                    .inicializarPedidosDemoUnaVez(contexto)

                // Los pedidos continúan disponibles.
                assertEquals(
                    4,
                    repositorio.contarPedidos()
                )

                assertNotNull(
                    repositorio.obtenerPedidoPorId(
                        "PED-001"
                    )
                )

                // Las existencias siguen intactas.
                assertEquals(
                    stockInicial,
                    base.productoDao()
                        .obtenerPorId(1)?.stock
                )

            } finally {

                base.close()
                contexto.deleteDatabase(nombreBase)

                contexto.getSharedPreferences(
                    nombrePreferencias,
                    Context.MODE_PRIVATE
                ).edit().clear().commit()
            }
        }
}
