
package com.example.oyl_fs.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.example.oyl_fs.data.local.OylDatabase

import kotlinx.coroutines.runBlocking

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas del registro y autenticación
 * de clientes almacenados en Room.
 */
@RunWith(AndroidJUnit4::class)
class UsuarioRoomRepositoryTest {

    @Test
    fun registraAutenticaYConservaClientes() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val nombreBase = "oyl_usuarios_repo_test.db"

        contexto.deleteDatabase(nombreBase)

        fun crearBase(): OylDatabase {
            return Room.databaseBuilder(
                contexto,
                OylDatabase::class.java,
                nombreBase
            ).build()
        }

        var base = crearBase()

        try {

            var repo = UsuarioRoomRepository(base)

            val datos = DatosRegistroCliente(
                alias = " ClienteUno ",
                nombre = "Ana Prueba",
                correo = " Ana@example.com ",
                clave = "ClaveSegura123!",
                apellido = "Ejemplo"
            )

            // 1. Registrar correctamente.
            val resultado = repo.registrarCliente(datos)

            assertTrue(
                resultado is ResultadoRegistro.Exito
            )

            val usuarioId =
                (resultado as ResultadoRegistro.Exito)
                    .usuarioId

            val guardado = requireNotNull(
                base.usuarioDao().obtenerPorId(usuarioId)
            )

            assertEquals(
                "clienteuno",
                guardado.aliasNormalizado
            )

            assertEquals(
                "ana@example.com",
                guardado.correoNormalizado
            )

            // La contraseña original no se guarda.
            assertNotEquals(
                datos.clave,
                guardado.claveHash
            )

            assertTrue(guardado.claveSalt.isNotBlank())

            // 2. Autenticación correcta.
            val autenticado = repo.autenticarCliente(
                alias = "CLIENTEUNO",
                clave = "ClaveSegura123!"
            )

            assertNotNull(autenticado)
            assertEquals(usuarioId, autenticado?.id)

            // 3. Contraseña incorrecta.
            assertNull(
                repo.autenticarCliente(
                    "clienteuno",
                    "OtraClave123!"
                )
            )

            // 4. Rechazar alias duplicado.
            val aliasDuplicado = repo.registrarCliente(
                datos.copy(
                    alias = "CLIENTEUNO",
                    correo = "otra@example.com"
                )
            )

            assertTrue(
                aliasDuplicado is ResultadoRegistro.Error
            )

            // 5. Rechazar correo duplicado.
            val correoDuplicado = repo.registrarCliente(
                datos.copy(
                    alias = "cliente_dos",
                    correo = "ANA@EXAMPLE.COM"
                )
            )

            assertTrue(
                correoDuplicado is ResultadoRegistro.Error
            )

            // 6. Registrar otro usuario con
            // la misma contraseña de prueba.
            val segundo = repo.registrarCliente(
                datos.copy(
                    alias = "cliente_dos",
                    correo = "dos@example.com"
                )
            )

            assertTrue(
                segundo is ResultadoRegistro.Exito
            )

            val segundoId =
                (segundo as ResultadoRegistro.Exito)
                    .usuarioId

            val guardado2 = requireNotNull(
                base.usuarioDao().obtenerPorId(segundoId)
            )

            // La sal aleatoria evita almacenar
            // el mismo hash para ambos clientes.
            assertNotEquals(
                guardado.claveSalt,
                guardado2.claveSalt
            )

            assertNotEquals(
                guardado.claveHash,
                guardado2.claveHash
            )

            assertEquals(
                2,
                base.usuarioDao().contarUsuarios()
            )

            // 7. Cerrar y reabrir SQLite.
            base.close()
            base = crearBase()

            repo = UsuarioRoomRepository(base)

            assertEquals(
                2,
                base.usuarioDao().contarUsuarios()
            )

            assertEquals(
                usuarioId,
                repo.autenticarCliente(
                    "clienteuno",
                    "ClaveSegura123!"
                )?.id
            )

        } finally {

            base.close()
            contexto.deleteDatabase(nombreBase)
        }
    }

    @Test
    fun rechazaDatosInvalidos() = runBlocking {

        val contexto = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val base = Room.inMemoryDatabaseBuilder(
            contexto,
            OylDatabase::class.java
        ).build()

        try {

            val repo = UsuarioRoomRepository(base)

            val datos = DatosRegistroCliente(
                alias = "usuario_prueba",
                nombre = "Ana",
                correo = "ana@example.com",
                clave = "ClaveSegura123!"
            )

            // Alias reservado.
            assertTrue(
                repo.registrarCliente(
                    datos.copy(alias = "admin")
                ) is ResultadoRegistro.Error
            )

            // Alias demasiado corto.
            assertTrue(
                repo.registrarCliente(
                    datos.copy(alias = "ab")
                ) is ResultadoRegistro.Error
            )

            // Correo incorrecto.
            assertTrue(
                repo.registrarCliente(
                    datos.copy(correo = "correo-invalido")
                ) is ResultadoRegistro.Error
            )

            // Contraseña demasiado corta.
            assertTrue(
                repo.registrarCliente(
                    datos.copy(clave = "1234")
                ) is ResultadoRegistro.Error
            )

            // Ningún registro inválido debe
            // terminar guardado en SQLite.
            assertEquals(
                0,
                base.usuarioDao().contarUsuarios()
            )

        } finally {
            base.close()
        }
    }
}
