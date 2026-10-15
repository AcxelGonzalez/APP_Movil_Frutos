package com.example.oyl_fs.screens

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

private val FondoAgregar = Color(0xFFFFF5E7)
private val CafeAgregar = Color(0xFF92623D)
private val OscuroAgregar = Color(0xFF4B2E15)

// Conecta el formulario para agregar con el ViewModel.
@Composable
fun AgregarProductoAdmin(
    viewModel: ProductoViewModel,
    onVolver: () -> Unit
) {
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.limpiarError()
    }

    AgregarProductoScreen(
        onVolver = onVolver,
        errorGuardado = error,
        onGuardar = { producto ->
            if (viewModel.agregarProducto(producto)) {
                onVolver()
            }
        }
    )
}

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

    AgregarProductoScreen(
        productoInicial = producto,
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
fun AgregarProductoScreen(
    onVolver: () -> Unit,
    onGuardar: (ProductoInfo) -> Unit,
    errorGuardado: String? = null,
    productoInicial: ProductoInfo? = null
) {
    val editando = productoInicial != null

    // Al editar, comienza con los datos del producto.
    var nombre by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.nombre ?: "")
    }
    var codigo by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.codigo ?: "")
    }
    var categoria by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.categoria ?: "")
    }
    var descripcion by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.descripcion ?: "")
    }
    var precio by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.precio?.toString() ?: "")
    }
    var stock by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.stock?.toString() ?: "")
    }
    var stockMinimo by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.stockMinimo?.toString() ?: "")
    }
    var presentacion by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.presentacion ?: "")
    }
    var disponible by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.disponible ?: true)
    }
    var imagenUri by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.imagenUri)
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
        id = productoInicial?.id ?: 0,
        nombre = nombre,
        codigo = codigo,
        categoria = categoria,
        descripcion = descripcion,
        presentacion = presentacion,
        precio = precio.toIntOrNull() ?: 0,
        stock = stock.toIntOrNull() ?: 0,
        stockMinimo = stockMinimo.toIntOrNull() ?: 0,
        disponible = disponible,
        imagen = productoInicial?.imagen ?: 0,
        imagenUri = imagenUri
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoAgregar)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onVolver) {
                Text("‹", color = OscuroAgregar, fontSize = 30.sp)
            }

            Text(
                text = if (editando) {
                    "Editar producto"
                } else {
                    "Agregar producto"
                },
                color = OscuroAgregar,
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
                    Text("+ Agregar imagen", color = OscuroAgregar)
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
                        color = CafeAgregar,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        CampoAgregar("Nombre del producto *", nombre) {
            nombre = it
        }

        CampoAgregar("Código *", codigo) {
            codigo = it
        }

        Text(
            text = "Categoría *",
            color = OscuroAgregar,
            fontWeight = FontWeight.Bold
        )

        Box {
            OutlinedButton(
                onClick = { menuCategorias = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroAgregar
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

        CampoAgregar(
            titulo = "Descripción",
            valor = descripcion,
            multilinea = true
        ) {
            descripcion = it
        }

        CampoAgregar(
            titulo = "Precio *",
            valor = precio,
            numerico = true
        ) {
            precio = it
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                CampoAgregar(
                    titulo = if (editando) {
                        "Stock actual *"
                    } else {
                        "Stock inicial *"
                    },
                    valor = stock,
                    numerico = true
                ) {
                    stock = it
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                CampoAgregar(
                    titulo = "Stock mínimo *",
                    valor = stockMinimo,
                    numerico = true
                ) {
                    stockMinimo = it
                }
            }
        }

        CampoAgregar("Peso / Presentación", presentacion) {
            presentacion = it
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Producto disponible",
                color = OscuroAgregar,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = disponible,
                onCheckedChange = { disponible = it },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = OscuroAgregar
                )
            )
        }

        val mensaje = errorFormulario ?: errorGuardado

        if (mensaje != null) {
            Text(mensaje, color = Color(0xFFC0392B))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroAgregar
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
                    containerColor = OscuroAgregar,
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (editando) "Guardar cambios" else "Guardar producto"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoAgregar(
    titulo: String,
    valor: String,
    numerico: Boolean = false,
    multilinea: Boolean = false,
    onCambio: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = titulo,
            color = OscuroAgregar,
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
                textColor = OscuroAgregar,
                containerColor = Color.White,
                cursorColor = OscuroAgregar,
                focusedBorderColor = CafeAgregar,
                unfocusedBorderColor = CafeAgregar
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (multilinea) 110.dp else 56.dp)
        )
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 1500
)
@Composable
private fun AgregarProductoPreview() {
    MaterialTheme {
        AgregarProductoScreen(
            onVolver = {},
            onGuardar = {}
        )
    }
}