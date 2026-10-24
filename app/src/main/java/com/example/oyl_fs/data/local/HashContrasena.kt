package com.example.oyl_fs.data.local

import android.os.Build
import android.util.Base64

import java.security.MessageDigest
import java.security.SecureRandom

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Resultado de proteger una contraseña.
 *
 * Se almacenan el hash y la sal,
 * nunca la contraseña original.
 */
data class CredencialProtegida(
    val hash: String,
    val salt: String
)

/**
 * Protección local de contraseñas mediante PBKDF2.
 *
 * Utiliza:
 * - Sal aleatoria de 16 bytes.
 * - 210.000 iteraciones.
 * - Derivación de 256 bits.
 * - Comparación de tiempo constante.
 *
 * En Android 8 o superior utiliza SHA-256.
 * Para API 24-25 utiliza SHA-1 como PRF
 * compatible de PBKDF2.
 *
 * El algoritmo utilizado queda registrado
 * junto con el hash para su verificación.
 */
object HashContrasena {

    private const val ITERACIONES = 210_000
    private const val LONGITUD_BITS = 256
    private const val LONGITUD_SALT = 16

    private const val SHA256 =
        "PBKDF2WithHmacSHA256"

    private const val SHA1 =
        "PBKDF2WithHmacSHA1"

    /**
     * Genera un hash con una sal nueva.
     */
    fun crear(clave: String): CredencialProtegida {

        require(clave.length >= 8) {
            "La contraseña debe tener al menos 8 caracteres"
        }

        val salt = ByteArray(LONGITUD_SALT)
        SecureRandom().nextBytes(salt)

        val algoritmo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SHA256
            } else {
                SHA1
            }

        val resultado = derivar(
            clave = clave,
            salt = salt,
            algoritmo = algoritmo,
            iteraciones = ITERACIONES
        )

        val hashCodificado = Base64.encodeToString(
            resultado,
            Base64.NO_WRAP
        )

        return CredencialProtegida(
            hash = "$algoritmo:$ITERACIONES:$hashCodificado",
            salt = Base64.encodeToString(
                salt,
                Base64.NO_WRAP
            )
        )
    }

    /**
     * Verifica una contraseña contra los
     * datos protegidos almacenados en SQLite.
     *
     * Devuelve false ante una contraseña
     * incorrecta o un formato no reconocido.
     */
    fun verificar(
        clave: String,
        hashAlmacenado: String,
        saltAlmacenado: String
    ): Boolean {

        return try {

            val partes = hashAlmacenado.split(
                ":",
                limit = 3
            )

            if (partes.size != 3) {
                return false
            }

            val algoritmo = partes[0]

            if (algoritmo != SHA256 && algoritmo != SHA1) {
                return false
            }

            val iteraciones = partes[1].toIntOrNull()
                ?: return false

            if (iteraciones !in 100_000..1_000_000) {
                return false
            }

            val hashEsperado = Base64.decode(
                partes[2],
                Base64.NO_WRAP
            )

            val salt = Base64.decode(
                saltAlmacenado,
                Base64.NO_WRAP
            )

            if (
                hashEsperado.size != LONGITUD_BITS / 8 ||
                salt.size != LONGITUD_SALT
            ) {
                return false
            }

            val hashCalculado = derivar(
                clave = clave,
                salt = salt,
                algoritmo = algoritmo,
                iteraciones = iteraciones
            )

            MessageDigest.isEqual(
                hashEsperado,
                hashCalculado
            )

        } catch (_: Exception) {
            false
        }
    }

    /**
     * Deriva la contraseña utilizando PBKDF2.
     * Limpia las copias temporales cuando termina.
     */
    private fun derivar(
        clave: String,
        salt: ByteArray,
        algoritmo: String,
        iteraciones: Int
    ): ByteArray {

        val caracteres = clave.toCharArray()

        val especificacion = PBEKeySpec(
            caracteres,
            salt,
            iteraciones,
            LONGITUD_BITS
        )

        return try {

            SecretKeyFactory.getInstance(algoritmo)
                .generateSecret(especificacion)
                .encoded

        } finally {
            especificacion.clearPassword()
            caracteres.fill('\u0000')
        }
    }
}
