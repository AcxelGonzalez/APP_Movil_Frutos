package com.example.oyl_fs.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** Separada de OylDatabase para mantener los perfiles remotos fuera de backup y transferencia. */
@Database(entities = [PerfilRemotoEntity::class, AvatarPerfilEntity::class, IntentoCompraEntity::class, CancelacionLocalEntity::class, MovimientoStockLocalEntity::class], version = 5, exportSchema = false)
abstract class PerfilRemotoDatabase : RoomDatabase() {
    abstract fun movimientoStockDao(): MovimientoStockLocalDao
    abstract fun cancelacionDao(): CancelacionLocalDao
    abstract fun perfilDao(): PerfilRemotoDao
    abstract fun intentoCompraDao(): IntentoCompraDao
    abstract fun avatarDao(): AvatarPerfilDao

    companion object {
        @Volatile private var instancia: PerfilRemotoDatabase? = null

        fun obtener(contexto: Context): PerfilRemotoDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                contexto.applicationContext, PerfilRemotoDatabase::class.java, "oyl_perfiles_remotos.db"
            ).addMigrations(MIGRATION_PERFIL_1_2, MIGRATION_PERFIL_2_3, MIGRATION_PERFIL_3_4, MIGRATION_PERFIL_4_5).build().also { instancia = it }
        }
    }
}
