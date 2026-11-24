
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
        imagenPresente = true,
        presentacion = "500 g"
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

    @Test fun presentacionVaciaNoPermiteGuardar() {
        assertNotNull(ValidacionFormularioProducto.validar(valido.copy(presentacion = "  ")).presentacion)
    }

    @Test fun limitesDeTextoSeAdmitenSinExcederElContrato() {
        val limite = valido.copy(nombre = "n".repeat(100), codigo = "c".repeat(30), descripcion = "d".repeat(500))
        assertFalse(ValidacionFormularioProducto.validar(limite).hayErrores)
        val errores = ValidacionFormularioProducto.validar(limite.copy(
            nombre = limite.nombre + "n", codigo = limite.codigo + "c", descripcion = limite.descripcion + "d"))
        assertNotNull(errores.nombre)
        assertNotNull(errores.codigo)
        assertNotNull(errores.descripcion)
    }

    @Test fun numerosFueraDelRangoNoSeConviertenEnCero() {
        val errores = ValidacionFormularioProducto.validar(valido.copy(
            precio = "2147483648", stock = "2147483648", stockMinimo = "-1"))
        assertNotNull(errores.precio)
        assertNotNull(errores.stock)
        assertNotNull(errores.stockMinimo)
    }
}
