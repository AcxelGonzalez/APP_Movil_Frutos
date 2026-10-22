package com.example.oyl_fs.data.local

import android.content.Context

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba la migración de SQLite v1 a v2
 * conservando los productos existentes.
 */
@RunWith(AndroidJUnit4::class)
class MigrationRoomTest {

    @Test
    fun migracionConservaProductos() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val nombreBase = "oyl_migracion_prueba.db"

        contexto.deleteDatabase(nombreBase)

        // Simular una base con el esquema v1.
        val baseAntigua = contexto.openOrCreateDatabase(
            nombreBase,
            Context.MODE_PRIVATE,
            null
        )

        try {

            baseAntigua.execSQL(
                """
                CREATE TABLE IF NOT EXISTS productos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nombre TEXT NOT NULL,
                    codigo TEXT NOT NULL,
                    categoria TEXT NOT NULL,
                    descripcion TEXT NOT NULL,
                    presentacion TEXT NOT NULL,
                    precio INTEGER NOT NULL,
                    stock INTEGER NOT NULL,
                    stockMinimo INTEGER NOT NULL,
                    disponible INTEGER NOT NULL,
                    imagenDrawableNombre TEXT,
                    imagenUri TEXT
                )
                """.trimIndent()
            )

            baseAntigua.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_productos_codigo
                ON productos(codigo)
                """.trimIndent()
            )

            // Producto de prueba que debe conservarse.
            baseAntigua.execSQL(
                """
                INSERT INTO productos (
                    id, nombre, codigo, categoria,
                    descripcion, presentacion,
                    precio, stock, stockMinimo,
                    disponible, imagenDrawableNombre,
                    imagenUri
                ) VALUES (
                    42, 'Almendras prueba', 'TEST-042',
                    'Frutos secos', 'Producto de prueba',
                    'Bolsa', 6990, 37, 2, 1,
                    NULL, NULL
                )
                """.trimIndent()
            )

            baseAntigua.version = 1

        } finally {
            baseAntigua.close()
        }

        // Abrir la misma base usando Room v2.
        val baseNueva = Room.databaseBuilder(
            contexto,
            OylDatabase::class.java,
            nombreBase
        )
            .addMigrations(MIGRATION_1_2)
            .build()

        try {

            // 1. El producto anterior debe conservarse.
            val producto = baseNueva
                .productoDao()
                .obtenerPorId(42)

            assertNotNull(producto)
            assertEquals("TEST-042", producto?.codigo)
            assertEquals(37, producto?.stock)

            // 2. Verificar que existe la tabla pedidos.
            assertEquals(
                0,
                baseNueva.pedidoDao().contarPedidos()
            )

            // 3. Insertar un pedido de prueba.
            val pedido = PedidoEntity(
                id = "PED-PRUEBA",
                usuarioId = "cliente-prueba",
                fecha = System.currentTimeMillis(),
                total = 6990,
                estado = "PENDIENTE_VALIDACION"
            )

            baseNueva.pedidoDao().insertarPedido(pedido)

            // 4. Insertar su detalle.
            baseNueva.pedidoDao().insertarDetalles(
                listOf(
                    DetallePedidoEntity(
                        pedidoId = "PED-PRUEBA",
                        productoId = "42",
                        nombreProducto = "Almendras prueba",
                        cantidad = 1,
                        precioUnitario = 6990
                    )
                )
            )

            // 5. Recuperar pedido y detalles.
            val recuperado = baseNueva.pedidoDao()
                .obtenerPedidoPorId("PED-PRUEBA")

            assertNotNull(recuperado)
            assertEquals(1, recuperado?.detalles?.size)
            assertEquals(6990, recuperado?.pedido?.total)

            // 6. Confirmar que el inventario no cambió.
            assertEquals(
                37,
                baseNueva.productoDao()
                    .obtenerPorId(42)?.stock
            )

        } finally {
            baseNueva.close()
            contexto.deleteDatabase(nombreBase)
        }
    }
}
