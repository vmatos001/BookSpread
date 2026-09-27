# 📋 Plan de Acción — CalibroTV v2.0
## Instrucciones para Agente de Código (Kimi / DeepSeek / Claude / GPT)

> **Repositorio destino:** `https://github.com/vmatos001/calibrotv`  
> **Workspace local:** `C:\Users\laura hart\Proyectos\Lector 3D\calibre_tv_app`  
> **JDK:** `C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7`  
> **Build command:** `$env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'; .\gradlew.bat assembleRelease --no-daemon`  
> **Paquete:** `com.example.calibretv` / App ID: `com.calibrotv.app`

---

## ⚠️ Reglas Obligatorias para el Agente

1. **NO eliminar** la animación 3D de paso de página (`curlAnim`, `CubicBezierEasing`, `TransformOrigin`). Es el diferencial principal de la app.
2. **NO cambiar** el sistema de temas (`ReadingTheme`), la paleta de colores ni la lógica de paginación por palabras (`EpubParser.paginate`).
3. **Compilar y verificar** que no hay errores Kotlin después de cada fase antes de pasar a la siguiente.
4. **Hacer git commit** al finalizar cada fase con el mensaje indicado.
5. **Preservar todos los comentarios** existentes en el código.
6. Después de completar todo, **notificar al revisor** con el resultado de la compilación final.

---

## FASE 0 — Preparación: Nuevas Dependencias en `gradle`
**Duración estimada: 15 minutos**

### 0.1 — Agregar versiones en `gradle/libs.versions.toml`

En la sección `[versions]` agregar:
```toml
room = "2.7.1"
androidxSecurity = "1.1.0-alpha06"
```

En la sección `[libraries]` agregar:
```toml
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "androidxSecurity" }
```

En la sección `[plugins]` agregar:
```toml
kotlin-ksp = { id = "com.google.devtools.ksp", version = "2.3.20-1.0.32" }
```

### 0.2 — Modificar `app/build.gradle.kts`

**Al inicio del archivo**, en el bloque `plugins { }`, agregar:
```kotlin
alias(libs.plugins.kotlin.ksp)
```

**En el bloque `dependencies { }`**, agregar al final:
```kotlin
// Room (SQLite local database)
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

// EncryptedSharedPreferences (seguridad)
implementation(libs.androidx.security.crypto)
```

**También cambiar** `versionCode = 3` a `versionCode = 4` y `versionName = "1.2"` a `versionName = "2.0"`.

### 0.3 — Modificar `build.gradle.kts` (nivel raíz)

Agregar al bloque `plugins { }` del archivo raíz:
```kotlin
alias(libs.plugins.kotlin.ksp) apply false
```

**Git commit:** `"build: add Room, EncryptedSharedPreferences, KSP dependencies for v2.0"`

---

## FASE 1 — Seguridad: EncryptedSharedPreferences + SSL Selectivo
**Duración estimada: 45 minutos**

### 1.1 — Crear `res/xml/network_security_config.xml`

**Ruta del archivo nuevo:** `app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Permite HTTP solo para redes locales (servidor Calibre en casa) -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">192.168.0.0</domain>
        <domain includeSubdomains="true">192.168.1.0</domain>
        <domain includeSubdomains="true">10.0.0.0</domain>
        <domain includeSubdomains="true">172.16.0.0</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
    <!-- Todo el resto: HTTPS únicamente -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system"/>
            <certificates src="user"/>
        </trust-anchors>
    </base-config>
</network-security-config>
```

### 1.2 — Modificar `AndroidManifest.xml`

Reemplazar `android:usesCleartextTraffic="true"` por `android:networkSecurityConfig="@xml/network_security_config"` en la etiqueta `<application>`.

### 1.3 — Modificar `SslHelper.kt`

**Ruta:** `app/src/main/java/com/example/calibretv/data/opds/SslHelper.kt`

Reemplazar el contenido completo con:

