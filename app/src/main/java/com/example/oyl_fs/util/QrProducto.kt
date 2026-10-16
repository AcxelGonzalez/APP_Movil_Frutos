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

fun generarQrProducto(id: Int): BitMatrix {
    return QRCodeWriter().encode(
        textoQrProducto(id), BarcodeFormat.QR_CODE, 512, 512,
        mapOf(EncodeHintType.MARGIN to 4)
    )
}
