
package com.example.oyl_fs.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

import androidx.compose.foundation.Image
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.example.oyl_fs.R

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
import com.example.oyl_fs.viewmodel.CarritoViewModel
import com.example.oyl_fs.viewmodel.ProductoViewModel
import com.example.oyl_fs.viewmodel.HistorialViewModel
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.ProductoInfo

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
    historialViewModel: HistorialViewModel,
    perfilViewModel: PerfilViewModel,
    onLogout: () -> Unit,
    onAbrirHistorial: () -> Unit,
    onAbrirCarrito: () -> Unit,
    pestanaSolicitada: Int? = null,
    onPestanaAtendida: () -> Unit = {}
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

// Observa el último pedido del cliente actual.
// remember evita recrear el Flow en cada recomposición.
    val ultimoPedido by remember(
        historialViewModel,
        clienteId
    ) {
        historialViewModel.observarUltimoPedidoCliente(
            clienteId
        )
    }.collectAsState(initial = null)

// Observamos el carrito para actualizar el contador.
    val estadoCarrito by carritoViewModel.uiState.collectAsState()

// Observamos mensajes de éxito y error.
    val mensajeCarrito by carritoViewModel.mensaje.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val escalaCarrito = remember { Animatable(1f) }
    val animaciones = rememberCoroutineScope()


// Identificamos al cliente en el ViewModel.
    LaunchedEffect(clienteId) {
        carritoViewModel.iniciar(clienteId)
    }

