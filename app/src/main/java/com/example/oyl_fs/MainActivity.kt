package com.example.oyl_fs

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import com.example.oyl_fs.screens.HomeAdmin
import com.example.oyl_fs.screens.HomeCliente
import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.ProductosAdmin
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.ProductoViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val productoViewModel = ViewModelProvider(this)[
            ProductoViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {
                var sesionIniciada by rememberSaveable {
                    mutableStateOf(false)
                }

                var esAdmin by rememberSaveable {
                    mutableStateOf(false)
                }

                var mostrarProductos by rememberSaveable {
                    mutableStateOf(false)
                }

                // Aviso hasta integrar el módulo de pedidos.
                val abrirHistorial: () -> Unit = {
                    Toast.makeText(
                        this@MainActivity,
                        "El historial todavía no está integrado",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                if (!sesionIniciada) {
                    LoginScreen(
                        onLoginSuccess = { admin ->
                            esAdmin = admin
                            mostrarProductos = false
                            sesionIniciada = true
                        }
                    )
                } else if (esAdmin && mostrarProductos) {
                    ProductosAdmin(
                        viewModel = productoViewModel,
                        onVolver = {
                            mostrarProductos = false
                        }
                    )
                } else if (esAdmin) {
                    HomeAdmin(
                        onLogout = {
                            mostrarProductos = false
                            sesionIniciada = false
                            esAdmin = false
                        },
                        onAbrirHistorial = abrirHistorial,
                        onAbrirProductos = {
                            mostrarProductos = true
                        }
                    )
                } else {
                    HomeCliente(
                        nombre = "Cliente",
                        onLogout = {
                            mostrarProductos = false
                            sesionIniciada = false
                            esAdmin = false
                        },
                        onAbrirHistorial = abrirHistorial
                    )
                }
            }
        }
    }
}