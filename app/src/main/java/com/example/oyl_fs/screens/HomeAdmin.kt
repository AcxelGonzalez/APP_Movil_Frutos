
package com.example.oyl_fs.screens

import com.example.oyl_fs.ui.theme.presionOYL
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind

import androidx.lifecycle.viewmodel.compose.viewModel as obtenerViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.Image
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.oyl_fs.R

import com.example.oyl_fs.viewmodel.ProductoViewModel
import com.example.oyl_fs.viewmodel.HistorialViewModel
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.ui.components.EstadoPedidoChip
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import com.example.oyl_fs.viewmodel.PerfilViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores de OYL Frutos Secos
private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)
private val AlertaOYL = Color(0xFFD9822B)
private val CriticoOYL = Color(0xFFC0392B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeAdmin(
    onLogout: () -> Unit,
    onAbrirHistorial: () -> Unit,
    viewModel: ProductoViewModel,
    historialViewModel: HistorialViewModel,
    perfilViewModel: PerfilViewModel,
    nombre: String = "Administrador",
    pestanaSolicitada: Int? = null,
    onPestanaAtendida: () -> Unit = {},
    pestanaSeleccionada: Int? = null,
    onSeleccionarPestana: ((Int) -> Unit)? = null
) {

    // Observa los productos actualizados del catálogo.
    val productos by viewModel.productos.collectAsState()

    // Detecta productos agotados o con stock
    // igual o inferior al mínimo establecido.
    val stockBajo = productos.filter { producto ->
        producto.stock <= producto.stockMinimo
    }

    // Observamos los pedidos reales registrados
    // en PedidoRepository.
    val pedidos by historialViewModel
        .pedidosParaAdmin
        .collectAsState()

    // Contamos únicamente pedidos pendientes.
    val cantidadPendientes = pedidos.count { pedido ->
        pedido.estado == EstadoPedido.PENDIENTE_VALIDACION
    }

    // Seleccionamos los tres pedidos más recientes.
    val pedidosRecientes = pedidos
        .sortedByDescending { it.fecha }
        .take(3)


    val perfil by perfilViewModel.perfil.collectAsState()
    val avisosViewModel: com.example.oyl_fs.viewmodel.NotificacionesAdminViewModel = obtenerViewModel()
    val leidas by avisosViewModel.leidas.collectAsState()
    val errorPedidos by historialViewModel.error.collectAsState()
    val errorProductos by viewModel.error.collectAsState()
    val cargandoProductos by viewModel.cargandoProductos.collectAsState()
    val avisos = com.example.oyl_fs.model.crearNotificacionesAdmin(pedidos, productos)
    val idsVigentes = avisos.map { it.id }.toSet()
    val sinLeer = avisos.count { it.id !in leidas }
    var mostrarAvisos by remember { mutableStateOf(false) }
    // Consulta nuevos avisos solo mientras el administrador está usando la app.
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(cicloDeVida, perfil?.id) {
        if (perfil?.rol == "ADMIN") {
            cicloDeVida.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    viewModel.refrescar()
                    historialViewModel.refrescar()
                    delay(30_000)
                }
            }
        }
    }
    val giroCampana = remember { androidx.compose.animation.core.Animatable(0f) }
    var cantidadAnterior by remember { mutableStateOf(0) }

    LaunchedEffect(perfil?.id) {
        avisosViewModel.configurarUsuario(perfil?.id?.toString())
    }
    LaunchedEffect(sinLeer) {
        if (sinLeer > cantidadAnterior) {
            cantidadAnterior = sinLeer
            giroCampana.animateTo(-14f, tween(100))
            giroCampana.animateTo(14f, tween(150))
            giroCampana.animateTo(-8f, tween(120))
            giroCampana.animateTo(0f, tween(120))
        } else {
            cantidadAnterior = sinLeer
            giroCampana.snapTo(0f)
        }
    }

    // Controla la pantalla seleccionada
    var tabLocal by rememberSaveable { mutableStateOf(0) }
    // MainActivity conserva la selección incluso al abrir módulos externos.
    val tab = pestanaSeleccionada ?: tabLocal
    fun seleccionarPestana(indice: Int) {
        if (onSeleccionarPestana != null) onSeleccionarPestana(indice)
        else tabLocal = indice
    }

    // El historial puede solicitar una pestaña del mismo menú.
    LaunchedEffect(pestanaSolicitada) {
        if (pestanaSolicitada != null && pestanaSolicitada in listOf(0, 1, 2, 4)) {
            seleccionarPestana(pestanaSolicitada)
            onPestanaAtendida()
        }
    }


    // Controla la animación inicial


    BackHandler(enabled = tab != 0) { seleccionarPestana(0) }

    Scaffold(
        containerColor = FondoOYL,

        // Barra inferior de navegación
        bottomBar = {
            BarraAdmin(tab) { indice ->

                if (indice == 3) {

                    // Abre la pantalla de historial de pedidos
                    onAbrirHistorial()

                } else {

                    // Cambia la pantalla seleccionada
                    seleccionarPestana(indice)
                }
            }
        }

    ) { padding ->

        // Mostramos el contenido según la opción seleccionada
        when (tab) {

            // ---------- INICIO ----------
            0 -> {
                AnimatedVisibility(
                    visible = true,
                    enter = androidx.compose.animation.EnterTransition.None,
                    exit = androidx.compose.animation.ExitTransition.None
                ) {

                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),

                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // ---------- ENCABEZADO ----------
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LogoInicio(modifier = Modifier.weight(1f))
                        Box(Modifier.size(64.dp)) {
                            Box(Modifier.graphicsLayer { rotationZ = giroCampana.value }) {
                                IconoCircular(Icons.Default.Notifications, "Notificaciones: $sinLeer sin leer", onClick = {
                                    mostrarAvisos = true
                                    viewModel.refrescar()
                                    historialViewModel.refrescar()
                                })
                            }
                            if (sinLeer > 0) {
                                Badge(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp),
                                    containerColor = Color(0xFFB3261E), contentColor = Color.White) {
                                    androidx.compose.animation.Crossfade(targetState = sinLeer, animationSpec = tween(350)) { cantidad ->
                                        Text(if (cantidad > 99) "99+" else "$cantidad", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Hola, ${nombre.trim().substringBefore(" ")}", color = SecundarioOYL,
                                fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp)

                        }

                    // ---------- INDICADORES ----------
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            TarjetaDato(
                                "${cantidadPendientes}",
                                "Pendientes",
                                AlertaOYL,
                                Modifier.weight(1f),
                                onClick = onAbrirHistorial
                            )

                            TarjetaDato(
                                "${stockBajo.size}",
                                "Stock bajo",
                                CriticoOYL,
                                Modifier.weight(1f)
                            ) {
                                seleccionarPestana(1)
                            }


                            TarjetaDato(
                                "${productos.size}",
                                "Productos",
                                PrincipalOYL,
                                Modifier.weight(1f)
                            ) {
                                seleccionarPestana(1)
                            }
                        }

                        // ---------- ADMINISTRAR PRODUCTOS ----------
                        Button(
                            onClick = {
                                // Abre el módulo de productos
                                seleccionarPestana(1)
                            },

                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecundarioOYL
                            ),

                            shape = RoundedCornerShape(16.dp),

                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                        ) {

                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White
                            )

                            Spacer(Modifier.width(8.dp))

                            Text("Administrar productos")
                        }

                        // ---------- PRODUCTOS CON ALERTAS ----------
                        Seccion("Estado del inventario") {

                            if (stockBajo.isEmpty()) {
                                Text(
                                    text = "No hay productos con stock bajo.",
                                    color = SecundarioOYL
                                )
                            }

                            stockBajo.forEach { p ->

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),

                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(FondoOYL),

                                        contentAlignment = Alignment.Center
                                    ) {

                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = null,

                                            tint = if (p.stock == 0) {
                                                CriticoOYL
                                            } else {
                                                AlertaOYL
                                            },

                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(Modifier.weight(1f)) {

                                        Text(
                                            p.nombre,
                                            color = SecundarioOYL,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Text(
                                            if (p.stock == 0) {
                                                "Sin stock"
                                            } else {
                                                "Quedan ${p.stock}"
                                            },

                                            color = if (p.stock == 0) {
                                                CriticoOYL
                                            } else {
                                                PrincipalOYL
                                            },

                                            fontSize = 12.sp
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            seleccionarPestana(1)
                                        },

                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = FondoOYL
                                        )
                                    ) {
                                        Text(
                                            "+ Stock",
                                            color = SecundarioOYL
                                        )
                                    }
                                }
                            }
                        }

                        // ---------- PEDIDOS RECIENTES ----------
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Pedidos recientes", color = SecundarioOYL,
                                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f))
                                TextButton(onClick = onAbrirHistorial) {
                                    Text("Ver todo →", color = PrincipalOYL)
                                }
                            }
                            if (pedidosRecientes.isEmpty()) {
                                Text("Todavía no hay pedidos.", color = PrincipalOYL)
                            }
                            pedidosRecientes.forEach { p ->
                                // Mismo estilo que Mi último pedido del Home cliente.
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Pedido", color = SecundarioOYL, fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f))
                                            Text(p.id, color = PrincipalOYL, fontSize = 13.sp)
                                        }
                                        Text("$" + "%,d".format(p.total).replace(',', '.'),
                                            color = SecundarioOYL, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        EstadoPedidoChip(p.estado, Modifier.widthIn(max = 280.dp))
                                        val indiceEstado = when (p.estado) {
                                            EstadoPedido.PENDIENTE_VALIDACION -> 0
                                            EstadoPedido.CONFIRMADO -> 1
                                            EstadoPedido.PREPARADO -> 2
                                            EstadoPedido.ENTREGADO -> 3
                                        }
                                        LinearProgressIndicator(
                                            progress = (indiceEstado + 1) / 4f,
                                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                                .clip(RoundedCornerShape(50)),
                                            color = PrincipalOYL,
                                            trackColor = FondoOYL
                                        )
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("Pendiente", "Confirmado", "Preparado", "Entregado")
                                                .forEachIndexed { indice, estado ->
                                                    Text(estado, fontSize = 10.sp,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        modifier = Modifier.weight(1f),
                                                        color = if (indice <= indiceEstado) SecundarioOYL
                                                            else PrincipalOYL.copy(alpha = 0.4f))
                                                }
                                        }
                                    }
                                }
                            }
                        }

                    } // Cierra Column

                } // Cierra AnimatedVisibility
            } // Cierra Inicio

            // ---------- CATÁLOGO DE PRODUCTOS ----------
            1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    ProductosAdmin(
                        viewModel = viewModel,
                        onVolver = {
                            seleccionarPestana(0)
                        }
                    )
                }
            }

            // ---------- INVENTARIO ----------
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    InventarioScreen(
                        viewModel = viewModel,
                        onVolver = {
                            seleccionarPestana(0)
                        }
                    )
                }
            }

            // ---------- PERFIL ----------
            4 -> Box(Modifier.fillMaxSize().padding(padding)) {
                PerfilUsuarioScreen(
                    usuarioId = "admin-demo",
                    viewModel = perfilViewModel,
                    onVolver = { seleccionarPestana(0) },
                    onLogout = onLogout
                )
            }

        } // Cierra when

    } // Cierra Scaffold

    if (mostrarAvisos) {
        AvisosAdministrador(
            avisos = avisos,
            leidas = leidas,
            error = errorPedidos ?: errorProductos,
            cargando = cargandoProductos,
            onActualizar = { viewModel.refrescar(); historialViewModel.refrescar() },
            onMarcarTodas = { avisosViewModel.marcarLeidas(idsVigentes, idsVigentes) },
            onAbrir = { aviso ->
                avisosViewModel.marcarLeidas(setOf(aviso.id), idsVigentes)
                mostrarAvisos = false
                when (aviso.destino) {
                    com.example.oyl_fs.model.DestinoNotificacion.PEDIDOS -> {
                        historialViewModel.seleccionarFiltro(EstadoPedido.PENDIENTE_VALIDACION)
                        onAbrirHistorial()
                    }
                    com.example.oyl_fs.model.DestinoNotificacion.INVENTARIO -> seleccionarPestana(2)
                }
            },
            onCerrar = { mostrarAvisos = false }
        )
    }
} // Cierra HomeAdmin