```kotlin
package com.example.calibretv.data.opds

import java.net.HttpURLConnection
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * SSL Helper SELECTIVO para CalibroTV.
 * - IPs privadas (192.168.x.x, 10.x.x.x, 172.16.x.x, localhost): bypass SSL
 *   para soportar certificados autofirmados en servidores domésticos.
 * - Todo lo demás: usa el TrustManager del sistema (seguro por defecto).
 */
object SslHelper {

    private val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    private val permissiveSslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }
    }

    private val permissiveHostnameVerifier = HostnameVerifier { _, _ -> true }

    private fun isPrivateHost(host: String): Boolean {
        return try {
            val addr = InetAddress.getByName(host)
            addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
        } catch (_: Exception) {
            // Si no se puede resolver, asume que podría ser local (DuckDNS en LAN, etc.)
            host.contains("local") || host.contains("home") || host.contains("duckdns")
        }
    }

    fun configureHttps(connection: HttpURLConnection) {
        if (connection is HttpsURLConnection) {
            try {
                val host = connection.url.host
                if (isPrivateHost(host)) {
                    // Solo bypass para servidores domésticos locales
                    connection.sslSocketFactory = permissiveSslContext.socketFactory
                    connection.hostnameVerifier = permissiveHostnameVerifier
                }
                // Para hosts externos, usa el TrustManager del sistema por defecto
            } catch (_: Exception) {}
        }
    }
}
```

### 1.4 — Modificar `PreferencesManager.kt`

**Ruta:** `app/src/main/java/com/example/calibretv/data/storage/PreferencesManager.kt`

Cambiar el constructor de la clase para usar `EncryptedSharedPreferences`:

```kotlin
// Reemplazar estas líneas al inicio de la clase:
// private val prefs: SharedPreferences = context.getSharedPreferences("calibre_tv_prefs", Context.MODE_PRIVATE)

// Por estas:
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "calibre_tv_prefs_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        // Fallback a SharedPreferences normales si el hardware no soporta TEE
        context.getSharedPreferences("calibre_tv_prefs", Context.MODE_PRIVATE)
    }
    // ... resto de la clase sin cambios
}
```

**Git commit:** `"security: EncryptedSharedPreferences + selective SSL bypass for private networks only"`

---

## FASE 2 — Corrección del Focus D-Pad en HUD del Lector
**Duración estimada: 20 minutos**

**Ruta:** `app/src/main/java/com/example/calibretv/ui/screens/ReaderScreen.kt`

**Problema:** Al presionar ↓ para abrir el HUD inferior, el foco no se transfiere automáticamente a sus botones.

En la función `onKeyEvent`, localizar el bloque:
```kotlin
Key.DirectionDown -> {
    showBottomHud = true
    showTopBar = false
    true
}
```

Reemplazarlo por:
```kotlin
Key.DirectionDown -> {
    showBottomHud = true
    showTopBar = false
    scope.launch {
        kotlinx.coroutines.delay(80L) // Espera una recomposición
        try { hudInitialFocusRequester.requestFocus() } catch (_: Exception) {}
    }
    true
}
```

Igualmente localizar:
```kotlin
Key.DirectionUp -> {
    showTopBar = true
    showBottomHud = false
    true
}
```

Reemplazarlo por:
```kotlin
Key.DirectionUp -> {
    showTopBar = true
    showBottomHud = false
    scope.launch {
        kotlinx.coroutines.delay(80L)
        try { topBarFocusRequester.requestFocus() } catch (_: Exception) {}
    }
    true
}
```

**Git commit:** `"fix: D-Pad HUD focus transfer with 80ms delay after recomposition"`

---

## FASE 3 — Migración de Catálogo a Room (SQLite)
**Duración estimada: 2 horas**

### 3.1 — Crear entidades Room

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/storage/AppDatabase.kt`

```kotlin
package com.example.calibretv.data.storage

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entidades ───────────────────────────────────────────────────────────────

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val epubUrl: String?,
    val summary: String,
    val category: String,
    val tags: String, // JSON array: ["tag1","tag2"]
    val progressPercent: Int = 0,
    val lastReadSpread: Int = 0
)

