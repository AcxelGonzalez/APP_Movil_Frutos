package com.example.oyl_fs.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `puntos_retiro` (
                `id` TEXT NOT NULL, `nombre` TEXT NOT NULL, `esTienda` INTEGER NOT NULL,
                `direccion` TEXT NOT NULL, `horario` TEXT NOT NULL, `indicaciones` TEXT NOT NULL,
                `latitud` REAL NOT NULL, `longitud` REAL NOT NULL, `orden` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        PuntosRetiroIniciales.entidades.forEach { punto ->
            database.insert("puntos_retiro", SQLiteDatabase.CONFLICT_IGNORE, ContentValues().apply {
                put("id", punto.id); put("nombre", punto.nombre); put("esTienda", if (punto.esTienda) 1 else 0)
                put("direccion", punto.direccion); put("horario", punto.horario); put("indicaciones", punto.indicaciones)
                put("latitud", punto.latitud); put("longitud", punto.longitud); put("orden", punto.orden)
            })
        }
    }
}
