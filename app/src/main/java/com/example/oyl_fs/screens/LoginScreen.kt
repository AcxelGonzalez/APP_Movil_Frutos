
package com.example.oyl_fs.screens

import com.example.oyl_fs.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// credenciales admin
private const val USUARIO_ADMIN = "admin"
private const val CLAVE_ADMIN = "admin"

// paleta colores
private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)
private val Blanco = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {

    // variables para guardar los datos ingresados
    var usuario by rememberSaveable { mutableStateOf("") }
    var contrasena by rememberSaveable { mutableStateOf("") }

    // mensaje para mostrar errores
    var mensajeError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = FondoOYL
    ) { paddingValues ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // altura disponible de la pantalla
            val alturaPantalla = maxHeight

            // imagen fondo + logotipo
            Image(
                painter = painterResource(id = R.drawable.login_main),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // elementos: esto no lo entiendo (acxel)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),

                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // un espacio para no tapar el logo
                Spacer(
                    modifier = Modifier.height(alturaPantalla * 0.37f)
                )

                // cuadro usuario
                Text(
                    text = "usuario",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = SecundarioOYL,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                        mensajeError = null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Blanco,
                            RoundedCornerShape(50.dp)
                        )
                )

                Spacer(
                    modifier = Modifier.height(alturaPantalla * 0.07f)
                )

                // cuadro password
                Text(
                    text = "contraseña",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = SecundarioOYL,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = contrasena,
                    onValueChange = {
                        contrasena = it
                        mensajeError = null
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Blanco,
                            RoundedCornerShape(50.dp)
                        )
                )

                Spacer(
                    modifier = Modifier.height(alturaPantalla * 0.10f)
                )

                // boton
                Button(
                    onClick = {
                        // validamos no vacios
                        if (usuario.isBlank() || contrasena.isBlank()) {
                            mensajeError = "Completa todos los campos"
                        } else if (
                            usuario.trim() == USUARIO_ADMIN &&
                            contrasena == CLAVE_ADMIN
                        ) {
                            // Si los datos son correctos, ingresamos
                            mensajeError = null
                            onLoginSuccess()
                        } else {
                            // guarda error
                            mensajeError = "Usuario o contraseña incorrectos"
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecundarioOYL
                    )
                ) {
                    Text(
                        text = "Iniciar sesión",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Serif,
                        color = Blanco
                    )
                }

                // error al credenciales erroneas (este muestra el error en pantalla)
                mensajeError?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// Vista previa del Login
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginSuccess = {})
}
