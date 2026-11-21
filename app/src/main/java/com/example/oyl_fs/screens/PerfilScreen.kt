package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.viewmodel.PerfilViewModel

private val FondoPerfil = Color(0xFFFFF5E7)
private val CafePerfil = Color(0xFF92623D)
private val OscuroPerfil = Color(0xFF4B2E15)

// Los datos salen del usuario que inició sesión (backend), no de un perfil local.
// El parámetro usuarioId se mantiene para no cambiar las pantallas que lo llaman.
@Composable
fun PerfilUsuarioScreen(
    usuarioId: String,
    viewModel: PerfilViewModel,
    onVolver: () -> Unit,
    onLogout: () -> Unit
) {
    val usuario by viewModel.perfil.collectAsState()
    val error by viewModel.error.collectAsState()
    val guardando by viewModel.guardando.collectAsState()
    val perfil = usuario ?: return
    var pantalla by rememberSaveable(perfil.id) { mutableStateOf("perfil") }

    fun volver() {
        viewModel.limpiarError()
        pantalla = when (pantalla) {
            "editar" -> "datos"
            "datos" -> "perfil"
            else -> { onVolver(); "perfil" }
        }
    }
    BackHandler { volver() }

    when (pantalla) {
        "editar" -> EditarPerfilScreen(perfil, error, guardando, { volver() }) { nombre, correo, telefono, direccion ->
            viewModel.guardarPerfil(nombre, correo, telefono, direccion) {
                pantalla = "datos"
            }
        }
        "datos" -> DatosPersonalesScreen(perfil, { volver() }) {
            viewModel.limpiarError()
            pantalla = "editar"
        }
        else -> PerfilScreen(perfil, onVolver, { pantalla = "datos" }, onLogout)
    }
}

@Composable
fun PerfilScreen(
    perfil: UsuarioDto,
    onVolver: () -> Unit,
    onDatosPersonales: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(FondoPerfil)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.align(Alignment.Start)) {
            Text("‹ Volver", color = OscuroPerfil)
        }
        Surface(color = OscuroPerfil, shape = CircleShape, modifier = Modifier.size(90.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, "Perfil", tint = Color.White, modifier = Modifier.size(54.dp))
            }
        }
        Text(perfil.nombre, color = OscuroPerfil,
            fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(perfil.correo, color = CafePerfil)
        Text(if (perfil.rol == "ADMIN") "Cuenta de administrador" else "Cuenta de cliente", color = CafePerfil)
        Button(
            onClick = onDatosPersonales, modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)
        ) {
            Icon(Icons.Default.Person, null)
            Spacer(Modifier.width(10.dp))
            Text("Datos personales", Modifier.weight(1f))
            Text("›")
        }
        Divider(color = CafePerfil.copy(alpha = 0.2f))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)) { Text("Cerrar sesión") }
    }
}

@Composable
private fun DatosPersonalesScreen(
    perfil: UsuarioDto,
    onVolver: () -> Unit,
    onEditar: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(FondoPerfil)
        .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onVolver) { Text("‹ Datos personales", color = OscuroPerfil) }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DatoPerfil("Nombre", perfil.nombre)
                DatoPerfil("Usuario", perfil.alias)
                DatoPerfil("Correo", perfil.correo)
                DatoPerfil("Teléfono", perfil.telefono.orEmpty())
                if (perfil.rol != "ADMIN") DatoPerfil("Dirección", perfil.direccion.orEmpty())
            }
        }
        Button(onClick = onEditar, modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)) { Text("Editar datos") }
    }
}

@Composable
private fun DatoPerfil(titulo: String, valor: String) {
    Text(titulo, color = CafePerfil, fontSize = 12.sp)
    Text(valor.ifBlank { "No registrado" }, color = OscuroPerfil, fontWeight = FontWeight.Medium)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarPerfilScreen(
    perfil: UsuarioDto,
    error: String?,
    guardando: Boolean,
    onVolver: () -> Unit,
    onGuardar: (nombre: String, correo: String, telefono: String, direccion: String) -> Unit
) {
    var nombre by rememberSaveable(perfil.id) { mutableStateOf(perfil.nombre) }
    var correo by rememberSaveable(perfil.id) { mutableStateOf(perfil.correo) }
    var telefono by rememberSaveable(perfil.id) { mutableStateOf(perfil.telefono.orEmpty()) }
    var direccion by rememberSaveable(perfil.id) { mutableStateOf(perfil.direccion.orEmpty()) }
    Column(Modifier.fillMaxSize().background(FondoPerfil)
        .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("‹ Editar datos", color = OscuroPerfil) }
        CampoPerfil("Nombre *", nombre) { nombre = it }
        CampoPerfil("Correo *", correo) { correo = it }
        CampoPerfil("Teléfono", telefono) { telefono = it }
        if (perfil.rol != "ADMIN") CampoPerfil("Dirección", direccion) { direccion = it }
        AnimatedVisibility(visible = error != null, enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Text(error.orEmpty(), color = Color(0xFFC0392B))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = {
                onGuardar(nombre, correo, telefono, direccion)
            }, enabled = !guardando, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)) {
                Text(if (guardando) "Guardando..." else "Guardar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoPerfil(titulo: String, valor: String, onCambio: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = onCambio,
        label = { Text(titulo) }, singleLine = true,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = OscuroPerfil, containerColor = Color.White,
            focusedBorderColor = CafePerfil, unfocusedBorderColor = CafePerfil))
}

@Preview(showBackground = true)
@Composable
private fun PerfilClientePreview() {
    MaterialTheme {
        PerfilScreen(
            UsuarioDto(2, "Cliente", "cliente", "cliente@example.test", rol = "CLIENTE"),
            {}, {}, {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilAdminPreview() {
    MaterialTheme {
        PerfilScreen(
            UsuarioDto(1, "Administrador", "admin", "admin@example.test", rol = "ADMIN"),
            {}, {}, {}
        )
    }
}

