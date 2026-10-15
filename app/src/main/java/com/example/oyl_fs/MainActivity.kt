
package com.example.oyl_fs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider

import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.HomeAdmin
import com.example.oyl_fs.screens.HomeCliente
import com.example.oyl_fs.screens.HistorialScreen
import com.example.oyl_fs.viewmodel.HistorialViewModel
import com.example.oyl_fs.ui.theme.OYL_FSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel asociado al ciclo de vida de la Activity.
        val historialViewModel = ViewModelProvider(this)[
            HistorialViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {

                // Controla si existe una sesión iniciada.
                var sesionIniciada by rememberSaveable {
                    mutableStateOf(false)
                }

                // Identifica el rol del usuario.
                var esAdmin by rememberSaveable {
                    mutableStateOf(false)
                }

                // Controla la navegación hacia Historial.
                var mostrarHistorial by rememberSaveable {
                    mutableStateOf(false)
                }

                when {

                    // Sin sesión: mostrar Login.
                    !sesionIniciada -> {
                        LoginScreen(
                            onLoginSuccess = { admin ->
                                esAdmin = admin
                                mostrarHistorial = false
                                sesionIniciada = true
                            }
                        )
                    }

                    // Historial del perfil autenticado.
                    mostrarHistorial -> {
                        HistorialScreen(
                            // Identificadores temporales de prueba.
                            usuarioId = if (esAdmin) {
                                "admin-demo"
                            } else {
                                "cliente-demo"
                            },

                            esAdmin = esAdmin,
                            viewModel = historialViewModel,

                            onVolver = {
                                mostrarHistorial = false
                            }
                        )
                    }

                    // Pantalla del administrador.
                    esAdmin -> {
                        HomeAdmin(
                            onLogout = {
                                mostrarHistorial = false
                                sesionIniciada = false
                            },

                            onAbrirHistorial = {
                                mostrarHistorial = true
                            }
                        )
                    }

                    // Pantalla del cliente.
                    else -> {
                        HomeCliente(
                            nombre = "Cliente",

                            onLogout = {
                                mostrarHistorial = false
                                sesionIniciada = false
                            },

                            onAbrirHistorial = {
                                mostrarHistorial = true
                            }
                        )
                    }
                }
            }
        }
    }
}