// ---------- ÍCONO CIRCULAR ----------
@Composable
private fun IconoCircular(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),

        contentAlignment = Alignment.Center
    ) {

        Icon(
            icono,
            contentDescription = descripcion,
            tint = SecundarioOYL,
            modifier = Modifier.size(32.dp)
        )
    }
}


// ---------- TARJETA DE INDICADOR ----------
@Composable
private fun TarjetaDato(
    numero: String,
    texto: String,
    acento: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {

    val interacciones = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.presionOYL(interacciones)
            .clickable(interactionSource = interacciones, indication = rememberRipple(), onClick = onClick),
        border = BorderStroke(1.dp, Color(0xFFEEDFCB)),
        shape = RoundedCornerShape(24.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(acento)
            )

            Spacer(Modifier.height(10.dp))

            androidx.compose.animation.Crossfade(numero, animationSpec = tween(450)) { valor ->
                Text(valor, color = SecundarioOYL, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                texto,
                color = PrincipalOYL,
                fontSize = 11.sp
            )
        }
    }
}


// ---------- SECCIÓN DEL HOME ----------
@Composable
private fun Seccion(
    titulo: String,
    accion: String? = null,
    onAccion: () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    titulo,
                    fontWeight = FontWeight.Bold,
                    color = SecundarioOYL,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                if (accion != null) {

                    TextButton(
                        onClick = onAccion
                    ) {
                        Text(
                            "$accion →",
                            color = PrincipalOYL
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            contenido()
        }
    }
}

// ---------- BARRA DE NAVEGACIÓN ----------
@Composable
fun BarraAdmin(
    seleccion: Int,
    onSelect: (Int) -> Unit
) {

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {

        val items = listOf(
            "Inicio" to Icons.Default.Home,
            "Catálogo" to Icons.Default.GridView,
            "Inventario" to Icons.Default.Inventory2,
            "Pedidos" to Icons.Default.ShoppingCart,
            "Perfil" to Icons.Default.Person
        )

        items.forEachIndexed { i, (texto, icono) ->

            NavigationBarItem(
                selected = seleccion == i,

                onClick = {
                    onSelect(i)
                },

                label = {
                    Text(
                        texto,
                        fontSize = 11.sp,
                        fontWeight = if (seleccion == i) FontWeight.SemiBold else FontWeight.Normal
                    )
                },

                icon = {
                    Icon(
                        icono,
                        contentDescription = texto
                    )
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = SecundarioOYL,
                    indicatorColor = SecundarioOYL,
                    unselectedIconColor = PrincipalOYL,
                    unselectedTextColor = PrincipalOYL
                )
            )
        }
    }
}

@Composable
private fun LogoInicio(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEDFCB))
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.logo_oyl_vector),
                contentDescription = "OYL Frutos Secos",
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().offset(y = (-2).dp)
            )
        }
    }
}

