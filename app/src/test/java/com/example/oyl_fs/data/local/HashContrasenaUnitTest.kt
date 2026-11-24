package com.example.oyl_fs.data.local

import android.util.Base64
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class HashContrasenaUnitTest {
    @Before fun preparar() {
        // Se adapta únicamente Base64 de Android; PBKDF2 y SecureRandom son reales.
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any<ByteArray>(),any()) } answers { java.util.Base64.getEncoder().encodeToString(firstArg()) }
        every { Base64.decode(any<String>(),any()) } answers { java.util.Base64.getDecoder().decode(firstArg<String>()) }
    }
    @After fun limpiar() { unmockkAll() }
    @Test fun salesAleatoriasVerificanClaveCorrectaYRechazanOtra() {
        val a=HashContrasena.crear("Contraseña123!");val b=HashContrasena.crear("Contraseña123!")
        assertNotEquals(a.salt,b.salt);assertNotEquals(a.hash,b.hash)
        assertTrue(HashContrasena.verificar("Contraseña123!",a.hash,a.salt))
        assertFalse(HashContrasena.verificar("Otra123!",a.hash,a.salt))
        try { HashContrasena.crear("corta");fail() } catch (_:IllegalArgumentException) { }
    }
    @Test fun admiteCredencialSha256YRechazaFormatosYLimitesInvalidos() {
        val salt=ByteArray(16) { it.toByte() }
        val hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec("Clave123!".toCharArray(),salt,100000,256)).encoded
        val b64=java.util.Base64.getEncoder()
        val valido="PBKDF2WithHmacSHA256:100000:${b64.encodeToString(hash)}"
        assertTrue(HashContrasena.verificar("Clave123!",valido,b64.encodeToString(salt)))
        for(malo in listOf("sin-formato","MD5:100000:a","PBKDF2WithHmacSHA256:x:a","PBKDF2WithHmacSHA256:99999:a",
            "PBKDF2WithHmacSHA256:1000001:a","PBKDF2WithHmacSHA256:100000:%%%%","PBKDF2WithHmacSHA256:100000:YQ=="))
            assertFalse(HashContrasena.verificar("Clave123!",malo,b64.encodeToString(salt)))
        assertFalse(HashContrasena.verificar("Clave123!",valido,"YQ=="))
    }
}
