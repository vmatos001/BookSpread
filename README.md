# CalibroTV 📖📺

**CalibroTV** es una aplicación moderna y cinemática para **Android TV y Amazon Fire TV** diseñada para conectarse a servidores **Calibre-Web (OPDS)** y ofrecer una experiencia de lectura inmersiva con **física 3D realista de paso de página en pliego dual (16:9)**.

---

## ✨ Características Principales

- **📖 Motor de Lectura 3D Dual-Page:**
  - Pliego dual panorámico optimizado para televisores y proyectores (relación de aspecto 16:9).
  - Animación de paso de página realista inspirada en *StPageFlip*: origen en el lomo central (`TransformOrigin(0f, 0.5f)`), curva orgánica Bézier cúbica y sombreado cilíndrico dinámico.
  - Soporte de velocidades de paso de página: Rápida (300ms), Normal (500ms) y Cinemática (750ms).
- **🔤 Paginación Dinámica y Adaptativa:**
  - Recalcula el presupuesto de palabras por página en tiempo real al cambiar el tamaño de fuente (16sp a 24sp), garantizando que ningún texto quede cortado ni desborde el borde inferior.
  - División limpia de párrafos entre pliegos y soporte de letra capital (drop-cap).
- **🎨 Soporte Completo de Ilustraciones EPUB:**
  - Extracción y renderizado en línea de imágenes e ilustraciones vectoriales/bitmap en su posición narrativa exacta con caché en memoria `LruCache`.
- **🌐 Conexión OPDS Calibre-Web:**
  - Navegación fluida por Bibliotecas, Búsquedas y Descargas de EPUBs directamente al dispositivo.
  - Compatibilidad con certificados SSL autofirmados y autenticación básica HTTP.
- **👥 Perfiles Multi-Usuario y Favoritos:**
  - Creación y gestión de perfiles de lectura independientes con listas de libros favoritos y seguimiento de lectura en curso.
- **🎮 Control Remoto 100% Optimizado (D-Pad):**
  - Navegación fluida mediante teclado/mando a distancia en todos los paneles, asistentes y menú lateral (HUD).

---

## 🛠️ Tecnologías y Arquitectura

- **Plataforma:** Android TV / Fire OS (API 26+)
- **Lenguaje:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose for TV / Material 3
- **Concurrencia:** Kotlin Coroutines & Flow
- **Persistencia:** EncryptedSharedPreferences & Room/File storage
- **Parser EPUB:** Extracción nativa con `XmlPullParser` y descompresión ZIP en streaming

---

## 🚀 Compilación e Instalación

### Requisitos
- **JDK 17**
- **Android SDK** con Platform API 35 y Build-Tools 35.0.0

### Construcción
```bash
# Compilar versión Release
./gradlew assembleRelease

# Firmar y alinear APK
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

---

## 📄 Licencia

Este proyecto está bajo la Licencia MIT.
