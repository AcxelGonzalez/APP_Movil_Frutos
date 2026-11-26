package com.example.oyl_fs.viewmodel

import com.example.oyl_fs.model.*
import com.example.oyl_fs.util.ValidacionContrasena
import org.junit.Assert.*
import org.junit.Test

class LimitesContratosTest {
    @Test fun claveUtf8AceptaElLimiteExactoYRechazaExcesoMultibyte() {
        for (clave in listOf("a".repeat(72), "é".repeat(36), "😀".repeat(18))) {
            assertNull(ValidacionContrasena.errorRegistro(clave))
            assertNull(ValidacionContrasena.errorMaximo(clave))
        }
        for (clave in listOf("a".repeat(73), "é".repeat(37), "😀".repeat(19)))
            assertNotNull(ValidacionContrasena.errorRegistro(clave))
        assertNotNull(ValidacionContrasena.errorRegistro(" ".repeat(8)))
        assertNotNull(ValidacionContrasena.errorRegistro("a".repeat(7)))
        assertNull(ValidacionContrasena.errorMaximo("admin")) // Login conserva cuentas anteriores.
    }
    @Test fun nombreDeRegistroYPerfilCompartenLimiteSinRecortarElTexto() {
        val registro = FormularioRegistro("usuario", "N".repeat(255), "correo@example.test", "12.345.678-5", "912345678", "clave1234", "clave1234")
        val perfil = PerfilUsuario("7", false, registro.nombre, correo = registro.correo)
        assertNull(ValidacionRegistro.validar(registro).nombre)
        assertNull(ValidacionPerfil.validar(perfil).nombre)
        assertNotNull(ValidacionRegistro.validar(registro.copy(nombre = registro.nombre + "N")).nombre)
        assertNotNull(ValidacionPerfil.validar(perfil.copy(nombre = perfil.nombre + "N")).nombre)
        assertEquals(255, perfil.nombre.length)
    }
    @Test fun presentacionExcesivaImpideGuardarConYSinFotografia() {
        val datos = DatosFormularioProducto("Almendras", "P01", "Frutos secos", "1000", "10", "2", true, "P".repeat(255))
        assertNull(ValidacionFormularioProducto.validar(datos).presentacion)
        for (foto in listOf(true, false)) {
            assertNotNull(ValidacionFormularioProducto.validar(datos.copy(presentacion = datos.presentacion + "P", imagenPresente = foto)).presentacion)
        }
    }
}
