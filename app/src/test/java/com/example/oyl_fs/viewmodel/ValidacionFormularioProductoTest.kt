
package com.example.oyl_fs.viewmodel

import org.junit.Assert.*
import org.junit.Test

class ValidacionFormularioProductoTest {

    private val valido = DatosFormularioProducto(
        nombre = "Almendras",
        codigo = "ALM-001",
        categoria = "Frutos secos",
        precio = "2990",
        stock = "20",
        stockMinimo = "5",
        imagenPresente = true
    )

    @Test
    fun productoValidoNoPresentaErrores() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido, requerirImagen = true
            )

        assertFalse(resultado.hayErrores)
    }

    @Test
    fun nombreYCodigoVaciosPresentanErrores() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido.copy(nombre = "", codigo = "")
            )

        assertNotNull(resultado.nombre)
        assertNotNull(resultado.codigo)
    }

    @Test
    fun precioCeroEsRechazado() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido.copy(precio = "0")
            )

        assertNotNull(resultado.precio)
    }

    @Test
    fun stockVacioNoSeAceptaComoCero() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido.copy(stock = "")
            )

        assertNotNull(resultado.stock)
    }

    @Test
    fun stockCeroEsValido() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido.copy(stock = "0")
            )

        assertNull(resultado.stock)
    }

    @Test
    fun categoriaInvalidaEsRechazada() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido.copy(categoria = "Otra")
            )

        assertNotNull(resultado.categoria)
    }

    @Test
    fun fotografiaObligatoriaEsValidada() {
        val resultado =
            ValidacionFormularioProducto.validar(
                valido.copy(imagenPresente = false),
                requerirImagen = true
            )

        assertNotNull(resultado.imagen)
    }
}