@Entity(tableName = "reading_progress", primaryKeys = ["profileId", "bookId"])
data class ReadingProgressEntity(
    val profileId: String,
    val bookId: String,
    val spreadIndex: Int = 0,
    val percent: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites", primaryKeys = ["profileId", "bookId"])
data class FavoriteEntity(
    val profileId: String,
    val bookId: String
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id IN (:ids)")
    suspend fun getBooksByIds(ids: List<String>): List<BookEntity>

    @Upsert
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int
}

@Dao
interface ProgressDao {
    @Query("SELECT spreadIndex FROM reading_progress WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun getSpreadIndex(profileId: String, bookId: String): Int?

    @Query("SELECT percent FROM reading_progress WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun getPercent(profileId: String, bookId: String): Int?

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT bookId FROM favorites WHERE profileId=:profileId")
    suspend fun getFavoriteIds(profileId: String): List<String>

    @Query("SELECT COUNT(*)>0 FROM favorites WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun isFavorite(profileId: String, bookId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun remove(profileId: String, bookId: String)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [BookEntity::class, ReadingProgressEntity::class, FavoriteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun progressDao(): ProgressDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calibrotv.db"
                ).build().also { INSTANCE = it }
            }
    }
}
```

### 3.2 — Actualizar `BookRepository.kt`

En el constructor de `BookRepository`, agregar el acceso a la base de datos:

```kotlin
class BookRepository(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val progressDao = db.progressDao()
    private val favoriteDao = db.favoriteDao()
    // ... resto igual
}
```

Reemplazar los métodos de catálogo, progreso y favoritos para usar los DAOs Room en lugar de `prefs.getCachedBooks()` / `prefs.saveCachedBooks()` / `prefs.getBookProgress()` / `prefs.saveBookProgress()` / `prefs.getFavoriteBookIds()` / `prefs.toggleFavorite()`.

> **Nota para el agente:** Mantener los métodos en `PreferencesManager` intactos durante la migración para no romper el código. Solo cambiar las implementaciones en `BookRepository` para delegar a los DAOs. Una vez que todo compila, los métodos obsoletos de `PreferencesManager` pueden marcarse con `@Deprecated`.

**Git commit:** `"feat: migrate book catalog and reading progress to Room SQLite database"`

---

## FASE 4 — Idea #9: Sleep Timer (Temporizador de Apagado)
**Duración estimada: 1 hora**
*Esta fase va primero porque no requiere dependencias nuevas y valida el flujo básico.*

### 4.1 — Agregar `sleepTimerMinutes` a `ReadingSettings`

En `Models.kt`, agregar el campo al data class `ReadingSettings`:
```kotlin
val sleepTimerMinutes: Int = 0  // 0 = desactivado, 15, 30, 45, 60
```

En `PreferencesManager.kt`, en `getReadingSettings()` agregar:
```kotlin
sleepTimerMinutes = prefs.getInt("sleep_timer", 0)
```
En `saveReadingSettings()` agregar:
```kotlin
.putInt("sleep_timer", settings.sleepTimerMinutes)
```

### 4.2 — Agregar lógica de sleep timer en `ReaderScreen.kt`

Dentro de `ReaderScreen()`, agregar estas variables de estado:
```kotlin
var sleepTimerSecondsLeft by remember { mutableIntStateOf(0) }
var isSleepTimerActive by remember { mutableStateOf(false) }
var screenAlpha by remember { mutableStateOf(1f) }
```

Agregar este `LaunchedEffect` **después** del bloque de carga del libro:
```kotlin
// Sleep Timer con atenuación progresiva en los últimos 120 segundos
LaunchedEffect(isSleepTimerActive, settings.sleepTimerMinutes) {
    if (!isSleepTimerActive || settings.sleepTimerMinutes == 0) {
        sleepTimerSecondsLeft = 0
        screenAlpha = 1f
        return@LaunchedEffect
    }
    sleepTimerSecondsLeft = settings.sleepTimerMinutes * 60
    while (sleepTimerSecondsLeft > 0) {
        kotlinx.coroutines.delay(1000L)
        sleepTimerSecondsLeft--
        // Atenuación progresiva en los últimos 120 segundos
        screenAlpha = if (sleepTimerSecondsLeft <= 120) {
            (sleepTimerSecondsLeft / 120f).coerceIn(0.05f, 1f)
        } else {
            1f
        }
    }
    // Timer expirado: lanzar intent de apagado de pantalla
    screenAlpha = 0f
    kotlinx.coroutines.delay(800L)
    onBack() // Cierra el lector al finalizar
}
```

Agregar `Modifier.graphicsLayer { alpha = screenAlpha }` al `Box` principal del lector.

### 4.3 — Agregar botón Sleep Timer en el HUD del lector

En la sección del HUD inferior de `ReaderScreen.kt`, localizar el `Row` de botones del HUD y agregar:
```kotlin
// Botón Sleep Timer
val timerLabel = when {
    !isSleepTimerActive -> "⏱ Sleep"
    sleepTimerSecondsLeft > 60 -> "⏱ ${sleepTimerSecondsLeft / 60}m"
    else -> "⏱ ${sleepTimerSecondsLeft}s"
}
StitchHudButton(
    title = timerLabel,
    icon = Icons.Filled.Timer,
    isPrimary = isSleepTimerActive,
    onClick = {
        // Ciclo: Off → 15min → 30min → 45min → 60min → Off
        val nextMinutes = when (settings.sleepTimerMinutes) {
            0 -> 15; 15 -> 30; 30 -> 45; 45 -> 60; else -> 0
        }
        settings = settings.copy(sleepTimerMinutes = nextMinutes)
        repository.saveReadingSettings(settings)
        isSleepTimerActive = nextMinutes > 0
    }
)
```

> **Importante:** Agregar `import androidx.compose.material.icons.filled.Timer` a los imports de `ReaderScreen.kt`.

**Git commit:** `"feat: sleep timer with progressive screen dimming (15/30/45/60 min cycles)"`

---

## FASE 5 — Idea #8: Modo Proyector / Cine Oscuro y Filtro de Luz Cálida
**Duración estimada: 1.5 horas**

### 5.1 — Agregar nuevos temas a `ReadingTheme`

En `Models.kt`, agregar dos valores al enum `ReadingTheme`:
```kotlin
@Serializable
enum class ReadingTheme {
    PERGAMINO,    // #F4F1EA fondo papel clásico (Apple Books)
    OLED_PURE,    // #000000 negro absoluto, texto nítido
    SEPIA_CINE,   // #26201A fondo cálido cinematográfico
    NIGHT_AMBER,  // #0D0D0D fondo noche, texto ámbar
    PROYECTOR_BLANCO,  // ← NUEVO: Fondo blanco puro para proyectores con fondo claro
    CINE_OSCURO        // ← NUEVO: Negro absoluto + texto ámbar muy tenue para sala oscura
}
```

### 5.2 — Agregar `readerBrightness` a `ReadingSettings`

En `Models.kt`, agregar a `ReadingSettings`:
```kotlin
val readerBrightness: Float = 1.0f  // 0.1f a 1.0f, control de brillo interno
```

En `PreferencesManager.kt` agregar lectura/escritura del campo (usar `getFloat` / `putFloat`).

### 5.3 — Actualizar paleta de colores en `ReaderScreen.kt`

Localizar el bloque `when (settings.theme)` que define `(pageBg, pageText, accentColor)` y agregar los dos nuevos casos:
```kotlin
ReadingTheme.PROYECTOR_BLANCO -> Triple(
    Color(0xFFFFFFFF),          // Blanco puro
    Color(0xFF1A1A1A),          // Negro suave
    Color(0xFF0066CC)           // Azul accent
)
ReadingTheme.CINE_OSCURO -> Triple(
    Color(0xFF000000),          // Negro absoluto OLED
    Color(0xFF8B7355),          // Ámbar muy tenue (0 luz azul)
    Color(0xFF6B4F2A)           // Acento madera oscura
)
```

### 5.4 — Aplicar `readerBrightness` al Box principal

Al `Box` principal del lector, en el `Modifier.graphicsLayer { }` existente, agregar:
```kotlin
alpha = screenAlpha * settings.readerBrightness
```

### 5.5 — Agregar control de brillo en el HUD

En el HUD inferior, agregar botón de ajuste de brillo que cicle entre: 100% → 70% → 50% → 30% → 100%.

### 5.6 — Agregar los 2 nuevos temas en `SettingsScreen.kt`

Localizar la sección de selección de temas en `SettingsScreen.kt` y agregar las opciones para `PROYECTOR_BLANCO` y `CINE_OSCURO` con sus nombres visuales y colores representativos.

**Git commit:** `"feat: projector mode, dark cinema theme, and reader brightness control"`

---

## FASE 6 — Idea #7: Catálogo de Fuentes Literarias + Ajuste de Profundidad 3D
**Duración estimada: 2 horas**

### 6.1 — Agregar `ReadingFont` a `Models.kt`

```kotlin
@Serializable
enum class ReadingFont {
    SERIF_SYSTEM,     // Fuente serif del sistema (actual)
    OPEN_DYSLEXIC,    // Para personas con dislexia (requiere asset)
    GEORGIA_LIKE,     // Georgia/serif elegante
    MONOSPACE,        // Ancho fijo para código/poesía
}
```

Y agregar a `ReadingSettings`:
```kotlin
val readingFont: ReadingFont = ReadingFont.SERIF_SYSTEM,
val spineDepth3D: Float = 0.5f  // 0.0 = plano, 1.0 = tomo grueso
```

### 6.2 — Agregar fuente OpenDyslexic

Descargar el archivo `OpenDyslexic-Regular.otf` de https://opendyslexic.org (es de código abierto / SIL Open Font License).

Colocarlo en: `app/src/main/assets/fonts/OpenDyslexic-Regular.otf`

### 6.3 — Crear `FontProvider.kt`

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/theme/FontProvider.kt`

```kotlin
package com.example.calibretv.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.calibretv.R
import com.example.calibretv.data.model.ReadingFont

object FontProvider {
    val openDyslexicFamily = FontFamily(
        Font(R.font.open_dyslexic_regular, FontWeight.Normal)
    )

    fun getFontFamily(readingFont: ReadingFont): FontFamily = when (readingFont) {
        ReadingFont.SERIF_SYSTEM -> FontFamily.Serif
        ReadingFont.OPEN_DYSLEXIC -> openDyslexicFamily
        ReadingFont.GEORGIA_LIKE -> FontFamily.Serif  // Mejora futura con asset
        ReadingFont.MONOSPACE -> FontFamily.Monospace
    }
}
```

> Copiar el archivo .otf también a `app/src/main/res/font/open_dyslexic_regular.otf`

### 6.4 — Aplicar fuente seleccionada en `PageColumn` de `ReaderScreen.kt`

Localizar la función `PageColumn` y modificar el `fontFamily` de todos los `Text()`:
```kotlin
// Cambiar: fontFamily = FontFamily.Serif
// Por:
fontFamily = FontProvider.getFontFamily(settings.readingFont)
```

### 6.5 — Ajuste de profundidad 3D del lomo

En `ReaderScreen.kt`, localizar el `graphicsLayer` de la página derecha durante el `curlAnim` y ajustar la sombra cilíndrica con base en `settings.spineDepth3D`:

```kotlin
// La sombra existente del lomo central depende de spineDepth3D
val shadowIntensity = settings.spineDepth3D * 0.7f
// Aplicar como alpha de la sombra del Box del lomo
```

### 6.6 — Agregar selección de fuentes y profundidad en `SettingsScreen.kt`

Agregar una sección nueva "Tipografía" con:
- Selector de fuente: SERIF_SYSTEM, OPEN_DYSLEXIC, MONOSPACE.
- Slider de profundidad 3D del lomo: 0% = libro delgado, 100% = tomo grueso con sombra profunda.

**Git commit:** `"feat: reading font catalog (OpenDyslexic, Serif, Monospace) + 3D spine depth control"`

---

## FASE 7 — Idea #6: Soporte CBZ/CBR (Cómics y Manga)
**Duración estimada: 3 horas**

### 7.1 — Crear `ComicParser.kt`

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/comic/ComicParser.kt`

```kotlin
package com.example.calibretv.data.comic

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

/**
 * Parser para cómics en formato CBZ (ZIP de imágenes) y CBR (RAR de imágenes).
 * CBZ se procesa nativamente con java.util.zip.
 * CBR requiere una librería externa — de momento se informa al usuario.
 */
object ComicParser {

    data class ComicPage(
        val pageFile: File,
        val pageNumber: Int,
        val isDoublePage: Boolean = false
    )

    data class ParsedComic(
        val title: String,
        val pages: List<ComicPage>,
        val isRightToLeft: Boolean = false  // Manga = true
    )

    /**
     * Detecta si el archivo es un cómic soportado.
     */
    fun isComicFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext == "cbz" || ext == "cbr"
    }

    /**
     * Extrae todas las páginas de un CBZ al directorio de caché.
     * Ordena las páginas por nombre de archivo (natural sort).
     */
    suspend fun parseCbz(cbzFile: File, cacheDir: File): ParsedComic {
        val pagesDir = File(cacheDir, "${cbzFile.nameWithoutExtension}_pages")
        if (!pagesDir.exists()) pagesDir.mkdirs()

        val pages = mutableListOf<ComicPage>()
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

        try {
            ZipFile(cbzFile).use { zip ->
                val imageEntries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { it.name.substringAfterLast('.').lowercase() in imageExtensions }
                    .sortedWith(Comparator { a, b ->
                        // Natural sort: imagen01 < imagen02 < imagen10
                        naturalCompare(a.name, b.name)
                    })
                    .toList()

                imageEntries.forEachIndexed { index, entry ->
                    val cleanName = "${index.toString().padStart(4, '0')}_${entry.name.replace('/', '_')}"
                    val destFile = File(pagesDir, cleanName)
                    if (!destFile.exists() || destFile.length() == 0L) {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    if (destFile.exists()) {
                        pages.add(ComicPage(destFile, index + 1))
                    }
                }
            }
        } catch (_: Exception) {}

        // Detectar Manga por nombre de archivo (convención común)
        val isRtl = cbzFile.name.lowercase().let {
            it.contains("manga") || it.contains("manga")
        }

        return ParsedComic(cbzFile.nameWithoutExtension, pages, isRtl)
    }

    /**
     * Comparación natural para nombres de archivo con números embebidos.
     * "Page10" > "Page2" correctamente.
     */
    private fun naturalCompare(a: String, b: String): Int {
        val ra = Regex("(\\D+)|(\\d+)")
        val ta = ra.findAll(a).toList()
        val tb = ra.findAll(b).toList()
        for (i in 0..minOf(ta.size, tb.size) - 1) {
            val partA = ta[i].value
            val partB = tb[i].value
            val cmp = if (partA[0].isDigit() && partB[0].isDigit()) {
                partA.toBigInteger().compareTo(partB.toBigInteger())
            } else {
                partA.compareTo(partB, ignoreCase = true)
            }
            if (cmp != 0) return cmp
        }
        return a.length - b.length
    }
}
```

### 7.2 — Crear `ComicReaderScreen.kt`

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/ui/screens/ComicReaderScreen.kt`

Implementar un `@Composable fun ComicReaderScreen(comic: ParsedComic, isRightToLeft: Boolean, onBack: () -> Unit)` que:
- Muestre las páginas del cómic en modo panorámico de doble página (igual que el lector de EPUB).
- Use `Image(bitmap = rememberLocalImage(page.pageFile), contentScale = ContentScale.Fit)` para cada página.
- Aplique la animación 3D de página existente (`curlAnim` + `CubicBezierEasing`). **Reutilizar exactamente el mismo código de animación de `ReaderScreen.kt`**.
- Para Manga (RTL): invertir la dirección de paso de página (← avanza en lugar de →).
- Controles D-Pad: ← / → para pasar páginas, ↓ para HUD con indicador de página actual.

### 7.3 — Modificar `BookRepository.kt`

Agregar método:
```kotlin
suspend fun loadComic(book: Book): ComicParser.ParsedComic = withContext(Dispatchers.IO) {
    val cacheFile = File(context.cacheDir, "book_${book.id.hashCode()}.cbz")
    if (!cacheFile.exists() || cacheFile.length() == 0L) {
        downloadEpub(book.epubUrl ?: return@withContext ComicParser.ParsedComic("", emptyList()), cacheFile)
    }
    ComicParser.parseCbz(cacheFile, context.cacheDir)
}
```

### 7.4 — Modificar `Navigation.kt` y `BookRepository`

- En el lanzador del lector, detectar la extensión del archivo descargado (`epubUrl`): si termina en `.cbz` o `.cbr`, navegar a `ComicReaderScreen` en lugar de `ReaderScreen`.
- El campo `epubUrl` en `Book` puede contener la URL del CBZ/CBR.

### 7.5 — Indicador visual en `LibraryScreen.kt`

En las tarjetas de libros, si el `epubUrl` termina en `.cbz` o `.cbr`, mostrar un badge "📚 Cómic" o "🗾 Manga" en la esquina de la portada.

**Git commit:** `"feat: CBZ comic and manga reader with RTL support and 3D page curl animation"`

---

## FASE 8 — Idea #2: Lectura en Voz Alta (TTS) con Resaltado Guiado
**Duración estimada: 2 horas**

### 8.1 — Agregar `ttsEnabled` a `ReadingSettings`

En `Models.kt`:
```kotlin
val ttsEnabled: Boolean = false,
val ttsSpeedRate: Float = 1.0f  // 0.5 = lento, 1.0 = normal, 1.5 = rápido
```

### 8.2 — Crear `TtsController.kt`

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/tts/TtsController.kt`

```kotlin
package com.example.calibretv.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsController(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    // Índice de la oración actualmente siendo leída
    private val _currentSentenceIndex = MutableStateFlow(-1)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private var sentences = listOf<String>()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Detectar idioma del sistema, con fallback a español
            val locale = if (tts.isLanguageAvailable(Locale("es", "ES")) >= TextToSpeech.LANG_AVAILABLE) {
                Locale("es", "ES")
            } else {
                Locale.getDefault()
            }
            tts.language = locale
            isReady = true
        }
    }

    /**
     * Divide el texto de la página en oraciones y las encola en el TTS.
     * Cada oración dispara una actualización del índice resaltado.
     */
    fun readPage(text: String, speedRate: Float = 1.0f) {
        if (!isReady) return
        stop()
        sentences = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        tts.setSpeechRate(speedRate)

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                _currentSentenceIndex.value = idx
                _isPlaying.value = true
            }
            override fun onDone(utteranceId: String) {
                val idx = utteranceId.removePrefix("sentence_").toIntOrNull() ?: -1
                if (idx >= sentences.size - 1) {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = -1
                }
            }
            override fun onError(utteranceId: String) { _isPlaying.value = false }
        })

        sentences.forEachIndexed { idx, sentence ->
            val params = android.os.Bundle()
            tts.speak(sentence, TextToSpeech.QUEUE_ADD, params, "sentence_$idx")
        }
    }

    fun stop() {
        tts.stop()
        _isPlaying.value = false
        _currentSentenceIndex.value = -1
    }

    fun destroy() {
        tts.stop()
        tts.shutdown()
    }
}
```

### 8.3 — Integrar TTS en `ReaderScreen.kt`

Dentro de `ReaderScreen()`, agregar:
```kotlin
val ttsController = remember { TtsController(LocalContext.current) }
val currentSentence by ttsController.currentSentenceIndex.collectAsState()
val isTtsPlaying by ttsController.isPlaying.collectAsState()

