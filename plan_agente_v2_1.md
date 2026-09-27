# 📋 Plan de Acción — CalibroTV v2.1
## Instrucciones para Agente de Código

> **Repositorio destino:** `https://github.com/vmatos001/calibrotv`
> **Workspace local:** `C:\Users\laura hart\Proyectos\Lector 3D\calibre_tv_app`
> **JDK:** `C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7`
> **Build command:** `$env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'; .\gradlew.bat assembleRelease --no-daemon`

---

## ⚠️ Reglas Obligatorias para el Agente

1. **NO tocar** la animación 3D de paso de página (`curlAnim`, `CubicBezierEasing`, `TransformOrigin`).
2. **Leer cada archivo COMPLETO** antes de modificarlo.
3. **Compilar** (`assembleRelease`) antes de hacer cada commit de fase.
4. **Un git commit por fase** con el mensaje indicado.
5. **Preservar** todos los comentarios existentes en el código.

---

## FASE 1 — Corrección del Parser EPUB: Eliminar encabezados XML/HTML
**Duración estimada: 30 minutos**

**Problema:** El lector muestra el encabezado crudo del archivo XHTML antes del texto del libro:
```
<?xml version="1.0" encoding="utf-8" standalone="no"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "...">
```

**Archivo a modificar:** `app/src/main/java/com/example/calibretv/data/epub/EpubParser.kt`

### 1.1 — Agregar limpieza de encabezados en `parseHtmlToItems()`

Leer el archivo completo. Localizar el inicio de `parseHtmlToItems()` en la línea ~176. Justo **antes** del bloque de sanitización existente (que elimina `<script>` y `<style>`), agregar un paso previo de limpieza de encabezados:

```kotlin
private fun parseHtmlToItems(html: String, imageMap: Map<String, File>): List<RawContentItem> {
    val items = mutableListOf<RawContentItem>()

    // ── PASO 0: Eliminar encabezados XML/DOCTYPE/HTML antes de parsear ──────────
    val bodyContent = run {
        // 1. Quitar declaración XML: <?xml ... ?>
        var cleaned = html.replace(Regex("""<\?xml[^?]*\?>""", RegexOption.IGNORE_CASE), "")
        // 2. Quitar DOCTYPE: <!DOCTYPE ... >  (puede ser multilínea)
        cleaned = cleaned.replace(Regex("""(?s)<!DOCTYPE[^>]*>""", RegexOption.IGNORE_CASE), "")
        // 3. Extraer solo el contenido dentro de <body>...</body> si existe
        val bodyMatch = Regex("""(?is)<body[^>]*>(.*?)</body>""").find(cleaned)
        if (bodyMatch != null) {
            bodyMatch.groupValues[1]
        } else {
            // Si no hay <body>, quitar las etiquetas <html>, <head> y su contenido
            cleaned = cleaned.replace(Regex("""(?is)<head[^>]*>.*?</head>"""), "")
            cleaned = cleaned.replace(Regex("""(?i)</?html[^>]*>"""), "")
            cleaned
        }
    }
    // ── FIN PASO 0 ──────────────────────────────────────────────────────────────

    val sanitizedHtml = bodyContent   // ← usar bodyContent en lugar de html
        .replace(Regex("""(?s)<script.*?</script>"""), "")
        .replace(Regex("""(?s)<style.*?</style>"""), "")

    // ... resto del método sin cambios
```

> [!IMPORTANT]
> Asegurarse de que la variable `sanitizedHtml` siga existiendo con ese nombre porque es usada más abajo en el método. Cambiar solo la fuente de donde se genera (antes era directo de `html`, ahora es de `bodyContent`).

**Git commit:** `"fix: strip XML/DOCTYPE/HTML headers from EPUB chapters before rendering"`

---

## FASE 2 — Corrección del HUD inferior del Lector: Layout y botones faltantes
**Duración estimada: 45 minutos**

**Problema:** El HUD inferior está partido en dos filas separadas con espacio vacío entre ellas, y visualmente no forma una barra horizontal compacta. Además, los botones de Sleep Timer, TTS y Brillo sí existen en el código pero no son visibles desde el televisor debido a un overflow que los corta.

**Archivo a modificar:** `app/src/main/java/com/example/calibretv/ui/screens/ReaderScreen.kt`

### 2.1 — Rediseñar el HUD inferior como barra única con scroll horizontal

Leer el archivo completo. Localizar el bloque del HUD inferior que comienza alrededor de la línea 733 con:
```kotlin
// Bottom Row: Action Controls with Single-Click
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    ...
```

**Reemplazar** todo ese bloque (que contiene dos `Row` hijos separados: navegación a la izquierda y opciones a la derecha) por **una sola** fila con scroll horizontal usando `LazyRow`:

