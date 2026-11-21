package com.example.oyl_fs

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.ViewModelProvider

import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.screens.LoginScreen
import com.example.oyl_fs.screens.RegistroScreen
import com.example.oyl_fs.screens.HomeAdmin
import com.example.oyl_fs.screens.HomeCliente
import com.example.oyl_fs.screens.HistorialScreen
import com.example.oyl_fs.screens.CarroScreen
import com.example.oyl_fs.screens.PrepararPedidoScreen

import com.example.oyl_fs.viewmodel.HistorialViewModel
import com.example.oyl_fs.viewmodel.ProductoViewModel
import com.example.oyl_fs.viewmodel.PerfilViewModel
import com.example.oyl_fs.viewmodel.CarritoViewModel
import com.example.oyl_fs.viewmodel.PrepararPedidoViewModel

import com.example.oyl_fs.ui.theme.OYL_FSTheme

import com.example.oyl_fs.repository.ProductoRemoteRepository
import com.example.oyl_fs.repository.SesionRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModel para gestionar los pedidos.
        val historialViewModel = ViewModelProvider(this)[
            HistorialViewModel::class.java
        ]

        // ViewModel para gestionar los productos.
        val productoViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProductoViewModel(
                    remoteRepository = ProductoRemoteRepository(applicationContext)
                ) as T
            }
        })[ProductoViewModel::class.java]

        // ViewModel compartido entre HomeCliente y CarroScreen.
        // Evita que cada pantalla utilice un estado independiente.
        val carritoViewModel = ViewModelProvider(this)[
            CarritoViewModel::class.java
        ]

        // Datos personales separados por rol, compartidos con ambos inicios.
        val perfilViewModel = ViewModelProvider(this)[
            PerfilViewModel::class.java
        ]

        // ViewModel para verificar los productos de un pedido mediante QR.
        val prepararPedidoViewModel = ViewModelProvider(this)[
            PrepararPedidoViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {

                // Usuario que inició sesión (datos del backend).
                val usuarioSesion by SesionRepository.usuario.collectAsState()

                // Observa los productos del catálogo compartido.
                val productosActuales by productoViewModel
                    .productos
                    .collectAsState()

                // Observa los pedidos del administrador.
                // Permite actualizar el pedido si cambia su estado.
                val pedidosAdmin by historialViewModel
                    .pedidosParaAdmin
                    .collectAsState()

                // Cada vez que cambia el catálogo, actualiza
                // las existencias disponibles en el carrito.
                LaunchedEffect(productosActuales) {
                    carritoViewModel.sincronizarConCatalogo(
                        clienteId = "cliente-demo",
                        productos = productosActuales
                    )
                }

                // Controla si existe una sesión iniciada.
                var sesionIniciada by rememberSaveable {
                    mutableStateOf(false)
                }

                // Identificamos si es administrador.
                var esAdmin by rememberSaveable {
                    mutableStateOf(false)
                }

                // Navegación a historial.
                var mostrarHistorial by rememberSaveable {
                    mutableStateOf(false)
                }

                // Navegación a registro.
                var mostrarRegistro by rememberSaveable {
                    mutableStateOf(false)
                }

                // Navegación al carrito.
                var mostrarCarrito by rememberSaveable {
                    mutableStateOf(false)
                }

                // Identificador del pedido que el administrador
                // seleccionó para preparar mediante QR.
                // null significa que no hay preparación abierta.
                var pedidoPrepararId by rememberSaveable {
                    mutableStateOf<String?>(null)
                }

                // Recupera el pedido desde el ViewModel compartido.
                val pedidoParaPreparar = pedidosAdmin.find {
                    it.id == pedidoPrepararId
                }

                // Regresa de la preparación al historial.
                // Tiene prioridad sobre el retroceso general.
                BackHandler(
                    enabled = sesionIniciada &&
                            pedidoPrepararId != null
                ) {
                    pedidoPrepararId = null
                    mostrarHistorial = true
                }

                // Regresa de los módulos externos
                // al inicio del mismo usuario.
                BackHandler(
                    enabled = sesionIniciada &&
                            pedidoPrepararId == null &&
                            (mostrarHistorial || mostrarCarrito)
                ) {
                    mostrarHistorial = false
                    mostrarCarrito = false
                }

                // Permite regresar desde el registro al login.
                BackHandler(
                    enabled = !sesionIniciada && mostrarRegistro
                ) {
                    mostrarRegistro = false
                }

                when {

                    // ---------- Login y registro ----------
                    !sesionIniciada -> {

                        if (mostrarRegistro) {

                            RegistroScreen(
                                onRegistrado = {
                                    mostrarRegistro = false
                                },
                                onIrALogin = {
                                    mostrarRegistro = false
                                }
                            )

                        } else {

                            LoginScreen(
                                onLoginSuccess = { admin ->
                                    esAdmin = admin
                                    mostrarHistorial = false
                                    mostrarCarrito = false
                                    pedidoPrepararId = null
                                    sesionIniciada = true

                                    // Carga los pedidos del usuario desde el backend.
                                    historialViewModel.configurarSesion(
                                        SesionRepository.usuarioId?.toString().orEmpty(),
                                        admin
                                    )
                                },
                                onIrARegistro = {
                                    mostrarRegistro = true
                                }
                            )
                        }
                    }

                    // ---------- Preparar pedido mediante QR ----------
                    pedidoPrepararId != null && esAdmin -> {

                        if (pedidoParaPreparar != null) {

                            PrepararPedidoScreen(
                                pedido = pedidoParaPreparar,
                                viewModel = prepararPedidoViewModel,

                                onVolver = {
                                    // Regresar al historial del administrador.
                                    pedidoPrepararId = null
                                    mostrarHistorial = true
                                },

                                onConfirmarPreparacion = { pedidoId ->

                                    // Comprueba que sea el pedido seleccionado,
                                    // que esté confirmado y que todas las
                                    // cantidades hayan sido verificadas.
                                    val puedeConfirmar =
                                        pedidoId == pedidoParaPreparar.id &&
                                                pedidoParaPreparar.estado ==
                                                EstadoPedido.CONFIRMADO &&
                                                prepararPedidoViewModel.pedidoCompleto()

                                    if (puedeConfirmar) {

                                        // Registrar el nuevo estado del pedido en el backend.
                                        // error es null si salió bien, o el motivo del servidor.
                                        historialViewModel.cambiarEstadoPedido(
                                            pedidoId,
                                            EstadoPedido.PREPARADO
                                        ) { error ->

                                            if (error == null) {

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "Pedido $pedidoId preparado correctamente",
                                                    Toast.LENGTH_LONG
                                                ).show()

                                                // Regresar al historial.
                                                pedidoPrepararId = null
                                                mostrarHistorial = true

                                            } else {

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    error,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }

                                    } else {

                                        Toast.makeText(
                                            this@MainActivity,
                                            "El pedido no cumple los requisitos de preparación",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )

                        } else {

                            // Si el pedido ya no existe, regresar al historial.
                            LaunchedEffect(pedidoPrepararId) {
                                pedidoPrepararId = null
                                mostrarHistorial = true
                            }
                        }
                    }

                    // ---------- Carrito ----------
                    mostrarCarrito && !esAdmin -> {

                        CarroScreen(
                            // Identificador temporal del cliente.
                            clienteId = "cliente-demo",

                            // ViewModel compartido con HomeCliente.
                            vm = carritoViewModel,

                            onVolver = {
                                mostrarCarrito = false
                            },

                            // La compra ahora la procesa el backend (ms-pedidos):
                            // valida el stock, lo descuenta y crea el pedido.
                            onConfirmar = { _ ->
                                // El pedido se crea a nombre del usuario que inició sesión.
                                val usuarioId = SesionRepository.usuarioId

                                if (usuarioId == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Inicia sesión nuevamente",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    carritoViewModel.enviarPedido(usuarioId) {
                                        // Solo se ejecuta si el pedido fue aceptado.
                                        // Vuelve a pedir el catálogo para ver el stock real.
                                        productoViewModel.refrescar()
                                        mostrarCarrito = false
                                        mostrarHistorial = true
                                    }
                                }
                            }
                        )
                    }

                    // ---------- Historial ----------
                    mostrarHistorial -> {

                        HistorialScreen(
                            // Identificadores temporales de prueba.
                            usuarioId = if (esAdmin) {
                                "admin-demo"
                            } else {
                                "cliente-demo"
                            },

                            esAdmin = esAdmin,
                            viewModel = historialViewModel,

                            onVolver = {
                                mostrarHistorial = false
                            },

                            // Se ejecuta cuando el administrador
                            // selecciona "Preparar pedido (QR)".
                            onPrepararPedido = { pedido ->

                                if (
                                    esAdmin &&
                                    pedido.estado == EstadoPedido.CONFIRMADO
                                ) {

                                    pedidoPrepararId = pedido.id
                                    mostrarHistorial = false

                                } else {

                                    Toast.makeText(
                                        this@MainActivity,
                                        "El pedido debe estar confirmado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    // ---------- Home administrador ----------
                    esAdmin -> {

                        HomeAdmin(
                            onLogout = {
                                SesionRepository.cerrarSesion()
                                historialViewModel.limpiar()
                                mostrarHistorial = false
                                mostrarCarrito = false
                                pedidoPrepararId = null
                                sesionIniciada = false
                            },

                            onAbrirHistorial = {
                                mostrarHistorial = true
                            },

                            // Catálogo y ViewModels compartidos.
                            viewModel = productoViewModel,
                            historialViewModel = historialViewModel,
                            perfilViewModel = perfilViewModel,

                            nombre = usuarioSesion?.nombre ?: "Administrador"
                        )
                    }

                    // ---------- Home cliente ----------
                    else -> {

                        HomeCliente(
                            nombre = usuarioSesion?.nombre ?: "Cliente",

                            // Identificador temporal hasta implementar sesiones.
                            clienteId = "cliente-demo",

                            // Compartimos el catálogo y el carrito.
                            productoViewModel = productoViewModel,
                            carritoViewModel = carritoViewModel,
                            historialViewModel = historialViewModel,
                            perfilViewModel = perfilViewModel,

                            onLogout = {
                                SesionRepository.cerrarSesion()
                                historialViewModel.limpiar()
                                mostrarHistorial = false
                                mostrarCarrito = false
                                pedidoPrepararId = null
                                sesionIniciada = false
                            },

                            onAbrirHistorial = {
                                mostrarHistorial = true
                            },

                            onAbrirCarrito = {
                                mostrarCarrito = true
                            }
                        )
                    }
                }
            }
        }
    }
}