// Limpiar TTS al salir
DisposableEffect(Unit) {
    onDispose { ttsController.destroy() }
}
```

En la función `PageColumn`, cuando TTS esté activo, dividir el contenido de texto en oraciones y resaltar visualmente la oración activa con un `Background` ámbar/dorado semitransparente:

```kotlin
// Si TTS activo y es una oración siendo leída: background ámbar semitransparente
if (settings.ttsEnabled && sentenceIndex == currentSentence) {
    modifier = Modifier.background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
}
```

Agregar botón TTS en el HUD del lector:
```kotlin
StitchHudButton(
    title = if (isTtsPlaying) "⏸ Pausa" else "▶ Leer",
    icon = Icons.Filled.RecordVoiceOver,
    isPrimary = isTtsPlaying,
    onClick = {
        if (isTtsPlaying) {
            ttsController.stop()
        } else {
            val pageText = currentSpread?.leftPage?.paragraphs?.joinToString(" ") ?: ""
            ttsController.readPage(pageText, settings.ttsSpeedRate)
        }
    }
)
```

**Git commit:** `"feat: TTS read-aloud with sentence highlighting (karaoke-style reading)"`

---

## FASE 9 — Idea #1: Efectos de Sonido Foley (Paso de Página)
**Duración estimada: 1 hora**

### 9.1 — Agregar asset de sonido

Descargar o crear 3 variaciones de sonido de paso de página de papel (formato `.ogg`, ~100KB cada uno, libres de derechos):
- Búsqueda recomendada: "paper page turn sound effect free CC0" en freesound.org
- Guardar en: `app/src/main/res/raw/page_turn_1.ogg`, `page_turn_2.ogg`, `page_turn_3.ogg`

Si no se encuentran archivos de audio gratuitos adecuados, usar el `SoundPool` de Android con sonidos generados programáticamente (tono decreciente corto que simula el susurro del papel).

### 9.2 — Crear `SoundManager.kt`

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/sound/SoundManager.kt`

