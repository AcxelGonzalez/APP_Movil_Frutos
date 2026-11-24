package com.example.oyl_fs.viewmodel

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.MainActivity
import org.junit.Rule
import org.junit.Test

class SesionNavegacionTest {
    @get:Rule val compose = createEmptyComposeRule()

    private fun esperarTexto(texto: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(texto).assertIsDisplayed()
    }

    @Test fun clienteConservaSesionAlRecrearYReabrirYElLogoutImpideRestaurarla() {
        val contexto = ApplicationProvider.getApplicationContext<Context>()
        val sesionProtegida = com.example.oyl_fs.util.SesionDispositivoProtegida(contexto)
        var escenario: ActivityScenario<MainActivity>? = null
        try {
            escenario = ActivityScenario.launch(MainActivity::class.java)
            esperarTexto("Comenzar")
            compose.onNodeWithText("Comenzar").performClick()
            esperarTexto("Iniciar sesión")
            // Login de la cuenta académica existente; no crea cuentas ni pedidos.
            compose.onNodeWithText("Usuario").performScrollTo().performTextInput("cliente")
            compose.onNodeWithText("Contraseña").performScrollTo().performTextInput("cliente")
            compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
            esperarTexto("Catálogo")
            escenario.recreate()
            esperarTexto("Catálogo")
            escenario.close()
            escenario = ActivityScenario.launch(MainActivity::class.java)
            esperarTexto("Catálogo")
            compose.onNodeWithText("Perfil").performClick()
            compose.onNodeWithText("Cerrar sesión").performClick()
            esperarTexto("Iniciar sesión")
            escenario.close()
            escenario = ActivityScenario.launch(MainActivity::class.java)
            esperarTexto("Comenzar")
            compose.onNodeWithText("Comenzar").performClick()
            esperarTexto("Iniciar sesión")
        } finally {
            escenario?.close()
            // Restaurar la sesión que tenía el dispositivo antes de la prueba.
            sesionProtegida.close()
        }
    }
}
