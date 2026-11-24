package com.example.oyl_fs.screens

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.model.EstadoPedido
import com.example.oyl_fs.model.DestinoNotificacion
import com.example.oyl_fs.model.crearNotificacionesAdmin
import com.example.oyl_fs.ui.theme.OYL_FSTheme
import com.example.oyl_fs.viewmodel.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificacionesAdminScreenTest {
    @get:Rule val compose = createComposeRule()
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val preferencias = app.getSharedPreferences("notificaciones_admin", Context.MODE_PRIVATE)
    private var lecturasAnteriores: Set<String>? = null
    private val almacen = ViewModelStore()
    private val modelos = ViewModelProvider(almacen, ViewModelProvider.AndroidViewModelFactory.getInstance(app))

    @Before fun preparar() {
        lecturasAnteriores = preferencias.getStringSet("leidas:admin-demo", null)?.toSet()
        check(preferencias.edit().remove("leidas:admin-demo").commit())
    }

    @After fun limpiar() {
        compose.runOnIdle { almacen.clear() }
        val editor = preferencias.edit()
        val anteriores = lecturasAnteriores
        if (anteriores == null) editor.remove("leidas:admin-demo") else editor.putStringSet("leidas:admin-demo", anteriores)
        check(editor.commit())
    }

    @Test fun contadorLecturasYAccesoAlPedidoUsanLosAvisosDeLaCuentaActual() {
        val productos = modelos[ProductoViewModel::class.java]
        val historial = modelos[HistorialViewModel::class.java]
        var abrioPedidos = false
        compose.setContent { OYL_FSTheme {
            HomeAdmin({}, { abrioPedidos = true }, productos, historial, modelos[PerfilViewModel::class.java])
        } }
        compose.waitUntil(10000) { historial.pedidosParaAdmin.value.any { it.estado == EstadoPedido.PENDIENTE_VALIDACION } }
        compose.onNodeWithContentDescription("Notificaciones:", substring = true).performClick()
        compose.onNodeWithText("Notificaciones").assertExists()
        val avisos = crearNotificacionesAdmin(historial.pedidosParaAdmin.value, productos.productos.value)
        assertTrue(avisos.isNotEmpty())
        compose.onNodeWithText("${avisos.size} sin leer").assertExists()
        compose.onNodeWithText("Marcar leídas").performClick()
        compose.onNodeWithText("0 sin leer").assertExists()
        val pedido = avisos.first { it.destino == DestinoNotificacion.PEDIDOS }
        compose.onNodeWithText(pedido.mensaje).performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(abrioPedidos)
            assertEquals(EstadoPedido.PENDIENTE_VALIDACION, historial.filtroEstado.value)
        }
        assertEquals(avisos.map { it.id }.toSet(), preferencias.getStringSet("leidas:admin-demo", null))
    }
}
