package com.example.oyl_fs.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oyl_fs.model.PuntoRetiro
import com.example.oyl_fs.viewmodel.MapaViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

// Paleta de la hoja de ruta
private val FondoOYL = Color(0xFFFFF5E7)
private val PrincipalOYL = Color(0xFF92623D)
private val SecundarioOYL = Color(0xFF4B2E15)

private fun PuntoRetiro.aGeoPoint() = GeoPoint(latitud, longitud)


@Composable
private fun rememberMapaOYL(): MapView {
    val context = LocalContext.current

    val mapa = remember {
        Configuration.getInstance().userAgentValue = context.packageName

        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)   // estilo clásico de OpenStreetMap
            setMultiTouchControls(true)               // zoom con dos dedos
            isTilesScaledToDpi = true                 // letras del mapa más grandes
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(11.5)
            controller.setCenter(GeoPoint(-33.45, -70.66)) // Santiago
        }
    }

    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(cicloDeVida) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        cicloDeVida.addObserver(observador)
        onDispose {
            cicloDeVida.removeObserver(observador)
            mapa.onDetach()
        }
    }
    return mapa
}

private fun MapView.dibujarMarcadores(
    puntos: List<PuntoRetiro>,
    onTocar: (PuntoRetiro) -> Unit
) {
    overlays.removeAll { it is Marker }

    puntos.forEach { punto ->
        val marcador = Marker(this)
        marcador.position = punto.aGeoPoint()
        marcador.title = punto.nombre
        marcador.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

        val color = if (punto.esTienda) SecundarioOYL else PrincipalOYL
        marcador.icon = marcador.icon.mutate().apply { setTint(color.toArgb()) }

        marcador.setOnMarkerClickListener { m, mapView ->
            onTocar(punto)
            mapView.controller.animateTo(m.position)
            true
        }
        overlays.add(marcador)
    }
    invalidate()
}

@Composable
fun MapaScreen(
    onVolver: () -> Unit,
    vm: MapaViewModel = viewModel()
) {
    val estado by vm.uiState.collectAsState()
    val mapa = rememberMapaOYL()

    LaunchedEffect(estado.puntos) {
        mapa.dibujarMarcadores(estado.puntos, onTocar = vm::seleccionar)
        estado.tienda?.let { mapa.controller.setCenter(it.aGeoPoint()) }
    }

    Box(Modifier.fillMaxSize()) {

        AndroidView(factory = { mapa }, modifier = Modifier.fillMaxSize())

        Card(
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(16.dp)
        ) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = SecundarioOYL)
                }
                Text(
                    "Tiendas y puntos de retiro",
                    fontWeight = FontWeight.Bold,
                    color = SecundarioOYL,
                    fontSize = 16.sp
                )
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "© OpenStreetMap",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    modifier = Modifier
                        .weight(1f)
                        .wrapContentWidth(Alignment.Start)
                        .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SmallFloatingActionButton(
                        onClick = { mapa.controller.zoomIn() },
                        containerColor = Color.White,
                        contentColor = SecundarioOYL
                    ) { Icon(Icons.Default.Add, "Acercar") }

                    SmallFloatingActionButton(
                        onClick = { mapa.controller.zoomOut() },
                        containerColor = Color.White,
                        contentColor = SecundarioOYL
                    ) { Icon(Icons.Default.Remove, "Alejar") }

                    FloatingActionButton(
                        onClick = {
                            estado.tienda?.let {
                                mapa.controller.animateTo(it.aGeoPoint(), 15.0, 1000L)
                                vm.seleccionar(it)
                            }
                        },
                        containerColor = SecundarioOYL,
                        contentColor = Color.White
                    ) { Icon(Icons.Default.Storefront, "Ir a la tienda") }
                }
            }

            estado.seleccionado?.let { punto ->
                TarjetaPunto(punto = punto, onCerrar = vm::cerrarDetalle)
            }
        }
    }
}

@Composable
private fun TarjetaPunto(punto: PuntoRetiro, onCerrar: () -> Unit) {
    val colorIcono = if (punto.esTienda) SecundarioOYL else PrincipalOYL

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(colorIcono),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (punto.esTienda) Icons.Default.Storefront else Icons.Default.Place,
                        null,
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (punto.esTienda) "Tienda" else "Punto de retiro",
                        fontSize = 12.sp,
                        color = PrincipalOYL
                    )
                    Text(punto.nombre, fontWeight = FontWeight.Bold, color = SecundarioOYL, fontSize = 16.sp)
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Default.Close, "Cerrar", tint = PrincipalOYL)
                }
            }

            Divider(color = FondoOYL, modifier = Modifier.padding(vertical = 12.dp))

            FilaDato(Icons.Default.LocationOn, punto.direccion)
            FilaDato(Icons.Default.Schedule, punto.horario)
            FilaDato(Icons.Default.Info, punto.indicaciones)
        }
    }
}

// Una línea con ícono + texto (dirección, horario, indicaciones)
@Composable
private fun FilaDato(icono: ImageVector, texto: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(icono, null, tint = PrincipalOYL, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 13.sp, color = SecundarioOYL)
    }
}

// ---------- Preview de la tarjeta (el mapa no se puede previsualizar) ----------
@Preview(showBackground = true, backgroundColor = 0xFFFFF5E7)
@Composable
private fun TarjetaPuntoPreview() {
    Box(Modifier.padding(16.dp)) {
        TarjetaPunto(
            punto = PuntoRetiro(
                "R01", "Punto de retiro Providencia", false,
                "Av. Providencia 2100, Providencia (referencial)",
                "Lun a Vie 11:00 - 18:00",
                "Cerca del Metro Los Leones, consultar en recepción",
                -33.4220, -70.6080
            ),
            onCerrar = {}
        )
    }
}