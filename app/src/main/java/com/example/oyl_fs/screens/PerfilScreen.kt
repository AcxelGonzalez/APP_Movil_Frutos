package com.example.oyl_fs.screens

import com.example.oyl_fs.ui.theme.EncabezadoOYL

import com.example.oyl_fs.ui.theme.MensajeErrorOYL

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Logout

import androidx.compose.foundation.BorderStroke

import com.example.oyl_fs.ui.theme.entradaOYL

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.oyl_fs.model.ErroresPerfil
import kotlinx.coroutines.launch
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
    val errores by viewModel.errores.collectAsState()
    val guardando by viewModel.guardando.collectAsState()
    val errorAvatar by viewModel.errorAvatar.collectAsState()
    val mensajeAvatar by viewModel.mensajeAvatar.collectAsState()
    val perfil = perfiles.find { it.id == usuarioId } ?: return
    var propietarioPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var generacionPendiente by rememberSaveable { mutableStateOf<Long?>(null) }
    val seleccionarAvatar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val propietario = propietarioPendiente
        val generacion = generacionPendiente
        propietarioPendiente = null
        generacionPendiente = null
        if (uri != null && propietario != null && generacion != null) {
            viewModel.cambiarAvatar(propietario, generacion, uri)
        }
    }
    LaunchedEffect(usuarioId) { viewModel.cargarAvatar(usuarioId) }
    var pantalla by rememberSaveable(usuarioId) { mutableStateOf("perfil") }

    fun volver() {
        if (guardando) return
        viewModel.limpiarError()
        pantalla = when (pantalla) {
            "editar" -> "datos"
            "datos", "permisos" -> "perfil"
            else -> { onVolver(); "perfil" }
        }
    }
    BackHandler { volver() }

    when (pantalla) {
        "editar" -> EditarPerfilScreen(perfil, errores, { volver() }) { editado ->
            if (viewModel.guardarPerfil(editado)) pantalla = "datos"
        }
        "permisos" -> PermisosPerfilScreen(onVolver = { volver() })
        "datos" -> DatosPersonalesScreen(perfil, { volver() }) {
            viewModel.limpiarError()
            pantalla = "editar"
        }
        else -> PerfilScreen(
            perfil, { if (!guardando) onVolver() }, { pantalla = "datos" }, onLogout,
            guardandoAvatar = guardando,
            errorAvatar = errorAvatar,
            mensajeAvatar = mensajeAvatar,
            onCambiarAvatar = {
                val generacion = viewModel.prepararSeleccionAvatar(usuarioId)
                if (generacion != null) {
                    propietarioPendiente = usuarioId
                    generacionPendiente = generacion
                    try { seleccionarAvatar.launch(arrayOf("image/*")) }
                    catch (_: Exception) {
                        propietarioPendiente = null
                        generacionPendiente = null
                        viewModel.errorSeleccionAvatar(usuarioId, generacion)
                    }
                }
            },
            onQuitarAvatar = { viewModel.quitarAvatar(usuarioId) },
            onPermisos = { if (!guardando) pantalla = "permisos" }
        )
    }
}

