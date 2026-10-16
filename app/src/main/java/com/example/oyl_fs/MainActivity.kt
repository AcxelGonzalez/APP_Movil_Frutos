
package com.example.oyl_fs

import android.os.Bundle
import android.widget.Toast
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
import com.example.oyl_fs.viewmodel.CarritoViewModel
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

        // ViewModel compartido entre HomeCliente y CarroScreen.
        // Evita que cada pantalla utilice un estado independiente.
        val carritoViewModel = ViewModelProvider(this)[
            CarritoViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {

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
                            // Identificador temporal del cliente.
                            clienteId = "cliente-demo",

                            // ViewModel compartido con HomeCliente.
                            vm = carritoViewModel,

                            onVolver = {
                                mostrarCarrito = false
                            },

                            onConfirmar = { productos ->

                                // 1. Registrar el pedido en Historial.
                                val pedido = historialViewModel.registrarPedido(
                                    usuarioId = "cliente-demo",
                                    productos = productos
                                )

                                // 2. Comprobar el resultado.
                                if (pedido != null) {

                                    // Vaciar el carrito solo si el registro
                                    // del pedido fue exitoso.
                                    carritoViewModel.vaciarDespuesDeConfirmar()

                                    // Cerrar CarroScreen y abrir HistorialScreen.
                                    mostrarCarrito = false
                                    mostrarHistorial = true

                                    // Informar al cliente sobre su compra.
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Pedido ${pedido.id} registrado correctamente",
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {

                                    // Si el registro falla, conservar el carrito.
                                    Toast.makeText(
                                        this@MainActivity,
                                        "No se pudo registrar el pedido. Revisa el carrito.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
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

                            // Identificador temporal hasta implementar sesiones.
                            clienteId = "cliente-demo",

                            // Compartimos el catálogo y el carrito.
                            productoViewModel = productoViewModel,
                            carritoViewModel = carritoViewModel,

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
