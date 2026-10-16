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
}
