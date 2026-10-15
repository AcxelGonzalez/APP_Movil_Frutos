package com.example.oyl_fs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider
import com.example.oyl_fs.screens.HomeScreen
import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.ProductosAdmin
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.ProductoViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comparte la lista entre catálogo, detalle y formularios.
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

                if (!sesionIniciada) {
                    LoginScreen(
                        onLoginSuccess = {
                            mostrarProductos = false
                            sesionIniciada = true
                        }
                    )
                } else if (mostrarProductos) {
                    ProductosAdmin(
                        viewModel = productoViewModel,
                        onVolver = { mostrarProductos = false }
                    )
                } else {
                    HomeScreen(
                        onLogout = {
                            mostrarProductos = false
                            sesionIniciada = false
                        },
                        onAbrirProductos = {
                            mostrarProductos = true
                        }
                    )
                }
            }
        }
    }
}
