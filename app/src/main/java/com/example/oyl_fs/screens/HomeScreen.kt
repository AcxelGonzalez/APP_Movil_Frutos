package com.example.oyl_fs.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onAbrirProductos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "OYL FRUTOS SECOS",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Menú principal",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {}) {
            Text("Pedidos")
        }

        // Abre el módulo de productos.
        Button(onClick = onAbrirProductos) {
            Text("Productos")
        }

        Button(onClick = {}) {
            Text("Ventas")
        }

        Button(onClick = {}) {
            Text("Ajustes")
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(onClick = onLogout) {
            Text("Cerrar sesión")
        }
    }
}