```kotlin
// Bottom Row: barra única con scroll horizontal — todos los botones en una línea
androidx.compose.foundation.lazy.LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
) {
    item {
        StitchHudButton(
            title = "‹ Anterior",
            icon = Icons.Default.ChevronLeft,
            isPrimary = false,
            modifier = Modifier.focusRequester(hudInitialFocusRequester),
            onClick = { turnPage(forward = false) }
        )
    }
    item {
        StitchHudButton(
            title = "Salto de Página",
            icon = Icons.Default.MenuBook,
            isPrimary = true,
            onClick = {
                val jumpIdx = (currentSpreadIndex + 5).coerceAtMost(spreads.size - 1)
                currentSpreadIndex = jumpIdx
                repository.saveBookProgress(book.id, jumpIdx)
            }
        )
    }
    item {
        StitchHudButton(
            title = "Siguiente ›",
            icon = Icons.Default.ChevronRight,
            isPrimary = false,
            onClick = { turnPage(forward = true) }
        )
    }
    item { Spacer(Modifier.width(16.dp)) }  // separador visual
    item {
        StitchHudButton(
            title = "Fuente (${settings.fontSizeSp}sp)",
            icon = Icons.Default.FormatSize,
            isPrimary = false,
            onClick = {
                val nextSize = when (settings.fontSizeSp) {
                    16 -> 18; 18 -> 20; 20 -> 22; 22 -> 24; else -> 16
                }
                settings = settings.copy(fontSizeSp = nextSize)
                repository.saveReadingSettings(settings)
            }
        )
    }
    item {
        val themeName = when (settings.theme) {
            ReadingTheme.PERGAMINO -> "Pergamino"
            ReadingTheme.OLED_PURE -> "OLED Puro"
            ReadingTheme.SEPIA_CINE -> "Sepia"
            ReadingTheme.NIGHT_AMBER -> "Ámbar"
            ReadingTheme.PROYECTOR_BLANCO -> "Proyector"
            ReadingTheme.CINE_OSCURO -> "Cine"
        }
        StitchHudButton(
            title = "Tema: $themeName",
            icon = Icons.Default.Palette,
            isPrimary = false,
            onClick = {
                val nextTheme = when (settings.theme) {
                    ReadingTheme.PERGAMINO -> ReadingTheme.OLED_PURE
                    ReadingTheme.OLED_PURE -> ReadingTheme.SEPIA_CINE
                    ReadingTheme.SEPIA_CINE -> ReadingTheme.NIGHT_AMBER
                    ReadingTheme.NIGHT_AMBER -> ReadingTheme.PROYECTOR_BLANCO
                    ReadingTheme.PROYECTOR_BLANCO -> ReadingTheme.CINE_OSCURO
                    ReadingTheme.CINE_OSCURO -> ReadingTheme.PERGAMINO
                }
                settings = settings.copy(theme = nextTheme)
                repository.saveReadingSettings(settings)
            }
        )
    }
    item {
        val marginPct = settings.overscanPercent
        StitchHudButton(
            title = "Márgenes: $marginPct%",
            icon = Icons.Default.AspectRatio,
            isPrimary = settings.overscanPercent > 0,
            onClick = {
                val nextMargin = when (settings.overscanPercent) { 0 -> 4; 4 -> 8; else -> 0 }
                settings = settings.copy(overscanPercent = nextMargin)
                repository.saveReadingSettings(settings)
            }
        )
    }
    item {
        val brightnessPercent = (settings.readerBrightness * 100).toInt()
        StitchHudButton(
            title = "☀ $brightnessPercent%",
            icon = Icons.Filled.Brightness4,
            isPrimary = settings.readerBrightness < 1.0f,
            onClick = {
                val nextBrightness = when {
                    settings.readerBrightness > 0.85f -> 0.70f
                    settings.readerBrightness > 0.60f -> 0.50f
                    settings.readerBrightness > 0.40f -> 0.30f
                    else -> 1.0f
                }
                settings = settings.copy(readerBrightness = nextBrightness)
                repository.saveReadingSettings(settings)
            }
        )
    }
    item {
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
                val nextMinutes = when (settings.sleepTimerMinutes) {
                    0 -> 15; 15 -> 30; 30 -> 45; 45 -> 60; else -> 0
                }
                settings = settings.copy(sleepTimerMinutes = nextMinutes)
                repository.saveReadingSettings(settings)
                isSleepTimerActive = nextMinutes > 0
            }
        )
    }
    item {
        StitchHudButton(
            title = if (isTtsPlaying) "⏸ Pausa" else "▶ Leer",
            icon = Icons.Filled.RecordVoiceOver,
            isPrimary = isTtsPlaying,
            onClick = {
                if (isTtsPlaying) {
                    ttsController.stop()
                } else {
                    val leftText = currentSpread?.leftPage?.paragraphs?.joinToString(" ") ?: ""
                    val rightText = currentSpread?.rightPage?.paragraphs?.joinToString(" ") ?: ""
                    val pageText = listOf(leftText, rightText).filter { it.isNotBlank() }.joinToString(" ")
                    if (pageText.isNotBlank()) {
                        ttsController.readPage(pageText, settings.ttsSpeedRate)
                    }
                }
            }
        )
    }
}
```

> [!IMPORTANT]
> Agregar al bloque de imports: `import androidx.compose.foundation.lazy.LazyRow` y `import androidx.compose.foundation.lazy.items` si no existen ya.

**Git commit:** `"fix: reader HUD redesigned as single horizontal scrollable bar with all controls visible"`

---

## FASE 3 — Agregar tarjetas faltantes en el menú de Ajustes
**Duración estimada: 1 hora**

**Problema:** Las configuraciones de Sleep Timer, TTS y Modo Cine/Brillo no aparecen en `SettingsScreen`. Solo existen en el HUD del lector pero no tienen tarjeta de configuración en Ajustes.

