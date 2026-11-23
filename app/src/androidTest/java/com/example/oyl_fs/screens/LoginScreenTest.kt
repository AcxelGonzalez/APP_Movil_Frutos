package com.example.oyl_fs.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.oyl_fs.viewmodel.LoginUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/*
Revisa la pantalla de inicio de sesión tal como la ve el usuario:
que aparezcan los campos y los botones, que al pulsar Iniciar sesión
se envíe lo que escribió, que el botón se bloquee mientras se conecta
y que se muestre el mensaje cuando algo falla.
 */
class LoginScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun seMuestranLosCamposYLosBotones() {
        compose.setContent {
            LoginContenido(
                estado = LoginUiState(),
                onLimpiarError = {},
                onIngresar = { _, _ -> },
                onIrARegistro = {}
            )
        }

        compose.onNodeWithText("Bienvenido").assertIsDisplayed()
        compose.onNodeWithText("Usuario").assertIsDisplayed()
        compose.onNodeWithText("Contraseña").assertIsDisplayed()
        compose.onNodeWithText("Iniciar sesión").assertIsDisplayed()
        compose.onNodeWithText("Crear una cuenta").assertIsDisplayed()
        compose.onNodeWithText("¿Olvidaste tu contraseña?").assertIsDisplayed()
    }

    @Test
    fun alPulsarIniciarSesionSeEnviaLoQueElUsuarioEscribio() {
        var usuarioEnviado = ""
        var claveEnviada = ""

        compose.setContent {
            LoginContenido(
                estado = LoginUiState(),
                onLimpiarError = {},
                onIngresar = { usuario, clave ->
                    usuarioEnviado = usuario
                    claveEnviada = clave
                },
                onIrARegistro = {}
            )
        }

        compose.onNodeWithText("Usuario").performTextInput("admin")
        compose.onNodeWithText("Contraseña").performTextInput("admin")
        compose.onNodeWithText("Iniciar sesión").performClick()

        assertEquals("admin", usuarioEnviado)
        assertEquals("admin", claveEnviada)
    }

    @Test
    fun elBotonCrearCuentaLlevaAlRegistro() {
        var fueAlRegistro = false

        compose.setContent {
            LoginContenido(
                estado = LoginUiState(),
                onLimpiarError = {},
                onIngresar = { _, _ -> },
                onIrARegistro = { fueAlRegistro = true }
            )
        }

        compose.onNodeWithText("Crear una cuenta").performClick()

        assertTrue(fueAlRegistro)
    }

    @Test
    fun mientrasSeConectaElBotonSeBloquea() {
        compose.setContent {
            LoginContenido(
                estado = LoginUiState(procesando = true),
                onLimpiarError = {},
                onIngresar = { _, _ -> },
                onIrARegistro = {}
            )
        }

        compose.onNodeWithText("Ingresando…").assertIsNotEnabled()
        compose.onNodeWithText("Crear una cuenta").assertIsNotEnabled()
    }

    @Test
    fun siLasCredencialesFallanSeMuestraElMensaje() {
        compose.setContent {
            LoginContenido(
                estado = LoginUiState(error = "Usuario o contraseña incorrectos."),
                onLimpiarError = {},
                onIngresar = { _, _ -> },
                onIrARegistro = {}
            )
        }

        compose.onNodeWithText("Usuario o contraseña incorrectos.").assertIsDisplayed()
        compose.onNodeWithText("Iniciar sesión").assertIsEnabled()
    }

    @Test
    fun elOjoCambiaEntreMostrarYOcultarLaContrasena() {
        compose.setContent {
            LoginContenido(
                estado = LoginUiState(),
                onLimpiarError = {},
                onIngresar = { _, _ -> },
                onIrARegistro = {}
            )
        }

        compose.onNodeWithContentDescription("Mostrar contraseña").performClick()
        compose.onNodeWithContentDescription("Ocultar contraseña").assertIsDisplayed()
    }

    @Test
    fun seAbreLaAyudaDeContrasenaOlvidada() {
        compose.setContent {
            LoginContenido(
                estado = LoginUiState(),
                onLimpiarError = {},
                onIngresar = { _, _ -> },
                onIrARegistro = {}
            )
        }

        compose.onNodeWithText("¿Olvidaste tu contraseña?").performClick()

        compose.onNodeWithText("Recuperar el acceso").assertIsDisplayed()
    }
}