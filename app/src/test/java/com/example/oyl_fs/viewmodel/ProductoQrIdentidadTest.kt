package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.screens.ProductosDatos
import org.junit.Assert.*
import org.junit.Test

class ProductoQrIdentidadTest {
    @Test
    fun eliminarYAgregarNoReutilizaElIdentificadorDeUnQrAnterior() {
        val vm = ProductoViewModel()
        assertTrue(vm.agregarProducto(ProductosDatos.almendras.copy(codigo = "PRUEBA-QR-1")))
        val anterior = vm.productos.value.last().id
        assertTrue(vm.eliminarProducto(anterior))
        assertTrue(vm.agregarProducto(ProductosDatos.almendras.copy(codigo = "PRUEBA-QR-2")))
        assertNotEquals(anterior, vm.productos.value.last().id)
    }
}
