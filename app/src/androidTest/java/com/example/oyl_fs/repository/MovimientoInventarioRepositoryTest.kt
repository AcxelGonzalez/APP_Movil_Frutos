package com.example.oyl_fs.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.oyl_fs.data.local.PerfilRemotoDatabase
import com.example.oyl_fs.model.ProductoInfo
import com.example.oyl_fs.network.*
import com.example.oyl_fs.viewmodel.SesionIdentificada
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.UUID

class MovimientoInventarioRepositoryTest {
    private val contexto = ApplicationProvider.getApplicationContext<Context>()
    private val nombre = "movimientos-prueba-${UUID.randomUUID()}.db"
    private lateinit var base: PerfilRemotoDatabase
    private lateinit var repo: MovimientoInventarioRepository
    private var actor = "7"
    private val producto = ProductoInfo(10, "Almendras", "M10", "Frutos secos", "", "500 g", 1000, 10, 2, true)
    private val api = Api()
    private class Api : MovimientosStockApi {
        var envios = 0; var consultas = 0; var aplicaciones = 0
        var stock = 10; var fallarAntes = false; var perderRespuesta = false; var respuestaIncorrecta = false
        val resultados = mutableMapOf<String, MovimientoStockDto>()
        override suspend fun consultar(clave: String): MovimientoStockDto {
            consultas++
            return resultados[clave] ?: MovimientoStockDto(7, clave, null, null, "NO_REGISTRADA", null, null, "No registrado")
        }
        override suspend fun continuar(clave: String, solicitud: MovimientoStockRequest): MovimientoStockDto {
            envios++
            if (fallarAntes) throw IOException("Sin conexión simulada")
            val resultado = resultados.getOrPut(clave) {
                val antes = stock; stock += solicitud.cambio; aplicaciones++
                MovimientoStockDto(7, clave, solicitud.productoId, solicitud.cambio, "APLICADA", antes, stock, "Registrado")
            }
            if (perderRespuesta) { perderRespuesta = false; throw IOException("Respuesta perdida después de aplicar") }
            return if (respuestaIncorrecta) resultado.copy(cambio = solicitud.cambio + 1) else resultado
        }
    }
    @Before fun preparar() { abrir() }
    private fun abrir() {
        base = Room.databaseBuilder(contexto, PerfilRemotoDatabase::class.java, nombre).build()
        repo = MovimientoInventarioRepository(base, api) {
            SesionActiva(SesionIdentificada(actor, "Administrador", true, false), "sesion-$actor", Long.MAX_VALUE)
        }
    }
    @After fun limpiar() { base.close(); contexto.deleteDatabase(nombre) }

    @Test fun respuestaPerdidaSeRecuperaTrasReabrirSinRepetirAplicacion() = runBlocking {
        api.perderRespuesta = true
        try { repo.iniciar("7", producto, 3); fail("Debe simular la pérdida") } catch (_: IOException) { }
        val clave = repo.obtener("7")!!.clave
        assertEquals(13, api.stock)
        base.close(); abrir()
        val recuperado = repo.recuperar("7", clave, continuar = true)
        assertEquals("APLICADA", recuperado.estado)
        assertEquals(clave, recuperado.clave)
        assertEquals(2, api.envios)
        assertEquals(1, api.aplicaciones)
        assertEquals(13, api.stock)
        repo.recuperar("7", clave, continuar = true)
        assertEquals(2, api.envios) // Un terminal local tampoco reenvía.
    }

    @Test fun consultarEsSoloLecturaYElPendienteNoSePuedeDescartar() = runBlocking {
        api.fallarAntes = true
        try { repo.iniciar("7", producto, -2); fail() } catch (_: IOException) { }
        val clave = repo.obtener("7")!!.clave
        api.fallarAntes = false
        val consulta = repo.recuperar("7", clave, continuar = false)
        assertFalse(consulta.finalizado)
        assertEquals(1, api.envios)
        assertEquals(1, api.consultas)
        assertEquals(10, api.stock)
        repo.cerrar("7", clave)
        assertNotNull(repo.obtener("7"))
        repo.recuperar("7", clave, continuar = true)
        assertEquals(8, api.stock)
        repo.cerrar("7", clave)
        assertNull(repo.obtener("7"))
    }

    @Test fun otroMovimientoNoReemplazaLaCantidadNiLaClaveOriginal() = runBlocking {
        api.fallarAntes = true
        try { repo.iniciar("7", producto, 3) } catch (_: IOException) { }
        val original = repo.obtener("7")!!
        try { repo.iniciar("7", producto.copy(id = 11), 4); fail() } catch (_: IllegalStateException) { }
        assertEquals(original, repo.obtener("7"))
        assertEquals(1, api.envios)
    }

    @Test fun registroDeOtraCuentaNoSePuedeLeerNiContinuar() = runBlocking {
        api.fallarAntes = true
        try { repo.iniciar("7", producto, 3) } catch (_: IOException) { }
        val clave = repo.obtener("7")!!.clave
        actor = "8"
        assertNull(repo.obtener("8"))
        try { repo.recuperar("7", clave, true); fail() } catch (_: IllegalStateException) { }
        assertEquals(1, api.envios)
        actor = "7"
        assertEquals(clave, repo.obtener("7")!!.clave)
    }

    @Test fun comprobanteConOtroCambioNoSeConfirmaNiSePierdeElPendiente() = runBlocking {
        api.respuestaIncorrecta = true
        try { repo.iniciar("7", producto, 3); fail() } catch (_: IllegalStateException) { }
        assertEquals("EN_REVISION", repo.obtener("7")!!.estado)
        val clave = repo.obtener("7")!!.clave
        val recuperado = repo.recuperar("7", clave, false)
        assertEquals("APLICADA", recuperado.estado)
        assertEquals(1, api.aplicaciones)
    }
}
