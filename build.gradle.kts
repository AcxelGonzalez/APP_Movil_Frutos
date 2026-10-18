/**
 * Configuracion general del proyecto OYL Frutos Secos.
 *
 * Se utiliza una version estable del Android Gradle Plugin
 * compatible con Gradle 8.2 y JDK 17.
 */

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false

    // Procesamiento de anotaciones para Room.
    id("org.jetbrains.kotlin.kapt") version "1.8.10" apply false
}

