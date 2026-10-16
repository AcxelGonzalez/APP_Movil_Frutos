package com.example.oyl_fs.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.util.generarQrProducto
import com.example.oyl_fs.util.textoQrProducto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QrProductoScreen(producto: ProductoInfo, onVolver: () -> Unit) {
    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()
    val fondo = Color(0xFFFFF5E7)
    val oscuro = Color(0xFF4B2E15)
    var mensaje by remember { mutableStateOf<String?>(null) }
    var guardando by remember { mutableStateOf(false) }
    var errorQr by remember(producto.id) { mutableStateOf<String?>(null) }

    // La codificación y creación del bitmap se realizan en segundo plano.
    val bitmap by produceState<Bitmap?>(null, producto.id) {
        value = withContext(Dispatchers.Default) {
            try {
                val matriz = generarQrProducto(producto.id)
                val pixeles = IntArray(matriz.width * matriz.height)
                for (y in 0 until matriz.height) {
                    for (x in 0 until matriz.width) {
                        pixeles[y * matriz.width + x] = if (matriz[x, y]) {
                            android.graphics.Color.BLACK
                        } else android.graphics.Color.WHITE
                    }
                }
                Bitmap.createBitmap(pixeles, matriz.width, matriz.height, Bitmap.Config.ARGB_8888)
            } catch (error: IllegalArgumentException) {
                null
            } catch (error: com.google.zxing.WriterException) {
                null
            }
        }
        if (value == null) errorQr = "No se pudo generar el código QR"
    }

    // Android permite elegir dónde guardar sin permisos de almacenamiento.
    val guardarQr = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val foto = bitmap
        if (uri != null && foto != null) {
            guardando = true
            scope.launch {
                val guardado = withContext(Dispatchers.IO) {
                    try {
                        contexto.contentResolver.openOutputStream(uri)?.use {
                            foto.compress(Bitmap.CompressFormat.PNG, 100, it)
                        } ?: false
                    } catch (error: java.io.IOException) {
                        false
                    } catch (error: SecurityException) {
                        false
                    }
                }
                guardando = false
                mensaje = if (guardado) "QR guardado correctamente" else "No se pudo guardar el QR"
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(fondo).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        TextButton(onClick = onVolver, modifier = Modifier.align(Alignment.Start)) { Text("‹ Volver al producto", color = oscuro) }
        Text("QR del producto", color = oscuro, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(producto.nombre, color = oscuro, fontWeight = FontWeight.Bold)
        Text("Código: ${producto.codigo}", color = oscuro)
        val foto = bitmap
        if (foto != null) {
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                Image(foto.asImageBitmap(), "Código QR de ${producto.nombre}", Modifier.size(260.dp).padding(8.dp))
            }
        } else if (errorQr != null) {
            Text(errorQr ?: "", color = Color(0xFFC0392B))
        } else {
            CircularProgressIndicator(color = oscuro)
        }
        Text("Identifica el producto por su ID. El precio y el stock se consultan en el catálogo.", color = oscuro)
        Text(textoQrProducto(producto.id), color = oscuro, fontSize = 12.sp)
        Button(
            onClick = { guardarQr.launch("producto-${producto.id}.png") },
            enabled = foto != null && !guardando,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = oscuro)
        ) { Text(if (guardando) "Guardando..." else "Guardar QR como imagen") }
        if (mensaje != null) Text(mensaje ?: "", color = oscuro)
    }
}

@Preview(showBackground = true)
@Composable
private fun QrProductoPreview() {
    MaterialTheme { QrProductoScreen(ProductosDatos.almendras, {}) }
}