```kotlin
package com.example.calibretv.data.sound

import android.content.Context
import android.media.SoundPool
import com.example.calibretv.R

class SoundManager(context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .build()

    private val pageSounds = listOf(
        soundPool.load(context, R.raw.page_turn_1, 1),
        soundPool.load(context, R.raw.page_turn_2, 1),
        soundPool.load(context, R.raw.page_turn_3, 1)
    )
    private var lastSoundIdx = -1

    /** Reproduce un sonido de paso de página aleatorio (nunca el mismo dos veces seguidas) */
    fun playPageTurn(volume: Float = 0.6f) {
        var idx: Int
        do { idx = (0..2).random() } while (idx == lastSoundIdx)
        lastSoundIdx = idx
        soundPool.play(pageSounds[idx], volume, volume, 1, 0, 1.0f)
    }

    fun release() = soundPool.release()
}
```

### 9.3 — Integrar en `ReaderScreen.kt`

```kotlin
val soundManager = remember { SoundManager(LocalContext.current) }
DisposableEffect(Unit) { onDispose { soundManager.release() } }
```

En la función `turnPage()`, agregar la llamada al sonido:
```kotlin
fun turnPage(forward: Boolean) {
    if (isFlipping) return
    // ...lógica existente...
    soundManager.playPageTurn()  // ← Agregar esta línea
    scope.launch {
        // ...animación existente...
    }
}
```

