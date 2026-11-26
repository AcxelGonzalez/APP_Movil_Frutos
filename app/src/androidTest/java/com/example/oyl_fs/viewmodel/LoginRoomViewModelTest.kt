
package com.example.oyl_fs.viewmodel

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.oyl_fs.data.local.OylDatabase
import com.example.oyl_fs.repository.DatosRegistroCliente
import com.example.oyl_fs.repository.ResultadoRegistro
import com.example.oyl_fs.repository.UsuarioRoomRepository

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprueba que el Login identifique
 * correctamente usuarios demo y clientes
 * registrados mediante Room.
 */
@RunWith(AndroidJUnit4::class)
class LoginRoomViewModelTest {

    @Test
    fun autenticaCuentasYSeparaIdentidades() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val base = Room.inMemoryDatabaseBuilder(
            contexto,
            OylDatabase::class.java
        ).build()

        try {

            val repositorio = UsuarioRoomRepository(base)
            val login = LoginViewModel(repositorio)

            // CASO 1: administrador de demostración.
            val admin = login.autenticarCredenciales(
                "admin",
                "admin"
            )

            assertTrue(admin is ResultadoLogin.Exito)

            val sesionAdmin =
                (admin as ResultadoLogin.Exito).sesion

            assertTrue(sesionAdmin.esAdmin)
            assertEquals(
                "admin-demo",
                sesionAdmin.usuarioId
            )

            // CASO 2: cliente de demostración.
            val clienteDemo = login.autenticarCredenciales(
                "cliente",
                "cliente"
            )

            assertTrue(
                clienteDemo is ResultadoLogin.Exito
            )

            val sesionDemo =
                (clienteDemo as ResultadoLogin.Exito).sesion

            assertFalse(sesionDemo.esAdmin)
            assertEquals(
                "cliente-demo",
                sesionDemo.usuarioId
            )

            // CASO 3: registrar dos clientes reales
            // de demostración en SQLite.
            val registroA = repositorio.registrarCliente(
                DatosRegistroCliente(
                    alias = "cliente_a",
                    nombre = "Ana",
                    correo = "ana@example.com",
                    clave = "ClaveSegura123!"
                )
            )

            val registroB = repositorio.registrarCliente(
                DatosRegistroCliente(
                    alias = "cliente_b",
                    nombre = "Luis",
                    correo = "luis@example.com",
                    clave = "OtraClave123!"
                )
            )

            assertTrue(
                registroA is ResultadoRegistro.Exito
            )

            assertTrue(
                registroB is ResultadoRegistro.Exito
            )

            // CASO 4: autenticar ambos clientes.
            val resultadoA = login.autenticarCredenciales(
                "CLIENTE_A",
                "ClaveSegura123!"
            )

            val resultadoB = login.autenticarCredenciales(
                "cliente_b",
                "OtraClave123!"
            )

            assertTrue(
                resultadoA is ResultadoLogin.Exito
            )

            assertTrue(
                resultadoB is ResultadoLogin.Exito
            )

            val sesionA =
                (resultadoA as ResultadoLogin.Exito).sesion

            val sesionB =
                (resultadoB as ResultadoLogin.Exito).sesion

            // Los dos clientes tienen IDs diferentes.
            assertNotEquals(
                sesionA.usuarioId,
                sesionB.usuarioId
            )

            assertNotEquals(
                "cliente-demo",
                sesionA.usuarioId
            )

            assertFalse(sesionA.esAdmin)
            assertFalse(sesionB.esAdmin)
            assertFalse(sesionA.esDemo)
            assertFalse(sesionB.esDemo)

            // CASO 5: contraseña incorrecta.
            val incorrecto = login.autenticarCredenciales(
                "cliente_a",
                "ClaveIncorrecta123!"
            )

            assertTrue(
                incorrecto is ResultadoLogin.Error
            )

            // CASO 6: usuario inexistente.
            val inexistente = login.autenticarCredenciales(
                "no_existe",
                "ClaveSegura123!"
            )

            assertTrue(
                inexistente is ResultadoLogin.Error
            )

            // CASO 7: campos vacíos.
            val vacio = login.autenticarCredenciales(
                "",
                ""
            )

            assertTrue(
                vacio is ResultadoLogin.Error
            )

        } finally {
            base.close()
        }
    }
}
