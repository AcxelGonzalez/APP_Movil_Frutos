package com.example.oyl_fs.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_PERFIL_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS cancelaciones_locales (
                actorId TEXT NOT NULL, pedidoId INTEGER NOT NULL, propietarioId TEXT NOT NULL,
                contenido TEXT NOT NULL, estado TEXT NOT NULL, mensaje TEXT,
                PRIMARY KEY(actorId, pedidoId)
            )
        """.trimIndent())
    }
}
