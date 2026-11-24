package com.example.oyl_fs.screens

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.MainActivity
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class IntegracionRamasScreenTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val contexto = ApplicationProvider.getApplicationContext<Application>()

    private fun esperar(texto: String) {
        compose.waitUntil(15000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun bienvenidaLoginYRegistroConservanNavegacionYErroresPorCampo() {
        val sesionProtegida = com.example.oyl_fs.util.SesionDispositivoProtegida(contexto)
        var escenario: ActivityScenario<MainActivity>? = null
        try {
            escenario = ActivityScenario.launch(MainActivity::class.java)
            esperar("Comenzar")
            compose.onNodeWithText("Comenzar").performClick()
            esperar("Iniciar sesión")
            escenario.recreate()
            esperar("Iniciar sesión")
            compose.onNodeWithText("Comenzar").assertDoesNotExist()
            compose.onNodeWithText("Iniciar sesión").performScrollTo().performClick()
            compose.onNodeWithText("Ingresa tu usuario.").assertExists()
            compose.onNodeWithText("Ingresa tu contraseña.").assertExists()
            compose.onNodeWithText("Crear una cuenta").performScrollTo().performClick()
            esperar("Crear cuenta")
            compose.onNodeWithText("Registrarse").performScrollTo().performClick()
            for (mensaje in listOf(
                "Usa entre 3 y 24 caracteres: letras, números, punto, guion o guion bajo.",
                "Ingresa tu nombre completo.", "Ingresa un correo electrónico válido.",
                "Ingresa un RUT válido, por ejemplo 12.345.678-5.", "Ingresa tu teléfono",
                "La contraseña debe tener al menos 8 caracteres y no puede estar en blanco."
            )) compose.onNodeWithText(mensaje).assertExists()
            compose.onNodeWithContentDescription("Volver").performScrollTo().performClick()
            esperar("Iniciar sesión")
        } finally {
            escenario?.close()
            sesionProtegida.close()
        }
    }
}
