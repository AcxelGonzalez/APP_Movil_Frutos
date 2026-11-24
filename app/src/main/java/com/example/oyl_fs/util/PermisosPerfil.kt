package com.example.oyl_fs.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class PermisoCamaraPerfil { NO_SOLICITADO, PERMITIDO, NO_PERMITIDO }
enum class PermisoUbicacionPerfil { NO_SOLICITADA, PRECISA, APROXIMADA, NO_PERMITIDA }

data class EstadoPermisosPerfil(val camara: PermisoCamaraPerfil, val ubicacion: PermisoUbicacionPerfil)

/** Consulta solo los permisos de OYL FS; no solicita acceso ni inspecciona otras aplicaciones. */
object PermisosPerfil {
    fun consultar(contexto: Context): EstadoPermisosPerfil {
        val gestor = contexto.packageManager
        val informacion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gestor.getPackageInfo(contexto.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            gestor.getPackageInfo(contexto.packageName, PackageManager.GET_PERMISSIONS)
        }
        val solicitados = informacion.requestedPermissions?.toSet().orEmpty()
        fun permitido(permiso: String) = permiso in solicitados &&
            ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

        val camara = when {
            Manifest.permission.CAMERA !in solicitados -> PermisoCamaraPerfil.NO_SOLICITADO
            permitido(Manifest.permission.CAMERA) -> PermisoCamaraPerfil.PERMITIDO
            else -> PermisoCamaraPerfil.NO_PERMITIDO
        }
        val ubicacion = when {
            Manifest.permission.ACCESS_FINE_LOCATION !in solicitados &&
                Manifest.permission.ACCESS_COARSE_LOCATION !in solicitados -> PermisoUbicacionPerfil.NO_SOLICITADA
            permitido(Manifest.permission.ACCESS_FINE_LOCATION) -> PermisoUbicacionPerfil.PRECISA
            permitido(Manifest.permission.ACCESS_COARSE_LOCATION) -> PermisoUbicacionPerfil.APROXIMADA
            else -> PermisoUbicacionPerfil.NO_PERMITIDA
        }
        return EstadoPermisosPerfil(camara, ubicacion)
    }
}
