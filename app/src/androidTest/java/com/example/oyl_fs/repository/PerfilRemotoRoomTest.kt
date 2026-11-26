package com.example.oyl_fs.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.data.local.PerfilRemotoDatabase
import com.example.oyl_fs.network.UsuarioDto
import com.example.oyl_fs.viewmodel.PerfilViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PerfilRemotoRoomTest {
    private val contexto = ApplicationProvider.getApplicationContext<Application>()
    private val nombre = "perfiles-prueba-${UUID.randomUUID()}"
    private val preferencias by lazy { contexto.getSharedPreferences(nombre, Context.MODE_PRIVATE) }
    private lateinit var base: PerfilRemotoDatabase
    private lateinit var repo: DatosLocalesRemotosRepository
    private val cliente = UsuarioDto(7, "Cliente ficticio", "cliente7", "cliente7@example.test", "912345678", "Calle ficticia 100", "CLIENTE")

    @Before fun preparar() { abrir() }
    private fun abrir() {
        base = Room.databaseBuilder(contexto, PerfilRemotoDatabase::class.java, "$nombre.db").build()
        repo = DatosLocalesRemotosRepository(preferencias, base)
    }
    @After fun limpiar() {
        base.close()
        contexto.deleteDatabase("$nombre.db")
        contexto.deleteSharedPreferences(nombre)
    }

    @Test fun migraRutYApellidoYGuardaLaCopiaCompletaSinModificarPreferencias() = runBlocking {
        val anterior = """{"rut":"12.345.678-5","apellido":"Apellido ficticio"}"""
        assertTrue(preferencias.edit().putString("perfil.7", anterior).commit())
        val perfil = repo.perfil(cliente)
        assertEquals("12.345.678-5", perfil.rut)
        assertEquals("Apellido ficticio", perfil.apellido)
        assertEquals(cliente.nombre, perfil.nombre)
        assertEquals(cliente.correo, perfil.correo)
        assertEquals(cliente.telefono, perfil.telefono)
        assertEquals(cliente.direccion, perfil.direccion)
        assertEquals(perfil, base.perfilDao().obtener(7)?.aPerfil())
        assertEquals(anterior, preferencias.getString("perfil.7", null))
    }

    @Test fun nuevaEdicionEnRoomNoEsSobrescritaPorPreferenciasAntiguasAlReabrir() = runBlocking {
        assertTrue(preferencias.edit().putString("perfil.7", """{"rut":"12.345.678-5","apellido":"Anterior"}""").commit())
        repo.perfil(cliente)
        repo.guardarPerfil(cliente, "11.111.111-1", "Nuevo")
        base.close()
        abrir()
        val actualizado = repo.perfil(cliente.copy(nombre = "Nombre desde servidor", telefono = "987654321"))
        assertEquals("Nuevo", actualizado.apellido)
        assertEquals("11.111.111-1", actualizado.rut)
        assertEquals("Nombre desde servidor", actualizado.nombre)
        assertEquals("987654321", actualizado.telefono)
        assertTrue(preferencias.getString("perfil.7", "").orEmpty().contains("Anterior"))
    }

    @Test fun registroGuardaPerfilCompletoYLosComplementosSoloEnRoom() = runBlocking {
        val guardado = repo.guardarPerfil(cliente, "12.345.678-5", "Apellido")
        assertEquals(guardado, base.perfilDao().obtener(7)?.aPerfil())
        assertFalse(preferencias.contains("perfil.7"))
        val sinContacto = repo.perfil(cliente.copy(telefono = null, direccion = null))
        assertEquals("", sinContacto.telefono)
        assertEquals("", sinContacto.direccion)
        assertEquals("Apellido", sinContacto.apellido)
    }

    @Test fun clienteYAdministradorConservanDatosSeparadosInclusoConEscriturasConcurrentes() = runBlocking {
        val admin = cliente.copy(id = 8, nombre = "Administrador ficticio", alias = "admin8", rol = "ADMIN")
        listOf(
            async(Dispatchers.IO) { repo.guardarPerfil(cliente, "12.345.678-5", "Cliente") },
            async(Dispatchers.IO) { repo.guardarPerfil(admin, "11.111.111-1", "Administrador") }
        ).awaitAll()
        assertEquals("Cliente", repo.perfil(cliente).apellido)
        assertFalse(repo.perfil(cliente).esAdmin)
        assertEquals("Administrador", repo.perfil(admin).apellido)
        assertTrue(repo.perfil(admin).esAdmin)
        assertEquals("", repo.perfil(cliente.copy(id = 9)).rut)
    }

    @Test fun preferenciasCorruptasNoSePierdenNiBloqueanOtraCuenta() = runBlocking {
        val corrupto = "{incompleto"
        assertTrue(preferencias.edit().putString("perfil.7", corrupto).commit())
        try { repo.perfil(cliente); fail("No debe descartar los datos anteriores") }
        catch (_: com.google.gson.JsonParseException) { }
        assertNull(base.perfilDao().obtener(7))
        assertEquals(corrupto, preferencias.getString("perfil.7", null))
        assertEquals("", repo.perfil(cliente.copy(id = 8)).rut)
    }

    @Test fun nulosEnPreferenciasNoCreanUnPerfilVacioYSuCorreccionPermiteReintentar() = runBlocking {
        for (texto in listOf("null", """{"rut":null,"apellido":"Anterior"}""", """{"rut":"","apellido":null}""")) {
            assertTrue(preferencias.edit().putString("perfil.7", texto).commit())
            try { repo.perfil(cliente); fail("No debe migrar un complemento nulo") }
            catch (_: IllegalArgumentException) { }
            assertNull(base.perfilDao().obtener(7))
            assertEquals(texto, preferencias.getString("perfil.7", null))
        }
        assertTrue(preferencias.edit().putString("perfil.7", """{"rut":"12.345.678-5"}""").commit())
        assertEquals("12.345.678-5", repo.perfil(cliente).rut)
        assertEquals("", repo.perfil(cliente).apellido)
    }

    @Test fun identidadesInvalidasNoPersistenDatos() = runBlocking {
        for (usuario in listOf(cliente.copy(id = 0), cliente.copy(id = -1), cliente.copy(rol = "OTRO"))) {
            try { repo.guardarPerfil(usuario, "", ""); fail("Debe rechazar identidad/rol inválido") }
            catch (_: IllegalArgumentException) { }
            assertNull(base.perfilDao().obtener(usuario.id))
        }
        for (id in listOf("", "0", "-1", "cliente-demo", "9223372036854775808")) {
            try { repo.guardarPerfil(id, ""); fail("Debe rechazar el identificador") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun activarCerrarYCambiarCuentaNoMezclaPerfilesNiBorraRoom() = runBlocking {
        val baseNegocio = Room.inMemoryDatabaseBuilder(contexto, OylDatabase::class.java).build()
        try {
            repo.guardarPerfil(cliente, "12.345.678-5", "Apellido cliente")
            val viewModel = PerfilViewModel(contexto, UsuarioRoomRepository(baseNegocio), preferencias, repo)
            withContext(Dispatchers.Main) { viewModel.activarRemoto(cliente) }
            assertEquals("Apellido cliente", viewModel.perfiles.value.single().apellido)
            withContext(Dispatchers.Main) { viewModel.limpiarSesionRegistrada() }
            assertTrue(viewModel.perfiles.value.isEmpty())
            assertNotNull(base.perfilDao().obtener(7))
            withContext(Dispatchers.Main) { viewModel.activarRemoto(cliente.copy(id = 8)) }
            assertEquals("8", viewModel.perfiles.value.single().id)
            assertEquals("", viewModel.perfiles.value.single().rut)
            withContext(Dispatchers.Main) { viewModel.activarRemoto(cliente) }
            assertEquals("12.345.678-5", viewModel.perfiles.value.single().rut)
        } finally { baseNegocio.close() }
    }

    @Test fun cargaLocalQueTerminaTrasCerrarSesionNoVuelveAPublicarElPerfil() = runBlocking {
        val baseNegocio = Room.inMemoryDatabaseBuilder(contexto, OylDatabase::class.java).build()
        val liberar = CompletableDeferred<Unit>()
        val bloqueada = CompletableDeferred<Unit>()
        try {
            val viewModel = PerfilViewModel(contexto, UsuarioRoomRepository(baseNegocio), preferencias, repo)
            val bloqueo = async(Dispatchers.IO) {
                base.withTransaction { bloqueada.complete(Unit); liberar.await() }
            }
            bloqueada.await()
            val activacion = async(Dispatchers.Main) { viewModel.activarRemoto(cliente) }
            withContext(Dispatchers.Main) { viewModel.limpiarSesionRegistrada() }
            liberar.complete(Unit)
            bloqueo.await()
            activacion.await()
            assertTrue(viewModel.perfiles.value.isEmpty())
            assertNotNull(base.perfilDao().obtener(7))
        } finally {
            liberar.complete(Unit)
            baseNegocio.close()
        }
    }
}
