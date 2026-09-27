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
            it.contains("manga")
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
