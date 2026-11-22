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

import com.example.oyl_fs.repository.UsuarioRoomRepository
import com.example.oyl_fs.repository.ProductoRoomRepository
import com.example.oyl_fs.repository.PedidoRoomRepository

import com.example.oyl_fs.ui.theme.OYL_FSTheme

import com.example.oyl_fs.repository.ProductoRemoteRepository
import com.example.oyl_fs.repository.SesionRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        /**
         * Repositorio compartido de pedidos.
         *
         * Gestiona la persistencia en SQLite.
         */
        val pedidoRoomRepository = PedidoRoomRepository(
            applicationContext
        )

        /**
         * HistorialViewModel utiliza los pedidos
         * almacenados en Room.
         */
        val historialViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {

                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {

                    require(
                        modelClass.isAssignableFrom(
                            HistorialViewModel::class.java
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    return HistorialViewModel(
                        pedidoRoomRepository
                    ) as T
                }
            }
        )[HistorialViewModel::class.java]

        /**
         * Carga los pedidos de demostración
         * solamente cuando corresponde.
         *
         * No descuenta stock durante esta carga.
         */
        lifecycleScope.launch {
            try {
                pedidoRoomRepository
                    .inicializarPedidosDemoUnaVez(
                        applicationContext
                    )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "No se pudieron inicializar los pedidos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

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

        /**
         * ViewModel encargado del registro de clientes.
         *
         * Utiliza UsuarioRoomRepository, conectado
         * a la base SQLite versión 3.
         */
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
                        UsuarioRoomRepository(applicationContext)
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
                        usuarioRoomRepository
                    ) as T
                }
            }
        )[LoginViewModel::class.java]

        // ViewModel para verificar los productos de un pedido mediante QR.
        val prepararPedidoViewModel = ViewModelProvider(this)[
            PrepararPedidoViewModel::class.java
        ]

        setContent {
            OYL_FSTheme {

                var mostrarBienvenida by rememberSaveable { mutableStateOf(true) }

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

                /**
                 * Sesión actual.
                 *
                 * Por ahora se conserva solamente en memoria.
                 * Si Android termina el proceso, se solicitará
                 * nuevamente el inicio de sesión.
                 */
                var sesionIniciada by remember {
                    mutableStateOf(false)
                }

                var esAdmin by remember {
                    mutableStateOf(false)
                }

                /**
                 * Identificador exclusivo del usuario
                 * que inició sesión.
                 */
                var usuarioSesionId by remember {
                    mutableStateOf("")
                }

                /**
                 * Mantener actualizado el carrito
                 * solamente del cliente autenticado.
                 */
                LaunchedEffect(
                    productosActuales,
                    usuarioSesionId,
                    esAdmin
                ) {

                    if (
                        usuarioSesionId.isNotBlank() &&
                        !esAdmin
                    ) {

                        carritoViewModel.sincronizarConCatalogo(
                            clienteId = usuarioSesionId,
                            productos = productosActuales
                        )
                    }
                }

                /**
                 * Nombre visible del usuario autenticado.
                 */
                var nombreSesion by remember {
                    mutableStateOf("")
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

                var procesandoCompra by remember { mutableStateOf(false) }
                var procesandoPreparacion by remember {
                    mutableStateOf(false)
                }
                var pestanaHome by rememberSaveable { mutableStateOf(0) }

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

                    val idAnterior = usuarioSesionId
                    SesionRepository.cerrarSesion()
                    historialViewModel.limpiar()

                    // Limpiar la selección del carrito.
                    carritoViewModel.iniciar("")

                    // El Historial deja de estar asociado
                    // con el usuario anterior.
                    historialViewModel.configurarSesion(
                        usuarioId = "",
                        esAdmin = false
                    )

                    // No mantener visibles los perfiles
                    // registrados de la sesión anterior.
                    perfilViewModel.limpiarError()

                    // Eliminar el estado visual guardado
                    // para el cliente que cerró sesión.
                    if (idAnterior.isNotBlank() && !esAdmin) {
                        estadoHomeCliente.removeState(idAnterior)
                    }

                    mostrarHistorial = false
                    mostrarCarrito = false
                    mostrarRegistro = false
                    pedidoPrepararId = null
                    pestanaHome = 0

                    usuarioSesionId = ""
                    nombreSesion = ""
                    esAdmin = false
                    sesionIniciada = false
                }

                when {
                    mostrarBienvenida -> BienvenidaScreen(
                        onComenzar = { mostrarBienvenida = false }
                    )

                    // ---------- Login y registro ----------
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
                                onLoginSuccess = { sesion ->
                                    esAdmin = sesion.esAdmin
                                    usuarioSesionId = sesion.usuarioId
                                    nombreSesion = sesion.nombre
                                    productoViewModel.refrescar()
                                    mostrarHistorial = false
                                    mostrarCarrito = false
                                    pedidoPrepararId = null
                                    sesionIniciada = true

                                    // Carga los pedidos del usuario desde el backend.
                                    historialViewModel.configurarSesion(
                                        sesion.usuarioId,
                                        sesion.esAdmin
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
                                    val puedeConfirmar = pedidoId == pedidoParaPreparar.id &&
                                        pedidoParaPreparar.estado == EstadoPedido.CONFIRMADO &&
                                        prepararPedidoViewModel.pedidoCompleto()
                                    if (!puedeConfirmar) {
                                        Toast.makeText(this@MainActivity,
                                            "Verifica todos los productos del pedido antes de confirmar",
                                            Toast.LENGTH_LONG).show()
                                    } else if (!procesandoPreparacion) {
                                        procesandoPreparacion = true
                                        historialViewModel.cambiarEstadoPedido(pedidoId, EstadoPedido.PREPARADO) { error ->
                                            procesandoPreparacion = false
                                            if (error == null) {
                                                pedidoPrepararId = null
                                                mostrarHistorial = true
                                            }
                                            Toast.makeText(this@MainActivity,
                                                error ?: "Pedido $pedidoId preparado correctamente",
                                                Toast.LENGTH_LONG).show()
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
                            // Identificador temporal del cliente.
                            clienteId = usuarioSesionId,

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
                            usuarioId = usuarioSesionId,

                            esAdmin = esAdmin,
                            viewModel = historialViewModel,
                            onNavegar = { indice ->
                                pestanaHome = indice
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
                            onLogout = { cerrarSesionActual() },

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

                        estadoHomeCliente.SaveableStateProvider(usuarioSesionId) {
                        HomeCliente(
                            pestanaSeleccionada = pestanaHome,
                            onSeleccionarPestana = { pestanaHome = it },
                            nombre = usuarioSesion?.nombre ?: "Cliente",

                            // Identificador temporal hasta implementar sesiones.
                            clienteId = usuarioSesionId,

                            // Compartimos el catálogo y el carrito.
                            productoViewModel = productoViewModel,
                            carritoViewModel = carritoViewModel,
                            historialViewModel = historialViewModel,
                            perfilViewModel = perfilViewModel,

                            onLogout = { cerrarSesionActual() },

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

