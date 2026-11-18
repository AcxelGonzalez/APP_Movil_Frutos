package com.example.oyl_fs.screens

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.util.textoQrProducto
import com.example.oyl_fs.viewmodel.CarritoViewModel
import com.example.oyl_fs.viewmodel.HistorialViewModel
import com.example.oyl_fs.viewmodel.PerfilViewModel
import com.example.oyl_fs.viewmodel.ProductoViewModel
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EscanerProductoTest {
    @get:Rule val compose = createComposeRule()
    private val almacen = ViewModelStore()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val modelos = ViewModelProvider(almacen, ViewModelProvider.AndroidViewModelFactory.getInstance(app))
    private val productos get() = modelos[ProductoViewModel::class.java]

    @After fun limpiar() { compose.runOnIdle { almacen.clear() } }

    @Test fun clienteAbreElEscanerDesdeInicioYLlegaAlDetalleConRetorno() {
        val vm = productos
        compose.setContent {
            OYL_FSTheme {
                HomeCliente(clienteId = "prueba-navegacion-qr", productoViewModel = vm,
                    carritoViewModel = modelos[CarritoViewModel::class.java],
                    historialViewModel = modelos[HistorialViewModel::class.java],
                    perfilViewModel = modelos[PerfilViewModel::class.java],
                    onLogout = {}, onAbrirHistorial = {}, onAbrirCarrito = {},
                    lanzarEscaner = { _, leer, _ -> leer(textoQrProducto(1)) })
            }
        }
        compose.onNodeWithText("Escanear producto").performScrollTo().performClick()
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("Ver producto").performScrollTo().performClick()
        compose.onNodeWithText("Detalle del producto").assertIsDisplayed()
        compose.onNodeWithText("Almendras").assertIsDisplayed()
        compose.onNodeWithText("‹").performClick()
        compose.onNodeWithText("Escanear código QR").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver").performClick()
        compose.onNodeWithText("Escanear producto").assertExists()
    }

    @Test fun administradorEscaneaProductoInactivoYAjustaSuStock() {
        val vm = productos
        val original = vm.obtenerProducto(1)!!
        assertTrue(vm.editarProducto(original.copy(disponible = false)))
        compose.setContent {
            OYL_FSTheme {
                HomeAdmin(onLogout = {}, onAbrirHistorial = {}, viewModel = vm,
                    historialViewModel = modelos[HistorialViewModel::class.java],
                    perfilViewModel = modelos[PerfilViewModel::class.java],
                    lanzarEscaner = { _, leer, _ -> leer(textoQrProducto(1)) })
            }
        }
        compose.onNodeWithText("Escanear producto").performScrollTo().performClick()
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("Producto inactivo").assertIsDisplayed()
        compose.onNodeWithText("Ajustar inventario").performScrollTo().performClick()
        compose.onNodeWithText("Editar producto").assertIsDisplayed()
        compose.onNode(hasSetTextAction() and hasText(original.stock.toString()))
            .performScrollTo().performTextReplacement("17")
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.obtenerProducto(1)?.stock == 17 }
        compose.onNodeWithText("Detalle del producto").assertExists()
        compose.onNodeWithText("‹").performScrollTo().performClick()
        compose.onNodeWithText("Escanear código QR").assertIsDisplayed()
        assertFalse(vm.obtenerProducto(1)!!.disponible)
        assertEquals(60, vm.obtenerProducto(2)?.stock)
    }

    @Test fun codigosInvalidosDesconocidosYCancelacionNoConservanUnaAccionAnterior() {
        var intento = 0
        val vm = productos
        compose.setContent {
            OYL_FSTheme {
                EscanerScreen(vm, false, {}, {}, lanzarEscaner = { _, leer, fallar ->
                    when (++intento) {
                        1 -> leer(textoQrProducto(1))
                        2 -> fallar("Escaneo cancelado.")
                        3 -> leer("https://ejemplo.test")
                        else -> leer(textoQrProducto(99999))
                    }
                })
            }
        }
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("Ver producto").assertExists()
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("Escaneo cancelado.").assertIsDisplayed()
        compose.onNodeWithText("Ver producto").assertDoesNotExist()
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("El código QR no es un producto OYL válido.").assertIsDisplayed()
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("El producto no existe en el catálogo actual.").assertIsDisplayed()
        compose.onNodeWithText("Ver producto").assertDoesNotExist()
    }

    @Test fun lecturaObservaCambiosDeStockDesactivacionYEliminacion() {
        val vm = productos
        var navegaciones = 0
        compose.setContent {
            OYL_FSTheme {
                EscanerScreen(vm, false, {}, { navegaciones++ },
                    lanzarEscaner = { _, leer, _ -> leer(textoQrProducto(1)) })
            }
        }
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.runOnIdle { assertTrue(vm.editarProducto(vm.obtenerProducto(1)!!.copy(stock = 4))) }
        compose.onNodeWithText("Stock: 4").assertIsDisplayed()
        compose.runOnIdle { assertTrue(vm.editarProducto(vm.obtenerProducto(1)!!.copy(disponible = false))) }
        compose.onNodeWithText("Ver producto").assertIsNotEnabled()
        compose.onNodeWithText("Este producto no está disponible para la venta.").assertIsDisplayed()
        compose.runOnIdle { assertTrue(vm.eliminarProducto(1)) }
        compose.onNodeWithText("El producto no existe en el catálogo actual.").assertIsDisplayed()
        compose.onNodeWithText("Ver producto").assertDoesNotExist()
        assertEquals(0, navegaciones)
    }

    @Test fun escaneoPendienteImpideDobleSolicitudEIgnoraLaRespuestaTrasSalir() {
        val vm = productos
        val visible = mutableStateOf(true)
        var respuesta: ((String) -> Unit)? = null
        var llamadas = 0
        compose.setContent {
            OYL_FSTheme {
                if (visible.value) {
                    EscanerScreen(vm, false, {}, {}, lanzarEscaner = { _, leer, _ ->
                        llamadas++
                        respuesta = leer
                    })
                } else Text("Fuera del escáner")
            }
        }
        compose.onNodeWithText("Escanear código QR").performClick()
        compose.onNodeWithText("Abriendo cámara…").assertIsNotEnabled()
        assertEquals(1, llamadas)
        compose.runOnIdle { visible.value = false }
        compose.runOnIdle { respuesta!!.invoke(textoQrProducto(1)); visible.value = true }
        compose.onNodeWithText("Escanear código QR").assertIsEnabled()
        compose.onNodeWithText("Ver producto").assertDoesNotExist()
    }
}