### 9.4 — Agregar toggle de sonido en Settings

En `ReadingSettings`, agregar:
```kotlin
val pageSoundEnabled: Boolean = true
```

En `SettingsScreen.kt`, agregar toggle para activar/desactivar el sonido de página.

En `turnPage()`, condicionar: `if (settings.pageSoundEnabled) soundManager.playPageTurn()`.

**Git commit:** `"feat: Foley page-turn sound effects with randomized variation"`

---

## FASE 10 — Verificación Final y Git Push

### 10.1 — Compilación final

Ejecutar:
```powershell
$env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'
.\gradlew.bat assembleRelease --no-daemon
```

Verificar que la salida sea `BUILD SUCCESSFUL`.

### 10.2 — Verificar que no se rompió nada existente

Checklist manual antes del push:
- [ ] La animación 3D de paso de página funciona igual que antes.
- [ ] El lector EPUB carga y pagina correctamente.
- [ ] El HUD inferior responde al D-Pad correctamente (focus transferido).
- [ ] El menú de configuración muestra los nuevos temas y opciones de fuente.
- [ ] El sleep timer activa/desactiva correctamente desde el HUD.
- [ ] El botón TTS aparece en el HUD del lector.
- [ ] Un CBZ de prueba se abre en el ComicReaderScreen.

