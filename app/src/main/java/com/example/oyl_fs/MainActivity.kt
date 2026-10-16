
package com.example.oyl_fs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider

import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.RegistroScreen
import com.example.oyl_fs.screens.HomeAdmin
import com.example.oyl_fs.screens.HomeCliente
import com.example.oyl_fs.screens.HistorialScreen
import com.example.oyl_fs.screens.CarroScreen

import com.example.oyl_fs.viewmodel.HistorialViewModel
import com.example.oyl_fs.viewmodel.ProductoViewModel
import com.example.oyl_fs.ui.theme.OYL_FSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel para gestionar los pedidos
        val historialViewModel = ViewModelProvider(this)[
            HistorialViewModel::class.java
        ]

        // ViewModel para gestionar los productos
        val productoViewModel = ViewModelProvider(this)[
            ProductoViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {
                // PRUEBA TEMPORAL - NO SUBIR A GIT
                com.example.oyl_fs.screens.MapaScreen(onVolver = {})
                return@OYL_FSTheme


                // Controla si existe una sesión iniciada
                var sesionIniciada by rememberSaveable {
                    mutableStateOf(false)
                }

                // identificamos si es admin
                var esAdmin by rememberSaveable {
                    mutableStateOf(false)
                }

                // navegacion a historial
                var mostrarHistorial by rememberSaveable {
                    mutableStateOf(false)
                }

                // navegacion a registro
                var mostrarRegistro by rememberSaveable {
                    mutableStateOf(false)
                }

                // navegacion al carrito
                var mostrarCarrito by rememberSaveable {
                    mutableStateOf(false)
                }

                when {

                    // login + registro
                    !sesionIniciada -> {

                        if (mostrarRegistro) {
                            RegistroScreen(
                                onRegistrado = {
                                    mostrarRegistro = false
                                },
                                onIrALogin = {
                                    mostrarRegistro = false
                                }
                            )
                        } else {
                            LoginScreen(
                                onLoginSuccess = { admin ->
                                    esAdmin = admin
                                    mostrarHistorial = false
                                    mostrarCarrito = false
                                    sesionIniciada = true
                                },
                                onIrARegistro = {
                                    mostrarRegistro = true
                                }
                            )
                        }
                    }

                    // carrito
                    mostrarCarrito && !esAdmin -> {
                        CarroScreen(
                            clienteId = "cliente-demo",
                            onVolver = {
                                mostrarCarrito = false
                            },
                            onConfirmar = { productos ->
                                // Pendiente: registrar el pedido confirmado
                                // utilizando los productos recibidos.
                            }
                        )
                    }

                    // historial
                    mostrarHistorial -> {
                        HistorialScreen(
                            // Identificadores temporales de prueba
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

                    // home admin
                    esAdmin -> {

                        HomeAdmin(
                            onLogout = {
                                mostrarHistorial = false
                                mostrarCarrito = false
                                sesionIniciada = false
                            },
                            onAbrirHistorial = {
                                mostrarHistorial = true
                            },
                            // catalogo + vm
                            viewModel = productoViewModel
                        )
                    }

                    // home cliente
                    else -> {

                        HomeCliente(
                            nombre = "Cliente",
                            onLogout = {
                                mostrarHistorial = false
                                mostrarCarrito = false
                                sesionIniciada = false
                            },
                            onAbrirHistorial = {
                                mostrarHistorial = true
                            },
                            onAbrirCarrito = {
                                mostrarCarrito = true
                            }
                        )
                    }
                }
            }
        }
    }
}
