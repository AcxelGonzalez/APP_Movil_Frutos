package com.example.oyl_fs.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.viewmodel.ProductoViewModel

private val FondoEditar = Color(0xFFFFF5E7)
private val CafeEditar = Color(0xFF92623D)
private val OscuroEditar = Color(0xFF4B2E15)

// Conecta el formulario para editar con el ViewModel.
@Composable
fun EditarProductoAdmin(
    producto: ProductoInfo,
    viewModel: ProductoViewModel,
    onVolver: () -> Unit
) {
    val error by viewModel.error.collectAsState()

    LaunchedEffect(producto.id) {
        viewModel.limpiarError()
    }

    EditarProductoScreen(
        producto = producto,
        errorGuardado = error,
        onVolver = {
            viewModel.limpiarError()
            onVolver()
        },
        onGuardar = { productoEditado ->
            if (viewModel.editarProducto(productoEditado)) {
                onVolver()
            }
        }
    )
}

@Composable
fun EditarProductoScreen(
    producto: ProductoInfo,
    onVolver: () -> Unit,
    onGuardar: (ProductoInfo) -> Unit,
    errorGuardado: String? = null
) {
    // Al editar, comienza con los datos del producto.
    var nombre by rememberSaveable(producto.id) {
        mutableStateOf(producto.nombre)
    }
    var codigo by rememberSaveable(producto.id) {
        mutableStateOf(producto.codigo)
    }
    var categoria by rememberSaveable(producto.id) {
        mutableStateOf(producto.categoria)
    }
    var descripcion by rememberSaveable(producto.id) {
        mutableStateOf(producto.descripcion)
    }
    var precio by rememberSaveable(producto.id) {
        mutableStateOf(producto.precio.toString())
    }
    var stock by rememberSaveable(producto.id) {
        mutableStateOf(producto.stock.toString())
    }
    var stockMinimo by rememberSaveable(producto.id) {
        mutableStateOf(producto.stockMinimo.toString())
    }
    var presentacion by rememberSaveable(producto.id) {
        mutableStateOf(producto.presentacion)
    }
    var disponible by rememberSaveable(producto.id) {
        mutableStateOf(producto.disponible)
    }
    var imagenUri by rememberSaveable(producto.id) {
        mutableStateOf(producto.imagenUri)
    }

    var menuCategorias by remember { mutableStateOf(false) }
    var errorFormulario by remember { mutableStateOf<String?>(null) }

    val contexto = LocalContext.current
    val categorias = listOf(
        "Frutos secos",
        "Semillas",
        "Deshidratados"
    )

    // Conserva el permiso para leer la fotografía seleccionada.
    val seleccionarImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contexto.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                imagenUri = uri.toString()
                errorFormulario = null
            } catch (error: SecurityException) {
                errorFormulario = "No se pudo conservar la fotografía."
            }
        }
    }

    // Conserva el identificador y la imagen original al editar.
    val productoFormulario = ProductoInfo(
        id = producto.id,
        nombre = nombre,
        codigo = codigo,
        categoria = categoria,
        descripcion = descripcion,
        presentacion = presentacion,
        precio = precio.toIntOrNull() ?: 0,
        stock = stock.toIntOrNull() ?: 0,
        stockMinimo = stockMinimo.toIntOrNull() ?: 0,
        disponible = disponible,
        imagen = producto.imagen,
        imagenUri = imagenUri
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoEditar)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) {
                Text("‹", color = OscuroEditar, fontSize = 30.sp)
            }

            Text(
                text = "Editar producto",
                color = OscuroEditar,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    seleccionarImagen.launch(
                        arrayOf("image/jpeg", "image/png")
                    )
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            if (imagenUri == null && productoFormulario.imagen == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Agregar imagen", color = OscuroEditar)
                }
            } else {
                Column {
                    ImagenProducto(
                        producto = productoFormulario,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    Text(
                        text = "Toca para cambiar la imagen",
                        color = CafeEditar,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        CampoEditar("Nombre del producto *", nombre) {
            nombre = it
        }

        CampoEditar("Código *", codigo) {
            codigo = it
        }

        Text(
            text = "Categoría *",
            color = OscuroEditar,
            fontWeight = FontWeight.Bold
        )

        Box {
            OutlinedButton(
                onClick = { menuCategorias = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroEditar
                )
            ) {
                Text(
                    categoria.ifEmpty { "Seleccionar categoría" }
                )
            }

            DropdownMenu(
                expanded = menuCategorias,
                onDismissRequest = { menuCategorias = false }
            ) {
                categorias.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = {
                            categoria = opcion
                            menuCategorias = false
                        }
                    )
                }
            }
        }

        CampoEditar(
            titulo = "Descripción",
            valor = descripcion,
            multilinea = true
        ) {
            descripcion = it
        }

        CampoEditar(
            titulo = "Precio *",
            valor = precio,
            numerico = true
        ) {
            precio = it
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                CampoEditar(
                    titulo = "Stock actual *",
                    valor = stock,
                    numerico = true
                ) {
                    stock = it
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                CampoEditar(
                    titulo = "Stock mínimo *",
                    valor = stockMinimo,
                    numerico = true
                ) {
                    stockMinimo = it
                }
            }
        }

        CampoEditar("Peso / Presentación", presentacion) {
            presentacion = it
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Producto disponible",
                color = OscuroEditar,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = disponible,
                onCheckedChange = { disponible = it },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = OscuroEditar
                )
            )
        }

        val mensaje = errorFormulario ?: errorGuardado

        AnimatedVisibility(visible = mensaje != null, enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            Text(mensaje.orEmpty(), color = Color(0xFFC0392B))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroEditar
                )
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    val precioNumero = precio.toIntOrNull()
                    val stockNumero = stock.toIntOrNull()
                    val minimoNumero = stockMinimo.toIntOrNull()

                    // Valida los datos antes de enviarlos al ViewModel.
                    errorFormulario = when {
                        nombre.isBlank() ->
                            "Escribe el nombre del producto."

                        codigo.isBlank() ->
                            "Escribe el código."

                        categoria.isBlank() ->
                            "Selecciona una categoría."

                        precioNumero == null || precioNumero <= 0 ->
                            "Ingresa un precio mayor que cero."

                        stockNumero == null || stockNumero < 0 ->
                            "Ingresa un stock válido."

                        minimoNumero == null || minimoNumero < 0 ->
                            "Ingresa un stock mínimo válido."

                        imagenUri == null &&
                                productoFormulario.imagen == 0 ->
                            "Selecciona una fotografía."

                        else -> null
                    }

                    if (errorFormulario == null) {
                        onGuardar(productoFormulario)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OscuroEditar,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Guardar cambios"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoEditar(
    titulo: String,
    valor: String,
    numerico: Boolean = false,
    multilinea: Boolean = false,
    onCambio: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = titulo,
            color = OscuroEditar,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = valor,
            onValueChange = { texto: String ->
                if (!numerico || texto.all { it in '0'..'9' }) {
                    onCambio(texto)
                }
            },
            singleLine = !multilinea,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numerico) {
                    KeyboardType.Number
                } else {
                    KeyboardType.Text
                }
            ),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = OscuroEditar,
                containerColor = Color.White,
                cursorColor = OscuroEditar,
                focusedBorderColor = CafeEditar,
                unfocusedBorderColor = CafeEditar
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (multilinea) 110.dp else 56.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1500)
@Composable
private fun EditarProductoPreview() {
    MaterialTheme {
        EditarProductoScreen(
            producto = ProductosDatos.almendras,
            onVolver = {},
            onGuardar = {}
        )
    }
}
