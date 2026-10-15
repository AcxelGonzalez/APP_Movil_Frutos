package com.example.oyl_fs.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (Boolean) -> Unit,// true sera admin
                onIrARegistro:() -> Unit
) {
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val cafe = Color(0xFF4A2C1A)
    val rojo = Color(0xFFC0392B)

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.login_main),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                    error = ""
                },
                label = { Text("Usuario") },
                singleLine = true,
                isError = error.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = contrasena,
                onValueChange = {
                    contrasena = it
                    error = ""
                },
                label = { Text("Contraseña") },
                singleLine = true,
                isError = error.isNotEmpty(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            // Mensaje de error (ocupa lugar solo cuando hay algo que decir)
            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = rojo, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val u = usuario.trim().lowercase()
                    val c = contrasena.trim()
                    when {
                        u.isEmpty() || c.isEmpty() ->
                            error = "Completa usuario y contraseña"
                        u == "admin" && c == "admin" -> onLoginSuccess(true)
                        u == "cliente" && c == "cliente" -> onLoginSuccess(false)
                        else -> error = "Usuario o contraseña incorrectos"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = cafe),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar sesión")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onIrARegistro,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrarse", color = cafe)
            }

            TextButton(onClick = {}) {
                Text("¿Olvidaste tu contraseña?", color = cafe)
            }
        }
    }
}