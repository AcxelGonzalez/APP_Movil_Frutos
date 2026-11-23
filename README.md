# OYL Frutos Secos — Aplicación Móvil
> Asignatura **DSY1105 — Desarrollo de Aplicaciones Móviles** · Duoc UC · Docente: Cristian Gomez Vega

Aplicación Android nativa para la tienda **OYL Frutos Secos**.

| | |
|---|---|
| **Aplicación** | https://github.com/AcxelGonzalez/APP_Movil_Frutos |
| **Backend** | https://github.com/fabcornejom/OYL-Backend |
| **Tablero Trello** | https://trello.com/b/CZow21QV/proyecto-app |

---

## Índice

1. [El caso](#1-el-caso)
2. [Integrantes](#2-integrantes)
3. [Tecnologías](#3-tecnologías)
4. [Arquitectura](#4-arquitectura)
5. [Modelo de datos](#5-modelo-de-datos)
6. [Funcionalidades](#6-funcionalidades)
7. [Microservicios y endpoints](#7-microservicios-y-endpoints)
8. [Instalación paso a paso](#8-instalación-paso-a-paso)
9. [Problemas frecuentes](#9-problemas-frecuentes)
10. [Pruebas](#10-pruebas)
11. [Firma digital](#11-firma-digital)
12. [Generación del APK](#12-generación-del-apk)
13. [Estructura del repositorio](#13-estructura-del-repositorio)
14. [Gestión del proyecto](#14-gestión-del-proyecto)

---

## 1. El caso

**OYL Frutos Secos** es un emprendimiento comercial chileno dedicado a la venta minorista
de frutos secos, semillas, deshidratados y productos naturales. Opera con una tienda física
y, en paralelo, atiende pedidos por canales informales como WhatsApp Business y Mercado Libre.

### El problema

Los dos frentes de venta funcionan de forma independiente y sin un inventario común. Cuando
un cliente consulta por chat si hay stock de un producto, la administración debe ir a revisar
visualmente los estantes, confirmar el precio, apartar el producto y anotar los datos de la
entrega en papel.

Esto produce tres consecuencias:

- **Descuadres de stock** entre lo que figura disponible y lo que realmente hay en bodega.
- **Sobreventa** de productos agotados, porque dos canales pueden comprometer la misma unidad.
- **Tiempo administrativo perdido** en revisar existencias y responder mensajes uno a uno.

### La solución

Una aplicación móvil con dos perfiles conectados a un único backend, de modo que cualquier
movimiento quede reflejado de inmediato para todos.

El **cliente** consulta un catálogo estructurado con disponibilidad real, arma su pedido y
lo envía sin depender de un chat. El **administrador** gestiona el inventario desde el local,
recibe los pedidos entrantes y los procesa hasta la entrega. El stock se descuenta una sola
vez, en el servidor, en el momento en que el pedido se confirma.

### Restricciones del caso

- El entregable es un **MVP académico de simulación**, no un producto para publicar en tiendas.
- Se trabaja exclusivamente con **datos sintéticos**. No se usan registros reales, datos
  comerciales internos ni credenciales productivas, conforme a las leyes chilenas
  **N.º 21.719** y **N.º 19.628** sobre protección de datos personales.

---

## 2. Integrantes

| **Acxel González** |
| **Kerly Dugarte**  |
| **Marco Caarrasco** |
| **Fabián Cornejo** |

---

## 3. Tecnologías

### Aplicación móvil

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin 1.8.10 |
| Interfaz | Jetpack Compose · Material 3 |
| Arquitectura | MVVM — Pantalla → ViewModel → Repositorio → API |
| Cliente HTTP | Retrofit 2.9.0 + Gson |
| Persistencia local | Room (SQLite) |
| Mapas | osmdroid |
| Códigos QR | ZXing (generación) · Google Code Scanner (lectura) |
| SDK | minSdk 24 · targetSdk 34 · compileSdk 34 · JDK 17 |

### Backend

| Componente | Tecnología |
|---|---|
| Framework | Spring Boot 4.1.2 |
| Lenguaje | Java 25 |
| Base de datos | PostgreSQL 17 |
| Contenedores | Docker · Docker Compose |
| Seguridad | BCrypt para el cifrado de contraseñas |

---

## 4. Arquitectura

```
┌──────────────────────────────────────┐
│        Aplicación Android            │
│     Kotlin · Jetpack Compose         │
│                                      │
│   Pantallas → ViewModels →           │
│   Repositorios → Retrofit            │
│                                      │
│   Room (caché local)                 │
└───────────────────┬──────────────────┘
                    │  HTTP · JSON
        ┌───────────┼────────────┐
        │           │            │
┌───────┴──────┐ ┌──┴────────┐ ┌─┴───────────┐
│ ms-productos │ │ ms-pedidos│ │ ms-usuarios │
│    :8081     │ │   :8082   │ │    :8083    │
└───────┬──────┘ └──┬────────┘ └─┬───────────┘
        │           │            │
        │   oyl_    │   oyl_     │   oyl_
        │ productos │  pedidos   │  usuarios
        └───────────┼────────────┘
                    │
         ┌──────────┴───────────┐
         │    PostgreSQL 17     │
         │        :5433         │
         └──────────────────────┘
```

### Por qué tres microservicios

La división sigue los tres dominios del negocio que aparecen en el caso: el **catálogo**,
las **transacciones** y las **personas**. Cada uno cambia por motivos distintos y a ritmos
distintos, que es el criterio habitual para separar servicios.

Cada microservicio tiene **su propia base de datos** dentro del mismo motor PostgreSQL. Ninguno
puede leer las tablas de otro: si necesita un dato, lo pide por HTTP. Esto evita que un cambio
en el esquema de uno rompa a los demás.

### Comunicación entre servicios

`ms-pedidos` es el único que llama a otro microservicio. Al crear un pedido:

1. Consulta a `ms-productos` el **precio real** de cada artículo. El precio nunca viaja desde
   el teléfono, de modo que nadie puede alterarlo manipulando la petición.
2. Pide el **descuento de stock** de cada producto.
3. Si alguno falla —por ejemplo, porque otro cliente se llevó la última unidad—, **devuelve
   el stock** ya descontado antes de informar el error.

Ese tercer paso es una compensación: como no hay una transacción que abarque a los dos
servicios, el que inicia la operación se hace responsable de deshacerla si algo sale mal.

### Persistencia local

La aplicación mantiene una caché en Room. El servidor es siempre la fuente de verdad; Room
guarda una copia para que la app siga mostrando el catálogo en condiciones de señal baja,
tal como exige el caso.

---

## 5. Modelo de datos

### Producto

| Campo | Descripción |
|---|---|
| `id` | Identificador único |
| `nombre` | Nombre comercial |
| `categoria` | Frutos secos · Semillas · Deshidratados |
| `precio` | Precio unitario |
| `stock` | Existencias disponibles |
| `imagen` | Ruta o URI de la imagen referencial |

### Pedido

| Campo | Descripción |
|---|---|
| `id` | Identificador de la transacción |
| `fecha` | Fecha y hora del requerimiento |
| `detalles` | Artículos seleccionados con cantidad y precio unitario |
| `total` | Monto total consolidado |
| `estado` | Estado actual de la solicitud |

### Usuario

| Campo | Descripción |
|---|---|
| `id` | Identificador único |
| `nombre`, `alias`, `correo` | Datos de identificación |
| `telefono`, `direccion` | Datos de contacto |
| `contrasena` | Cifrada con BCrypt, nunca se devuelve en las respuestas |
| `rol` | `ADMIN` o `CLIENTE` |

---

## 6. Funcionalidades

### Perfil Cliente

- Registro de cuenta propia e inicio de sesión
- Catálogo organizado por categorías, con imagen, precio y disponibilidad real
- Detalle de cada producto
- Carrito de compras con control de cantidades
- Confirmación de compra y selección de punto de retiro en el mapa
- Historial de pedidos propios con su estado actual
- Edición de datos personales

### Perfil Administrador

- Inventario completo: alta, edición y eliminación de productos
- Aviso de productos con stock bajo
- Generación de códigos QR por producto
- Escáner QR para verificar los artículos al preparar un pedido
- Bandeja con los pedidos entrantes de todos los clientes
- Avance del pedido por sus estados hasta la entrega
- Notificaciones de pedidos nuevos

### Ciclo de vida de un pedido

```
PENDIENTE_VALIDACIÓN  →  CONFIRMADO  →  PREPARADO  →  ENTREGADO
```

El pedido nace en `PENDIENTE_VALIDACION` cuando el cliente lo envía. El administrador lo hace
avanzar **un paso a la vez**: el backend rechaza los saltos y los retrocesos.

Para pasar a `PREPARADO`, el administrador debe escanear los códigos QR de los productos y
verificar que las cantidades coincidan con las solicitadas.

Un pedido solo puede cancelarse mientras está en `PENDIENTE_VALIDACION`. Al cancelarlo, el
stock vuelve al inventario.

---

## 7. Microservicios y endpoints

### ms-productos — puerto 8081

Gestiona el catálogo y las existencias.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/productos` | Lista todo el catálogo |
| `GET` | `/api/productos/{id}` | Obtiene un producto |
| `GET` | `/api/productos/stock-bajo` | Productos bajo el umbral de stock |
| `POST` | `/api/productos` | Crea un producto |
| `PUT` | `/api/productos/{id}` | Edita un producto |
| `PATCH` | `/api/productos/{id}/stock` | Ajusta el stock |
| `DELETE` | `/api/productos/{id}` | Elimina un producto |

### ms-pedidos — puerto 8082

Gestiona las transacciones y coordina el descuento de stock.

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/pedidos` | Crea un pedido y descuenta el stock |
| `GET` | `/api/pedidos` | Lista todos los pedidos (administrador) |
| `GET` | `/api/pedidos/{id}` | Obtiene un pedido |
| `GET` | `/api/pedidos/usuario/{usuarioId}` | Pedidos de un cliente |
| `PATCH` | `/api/pedidos/{id}/estado` | Avanza al siguiente estado |
| `DELETE` | `/api/pedidos/{id}` | Cancela y devuelve el stock |

### ms-usuarios — puerto 8083

Gestiona cuentas, autenticación y perfiles.

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/usuarios/registro` | Crea una cuenta de cliente |
| `POST` | `/api/usuarios/login` | Valida credenciales y devuelve el usuario |
| `GET` | `/api/usuarios` | Lista los usuarios |
| `GET` | `/api/usuarios/{id}` | Obtiene un usuario |
| `PUT` | `/api/usuarios/{id}` | Edita los datos de contacto |
| `DELETE` | `/api/usuarios/{id}` | Elimina un usuario |

### Notas de seguridad

- Las contraseñas se cifran con **BCrypt** antes de guardarse. Al iniciar sesión no se comparan
  contraseñas: se vuelve a aplicar el algoritmo y se comparan los resultados.
- La contraseña **nunca** aparece en las respuestas del servidor.
- El registro público siempre crea usuarios con rol `CLIENTE`. Aunque una petición incluya
  `"rol": "ADMIN"`, el servidor la ignora. No es posible crear administradores desde la app.
- Ante credenciales incorrectas, el mensaje es el mismo tanto si falla el alias como la
  contraseña, para no revelar qué usuarios existen.

---

## 8. Instalación paso a paso

### 8.1 Requisitos previos

| Herramienta | Para qué |
|---|---|
| **Docker** y **Docker Compose** | Levantar el backend |
| **Android Studio** (Ladybug o superior) | Compilar y ejecutar la app |
| **Git** | Clonar los repositorios |

No hace falta instalar Java, Maven ni PostgreSQL: todo eso vive dentro de los contenedores.

---

### 8.2 Instalar Docker

#### Windows

Docker en Windows funciona sobre **WSL 2**, un subsistema que ejecuta Linux dentro de Windows.

**Requisitos:** Windows 10 versión 22H2 o Windows 11 versión 23H2 en adelante (ediciones Pro,
Enterprise o Education), 8 GB de RAM y virtualización habilitada en la BIOS.

1. **Instalar WSL 2.** Abre PowerShell **como administrador** y ejecuta:

   ```powershell
   wsl --install
   wsl --update
   ```

   Reinicia el computador si te lo pide.
   → [Guía oficial de instalación de WSL](https://learn.microsoft.com/windows/wsl/install)

2. **Descargar Docker Desktop** desde la página oficial:
   → [Install Docker Desktop on Windows](https://docs.docker.com/desktop/setup/install/windows-install/)

3. **Ejecutar el instalador** `Docker Desktop Installer.exe`. En la pantalla de configuración,
   marca la opción **Use WSL 2 instead of Hyper-V**.

4. **Abrir Docker Desktop** y aceptar el acuerdo de uso. El ícono de la ballena en la barra de
   tareas debe quedar fijo, sin animación: eso indica que el motor está en marcha.

5. **Verificar** desde PowerShell o CMD:

   ```powershell
   docker --version
   docker compose version
   ```

#### Linux (Ubuntu, Pop!_OS, Linux Mint y derivados)

En Linux se instala el motor directamente, sin Docker Desktop.

1. **Agregar el repositorio oficial de Docker:**

   ```bash
   sudo apt update
   sudo apt install ca-certificates curl
   sudo install -m 0755 -d /etc/apt/keyrings
   sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
   sudo chmod a+r /etc/apt/keyrings/docker.asc

   sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
   Types: deb
   URIs: https://download.docker.com/linux/ubuntu
   Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
   Components: stable
   Architectures: $(dpkg --print-architecture)
   Signed-By: /etc/apt/keyrings/docker.asc
   EOF

   sudo apt update
   ```

2. **Instalar Docker y el plugin de Compose:**

   ```bash
   sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
   ```

3. **Poder usar Docker sin `sudo`** (opcional pero recomendable):

   ```bash
   sudo usermod -aG docker $USER
   ```

   Cierra la sesión y vuelve a entrar para que el cambio surta efecto.

4. **Verificar:**

   ```bash
   docker --version
   docker compose version
   docker run hello-world
   ```

   → [Guía oficial para Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
   → [Pasos posteriores a la instalación](https://docs.docker.com/engine/install/linux-postinstall/)

> **Importante:** si tenías instalado el paquete `docker.io` de los repositorios de Ubuntu,
> desinstálalo antes. Es una versión antigua y entra en conflicto con la oficial.

#### macOS

Descarga Docker Desktop eligiendo la versión que corresponda a tu procesador, Apple Silicon
o Intel:
→ [Install Docker Desktop on Mac](https://docs.docker.com/desktop/setup/install/mac-install/)

---

### 8.3 Levantar el backend

```bash
git clone https://github.com/fabcornejom/OYL-Backend.git
cd OYL-Backend
docker compose up -d --build
```

La primera vez tarda **varios minutos**: Docker descarga las imágenes base de Java y PostgreSQL,
y compila los tres microservicios con Maven. Las siguientes veces es cuestión de segundos.

El comando levanta **cuatro contenedores**:

| Contenedor | Puerto | Función |
|---|---|---|
| `postgres` | 5433 | Base de datos compartida |
| `ms-productos` | 8081 | Catálogo e inventario |
| `ms-pedidos` | 8082 | Pedidos y estados |
| `ms-usuarios` | 8083 | Cuentas y autenticación |

> El puerto **5433** se usa en lugar del 5432 habitual de PostgreSQL para no chocar con una
> instalación local que ya tengas en el computador.

**Comprobar que los cuatro quedaron arriba:**

```bash
docker compose ps
```

Los cuatro deben aparecer con estado `Up`.

**Comprobar que responden,** desde el navegador o con `curl`:

- http://localhost:8081/api/productos → debe devolver el catálogo inicial
- http://localhost:8083/api/usuarios → debe devolver las dos cuentas de prueba

Al arrancar por primera vez, el backend crea solo el catálogo y las cuentas. No hay que
cargar nada a mano.

**Para detener los contenedores:**

```bash
docker compose down
```

**Para detenerlos y además borrar la base de datos** (vuelve todo al estado inicial):

```bash
docker compose down -v
```

---

### 8.4 Ejecutar la aplicación

```bash
git clone https://github.com/AcxelGonzalez/APP_Movil_Frutos.git
```

1. Abre la carpeta en **Android Studio**.
2. Espera a que termine la sincronización de Gradle. La primera vez descarga las dependencias
   y puede tardar.
3. Selecciona un emulador con **API 24 o superior**.
4. Pulsa **Run**.

#### Dirección del backend

La dirección del servidor está centralizada en un solo archivo,
`app/src/main/java/com/example/oyl_fs/network/RetrofitClient.kt`:

```kotlin
private const val HOST = "http://10.0.2.2"
```

`10.0.2.2` es la dirección con la que el **emulador** de Android se refiere al computador donde
está corriendo. No es `localhost`, porque para el emulador `localhost` es él mismo.

Si quieres probar en un **teléfono físico** conectado a la misma red WiFi, reemplázala por la
IP local del computador:

```kotlin
private const val HOST = "http://192.168.1.8"   // ejemplo
```

Para averiguar esa IP: `hostname -I` en Linux, `ipconfig` en Windows.

---

### 8.5 Cuentas de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin` | Administrador |
| `cliente` | `cliente` | Cliente |

Ambas se crean automáticamente al levantar `ms-usuarios`, y se vuelven a crear si borras la
base de datos. También puedes crear cuentas nuevas desde el botón **Registrarse** de la
pantalla de inicio de sesión: esas se guardan en el backend igual que las anteriores.

---

### 8.6 Recorrido de demostración

Para ver el sistema completo funcionando:

1. Entra como **cliente**, agrega productos al carrito y confirma la compra.
2. Abre http://localhost:8081/api/productos en el navegador: el stock bajó.
3. Abre http://localhost:8082/api/pedidos: el pedido está ahí, en `PENDIENTE_VALIDACION`.
4. Cierra sesión y entra como **admin**: el pedido aparece en la bandeja.
5. Hazlo avanzar de estado y comprueba en el navegador que el cambio quedó registrado.

---

## 9. Problemas frecuentes

<details>
<summary><strong>«port is already allocated» al levantar los contenedores</strong></summary>

El puerto 5433 está ocupado por otro contenedor, normalmente una copia anterior del proyecto.
Revisa qué hay corriendo:

```bash
docker ps --format "table {{.Names}}\t{{.Ports}}"
```

Apaga el contenedor que ocupa el puerto desde su propia carpeta con `docker compose down`, y
vuelve a intentarlo.
</details>

<details>
<summary><strong>«no configuration file provided: not found»</strong></summary>

Estás ejecutando `docker compose` desde una carpeta que no contiene el archivo
`docker-compose.yml`. Sitúate en la raíz del repositorio del backend.
</details>

<details>
<summary><strong>La app muestra «No pudimos conectar con el servidor»</strong></summary>

El backend no está corriendo o la app apunta a una dirección equivocada.

1. Verifica con `docker compose ps` que los cuatro contenedores estén `Up`.
2. Abre http://localhost:8081/api/productos en el navegador del computador.
3. Si usas el emulador, confirma que `HOST` sea `http://10.0.2.2`.
4. Si usas un teléfono físico, confirma que `HOST` sea la IP del computador y que ambos estén
   en la misma red WiFi.
</details>

<details>
<summary><strong>El catálogo aparece vacío</strong></summary>

Revisa los registros del microservicio de productos:

```bash
docker compose logs ms-productos
```

Si no aparece el mensaje de carga inicial, el contenedor no está ejecutando el código más
reciente. Reconstruye con `docker compose up -d --build ms-productos`.
</details>

<details>
<summary><strong>Gradle falla con «Unknown Kotlin JVM target: 21»</strong></summary>

Estás ejecutando Gradle desde una terminal que usa el JDK del sistema. Ejecuta las tareas
**desde Android Studio**, que usa su propio JDK 17, o indica la ruta explícitamente:

```bash
./gradlew test -Dorg.gradle.java.home=$HOME/android-studio/jbr
```
</details>

---

## 10. Pruebas

### Pruebas unitarias (JVM)

Ubicación: `app/src/test/`

No requieren emulador ni servidor. Cubren la lógica de negocio: repositorios remotos, mapeo de
datos entre el backend y los modelos de la app, interpretación de errores de la API,
validaciones del carrito y ViewModels.

Las conexiones reales se reemplazan por dobles de prueba con **MockK**, de modo que las pruebas
pasan aunque los contenedores estén apagados y siempre dan el mismo resultado.

Para ejecutarlas: clic derecho sobre la carpeta `test` → **Run 'Tests in ...'**

### Pruebas instrumentadas

Ubicación: `app/src/androidTest/`

Validan la base de datos local Room y sus migraciones entre versiones. Requieren un emulador
en marcha, porque dependen del motor SQLite de Android.

---

## 11. Firma digital

La aplicación se firma con un almacén de claves propio, ubicado en `keystore/oyl.jks`.

### Para qué sirve

Una aplicación sin firmar no puede instalarse fuera del modo desarrollador. La firma cumple
dos funciones:

- **Identifica el origen.** Android verifica la firma al instalar. Si alguien modifica el APK
  después de firmarlo, la verificación falla y el sistema lo rechaza.
- **Habilita las actualizaciones.** Solo se puede instalar una versión nueva sobre otra si
  ambas están firmadas con la misma clave. Si se pierde la clave, la aplicación pasa a ser,
  para el sistema, una aplicación distinta.

### Pasos seguidos

1. Menú **Build → Generate Signed App Bundle / APK**, opción **APK**.
2. Botón **Create new...** para generar el almacén de claves.
3. Se completaron los datos del certificado: ruta del archivo, contraseña, alias `oyl` y una
   validez de 25 años.
4. Se seleccionó la variante **release** para generar el APK firmado.

### Evidencia

| | |
|---|---|
| ![Firma 1](entrega/evidencias-firma/Evidencia%201.png) | ![Firma 2](entrega/evidencias-firma/Evidencia%202.png) |
| ![Firma 3](entrega/evidencias-firma/Evidencia%203.png) | ![Firma 4](entrega/evidencias-firma/Evidencia%204.png) |
| ![Firma 5](entrega/evidencias-firma/Evidencia%205.png) | ![Firma 6](entrega/evidencias-firma/Evidencia%206.png) |
| ![Firma 7](entrega/evidencias-firma/Evidencia%207.png) | ![Firma 8](entrega/evidencias-firma/Evidencia%208.png) |
| ![Firma 9](entrega/evidencias-firma/Evidencia%209.png) | |

### Configuración en Gradle

La firma quedó declarada en `app/build.gradle.kts`, de modo que cualquier compilación en modo
release la aplica automáticamente:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../keystore/oyl.jks")
        storePassword = "oyl2026"
        keyAlias = "oyl"
        keyPassword = "oyl2026"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = false
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

> **Nota sobre buenas prácticas.** Incluir el almacén de claves y sus contraseñas dentro del
> repositorio no es recomendable: quien tenga ese archivo puede firmar actualizaciones en
> nombre de la aplicación. Aquí se entregan porque la evaluación los solicita como evidencia.
> En un proyecto real el `.jks` se guarda fuera del repositorio y las contraseñas se leen desde
> variables de entorno o desde un archivo local excluido del control de versiones.

---

## 12. Generación del APK

Con la configuración de firma aplicada:

1. En el panel **Build Variants**, cambiar el módulo `app` a la variante **release**.
2. Menú **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. El archivo se genera en `app/build/outputs/apk/release/app-release.apk`.

Que el nombre sea `app-release.apk` y no `app-release-unsigned.apk` confirma que la firma se
aplicó correctamente.

El APK entregado se encuentra en **[`entrega/app-release.apk`](entrega/app-release.apk)**.

### Evidencia

| | |
|---|---|
| ![APK 1](entrega/evidencias-apk/Evidencia%201.png) | ![APK 2](entrega/evidencias-apk/Evidencia%202.png) |

---

## 13. Estructura del repositorio

```
APP_Movil_Frutos/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/oyl_fs/
│       │   │   ├── data/local/      Room: entidades, DAOs y migraciones
│       │   │   ├── model/           Modelos de dominio
│       │   │   ├── network/         Retrofit: interfaces, DTOs y cliente
│       │   │   ├── repository/      Acceso a datos, remoto y local
│       │   │   ├── screens/         Pantallas en Jetpack Compose
│       │   │   ├── ui/              Tema, colores y componentes
│       │   │   ├── util/            Utilidades (QR y otros)
│       │   │   ├── viewmodel/       Lógica de presentación
│       │   │   └── MainActivity.kt  Navegación entre pantallas
│       │   └── res/                 Imágenes, íconos y estilos
│       ├── test/                    Pruebas unitarias (JVM)
│       └── androidTest/             Pruebas instrumentadas (emulador)
├── entrega/
│   ├── app-release.apk              APK firmado
│   ├── evidencias-apk/              Capturas de la generación del APK
│   └── evidencias-firma/            Capturas de la generación de la firma
├── keystore/
│   └── oyl.jks                      Almacén de claves de firma
└── build.gradle.kts
```

### Paleta de colores

| Color | Código | Uso |
|---|---|---|
| Fondo | `#FFF5E7` | Fondo general de las pantallas |
| Principal | `#92623D` | Botones y elementos destacados |
| Secundario | `#4B2E15` | Textos y barras superiores |
| Blanco | `#FFFFFF` | Tarjetas y contenedores |

---

## 14. Gestión del proyecto

### Ramas

| Rama | Propósito |
|---|---|
| `main` | Versión estable |
| `desarrollo` | Integración del trabajo del equipo |
| `desarrollo-<nombre>` | Rama de trabajo individual de cada integrante |

Cada integrante trabaja en su propia rama y la integra a `desarrollo` cuando sus cambios están
probados. Así se evitan conflictos entre pantallas que se desarrollan en paralelo.

### Tablero Trello

El avance se gestionó en Trello, con columnas por etapa, tarjetas por funcionalidad y responsabilidades.

*(Agregar aquí el enlace al tablero y una captura.)*

---

<p align="center">
  Duoc UC · Escuela de Informática y Telecomunicaciones<br>
  DSY1105 — Desarrollo de Aplicaciones Móviles - Docente: Cristian Gomez Vega
</p>
