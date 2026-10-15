package com.example.oyl_fs.screens
import androidx.activity.compose.BackHandler
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.viewmodel.ProductoViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FondoCatalogo = Color(0xFFFFF5E7)
private val CafeCatalogo = Color(0xFF92623D)
private val CafeOscuroCatalogo = Color(0xFF4B2E15)

// Conecta el catálogo con la lista observable del ViewModel.
@Composable
fun CatalogoAdmin(
    viewModel: ProductoViewModel,
    onProductoClick: (ProductoInfo) -> Unit,
    onAgregarProducto: () -> Unit
) {
    val productos by viewModel.productos.collectAsState()

    CatalogoScreen(
        productos = productos,
        onProductoClick = onProductoClick,
        onAgregarProducto = onAgregarProducto
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    productos: List<ProductoInfo>,
    onProductoClick: (ProductoInfo) -> Unit,
    onAgregarProducto: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Todos") }

    val categorias = listOf(
        "Todos",
        "Frutos secos",
        "Semillas",
        "Deshidratados"
    )

    // Busca por nombre y filtra por categoría.
    val filtrados = productos.filter { producto ->
        producto.nombre.contains(busqueda.trim(), ignoreCase = true) &&
                (categoria == "Todos" || producto.categoria == categoria)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoCatalogo)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Productos",
            color = CafeOscuroCatalogo,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            placeholder = { Text("Buscar productos...") },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = CafeOscuroCatalogo,
                containerColor = Color.White,
                cursorColor = CafeOscuroCatalogo,
                focusedBorderColor = CafeCatalogo,
                unfocusedBorderColor = CafeCatalogo
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Categorías
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categorias.size) { indice ->
                val texto = categorias[indice]
                val seleccionada = categoria == texto

                Text(
                    text = texto,
                    color = if (seleccionada) {
                        Color.White
                    } else {
                        CafeCatalogo
                    },
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (seleccionada) {
                                CafeOscuroCatalogo
                            } else {
                                Color.White
                            }
                        )
                        .clickable { categoria = texto }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 9.dp
                        )
                )
            }
        }

        Button(
            onClick = onAgregarProducto,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CafeOscuroCatalogo,
                contentColor = Color.White
            )
        ) {
            Text("+ Agregar nuevo producto")
        }

        if (filtrados.isEmpty()) {
            Text(
                text = "No se encontraron productos.",
                color = CafeCatalogo
            )
        }

        // Dos productos por fila.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(
                items = filtrados,
                key = { producto -> producto.id }
            ) { producto ->
                TarjetaCatalogo(
                    producto = producto,
                    onClick = { onProductoClick(producto) }
                )
            }
        }
    }
}

@Composable
private fun TarjetaCatalogo(
    producto: ProductoInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ImagenProducto(
                producto = producto,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Text(
                text = producto.nombre,
                color = CafeOscuroCatalogo,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = producto.presentacion,
                color = CafeCatalogo,
                fontSize = 12.sp
            )

            Text(
                text = "$" + "%,d"
                    .format(producto.precio)
                    .replace(',', '.'),
                color = CafeOscuroCatalogo,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Stock: ${producto.stock} uds",
                color = CafeCatalogo,
                fontSize = 12.sp
            )

            Text(
                text = when {
                    !producto.disponible -> "Inactivo"
                    producto.stock <= 0 -> "Sin stock"
                    else -> "En stock"
                },
                color = CafeOscuroCatalogo,
                fontSize = 12.sp
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CafeOscuroCatalogo,
                    contentColor = Color.White
                )
            ) {
                Text("Ver detalle")
            }

        }
    }
}


// Controla el paso entre el catálogo y el formulario.

@Composable
fun ProductosAdmin(
    viewModel: ProductoViewModel,
    onProductoClick: (ProductoInfo) -> Unit
) {
    var mostrarFormulario by remember {
        mutableStateOf(false)
    }

    //botón Atrás 
    BackHandler(enabled = mostrarFormulario) {
        viewModel.limpiarError()
        mostrarFormulario = false
    }

    if (mostrarFormulario) {
        AgregarProductoAdmin(
            viewModel = viewModel,
            onVolver = {
                viewModel.limpiarError()
                mostrarFormulario = false
            }
        )
    } else {
        CatalogoAdmin(
            viewModel = viewModel,
            onProductoClick = onProductoClick,
            onAgregarProducto = {
                viewModel.limpiarError()
                mostrarFormulario = true
            }
        )
    }
}


@Preview(showBackground = true)
@Composable
private fun CatalogoPreview() {
    MaterialTheme {
        CatalogoScreen(
            productos = ProductosDatos.productos,
            onProductoClick = {},
            onAgregarProducto = {}
        )
    }
}

