package com.example.oyl_fs.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.oyl_fs.repository.NotificacionAdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificacionesAdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = NotificacionAdminRepository(application)
    private var usuarioId: String? = null
    private val _leidas = MutableStateFlow<Set<String>>(emptySet())
    val leidas = _leidas.asStateFlow()

    fun configurarUsuario(id: String?) {
        if (id == usuarioId) return
        usuarioId = id
        _leidas.value = if (id == null) emptySet() else repositorio.leer(id)
    }

    fun marcarLeidas(ids: Set<String>, idsVigentes: Set<String>) {
        val id = usuarioId ?: return
        // Descarta registros antiguos para no acumular lecturas indefinidamente.
        _leidas.value = (_leidas.value + ids).intersect(idsVigentes)
        repositorio.guardar(id, _leidas.value)
    }
}
