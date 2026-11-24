package com.example.oyl_fs.network
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Aquí vive la dirección del backend. Es el único lugar que se cambia
// si el servidor cambia de PC.
object RetrofitClient {

    // 10.0.2.2 = "la PC donde corre el emulador" (localhost de la PC)
    // Con celular real: la IP de la PC en el WiFi, ej. "http://192.168.1.15"
    private val HOST = com.example.oyl_fs.BuildConfig.OYL_BACKEND_HOST
    // Para USB: compilar con -PoylBackendHost=http://127.0.0.1 y usar adb reverse en los tres puertos.

    // No registrar contraseñas ni datos personales en Logcat.
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val http = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(AutorizacionInterceptor())
        .addInterceptor(logging)
        .build()

    // Cada microservicio tiene su puerto, por eso se crea un Retrofit por puerto
    private fun <T> crear(puerto: Int, api: Class<T>): T =
        Retrofit.Builder()
            .baseUrl("$HOST:$puerto/")
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(api)

    val productos: ProductosApi by lazy { crear(8081, ProductosApi::class.java) }
    val pedidos: PedidosApi by lazy { crear(8082, PedidosApi::class.java) }
    val usuarios: UsuariosApi by lazy { crear(8083, UsuariosApi::class.java) }
}
