package com.example.oyl_fs.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS compras_aplicadas (clave TEXT NOT NULL, usuarioId TEXT NOT NULL, PRIMARY KEY(clave))")
    }
}
