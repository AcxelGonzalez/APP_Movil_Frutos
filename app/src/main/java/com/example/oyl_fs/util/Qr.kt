
package com.example.oyl_fs.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/*
 * Función para abrir la cámara y escanear códigos QR.
 *
 * @param context Contexto necesario para abrir el escáner.
 * @param alLeer Se ejecuta con el texto del QR cuando se leyó correctamente.
 * @param alFallar Se ejecuta con un mensaje si se canceló o falló.
 */
fun escanearQr(
    context: Context,
    alLeer: (String) -> Unit,
    alFallar: (String) -> Unit
) {
    // Solo nos interesan los códigos QR, no los de barras.
    val opciones = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .enableAutoZoom()
        .build()

    // Abre el escáner de Google.
    GmsBarcodeScanning.getClient(context, opciones)
        .startScan()
        .addOnSuccessListener { codigo ->

            // Obtener el contenido del QR.
            val texto = codigo.rawValue?.trim()

            // Comprobar que el código tenga contenido.
            if (texto.isNullOrEmpty()) {
                alFallar("El código QR está vacío.")
            } else {
                alLeer(texto)
            }
        }
        .addOnCanceledListener {
            alFallar("Escaneo cancelado.")
        }
        .addOnFailureListener { error ->
            alFallar(
                "No se pudo escanear: ${error.message ?: "Error desconocido"}"
            )
        }
}
