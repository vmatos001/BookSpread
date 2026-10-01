package com.example.calibretv.data.server

import android.content.Context
import android.util.Log
import com.example.calibretv.data.BookRepository
import com.example.calibretv.data.comic.ComicParser
import com.example.calibretv.data.epub.EpubParser
import com.example.calibretv.data.model.Book
import com.example.calibretv.data.pdf.PdfParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

/**
 * Pure Android-compliant Socket HTTP Server.
 * Uses java.net.ServerSocket for 100% runtime compatibility on all Android TV devices
 * without depending on desktop JVM classes.
 */
class WifiImportServer(private val context: Context, private val repository: BookRepository) {

    private val TAG = "CalibroWifiServer"
    private var serverSocket: ServerSocket? = null
    @Volatile
    var isRunning: Boolean = false
        private set

    @Volatile
    var connectedClientsCount: Int = 0
        private set

    @Volatile
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
            serverSocket = ServerSocket(port)
            isRunning = true
            Log.d(TAG, "Socket Server started on port $port")

            CoroutineScope(Dispatchers.IO).launch {
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        connectedClientsCount++
                        launch {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting ServerSocket", e)
            isRunning = false
            false
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            socket.use { s ->
                val input = s.getInputStream()
                val output = s.getOutputStream()
                val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))

                val requestLine = reader.readLine() ?: return@withContext
                val tokens = requestLine.split(" ")
                if (tokens.size < 2) return@withContext

                val method = tokens[0]
                val path = tokens[1]

                val headers = mutableMapOf<String, String>()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val headerLine = line ?: break
                    if (headerLine.isBlank()) break
                    val parts = headerLine.split(":", limit = 2)
                    if (parts.size == 2) {
                        headers[parts[0].trim().lowercase()] = parts[1].trim()
                    }
                }

                if (method.equals("GET", ignoreCase = true)) {
                    serveMainPage(output)
                } else if (method.equals("POST", ignoreCase = true) && path.startsWith("/upload")) {
                    val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                    val contentType = headers["content-type"] ?: ""
                    handleFileUpload(input, output, contentLength, contentType)
                } else {
                    serveMainPage(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client connection", e)
        }
    }

