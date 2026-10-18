
package com.example.oyl_fs.screens

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
    onPestanaAtendida: () -> Unit = {}
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


    // Controla la pantalla seleccionada
    var tab by rememberSaveable { mutableStateOf(0) }

    // El historial puede solicitar una pestaña del mismo menú.
    LaunchedEffect(pestanaSolicitada) {
        if (pestanaSolicitada != null && pestanaSolicitada in listOf(0, 1, 2, 4)) {
            tab = pestanaSolicitada
            onPestanaAtendida()
        }
    }


    // Controla la animación inicial
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    BackHandler(enabled = tab != 0) { tab = 0 }

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
                    tab = indice
                }
            }
        }

    ) { padding ->

        // Mostramos el contenido según la opción seleccionada
        when (tab) {

            // ---------- INICIO ----------
            0 -> {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) +
                            slideInVertically { it / 6 }
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
                            IconoCircular(Icons.Default.Notifications, "Notificaciones", onClick = {})
                            if (3 > 0) {
                                Badge(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp),
                                    containerColor = Color(0xFFB3261E), contentColor = Color.White) {
                                    Text(if (3 > 99) "99+" else "${3}", fontSize = 11.sp)
                                }
                            }
                        }
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
                                tab = 1
                            }


                            TarjetaDato(
                                "${productos.size}",
                                "Productos",
                                PrincipalOYL,
                                Modifier.weight(1f)
                            ) {
                                tab = 1
                            }
                        }

                        // ---------- ADMINISTRAR PRODUCTOS ----------
                        Button(
                            onClick = {
                                // Abre el módulo de productos
                                tab = 1
                            },

                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecundarioOYL
                            ),

                            shape = RoundedCornerShape(16.dp),

                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
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
                        Seccion("Atencion!!") {

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
                                            tab = 1
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
                                    shape = RoundedCornerShape(20.dp),
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
                            tab = 0
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
                            tab = 0
                        }
                    )
                }
            }

            // ---------- PERFIL ----------
            4 -> Box(Modifier.fillMaxSize().padding(padding)) {
                PerfilUsuarioScreen(
                    usuarioId = "admin-demo",
                    viewModel = perfilViewModel,
                    onVolver = { tab = 0 },
                    onLogout = onLogout
                )
            }

        } // Cierra when

    } // Cierra Scaffold

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

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(acento)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                numero,
                color = SecundarioOYL,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

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
        shape = RoundedCornerShape(20.dp),

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
        containerColor = Color.White
    ) {

        val items = listOf(
            "Inicio" to Icons.Default.Home,
            "Catalogo" to Icons.Default.List,
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
                        fontSize = 10.sp
                    )
                },

                icon = {
                    Icon(
                        icono,
                        contentDescription = texto
                    )
                },

                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SecundarioOYL,
                    selectedTextColor = SecundarioOYL,
                    indicatorColor = FondoOYL,
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
        modifier = modifier.fillMaxWidth().height(76.dp),
        shape = RoundedCornerShape(38.dp),
        color = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.logo_oyl_vector),
                contentDescription = "OYL Frutos Secos",
                // Ajuste visual: compensa el peso del texto bajo el símbolo.
                modifier = Modifier.fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .offset(y = (-2).dp)
            )
        }
    }
}
