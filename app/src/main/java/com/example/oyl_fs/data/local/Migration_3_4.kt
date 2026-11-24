package com.example.oyl_fs.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `usuarios` ADD COLUMN `perfilPreferenciasMigrado` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `carrito` (
                `usuarioId` TEXT NOT NULL,
                `productoId` INTEGER NOT NULL,
                `nombre` TEXT NOT NULL,
                `formato` TEXT NOT NULL,
                `precio` INTEGER NOT NULL,
                `cantidad` INTEGER NOT NULL,
                `imagenDrawableNombre` TEXT,
                `imagenUri` TEXT,
                PRIMARY KEY(`usuarioId`, `productoId`)
            )
        """.trimIndent())
    }
}
