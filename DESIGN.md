# 🎨 BookSpread — Editorial Luxury Design System (10-Foot TV UI)

> **Filosofía de Diseño:** *"The High-End Atelier"* (El Taller Editorial de Alta Gama)  
> **Objetivo:** Erradicar la estética genérica de software y "AI slop" (neones gamer, degradados cibernéticos y fuentes genéricas). Transformar el televisor en una biblioteca encuadernada en tela y pan de oro, al nivel de *The Folio Society, Criterion Channel y Penguin Classics*.

---

## 🚫 Reglas Anti-AI-Slop (Prohibiciones Estéticas Estrictas)

1. **PROHIBIDO el cian eléctrico / azul gamer (`#38bdf8`)**: Nada de halos fluorescentes tipo consola o cripto-dashboard.
2. **PROHIBIDO el plástico gris (`#131315`)**: Todos los oscuros deben tener una sub-tonalidad cálida orgánica de tinta y madera.
3. **PROHIBIDO ilustraciones 3D hinchadas con destellos**: Cero libros flotando con chispas mágicas generadas por IA. Solo arte vectorial plano, grabados litográficos sobrios o tipografía pura.
4. **PROHIBIDO el uso de fuentes sans-serif para todo**: Los títulos literarios y citas DEBEN tener peso editorial en tipografía Serif clásica.

---

## 🏛️ Paleta de Color "Noble Ink & Gold"

Diseñada matemáticamente para pantallas **OLED y 4K**, maximizando el descanso visual en la sala de estar a 3 metros de distancia.

```
┌────────────────────────────────────────────────────────────────────────┐
│  SUPERFICIES Y FONDOS (OLED SAFE)                                      │
│  #0C0A09  Obsidian Ink      Fondo base absoluto (negro tinta china)    │
│  #181513  Binding Board     Superficie de tarjeta elevada (madera)     │
│  #221E1B  Pressed Linen     Superficie con foco D-Pad activa           │
│  #2D2925  Elevated Dock     Acrílico translúcido del menú HUD          │
│                                                                        │
│  ACENTOS NOBLES (PAN DE ORO Y CUERO)                                   │
│  #C5A059  Gold Foil         Aro de foco D-Pad, botones de acción       │
│  #D4AF37  Bright Gold       Estrellas de calificación, medallas        │
│  #423419  Deep Gold Tint    Fondo atenuado de selección                │
│  #4A1E22  Moroccan Leather  Cuero granate para insignias especiales    │
│                                                                        │
│  TIPOGRAFÍA EDITORIAL (CONTRASTE WCAG AAA)                             │
│  #F7F4EE  Antique Ivory     Texto principal de lectura y títulos       │
│  #D4CFC6  Soft Parchment    Subtítulos y nombres de autor              │
│  #A8A29E  Warm Linen        Sinopsis, metadata y etiquetas secundarias │
└────────────────────────────────────────────────────────────────────────┘
```

### Tabla de Tokens de Color para Compose

| Token Name | Hex Code | Uso Específico en Compose |
| :--- | :--- | :--- |
| `SurfaceBase` | `#0C0A09` | Fondo de `HomeScreen`, `LibraryGridScreen` y `ReaderScreen` (OLED). |
| `SurfaceCard` | `#181513` | Fondo de las tarjetas de libros sin enfocar. |
| `SurfaceFocused`| `#221E1B` | Fondo de la tarjeta cuando recibe foco D-Pad. |
| `AccentGold` | `#C5A059` | Borde de foco activo (2dp), selector de página y botón "Leer". |
| `StarGold` | `#D4AF37` | Estrellas de valoración y sellos de Dominio Público. |
| `TextPrimary` | `#F7F4EE` | Títulos grandes, cuerpo de libro en modo noche, botón activo. |
| `TextSecondary`| `#D4CFC6` | Nombre de autores, número de páginas, citas célebres. |
| `TextMuted` | `#A8A29E` | Sinopsis largas, categorías y leyendas de pie de página. |

---

## ✒️ Sistema Tipográfico Dual

El secreto de la percepción de valor es combinar una **voz literaria** para los contenidos con una **voz técnica** para la interfaz.

