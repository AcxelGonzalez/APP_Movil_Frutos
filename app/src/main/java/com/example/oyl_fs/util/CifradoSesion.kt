package com.example.oyl_fs.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CifradoSesion {
    fun cifrar(texto: String): String
    fun descifrar(texto: String): String
}

/** La clave no sale del Keystore; el contenido e identidad de la sesión quedan autenticados. */
class CifradoSesionKeystore : CifradoSesion {
    private val alias = "oyl_sesion_v1"
    private val aad = "OYL_FS_SESSION_V1".toByteArray(Charsets.UTF_8)
    private fun clave(crear: Boolean): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(alias, null) as? SecretKey)?.let { return it }
        check(crear) { "La clave de sesión ya no está disponible" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    override fun cifrar(texto: String): String {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, clave(true))
        cifrador.updateAAD(aad)
        return "1." + Base64.encodeToString(cifrador.iv, Base64.NO_WRAP) + "." +
            Base64.encodeToString(cifrador.doFinal(texto.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    override fun descifrar(texto: String): String {
        val partes = texto.split('.')
        require(partes.size == 3 && partes[0] == "1")
        val iv = Base64.decode(partes[1], Base64.NO_WRAP)
        val contenido = Base64.decode(partes[2], Base64.NO_WRAP)
        require(iv.size == 12 && contenido.size >= 16)
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE, clave(false), GCMParameterSpec(128, iv))
        cifrador.updateAAD(aad)
        return String(cifrador.doFinal(contenido), Charsets.UTF_8)
    }
}
