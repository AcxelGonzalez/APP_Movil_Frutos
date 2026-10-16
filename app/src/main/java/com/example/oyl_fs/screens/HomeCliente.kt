
package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.oyl_fs.viewmodel.CarritoViewModel
import com.example.oyl_fs.viewmodel.ProductoViewModel

private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)

data class Promo(val titulo: String, val detalle: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun HomeCliente(
    nombre: String = "Cliente",
    clienteId: String,
    productoViewModel: ProductoViewModel,
    carritoViewModel: CarritoViewModel,
    onLogout: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirCarrito: () -> Unit
) {
    // Datos de prueba (luego vendrán del ViewModel / SQLite)
    val promos = listOf(
        Promo("20% en nueces", "Solo esta semana"),
        Promo("Mix energético", "Llevando 2 unidades"),
        Promo("Despacho gratis", "Sobre $20.000")
    )

    val categorias = listOf("Todos", "Frutos secos", "Semillas", "Deshidratados")


// Observamos los productos administrados por el equipo.
    val productos by productoViewModel.productos.collectAsState()

// Categoría seleccionada por el cliente.
    var categoria by remember { mutableStateOf(0) }

// Muestra productos activos y con stock disponible.
    val destacados = productos.filter { producto ->
        producto.disponible &&
                producto.stock > 0 &&
                (categoria == 0 ||
                        producto.categoria == categorias[categoria])
    }

// Observamos el carrito para actualizar el contador.
    val estadoCarrito by carritoViewModel.uiState.collectAsState()

// Observamos mensajes de éxito y error.
    val mensajeCarrito by carritoViewModel.mensaje.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

// Identificamos al cliente en el ViewModel.
    LaunchedEffect(clienteId) {
        carritoViewModel.iniciar(clienteId)
    }

// Mostramos mensajes al agregar productos.
    LaunchedEffect(mensajeCarrito) {
        val texto = mensajeCarrito ?: return@LaunchedEffect

        carritoViewModel.mensajeMostrado()
        snackbarHostState.showSnackbar(texto)
    }


    var tab by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Scaffold(
        containerColor = FondoOYL,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },

        bottomBar = {
            BarraCliente(tab) { indice ->

                // La pestaña Pedidos tiene índice 3.
                if (indice == 3) {
                    onAbrirHistorial()
                } else {
                    // Las demás pestañas conservan su comportamiento.
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

                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // ---------- Encabezado ----------
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Card(
                        // Temporal: así puedes probar el cierre de sesión.
                        // Cuando exista PerfilScreen, cambia esto por { tab = 4 }
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLogout() },

                        shape = RoundedCornerShape(50),

                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Row(
                            Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(FondoOYL),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    null,
                                    tint = SecundarioOYL
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(Modifier.weight(1f)) {

                                Text(
                                    "Hola,",
                                    color = PrincipalOYL,
                                    fontSize = 12.sp
                                )

                                Text(
                                    nombre,
                                    fontWeight = FontWeight.Bold,
                                    color = SecundarioOYL,
                                    fontSize = 15.sp
                                )
                            }

                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                null,
                                tint = PrincipalOYL
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    // ---------- Carrito de compras ----------
                    BadgedBox(
                        badge = {
                            if (estadoCarrito.cantidadProductos > 0) {
                                Badge {
                                    Text("${estadoCarrito.cantidadProductos}")
                                }
                            }
                        }
                    ) {
                        IconoCircular(
                            Icons.Default.ShoppingCart,
                            "Carrito",
                            onClick = onAbrirCarrito
                        )
                    }
                }

                // ---------- abrir catalogo ----------
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { tab = 1 },

                    shape = RoundedCornerShape(50),

                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Row(
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 14.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Default.Search,
                            null,
                            tint = PrincipalOYL
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            "Buscar productos...",
                            color = PrincipalOYL
                        )
                    }
                }

                // ---------- promos ----------
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(promos) { promo ->

                        Card(
                            modifier = Modifier
                                .width(250.dp)
                                .height(105.dp),

                            shape = RoundedCornerShape(20.dp),

                            colors = CardDefaults.cardColors(
                                containerColor = PrincipalOYL
                            )
                        ) {

                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),

                                verticalArrangement = Arrangement.Center
                            ) {

                                Text(
                                    promo.titulo,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    promo.detalle,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // ---------- categorías ----------
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(categorias.size) { i ->

                        CategoriaChip(
                            texto = categorias[i],
                            seleccionado = categoria == i,
                            onClick = { categoria = i }
                        )
                    }
                }

                // ---------- destacado ----------
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        "Destacados",
                        fontWeight = FontWeight.Bold,
                        color = SecundarioOYL,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = { tab = 1 }
                    ) {
                        Text(
                            "Ver todo →",
                            color = PrincipalOYL
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(destacados, key = { it.id }) { p ->

                        Card(
                            modifier = Modifier.width(150.dp),

                            shape = RoundedCornerShape(18.dp),

                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {

                            Column(
                                Modifier.padding(12.dp)
                            ) {

                                // Marcador de imagen (después: foto del producto)
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(85.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(FondoOYL),

                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        Icons.Default.Inventory2,
                                        null,
                                        tint = PrincipalOYL
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    p.nombre,
                                    fontWeight = FontWeight.Bold,
                                    color = SecundarioOYL
                                )

                                Text(
                                    p.presentacion,
                                    fontSize = 12.sp,
                                    color = PrincipalOYL
                                )

                                Spacer(Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        "$" + "%,d"
                                            .format(p.precio)
                                            .replace(',', '.'),

                                        fontWeight = FontWeight.Bold,
                                        color = SecundarioOYL,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Box(
                                        Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SecundarioOYL)
                                            .clickable {
                                                carritoViewModel.agregarProducto(p)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {

                                        Icon(
                                            Icons.Default.Add,
                                            "Agregar al carrito",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------- ultimos pedidos ----------
                Card(
                    shape = RoundedCornerShape(20.dp),

                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {

                    Column(
                        Modifier.padding(16.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                "Mi último pedido",
                                fontWeight = FontWeight.Bold,
                                color = SecundarioOYL,
                                fontSize = 18.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                "#Pedido 3",
                                color = PrincipalOYL,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = 0.75f,   // 3 de 4 pasos
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = PrincipalOYL,
                            trackColor = FondoOYL
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            listOf(
                                "Pendiente",
                                "Confirmado",
                                "Preparado",
                                "Entregado"
                            ).forEachIndexed { i, paso ->

                                Text(
                                    paso,
                                    fontSize = 10.sp,

                                    color = if (i <= 2) {
                                        SecundarioOYL
                                    } else {
                                        PrincipalOYL.copy(alpha = 0.4f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconoCircular(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit
) {

    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),

        contentAlignment = Alignment.Center
    ) {

        Icon(
            icono,
            contentDescription = descripcion,
            tint = SecundarioOYL,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CategoriaChip(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {

    Text(
        texto,
        color = if (seleccionado) Color.White else PrincipalOYL,
        fontSize = 13.sp,
        fontWeight = if (seleccionado) {
            FontWeight.Bold
        } else {
            FontWeight.Normal
        },

        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (seleccionado) {
                    SecundarioOYL
                } else {
                    Color.White
                }
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 9.dp
            )
    )
}

@Composable
private fun BarraCliente(
    seleccion: Int,
    onSelect: (Int) -> Unit
) {

    NavigationBar(
        containerColor = Color.White
    ) {

        val items = listOf(
            "Inicio" to Icons.Default.Home,
            "Catálogo" to Icons.Default.List,
            "Tiendas" to Icons.Default.Storefront,
            "Pedidos" to Icons.Default.Receipt,
            "Perfil" to Icons.Default.Person
        )

        items.forEachIndexed { i, (texto, icono) ->

            NavigationBarItem(
                selected = seleccion == i,
                onClick = { onSelect(i) },

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
