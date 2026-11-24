package com.example.oyl_fs.util

import android.content.Context
import com.example.oyl_fs.repository.SesionRemotaRepository
import kotlinx.coroutines.runBlocking

/** Conserva todas las preferencias de sesión, incluida la sesión cifrada actual. */
class SesionDispositivoProtegida(private val contexto: Context) : AutoCloseable {
    private val preferencias = contexto.getSharedPreferences("sesion_remota_oyl", Context.MODE_PRIVATE)
    private val anterior = preferencias.all.toMap()
    init { runBlocking { SesionRemotaRepository.configurar(contexto); SesionRemotaRepository.cerrar() } }
    private fun restaurarPreferencias() {
        val editor = preferencias.edit().clear()
        anterior.forEach { (clave, valor) -> when (valor) {
            is String -> editor.putString(clave, valor)
            is Boolean -> editor.putBoolean(clave, valor)
            is Int -> editor.putInt(clave, valor)
            is Long -> editor.putLong(clave, valor)
            is Float -> editor.putFloat(clave, valor)
            is Set<*> -> editor.putStringSet(clave, valor.filterIsInstance<String>().toSet())
        } }
        check(editor.commit())
    }
    override fun close() {
        restaurarPreferencias()
        try { runBlocking { SesionRemotaRepository.configurar(contexto); SesionRemotaRepository.restaurar() } }
        finally { restaurarPreferencias() }
    }
}
