package com.example.oyl_fs.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local compartida de
 * OYL Frutos Secos.
 *
 * Comenzamos con productos.
 * Posteriormente agregaremos las entidades
 * de pedidos y detalles de pedidos.
 */
@Database(
    entities = [
        ProductoEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OylDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    companion object {

        @Volatile
        private var INSTANCE: OylDatabase? = null

        /**
         * Obtiene una única instancia de la base
         * durante la ejecución de la aplicación.
         */
        fun obtener(context: Context): OylDatabase {

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    OylDatabase::class.java,
                    "oyl_frutos_secos.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
