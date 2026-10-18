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
import com.example.oyl_fs.model.PerfilUsuario
import com.example.oyl_fs.viewmodel.PerfilViewModel

private val FondoPerfil = Color(0xFFFFF5E7)
private val CafePerfil = Color(0xFF92623D)
private val OscuroPerfil = Color(0xFF4B2E15)

@Composable
fun PerfilUsuarioScreen(
    usuarioId: String,
    viewModel: PerfilViewModel,
    onVolver: () -> Unit,
    onLogout: () -> Unit
) {
    val perfiles by viewModel.perfiles.collectAsState()
    val error by viewModel.error.collectAsState()
    val perfil = perfiles.find { it.id == usuarioId } ?: return
    var pantalla by rememberSaveable(usuarioId) { mutableStateOf("perfil") }

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
        "editar" -> EditarPerfilScreen(perfil, error, { volver() }) { editado ->
            if (viewModel.guardarPerfil(editado)) pantalla = "datos"
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
    perfil: PerfilUsuario,
    onVolver: () -> Unit,
    onDatosPersonales: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(FondoPerfil)
            .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
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
        Text("${perfil.nombre} ${perfil.apellido}".trim(), color = OscuroPerfil,
            fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(perfil.correo, color = CafePerfil)
        Text(if (perfil.esAdmin) "Cuenta de administrador" else "Cuenta de cliente", color = CafePerfil)
        Button(
            onClick = onDatosPersonales, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)
        ) {
            Icon(Icons.Default.Person, null)
            Spacer(Modifier.width(10.dp))
            Text("Datos personales", Modifier.weight(1f))
            Text("›")
        }
        Divider(color = CafePerfil.copy(alpha = 0.2f))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)) { Text("Cerrar sesión") }
    }
}

@Composable
private fun DatosPersonalesScreen(
    perfil: PerfilUsuario,
    onVolver: () -> Unit,
    onEditar: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(FondoPerfil)
        .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TextButton(onClick = onVolver) { Text("‹ Datos personales", color = OscuroPerfil) }
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DatoPerfil("Nombre", perfil.nombre)
                DatoPerfil("Apellido", perfil.apellido)
                DatoPerfil("RUT", perfil.rut)
                DatoPerfil("Correo", perfil.correo)
                DatoPerfil("Teléfono", perfil.telefono)
                if (!perfil.esAdmin) DatoPerfil("Dirección", perfil.direccion)
            }
        }
        Button(onClick = onEditar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
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
    perfil: PerfilUsuario,
    error: String?,
    onVolver: () -> Unit,
    onGuardar: (PerfilUsuario) -> Unit
) {
    var nombre by rememberSaveable(perfil.id) { mutableStateOf(perfil.nombre) }
    var apellido by rememberSaveable(perfil.id) { mutableStateOf(perfil.apellido) }
    var rut by rememberSaveable(perfil.id) { mutableStateOf(perfil.rut) }
    var correo by rememberSaveable(perfil.id) { mutableStateOf(perfil.correo) }
    var telefono by rememberSaveable(perfil.id) { mutableStateOf(perfil.telefono) }
    var direccion by rememberSaveable(perfil.id) { mutableStateOf(perfil.direccion) }
    Column(Modifier.fillMaxSize().background(FondoPerfil)
        .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onVolver) { Text("‹ Editar datos", color = OscuroPerfil) }
        CampoPerfil("Nombre *", nombre) { nombre = it }
        CampoPerfil("Apellido", apellido) { apellido = it }
        CampoPerfil("RUT de prueba", rut) { rut = it }
        CampoPerfil("Correo *", correo) { correo = it }
        CampoPerfil("Teléfono", telefono) { telefono = it }
        if (!perfil.esAdmin) CampoPerfil("Dirección de prueba", direccion) { direccion = it }
        AnimatedVisibility(visible = error != null, enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Text(error.orEmpty(), color = Color(0xFFC0392B))
        }
        Text("Utiliza únicamente datos ficticios para la demostración.", color = CafePerfil, fontSize = 12.sp)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            Button(onClick = {
                onGuardar(perfil.copy(nombre = nombre, apellido = apellido, rut = rut,
                    correo = correo, telefono = telefono, direccion = direccion))
            }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)) { Text("Guardar") }
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
    MaterialTheme { PerfilScreen(PerfilUsuario("cliente-demo", false, "Cliente", correo = "cliente@example.test"), {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun PerfilAdminPreview() {
    MaterialTheme { PerfilScreen(PerfilUsuario("admin-demo", true, "Administrador", correo = "admin@example.test"), {}, {}, {}) }
}
