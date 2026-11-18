package com.example.oyl_fs.viewmodel

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        val preferencias = contexto.getSharedPreferences("sesion_oyl", Context.MODE_PRIVATE)
        val anterior = preferencias.getString("usuarioId", null)
        var escenario: ActivityScenario<MainActivity>? = null
        try {
            check(preferencias.edit().putString("usuarioId", "cliente-demo").commit())
            escenario = ActivityScenario.launch(MainActivity::class.java)
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
            esperarTexto("Iniciar sesión")
        } finally {
            escenario?.close()
            // Restaurar la sesión que tenía el dispositivo antes de la prueba.
            val editor = preferencias.edit()
            if (anterior == null) editor.remove("usuarioId") else editor.putString("usuarioId", anterior)
            check(editor.commit())
        }
    }
}
