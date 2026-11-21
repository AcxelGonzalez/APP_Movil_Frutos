package com.example.oyl_fs.repository

import com.example.oyl_fs.model.ItemCarrito
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CarritoCantidadTest {
    @Test
    fun cantidadAcumuladaNoSuperaStockYSeSeparaPorCliente() = runBlocking {
        val cliente = "prueba-cantidades"
        val otro = "prueba-otro-cliente"
        CarritoRepository.vaciar(cliente)
        CarritoRepository.vaciar(otro)
        val item = ItemCarrito("1", "Almendras", "500 g", 6990, 2, 3)
        assertTrue(CarritoRepository.agregar(cliente, item))
        assertFalse(CarritoRepository.agregar(cliente, item))
        assertEquals(2, CarritoRepository.obtenerCarrito(cliente).first().single().cantidad)
        assertTrue(CarritoRepository.obtenerCarrito(otro).first().isEmpty())
        CarritoRepository.vaciar(cliente)
    }
    @Test
    fun fotoSeActualizaAlAgregarOtraUnidad() = runBlocking {
        val cliente = "prueba-foto"
        CarritoRepository.vaciar(cliente)
        try {
            val item = ItemCarrito("1", "Almendras", "500 g", 6990, 1, 5, imagen = 11)
            assertTrue(CarritoRepository.agregar(cliente, item))
            assertEquals(11, CarritoRepository.obtenerCarrito(cliente).first().single().imagen)
            assertTrue(CarritoRepository.agregar(cliente, item.copy(imagenUri = "content://fotos/nueva")))
            val guardado = CarritoRepository.obtenerCarrito(cliente).first().single()
            assertEquals(2, guardado.cantidad)
            assertEquals("content://fotos/nueva", guardado.imagenUri)
        } finally {
            CarritoRepository.vaciar(cliente)
        }
    }

    @Test
    fun sincronizarPermiteVolverDeFotoDelTelefonoADrawable() = runBlocking {
        val cliente = "prueba-foto-catalogo"
        CarritoRepository.vaciar(cliente)
        try {
            CarritoRepository.agregar(cliente,
                ItemCarrito("1", "Almendras", "500 g", 6990, 1, 5, imagenUri = "content://foto/anterior"))
            val producto = com.example.oyl_fs.model.ProductoInfo(
                1, "Almendras", "OYL001", "Frutos secos", "", "500 g", 6990, 5, 1, true,
                imagen = 22, imagenUri = null)
            CarritoRepository.sincronizarConCatalogo(cliente, listOf(producto))
            val guardado = CarritoRepository.obtenerCarrito(cliente).first().single()
            assertEquals(22, guardado.imagen)
            assertNull(guardado.imagenUri)
        } finally {
            CarritoRepository.vaciar(cliente)
        }
    }

}