    private fun serveMainPage(output: OutputStream) {
        val html = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>BookSpread — Importar Libro por WiFi</title>
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0C0A09; color: #F7F4EE; text-align: center; padding: 24px; margin: 0; }
                    .card { max-width: 480px; margin: 20px auto; background: #181513; border-radius: 16px; padding: 28px; box-shadow: 0 8px 24px rgba(0,0,0,0.6); border: 1px solid #423419; }
                    h1 { color: #C5A059; font-size: 24px; margin-bottom: 8px; letter-spacing: 1px; }
                    p { color: #A8A29E; font-size: 14px; line-height: 1.5; }
                    .drop-zone { border: 2px dashed #C5A059; border-radius: 12px; padding: 32px 16px; margin: 20px 0; background: rgba(197, 160, 89, 0.06); cursor: pointer; }
                    input[type="file"] { display: none; }
                    .btn { background: #C5A059; color: #0C0A09; border: none; padding: 14px 28px; font-size: 16px; font-weight: bold; border-radius: 24px; cursor: pointer; width: 100%; margin-top: 12px; }
                    .btn:hover { background: #D4AF37; }
                    .status { margin-top: 16px; font-weight: bold; color: #C5A059; }
                </style>
            </head>
            <body>
                <div class="card">
                    <h1>📖 BookSpread</h1>
                    <p>Sube libros (.epub, .pdf) o cómics (.cbz / .cbr) directamente a tu televisor.</p>
                    <form action="/upload" method="post" enctype="multipart/form-data" id="uploadForm">
                        <div class="drop-zone" onclick="document.getElementById('fileInput').click()">
                            <p id="dropText">📁 Haz clic aquí para seleccionar tu archivo EPUB / PDF / CBZ</p>
                            <input type="file" name="file" id="fileInput" accept=".epub,.pdf,.cbz,.cbr" onchange="fileSelected()">
                        </div>
                        <button type="submit" class="btn">🚀 Enviar a BookSpread</button>
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
        val response = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n" +
                "\r\n"
        output.write(response.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private suspend fun handleFileUpload(
        input: java.io.InputStream,
        output: OutputStream,
        contentLength: Int,
        contentType: String
    ) {
        val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.tmp")
        try {
            FileOutputStream(tempFile).use { out ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = 0
                while (totalRead < contentLength || contentLength == 0) {
                    val toRead = if (contentLength > 0) minOf(buffer.size, contentLength - totalRead) else buffer.size
                    if (toRead <= 0) break
                    bytesRead = input.read(buffer, 0, toRead)
                    if (bytesRead == -1) break
                    out.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                uploadedFilesCount++
                processUploadedFile(tempFile, contentType)

                val successHtml = """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"><title>¡Enviado!</title>
                    <style>
                        body { background: #121216; color: #fff; font-family: sans-serif; text-align: center; padding: 40px; }
                        .box { background: #181513; border-radius: 16px; padding: 32px; max-width: 400px; margin: auto; border: 1px solid #C5A059; }
                        h2 { color: #C5A059; }
                        p { color: #A8A29E; }
                        a { color: #C5A059; font-weight: bold; text-decoration: none; }
                    </style>
                    </head>
                    <body>
                        <div class="box">
                            <h2>✅ ¡Libro Enviado con Éxito!</h2>
                            <p>El archivo ya está disponible en tu biblioteca de BookSpread.</p>
                            <br>
                            <a href="/">+ Subir otro libro</a>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val bytes = successHtml.toByteArray(Charsets.UTF_8)
                val response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${bytes.size}\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
                output.write(response.toByteArray(Charsets.UTF_8))
                output.write(bytes)
                output.flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file", e)
            val errResponse = "HTTP/1.1 500 Internal Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
            output.write(errResponse.toByteArray(Charsets.UTF_8))
            output.flush()
        }
    }

    private suspend fun processUploadedFile(tempFile: File, contentType: String) {
        withContext(Dispatchers.IO) {
            try {
                val isPdf = PdfParser.isPdfFile(tempFile) || contentType.contains("pdf")
                val isComic = !isPdf && (ComicParser.isComicFile(tempFile) || contentType.contains("zip") || tempFile.name.endsWith(".cbz"))
                val bookId = "local_wifi_${System.currentTimeMillis()}"

                var title = "Libro Importado WiFi"
                var author = "Importado por WiFi"
                var summary = "Libro importado directamente desde tu dispositivo mediante WiFi."
                var coverPath: String? = null

                when {
                    isPdf -> {
                        title = tempFile.nameWithoutExtension.replace('_', ' ')
                        author = "Documento PDF"
                        summary = "Documento PDF importado directamente por WiFi."

                        // Extraer miniatura de portada
                        val coverBmp = PdfParser.extractCover(tempFile, 400, 600)
                        if (coverBmp != null) {
                            val coverFile = File(context.filesDir, "cover_$bookId.png")
                            FileOutputStream(coverFile).use { out ->
                                coverBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                            }
                            coverPath = coverFile.absolutePath
                            coverBmp.recycle()
                        }
                    }
                    isComic -> {
                        title = tempFile.nameWithoutExtension
                        author = "Cómic"
                        summary = "Cómic importado directamente por WiFi."
                    }
                    else -> {
                        val parsed = EpubParser.parseEpubToBook(tempFile, tempFile.nameWithoutExtension)
                        if (parsed.title.isNotBlank()) title = parsed.title
                        val extractedDesc = EpubParser.extractDescription(tempFile)
                        if (!extractedDesc.isNullOrBlank()) summary = extractedDesc
                    }
                }

                val ext = when {
                    isPdf -> "pdf"
                    isComic -> "cbz"
                    else -> "epub"
                }

                val destFile = File(context.filesDir, "book_$bookId.$ext")
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()

                val category = when {
                    isPdf -> "PDF"
                    isComic -> "Cómic"
                    else -> "WiFi"
                }

                val newBook = Book(
                    id = bookId,
                    title = title,
                    author = author,
                    coverUrl = coverPath,
                    epubUrl = destFile.absolutePath,
                    summary = summary,
                    category = category,
                    tags = listOf(category, "Local")
                )

                val current = repository.getCachedBooks().toMutableList()
                current.add(0, newBook)
                repository.saveCachedBooks(current)
                Log.d(TAG, "Successfully processed uploaded book: $title ($category)")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing uploaded file", e)
            }
        }
    }
}
