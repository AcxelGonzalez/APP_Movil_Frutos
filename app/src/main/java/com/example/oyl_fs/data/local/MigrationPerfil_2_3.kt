package com.example.oyl_fs.data.local
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
val MIGRATION_PERFIL_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS intentos_compra (usuarioId TEXT NOT NULL, clave TEXT NOT NULL, contenido TEXT NOT NULL, PRIMARY KEY(usuarioId))")
    }
}