@Composable
private fun AvisosAdministrador(
    avisos: List<com.example.oyl_fs.model.NotificacionAdmin>,
    leidas: Set<String>,
    error: String?,
    cargando: Boolean,
    onActualizar: () -> Unit,
    onMarcarTodas: () -> Unit,
    onAbrir: (com.example.oyl_fs.model.NotificacionAdmin) -> Unit,
    onCerrar: () -> Unit
) {
    val entrada = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        entrada.animateTo(1f, tween(500))
    }
    AlertDialog(
        onDismissRequest = onCerrar,
        modifier = Modifier.graphicsLayer {
            alpha = entrada.value
            scaleX = 0.88f + entrada.value * 0.12f
            scaleY = scaleX
        },
        containerColor = FondoOYL,
        icon = { Icon(Icons.Default.Notifications, null, tint = SecundarioOYL) },
        title = { Text("Notificaciones", color = SecundarioOYL) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${avisos.count { it.id !in leidas }} sin leer", Modifier.weight(1f), color = PrincipalOYL)
                    TextButton(onClick = onActualizar, enabled = !cargando) { Text("Actualizar") }
                }
                if (cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = PrincipalOYL)
                if (error != null) {
                    Text("No se pudieron actualizar todos los datos. $error",
                        color = CriticoOYL, fontSize = 13.sp)
                }
                if (avisos.isEmpty()) {
                    Icon(Icons.Default.NotificationsNone, null, Modifier.align(Alignment.CenterHorizontally).size(44.dp), tint = PrincipalOYL)
                    Text(if (error == null) "No hay pedidos pendientes ni alertas de stock en los datos cargados."
                        else "Pulsa Actualizar para volver a consultar las alertas.", color = PrincipalOYL)
                }
                avisos.forEach { aviso ->
                    val sinLeer = aviso.id !in leidas
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(if (sinLeer) Color.White else Color(0xFFF2E5D4))
                        .clickable { onAbrir(aviso) }.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (aviso.destino == com.example.oyl_fs.model.DestinoNotificacion.PEDIDOS)
                                Icons.Default.ReceiptLong else Icons.Default.WarningAmber,
                                null, Modifier.size(20.dp), tint = PrincipalOYL)
                            Spacer(Modifier.width(8.dp))
                            Text(aviso.titulo, Modifier.weight(1f), color = SecundarioOYL, fontWeight = FontWeight.Bold)
                            if (sinLeer) Box(Modifier.size(8.dp).background(CriticoOYL, CircleShape))
                        }
                        Text(aviso.mensaje, color = PrincipalOYL, fontSize = 13.sp)
                        Text(if (aviso.destino == com.example.oyl_fs.model.DestinoNotificacion.PEDIDOS)
                            "Ver pedidos →" else "Ver inventario →", color = SecundarioOYL,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onMarcarTodas, enabled = avisos.any { it.id !in leidas }) {
                Text("Marcar leídas")
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } }
    )
}
