package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.R
import androidx.compose.ui.tooling.preview.Preview
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.LoginUiState
import com.example.oyl_fs.viewmodel.LoginViewModel
import com.example.oyl_fs.viewmodel.SesionIdentificada

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: (SesionIdentificada) -> Unit,
    onIrARegistro: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    LaunchedEffect(Unit) { viewModel.limpiarError() }
    LoginContenido(
        estado = estado,
        onLimpiarError = viewModel::limpiarError,
        onIngresar = { usuario, contrasena ->
            viewModel.iniciarSesionRemota(usuario, contrasena, onLoginSuccess)
        },
        onIrARegistro = onIrARegistro
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContenido(
    estado: LoginUiState,
    onLimpiarError: () -> Unit,
    onIngresar: (String, String) -> Unit,
    onIrARegistro: () -> Unit
) {
    var usuario by rememberSaveable { mutableStateOf("") }
    // La contraseña solo vive en memoria mientras esta pantalla está abierta.
    var contrasena by remember { mutableStateOf("") }
    var verContrasena by remember { mutableStateOf(false) }
    var mostrarAyuda by remember { mutableStateOf(false) }
    val foco = LocalFocusManager.current
    val cafe = Color(0xFF4B2E15)
    val vistaPrevia = LocalInspectionMode.current
    val entrada = remember { Animatable(if (vistaPrevia) 1f else 0f) }
    val recorrido = with(LocalDensity.current) { 44.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!vistaPrevia) {
            withFrameNanos { }
            entrada.animateTo(1f, tween(850))
        }
    }
    val ingresar: () -> Unit = {
        foco.clearFocus()
        onIngresar(usuario, contrasena)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFFFF5E7))) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight).padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painterResource(R.drawable.logo_oyl_vector), "OYL Frutos Secos",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.width(220.dp).height(94.dp).graphicsLayer {
                        alpha = entrada.value
                        scaleX = 0.85f + entrada.value * 0.15f
                        scaleY = scaleX
                    }
                )
                Spacer(Modifier.height(24.dp))
                Card(
                    Modifier.widthIn(max = 440.dp).fillMaxWidth().graphicsLayer {
                        alpha = entrada.value
                        translationY = (1f - entrada.value) * recorrido
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Bienvenido", color = cafe, fontSize = 27.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                        Text("Inicia sesión para continuar en OYL.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color(0xFF92623D), fontSize = 14.sp, lineHeight = 20.sp)
                        OutlinedTextField(
                            value = usuario,
                            onValueChange = { usuario = it; onLimpiarError() },
                            label = { Text("Usuario") },
                            leadingIcon = { Icon(Icons.Default.PersonOutline, null) },
                            enabled = !estado.procesando, singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFFFFFBF5),
                                focusedBorderColor = cafe,
                                unfocusedBorderColor = Color(0xFFD8C4AE),
                                focusedLabelColor = cafe,
                                unfocusedLabelColor = Color(0xFF785334),
                                cursorColor = cafe
                            ),
                            isError = estado.error != null,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contrasena,
                            onValueChange = { contrasena = it; onLimpiarError() },
                            label = { Text("Contraseña") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            trailingIcon = {
                                IconButton(onClick = { verContrasena = !verContrasena }, enabled = !estado.procesando) {
                                    Icon(if (verContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        if (verContrasena) "Ocultar contraseña" else "Mostrar contraseña")
                                }
                            },
                            enabled = !estado.procesando, singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                containerColor = Color(0xFFFFFBF5),
                                focusedBorderColor = cafe,
                                unfocusedBorderColor = Color(0xFFD8C4AE),
                                focusedLabelColor = cafe,
                                unfocusedLabelColor = Color(0xFF785334),
                                cursorColor = cafe
                            ),
                            isError = estado.error != null,
                            visualTransformation = if (verContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { ingresar() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        AnimatedVisibility(estado.error != null,
                            enter = fadeIn(tween(450)) + expandVertically(tween(450)),
                            exit = fadeOut(tween(250)) + shrinkVertically(tween(250))) {
                            Surface(color = Color(0xFFFCE5E1), shape = RoundedCornerShape(12.dp)) {
                                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFC0392B), modifier = Modifier.size(20.dp))
                                    Text(estado.error.orEmpty(), color = Color(0xFF7E251B), fontSize = 13.sp)
                                }
                            }
                        }
                        Button(
                            onClick = ingresar, enabled = !estado.procesando,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cafe)
                        ) {
                            if (estado.procesando) {
                                CircularProgressIndicator(Modifier.size(20.dp), color = cafe, strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(if (estado.procesando) "Ingresando…" else "Iniciar sesión", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = onIrARegistro, enabled = !estado.procesando,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            shape = RoundedCornerShape(16.dp)) { Text("Crear una cuenta") }
                        TextButton(onClick = { mostrarAyuda = true }, enabled = !estado.procesando,
                            modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("¿Olvidaste tu contraseña?", color = cafe)
                        }
                    }
                }
            }
        }
    }
    if (mostrarAyuda) {
        AlertDialog(
            onDismissRequest = { mostrarAyuda = false },
            icon = { Icon(Icons.Default.LockReset, null, tint = cafe) },
            title = { Text("Recuperar el acceso") },
            text = { Text("Solicita al administrador de OYL que te ayude a restablecer tu contraseña. Ten a mano tu usuario y correo de registro.") },
            confirmButton = { TextButton(onClick = { mostrarAyuda = false }) { Text("Entendido") } }
        )
    }
}

@Preview(showBackground = true, name = "Login OYL", widthDp = 360, heightDp = 760)
@Preview(showBackground = true, name = "Login · texto grande", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun LoginPreview() {
    OYL_FSTheme {
        LoginContenido(LoginUiState(), {}, { _, _ -> }, {})
    }
}