```
┌────────────────────────────────────────────────────────────────────────┐
│  VOZ LITERARIA (Serif con carácter):                                   │
│  Títulos de libros, citas de personajes, capítulos y lectura en TV.    │
│  → Playfair Display / Cormorant Garamond / Serif System                │
│                                                                        │
│  VOZ DE NAVEGACIÓN (Grotesk de alta legibilidad):                      │
│  Menús D-Pad, ajustes técnicos, tiempos de lectura y badges.           │
│  → Plus Jakarta Sans / Inter con tracking amplio (+0.08em)             │
└────────────────────────────────────────────────────────────────────────┘
```

### Escala de Tamaños para Pantalla 16:9 (10-Foot UI)

- **Headline XL (56sp / Bold):** Título del libro en el Hero Banner principal.
- **Headline LG (36sp / SemiBold):** Títulos de estanterías y nombres de personajes.
- **Body LG (22sp / Regular / Altura de línea 32sp):** Lectura estándar en el visor 3D para salas a 3 metros.
- **Body MD (18sp / Regular):** Sinopsis y citas destacadas.
- **Label LG (14sp / Medium / Mayúsculas espaciadas):** Etiquetas de navegación (`CAPÍTULO III  •  45 MIN`).

---

## 🎯 Comportamiento y Foco D-Pad (Micro-Interacciones Vivas)

En la televisión no hay ratón ni pantalla táctil. La respuesta visual del control remoto debe ser instantánea y gratificante:

### Estado Normal (Sin Foco)
- Tarjeta plana, escala `1.0f`.
- Borde: 0dp (sin contorno visible).
- Sombra suave básica.

### Estado Enfocado (Con Foco D-Pad)
- **Escala de Elevación:** `scale(1.08f)` mediante animación de muelle suave (*Spring Animation* a 60 FPS).
- **Marco de Pan de Oro:** Borde nítido de **2dp** en color `#C5A059` (cero desenfoques borrosos de neón).
- **Sombra Volumétrica Profunda:** Sombra difusa de 28px (`rgba(0, 0, 0, 0.85)`) que levanta el libro de la pantalla.
- **Audio Táctil (Foley):** Micro-chasquido acústico suave de madera/papel en el altavoz de la TV.

---

## 📖 El Lector en Pliego Dual 16:9 (Santuario de Lectura)

La experiencia de lectura es sagrada y no tolera distracciones de software:

1. **La Sombra del Lomo (Spine Shadow):**
   - Un degradado vertical central sutil (`Brush.horizontalGradient` de 32dp de ancho) que simula la curvatura real de las hojas cosidas al lomo de un libro de tapa dura.
2. **Texturas de Papel Curadas:**
   - **Pergamino:** Fondo `#F4EFE6`, texto `#2B2623` (cálido, para el día).
   - **OLED Ink:** Fondo `#000000`, texto `#E5E0D8` (cero luz parásita).
   - **Sepia Cine:** Fondo `#1F1914`, texto `#E0D2C1` (atmósfera cinematográfica).
3. **El HUD Acrílico Oculto:**
   - Durante la lectura: pantalla 100% limpia (cero iconos, cero barras).
   - **D-Pad Flecha ABAJO:** Hace emerger un dock translúcido flotante (`#2D2925` al 85% de opacidad con desenfoque de fondo) con:
     - Control deslizante de tamaño tipográfico (16sp a 28sp).
     - Switch de sonidos relajantes (Lluvia, Chimenea, Océano).
     - Temporizador de sueño (*Sleep Timer*).
   - Se oculta automáticamente tras 4 segundos de inactividad.

---

## 🖼️ Especificación para Avatares y Portadas

- **Avatares de Personajes:** Retratos circulares vectoriales planos con fondo cálido y línea fina de pan de oro de 2dp. Nunca fotografías recortadas ni renders 3D baratos.
- **Proporción de Portadas:** Proporción áurea clásica de libro físico (**1 : 1.5**). Esquinas con redondeo sutil de `8dp` (aspecto de portada encuadernada).

---

## 🏁 Compromiso de Valor
Cualquier persona que encienda su televisor y abra **BookSpread** debe percibir de inmediato que está ante un objeto de diseño de coleccionista. Este sistema de diseño garantiza que cada botón, tipografía y animación justifique con creces el pago de la licencia Pro.
