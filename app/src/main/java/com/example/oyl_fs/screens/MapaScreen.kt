package com.example.oyl_fs.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oyl_fs.model.PuntoRetiro
import com.example.oyl_fs.ui.theme.EncabezadoOYL
import com.example.oyl_fs.viewmodel.MapaViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)

private fun PuntoRetiro.aGeoPoint() = GeoPoint(latitud, longitud)

@Composable
private fun rememberMapaOYL(): MapView {
    val contexto = LocalContext.current
    val mapa = remember(contexto) {
        Configuration.getInstance().userAgentValue = "OYL-FrutosSecos/1.0 (proyecto academico DUOC)"
        MapView(contexto).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(11.5)
            controller.setCenter(GeoPoint(-33.45, -70.66))
        }
    }
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(mapa, cicloDeVida) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        cicloDeVida.addObserver(observador)
        // Al entrar desde el menú, la actividad puede estar ya reanudada.
        if (cicloDeVida.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapa.onResume()
        onDispose {
            cicloDeVida.removeObserver(observador)
            mapa.onPause()
            mapa.onDetach()
        }
    }
    return mapa
}

private fun MapView.mostrarPuntos(puntos: List<PuntoRetiro>) {
    if (puntos.isEmpty()) return
    doOnLayout {
        if (puntos.size == 1) controller.animateTo(puntos.first().aGeoPoint(), 14.5, 500L)
        else zoomToBoundingBox(BoundingBox.fromGeoPoints(puntos.map { it.aGeoPoint() }),
            true, (48 * resources.displayMetrics.density).toInt())
    }
}

