
package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.ItemCarrito
import com.example.oyl_fs.model.ProductoInfo
import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias de las reglas de inventario.
 *
 * Cada prueba crea un ProductoViewModel nuevo
 * para evitar compartir modificaciones de stock.
 */
class ProductoInventarioTest {

    /**
     * Crea un artículo de prueba utilizando
     * la información de un producto del catálogo.
     */
    private fun crearItem(
        producto: ProductoInfo,
        cantidad: Int,
        precio: Int = producto.precio
    ): ItemCarrito {
        return ItemCarrito(
            productoId = producto.id.toString(),
            nombre = producto.nombre,
            formato = producto.presentacion,
            precio = precio,
            cantidad = cantidad,
            stockDisponible = producto.stock
        )
    }

    @Test
    fun compraValidaDescuentaStockCorrectamente() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        val carrito = listOf(
            crearItem(almendras, cantidad = 2)
        )

        // La compra debe superar la validación.
        assertNull(vm.validarStockParaCompra(carrito))

        // Debe descontar las dos unidades compradas.
        assertTrue(vm.descontarStockPorCompra(carrito))

        val actualizado = vm.obtenerProducto(almendras.id)

        assertNotNull(actualizado)
        assertEquals(
            almendras.stock - 2,
            actualizado!!.stock
        )
    }

    @Test
    fun compraSinStockSuficienteEsRechazada() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        // Solicita una unidad más que el stock.
        val carrito = listOf(
            crearItem(
                almendras,
                cantidad = almendras.stock + 1
            )
        )

        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))

        // El stock original debe mantenerse.
        assertEquals(
            almendras.stock,
            vm.obtenerProducto(almendras.id)?.stock
        )
    }

    @Test
    fun compraDetectaStockModificadoPorAdministrador() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        // El cliente agregó tres unidades
        // cuando había suficiente stock.
        val carrito = listOf(
            crearItem(almendras, cantidad = 3)
        )

        // El administrador reduce el stock.
        assertTrue(
            vm.editarProducto(
                almendras.copy(stock = 2)
            )
        )

        // Debe consultar el stock actualizado.
        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))

        assertEquals(
            2,
            vm.obtenerProducto(almendras.id)?.stock
        )
    }

    @Test
    fun compraDetectaCambioDePrecio() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        // El carrito conserva el precio anterior.
        val carrito = listOf(
            crearItem(almendras, cantidad = 1)
        )

        // El administrador modifica el precio.
        assertTrue(
            vm.editarProducto(
                almendras.copy(
                    precio = almendras.precio + 1000
                )
            )
        )

        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))

        // Un cambio de precio no descuenta stock.
        assertEquals(
            almendras.stock,
            vm.obtenerProducto(almendras.id)?.stock
        )
    }

    @Test
    fun productoDesactivadoNoPuedeComprarse() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        val carrito = listOf(
            crearItem(almendras, cantidad = 1)
        )

        assertTrue(
            vm.editarProducto(
                almendras.copy(disponible = false)
            )
        )

        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))
    }

    @Test
    fun productoEliminadoNoPuedeComprarse() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        val carrito = listOf(
            crearItem(almendras, cantidad = 1)
        )

        assertTrue(vm.eliminarProducto(almendras.id))

        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))
    }

    @Test
    fun productosDuplicadosNoPuedenSuperarStock() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        // Dos entradas del mismo producto.
        // Individualmente son válidas,
        // pero juntas superan el stock.
        val carrito = listOf(
            crearItem(almendras, cantidad = 40),
            crearItem(almendras, cantidad = 30)
        )

        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))

        assertEquals(
            almendras.stock,
            vm.obtenerProducto(almendras.id)?.stock
        )
    }

    @Test
    fun cantidadCeroNoPuedeComprarse() {
        val vm = ProductoViewModel()
        val almendras = vm.productos.value.first()

        val carrito = listOf(
            crearItem(almendras, cantidad = 0)
        )

        assertNotNull(vm.validarStockParaCompra(carrito))
        assertFalse(vm.descontarStockPorCompra(carrito))
    }
}
