package com.example.oyl_fs.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.example.oyl_fs.model.*
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConfirmacionPedidoScreenTest {
    @get:Rule val compose = createComposeRule()
    private val items = listOf(ItemCarrito("1", "Almendras", "500 g", 1000, 2, 10))
    private val perfil = PerfilUsuario("cliente-a", false, "Cliente ficticio", telefono = "912345678")

    @Test fun camposObligatoriosImpidenEnviarYDireccionDependeDeModalidad() {
        var envios = 0
        compose.setContent { OYL_FSTheme { ConfirmacionPedidoScreen(items, null, false, null, {}, { envios++ }) } }
        compose.onNodeWithText("Finalizar pedido").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa el nombre de contacto").assertExists()
        compose.onNodeWithText("Ingresa un teléfono válido de 8 a 11 dígitos").assertExists()
        compose.onNodeWithText("Selecciona un punto de retiro válido").assertExists()
        assertEquals(0, envios)
        compose.onNodeWithText("Entrega a domicilio").performScrollTo().performClick()
        compose.onNodeWithText("Finalizar pedido").performScrollTo().performClick()
        compose.onNodeWithText("Ingresa la dirección de entrega").assertExists()
        assertEquals(0, envios)
    }

    @Test fun formularioConservaDatosAlRestaurarseYEnviaDomicilioSinPuntoAnterior() {
        val restauracion = StateRestorationTester(compose)
        var entrega: EntregaPedido? = null
        restauracion.setContent { OYL_FSTheme { ConfirmacionPedidoScreen(items, perfil, false, null, {}, { entrega = it }) } }
        compose.onNodeWithText("OYL Frutos Secos - Tienda principal").performScrollTo().performClick()
        compose.onNodeWithText("Entrega a domicilio").performScrollTo().performClick()
        compose.onNodeWithText("Dirección de entrega").performScrollTo().performTextInput("Calle ficticia 200")
        restauracion.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Dirección de entrega").assertTextContains("Calle ficticia 200")
        compose.onNodeWithText("Finalizar pedido").performScrollTo().performClick()
        assertEquals(ModalidadEntrega.DOMICILIO, entrega?.modalidad)
        assertEquals("Cliente ficticio", entrega?.nombreContacto)
        assertEquals("Calle ficticia 200", entrega?.direccion)
        assertEquals("", entrega?.puntoRetiroId)
    }

    @Test fun comprobanteMuestraIdentidadEstadoYContactoYAbreMisPedidos() {
        var aperturas = 0
        val pedido = Pedido("PED-PRUEBA", "cliente-a", 1,
            listOf(DetallePedido("1", "Almendras", 2, 1000)), 2000, EstadoPedido.PENDIENTE_VALIDACION,
            EntregaPedido(ModalidadEntrega.RETIRO, "Cliente ficticio", "912345678", "Calle prueba 100", "T01", "Tienda prueba"))
        compose.setContent { OYL_FSTheme { PedidoConfirmadoScreen(pedido) { aperturas++ } } }
        compose.onNodeWithText("Pedido registrado").assertIsDisplayed()
        compose.onNodeWithText("Número de solicitud: PED-PRUEBA").assertExists()
        compose.onNodeWithText("Pendiente de Validación").assertExists()
        compose.onNodeWithText("Contacto: Cliente ficticio").assertExists()
        compose.onNodeWithText("Punto de retiro: Tienda prueba").assertExists()
        compose.onNodeWithText("Ver mis pedidos").performScrollTo().performClick()
        assertEquals(1, aperturas)
    }
}
