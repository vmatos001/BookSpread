package com.example.calibretv.data.server

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.comic.ComicParser
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.model.Book
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.util.Collections

class WifiImportServer(private val context: Context, private val repository: BookRepository) {

    private val TAG = "CalibroWifiServer"
    private var server: HttpServer? = null
    var isRunning: Boolean = false
        private set

    var connectedClientsCount: Int = 0
        private set

    var uploadedFilesCount: Int = 0
        private set

    fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is InetAddress) {
                        val host = addr.hostAddress ?: ""
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining IP", e)
        }
        return "192.168.1.100"
    }

    fun startServer(port: Int = 8080): Boolean {
        if (isRunning) return true
        return try {
            server = HttpServer.create(InetSocketAddress(port), 0).apply {
                createContext("/", MainHandler())
                createContext("/upload", UploadHandler())
                executor = java.util.concurrent.Executors.newFixedThreadPool(4)
                start()
            }
            isRunning = true
            Log.d(TAG, "Server started on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting HttpServer", e)
            isRunning = false
            false
        }
    }

    fun stopServer() {
        try {
            server?.stop(0)
            server = null
        } catch (_: Exception) {}
        isRunning = false
    }

    private inner class MainHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            connectedClientsCount++
            val html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>CalibroTV — Importar Libro por WiFi</title>
                    <style>
                        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #121216; color: #fff; text-align: center; padding: 24px; margin: 0; }
                        .card { max-width: 480px; margin: 20px auto; background: #1e1e24; border-radius: 16px; padding: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.4); border: 1px solid #333; }
                        h1 { color: #ffb300; font-size: 24px; margin-bottom: 8px; }
                        p { color: #aaa; font-size: 14px; line-height: 1.5; }
                        .drop-zone { border: 2px dashed #00e5ff; border-radius: 12px; padding: 32px 16px; margin: 20px 0; background: rgba(0, 229, 255, 0.05); cursor: pointer; }
                        input[type="file"] { display: none; }
                        .btn { background: #ffb300; color: #121216; border: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 24px; cursor: pointer; width: 100%; margin-top: 12px; }
                        .btn:hover { background: #ffa000; }
                        .status { margin-top: 16px; font-weight: bold; color: #00e5ff; }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>📖 CalibroTV</h1>
                        <p>Sube libros (.epub) o cómics (.cbz / .cbr) directamente a tu televisor.</p>
                        <form action="/upload" method="post" enctype="multipart/form-data" id="uploadForm">
                            <div class="drop-zone" onclick="document.getElementById('fileInput').click()">
                                <p id="dropText">📁 Haz clic aquí para seleccionar o arrastra tu archivo EPUB / CBZ</p>
                                <input type="file" name="file" id="fileInput" accept=".epub,.cbz,.cbr" onchange="fileSelected()">
                            </div>
                            <button type="submit" class="btn">🚀 Enviar a CalibroTV</button>
                        </form>
                        <div class="status" id="statusMsg"></div>
                    </div>
                    <script>
                        function fileSelected() {
                            const fi = document.getElementById('fileInput');
                            if (fi.files.length > 0) {
                                document.getElementById('dropText').innerText = "📄 " + fi.files[0].name;
                            }
                        }
                    </script>
                </body>
                </html>
            """.trimIndent()

            val bytes = html.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    private inner class UploadHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            if (!exchange.requestMethod.equals("POST", ignoreCase = true)) {
                exchange.sendResponseHeaders(451, -1)
                return
            }

            try {
                val input = exchange.requestBody
                val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: ""
                
                // Read input stream and save to temporary file
                val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.tmp")
                FileOutputStream(tempFile).use { out ->
                    input.copyTo(out)
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    uploadedFilesCount++
                    // Process file asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        processUploadedFile(tempFile, contentType)
                    }

                    val successHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head><meta charset="UTF-8"><title>¡Enviado!</title>
                        <style>
                            body { background: #121216; color: #fff; font-family: sans-serif; text-align: center; padding: 40px; }
                            .box { background: #1e1e24; border-radius: 16px; padding: 32px; max-width: 400px; margin: auto; border: 1px solid #00e5ff; }
                            h2 { color: #00e5ff; }
                            a { color: #ffb300; font-weight: bold; text-decoration: none; }
                        </style>
                        </head>
                        <body>
                            <div class="box">
                                <h2>✅ ¡Libro Enviado con Éxito!</h2>
                                <p>El archivo ya está disponible en la pantalla de tu televisor.</p>
                                <br>
                                <a href="/">+ Subir otro libro</a>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()

                    val bytes = successHtml.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "text/html; charset=utf-8")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                } else {
                    exchange.sendResponseHeaders(500, -1)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling upload", e)
                exchange.sendResponseHeaders(500, -1)
            }
        }
    }

    private suspend fun processUploadedFile(tempFile: File, contentType: String) {
        withContext(Dispatchers.IO) {
            try {
                val isComic = ComicParser.isComicFile(tempFile) || contentType.contains("zip") || tempFile.name.endsWith(".cbz")
                val bookId = "local_wifi_${System.currentTimeMillis()}"
                
                var title = "Libro Importado WiFi"
                var author = "Importado por WiFi"
                var summary = "Libro importado directamente desde tu dispositivo mediante WiFi."

                if (!isComic) {
                    val parsed = EpubParser.parseEpubToBook(tempFile, tempFile.nameWithoutExtension)
                    if (parsed.title.isNotBlank()) title = parsed.title
                    val extractedDesc = EpubParser.extractDescription(tempFile)
                    if (!extractedDesc.isNullOrBlank()) summary = extractedDesc
                } else {
                    title = tempFile.nameWithoutExtension
                }

                val destFile = File(context.filesDir, "book_$bookId.${if (isComic) "cbz" else "epub"}")
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()

                val newBook = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUrl = null,
                    epubUrl = destFile.absolutePath,
                    summary = summary,
                    category = if (isComic) "Cómic" else "WiFi",
                    tags = listOf("WiFi", "Local")
                )

                val current = repository.getCachedBooks().toMutableList()
                current.add(0, newBook)
                repository.saveCachedBooks(current)
                Log.d(TAG, "Successfully processed and saved uploaded book: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing uploaded file", e)
            }
        }
    }
}
