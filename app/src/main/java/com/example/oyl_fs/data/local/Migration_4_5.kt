package com.example.oyl_fs.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // NULL identifica pedidos antiguos sin inventar datos de contacto o envío.
        listOf("modalidadEntrega", "nombreContacto", "telefonoContacto", "direccionEntrega",
            "puntoRetiroId", "puntoRetiroNombre").forEach { columna ->
            database.execSQL("ALTER TABLE `pedidos` ADD COLUMN `$columna` TEXT")
        }
    }
}
