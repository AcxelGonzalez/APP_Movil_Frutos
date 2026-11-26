package com.example.oyl_fs.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FormularioProductoScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nuevoProductoSinDatosMuestraPresentacionObligatoriaYNoEnvia() {
        var envios = 0
        compose.setContent { OYL_FSTheme { AgregarProductoScreen({}, { envios++ }) } }
        compose.onNodeWithText("Tomar foto").assertExists()
        compose.onNodeWithText("Guardar producto").performScrollTo().performClick()
        compose.onNodeWithText("Escribe el peso o la presentación.").assertExists()
        compose.onNodeWithText("Escribe el nombre del producto.").assertExists()
        compose.onNodeWithText("Selecciona una fotografía.").assertExists()
        assertEquals(0, envios)
    }

    @Test fun editarProductoConTextoExcedidoNoEnviaYConservaLaFotoOriginal() {
        var envios = 0
        compose.setContent { OYL_FSTheme {
            EditarProductoScreen(ProductosDatos.almendras.copy(
                codigo = "C".repeat(31), descripcion = "D".repeat(501), presentacion = ""), {}, { envios++ })
        } }
        compose.onNodeWithText("Guardar cambios").performScrollTo().performClick()
        compose.onNodeWithText("El código admite hasta 30 caracteres.").assertExists()
        compose.onNodeWithText("La descripción admite hasta 500 caracteres.").assertExists()
        compose.onNodeWithText("Escribe el peso o la presentación.").assertExists()
        compose.onNodeWithText("Selecciona una fotografía.").assertDoesNotExist()
        assertEquals(0, envios)
    }
}
