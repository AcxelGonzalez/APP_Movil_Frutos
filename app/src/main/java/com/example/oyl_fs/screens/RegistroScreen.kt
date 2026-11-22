
package com.example.oyl_fs.screens

import com.example.oyl_fs.ui.theme.MensajeErrorOYL

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

import com.example.oyl_fs.ui.theme.entradaOYL

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.network.RegistroRequest
import com.example.oyl_fs.network.mensajeApi
import com.example.oyl_fs.repository.SesionRepository
import kotlinx.coroutines.launch

import com.example.oyl_fs.viewmodel.CampoRegistro
import com.example.oyl_fs.viewmodel.FormularioRegistro
import com.example.oyl_fs.viewmodel.RegistroViewModel


/**
 * Formulario para registrar clientes en Room.
 *
 * Las validaciones se realizan desde
 * RegistroViewModel y no desde Compose.
 *
 * ExperimentalMaterial3Api permite utilizar
 * OutlinedTextField con la versión actual
 * de Material 3 del proyecto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    viewModel: RegistroViewModel,
    onRegistrado: () -> Unit,
    onIrALogin: () -> Unit
) {
    var error by remember { mutableStateOf("") }
    var intentoEnvio by remember { mutableStateOf(false) }
    val correoValido = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    var nombre by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    // Mientras se espera la respuesta del servidor
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val cafe = Color(0xFF4A2C1A)
    val rojo = Color(0xFFC0392B)

    Column(
        modifier = Modifier
            .fillMaxSize().background(Color(0xFFFFF5E7)).entradaOYL()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Crear cuenta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = cafe
        )

        Text(
            text = "Registra tus datos para acceder a OYL Frutos Secos",
            fontSize = 13.sp,
            color = cafe
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = alias,
            onValueChange = {
                alias = it; error = ""
                viewModel.limpiarError(CampoRegistro.ALIAS)
            },
            label = { Text("Alias de usuario *") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            isError = intentoEnvio && (alias.trim().length !in 3..20),
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it; error = ""
                viewModel.limpiarError(CampoRegistro.NOMBRE)
            },
            label = { Text("Nombre completo *") },
            leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
            isError = intentoEnvio && (nombre.isBlank()),
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it; error = "" },
            label = { Text("Correo electrónico *") },
            isError = intentoEnvio && !correoValido.matches(correo.trim()),
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = {
                telefono = it; error = ""
                viewModel.limpiarError(CampoRegistro.TELEFONO)
            },
            label = { Text("Teléfono (opcional)") },
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            isError = error.isNotEmpty(),
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = direccion,
            onValueChange = { direccion = it; error = "" },
            label = { Text("Dirección (opcional)") },
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = {
                clave = it; error = ""
                viewModel.limpiarError(CampoRegistro.CLAVE)
            },
            label = { Text("Contraseña *") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            isError = intentoEnvio && (clave.length < 6),
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmacion,
            onValueChange = {
                confirmacion = it; error = ""
                viewModel.limpiarError(CampoRegistro.CONFIRMACION)
            },
            label = { Text("Confirmar contraseña *") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            isError = intentoEnvio && (clave != confirmacion),
            enabled = !cargando,
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White, focusedBorderColor = cafe,
                unfocusedBorderColor = Color(0xFFD8C4AE), cursorColor = cafe),
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedVisibility(visible = error.isNotEmpty(), enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Column {
                Spacer(Modifier.height(10.dp))
                MensajeErrorOYL(error)
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                intentoEnvio = true
                // Primero se validan los datos en el teléfono
                error = when {
                    nombre.isBlank() || alias.isBlank() || correo.isBlank() ->
                        "Completa todos los campos obligatorios"
                    alias.trim().length !in 3..20 ->
                        "El usuario debe tener entre 3 y 20 caracteres"
                    !correoValido.matches(correo.trim()) -> "Ingresa un correo válido"
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

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                viewModel.reiniciar()
                onIrALogin()
            },
            enabled = !cargando
        ) {
            Text(
                "¿Ya tienes cuenta? Iniciar sesión",
                color = cafe
            )
        }
    }
}
