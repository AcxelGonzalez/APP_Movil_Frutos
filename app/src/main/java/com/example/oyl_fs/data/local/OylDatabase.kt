package com.example.oyl_fs.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

import com.example.oyl_fs.screens.ProductosDatos

/**
 * Base de datos local OYL Frutos Secos.
 *
 * Versión 3:
 * - productos
 * - pedidos
 * - detalle_pedidos
 * - usuarios
 */
@Database(
    entities = [
        ProductoEntity::class,
        PedidoEntity::class,
        DetallePedidoEntity::class,
        UsuarioEntity::class
    ],
    version = 3,
    exportSchema = false
)

abstract class OylDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    /**
     * Acceso a las operaciones de pedidos.
     */
    abstract fun pedidoDao(): PedidoDao

    /**
     * Operaciones de usuarios locales.
     */
    abstract fun usuarioDao(): UsuarioDao

    companion object {

        @Volatile
        private var INSTANCE: OylDatabase? = null

        /**
         * Devuelve una única instancia de la base
         * para toda la aplicación.
         */
        fun obtener(context: Context): OylDatabase {

            val contexto = context.applicationContext

            return INSTANCE ?: synchronized(this) {

                INSTANCE ?: Room.databaseBuilder(
                    contexto,
                    OylDatabase::class.java,
                    "oyl_frutos_secos.db"
                )

                    // Permite actualizar instalaciones antiguas:
                    //
                    // v1 -> v2 -> v3
                    // v2 -> v3
                    //
                    // No elimina registros anteriores.
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3
                    )

                    .addCallback(
                        crearCallbackInicializacion(contexto)
                    )

                    .build()
                    .also {
                        INSTANCE = it
                    }
            }
        }

        /**
         * Prepara la carga inicial de productos.
         *
         * Se utiliza tanto en la base principal
         * como en las pruebas instrumentadas.
         */
        fun crearCallbackInicializacion(
            context: Context
        ): Callback {

            return object : Callback() {

                override fun onCreate(
                    db: SupportSQLiteDatabase
                ) {
                    super.onCreate(db)

                    insertarProductosIniciales(
                        context.applicationContext,
                        db
                    )
                }
            }
        }

        /**
         * Inserta los productos iniciales
         * manteniendo los IDs originales.
         *
         * Es importante conservar los IDs
         * porque también identifican los QR.
         */
        private fun insertarProductosIniciales(
            context: Context,
            db: SupportSQLiteDatabase
        ) {

            ProductosDatos.productos.forEach { producto ->

                // Obtener el nombre estable del drawable,
                // en lugar de guardar su ID numérico.
                val nombreImagen = if (producto.imagen != 0) {
                    runCatching {
                        context.resources.getResourceEntryName(
                            producto.imagen
                        )
                    }.getOrNull()
                } else {
                    null
                }

                // Preparar los campos de la tabla.
                val valores = ContentValues().apply {

                    put("id", producto.id)
                    put("nombre", producto.nombre)
                    put("codigo", producto.codigo)
                    put("categoria", producto.categoria)
                    put("descripcion", producto.descripcion)
                    put("presentacion", producto.presentacion)

                    put("precio", producto.precio)
                    put("stock", producto.stock)
                    put("stockMinimo", producto.stockMinimo)

                    // SQLite representa Boolean
                    // mediante 1 (true) y 0 (false).
                    put(
                        "disponible",
                        if (producto.disponible) 1 else 0
                    )

                    put(
                        "imagenDrawableNombre",
                        nombreImagen
                    )

                    put(
                        "imagenUri",
                        producto.imagenUri
                    )
                }

                // Insertar manteniendo el ID original.
                // No reemplazar registros existentes.
                db.insert(
                    "productos",
                    SQLiteDatabase.CONFLICT_ABORT,
                    valores
                )
            }
        }
    }
}
