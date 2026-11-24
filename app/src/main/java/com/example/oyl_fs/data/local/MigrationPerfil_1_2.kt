package com.example.oyl_fs.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_PERFIL_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `avatares_perfil` (
                `perfilClave` TEXT NOT NULL, `archivo` TEXT NOT NULL, PRIMARY KEY(`perfilClave`)
            )
        """.trimIndent())
    }
}
