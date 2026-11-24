package com.example.oyl_fs.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_PERFIL_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS movimientos_stock_locales (
                actorId TEXT NOT NULL PRIMARY KEY, clave TEXT NOT NULL,
                productoId INTEGER NOT NULL, cambio INTEGER NOT NULL, contenido TEXT NOT NULL,
                estado TEXT NOT NULL, stockAnterior INTEGER, stockResultante INTEGER, mensaje TEXT
            )
        """.trimIndent())
    }
}
