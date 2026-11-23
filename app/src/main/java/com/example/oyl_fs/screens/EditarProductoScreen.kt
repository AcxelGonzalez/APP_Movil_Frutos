package com.example.oyl_fs.screens

import com.example.oyl_fs.ui.theme.EncabezadoOYL

import com.example.oyl_fs.ui.theme.MensajeErrorOYL

import com.example.oyl_fs.ui.theme.entradaOYL

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
import kotlinx.coroutines.launch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector

import com.example.oyl_fs.viewmodel.DatosFormularioProducto
import com.example.oyl_fs.viewmodel.ErroresFormularioProducto
import com.example.oyl_fs.viewmodel.ValidacionFormularioProducto


private val FondoEditar = Color(0xFFFFF5E7)
private val CafeEditar = Color(0xFF92623D)
private val OscuroEditar = Color(0xFF4B2E15)

/**
 * Conecta la edición de productos con el ViewModel.
 *
 * Cuando Room esté activo, los cambios
 * se confirmarán desde SQLite antes de salir.
 */
@Composable
fun EditarProductoAdmin(
    producto: ProductoInfo,
    viewModel: ProductoViewModel,
    onVolver: () -> Unit
) {

    val error by viewModel.error.collectAsState()

    val scope = rememberCoroutineScope()

    var guardando by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(producto.id) {
        viewModel.limpiarError()
    }

    EditarProductoScreen(
        producto = producto,
        errorGuardado = error,
        guardando = guardando,

        onEdicionFormulario = {
            viewModel.limpiarError()
        },

        onVolver = {
            viewModel.limpiarError()
            onVolver()
        },

        onGuardar = { productoEditado ->

            if (!guardando) {

                guardando = true

                scope.launch {

                    try {

                        val actualizado = viewModel
                            .editarProductoPersistente(
                                productoEditado
                            )

                        if (actualizado) {
                            onVolver()
                        }

                    } finally {
                        guardando = false
                    }
                }
            }
        }
    )
}

