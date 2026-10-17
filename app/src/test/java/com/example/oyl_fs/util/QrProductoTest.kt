package com.example.oyl_fs.util

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.Assert.assertNull

class QrProductoTest {
    @Test
    fun qrSePuedeLeerYDevuelveElIdentificadorDelProducto() {
        // Prueba los cinco productos iniciales y un ID de producto nuevo.
        listOf(1, 2, 3, 4, 5, 123456).forEach { id ->
            val matriz = generarQrProducto(id)
            val pixeles = IntArray(matriz.width * matriz.height)
            for (y in 0 until matriz.height) {
                for (x in 0 until matriz.width) {
                    pixeles[y * matriz.width + x] = if (matriz[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            val imagen = BinaryBitmap(HybridBinarizer(
                RGBLuminanceSource(matriz.width, matriz.height, pixeles)
            ))
            val leido = QRCodeReader().decode(imagen).text
            assertEquals("""{"tipo":"producto","id":$id}""", leido)
        }
    }

    @Test
    fun productosDistintosTienenIdentificadoresDistintos() {
        assertNotEquals(textoQrProducto(1), textoQrProducto(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rechazaIdentificadoresInvalidos() {
        generarQrProducto(0)
    }

    @Test
    fun qrGeneradoPermiteRecuperarId() {
        val texto = textoQrProducto(1)

        assertEquals(1, leerIdQrProducto(texto))
    }

    @Test
    fun qrConTipoIncorrectoEsRechazado() {
        val texto = """{"tipo":"pedido","id":1}"""

        assertNull(leerIdQrProducto(texto))
    }

    @Test
    fun qrConIdInvalidoEsRechazado() {
        val texto = """{"tipo":"producto","id":0}"""

        assertNull(leerIdQrProducto(texto))
    }

    @Test
    fun qrConTextoDesconocidoEsRechazado() {
        assertNull(leerIdQrProducto("OYL001"))
    }

}
