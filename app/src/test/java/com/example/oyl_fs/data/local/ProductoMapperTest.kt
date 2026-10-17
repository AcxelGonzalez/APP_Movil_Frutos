
package com.example.oyl_fs.data.local

import com.example.oyl_fs.screens.ProductosDatos
import org.junit.Assert.*
import org.junit.Test

/**
 * Verifica que las conversiones entre ProductoInfo
 * y ProductoEntity conserven los datos originales.
 */
class ProductoMapperTest {

    @Test
    fun conversionConservaLosDatosDelProducto() {

        val original = ProductosDatos.almendras.copy(
            imagenUri = "content://prueba/almendras"
        )

        // Convertir el producto en entidad Room.
        val entidad = original.aEntidad(
            imagenDrawableNombre = "almendras"
        )

        // Simular la restauración del ID de drawable.
        val recuperado = entidad.aProducto(
            imagen = original.imagen
        )

        // Todos los campos deben coincidir.
        assertEquals(original, recuperado)

        // Verificar que se conserva la referencia
        // estable de la imagen.
        assertEquals(
            "almendras",
            entidad.imagenDrawableNombre
        )

        assertEquals(
            original.imagenUri,
            recuperado.imagenUri
        )
    }

    @Test
    fun conversionConservaIdentificadorDelProducto() {

        val original = ProductosDatos.nueces.copy(
            id = 25,
            codigo = "OYL025",
            imagen = 0,
            imagenUri = null
        )

        val entidad = original.aEntidad()
        val recuperado = entidad.aProducto()

        // El ID no debe cambiar porque se utiliza
        // para identificar productos en los QR.
        assertEquals(25, entidad.id)
        assertEquals(25, recuperado.id)

        assertEquals(original, recuperado)
        assertNull(entidad.imagenDrawableNombre)
    }
}
