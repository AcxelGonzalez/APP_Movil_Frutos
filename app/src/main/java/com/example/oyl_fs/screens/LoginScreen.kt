package com.example.oyl_fs.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

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
    val foco = LocalFocusManager.current
    val cafe = Color(0xFF4A2C1A)
    val rojo = Color(0xFFC0392B)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().imePadding()) {

        Image(
            painter = painterResource(R.drawable.fondo_login_vector),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_oyl_vector),
                contentDescription = "OYL Frutos Secos",
                contentScale = ContentScale.Fit,
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth()
                    .height(110.dp).padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                    error = ""
                },
                keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                label = { Text("Usuario") },
                singleLine = true,
                isError = error.isNotEmpty(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Next
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { foco.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            // Mensaje de error (ocupa lugar solo cuando hay algo que decir)
            AnimatedVisibility(visible = error.isNotEmpty(), enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(error, color = rojo, fontSize = 13.sp)
            }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    foco.clearFocus()
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