**Archivo a modificar:** `app/src/main/java/com/example/calibretv/ui/screens/SettingsScreen.kt`

### 3.1 — Agregar icono `RecordVoiceOver` y `Timer` a los imports

Si no existen:
```kotlin
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.NightShelter
```

### 3.2 — Agregar tarjeta "Sleep Timer" después de la tarjeta de Efectos de Sonido

Leer el archivo completo. Localizar el cierre de la tarjeta `"Efectos de Sonido (Paso de Página)"`. Insertar DESPUÉS de ella:

```kotlin
Spacer(modifier = Modifier.height(16.dp))

// Sleep Timer
CleanBentoCard(
    modifier = Modifier.fillMaxWidth(),
    title = "Temporizador de Apagado (Sleep Timer)",
    icon = Icons.Filled.Timer
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Apaga automáticamente el lector tras el tiempo seleccionado",
                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "La pantalla se atenúa progresivamente en los últimos 2 minutos antes de cerrar.",
                color = TextMuted, fontSize = 11.sp
            )
        }
        Row(
            modifier = Modifier.width(300.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0 to "Apagado", 15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hora").forEach { (min, label) ->
                SegmentedOption(
                    modifier = Modifier.weight(1f),
                    title = label,
                    isSelected = settings.sleepTimerMinutes == min,
                    onClick = {
                        settings = settings.copy(sleepTimerMinutes = min)
                        repository.saveReadingSettings(settings)
                    }
                )
            }
        }
    }
}

Spacer(modifier = Modifier.height(16.dp))

// Lectura en Voz Alta (TTS)
CleanBentoCard(
    modifier = Modifier.fillMaxWidth(),
    title = "Lectura en Voz Alta (TTS)",
    icon = Icons.Filled.RecordVoiceOver
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Lee el libro en voz alta con resaltado de oración activa",
                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Activa desde el HUD del lector (botón ▶ Leer). Ajusta aquí la velocidad.",
                color = TextMuted, fontSize = 11.sp
            )
        }
        Row(
            modifier = Modifier.width(220.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.75f to "Lento", 1.0f to "Normal", 1.25f to "Rápido", 1.5f to "Veloz").forEach { (speed, label) ->
                SegmentedOption(
                    modifier = Modifier.weight(1f),
                    title = label,
                    isSelected = settings.ttsSpeedRate == speed,
                    onClick = {
                        settings = settings.copy(ttsSpeedRate = speed)
                        repository.saveReadingSettings(settings)
                    }
                )
            }
        }
    }
}

Spacer(modifier = Modifier.height(16.dp))

// Brillo del Lector y Modo Cine
CleanBentoCard(
    modifier = Modifier.fillMaxWidth(),
    title = "Brillo del Lector y Modo Cine",
    icon = Icons.Filled.Brightness4
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Control de brillo interno independiente del brillo del TV",
                color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "También ajustable desde el HUD del lector. Combinar con tema 'Cine Oscuro' para sala oscura.",
                color = TextMuted, fontSize = 11.sp
            )
        }
        Row(
            modifier = Modifier.width(240.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1.0f to "100%", 0.70f to "70%", 0.50f to "50%", 0.30f to "30%").forEach { (brightness, label) ->
                SegmentedOption(
                    modifier = Modifier.weight(1f),
                    title = label,
                    isSelected = settings.readerBrightness == brightness,
                    onClick = {
                        settings = settings.copy(readerBrightness = brightness)
                        repository.saveReadingSettings(settings)
                    }
                )
            }
        }
    }
}
```

**Git commit:** `"feat: add Sleep Timer, TTS speed and Reader Brightness cards to Settings screen"`

---

## FASE 4 — Sonido Ambiental para Lectura
**Duración estimada: 2 horas**

El usuario usa un TV en sala de estar. La función reproduce sonidos de ambiente de forma continua mientras lee, con volumen independiente.

### 4.1 — Agregar `ambientSound` a `ReadingSettings` en `Models.kt`

```kotlin
@Serializable
enum class AmbientSound {
    NONE,       // Sin sonido
    RAIN,       // Lluvia suave
    FIREPLACE,  // Chimenea crepitando
    OCEAN,      // Olas del mar
    CAFE,       // Cafetería con murmullos
    FOREST      // Bosque / naturaleza
}
```

Y en `ReadingSettings`:
```kotlin
val ambientSound: AmbientSound = AmbientSound.NONE,
val ambientVolume: Float = 0.4f  // 0.0f a 1.0f
```

En `PreferencesManager.kt` agregar lectura y escritura:
```kotlin
// En getReadingSettings():
ambientSound = try {
    AmbientSound.valueOf(prefs.getString("ambient_sound", "NONE") ?: "NONE")
} catch (_: Exception) { AmbientSound.NONE },
ambientVolume = prefs.getFloat("ambient_volume", 0.4f),

// En saveReadingSettings():
.putString("ambient_sound", settings.ambientSound.name)
.putFloat("ambient_volume", settings.ambientVolume)
```

### 4.2 — Descargar assets de audio (sonidos ambientales)

