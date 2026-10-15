package com.example.oyl_fs.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)
private val AlertaOYL = Color(0xFFD9822B)
private val CriticoOYL = Color(0xFFC0392B)

data class PedidoResumen(val id: Int, val total: Int, val estado: String)
data class ProductoStock(val nombre: String, val stock: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun HomeAdmin(
    onLogout: () -> Unit,
    onAbrirHistorial: () -> Unit
) {

    // Datos de prueba (luego vendrán del ViewModel / SQLite)
    val pedidos = listOf(
        PedidoResumen(1, 12500, "Pendiente"),
        PedidoResumen(2, 8900, "Confirmado"),
        PedidoResumen(3, 15300, "Preparado")
    )
    val stockBajo = listOf(
        ProductoStock("Almendras 500g", 0),
        ProductoStock("Nueces 1kg", 2),
        ProductoStock("Pasas 250g", 0)
    )

    var tab by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = FondoOYL,

        bottomBar = {
            BarraAdmin(tab) { indice ->

                if (indice == 3) {
                    onAbrirHistorial()
                } else {
                    tab = indice
                }
            }
        }

    ) { padding ->
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically { it / 6 }
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // ---------- Encabezado ----------
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        // Temporal: así puedes probar el cierre de sesión.
                        // Cuando exista PerfilScreen, cambia esto por { tab = 4 }
                        modifier = Modifier.weight(1f).clickable { onLogout() },
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(FondoOYL),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Person, null, tint = SecundarioOYL) }

                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Hola,", color = PrincipalOYL, fontSize = 12.sp)
                                Text(
                                    "Admin",
                                    fontWeight = FontWeight.Bold,
                                    color = SecundarioOYL,
                                    fontSize = 15.sp
                                )
                            }
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = PrincipalOYL)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    BadgedBox(badge = { Badge { Text("3") } }) {
                        IconoCircular(Icons.Default.Notifications, "Notificaciones") { }
                    }
                }

                // ---------- Indicadores ----------
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaDato(
                        "${pedidos.size}",
                        "Pendientes",
                        AlertaOYL,
                        Modifier.weight(1f),
                        onClick = onAbrirHistorial
                    )
                    TarjetaDato("${stockBajo.size}", "Stock bajo", CriticoOYL, Modifier.weight(1f)) { tab = 2 }
                    TarjetaDato("24", "Productos", PrincipalOYL, Modifier.weight(1f)) { tab = 1 }
                }

                // ---------- administrar productos (CRUD) ----------
                Button(
                    onClick = { /* Ir a formulario de Productos */ },
                    colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Administrar productos")
                }


                // ---------- productos alerta ----------
                Seccion("Atencion!!") {
                    stockBajo.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(FondoOYL),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    null,
                                    tint = if (p.stock == 0) CriticoOYL else AlertaOYL,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.nombre, color = SecundarioOYL, fontWeight = FontWeight.Medium)
                                Text(
                                    if (p.stock == 0) "Sin stock" else "Quedan ${p.stock}",
                                    color = if (p.stock == 0) CriticoOYL else PrincipalOYL,
                                    fontSize = 12.sp
                                )
                            }
                            FilledTonalButton(
                                onClick = { tab = 2 },   // Ir a InventarioScreen
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = FondoOYL)
                            ) { Text("+ Stock", color = SecundarioOYL) }
                        }
                    }
                }

                // ---------- Pedidos recientes ----------
                Seccion(
                    "Pedidos recientes",
                    accion = "Ver todo",
                    onAccion = onAbrirHistorial
                ) {
                    pedidos.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#Pedido ${p.id}", color = SecundarioOYL, modifier = Modifier.weight(1f))
                            EstadoChip(p.estado)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "$" + "%,d".format(p.total).replace(',', '.'),
                                fontWeight = FontWeight.Bold,
                                color = SecundarioOYL
                            )
                        }
                        Divider(color = FondoOYL)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconoCircular(icono: ImageVector, descripcion: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = descripcion, tint = SecundarioOYL, modifier = Modifier.size(20.dp))
    }
}

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
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .width(26.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(acento)
            )
            Spacer(Modifier.height(10.dp))
            Text(numero, color = SecundarioOYL, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(texto, color = PrincipalOYL, fontSize = 11.sp)
        }
    }
}

@Composable
private fun Seccion(
    titulo: String,
    accion: String? = null,
    onAccion: () -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    titulo,
                    fontWeight = FontWeight.Bold,
                    color = SecundarioOYL,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                if (accion != null) {
                    TextButton(onClick = onAccion) { Text("$accion →", color = PrincipalOYL) }
                }
            }
            Spacer(Modifier.height(8.dp))
            contenido()
        }
    }
}

@Composable
private fun EstadoChip(estado: String) {
    val color = when (estado) {
        "Pendiente" -> AlertaOYL
        "Confirmado" -> PrincipalOYL
        "Preparado" -> Color(0xFF6B8E23)
        else -> Color(0xFF2E7D32) // Entregado
    }
    Text(
        estado,
        color = Color.White,
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun BarraAdmin(seleccion: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
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
                onClick = { onSelect(i) },
                label = { Text(texto, fontSize = 10.sp) },
                icon = { Icon(icono, contentDescription = texto) },
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