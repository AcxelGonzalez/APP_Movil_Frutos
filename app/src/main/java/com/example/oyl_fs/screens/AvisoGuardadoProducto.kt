package com.example.oyl_fs.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal fun mensajeGuardadoProducto(editado: Boolean, catalogoPendiente: Boolean): String {
    val confirmacion = if (editado) "Producto actualizado correctamente." else "Producto creado correctamente."
    return if (catalogoPendiente) {
        "$confirmacion No se pudo actualizar el catálogo y puede mostrar datos anteriores. El guardado ya está confirmado; no necesitas repetirlo."
    } else confirmacion
}

/** Se conserva en la vista de destino hasta cerrarlo o iniciar otra operación. */
@Composable
internal fun AvisoGuardadoProducto(mensaje: String?, onCerrar: () -> Unit) {
    if (mensaje == null) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF5EA))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(mensaje, color = Color(0xFF1B5E20))
            TextButton(onClick = onCerrar) { Text("Cerrar confirmación") }
        }
    }
}
