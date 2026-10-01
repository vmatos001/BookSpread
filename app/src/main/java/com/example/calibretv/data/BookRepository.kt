package com.example.calibretv.data

import android.content.Context
import com.example.calibretv.data.comic.ComicParser
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.epub.PageSpread
import com.example.calibretv.data.epub.ParsedBook
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.model.CalibreShelf
import com.example.calibretv.data.model.OpdsCategory
import com.example.calibretv.data.model.ReadingSettings
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.data.model.UserProfile
import com.example.calibretv.data.opds.OpdsClient
import com.example.calibretv.data.opds.OpdsFeedContent
import com.example.calibretv.data.provider.BookSourceProvider
import com.example.calibretv.data.provider.DirectTransferProvider
import com.example.calibretv.data.provider.LocalRoomProvider
import com.example.calibretv.data.provider.OpdsProvider
import com.example.calibretv.data.storage.AppDatabase
import com.example.calibretv.data.storage.BookEntity
import com.example.calibretv.data.storage.FavoriteEntity
import com.example.calibretv.data.storage.PreferencesManager
import com.example.calibretv.data.storage.ReadingProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 📚 BookRepository — Repositorio Central de BookSpread v3.0 (Multi-Source Pattern)
 * Orquesta los proveedores de libros (LocalRoomProvider, DirectTransferProvider, OpdsProvider)
 * garantizando funcionamiento offline instantáneo y compatibilidad omnicanal.
 */
