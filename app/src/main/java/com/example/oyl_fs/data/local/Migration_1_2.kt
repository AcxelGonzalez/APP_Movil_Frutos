package com.example.oyl_fs.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración de la base de datos de versión 1 a 2.
 *
 * Versión 1:
 * - productos
 *
 * Versión 2:
 * - productos
 * - pedidos
 * - detalle_pedidos
 *
 * No modifica ni elimina los productos existentes.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        // 1. Crear tabla principal de pedidos.
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pedidos` (
                `id` TEXT NOT NULL,
                `usuarioId` TEXT NOT NULL,
                `fecha` INTEGER NOT NULL,
                `total` INTEGER NOT NULL,
                `estado` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // 2. Índices para consultas de pedidos.
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_pedidos_usuarioId`
            ON `pedidos` (`usuarioId`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_pedidos_fecha`
            ON `pedidos` (`fecha`)
            """.trimIndent()
        )

        // 3. Crear tabla de detalles.
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `detalle_pedidos` (
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

        // 4. Índice para relacionar los detalles
        // con sus pedidos.
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_detalle_pedidos_pedidoId`
            ON `detalle_pedidos` (`pedidoId`)
            """.trimIndent()
        )
    }
}