// Mostramos mensajes al agregar productos.
    LaunchedEffect(mensajeCarrito) {
        val texto = mensajeCarrito ?: return@LaunchedEffect

        // Latido breve únicamente cuando se agregó correctamente un producto.
        if (texto.endsWith("agregado al carrito")) {
            animaciones.launch {
                escalaCarrito.snapTo(1f)
                escalaCarrito.animateTo(1.2f, tween(250))
                escalaCarrito.animateTo(1f, tween(250))
                escalaCarrito.animateTo(1.14f, tween(220))
                escalaCarrito.animateTo(1f, tween(220))
            }
        }
        carritoViewModel.mensajeMostrado()
        snackbarHostState.showSnackbar(texto)
    }


    var tab by rememberSaveable { mutableStateOf(0) }

    // El historial puede solicitar una pestaña del mismo menú.
    LaunchedEffect(pestanaSolicitada) {
        if (pestanaSolicitada != null && pestanaSolicitada in listOf(0, 1, 2, 4)) {
            tab = pestanaSolicitada
            onPestanaAtendida()
        }
    }


    // identificar producto
    var productoDetalleId by rememberSaveable {
        mutableStateOf<Int?>(null)
    }


    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    BackHandler(enabled = tab != 0) {
        if (tab == 5) {
            // Salimos del detalle y volvemos al inicio.
            productoDetalleId = null
        }

        tab = 0
    }

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

        when (tab) {
            /*
             * PESTAÑA TIENDAS
             *
             * Integra MapaScreen dentro de HomeCliente.
             *
             * El mapa obtiene sus puntos desde MapaViewModel
             * y PuntoRetiroRepository.
             *
             * El botón Volver regresa al inicio del cliente.
             */
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    MapaScreen(
                        onVolver = {
                            tab = 0
                        }
                    )
                }
            }

            1 -> Box(Modifier.fillMaxSize().padding(padding)) {
                ProductosCliente(
                    productoViewModel = productoViewModel,
                    carritoViewModel = carritoViewModel,
                    onVolver = { tab = 0 },
                    onAbrirCarrito = onAbrirCarrito
                )
            }

            // ---------- DETALLE DEL PRODUCTO ----------
            5 -> {
                val seleccionado = productos.find {
                    it.id == productoDetalleId && it.disponible
                }

                if (seleccionado != null) {

                    // Descontamos las unidades que ya están en el carrito.
                    val enCarrito = estadoCarrito.items.find {
                        it.productoId == seleccionado.id.toString()
                    }?.cantidad ?: 0

                    val unidadesDisponibles =
                        (seleccionado.stock - enCarrito).coerceAtLeast(0)

                    DetalleProductoClienteScreen(
                        producto = seleccionado,
                        disponibleParaAgregar = unidadesDisponibles,

                        onVolver = {
                            productoDetalleId = null
                            tab = 0
                        },

                        onAgregar = { cantidad ->
                            carritoViewModel.agregarProducto(
                                seleccionado,
                                cantidad
                            )
                        },

                        onAbrirCarrito = onAbrirCarrito
                    )

                } else {
                    LaunchedEffect(productoDetalleId) {
                        productoDetalleId = null
                        tab = 0
                    }
                }
            }

            4 -> Box(Modifier.fillMaxSize().padding(padding)) {
                PerfilUsuarioScreen(
                    usuarioId = clienteId,
                    viewModel = perfilViewModel,
                    onVolver = { tab = 0 },
                    onLogout = onLogout
                )
            }
            else -> AnimatedVisibility(
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LogoInicio(modifier = Modifier.weight(1f))
                        Box(Modifier.size(64.dp).graphicsLayer {
                            scaleX = escalaCarrito.value
                            scaleY = escalaCarrito.value
                        }) {
                            IconoCircular(Icons.Default.ShoppingCart, "Carrito", onClick = onAbrirCarrito)
                            if (estadoCarrito.cantidadProductos > 0) {
                                Badge(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 2.dp),
                                    containerColor = Color(0xFFB3261E), contentColor = Color.White) {
                                    Text(if (estadoCarrito.cantidadProductos > 99) "99+" else "${estadoCarrito.cantidadProductos}", fontSize = 11.sp)
                                }
                            }
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

                                    ImagenProducto(
                                        producto = p,
                                        modifier = Modifier.fillMaxWidth().height(85.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    TextoTarjetaProducto(
                                        p.nombre,
                                        fontWeight = FontWeight.Bold,
                                        color = SecundarioOYL
                                    )

                                    TextoTarjetaProducto(
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
                                                    productoDetalleId = p.id
                                                    tab = 5
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {

                                            Icon(
                                                Icons.Default.Add,
                                                "Ver detalle del producto",
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
                                    ultimoPedido?.id ?: "Sin pedidos",
                                    color = PrincipalOYL,
                                    fontSize = 13.sp
                                )
                            }


                            ultimoPedido?.let { pedido ->

                                // Posición correspondiente al estado actual.
                                val indiceEstado = when (pedido.estado) {
                                    EstadoPedido.PENDIENTE_VALIDACION -> 0
                                    EstadoPedido.CONFIRMADO -> 1
                                    EstadoPedido.PREPARADO -> 2
                                    EstadoPedido.ENTREGADO -> 3
                                }

                                Spacer(Modifier.height(14.dp))

                                // Progreso del pedido: 25 %, 50 %, 75 % o 100 %.
                                LinearProgressIndicator(
                                    progress = (indiceEstado + 1) / 4f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50)),
                                    color = PrincipalOYL,
                                    trackColor = FondoOYL
                                )

                                Spacer(Modifier.height(8.dp))

                                // Etapas del pedido.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(
                                        "Pendiente",
                                        "Confirmado",
                                        "Preparado",
                                        "Entregado"
                                    ).forEachIndexed { indice, nombreEstado ->

                                        Text(
                                            text = nombreEstado,
                                            fontSize = 10.sp,
                                            color = if (indice <= indiceEstado) {
                                                SecundarioOYL
                                            } else {
                                                PrincipalOYL.copy(alpha = 0.4f)
                                            }
                                        )
                                    }
                                }

                            } ?: Text(
                                text = "Aún no tienes pedidos registrados.",
                                color = PrincipalOYL,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 14.dp)
                            )

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
fun BarraCliente(
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

@Composable
private fun TextoTarjetaProducto(
    text: String,
    color: Color,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: TextUnit = 16.sp,
    lineas: Int = 2
) {
    val altoLinea = (fontSize.value * 1.5f).sp
    val alto = with(LocalDensity.current) { (altoLinea.value * lineas).sp.toDp() }
    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = altoLinea,
        maxLines = lineas,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().height(alto)
    )
}
