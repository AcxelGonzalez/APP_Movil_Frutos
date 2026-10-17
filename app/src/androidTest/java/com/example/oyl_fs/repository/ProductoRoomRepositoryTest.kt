
package com.example.oyl_fs.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.screens.ProductosDatos

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba el funcionamiento real del repositorio
 * de productos utilizando SQLite.
 *
 * La base de pruebas es independiente de la
 * utilizada normalmente por OYL Frutos Secos.
 */
@RunWith(AndroidJUnit4::class)
class ProductoRoomRepositoryTest {

    @Test
    fun crudYPersistenciaDeProductos() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val nombreBase = "oyl_repositorio_prueba.db"

        // Eliminar únicamente la base de pruebas.
        contexto.deleteDatabase(nombreBase)

        fun crearBase(): OylDatabase {
            return Room.databaseBuilder(
                contexto,
                OylDatabase::class.java,
                nombreBase
            )
                .addCallback(
                    OylDatabase.crearCallbackInicializacion(contexto)
                )
                .build()
        }

        var base = crearBase()

        try {

            val repositorio = ProductoRoomRepository(
                contexto,
                base.productoDao()
            )

            // 1. Verificar los cinco productos iniciales.
            assertEquals(
                5,
                repositorio.productos.first().size
            )

            // 2. Agregar un producto nuevo.
            val nuevo = ProductosDatos.almendras.copy(
                id = 0,
                nombre = "Producto de prueba Room",
                codigo = "OYL-ROOM-TEST",
                stock = 15
            )

            val nuevoId = repositorio.insertar(nuevo).toInt()

            // El nuevo ID no debe reemplazar los IDs
            // iniciales utilizados por los códigos QR.
            assertTrue(nuevoId > 5)

            // 3. Recuperar el producto insertado.
            val guardado = repositorio.obtenerPorId(nuevoId)

            assertNotNull(guardado)
            assertEquals(
                "Producto de prueba Room",
                guardado?.nombre
            )

            // 4. Editar el stock.
            assertTrue(
                repositorio.actualizar(
                    guardado!!.copy(stock = 7)
                )
            )

            assertEquals(
                7,
                repositorio.obtenerPorId(nuevoId)?.stock
            )

            // 5. Comprobar que ahora existen seis productos.
            assertEquals(
                6,
                repositorio.contar()
            )

            // 6. Eliminar el producto agregado.
            assertTrue(repositorio.eliminar(nuevoId))

            assertNull(
                repositorio.obtenerPorId(nuevoId)
            )

            // 7. Modificar el stock de Almendras para
            // comprobar que persiste al cerrar la base.
            val almendras = repositorio.obtenerPorId(1)!!

            assertTrue(
                repositorio.actualizar(
                    almendras.copy(stock = 45)
                )
            )

            // Cerrar y volver a abrir la misma base.
            base.close()
            base = crearBase()

            val repositorioRecuperado =
                ProductoRoomRepository(
                    contexto,
                    base.productoDao()
                )

            // 8. Los cambios deben permanecer.
            assertEquals(
                5,
                repositorioRecuperado.contar()
            )

            assertEquals(
                45,
                repositorioRecuperado
                    .obtenerPorId(1)?.stock
            )

            // El producto eliminado no debe reaparecer.
            assertNull(
                repositorioRecuperado.obtenerPorId(nuevoId)
            )

        } finally {
            base.close()
            contexto.deleteDatabase(nombreBase)
        }
    }
}
