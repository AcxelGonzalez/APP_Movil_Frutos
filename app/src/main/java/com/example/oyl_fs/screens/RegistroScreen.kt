package com.example.oyl_fs.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.network.RegistroRequest
import com.example.oyl_fs.network.mensajeApi
import com.example.oyl_fs.repository.SesionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    onRegistrado: () -> Unit,
    onIrALogin: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // Mientras se espera la respuesta del servidor
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cafe = Color(0xFF4A2C1A)
    val rojo = Color(0xFFC0392B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Crear cuenta", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = cafe)
        Text("Completa tus datos para registrarte", fontSize = 13.sp, color = cafe)

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // El alias es el nombre con el que se inicia sesión
        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it },
            label = { Text("Usuario (para iniciar sesión)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it },
            label = { Text("Dirección (opcional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmacion,
            onValueChange = { confirmacion = it },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = error.isNotEmpty(), enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Column {
                Spacer(Modifier.height(10.dp))
                Text(error, color = rojo, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                // Primero se validan los datos en el teléfono
                error = when {
                    nombre.isBlank() || alias.isBlank() || correo.isBlank() ->
                        "Completa todos los campos obligatorios"
                    alias.trim().length !in 3..20 ->
                        "El usuario debe tener entre 3 y 20 caracteres"
                    !correo.contains("@") -> "Ingresa un correo válido"
                    clave.length < 6 -> "La contraseña debe tener mínimo 6 caracteres"
                    clave != confirmacion -> "Las contraseñas no coinciden"
                    else -> ""
                }

                // Si todo está bien, se crea la cuenta en el backend (ms-usuarios)
                if (error.isEmpty()) {
                    cargando = true
                    scope.launch {
                        SesionRepository.registrar(
                            RegistroRequest(
                                nombre = nombre.trim(),
                                alias = alias.trim(),
                                correo = correo.trim(),
                                telefono = telefono.trim().ifEmpty { null },
                                direccion = direccion.trim().ifEmpty { null },
                                contrasena = clave
                            )
                        )
                            .onSuccess {
                                Toast.makeText(
                                    context,
                                    "Cuenta creada. Ya puedes iniciar sesión",
                                    Toast.LENGTH_LONG
                                ).show()
                                onRegistrado()
                            }
                            .onFailure { e ->
                                error = e.mensajeApi()
                            }
                        cargando = false
                    }
                }
            },
            enabled = !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = cafe),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (cargando) "Creando cuenta..." else "Registrarse")
        }

        TextButton(onClick = onIrALogin) {
            Text("¿Ya tienes cuenta? Iniciar Sesión", color = cafe)
        }
    }
}
