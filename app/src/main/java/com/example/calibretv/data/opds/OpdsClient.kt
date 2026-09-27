package com.example.calibretv.data.opds

import android.util.Xml
import com.example.calibretv.data.image.CoverLoader
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.OpdsCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class OpdsFeedContent(
    val title: String,
    val categories: List<OpdsCategory>,
    val books: List<Book>,
    val nextUrl: String? = null
)

object OpdsClient {

    suspend fun fetchFeed(
        serverUrl: String,
        username: String = "",
        password: String = ""
    ): Result<OpdsFeedContent> = withContext(Dispatchers.IO) {
        try {
            val url = URL(serverUrl.trim())
            val conn = url.openConnection() as HttpURLConnection
            SslHelper.configureHttps(conn)
            conn.connectTimeout = 8000
            conn.readTimeout = 14000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "CalibreTV/1.0 (Android TV)")

            val auth = CoverLoader.buildBasicAuth(username, password)
            if (auth != null) {
                conn.setRequestProperty("Authorization", auth)
            }
            conn.setRequestProperty("Accept", "application/atom+xml,application/xml,text/xml,*/*")
            conn.connect()

            val code = conn.responseCode
            if (code in 200..299) {
                conn.inputStream.use { stream ->
                    val content = parseAtomFeed(stream, serverUrl)
                    Result.success(content)
                }
            } else if (code == 401 || code == 403) {
                Result.failure(Exception("Error $code: Acceso no autorizado. Verifica tu Usuario y Contraseña de Calibre-Web."))
            } else {
                Result.failure(Exception("Error $code del servidor: ${conn.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Smart Calibre-Web and generic OPDS catalog scanner.
     * Follows OPDS navigation down to the full book catalog (e.g. /opds/books/letter/00 in Calibre-Web)
     * and paginates through all 'rel=next' pages to load ALL books in the library.
     */
    suspend fun fetchLibraryCatalog(
        serverUrl: String,
        username: String = "",
        password: String = ""
    ): Result<OpdsFeedContent> = withContext(Dispatchers.IO) {
        val cleanUrl = serverUrl.trim()
        val baseServer = if (cleanUrl.endsWith("/opds")) cleanUrl.dropLast(5)
            else if (cleanUrl.endsWith("/opds/")) cleanUrl.dropLast(6)
            else if (cleanUrl.endsWith("/")) cleanUrl.dropLast(1)
            else cleanUrl

        // 1. First probe for Calibre-Web canonical full catalog endpoint: /opds/books/letter/00
        val calibreAllUrl = "$baseServer/opds/books/letter/00"
        val probeAll = fetchFeed(calibreAllUrl, username, password)
        var baseFeed: OpdsFeedContent? = null

        if (probeAll.isSuccess && probeAll.getOrNull()!!.books.isNotEmpty()) {
            baseFeed = probeAll.getOrNull()!!
        } else {
            // 2. Fall back to standard root navigation feed
            val initialResult = fetchFeed(cleanUrl, username, password)
            if (initialResult.isFailure) {
                return@withContext initialResult
            }
            val rootFeed = initialResult.getOrNull()!!

            if (rootFeed.books.isNotEmpty()) {
                baseFeed = rootFeed
            } else {
                // If root OPDS returned navigation categories:
                // Check categories for "All", "Alphabetical", "Books", "Todos", "Libros"
                val candidates = rootFeed.categories.filter { cat ->
                    val t = cat.title.lowercase()
                    t.contains("libro") || t.contains("book") || t.contains("todo") ||
                            t.contains("all") || t.contains("alphabet") || t.contains("novedad")
                }

                for (candidate in candidates) {
                    if (candidate.feedUrl.isNotBlank()) {
                        val subResult = fetchFeed(candidate.feedUrl, username, password)
                        if (subResult.isSuccess) {
                            val subFeed = subResult.getOrNull()!!
                            if (subFeed.books.isNotEmpty()) {
                                baseFeed = subFeed
                                break
                            } else if (subFeed.categories.isNotEmpty()) {
                                // Nested subcategory (e.g. /opds/books -> "All" -> /opds/books/letter/00)
                                val allCat = subFeed.categories.firstOrNull { c ->
                                    val ct = c.title.lowercase()
                                    ct == "all" || ct == "todos" || ct.contains("all") || c.feedUrl.contains("00")
                                } ?: subFeed.categories.firstOrNull()

                                if (allCat != null && allCat.feedUrl.isNotBlank()) {
                                    val nestedRes = fetchFeed(allCat.feedUrl, username, password)
                                    if (nestedRes.isSuccess && nestedRes.getOrNull()!!.books.isNotEmpty()) {
                                        baseFeed = nestedRes.getOrNull()!!
                                        break
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (baseFeed == null || baseFeed.books.isEmpty()) {
            return@withContext Result.failure(Exception("No se encontraron libros en el servidor OPDS. Verifica la dirección."))
        }

        // 3. Paginate through all rel=next pages to index the entire library
        val allBooks = mutableListOf<Book>()
        allBooks.addAll(baseFeed.books)

        var currentNextUrl = baseFeed.nextUrl
        var page = 1
        while (!currentNextUrl.isNullOrBlank() && page < 60) {
            page++
            val nextResult = fetchFeed(currentNextUrl, username, password)
            if (nextResult.isSuccess) {
                val nextFeed = nextResult.getOrNull()!!
                if (nextFeed.books.isEmpty()) break
                allBooks.addAll(nextFeed.books)
                currentNextUrl = nextFeed.nextUrl
            } else {
                break
            }
        }

        // Deduplicate books by ID or Title
        val dedupedBooks = allBooks.distinctBy { it.id.ifBlank { it.title } }
        return@withContext Result.success(
            baseFeed.copy(
                books = dedupedBooks,
                nextUrl = null
            )
        )
    }

    private fun parseAtomFeed(stream: InputStream, baseUrl: String): OpdsFeedContent {
        val categories = mutableListOf<OpdsCategory>()
        val books = mutableListOf<Book>()
        var feedTitle = "Catálogo Calibre"
        var feedNextUrl: String? = null

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(stream, null)

        var eventType = parser.eventType
        var currentId = ""
        var currentTitle = ""
        var currentAuthor = ""
        var currentSummary = ""
        var currentCategory = "General"
        val currentTags = mutableListOf<String>()
        var currentCover: String? = null
        var currentEpub: String? = null
        var currentPdf: String? = null
        var currentNavigationLink: String? = null
        var inEntry = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (name.equals("title", ignoreCase = true) && !inEntry) {
                        feedTitle = parser.nextText().trim()
                    } else if (name.equals("link", ignoreCase = true) && !inEntry) {
                        val rel = parser.getAttributeValue(null, "rel") ?: ""
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        if (rel.contains("next", ignoreCase = true) && href.isNotBlank()) {
                            feedNextUrl = resolveUrl(baseUrl, href)
                        }
                    } else if (name.equals("entry", ignoreCase = true)) {
                        inEntry = true
                        currentId = ""
                        currentTitle = ""
                        currentAuthor = ""
                        currentSummary = ""
                        currentCategory = "General"
                        currentTags.clear()
                        currentCover = null
                        currentEpub = null
                        currentPdf = null
                        currentNavigationLink = null
                    } else if (inEntry) {
                        when (name.lowercase()) {
                            "id" -> currentId = try { parser.nextText().trim() } catch (_: Exception) { "" }
                            "title" -> currentTitle = try { parser.nextText().trim() } catch (_: Exception) { "" }
                            "summary", "content" -> currentSummary = try { parser.nextText().trim() } catch (_: Exception) { "" }
                            "name", "author" -> {
                                val a = try { parser.nextText().trim() } catch (_: Exception) { "" }
                                if (a.isNotBlank() && currentAuthor.isBlank()) currentAuthor = a
                            }
                            "category" -> {
                                val term = parser.getAttributeValue(null, "term")
                                    ?: parser.getAttributeValue(null, "label")
                                    ?: ""
                                if (term.isNotBlank()) {
                                    val cleanTerm = term.trim()
                                    if (!cleanTerm.startsWith("http://") && !cleanTerm.startsWith("https://")) {
                                        currentTags.add(cleanTerm)
                                        if (currentCategory == "General") currentCategory = cleanTerm
                                    }
                                }
                            }
                            "link" -> {
                                val rel = parser.getAttributeValue(null, "rel") ?: ""
                                val type = parser.getAttributeValue(null, "type") ?: ""
                                val href = parser.getAttributeValue(null, "href") ?: ""
                                val linkTitle = parser.getAttributeValue(null, "title") ?: ""

                                if (rel.contains("image") || rel.contains("thumbnail") || rel.contains("cover") || type.startsWith("image/")) {
                                    val resolved = resolveUrl(baseUrl, href)
                                    if (currentCover == null || rel.contains("cover") || !rel.contains("thumbnail")) {
                                        currentCover = resolved
                                    }
                                } else if ((type.contains("epub") || linkTitle.equals("EPUB", true) || href.contains("/epub")) && (rel.contains("acquisition") || href.contains("/download/"))) {
                                    currentEpub = resolveUrl(baseUrl, href)
                                } else if (currentPdf == null && (type.contains("pdf") || linkTitle.equals("PDF", true) || href.contains("/pdf")) && (rel.contains("acquisition") || href.contains("/download/"))) {
                                    currentPdf = resolveUrl(baseUrl, href)
                                } else if (rel.contains("subsection") || type.contains("atom+xml") || rel.contains("nav")) {
                                    currentNavigationLink = resolveUrl(baseUrl, href)
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("entry", ignoreCase = true)) {
                        inEntry = false
                        if (currentTitle.isNotBlank()) {
                            // STRICT REQUIREMENT: Only import books that have a genuine EPUB file.
                            // Books that are solely PDF or without download links are excluded from the app!
                            if (currentEpub != null) {
                                val cleanTags = if (currentTags.isNotEmpty()) currentTags.distinct() else listOf(currentCategory)
                                val finalSummary = if (currentSummary.isNotBlank() && currentSummary.length > 25 && !currentSummary.trim().startsWith("TAGS:", ignoreCase = true)) {
                                    currentSummary
                                } else {
                                    buildSmartDescription(currentTitle, currentAuthor, currentCategory, cleanTags)
                                }

                                books.add(
                                    Book(
                                        id = currentId.ifBlank { "book_${books.size + 1}" },
                                        title = currentTitle,
                                        author = currentAuthor.ifBlank { "Autor Desconocido" },
                                        coverUrl = currentCover,
                                        epubUrl = currentEpub,
                                        summary = finalSummary,
                                        category = currentCategory,
                                        tags = cleanTags
                                    )
                                )
                            } else if (currentNavigationLink != null) {
                                categories.add(
                                    OpdsCategory(
                                        id = currentId.ifBlank { "cat_${categories.size + 1}" },
                                        title = currentTitle,
                                        feedUrl = currentNavigationLink!!
                                    )
                                )
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return OpdsFeedContent(
            title = feedTitle,
            categories = categories,
            books = books,
            nextUrl = feedNextUrl
        )
    }

    fun resolveUrl(base: String, path: String): String {
        return try {
            val baseUri = URI(base)
            val resolved = baseUri.resolve(path)
            resolved.toString()
        } catch (_: Exception) {
            path
        }
    }

    fun buildSmartDescription(title: String, author: String, category: String, tags: List<String>): String {
        val cleanAuthor = if (author.isNotBlank() && !author.equals("Autor Desconocido", ignoreCase = true)) author else "Autor contemporáneo"
        val filteredTags = tags.filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
        val genre = if (filteredTags.isNotEmpty()) filteredTags.take(3).joinToString(", ")
            else if (category.isNotBlank() && !category.equals("General", ignoreCase = true)) category
            else "Literatura"
        return "Obra de $cleanAuthor dentro del género $genre. Título sincronizado desde tu servidor Calibre-Web, optimizado para lectura inmersiva pliego a pliego en formato panorámico 16:9."
    }

    fun getDemoFeedContent(): OpdsFeedContent {
        return OpdsFeedContent(
            title = "Biblioteca CalibroTV",
            categories = emptyList(),
            books = emptyList()
        )
    }
}