private fun MapView.dibujarMarcadores(puntos: List<PuntoRetiro>, onTocar: (PuntoRetiro) -> Unit) {
    overlays.removeAll { it is Marker }
    puntos.forEach { punto ->
        val marcador = Marker(this)
        marcador.position = punto.aGeoPoint()
        marcador.title = punto.nombre
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marcador.icon = marcador.icon.mutate().apply {
            setTint((if (punto.esTienda) SecundarioOYL else PrincipalOYL).toArgb())
        }
        marcador.setOnMarkerClickListener { _, _ -> onTocar(punto); true }
        overlays.add(marcador)
    }
    invalidate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(onVolver: () -> Unit, vm: MapaViewModel = viewModel()) {
    val estado by vm.uiState.collectAsState()
    val mapa = rememberMapaOYL()
    val contexto = LocalContext.current
    var filtro by rememberSaveable { mutableStateOf("Todos") }
    val visibles = estado.puntos.filter {
        filtro == "Todos" || (filtro == "Tiendas" && it.esTienda) || (filtro == "Retiro" && !it.esTienda)
    }
    fun seleccionar(punto: PuntoRetiro) {
        vm.seleccionar(punto)
        mapa.controller.animateTo(punto.aGeoPoint(), 15.0, 550L)
    }
    LaunchedEffect(visibles) {
        mapa.dibujarMarcadores(visibles, ::seleccionar)
        val elegido = estado.seleccionado?.takeIf { punto -> visibles.any { it.id == punto.id } }
        if (elegido != null) mapa.controller.animateTo(elegido.aGeoPoint(), 15.0, 550L)
        else mapa.mostrarPuntos(visibles)
        if (estado.seleccionado != null && visibles.none { it.id == estado.seleccionado?.id }) vm.cerrarDetalle()
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(FondoOYL)) {
        val altoDetalle = maxHeight * 0.40f
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
                EncabezadoOYL("Tiendas y retiro", onVolver)
                Spacer(Modifier.height(10.dp))
                Text("Encuentra un punto y consulta cómo llegar.", color = PrincipalOYL, fontSize = 13.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Todos", "Tiendas", "Retiro")) { opcion ->
                        FilterChip(selected = filtro == opcion, onClick = { vm.cerrarDetalle(); filtro = opcion },
                            label = { Text(opcion) }, shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(containerColor = Color.White,
                                labelColor = PrincipalOYL, selectedContainerColor = SecundarioOYL,
                                selectedLabelColor = Color.White))
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp)).background(Color.White)) {
                AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize())
                Row(Modifier.align(Alignment.BottomEnd).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ControlMapa(Icons.Default.Remove, "Alejar") { mapa.controller.zoomOut() }
                    ControlMapa(Icons.Default.Add, "Acercar") { mapa.controller.zoomIn() }
                    ControlMapa(Icons.Default.CenterFocusStrong, "Mostrar todos los puntos") {
                        vm.cerrarDetalle(); mapa.mostrarPuntos(visibles)
                    }
                }
                Text("© OpenStreetMap contributors", color = SecundarioOYL, fontSize = 9.sp,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp))
            }
            val punto = estado.seleccionado
            if (punto != null) {
                TarjetaPunto(punto, vm::cerrarDetalle,
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).heightIn(max = altoDetalle),
                    onAbrir = {
                        val uri = Uri.parse("geo:${punto.latitud},${punto.longitud}?q=${punto.latitud},${punto.longitud}(${Uri.encode(punto.nombre)})")
                        try { contexto.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        catch (_: ActivityNotFoundException) {
                            Toast.makeText(contexto, "No hay una aplicación de mapas disponible en este teléfono.", Toast.LENGTH_LONG).show()
                        }
                    })
            } else {
                Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${visibles.size} puntos disponibles", color = SecundarioOYL, fontWeight = FontWeight.Bold)
                    if (visibles.isEmpty()) Text("No hay puntos para este filtro.", color = PrincipalOYL)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibles, key = { it.id }) { sitio ->
                            Surface(color = Color.White, shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFFEADBC7)),
                                modifier = Modifier.width(218.dp).clickable { seleccionar(sitio) }) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(if (sitio.esTienda) Icons.Default.Storefront else Icons.Default.Place, null, tint = PrincipalOYL)
                                    Column(Modifier.weight(1f)) {
                                        Text(sitio.nombre, color = SecundarioOYL, fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(if (sitio.esTienda) "Tienda" else "Punto de retiro", color = PrincipalOYL, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlMapa(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(12.dp), shadowElevation = 2.dp) {
        IconButton(onClick = onClick) { Icon(icono, texto, tint = SecundarioOYL, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun TarjetaPunto(punto: PuntoRetiro, onCerrar: () -> Unit, modifier: Modifier = Modifier, onAbrir: (() -> Unit)? = null) {
    Surface(modifier, color = Color.White, tonalElevation = 0.dp, shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFEADBC7))) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (punto.esTienda) "TIENDA OYL" else "PUNTO DE RETIRO", color = PrincipalOYL,
                        fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                    Text(punto.nombre, color = SecundarioOYL, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                IconButton(onClick = onCerrar) { Icon(Icons.Default.Close, "Cerrar información", tint = PrincipalOYL) }
            }
            FilaDato(Icons.Default.LocationOn, punto.direccion)
            FilaDato(Icons.Default.Schedule, punto.horario)
            FilaDato(Icons.Default.Info, punto.indicaciones)
            if (onAbrir != null) Button(onClick = onAbrir, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SecundarioOYL)) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Abrir en Mapas")
            }
        }
    }
}

@Composable
private fun FilaDato(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, null, tint = PrincipalOYL, modifier = Modifier.size(18.dp))
        Text(texto, color = SecundarioOYL, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaPuntoPreview() {
    com.example.oyl_fs.ui.theme.OYL_FSTheme {
        TarjetaPunto(PuntoRetiro("R01", "Retiro Providencia", false,
            "Av. Providencia 2100 (referencial)", "Lun a Vie 11:00 - 18:00",
            "Cerca del Metro Los Leones", -33.4220, -70.6080), {}, Modifier.padding(16.dp), {})
    }
}
