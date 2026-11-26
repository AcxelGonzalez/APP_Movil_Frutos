
package com.example.oyl_fs.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que Room inicialice correctamente
 * los productos en una base SQLite real.
 *
 * Utiliza una base de pruebas independiente.
 */
@RunWith(AndroidJUnit4::class)
class ProductoRoomInicializacionTest {

    @Test
    fun productosSeCarganSoloUnaVez() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        // Base exclusiva para esta prueba.
        val nombreBase = "oyl_productos_prueba.db"

        // Eliminar únicamente la base de pruebas.
        contexto.deleteDatabase(nombreBase)

        fun crearBase(): OylDatabase {
            return Room.databaseBuilder(
                contexto,
                OylDatabase::class.java,
                nombreBase
            )
                .addCallback(
                    OylDatabase.crearCallbackInicializacion(
                        contexto
                    )
                )
                .build()
        }

        var base: OylDatabase? = null

        try {

            // Primera apertura de la base.
            base = crearBase()

            val dao = base.productoDao()

            // Room debe cargar los cinco productos.
            val productos = dao.observarProductos().first()

            assertEquals(5, productos.size)

            // Comprobar identificador y código QR.
            val almendras = dao.obtenerPorId(1)

            assertNotNull(almendras)

            assertEquals(
                "OYL001",
                almendras!!.codigo
            )

            // Eliminar un producto de prueba.
            assertEquals(
                1,
                dao.eliminar(almendras)
            )

            assertEquals(4, dao.contar())

            // Cerrar la primera conexión.
            base.close()
            base = null

            // Volver a abrir la misma base.
            base = crearBase()

            // Como la base ya existe, el producto
            // eliminado NO debe reaparecer.
            val cantidadFinal = base.productoDao()
                .contar()

            assertEquals(4, cantidadFinal)

        } finally {

            // Cerrar conexiones y limpiar solamente
            // los archivos de esta prueba.
            base?.close()
            contexto.deleteDatabase(nombreBase)
        }
    }
}