class BookRepository(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val db = AppDatabase.getInstance(context)
    private val bookDao = db.bookDao()
    private val progressDao = db.progressDao()
    private val favoriteDao = db.favoriteDao()

    // Proveedores desacoplados de fuentes de libros
    val localProvider = LocalRoomProvider(context)
    val directTransferProvider = DirectTransferProvider(context)
    val opdsProvider = OpdsProvider(context) { getServerConfig() }

    val providers: List<BookSourceProvider> = listOf(
        localProvider,
        directTransferProvider,
        opdsProvider
    )

    private fun BookEntity.toBook(): Book {
        val tagList = try {
            val arr = org.json.JSONArray(tags)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
        val shelfList = try {
            val arr = org.json.JSONArray(shelves)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
        return Book(
            id = id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            epubUrl = epubUrl,
            summary = summary,
            category = category,
            tags = tagList,
            shelves = shelfList,
            progressPercent = progressPercent
        )
    }

    private fun Book.toEntity(lastReadSpread: Int = 0): BookEntity {
        val tagsJson = org.json.JSONArray(tags).toString()
        val shelvesJson = org.json.JSONArray(shelves).toString()
        return BookEntity(
            id = id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            epubUrl = epubUrl,
            summary = summary,
            category = category,
            tags = tagsJson,
            shelves = shelvesJson,
            progressPercent = progressPercent,
            lastReadSpread = lastReadSpread
        )
    }

    // Configuración y Perfiles
    fun getServerConfig(): ServerConfig = prefs.getServerConfig()
    fun saveServerConfig(config: ServerConfig) = prefs.saveServerConfig(config)

    fun getReadingSettings(): ReadingSettings = prefs.getReadingSettings()
    fun saveReadingSettings(settings: ReadingSettings) = prefs.saveReadingSettings(settings)

    fun getActiveProfile(): UserProfile = prefs.getActiveProfile()
    fun saveActiveProfile(profile: UserProfile) = prefs.saveActiveProfile(profile)
    fun getProfiles(): List<UserProfile> = prefs.getProfiles()
    fun saveProfiles(profiles: List<UserProfile>) = prefs.saveProfiles(profiles)
    fun createProfile(name: String, colorHex: String = "#C5A059"): UserProfile = prefs.createProfile(name, colorHex)

    // Favoritos
    fun isFavorite(bookId: String): Boolean = runBlocking(Dispatchers.IO) {
        favoriteDao.isFavorite(prefs.getActiveProfile().id, bookId)
    }

    fun toggleFavorite(bookId: String): Boolean = runBlocking(Dispatchers.IO) {
        val profileId = prefs.getActiveProfile().id
        val isFav = favoriteDao.isFavorite(profileId, bookId)
        if (isFav) {
            favoriteDao.remove(profileId, bookId)
            false
        } else {
            favoriteDao.add(FavoriteEntity(profileId, bookId))
            true
        }
    }

    fun getFavoriteBookIds(): Set<String> = runBlocking(Dispatchers.IO) {
        favoriteDao.getFavoriteIds(prefs.getActiveProfile().id).toSet()
    }

    fun getFavoriteBooks(): List<Book> = runBlocking(Dispatchers.IO) {
        val favIds = favoriteDao.getFavoriteIds(prefs.getActiveProfile().id)
        if (favIds.isEmpty()) emptyList() else bookDao.getBooksByIds(favIds).map { it.toBook() }
    }

    // Catálogo en Caché / Base Local
    fun getCachedBooks(): List<Book> = runBlocking(Dispatchers.IO) {
        val entities = bookDao.getAllBooks()
        if (entities.isEmpty()) {
            val legacy = prefs.getCachedBooks()
            if (legacy.isNotEmpty()) {
                bookDao.upsertBooks(legacy.map { it.toEntity() })
                return@runBlocking legacy
            }
        }
        entities.map { it.toBook() }
    }

    suspend fun saveCachedBooks(books: List<Book>) = withContext(Dispatchers.IO) {
        bookDao.upsertBooks(books.map { it.toEntity() })
    }

    // Progreso de Lectura
    fun getBookProgress(bookId: String): Int = runBlocking(Dispatchers.IO) {
        progressDao.getSpreadIndex(prefs.getActiveProfile().id, bookId) ?: prefs.getBookProgress(bookId)
    }

    fun getBookProgressPercent(bookId: String): Int = runBlocking(Dispatchers.IO) {
        progressDao.getPercent(prefs.getActiveProfile().id, bookId) ?: prefs.getBookProgressPercent(bookId)
    }

    fun saveBookProgress(bookId: String, spreadIndex: Int, percent: Int = 0) {
        val profileId = prefs.getActiveProfile().id
        prefs.saveBookProgress(bookId, spreadIndex, percent)
        CoroutineScope(Dispatchers.IO).launch {
            progressDao.upsert(
                ReadingProgressEntity(
                    profileId = profileId,
                    bookId = bookId,
                    spreadIndex = spreadIndex,
                    percent = percent,
                    lastReadAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun getLastOpenedBook(): Book? = prefs.getLastOpenedBook()
    fun saveLastOpenedBook(book: Book) = prefs.saveLastOpenedBook(book)

    private var cachedShelves: List<CalibreShelf> = emptyList()

    fun getShelves(): List<CalibreShelf> = cachedShelves

    suspend fun loadAndApplyShelves(config: ServerConfig): List<CalibreShelf> = withContext(Dispatchers.IO) {
        val result = OpdsClient.fetchShelves(config.serverUrl, config.username, config.password)
        if (result.isSuccess) {
            val shelves = result.getOrNull() ?: emptyList()
            cachedShelves = shelves

            if (shelves.isNotEmpty()) {
                val currentBooks = getCachedBooks()
                val updatedBooks = currentBooks.map { book ->
                    val matchingShelves = shelves.filter { it.bookIds.contains(book.id) }.map { it.name }
                    if (matchingShelves.isNotEmpty()) {
                        book.copy(shelves = matchingShelves)
                    } else {
                        book
                    }
                }
                saveCachedBooks(updatedBooks)
            }
            shelves
        } else {
            emptyList()
        }
    }

    suspend fun getOrFetchBookDescription(book: Book): String = withContext(Dispatchers.IO) {
        // 1. Si la descripción ya es texto real, la retornamos inmediatamente
        if (book.summary.isNotBlank() && book.summary.length > 25 &&
            !book.summary.startsWith("Obra de", ignoreCase = true) &&
            !book.summary.contains("Sin descripción", ignoreCase = true)
        ) {
            return@withContext book.summary
        }

        // 2. Consulta al endpoint individual de detalles de Calibre-Web (/opds/book/{id}) si está disponible
        val config = getServerConfig()
        if (config.serverUrl.isNotBlank()) {
            val fetchedSynopsis = OpdsClient.fetchBookDetailSynopsis(config.serverUrl, book.id, config.username, config.password)
            if (!fetchedSynopsis.isNullOrBlank() && fetchedSynopsis.length > 15 && !fetchedSynopsis.startsWith("Obra de", ignoreCase = true)) {
                val updatedBook = book.copy(summary = fetchedSynopsis)
                saveCachedBooks(listOf(updatedBook))
                return@withContext fetchedSynopsis
            }
        }

        // 3. Extraer descripción del archivo local si ya se encuentra resuelto
        val resolved = resolveBookFile(book)
        if (resolved != null && resolved.exists() && resolved.length() > 0L) {
            val internalDesc = EpubParser.extractDescription(resolved)
            if (!internalDesc.isNullOrBlank() && internalDesc.length > 15) {
                val updatedBook = book.copy(summary = internalDesc)
                saveCachedBooks(listOf(updatedBook))
                return@withContext internalDesc
            }
        }

        // 4. Fallback generado inteligente
        val tagsToUse = book.shelves.ifEmpty { book.tags }
        OpdsClient.buildSmartDescription(book.title, book.author, book.category, tagsToUse)
    }

    /**
     * Sincronización con servidor OPDS (opcional y secundario en BookSpread v3.0).
     */
    suspend fun scanServerLibrary(config: ServerConfig): Result<OpdsFeedContent> = withContext(Dispatchers.IO) {
        val scanResult = OpdsClient.fetchLibraryCatalog(
            serverUrl = config.serverUrl,
            username = config.username,
            password = config.password
        )

        if (scanResult.isSuccess) {
            val feed = scanResult.getOrNull()!!
            saveServerConfig(config)
            if (feed.books.isNotEmpty()) {
                val mergedBooks = feed.books.map { newBook ->
                    val savedPct = getBookProgressPercent(newBook.id)
                    if (savedPct > 0) newBook.copy(progressPercent = savedPct) else newBook
                }
                saveCachedBooks(mergedBooks)

                try {
                    loadAndApplyShelves(config)
                } catch (_: Exception) {}

                Result.success(feed.copy(books = mergedBooks))
            } else {
                Result.success(feed)
            }
        } else {
            scanResult
        }
    }

    /**
     * Orquestador unificado de catálogo multi-fuente:
     * Carga libros locales (Room y transferencias WiFi directas) y los unifica sin bloquear
     * por falta de red ni exigir configuración previa de servidor.
     */
    suspend fun getFeed(targetUrl: String? = null): OpdsFeedContent = withContext(Dispatchers.IO) {
        val config = getServerConfig()

        // 1. Si se solicita un subfeed OPDS explícito
        if (!targetUrl.isNullOrBlank() && (targetUrl.startsWith("http://") || targetUrl.startsWith("https://"))) {
            val result = OpdsClient.fetchFeed(targetUrl, config.username, config.password)
            if (result.isSuccess) {
                val feed = result.getOrNull()!!
                val annotated = feed.books.map { b ->
                    val realPct = getBookProgressPercent(b.id)
                    b.copy(progressPercent = if (realPct > 0) realPct else b.progressPercent)
                }
                return@withContext feed.copy(books = annotated)
            }
        }

        // 2. Cargar desde la biblioteca local (Room + Transferencias WiFi directas)
        val cached = getCachedBooks()
        if (cached.isNotEmpty()) {
            val allTags = cached.flatMap { it.tags.ifEmpty { listOf(it.category) } }
                .distinct()
                .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }

            val categories = mutableListOf(OpdsCategory("cat_all", "Todos", ""))
            allTags.forEachIndexed { idx, tag ->
                categories.add(OpdsCategory("cat_$idx", tag, ""))
            }

            val annotated = cached.map { b ->
                val realPct = getBookProgressPercent(b.id)
                b.copy(progressPercent = if (realPct > 0) realPct else b.progressPercent)
            }
            return@withContext OpdsFeedContent(
                title = "Biblioteca BookSpread",
                categories = categories,
                books = annotated
            )
        }

        // 3. Si no hay libros en Room pero hay un servidor OPDS configurado, intentar escaneo no bloqueante
        if (opdsProvider.isAvailable()) {
            try {
                val scanResult = scanServerLibrary(config)
                if (scanResult.isSuccess) {
                    val feed = scanResult.getOrNull()!!
                    if (feed.books.isNotEmpty()) {
                        val annotated = feed.books.map { b ->
                            val realPct = getBookProgressPercent(b.id)
                            b.copy(progressPercent = if (realPct > 0) realPct else b.progressPercent)
                        }
                        return@withContext feed.copy(books = annotated)
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Catálogo vacío local-first (cero crashes, listo para transferencias WiFi)
        OpdsFeedContent(
            title = "Biblioteca BookSpread",
            categories = listOf(OpdsCategory("cat_all", "Todos", "")),
            books = emptyList()
        )
    }

    /**
     * Resuelve el archivo físico del libro a través de la cadena de proveedores:
     * 1. LocalRoomProvider
     * 2. DirectTransferProvider (WiFi Import)
     * 3. OpdsProvider (Descarga remota en cacheDir)
     */
    suspend fun resolveBookFile(book: Book): File? = withContext(Dispatchers.IO) {
        for (provider in providers) {
            try {
                val file = provider.resolveBookFile(book)
                if (file != null && file.exists() && file.length() > 0) {
                    return@withContext file
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * En BookSpread v3.0 el setup inicial no es obligatorio para entrar a la app.
     * La app arranca inmediatamente en modo local.
     */
    fun isSetupCompleted(): Boolean = true
    fun setSetupCompleted(completed: Boolean) = prefs.setSetupCompleted(completed)

    /**
     * Carga el libro para lectura 3D resolviendo el archivo físico mediante la cadena de proveedores.
     */
    suspend fun loadRawBook(book: Book): ParsedBook = withContext(Dispatchers.IO) {
        saveLastOpenedBook(book)

        val file = resolveBookFile(book)
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext EpubParser.getNoticeBook(
                book.title,
                "No se pudo cargar el archivo del libro. Si fue transferido por WiFi, asegúrate de que el archivo no haya sido eliminado."
            )
        }

        return@withContext EpubParser.parseEpubToBook(file, book.title)
    }

    suspend fun loadBookSpreads(
        book: Book,
        fontSizeSp: Int = 18,
        overscanPercent: Int = 0
    ): List<PageSpread> = withContext(Dispatchers.IO) {
        val raw = loadRawBook(book)
        return@withContext EpubParser.paginate(raw, fontSizeSp, overscanPercent)
    }

    suspend fun loadComic(book: Book): ComicParser.ParsedComic = withContext(Dispatchers.IO) {
        val file = resolveBookFile(book)
        if (file == null || !file.exists() || file.length() == 0L) {
            return@withContext ComicParser.ParsedComic(book.title, emptyList())
        }
        ComicParser.parseCbz(file, context.cacheDir)
    }
}
