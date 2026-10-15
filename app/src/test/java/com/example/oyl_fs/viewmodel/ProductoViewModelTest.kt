package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.screens.ProductosDatos
import org.junit.Assert.*
import org.junit.Test

class ProductoViewModelTest {

    @Test
    fun editarCadaProductoConservaLosDemas() {
        val viewModel = ProductoViewModel()
        val originales = viewModel.productos.value

        originales.forEach { producto ->
            val editado = producto.copy(precio = producto.precio + 100)
            assertTrue(viewModel.editarProducto(editado))
            assertEquals(editado, viewModel.obtenerProducto(producto.id))
        }

        assertEquals(originales.size, viewModel.productos.value.size)
        assertEquals(
            originales.map { it.id },
            viewModel.productos.value.map { it.id }
        )
    }

    @Test
    fun productoNuevoSePuedeConsultarEditarYEliminar() {
        val viewModel = ProductoViewModel()
        val cantidadInicial = viewModel.productos.value.size
        val nuevo = ProductosDatos.almendras.copy(
            id = 0,
            nombre = "Producto de prueba",
            codigo = "PRUEBA-001",
            imagen = 0,
            imagenUri = "content://prueba/foto"
        )

        assertTrue(viewModel.agregarProducto(nuevo))
        val guardado = viewModel.productos.value.last()
        assertEquals(cantidadInicial + 1, viewModel.productos.value.size)
        assertEquals(guardado, viewModel.obtenerProducto(guardado.id))
        assertEquals(nuevo.imagenUri, guardado.imagenUri)

        val editado = guardado.copy(precio = 12000, disponible = false)
        assertTrue(viewModel.editarProducto(editado))
        assertEquals(editado, viewModel.obtenerProducto(guardado.id))

        assertTrue(viewModel.eliminarProducto(guardado.id))
        assertNull(viewModel.obtenerProducto(guardado.id))
        assertEquals(cantidadInicial, viewModel.productos.value.size)
    }

    @Test
    fun codigoDuplicadoNoModificaLaLista() {
        val viewModel = ProductoViewModel()
        val originales = viewModel.productos.value
        val duplicado = originales[1].copy(
            codigo = "  ${originales[0].codigo.lowercase()}  "
        )

        assertFalse(viewModel.editarProducto(duplicado))
        assertEquals(originales, viewModel.productos.value)
        assertNotNull(viewModel.error.value)
    }
}