@Composable
fun PerfilScreen(
    perfil: PerfilUsuario,
    onVolver: () -> Unit,
    onDatosPersonales: () -> Unit,
    onLogout: () -> Unit,
    onCambiarAvatar: (() -> Unit)? = null,
    onQuitarAvatar: (() -> Unit)? = null,
    guardandoAvatar: Boolean = false,
    errorAvatar: String? = null,
    mensajeAvatar: String? = null,
    onPermisos: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxSize().background(FondoPerfil)
            .verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        EncabezadoOYL("Mi perfil", onVolver)
        Surface(color = Color.White, tonalElevation = 0.dp, shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFFEEDFCB))) {
            Column(Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = FondoPerfil, shape = CircleShape, modifier = Modifier.size(88.dp)) {
                    ImagenAvatarPerfil(perfil)
                }
                if (onCambiarAvatar != null) {
                    OutlinedButton(onClick = onCambiarAvatar, enabled = !guardandoAvatar) { Text("Cambiar avatar") }
                    if (perfil.avatarArchivo != null && onQuitarAvatar != null) {
                        TextButton(onClick = onQuitarAvatar, enabled = !guardandoAvatar) { Text("Quitar avatar") }
                    }
                    Text("Elige una imagen de demostración. Se guarda solo en este dispositivo.",
                        color = CafePerfil, fontSize = 12.sp)
                }
                if (guardandoAvatar) LinearProgressIndicator(Modifier.fillMaxWidth())
                errorAvatar?.let { MensajeErrorOYL(it) }
                mensajeAvatar?.let { Text(it, color = Color(0xFF1B5E20)) }
                Text("${perfil.nombre} ${perfil.apellido}".trim(), color = OscuroPerfil, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(perfil.correo, color = CafePerfil, fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Surface(color = Color(0xFFF3E8DA), shape = RoundedCornerShape(50)) {
                    Text(if (perfil.esAdmin) "Administrador" else "Cliente OYL",
                        color = OscuroPerfil, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
                }
            }
        }
        Button(
            onClick = onDatosPersonales, enabled = !guardandoAvatar,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)
        ) {
            Icon(Icons.Default.ManageAccounts, null)
            Spacer(Modifier.width(12.dp))
            Text("Datos personales", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Icon(Icons.Default.ChevronRight, null)
        }
        if (onPermisos != null) {
            OutlinedButton(onClick = onPermisos, enabled = !guardandoAvatar,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OscuroPerfil)) {
                Text("Permisos del dispositivo")
            }
        }
        OutlinedButton(onClick = onLogout, enabled = !guardandoAvatar,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            border = BorderStroke(1.dp, Color(0xFFD7BEA3)), shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OscuroPerfil)) {
            Icon(Icons.Default.Logout, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun DatosPersonalesScreen(
    perfil: PerfilUsuario,
    onVolver: () -> Unit,
    onEditar: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(FondoPerfil).entradaOYL("datos")
        .imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EncabezadoOYL("Datos personales", onVolver)
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
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
    errores: ErroresPerfil,
    onVolver: () -> Unit,
    onGuardar: suspend (PerfilUsuario) -> Unit
) {
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
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
        CampoPerfil("Nombre *", nombre, errores.nombre) { nombre = it }
        if (perfil.id.toLongOrNull() != null) Text("El RUT y el apellido se guardan solo en este dispositivo.", color = CafePerfil, fontSize = 12.sp)
        CampoPerfil("Apellido", apellido) { apellido = it }
        CampoPerfil("RUT de prueba", rut, errores.rut) { rut = it }
        CampoPerfil("Correo *", correo, errores.correo) { correo = it }
        CampoPerfil(if (perfil.id.toLongOrNull() != null) "Teléfono *" else "Teléfono", telefono, errores.telefono) { telefono = it }
        if (!perfil.esAdmin) CampoPerfil("Dirección de prueba", direccion) { direccion = it }
        AnimatedVisibility(visible = errores.general != null, enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Text(errores.general.orEmpty(), color = Color(0xFFC0392B))
        }
        Text("Utiliza únicamente datos ficticios para la demostración.", color = CafePerfil, fontSize = 12.sp)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            Button(onClick = {
                if (!guardando) {
                    guardando = true
                    val editado = perfil.copy(nombre = nombre, apellido = apellido, rut = rut,
                        correo = correo, telefono = telefono, direccion = direccion)
                    scope.launch {
                        try { onGuardar(editado) } finally { guardando = false }
                    }
                }
            }, enabled = !guardando, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OscuroPerfil)) { Text(if (guardando) "Guardando…" else "Guardar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoPerfil(titulo: String, valor: String, error: String? = null, onCambio: (String) -> Unit) {
    OutlinedTextField(value = valor, onValueChange = onCambio,
        label = { Text(titulo) }, singleLine = true, isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = OscuroPerfil, containerColor = Color.White,
            focusedBorderColor = CafePerfil, unfocusedBorderColor = Color(0xFFD5BEA6)))
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