Descargar 5 archivos de audio en formato `.ogg` de dominio público (CC0) de [freesound.org](https://freesound.org) o generar tono base sintético si no están disponibles:

| Archivo | Descripción |
|---------|-------------|
| `app/src/main/res/raw/ambient_rain.ogg` | Lluvia suave ~2 min loop |
| `app/src/main/res/raw/ambient_fireplace.ogg` | Chimenea ~2 min loop |
| `app/src/main/res/raw/ambient_ocean.ogg` | Olas del mar ~2 min loop |
| `app/src/main/res/raw/ambient_cafe.ogg` | Cafetería murmullos ~2 min loop |
| `app/src/main/res/raw/ambient_forest.ogg` | Bosque/naturaleza ~2 min loop |

> [!NOTE]
> Si no se pueden descargar sonidos reales con licencia CC0, crear archivos de audio sintéticos de 5 segundos que se repiten en loop. Lo importante es que el sistema de `MediaPlayer` funcione para cuando la usuaria agregue los archivos reales.
> Alternativamente, usar la búsqueda de freesound: https://freesound.org/search/?q=rain+loop&f=license%3A%22Creative+Commons+0%22

### 4.3 — Crear `AmbientSoundManager.kt`

**Archivo nuevo:** `app/src/main/java/com/example/calibretv/data/sound/AmbientSoundManager.kt`

```kotlin
package com.example.calibretv.data.sound

import android.content.Context
import android.media.MediaPlayer
import com.example.calibretv.R
import com.example.calibretv.data.model.AmbientSound

class AmbientSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: AmbientSound = AmbientSound.NONE

    private fun getResId(sound: AmbientSound): Int? = when (sound) {
        AmbientSound.NONE -> null
        AmbientSound.RAIN -> R.raw.ambient_rain
        AmbientSound.FIREPLACE -> R.raw.ambient_fireplace
        AmbientSound.OCEAN -> R.raw.ambient_ocean
        AmbientSound.CAFE -> R.raw.ambient_cafe
        AmbientSound.FOREST -> R.raw.ambient_forest
    }

    fun play(sound: AmbientSound, volume: Float = 0.4f) {
        if (sound == currentSound && mediaPlayer?.isPlaying == true) {
            // Solo ajustar volumen si el mismo sonido ya está activo
            mediaPlayer?.setVolume(volume, volume)
            return
        }
        stop()
        if (sound == AmbientSound.NONE) return
        val resId = getResId(sound) ?: return
        try {
            mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            currentSound = sound
        } catch (_: Exception) {}
    }

    fun setVolume(volume: Float) {
        mediaPlayer?.setVolume(volume, volume)
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        currentSound = AmbientSound.NONE
    }

    fun release() = stop()
}
```

### 4.4 — Integrar `AmbientSoundManager` en `ReaderScreen.kt`

Dentro de `ReaderScreen()`, agregar:
```kotlin
val ambientManager = remember { AmbientSoundManager(context) }
DisposableEffect(Unit) {
    onDispose { ambientManager.release() }
}

// LaunchedEffect para activar/cambiar el sonido cuando cambian los settings
LaunchedEffect(settings.ambientSound, settings.ambientVolume) {
    ambientManager.play(settings.ambientSound, settings.ambientVolume)
}
```

### 4.5 — Agregar botón de sonido ambiental al HUD del lector

En la `LazyRow` del HUD (creada en Fase 2), agregar item:

```kotlin
item {
    val ambientLabel = when (settings.ambientSound) {
        AmbientSound.NONE -> "🔕 Ambiente"
        AmbientSound.RAIN -> "🌧 Lluvia"
        AmbientSound.FIREPLACE -> "🔥 Chimenea"
        AmbientSound.OCEAN -> "🌊 Mar"
        AmbientSound.CAFE -> "☕ Café"
        AmbientSound.FOREST -> "🌲 Bosque"
    }
    StitchHudButton(
        title = ambientLabel,
        icon = Icons.Filled.MusicNote,
        isPrimary = settings.ambientSound != AmbientSound.NONE,
        onClick = {
            val nextSound = when (settings.ambientSound) {
                AmbientSound.NONE -> AmbientSound.RAIN
                AmbientSound.RAIN -> AmbientSound.FIREPLACE
                AmbientSound.FIREPLACE -> AmbientSound.OCEAN
                AmbientSound.OCEAN -> AmbientSound.CAFE
                AmbientSound.CAFE -> AmbientSound.FOREST
                AmbientSound.FOREST -> AmbientSound.NONE
            }
            settings = settings.copy(ambientSound = nextSound)
            repository.saveReadingSettings(settings)
        }
    )
}
```

> Agregar import: `import androidx.compose.material.icons.filled.MusicNote`

### 4.6 — Agregar tarjeta de Sonido Ambiental en `SettingsScreen.kt`

Después de la tarjeta de TTS (Fase 3), insertar:

```kotlin
Spacer(modifier = Modifier.height(16.dp))

// Sonido Ambiental
CleanBentoCard(
    modifier = Modifier.fillMaxWidth(),
    title = "Sonido Ambiental de Lectura",
    icon = Icons.Filled.MusicNote
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reproduce ambiente sonoro continuo durante la lectura",
                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Diseñado para TV en sala. Cicla entre sonidos desde el HUD del lector.",
                    color = TextMuted, fontSize = 11.sp
                )
            }
        }
        // Selector de tipo de sonido
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                AmbientSound.NONE to "🔕 Ninguno",
                AmbientSound.RAIN to "🌧 Lluvia",
                AmbientSound.FIREPLACE to "🔥 Chimenea",
                AmbientSound.OCEAN to "🌊 Mar",
                AmbientSound.CAFE to "☕ Café",
                AmbientSound.FOREST to "🌲 Bosque"
            ).forEach { (sound, label) ->
                SegmentedOption(
                    modifier = Modifier.weight(1f),
                    title = label,
                    isSelected = settings.ambientSound == sound,
                    onClick = {
                        settings = settings.copy(ambientSound = sound)
                        repository.saveReadingSettings(settings)
                    }
                )
            }
        }
    }
}
```

> Agregar import en SettingsScreen.kt: `import com.example.calibretv.data.model.AmbientSound`

**Git commit:** `"feat: ambient reading sounds (rain, fireplace, ocean, cafe, forest) with TV-optimized HUD control"`

---

## FASE 5 — Corrección del botón Favorito con estrella duplicada
**Duración estimada: 10 minutos**

**Archivo a modificar:** `app/src/main/java/com/example/calibretv/ui/screens/HomeScreen.kt`

**Problema:** El texto del botón incluye el emoji `★` / `☆` Y el ícono `Icons.Default.Star`. Resultado: doble estrella visual.

Localizar las líneas (aprox. L675 y L810):
```kotlin
title = if (repository.isFavorite(book.id)) "★ En Favoritos" else "☆ Favorito",
// y
title = if (isFavorite) "★ Favorito" else "☆ Favorito",
```

Reemplazar por (eliminar el emoji del texto, el ícono ya muestra la estrella):
```kotlin
title = if (repository.isFavorite(book.id)) "En Favoritos" else "Favorito",
// y
title = if (isFavorite) "En Favoritos" else "Favorito",
```

**Git commit:** `"fix: remove duplicate star emoji from Favorite button — icon already shows the star"`

---

## FASE 6 — Mejora UX del panel de creación de usuarios para TV
**Duración estimada: 45 minutos**

**Archivo a modificar:** `app/src/main/java/com/example/calibretv/ui/components/UserProfilesDialog.kt`

### 6.1 — Campo de nombre con visibilidad mejorada

Leer el archivo completo. Localizar el `OutlinedTextField` (aprox. L311). Reemplazar el componente completo por una versión con mejor contraste y tamaño para TV:

```kotlin
// Reemplazar el OutlinedTextField existente por:
var isNameFieldFocused by remember { mutableStateOf(false) }
Box(
    modifier = Modifier
        .weight(1f)
        .height(52.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(if (isNameFieldFocused) Color(0xFF1E2A35) else SurfaceContainerHigh)
        .border(
            width = if (isNameFieldFocused) 2.dp else 1.dp,
            color = if (isNameFieldFocused) CyanElectric else Color(0xFF4A4A58),
            shape = RoundedCornerShape(10.dp)
        )
        .onFocusChanged { isNameFieldFocused = it.isFocused }
) {
    OutlinedTextField(
        value = newUserName,
        onValueChange = { newUserName = it },
        placeholder = {
            Text(
                "Ej: Laura, Niños...",
                color = if (isNameFieldFocused) TextMuted else Color(0xFF606070),
                fontSize = 14.sp
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanElectric
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    )
}
```

### 6.2 — Selector de colores grande y claramente enfocable con D-Pad

Reemplazar el bloque del selector de colores (aprox. L326-342) por:

```kotlin
// Color selector — círculos grandes con foco D-Pad visible
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    presetColors.forEach { hex ->
        val isColorSelected = selectedColorHex == hex
        var isColorFocused by remember { mutableStateOf(false) }
        val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { AmberWarm }

        Box(
            modifier = Modifier
                .size(if (isColorFocused) 42.dp else 36.dp)  // crece al enfocar
                .shadow(
                    elevation = if (isColorFocused) 10.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = parsedColor
                )
                .clip(CircleShape)
                .background(parsedColor)
                .border(
                    width = when {
                        isColorFocused -> 3.dp
                        isColorSelected -> 3.dp
                        else -> 0.dp
                    },
                    color = when {
                        isColorFocused -> Color.White
                        isColorSelected -> Color.White
                        else -> Color.Transparent
                    },
                    shape = CircleShape
                )
                .onFocusChanged { isColorFocused = it.isFocused }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        selectedColorHex = hex
                        true
                    } else false
                }
                .focusable()
                .clickable { selectedColorHex = hex },
            contentAlignment = Alignment.Center
        ) {
            // Checkmark cuando está seleccionado
            if (isColorSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
```

> [!IMPORTANT]
> Verificar que los imports `Key`, `KeyEventType`, `onKeyEvent` ya están en el archivo (están en L45-48). Agregar si falta: `import androidx.compose.ui.draw.shadow`

**Git commit:** `"fix: user creation panel — larger name field with visible focus state and D-Pad navigable color circles"`

---

## FASE 7 — Integración de Shelves de Calibre-Web como filtros dinámicos
**Duración estimada: 3 horas**

Esta fase conecta la app con los Shelves (estanterías) de Calibre-Web para:
- Mostrar los shelves como filtros en la biblioteca
- Usar los shelves del libro como sus "categorías/etiquetas"
- Tomar la descripción del libro desde Calibre-Web

### 7.1 — Agregar modelo `CalibreShelf` en `Models.kt`

```kotlin
@Serializable
data class CalibreShelf(
    val id: String,
    val name: String,
    val bookIds: List<String> = emptyList(),
    val isCharacterShelf: Boolean = false  // true si el nombre es de un personaje de TV/serie
)
```

También agregar campo `shelves` al modelo `Book`:
```kotlin
@Serializable
data class Book(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String? = null,
    val epubUrl: String? = null,
    val summary: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val shelves: List<String> = emptyList(),  // ← NUEVO: nombres de shelves de Calibre
    val progressPercent: Int = 0,
    val lastReadSpread: Int = 0
)
```

> [!IMPORTANT]
> Verificar que `AppDatabase.kt` tiene `BookEntity` con campo `shelves`. Si no existe, agregar:
> ```kotlin
> val shelves: String = ""  // JSON array de nombres de shelves
> ```
> Y actualizar los métodos `toEntity()` y `toBook()` de conversión.

### 7.2 — Agregar fetch de shelves en `OpdsClient.kt`

Calibre-Web expone los shelves en OPDS en: `GET /opds/shelf/{shelf_id}` y el listado en `/opds/shelves`.

Agregar en `OpdsClient.kt`:

```kotlin
/**
 * Obtiene todos los shelves públicos de Calibre-Web.
 * Endpoint: GET /opds/shelves
 * Retorna lista de CalibreShelf con nombre e ID.
 */
suspend fun fetchShelves(
    serverUrl: String,
    username: String = "",
    password: String = ""
): Result<List<CalibreShelf>> = withContext(Dispatchers.IO) {
    try {
        val baseServer = serverUrl.trim()
            .removeSuffix("/opds/").removeSuffix("/opds").removeSuffix("/")
        val shelvesUrl = "$baseServer/opds/shelves"

        val url = URL(shelvesUrl)
        val conn = url.openConnection() as HttpURLConnection
        SslHelper.configureHttps(conn)
        conn.connectTimeout = 8000
        conn.readTimeout = 14000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "CalibreTV/1.0 (Android TV)")
        val auth = CoverLoader.buildBasicAuth(username, password)
        if (auth != null) conn.setRequestProperty("Authorization", auth)
        conn.setRequestProperty("Accept", "application/atom+xml,application/xml,text/xml,*/*")
        conn.connect()

        if (conn.responseCode in 200..299) {
            conn.inputStream.use { stream ->
                val shelves = parseShelvesAtomFeed(stream)
                Result.success(shelves)
            }
        } else {
            Result.failure(Exception("Error ${conn.responseCode} al obtener shelves"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Obtiene todos los libros de un shelf específico.
 * Endpoint: GET /opds/shelf/{shelfId}
 */
suspend fun fetchShelfBooks(
    shelfFeedUrl: String,
    username: String = "",
    password: String = ""
): Result<List<String>> = withContext(Dispatchers.IO) {
    // Devuelve lista de book IDs pertenecientes al shelf
    try {
        val feed = fetchFeed(shelfFeedUrl, username, password).getOrThrow()
        Result.success(feed.books.map { it.id })
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Parsea el feed ATOM de /opds/shelves extrayendo nombre e ID de cada shelf.
 */
private fun parseShelvesAtomFeed(stream: InputStream): List<CalibreShelf> {
    val shelves = mutableListOf<CalibreShelf>()
    try {
        val parser = Xml.newPullParser()
        parser.setInput(stream, null)
        var currentId = ""
        var currentTitle = ""
        var currentFeedUrl = ""
        var inEntry = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name.lowercase()) {
                    "entry" -> { inEntry = true; currentId = ""; currentTitle = ""; currentFeedUrl = "" }
                    "id" -> if (inEntry) currentId = parser.nextText()
                    "title" -> if (inEntry) currentTitle = parser.nextText()
                    "link" -> if (inEntry) {
                        val rel = parser.getAttributeValue(null, "rel") ?: ""
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        if (rel == "subsection" || href.contains("/shelf/")) {
                            currentFeedUrl = href
                        }
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name.lowercase() == "entry" && inEntry) {
                    inEntry = false
                    if (currentTitle.isNotBlank() && currentId.isNotBlank()) {
                        val shelfId = currentId.substringAfterLast(":").substringAfterLast("/")
                        // Detectar si es shelf de personaje (no es nivel 1-5)
                        val isCharacter = !currentTitle.matches(Regex("""^\d+ nivel.*""", RegexOption.IGNORE_CASE))
                        shelves.add(CalibreShelf(
                            id = shelfId,
                            name = currentTitle,
                            isCharacterShelf = isCharacter
                        ))
                    }
                }
            }
            eventType = parser.next()
        }
    } catch (_: Exception) {}
    return shelves
}
```

### 7.3 — Actualizar `BookRepository.kt` para cargar shelves y enriquecer libros

Agregar en `BookRepository`:

```kotlin
private var cachedShelves: List<CalibreShelf> = emptyList()

/**
 * Carga todos los shelves de Calibre-Web y los asocia a los libros del catálogo.
 * Llama después de fetchLibraryCatalog().
 */
suspend fun loadAndApplyShelves(config: ServerConfig) {
    val shelvesResult = OpdsClient.fetchShelves(config.serverUrl, config.username, config.password)
    val shelvesList = shelvesResult.getOrNull() ?: return
    cachedShelves = shelvesList

    // Para cada shelf, obtener sus libros y etiquetar
    val bookShelfMap = mutableMapOf<String, MutableList<String>>() // bookId -> [shelfNames]
    for (shelf in shelvesList) {
        val baseServer = config.serverUrl.trim()
            .removeSuffix("/opds/").removeSuffix("/opds").removeSuffix("/")
        val shelfUrl = "$baseServer/opds/shelf/${shelf.id}"
        val bookIds = OpdsClient.fetchShelfBooks(shelfUrl, config.username, config.password)
            .getOrNull() ?: continue
        for (bookId in bookIds) {
            bookShelfMap.getOrPut(bookId) { mutableListOf() }.add(shelf.name)
        }
    }

    // Actualizar los libros en Room con sus shelves
    val allBooks = bookDao.getAllBooks()
    val updated = allBooks.map { entity ->
        val shelvesForBook = bookShelfMap[entity.id] ?: emptyList()
        entity.copy(shelves = org.json.JSONArray(shelvesForBook).toString())
    }
    bookDao.upsertBooks(updated)
}

/** Devuelve los shelves cargados, ordenando personajes primero */
fun getShelves(): List<CalibreShelf> {
    return cachedShelves.sortedWith(
        compareByDescending<CalibreShelf> { it.isCharacterShelf }.thenBy { it.name }
    )
}
```

### 7.4 — Agregar descripción desde Calibre-Web con cadena de fallback

En `OpdsClient.kt`, en el método `parseAtomFeed()`, asegurarse de que el campo `<summary>` o `<content>` del feed ATOM se mapee correctamente al campo `summary` del modelo `Book`. Si el parser ya lo hace, verificar. Si `summary` llega vacío, aplicar fallback en `BookRepository`:

```kotlin
/**
 * Enriquece la descripción de un libro con cadena de fallback:
 * 1. summary de Calibre-Web (si no está vacío)
 * 2. <dc:description> dentro del EPUB (content.opf)
 * 3. Descripción generada con metadata disponible
 */
suspend fun enrichBookDescription(book: Book, epubFile: File?): String {
    // Nivel 1: ya tiene descripción de Calibre-Web
    if (book.summary.isNotBlank() && book.summary.length > 30) return book.summary

    // Nivel 2: leer description del content.opf dentro del EPUB
    if (epubFile != null && epubFile.exists()) {
        try {
            val desc = withContext(Dispatchers.IO) {
                java.util.zip.ZipFile(epubFile).use { zip ->
                    val opfEntry = zip.entries().asSequence()
                        .firstOrNull { it.name.endsWith(".opf", ignoreCase = true) }
                    if (opfEntry != null) {
                        val opfText = zip.getInputStream(opfEntry).bufferedReader().readText()
                        Regex("""<dc:description[^>]*>(.*?)</dc:description>""", RegexOption.DOT_MATCHES_ALL)
                            .find(opfText)?.groupValues?.get(1)?.trim()
                    } else null
                }
            }
            if (!desc.isNullOrBlank() && desc.length > 20) return desc
        } catch (_: Exception) {}
    }

    // Nivel 3: descripción generada automáticamente
    val shelvesStr = if (book.shelves.isNotEmpty()) " · Colecciones: ${book.shelves.joinToString(", ")}" else ""
    val tagsStr = if (book.tags.isNotEmpty()) " · Géneros: ${book.tags.joinToString(", ")}" else ""
    return "\"${book.title}\" de ${book.author}.${tagsStr}${shelvesStr}"
}
```

### 7.5 — Actualizar `LibraryScreen.kt` con filtros de Shelves

**Archivo a modificar:** `app/src/main/java/com/example/calibretv/ui/screens/LibraryScreen.kt`

Leer el archivo completo. Localizar la fila de filtros de géneros/categorías (actualmente muestra tags como "Drama", "Infantil", etc.).

**Reemplazar** el sistema de filtros por uno que muestre los shelves de personajes como filtros principales y los géneros como secundarios:

```kotlin
// Estado
var selectedShelf by remember { mutableStateOf<String?>(null) }
val shelves = remember { repository.getShelves() }
val characterShelves = shelves.filter { it.isCharacterShelf }
val levelShelves = shelves.filter { !it.isCharacterShelf }

// Filtrado de libros
val filteredBooks = if (selectedShelf == null) {
    allBooks
} else {
    allBooks.filter { book -> book.shelves.contains(selectedShelf) }
}

// UI de filtros — fila horizontal
Column(modifier = Modifier.fillMaxWidth()) {
    // Filtros de personajes (destacados)
    if (characterShelves.isNotEmpty()) {
        Text("Colecciones", color = TextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                ShelfFilterChip(
                    label = "📚 Todos",
                    isSelected = selectedShelf == null,
                    onClick = { selectedShelf = null }
                )
            }
            items(characterShelves) { shelf ->
                // Extraer nombre limpio del personaje (quitar "(serie (Public))")
                val cleanName = shelf.name.substringBefore("(").trim()
                ShelfFilterChip(
                    label = cleanName,
                    isSelected = selectedShelf == shelf.name,
                    onClick = {
                        selectedShelf = if (selectedShelf == shelf.name) null else shelf.name
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Filtros de nivel (secundarios)
    if (levelShelves.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(levelShelves) { shelf ->
                val cleanName = shelf.name.substringBefore("(").trim()
                ShelfFilterChip(
                    label = cleanName,
                    isSelected = selectedShelf == shelf.name,
                    onClick = {
                        selectedShelf = if (selectedShelf == shelf.name) null else shelf.name
                    }
                )
            }
        }
    }
}
```

Agregar composable `ShelfFilterChip` al final del archivo:
```kotlin
@Composable
private fun ShelfFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when {
                    isSelected -> AmberWarm
                    isFocused -> CyanElectric.copy(alpha = 0.25f)
                    else -> SurfaceContainerHigh
                }
            )
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 0.dp else 1.dp,
                color = if (isFocused) CyanElectric else if (isSelected) Color.Transparent else Color(0xFF383842),
                shape = RoundedCornerShape(20.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFF131315) else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
```

### 7.6 — Usar shelves como etiquetas de libro en tarjetas de biblioteca

En `LibraryScreen.kt`, en la tarjeta de libro donde se muestran las categorías/tags, reemplazar para mostrar los shelves de personaje:

```kotlin
// En lugar de: book.category o book.tags
// Mostrar: shelves del libro (solo nombre limpio del personaje)
val displayTags = book.shelves
    .filter { shelfName ->
        // Mostrar solo shelves de personaje (excluir "1 nivel", "2 nivel", etc.)
        !shelfName.matches(Regex("""^\d+ nivel.*""", RegexOption.IGNORE_CASE))
    }
    .map { it.substringBefore("(").trim() }
    .take(2) // Máximo 2 etiquetas para no desbordar la tarjeta
    .ifEmpty { book.tags.take(2) } // Fallback a tags genéricos si no hay shelves
```

### 7.7 — Disparar carga de shelves al sincronizar

En `BookRepository.kt`, en el método que se llama al sincronizar con Calibre-Web (buscar la llamada a `fetchLibraryCatalog`), agregar llamada después de cargar el catálogo:

```kotlin
// Después de cargar el catálogo de libros, cargar shelves:
val config = prefs.getServerConfig()
loadAndApplyShelves(config)
```

**Git commit:** `"feat: Calibre-Web shelves as dynamic library filters — character shelves, tags and description fallback chain"`

---

## FASE 8 — Compilación Final y Push
**Duración estimada: 10 minutos**

### 8.1 — Compilación de verificación

```powershell
$env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'
.\gradlew.bat assembleRelease --no-daemon
```

Verificar `BUILD SUCCESSFUL`.

### 8.2 — Actualizar `versionCode` y `versionName` en `app/build.gradle.kts`

```kotlin
versionCode = 5
versionName = "2.1"
```

### 8.3 — Commit y push final

```bash
git add .
git commit -m "feat: CalibroTV v2.1 — EPUB fix, HUD redesign, ambient sounds, shelves filters, UX improvements"
git push origin main
```

### 8.4 — Notificar al revisor

Al terminar, reportar:
1. Resultado de compilación (BUILD SUCCESSFUL / errores).
2. Hash del commit final.
3. Fases completadas vs. pendientes.
4. Cualquier desviación de diseño necesaria.

---

## 📁 Resumen de Archivos a Modificar / Crear

### Archivos MODIFICADOS:
```
data/model/Models.kt                    ← Fases 4, 7 (AmbientSound, shelves en Book, CalibreShelf)
data/epub/EpubParser.kt                 ← Fase 1 (strip XML headers)
data/opds/OpdsClient.kt                 ← Fase 7 (fetchShelves, fetchShelfBooks)
data/BookRepository.kt                  ← Fases 4, 7 (AmbientSound, loadAndApplyShelves)
data/storage/PreferencesManager.kt      ← Fase 4 (ambient_sound, ambient_volume)
data/storage/AppDatabase.kt             ← Fase 7 (shelves field en BookEntity)
ui/screens/ReaderScreen.kt              ← Fases 2, 4 (HUD LazyRow, AmbientSoundManager)
ui/screens/SettingsScreen.kt            ← Fases 3, 4 (tarjetas Sleep, TTS, Brillo, Ambiente)
ui/screens/HomeScreen.kt                ← Fase 5 (quitar emoji ★ del texto)
ui/screens/LibraryScreen.kt             ← Fase 7 (filtros de shelves, etiquetas)
ui/components/UserProfilesDialog.kt     ← Fase 6 (campo nombre + selector color)
app/build.gradle.kts                    ← Fase 8 (versionCode=5, versionName="2.1")
```

### Archivos NUEVOS:
```
data/sound/AmbientSoundManager.kt       ← Fase 4
res/raw/ambient_rain.ogg                ← Fase 4
res/raw/ambient_fireplace.ogg           ← Fase 4
res/raw/ambient_ocean.ogg               ← Fase 4
res/raw/ambient_cafe.ogg                ← Fase 4
res/raw/ambient_forest.ogg              ← Fase 4
```

---

> [!IMPORTANT]
> **Para el Agente:** Leer cada archivo completo antes de modificarlo. Hacer modificaciones quirúrgicas en las secciones indicadas. La animación 3D (`curlAnim`) en `ReaderScreen.kt` no debe tocarse en ninguna circunstancia. Compilar después de cada fase antes del commit.
