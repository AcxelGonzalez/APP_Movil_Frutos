package com.example.oyl_fs.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter

// JSON sencillo como el ejemplo del profesor, sin precio ni stock desactualizados.
fun textoQrProducto(id: Int): String {
    require(id > 0) { "El identificador del producto no es válido" }
    return """{"tipo":"producto","id":$id}"""
}

/**
 * Obtiene el identificador numérico de un QR
 * generado por OYL Frutos Secos.
 *
 * Formato esperado:
 * {"tipo":"producto","id":1}
 *
 * Devuelve null si el contenido es inválido.
 */
fun leerIdQrProducto(texto: String): Int? {

    // Validar el formato producido por textoQrProducto().
    val patron = Regex(
        """\{\s*"tipo"\s*:\s*"producto"\s*,\s*"id"\s*:\s*([1-9][0-9]*)\s*\}"""
    )

    val resultado = patron.matchEntire(texto.trim())
        ?: return null

    return resultado.groupValues[1]
        .toIntOrNull()
        ?.takeIf { it > 0 }
}

fun generarQrProducto(id: Int): BitMatrix {
    return QRCodeWriter().encode(
        textoQrProducto(id), BarcodeFormat.QR_CODE, 512, 512,
        mapOf(EncodeHintType.MARGIN to 4)
    )
}
