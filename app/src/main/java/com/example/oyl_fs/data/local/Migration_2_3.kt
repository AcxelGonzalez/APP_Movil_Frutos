package com.example.oyl_fs.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migración SQLite de la versión 2 a la 3.
 *
 * V2:
 * - productos
 * - pedidos
 * - detalle_pedidos
 *
 * V3:
 * - productos
 * - pedidos
 * - detalle_pedidos
 * - usuarios
 *
 * Conserva los datos existentes.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        // 1. Crear la tabla de clientes.
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `usuarios` (
                `id` TEXT NOT NULL,
                `alias` TEXT NOT NULL,
                `aliasNormalizado` TEXT NOT NULL,
                `nombre` TEXT NOT NULL,
                `apellido` TEXT NOT NULL,
                `rut` TEXT NOT NULL,
                `correo` TEXT NOT NULL,
                `correoNormalizado` TEXT NOT NULL,
                `telefono` TEXT NOT NULL,
                `direccion` TEXT NOT NULL,
                `claveHash` TEXT NOT NULL,
                `claveSalt` TEXT NOT NULL,
                `creadoEn` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // 2. Impedir alias duplicados.
        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_usuarios_aliasNormalizado`
            ON `usuarios` (`aliasNormalizado`)
            """.trimIndent()
        )

        // 3. Impedir correos duplicados.
        database.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_usuarios_correoNormalizado`
            ON `usuarios` (`correoNormalizado`)
            """.trimIndent()
        )
    }
}
