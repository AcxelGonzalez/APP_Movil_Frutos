package com.example.oyl_fs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.HomeScreen
import com.example.oyl_fs.ui.theme.OYL_FSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            OYL_FSTheme {

                // inicio sesion
                var sesionIniciada by rememberSaveable {
                    mutableStateOf(false)
                }
                // Si inició sesión, mostramos el menú principal
                if (sesionIniciada) {
                    HomeScreen(
                        onLogout = {
                            sesionIniciada = false
                        }
                    )

                } else {

                    // Si no inició sesión, mostramos el Login
                    LoginScreen(
                        onLoginSuccess = {
                            sesionIniciada = true
                        }
                    )
                }
            }
        }
    }
}
