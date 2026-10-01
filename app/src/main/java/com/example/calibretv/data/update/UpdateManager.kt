package com.example.calibretv.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {

    private const val GITHUB_REPO = "vmatos001/BookSpread"
    private const val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"

    data class ReleaseInfo(
        val tagName: String,
        val title: String,
        val changelog: String,
        val apkUrl: String,
        val apkName: String,
        val apkSizeBytes: Long
    )

    sealed class CheckResult {
        data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : CheckResult()
        data class UpToDate(val currentVersion: String) : CheckResult()
        data class Error(val message: String) : CheckResult()
    }

    fun getCurrentVersion(context: Context): Pair<String, Long> {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo.versionName ?: "2.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            Pair(vName, vCode)
        } catch (_: Exception) {
            Pair("2.0", 4L)
        }
    }

    suspend fun checkForUpdate(context: Context): CheckResult = withContext(Dispatchers.IO) {
        try {
            val (currentVersion, _) = getCurrentVersion(context)
            val url = URL(RELEASES_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "BookSpread-AndroidTV")
            conn.connect()

            if (conn.responseCode !in 200..299) {
                return@withContext CheckResult.Error("No se pudo conectar con GitHub (${conn.responseCode})")
            }

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)

            val tagName = root.optString("tag_name", "")
            val title = root.optString("name", tagName)
            val changelog = root.optString("body", "Mejoras y correcciones generales.")

            val assets = root.optJSONArray("assets")
            var apkUrl = ""
            var apkName = "BookSpread-$tagName.apk"
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isBlank()) {
                return@withContext CheckResult.Error("La última versión no contiene un archivo APK adjunto.")
            }

            if (isNewerVersion(currentVersion, tagName)) {
                CheckResult.UpdateAvailable(
                    ReleaseInfo(
                        tagName = tagName,
                        title = title,
                        changelog = changelog,
                        apkUrl = apkUrl,
                        apkName = apkName,
                        apkSizeBytes = apkSize
                    ),
                    currentVersion = currentVersion
                )
            } else {
                CheckResult.UpToDate(currentVersion = currentVersion)
            }
        } catch (e: Exception) {
            CheckResult.Error(e.message ?: "Error al verificar actualizaciones.")
        }
    }

    fun isNewerVersion(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()

        val currParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val lateParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(currParts.size, lateParts.size)
        for (i in 0 until maxLen) {
            val c = currParts.getOrElse(i) { 0 }
            val l = lateParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.cacheDir, "update_${release.tagName.replace('/', '_')}.apk")
            if (destFile.exists()) destFile.delete()

            var currentUrl = release.apkUrl
            var conn: HttpURLConnection

            // Handle GitHub redirect (GitHub Releases redirect to AWS S3)
            var redirectCount = 0
            while (true) {
                val url = URL(currentUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "BookSpread-AndroidTV")
                conn.connect()

                val code = conn.responseCode
                if (code in 300..399) {
                    val redirectUrl = conn.getHeaderField("Location")
                    if (redirectUrl.isNullOrBlank() || ++redirectCount > 5) break
                    currentUrl = redirectUrl
                    conn.disconnect()
                } else {
                    break
                }
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSizeBytes
            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        val percent = if (totalBytes > 0) {
                            ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                        } else {
                            0
                        }
                        onProgress(percent, downloadedBytes, totalBytes)
                    }
                }
            }

            if (destFile.exists() && destFile.length() > 0) {
                destFile
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
