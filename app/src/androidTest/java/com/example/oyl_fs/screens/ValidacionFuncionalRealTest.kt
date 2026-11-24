package com.example.oyl_fs.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.lifecycle.ViewModelProvider
import com.example.oyl_fs.MainActivity
import com.example.oyl_fs.viewmodel.PrepararPedidoViewModel
import androidx.core.content.FileProvider
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.oyl_fs.BuildConfig
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.*
import com.example.oyl_fs.repository.*
import com.example.oyl_fs.util.CargadorImagenProducto
import com.example.oyl_fs.util.SesionDispositivoProtegida
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import java.io.File
import retrofit2.HttpException

/** Validación opt-in: crea exclusivamente datos ficticios del identificador QA suministrado. */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ValidacionFuncionalRealTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val argumentos get() = InstrumentationRegistry.getArguments()
    private val qa: String get() = requireNotNull(argumentos.getString("qaId")).also {
        require(Regex("qa[0-9a-f]{12}").matches(it))
    }
    private val registro get() = contexto.getSharedPreferences("validacion-real-$qa",Context.MODE_PRIVATE)
    private var escenario: ActivityScenario<MainActivity>? = null
    private val claveQa = "QAFrutos2026!"
    private fun paso(nombre: String) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2,Bundle().apply { putString("stream","\nOYL_QA_STEP $nombre\n") })
    }
    private fun esperar(texto: String) {
        try {
            compose.waitUntil(30_000) { compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: Throwable) {
            runCatching { captura("fallo") }
            val arbol = runCatching { compose.onAllNodes(isRoot()).onLast().printToString(14) }.getOrDefault("Sin árbol")
            throw AssertionError("No apareció $texto\n$arbol",e)
        }
    }
    private fun pulsar(texto: String) {
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        val nodo = compose.onNodeWithText(texto)
        try { nodo.assertIsDisplayed() } catch (_: AssertionError) { nodo.performScrollTo() }
        nodo.performClick()
    }
    private fun escribir(etiqueta: String, texto: String) {
        val nodo = compose.onNodeWithText(etiqueta)
        try { nodo.assertIsDisplayed() } catch (_: AssertionError) { nodo.performScrollTo() }
        nodo.performTextReplacement(texto)
    }
    private fun captura(nombre: String) {
        File(contexto.cacheDir,"$qa-$nombre.png").outputStream().use {
            compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)
        }
    }
    private fun abrirLogin() {
        escenario?.close()
        runBlocking { SesionRemotaRepository.cerrar() }
        escenario = ActivityScenario.launch(MainActivity::class.java)
        esperar("Comenzar"); pulsar("Comenzar"); esperar("Iniciar sesión")
    }
    private fun ingresar(alias: String, clave: String, destino: String) {
        escribir("Usuario",alias); escribir("Contraseña",clave); pulsar("Iniciar sesión"); esperar(destino)
    }
    private fun pedidoTexto(id: Long) = "PED-" + id.toString().padStart(3,'0')
    private fun pulsarEnPedido(id: Long, texto: String) {
        val otros = runBlocking { RetrofitClient.pedidos.listar().map { pedidoTexto(it.id) }.filterNot { it == pedidoTexto(id) } }
        var grupo = hasAnyDescendant(hasText(pedidoTexto(id)))
        for (otro in otros) grupo = grupo and !hasAnyDescendant(hasText(otro))
        compose.onAllNodes(hasScrollToIndexAction()).onLast().performScrollToNode(hasText(pedidoTexto(id)))
        val boton = hasText(texto) and hasAnyAncestor(grupo) and isEnabled()
        compose.waitUntil(15_000) { compose.onAllNodes(boton).fetchSemanticsNodes().size == 1 }
        compose.onNode(boton).performScrollTo().performClick()
    }
    private fun comprar(producto: ProductoDto, etiqueta: String): PedidoDto {
        val usuarioId = requireNotNull(SesionRemotaRepository.usuarioId)
        val previos = runBlocking { RetrofitClient.pedidos.listarPorUsuario(usuarioId) }
            .filter { it.detalles.any { d -> d.productoId == producto.id } }
        if (previos.isNotEmpty()) {
            require(previos.size == 1) { "No repetir una compra QA existente" }
            check(registro.edit().putLong(etiqueta,previos.single().id).commit())
            return previos.single()
        }
        pulsar("Catálogo"); esperar("Buscar productos...")
        escribir("Buscar productos...",producto.nombre); Espresso.closeSoftKeyboard()
        esperar(producto.nombre)
        esperar("Detalle"); pulsar("Detalle")
        esperar("Detalle del producto")
        val carrito = runBlocking { CarritoRoomRepository(contexto).observar(usuarioId.toString()).first() }
        if (carrito.isEmpty()) {
            pulsar("Agregar al carrito")
            compose.waitUntil(15_000) { runBlocking { CarritoRoomRepository(contexto).observar(usuarioId.toString()).first().sumOf { it.cantidad } } == 1 }
            compose.waitForIdle()
            compose.waitUntil(15_000) { compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty() }
        } else require(carrito.size == 1 && carrito.single().productoId == producto.id.toString() && carrito.single().cantidad == 1)
        pulsar("Ver carrito"); esperar("Mi carrito"); pulsar("Confirmar pedido")
        esperar("Nombre de contacto"); pulsar("Entrega a domicilio")
        escribir("Dirección de entrega","Calle ficticia QA 100"); pulsar("Finalizar pedido")
        esperar("Pedido registrado")
        val pedido = runBlocking { RetrofitClient.pedidos.listarPorUsuario(usuarioId).single { p -> p.detalles.any { d -> d.productoId == producto.id } } }
        check(registro.edit().putLong(etiqueta,pedido.id).commit())
        assertEquals(producto.precio.toLong(),pedido.total)
        assertEquals(1,pedido.detalles.single().cantidad)
        compose.onNodeWithText("Número de solicitud: ${pedidoTexto(pedido.id)}").assertExists()
        captura(etiqueta)
        escenario!!.recreate(); esperar("Pedido registrado")
        assertTrue(runBlocking { CarritoRoomRepository(contexto).observar(usuarioId.toString()).first().isEmpty() })
        pulsar("Ver mis pedidos"); esperar(pedidoTexto(pedido.id))
        return pedido
    }
    private fun completarEntrega() {
        val id = registro.getLong("pedido-entregar",0)
        val idE = registro.getInt("producto-E",0).toLong()
        require(id > 0 && idE > 0)
        abrirLogin(); ingresar("admin","admin","Inicio"); pulsar("Pedidos"); esperar(pedidoTexto(id))
        val pedido = runBlocking { RetrofitClient.pedidos.obtener(id) }
        require(runBlocking { RetrofitClient.productos.obtener(idE).codigo } == qa.uppercase()+"-E") {
            "El producto de entrega no pertenece al identificador QA"
        }
        require(pedido.usuarioId == registro.getLong("usuario-qa",0) && pedido.detalles.single().productoId == idE) {
            "El pedido no corresponde a esta validación QA"
        }
        if (pedido.estado == "PENDIENTE_VALIDACION") {
            pulsarEnPedido(id,"Cambiar estado")
            compose.onNode(hasText("Confirmado") and hasAnyAncestor(isPopup())).performClick()
            compose.waitUntil(15_000) { runBlocking { RetrofitClient.pedidos.obtener(id).estado } == "CONFIRMADO" }
        }
        if (runBlocking { RetrofitClient.pedidos.obtener(id).estado } == "CONFIRMADO") {
            pulsarEnPedido(id,"Preparar pedido (QR)"); esperar("Confirmar pedido preparado")
            if (argumentos.getString("camaraReal") == "true") {
                paso("QR_CAMARA_LISTA"); pulsar("Escanear producto")
                compose.waitUntil(120_000) {
                    // El lector Google se abre en otra actividad y temporalmente no hay raíces Compose.
                    runCatching {
                        compose.onAllNodes(hasText("Confirmar pedido preparado") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
                    }.getOrDefault(false)
                }
                paso("QR_CAMARA_REAL_OK")
            } else {
                escenario!!.onActivity { ViewModelProvider(it)[PrepararPedidoViewModel::class.java].validarQr(com.example.oyl_fs.util.textoQrProducto(idE.toInt())) }
                paso("QR_PROPORCIONADO_AL_VIEWMODEL_CAMARA_PENDIENTE")
            }
            pulsar("Confirmar pedido preparado")
            compose.waitUntil(15_000) { runBlocking { RetrofitClient.pedidos.obtener(id).estado } == "PREPARADO" }
        }
        if (runBlocking { RetrofitClient.pedidos.obtener(id).estado } == "PREPARADO") {
            esperar(pedidoTexto(id)); pulsarEnPedido(id,"Cambiar estado")
            compose.onNode(hasText("Entregado") and hasAnyAncestor(isPopup())).performClick()
            compose.waitUntil(15_000) { runBlocking { RetrofitClient.pedidos.obtener(id).estado } == "ENTREGADO" }
        }
        assertEquals("ENTREGADO",runBlocking { RetrofitClient.pedidos.obtener(id).estado })
        captura("entregado")
        assertEquals(9,runBlocking { RetrofitClient.productos.obtener(idE).stock })
        paso("PREPARACION_Y_ENTREGA_OK")
        check(registro.edit().putBoolean("flujos-validados",true).commit())
    }
    private fun habilitar() {
        assumeTrue("Requiere autorización de validación real", argumentos.getString("validacionFuncionalReal") == "true")
        require(BuildConfig.OYL_BACKEND_HOST == "http://127.0.0.1")
    }
    private suspend fun admin() {
        val usuario = SesionRemotaRepository.iniciarSesion("admin","admin").getOrThrow()
        check(usuario.rol == "ADMIN")
        SesionRemotaRepository.guardar(with(SesionRemotaRepository) { usuario.identidad() })
    }
    private fun foto(color: Int, nombre: String): String {
        val carpeta = File(contexto.filesDir,"fotos_productos").apply { mkdirs() }
        val archivo = File(carpeta,"$qa-$nombre.jpg")
        val bitmap = Bitmap.createBitmap(80,60,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        archivo.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG,90,it)) }
        bitmap.recycle()
        return FileProvider.getUriForFile(contexto,"${contexto.packageName}.fotos",archivo).toString()
    }
    private suspend fun producto(letra: String, uri: String): ProductoInfo {
        val codigo = qa.uppercase() + "-" + letra
        val repo = ProductoRemoteRepository(contexto)
        val idGuardado = registro.getInt("producto-$letra",0)
        if (idGuardado > 0) return repo.listar().single { it.id == idGuardado && it.codigo == codigo }
        val existente = repo.listar().singleOrNull { it.codigo == codigo }
        val creado = existente ?: repo.crear(ProductoInfo(0,"QA $qa $letra",codigo,"Frutos secos",
            "Producto ficticio de validación funcional","500 g",1000,10,2,true,imagenUri = uri))
        check(registro.edit().putInt("producto-$letra",creado.id).commit())
        return creado
    }
    private suspend fun movimiento(producto: ProductoInfo, cambio: Int, etiqueta: String): MovimientoInventarioGuardado {
        val actor = requireNotNull(SesionRemotaRepository.usuarioId).toString()
        val repo = MovimientoInventarioRepository(contexto)
        val pendiente = repo.obtener(actor)
        check(pendiente == null || (pendiente.producto.id == producto.id && pendiente.cambio == cambio)) {
            "Existe otro movimiento guardado: no reemplazarlo ni descartarlo desde la validación"
        }
        val resultado = if (pendiente == null) repo.iniciar(actor,producto,cambio)
            else repo.recuperar(actor,pendiente.clave,true)
        assertEquals("APLICADA",resultado.estado)
        check(registro.edit().putString(etiqueta,resultado.clave).commit())
        repo.cerrar(actor,resultado.clave)
        return resultado
    }
    @Test fun a_catalogoFotosYMovimientosContraLosServiciosReales() = runBlocking {
        habilitar()
        if (registro.getBoolean("catalogo-validado",false)) {
            println("OYL_QA fase catálogo previamente validada para $qa")
            return@runBlocking
        }
        SesionDispositivoProtegida(contexto).use {
            admin()
            val repo = ProductoRemoteRepository(contexto)
            val a = producto("C",foto(Color.RED,"roja"))
            val b = producto("E",foto(Color.BLUE,"azul"))
            assertNotNull(a.imagenUri); assertNotNull(b.imagenUri)
            val primera = requireNotNull(a.imagenUri)
            assertTrue(primera.startsWith("/api/productos/${a.id}/foto/"))
            assertNotNull(CargadorImagenProducto.cargar(contexto,primera)?.also { imagen -> imagen.recycle() })
            println("OYL_QA productosConFoto=${a.id},${b.id}")

            val entrada = movimiento(a,3,"entrada-clave")
            assertEquals(13,entrada.stockResultante)
            val repetido = RetrofitClient.movimientosStock.continuar(entrada.clave,MovimientoStockRequest(a.id.toLong(),3))
            assertEquals("APLICADA",repetido.estado)
            assertEquals(13,RetrofitClient.productos.obtener(a.id.toLong()).stock)
            val consulta = RetrofitClient.movimientosStock.consultar(entrada.clave)
            assertEquals(13,consulta.stockResultante)
            try {
                RetrofitClient.movimientosStock.continuar(entrada.clave,MovimientoStockRequest(a.id.toLong(),4))
                fail("Una clave no puede cambiar de cantidad")
            } catch (e: HttpException) { assertEquals(409,e.code()) }

            val editado = repo.actualizar(a.copy(descripcion = "Edición real QA",stock = 999,
                imagenUri = foto(Color.GREEN,"verde")))
            assertEquals(13,editado.stock)
            assertNotEquals(primera,editado.imagenUri)
            val imagen = requireNotNull(CargadorImagenProducto.cargar(contexto,requireNotNull(editado.imagenUri)))
            assertTrue(Color.green(imagen.getPixel(10,10)) > Color.red(imagen.getPixel(10,10)))
            imagen.recycle()
            val salida = movimiento(editado,-3,"salida-clave")
            assertEquals(10,salida.stockResultante)
            // El recibo original no se convierte en el stock actual al consultarlo otra vez.
            assertEquals(13,RetrofitClient.movimientosStock.consultar(entrada.clave).stockResultante)
            assertEquals(10,RetrofitClient.productos.obtener(a.id.toLong()).stock)
            val actor = requireNotNull(SesionRemotaRepository.usuarioId).toString()
            val movimientos = MovimientoInventarioRepository(contexto)
            val rechazado = movimientos.iniciar(actor,a,-11)
            assertEquals("RECHAZADA",rechazado.estado)
            assertEquals(10,RetrofitClient.productos.obtener(a.id.toLong()).stock)
            movimientos.cerrar(actor,rechazado.clave)
            try { repo.crear(a.copy(id = 0,codigo = a.codigo.lowercase(),imagenUri = null)); fail() }
            catch (e: HttpException) { assertEquals(409,e.code()) }
            check(registro.edit().putBoolean("catalogo-validado",true).commit())
            println("OYL_QA catálogo, fotos compartidas, edición conservando stock, idempotencia y rechazo validados")
        }
    }

    @Test fun b_registroCompraCancelacionYEntregaDesdeLaInterfazReal() {
        habilitar()
        require(registro.getBoolean("catalogo-validado",false)) { "Validar primero catálogo e inventario" }
        if (registro.getBoolean("flujos-validados",false)) return
        val sesionAnterior = SesionDispositivoProtegida(contexto)
        try {
            runBlocking { admin() }
            if (registro.getLong("pedido-entregar",0) > 0) {
                val cancelado = runBlocking { RetrofitClient.cancelaciones.consultar(registro.getLong("pedido-cancelar",0)) }
                require(cancelado.estado == "COMPLETADA") { "No repetir compras: revisar la cancelación QA existente" }
                paso("REANUDANDO_ENTREGA_EXISTENTE")
                completarEntrega()
                return
            }
            val idC = registro.getInt("producto-C",0).toLong()
            val idE = registro.getInt("producto-E",0).toLong()
            val productoC = runBlocking { RetrofitClient.productos.obtener(idC) }
            val productoE = runBlocking { RetrofitClient.productos.obtener(idE) }
            require(productoC.codigo == qa.uppercase()+"-C" && productoE.codigo == qa.uppercase()+"-E") {
                "No operar sobre productos ajenos a esta validación QA"
            }
            abrirLogin()
            if (!registro.getBoolean("registro-enviado",false)) {
                pulsar("Crear una cuenta"); esperar("Crear cuenta")
                escribir("Alias de usuario *",qa); escribir("Nombre completo *","QA $qa")
                escribir("Correo electrónico *","$qa@example.test"); escribir("RUT *","12.345.678-5")
                escribir("Teléfono *","912345678"); escribir("Contraseña *",claveQa); escribir("Confirmar contraseña *",claveQa)
                check(registro.edit().putBoolean("registro-enviado",true).commit())
                pulsar("Registrarse"); esperar("Iniciar sesión")
            }
            ingresar(qa,claveQa,"Catálogo")
            val usuario = requireNotNull(SesionRemotaRepository.usuario.value)
            assertEquals("CLIENTE",usuario.rol)
            assertEquals(qa,usuario.alias)
            check(registro.edit().putLong("usuario-qa",usuario.id).commit())
            assertEquals("12.345.678-5",runBlocking { DatosLocalesRemotosRepository(contexto).perfil(usuario) }.rut)
            escenario!!.recreate(); esperar("Catálogo")
            assertEquals(usuario.id,SesionRemotaRepository.usuarioId)
            runBlocking {
                assertNotNull(CargadorImagenProducto.cargar(contexto,requireNotNull(productoE.imagenUrl))?.also { it.recycle() })
                try { RetrofitClient.productos.crear(productoE.copy(id = null,codigo = qa.uppercase()+"-DENEGADO")); fail("Cliente no puede crear productos") }
                catch (e: HttpException) { assertEquals(403,e.code()) }
            }
            paso("REGISTRO_SESION_FOTO_Y_ROL_CLIENTE_OK")

            pulsar("Perfil"); pulsar("Datos personales"); pulsar("Editar datos")
            escribir("Nombre *","QA editado $qa"); escribir("Apellido","Apellido QA")
            pulsar("Guardar"); esperar("Datos personales")
            assertEquals("QA editado $qa",SesionRemotaRepository.usuario.value?.nombre)
            escenario!!.recreate(); esperar("Datos personales")
            val perfil = runBlocking { DatosLocalesRemotosRepository(contexto).perfil(requireNotNull(SesionRemotaRepository.usuario.value)) }
            assertEquals("Apellido QA",perfil.apellido); assertEquals("12.345.678-5",perfil.rut)
            // Reabrir el inicio evita conservar filtros de perfil entre las dos compras QA.
            escenario!!.close(); escenario = ActivityScenario.launch(MainActivity::class.java); esperar("Catálogo")
            val cancelar = comprar(productoC,"pedido-cancelar")
            assertEquals(9,runBlocking { RetrofitClient.productos.obtener(idC).stock })
            paso("COMPRA_Y_COMPROBANTE_RESTAURADO_OK")

            // Eliminar solo el producto QA antes de devolver su compra: caso crítico histórico.
            escenario!!.close(); escenario = null
            runBlocking { SesionRemotaRepository.cerrar(); admin(); ProductoRemoteRepository(contexto).eliminarRegistrado(idC.toInt()) }
            abrirLogin(); ingresar(qa,claveQa,"Catálogo")
            pulsar("Pedidos"); esperar(pedidoTexto(cancelar.id)); pulsar("Cancelar pedido"); pulsar("Solicitar cancelación")
            esperar("Pedido cancelado")
            assertEquals("COMPLETADA",runBlocking { RetrofitClient.cancelaciones.consultar(cancelar.id).estado })
            assertTrue(runBlocking { RetrofitClient.pedidos.listarPorUsuario(usuario.id).none { it.id == cancelar.id } })
            captura("cancelacion")
            // Repetir la misma cancelación conserva el resultado y no vuelve a acreditar stock.
            assertEquals("COMPLETADA",runBlocking { RetrofitClient.cancelaciones.continuar(cancelar.id).estado })
            paso("CANCELACION_DE_PRODUCTO_ELIMINADO_OK")

            escenario!!.close(); escenario = ActivityScenario.launch(MainActivity::class.java); esperar("Catálogo")
            val entregar = comprar(productoE,"pedido-entregar")
            assertEquals(9,runBlocking { RetrofitClient.productos.obtener(idE).stock })
            completarEntrega()
        } catch (e: Throwable) {
            runCatching { captura("fallo-flujos") }
            val arbol = runCatching { compose.onAllNodes(isRoot()).onLast().printToString(14) }.getOrDefault("Sin árbol")
            throw AssertionError("Validación real interrumpida: ${e.message}\n$arbol",e)
        } finally { escenario?.close(); escenario = null; sesionAnterior.close() }
    }
}