@Composable
fun EditarProductoScreen(
    producto: ProductoInfo,
    onVolver: () -> Unit,
    onGuardar: (ProductoInfo) -> Unit,
    errorGuardado: String? = null,
    guardando: Boolean = false,
    onEdicionFormulario: () -> Unit = {}
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
    // El stock se consulta; las existencias se cambian mediante movimientos.
    val stock = producto.stock.toString()
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

    var erroresFormulario by remember(producto.id) {
        mutableStateOf(
            ErroresFormularioProducto()
        )
    }

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

                erroresFormulario = erroresFormulario.copy(
                    imagen = null
                )
                onEdicionFormulario()

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
            .fillMaxSize().entradaOYL()
            .background(FondoEditar)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        EncabezadoOYL("Editar producto", onVolver)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !guardando) {
                    seleccionarImagen.launch(
                        arrayOf("image/jpeg", "image/png")
                    )
                },
            shape = RoundedCornerShape(24.dp),
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

        CapturarFotoProducto(
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth(),
            onFoto = { uri ->
                imagenUri = uri
                errorFormulario = null
                erroresFormulario = erroresFormulario.copy(imagen = null)
                onEdicionFormulario()
            },
            onError = { errorFormulario = it }
        )

        erroresFormulario.imagen?.let { mensaje ->
            Text(
                text = mensaje,
                color = Color(0xFFC0392B),
                fontSize = 12.sp
            )
        }

        CampoEditar(
            titulo = "Nombre del producto *",
            valor = nombre,
            icono = Icons.Default.Label,
            error = erroresFormulario.nombre
        ) {
            nombre = it
            erroresFormulario = erroresFormulario.copy(
                nombre = null
            )
            onEdicionFormulario()
        }

        CampoEditar(
            titulo = "Código *",
            valor = codigo,
            icono = Icons.Default.QrCode,
            error = erroresFormulario.codigo
                ?: errorGuardado?.takeIf {
                    it.contains("código", ignoreCase = true)
                }
        ) {
            codigo = it
            erroresFormulario = erroresFormulario.copy(
                codigo = null
            )
            onEdicionFormulario()
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroEditar
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    categoria.ifEmpty {
                        "Seleccionar categoría"
                    }
                )
            }

            DropdownMenu(
                expanded = menuCategorias,
                onDismissRequest = {
                    menuCategorias = false
                }
            ) {
                categorias.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(opcion) },
                        onClick = {
                            categoria = opcion
                            menuCategorias = false

                            erroresFormulario =
                                erroresFormulario.copy(
                                    categoria = null
                                )
                            onEdicionFormulario()
                        }
                    )
                }
            }
        }

        erroresFormulario.categoria?.let { mensaje ->
            Text(
                text = mensaje,
                color = Color(0xFFC0392B),
                fontSize = 12.sp
            )
        }

        CampoEditar(
            titulo = "Descripción",
            valor = descripcion,
            icono = Icons.Default.Description,
            error = erroresFormulario.descripcion,
            multilinea = true
        ) {
            descripcion = it
            erroresFormulario = erroresFormulario.copy(descripcion = null)
            onEdicionFormulario()
        }

        CampoEditar(
            titulo = "Precio *",
            valor = precio,
            icono = Icons.Default.AttachMoney,
            error = erroresFormulario.precio,
            numerico = true
        ) {
            precio = it
            erroresFormulario = erroresFormulario.copy(
                precio = null
            )
            onEdicionFormulario()
        }

        CampoEditar(
            titulo = "Stock actual (solo lectura)",
            valor = stock,
            icono = Icons.Default.Inventory,
            soloLectura = true
        ) { }
        Text(
            text = "Para cambiar existencias, registra una entrada o salida desde Inventario.",
            color = CafeEditar,
            fontSize = 12.sp
        )

        CampoEditar(
            titulo = "Stock mínimo *",
            valor = stockMinimo,
            icono = Icons.Default.Warning,
            error = erroresFormulario.stockMinimo,
            numerico = true
        ) {
            stockMinimo = it
            erroresFormulario = erroresFormulario.copy(
                stockMinimo = null
            )
            onEdicionFormulario()
        }

        CampoEditar(
            titulo = "Peso / Presentación *",
            valor = presentacion,
            icono = Icons.Default.Straighten,
            error = erroresFormulario.presentacion
        ) {
            presentacion = it
            erroresFormulario = erroresFormulario.copy(presentacion = null)
            onEdicionFormulario()
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

        val mensaje = errorFormulario
            ?: errorGuardado?.takeUnless {
                it.contains("código", ignoreCase = true)
            }

        AnimatedVisibility(visible = mensaje != null, enter = fadeIn(tween(350)) + expandVertically(tween(350)),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))) {
            MensajeErrorOYL(mensaje.orEmpty())
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onVolver,
                modifier = Modifier.fillMaxWidth(),
                enabled = !guardando,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = OscuroEditar
                )
            ) {
                Text("Cancelar")
            }

            Button(

                onClick = {
                    errorFormulario = null

                    erroresFormulario =
                        ValidacionFormularioProducto.validar(
                            datos = DatosFormularioProducto(
                                nombre = nombre,
                                codigo = codigo,
                                categoria = categoria,
                                precio = precio,
                                stock = stock,
                                stockMinimo = stockMinimo,
                                presentacion = presentacion,
                                descripcion = descripcion,
                                imagenPresente =
                                    !imagenUri.isNullOrBlank() ||
                                            producto.imagen != 0
                            ),
                            requerirImagen = true
                        )

                    if (!erroresFormulario.hayErrores) {
                        onGuardar(productoFormulario)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !guardando,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OscuroEditar,
                    contentColor = Color.White
                )
            ) {
                Text(
                    if (guardando) "Guardando..." else "Guardar cambios"
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
    icono: ImageVector,
    error: String? = null,
    numerico: Boolean = false,
    multilinea: Boolean = false,
    soloLectura: Boolean = false,
    onCambio: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = titulo,
            color = OscuroEditar,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = valor,
            readOnly = soloLectura,
            onValueChange = { texto ->
                if (!soloLectura && (!numerico || texto.all { it in '0'..'9' })) {
                    onCambio(texto)
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = icono,
                    contentDescription = null
                )
            },
            isError = error != null,
            supportingText = if (error != null) {
                {
                    Text(text = error)
                }
            } else {
                null
            },
            singleLine = !multilinea,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numerico) {
                    KeyboardType.Number
                } else {
                    KeyboardType.Text
                }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = OscuroEditar,
                containerColor = Color.White,
                cursorColor = OscuroEditar,
                focusedBorderColor = CafeEditar,
                unfocusedBorderColor = Color(0xFFD5BEA6)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(
                    min = if (multilinea) 110.dp else 56.dp
                )
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 1500)
@Composable
private fun EditarProductoPreview() {
    com.example.oyl_fs.ui.theme.OYL_FSTheme {
        EditarProductoScreen(
            producto = ProductosDatos.almendras,
            onVolver = {},
            onGuardar = {}
        )
    }
}
