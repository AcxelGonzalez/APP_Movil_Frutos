package com.example.oyl_fs

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import com.example.oyl_fs.screens.AlmendraScreen
import com.example.oyl_fs.screens.EditarProductoAdmin
import com.example.oyl_fs.screens.HomeScreen
import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.ProductosAdmin
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.ProductoViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comparte los datos entre catálogo, formulario y detalle.
        val productoViewModel = ViewModelProvider(this)[
            ProductoViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {
                var sesionIniciada by rememberSaveable {
                    mutableStateOf(false)
                }

                var mostrarProductos by rememberSaveable {
                    mutableStateOf(false)
                }

                var mostrarAlmendra by rememberSaveable {
                    mutableStateOf(false)
                }

                var editarAlmendra by rememberSaveable {
                    mutableStateOf(false)
                }

                val productos by productoViewModel.productos.collectAsState()
                val almendra = productos.find { it.id == 1 }

                // Regresa del catálogo al menú.
                BackHandler(
                    enabled = sesionIniciada &&
                            mostrarProductos &&
                            !mostrarAlmendra &&
                            !editarAlmendra
                ) {
                    mostrarProductos = false
                }

                if (!sesionIniciada) {
                    LoginScreen(
                        onLoginSuccess = {
                            mostrarProductos = false
                            mostrarAlmendra = false
                            editarAlmendra = false
                            sesionIniciada = true
                        }
                    )
                } else if (editarAlmendra && almendra != null) {
                    // Cancela la edición con Atrás.
                    BackHandler {
                        productoViewModel.limpiarError()
                        editarAlmendra = false
                    }

                    EditarProductoAdmin(
                        producto = almendra,
                        viewModel = productoViewModel,
                        onVolver = {
                            editarAlmendra = false
                        }
                    )
                } else if (mostrarAlmendra && almendra != null) {
                    BackHandler {
                        mostrarAlmendra = false
                    }

                    AlmendraScreen(
                        producto = almendra,
                        onVolver = {
                            mostrarAlmendra = false
                        },
                        onEditar = {
                            editarAlmendra = true
                        },
                        onEliminar = {
                            if (
                                productoViewModel.eliminarProducto(almendra.id)
                            ) {
                                editarAlmendra = false
                                mostrarAlmendra = false
                            }
                        }
                    )
                } else if (mostrarProductos) {
                    ProductosAdmin(
                        viewModel = productoViewModel,
                        onProductoClick = { producto ->
                            if (producto.id == 1) {
                                editarAlmendra = false
                                mostrarAlmendra = true
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "El detalle de este producto está pendiente",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                } else {
                    HomeScreen(
                        onLogout = {
                            mostrarProductos = false
                            mostrarAlmendra = false
                            editarAlmendra = false
                            sesionIniciada = false
                        },
                        onAbrirProductos = {
                            mostrarAlmendra = false
                            editarAlmendra = false
                            mostrarProductos = true
                        }
                    )
                }
            }
        }
    }
}