# 📖 CONTEXTO.md — BookSpread (Lector 3D en Pliego Dual para Android TV)

> **Instrucción para el Agente AI:** Lee este archivo al iniciar cualquier nueva sesión para entender el estado completo del proyecto, la arquitectura, la configuración de firma de APK, la evolución de la marca y las reglas de diseño para Android TV.

---

## 📅 Estado Actual del Proyecto (Actualizado: 30/09/2026)

- **Nombre Oficial:** **BookSpread** *(The 3D Dual-Page Reader for the Living Room)*
- **Versión Activa:** `v3.0-dev` (Evolución arquitectónica y rebranding desde CalibroTV v2.5)
- **Package ID:** `com.bookspread.app`
- **Repositorio Oficial:** `https://github.com/vmatos001/BookSpread` (Upstream original: `https://github.com/vmatos001/calibrotv`)
- **Plan Activo de Desarrollo:** Consúltese [`plan_agente_v3.md`](file:///c:/Users/laura%20hart/Proyectos/BookSpread/bookspread_app/plan_agente_v3.md) (Fases 1, 2, 3 y 4 completadas: APK 6.14 MB, R8 activo, motor PDF en pliego dual y cartelera curada por arquetipos con códigos QR de afiliado y descargas directas).
- **Keystore de Firma:** `app/keystore/calibrotv.keystore` (Firma unificada permanente para Debug y Release con Huella SHA256: `52:E8:ED:A7:6F:9C:CA:51:F5:55:6E:48:47:7C:20:C3:8F:AE:6B:4F:6C:AE:C3:D4:84:FB:98:17:7F:7E:F8:0C`).

---

## 🏗️ Nueva Orientación Arquitectónica (BookSpread v3.0)

El proyecto evoluciona de ser un cliente dependiente de Calibre OPDS a un **reproductor omnicanal y familiar de lectura en televisión**:

1. **Entrada Multi-Fuente ("Trae tu propia biblioteca"):**
   - **Transferencia directa P2P:** Mediante código QR local generado en la TV (`WifiImportServer`) para arrastrar libros desde el móvil o PC sin cables ni nubes.
   - **Nubes Personales:** Conexión con Google Drive / OneDrive mediante autenticación por QR (Device Flow) sin escribir credenciales con el D-Pad.
   - **OPDS Personal:** Se mantiene como opción secundaria en Ajustes para usuarios avanzados con servidor propio Calibre-Web.
   - **Repositorios Abiertos:** Descarga directa de obras de dominio público (Project Gutenberg).

2. **Soporte Universal de Formatos:**
   - **EPUB:** Texto fluido con renderizado 3D de paso de página en pliego dual (16:9).
   - **PDF:** Renderizado con `PdfRenderer` nativo de Android en pliego de dos páginas a la vez (par/impar) con `Bitmap.Config.HARDWARE` y presupuesto estricto de memoria (máximo 3 pliegos en RAM para evitar OOM).
   - **CBZ / CBR:** Visualizador de cómics de alta resolución.

3. **Cartelera y Curaduría por Personajes (Firestore):**
   - Metadatos ligeros descargados por internet sin binarios pesados (cero coste de almacenamiento en la nube).
   - Carruseles guiados por avatares estilizados / arquetipos pop ("La Estudiante Prodigio", "El Médico Cínico", clásicos como Sherlock Holmes) evitando infracción de marcas registradas.
   - Enlaces de compra oficiales mediante código QR dinámico de afiliado (Amazon / librerías) para obras comerciales protegidas.

4. **Perfiles Familiares y Modo Kids (Estilo Netflix Kids):**
   - Selección de perfil visual en 16:9 con PIN parental de 4 dígitos ingresado con D-Pad (Pad numérico 3x4 en pantalla).
   - Modo Infantil con lista blanca estricta (whitelist) de obras aprobadas.
   - Mini-quizzes interactivos de retención al final de cada capítulo y panel de métricas para padres.

5. **Mobile Companion:**
   - Redacción de notas y reseñas desde el teclado del teléfono escaneando un QR.
   - Generación de tarjetas de citas estéticas (*Quote Cards*) en 16:9 para compartir en redes sociales con un toque.

6. **Monetización Ética:**
   - Comisiones de afiliación por compras recomendadas vía QR.
   - Descarga gratuita de obras libres de derechos.
   - Licencia Pro vitalicia mediante Google Play Billing (pago único) para funciones prémium (3D avanzado, audio ambiental coordinado con TTS, perfiles ilimitados).

---

## ⚠️ Reglas Intocables para Agentes de IA

1. **NO alterar la física del paso de página 3D:** Los parámetros `curlAnim`, `CubicBezierEasing` y `TransformOrigin` en `ReaderScreen.kt` están afinados y no deben modificarse.
2. **Prioridad Absoluta al D-Pad:** Toda interacción debe funcionar con el control remoto físico de la televisión. Cero dependencias táctiles.
3. **Cero Fugas de Memoria (Anti-OOM):** En Android TV la RAM es escasa (1-2 GB). Todo renderizado de imágenes debe reciclar bitmaps viejos y operar fuera del hilo principal (`Dispatchers.IO`).
4. **Comprobación de Compilación:** Antes de finalizar cualquier tarea, se debe verificar que compile limpiamente en Release:
   ```powershell
   $env:JAVA_HOME = 'C:\Users\laura hart\AppData\Local\Programs\jdk-17\jdk-17.0.14+7'; .\gradlew.bat assembleRelease --no-daemon
   ```
5. **Preservar Documentación:** Mantener actualizados `CONTEXTO.md` y `plan_agente_v3.md` en cada hito completado.
