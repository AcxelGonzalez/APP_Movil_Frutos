package com.example.oyl_fs

import com.example.oyl_fs.screens.BienvenidaScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import android.os.Bundle
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.*
import com.example.oyl_fs.viewmodel.SesionViewModel
import com.example.oyl_fs.viewmodel.ConfirmacionPedidoViewModel

import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent

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
import com.example.oyl_fs.viewmodel.RegistroViewModel
import com.example.oyl_fs.viewmodel.LoginViewModel

import com.example.oyl_fs.repository.DatosLocalesRemotosRepository
import com.example.oyl_fs.repository.ProductoRemoteRepository
import com.example.oyl_fs.repository.SesionRemotaRepository
import com.example.oyl_fs.repository.UsuarioRoomRepository

import com.example.oyl_fs.ui.theme.OYL_FSTheme

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        ViewModelProvider(this)[SesionViewModel::class.java].verificarVigencia()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val historialViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(HistorialViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return HistorialViewModel(
                    remoto = true,
                    complementos = DatosLocalesRemotosRepository(applicationContext)
                ) as T
            }
        })[HistorialViewModel::class.java]

        // ViewModel para gestionar los productos.
        val productoViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ProductoViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
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

        // Registro remoto; el RUT se conserva como complemento local.
        val registroViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {

                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    require(
                        modelClass.isAssignableFrom(
                            RegistroViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return RegistroViewModel(
                        UsuarioRoomRepository(applicationContext),
                        DatosLocalesRemotosRepository(applicationContext)
                    ) as T
                }
            }
        )[RegistroViewModel::class.java]

        /**
         * Repositorio de usuarios registrados.
         */
        val usuarioRoomRepository =
            UsuarioRoomRepository(applicationContext)

        /**
         * ViewModel del inicio de sesión.
         */
        val loginViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {

                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    require(
                        modelClass.isAssignableFrom(
                            LoginViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return LoginViewModel(
                        usuarioRoomRepository, remoto = true
                    ) as T
                }
            }
        )[LoginViewModel::class.java]

        // ViewModel para verificar los productos de un pedido mediante QR.
        val prepararPedidoViewModel = ViewModelProvider(this)[
            PrepararPedidoViewModel::class.java
        ]

        val sesionViewModel = ViewModelProvider(this)[SesionViewModel::class.java]
        val confirmacionPedidoViewModel = ViewModelProvider(this)[ConfirmacionPedidoViewModel::class.java]

        setContent {
            OYL_FSTheme {

                val perfiles by perfilViewModel.perfiles.collectAsState()
                var mostrarBienvenida by rememberSaveable { mutableStateOf(true) }

                // Observa los pedidos del administrador.
                // Permite actualizar el pedido si cambia su estado.
                val pedidosAdmin by historialViewModel
                    .pedidosParaAdmin
                    .collectAsState()

                val estadoSesion by sesionViewModel.estado.collectAsState()
                val sesion = estadoSesion.sesion
                val sesionIniciada = sesion != null
                val esAdmin = sesion?.esAdmin ?: false
                val usuarioSesionId = sesion?.usuarioId.orEmpty()
                val nombreSesion = sesion?.nombre.orEmpty()
                var datosSesionListos by remember(usuarioSesionId) { mutableStateOf(false) }
                var errorPreparacion by remember(usuarioSesionId) { mutableStateOf<String?>(null) }
                var reintentoSesion by remember { mutableStateOf(0) }

                LaunchedEffect(estadoSesion.error) {
                    estadoSesion.error?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    }
                }

                // Navegación a historial.
                var mostrarHistorial by rememberSaveable {
                    mutableStateOf(false)
                }

                // Navegación a registro.
                var mostrarRegistro by rememberSaveable {
                    mutableStateOf(false)
                }

                // Conserva catálogo, filtros y desplazamiento al volver del carrito.
                val estadoHomeCliente = rememberSaveableStateHolder()

                val estadoCompra by confirmacionPedidoViewModel.estado.collectAsState()
                val procesandoCompra = estadoCompra.procesando
                var procesandoPreparacion by remember {
                    mutableStateOf(false)
                }
                var pestanaHome by rememberSaveable { mutableStateOf(0) }
                var pestanaDesdeHistorial by rememberSaveable { mutableStateOf<Int?>(null) }

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

                /**
                 * Cierra la sesión sin eliminar los pedidos,
                 * las cuentas ni los carritos de otros clientes.
                 */
                fun cerrarSesionActual() {
                    if (procesandoCompra || procesandoPreparacion) {
                        Toast.makeText(this@MainActivity, "Espera a que termine la operación", Toast.LENGTH_SHORT).show()
                        return
                    }
                    sesionViewModel.cerrar()
                }

                LaunchedEffect(sesion, reintentoSesion) {
                    datosSesionListos = false
                    errorPreparacion = null
                    try {
                        if (sesion != null) {
                            mostrarBienvenida = false
                            perfilViewModel.activarRemoto(requireNotNull(SesionRemotaRepository.usuario.value))
                            productoViewModel.refrescar()
                            carritoViewModel.iniciar(if (sesion.esAdmin) "" else sesion.usuarioId)
                            historialViewModel.configurarSesion(sesion.usuarioId, sesion.esAdmin)
                            datosSesionListos = true
                        } else {
                            carritoViewModel.iniciar("")
                            historialViewModel.configurarSesion("", false)
                            perfilViewModel.limpiarSesionRegistrada()
                            mostrarHistorial = false
                            mostrarCarrito = false
                            pedidoPrepararId = null
                            pestanaHome = 0
                            pestanaDesdeHistorial = null
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        errorPreparacion = "No se pudieron recuperar los datos de la cuenta"
                    }
                }

                val errorCatalogo by productoViewModel.errorCatalogo.collectAsState()
                val cargandoCatalogo by productoViewModel.cargandoProductos.collectAsState()
                Column(Modifier.fillMaxSize()) {
                    if (sesionIniciada && datosSesionListos) {
                        if (cargandoCatalogo) LinearProgressIndicator(Modifier.fillMaxWidth())
                        errorCatalogo?.let { mensaje ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(mensaje, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = { productoViewModel.refrescar() }, enabled = !cargandoCatalogo) { Text("Reintentar catálogo") }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        when {
                            estadoSesion.restaurando || estadoSesion.procesando || (sesionIniciada && !datosSesionListos) -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (errorPreparacion == null) {
                                        CircularProgressIndicator()
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(errorPreparacion.orEmpty())
                                            Button(onClick = { reintentoSesion++ }) { Text("Reintentar") }
                                            TextButton(onClick = { cerrarSesionActual() }) { Text("Cerrar sesión") }
                                        }
                                    }
                                }
                            }

                            // ---------- Bienvenida, login y registro ----------
                            !sesionIniciada && mostrarBienvenida -> BienvenidaScreen(
                                onComenzar = { mostrarBienvenida = false }
                            )
                            !sesionIniciada -> {

                                if (mostrarRegistro) {


                                    RegistroScreen(
                                        viewModel = registroViewModel,

                                        onRegistrado = {
                                            mostrarRegistro = false

                                            Toast.makeText(
                                                this@MainActivity,
                                                "Cuenta creada correctamente. Inicia sesión con tu alias y contraseña.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        },

                                        onIrALogin = {
                                            mostrarRegistro = false
                                        }
                                    )

                                } else {


                                    LoginScreen(
                                        viewModel = loginViewModel,

                                        onLoginSuccess = { identificada ->
                                            mostrarHistorial = false
                                            mostrarCarrito = false
                                            mostrarRegistro = false
                                            pedidoPrepararId = null
                                            pestanaDesdeHistorial = null
                                            sesionViewModel.iniciar(identificada)
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

                                            val puedeConfirmar =
                                                pedidoId == pedidoParaPreparar.id &&
                                                        pedidoParaPreparar.estado ==
                                                        EstadoPedido.CONFIRMADO &&
                                                        prepararPedidoViewModel.pedidoCompleto()

                                            if (!puedeConfirmar) {

                                                Toast.makeText(
                                                    this@MainActivity,
                                                    "El pedido no cumple los requisitos de preparación",
                                                    Toast.LENGTH_LONG
                                                ).show()

                                            } else if (!procesandoPreparacion) {

                                                procesandoPreparacion = true

                                                lifecycleScope.launch {

                                                    try {

                                                        val actualizado = historialViewModel
                                                            .cambiarEstadoPedidoPersistente(
                                                                pedidoId = pedidoId,
                                                                nuevoEstado = EstadoPedido.PREPARADO
                                                            )

                                                        if (actualizado) {

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "Pedido preparado correctamente",
                                                                Toast.LENGTH_LONG
                                                            ).show()

                                                            pedidoPrepararId = null
                                                            mostrarHistorial = true

                                                        } else {

                                                            Toast.makeText(
                                                                this@MainActivity,
                                                                "No se pudo actualizar el pedido",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }

                                                    } catch (e: CancellationException) {
                                                        throw e

                                                    } catch (e: Exception) {

                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Error al guardar la preparación",
                                                            Toast.LENGTH_LONG
                                                        ).show()

                                                    } finally {
                                                        procesandoPreparacion = false
                                                    }
                                                }
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
                                    // Identificador del cliente autenticado.
                                    clienteId = usuarioSesionId,

                                    // ViewModel compartido con HomeCliente.
                                    vm = carritoViewModel,

                                    onVolver = {
                                        mostrarCarrito = false
                                    },


                                    perfil = perfiles.find { it.id == usuarioSesionId },
                                    confirmacionVm = confirmacionPedidoViewModel,
                                    onMisPedidos = {
                                        mostrarCarrito = false
                                        historialViewModel.refrescar()
                                        productoViewModel.refrescar()
                                        mostrarHistorial = true
                                    }

                                )
                            }

                            // ---------- Historial ----------
                            mostrarHistorial -> {

                                HistorialScreen(
                                    // Identidad y rol de la sesión actual.
                                    usuarioId = usuarioSesionId,

                                    esAdmin = esAdmin,
                                    viewModel = historialViewModel,
                                    onNavegar = { indice ->
                                        pestanaDesdeHistorial = indice
                                        mostrarHistorial = false
                                    },

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
                                    pestanaSeleccionada = pestanaHome,
                                    onSeleccionarPestana = { pestanaHome = it },
                                    pestanaSolicitada = pestanaDesdeHistorial,
                                    onPestanaAtendida = { pestanaDesdeHistorial = null },
                                    onLogout = {
                                        cerrarSesionActual()
                                    },

                                    onAbrirHistorial = {
                                        mostrarHistorial = true
                                    },

                                    // Catálogo y ViewModels compartidos.
                                    viewModel = productoViewModel,
                                    historialViewModel = historialViewModel,
                                    perfilViewModel = perfilViewModel,

                                    nombre = perfiles.find {
                                        it.id == usuarioSesionId
                                    }?.nombre ?: "Administrador"
                                )
                            }

                            // ---------- Home cliente ----------
                            else -> {

                                estadoHomeCliente.SaveableStateProvider(usuarioSesionId) {
                                HomeCliente(
                                    pestanaSeleccionada = pestanaHome,
                                    onSeleccionarPestana = { pestanaHome = it },
                                    pestanaSolicitada = pestanaDesdeHistorial,
                                    onPestanaAtendida = { pestanaDesdeHistorial = null },

                                    nombre = perfiles.find {
                                        it.id == usuarioSesionId
                                    }?.nombre ?: nombreSesion,


                                    // Identificador del cliente autenticado.
                                    clienteId = usuarioSesionId,

                                    // Compartimos el catálogo y el carrito.
                                    productoViewModel = productoViewModel,
                                    carritoViewModel = carritoViewModel,
                                    historialViewModel = historialViewModel,
                                    perfilViewModel = perfilViewModel,

                                    onLogout = {
                                        cerrarSesionActual()
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
        }
    }
}
