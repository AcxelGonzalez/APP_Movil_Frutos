package com.example.oyl_fs.util

import java.util.Locale

object ValidacionContacto {
    fun normalizarRut(rut: String): String = rut.trim().replace(".", "").replace("-", "").replace(" ", "").uppercase(Locale.ROOT)

    fun telefonoValido(telefono: String): Boolean = telefono.isBlank() ||
        (telefono.count { it.isDigit() } in 8..11 && telefono.all { it.isDigit() || it in "+ -()" })

    fun rutValido(rut: String): Boolean {

        val limpio = normalizarRut(rut)

        if (!Regex("^\\d{7,8}[\\dK]$").matches(limpio)) {
            return false
        }

        val numero = limpio.dropLast(1)
        val verificador = limpio.last()

        var suma = 0
        var factor = 2

        for (digito in numero.reversed()) {
            suma += digito.digitToInt() * factor
            factor = if (factor == 7) 2 else factor + 1
        }

        val resto = 11 - (suma % 11)

        val esperado = when (resto) {
            11 -> '0'
            10 -> 'K'
            else -> resto.digitToChar()
        }

        return verificador == esperado
    }
}
