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
 * Prueba la migración Room v2 -> v3.
 *
 * Comprueba:
 * - Conservación del catálogo.
 * - Conservación de pedidos y detalles.
 * - Funcionamiento de UsuarioDao.
 * - Restricción de alias duplicados.
 * - Persistencia al reabrir SQLite.
 */
@RunWith(AndroidJUnit4::class)
class UsuarioMigrationTest {

    @Test
    fun migracionV2aV3ConservaDatos() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val nombreBase = "oyl_migracion_usuarios_test.db"

        contexto.deleteDatabase(nombreBase)

        // Preparar una base con el esquema v2.
        val antigua = contexto.openOrCreateDatabase(
            nombreBase,
            Context.MODE_PRIVATE,
            null
        )

        try {

            antigua.execSQL(
                """
                CREATE TABLE `productos` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `nombre` TEXT NOT NULL,
                    `codigo` TEXT NOT NULL,
                    `categoria` TEXT NOT NULL,
                    `descripcion` TEXT NOT NULL,
                    `presentacion` TEXT NOT NULL,
                    `precio` INTEGER NOT NULL,
                    `stock` INTEGER NOT NULL,
                    `stockMinimo` INTEGER NOT NULL,
                    `disponible` INTEGER NOT NULL,
                    `imagenDrawableNombre` TEXT,
                    `imagenUri` TEXT
                )
                """.trimIndent()
            )

            antigua.execSQL(
                """
                CREATE UNIQUE INDEX
                `index_productos_codigo`
                ON `productos` (`codigo`)
                """.trimIndent()
            )

            antigua.execSQL(
                """
                CREATE TABLE `pedidos` (
                    `id` TEXT NOT NULL,
                    `usuarioId` TEXT NOT NULL,
                    `fecha` INTEGER NOT NULL,
                    `total` INTEGER NOT NULL,
                    `estado` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            antigua.execSQL(
                """
                CREATE INDEX `index_pedidos_usuarioId`
                ON `pedidos` (`usuarioId`)
                """.trimIndent()
            )

            antigua.execSQL(
                """
                CREATE INDEX `index_pedidos_fecha`
                ON `pedidos` (`fecha`)
                """.trimIndent()
            )

            antigua.execSQL(
                """
                CREATE TABLE `detalle_pedidos` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `pedidoId` TEXT NOT NULL,
                    `productoId` TEXT NOT NULL,
                    `nombreProducto` TEXT NOT NULL,
                    `cantidad` INTEGER NOT NULL,
                    `precioUnitario` INTEGER NOT NULL,
                    FOREIGN KEY(`pedidoId`)
                        REFERENCES `pedidos`(`id`)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            antigua.execSQL(
                """
                CREATE INDEX
                `index_detalle_pedidos_pedidoId`
                ON `detalle_pedidos` (`pedidoId`)
                """.trimIndent()
            )

            // Información existente antes de migrar.
            antigua.execSQL(
                """
                INSERT INTO `productos`
                (`id`, `nombre`, `codigo`, `categoria`,
                 `descripcion`, `presentacion`, `precio`,
                 `stock`, `stockMinimo`, `disponible`,
                 `imagenDrawableNombre`, `imagenUri`)
                VALUES
                (42, 'Almendras prueba', 'TEST-042',
                 'Frutos secos', 'Producto anterior',
                 'Bolsa', 6990, 37, 2, 1, NULL, NULL)
                """.trimIndent()
            )

            antigua.execSQL(
                """
                INSERT INTO `pedidos`
                (`id`, `usuarioId`, `fecha`, `total`, `estado`)
                VALUES
                ('PED-V2', 'cliente-demo',
                 1700000000000, 6990, 'CONFIRMADO')
                """.trimIndent()
            )

            antigua.execSQL(
                """
                INSERT INTO `detalle_pedidos`
                (`pedidoId`, `productoId`,
                 `nombreProducto`, `cantidad`,
                 `precioUnitario`)
                VALUES
                ('PED-V2', '42', 'Almendras prueba',
                 1, 6990)
                """.trimIndent()
            )

            antigua.version = 2

        } finally {
            antigua.close()
        }

        fun abrirVersion3(): OylDatabase {
            return Room.databaseBuilder(
                contexto,
                OylDatabase::class.java,
                nombreBase
            )
                .addMigrations(MIGRATION_2_3)
                .build()
        }

        var nueva = abrirVersion3()

        try {

            // 1. Se conserva el inventario.
            val producto = nueva.productoDao()
                .obtenerPorId(42)

            assertNotNull(producto)
            assertEquals(37, producto?.stock)
            assertEquals("TEST-042", producto?.codigo)

            // 2. Se conserva el historial.
            val pedido = nueva.pedidoDao()
                .obtenerPedidoPorId("PED-V2")

            assertNotNull(pedido)
            assertEquals("CONFIRMADO", pedido?.pedido?.estado)
            assertEquals(1, pedido?.detalles?.size)

            // 3. La tabla usuarios comienza vacía.
            val usuarios = nueva.usuarioDao()

            assertEquals(0, usuarios.contarUsuarios())

            // 4. Insertar un cliente ficticio.
            val usuario = UsuarioEntity(
                id = "USR-TEST-001",
                alias = "ClientePrueba",
                aliasNormalizado = "clienteprueba",
                nombre = "Ana",
                apellido = "Prueba",
                rut = "RUT-FICTICIO",
                correo = "ana@example.com",
                correoNormalizado = "ana@example.com",
                telefono = "000000000",
                direccion = "Dirección de prueba",
                claveHash = "HASH-FICTICIO",
                claveSalt = "SALT-FICTICIA",
                creadoEn = 1700000000000L
            )

            usuarios.insertar(usuario)

            assertEquals(1, usuarios.contarUsuarios())

            assertEquals(
                usuario.id,
                usuarios.obtenerPorAlias(
                    "clienteprueba"
                )?.id
            )

            // 5. No se permiten alias duplicados.
            val duplicado = usuario.copy(
                id = "USR-TEST-002",
                correo = "otro@example.com",
                correoNormalizado = "otro@example.com"
            )

            val error = runCatching {
                usuarios.insertar(duplicado)
            }.exceptionOrNull()

            assertNotNull(error)
            assertEquals(1, usuarios.contarUsuarios())

            // 6. Cerrar y abrir nuevamente.
            nueva.close()
            nueva = abrirVersion3()

            assertNotNull(
                nueva.pedidoDao()
                    .obtenerPedidoPorId("PED-V2")
            )

            assertEquals(
                37,
                nueva.productoDao()
                    .obtenerPorId(42)?.stock
            )

            assertEquals(
                "ClientePrueba",
                nueva.usuarioDao()
                    .obtenerPorId("USR-TEST-001")
                    ?.alias
            )

        } finally {

            nueva.close()

            // Eliminar solamente la base de prueba.
            contexto.deleteDatabase(nombreBase)
        }
    }
}
