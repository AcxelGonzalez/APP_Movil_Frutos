package com.example.oyl_fs.repository

import android.content.Context

// Guarda únicamente las lecturas en este teléfono, separadas por administrador.
class NotificacionAdminRepository(context: Context) {
    private val preferencias = context.applicationContext
        .getSharedPreferences("notificaciones_admin", Context.MODE_PRIVATE)

    fun leer(usuarioId: String): Set<String> =
        preferencias.getStringSet("leidas:$usuarioId", emptySet())?.toSet() ?: emptySet()

    fun guardar(usuarioId: String, ids: Set<String>) {
        preferencias.edit().putStringSet("leidas:$usuarioId", ids.toSet()).apply()
    }
}