### 10.3 — Git commit y push final

```bash
git add .
git commit -m "feat: CalibroTV v2.0 — Sleep timer, TTS, CBZ comics, font catalog, cinema mode, Foley sounds + security fixes"
git push origin main
```

### 10.4 — Notificar al revisor

Al finalizar, el agente debe reportar:
1. Resultado de la compilación (BUILD SUCCESSFUL / errores).
2. Lista de fases completadas vs. pendientes.
3. Cualquier decisión de diseño que haya requerido desviarse de las instrucciones.
4. El hash del commit final de GitHub.

---

## 📁 Mapa de Archivos Nuevos/Modificados

### Archivos NUEVOS que se crearán:
```
app/src/main/java/com/example/calibretv/
  ├── data/
  │   ├── comic/ComicParser.kt              ← Fase 7
  │   ├── sound/SoundManager.kt             ← Fase 9
  │   ├── tts/TtsController.kt              ← Fase 8
  │   └── storage/AppDatabase.kt            ← Fase 3
  └── ui/screens/
      └── ComicReaderScreen.kt              ← Fase 7
  theme/FontProvider.kt                     ← Fase 6
app/src/main/res/
  ├── xml/network_security_config.xml       ← Fase 1
  ├── raw/page_turn_1.ogg                   ← Fase 9
  ├── raw/page_turn_2.ogg                   ← Fase 9
  ├── raw/page_turn_3.ogg                   ← Fase 9
  └── font/open_dyslexic_regular.otf        ← Fase 6
app/src/main/assets/fonts/
  └── OpenDyslexic-Regular.otf              ← Fase 6
gradle/
  └── libs.versions.toml                    ← Fase 0 (modificado)
```

### Archivos MODIFICADOS:
```
app/build.gradle.kts                        ← Fase 0
app/src/main/AndroidManifest.xml            ← Fase 1
data/model/Models.kt                        ← Fases 4,5,6,8,9
data/opds/SslHelper.kt                      ← Fase 1
data/storage/PreferencesManager.kt          ← Fases 1,4,5,6,8,9
data/BookRepository.kt                      ← Fases 3,7
ui/screens/ReaderScreen.kt                  ← Fases 2,4,5,6,8,9
ui/screens/SettingsScreen.kt                ← Fases 5,6,9
ui/screens/LibraryScreen.kt                 ← Fase 7 (badge cómic)
Navigation.kt                               ← Fase 7 (ruta ComicReader)
```

---

> [!IMPORTANT]
> **Para el Agente:** Leer primero los archivos existentes antes de hacer cualquier modificación. No reescribir archivos completos — hacer modificaciones quirúrgicas en las secciones indicadas para no perder código funcional existente. La animación 3D de página es el componente más crítico y no debe tocarse en ninguna circunstancia.
