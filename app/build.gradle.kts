import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    // Permite generar las implementaciones de los DAOs.
    id("org.jetbrains.kotlin.kapt")
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.13"
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

// Incluye la lógica escrita a mano, también las corrutinas y clases sin pruebas.
// Solo quedan fuera la UI y las implementaciones generadas por Room.
val paquetesLogica = listOf("viewmodel", "repository", "model", "util", "network", "data/local")
val clasesLogica = files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include(paquetesLogica.map { "com/example/oyl_fs/$it/**" })
        exclude("**/*_Impl*")
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        include(paquetesLogica.map { "com/example/oyl_fs/$it/**" })
        exclude("**/*_Impl*")
    }
)

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Genera cobertura HTML/XML de la lógica con pruebas unitarias JVM, sin instrumentación."
    dependsOn("testDebugUnitTest")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(clasesLogica)
    sourceDirectories.setFrom(files("src/main/java"))
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
}

tasks.register<JacocoCoverageVerification>("jacocoDebugUnitTestCoverageVerification") {
    group = "verification"
    description = "Comprueba el mínimo de 80 % de líneas de lógica cubiertas por pruebas unitarias JVM."
    dependsOn("jacocoDebugUnitTestReport")
    executionData(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))
    classDirectories.setFrom(clasesLogica)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}


android {
    namespace = "com.example.oyl_fs"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.oyl_fs"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        val hostBackend = providers.gradleProperty("oylBackendHost").orElse("http://10.0.2.2").get()
        require(Regex("^https?://[a-zA-Z0-9.-]+$").matches(hostBackend)) {
            "oylBackendHost debe incluir protocolo y host, sin puerto ni ruta"
        }
        buildConfigField("String", "OYL_BACKEND_HOST", "\"$hostBackend\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        // Java y KAPT utilizarán la misma versión.
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        // Kotlin generará bytecode compatible con Java 17.
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true 
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // Permite utilizar ViewModel y viewModelScope.
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    // Permite usar viewModel() dentro de pantallas Compose.
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Generación local de códigos QR (autorizada).
    implementation("com.google.zxing:core:3.5.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Room / SQLite: persistencia local de datos.
    implementation("androidx.room:room-runtime:2.5.2")

    // Integración de Room con Kotlin Coroutines y Flow.
    implementation("androidx.room:room-ktx:2.5.2")

    // Genera automáticamente el código de entidades y DAOs.
    kapt("androidx.room:room-compiler:2.5.2")

    // Retrofit: conexión con el backend
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
