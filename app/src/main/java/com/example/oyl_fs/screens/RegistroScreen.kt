
package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    val estado by viewModel.estado.collectAsState()
    val errores = estado.errores

    var alias by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var rut by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }

    // No almacenar las contraseñas mediante
    // rememberSaveable ni SavedState.
    var clave by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }

    val cafe = androidx.compose.ui.graphics.Color(0xFF4A2C1A)
    val errorColor = MaterialTheme.colorScheme.error

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                alias = it
                viewModel.limpiarError(CampoRegistro.ALIAS)
            },
            label = { Text("Alias de usuario *") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            isError = errores.alias != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.alias)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                viewModel.limpiarError(CampoRegistro.NOMBRE)
            },
            label = { Text("Nombre completo *") },
            leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
            isError = errores.nombre != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.nombre)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = {
                correo = it
                viewModel.limpiarError(CampoRegistro.CORREO)
            },
            label = { Text("Correo electrónico *") },
            leadingIcon = { Icon(Icons.Default.Email, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            isError = errores.correo != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.correo)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = rut,
            onValueChange = {
                rut = it
                viewModel.limpiarError(CampoRegistro.RUT)
            },
            label = { Text("RUT *") },
            placeholder = { Text("12.345.678-5") },
            leadingIcon = { Icon(Icons.Default.Badge, null) },
            isError = errores.rut != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.rut)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = {
                telefono = it
                viewModel.limpiarError(CampoRegistro.TELEFONO)
            },
            label = { Text("Teléfono (opcional)") },
            leadingIcon = { Icon(Icons.Default.Phone, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            isError = errores.telefono != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.telefono)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = {
                clave = it
                viewModel.limpiarError(CampoRegistro.CLAVE)
            },
            label = { Text("Contraseña *") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            isError = errores.clave != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.clave)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmacion,
            onValueChange = {
                confirmacion = it
                viewModel.limpiarError(CampoRegistro.CONFIRMACION)
            },
            label = { Text("Confirmar contraseña *") },
            leadingIcon = { Icon(Icons.Default.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            isError = errores.confirmacion != null,
            enabled = !estado.procesando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        ErrorRegistroCampo(errores.confirmacion)

        AnimatedVisibility(
            visible = errores.general != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = errores.general.orEmpty(),
                color = errorColor,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                viewModel.registrar(
                    formulario = FormularioRegistro(
                        alias = alias,
                        nombre = nombre,
                        correo = correo,
                        rut = rut,
                        telefono = telefono,
                        clave = clave,
                        confirmacion = confirmacion
                    ),
                    onRegistroExitoso = onRegistrado
                )
            },
            enabled = !estado.procesando,
            colors = ButtonDefaults.buttonColors(
                containerColor = cafe
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        ) {
            if (estado.procesando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Registrarse")
            }
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = {
                viewModel.reiniciar()
                onIrALogin()
            },
            enabled = !estado.procesando
        ) {
            Text(
                "¿Ya tienes cuenta? Iniciar sesión",
                color = cafe
            )
        }
    }
}

/**
 * Mensaje visual reutilizable debajo
 * de los campos con errores.
 */
@Composable
private fun ErrorRegistroCampo(mensaje: String?) {

    AnimatedVisibility(
        visible = mensaje != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Text(
            text = mensaje.orEmpty(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
    }
}
