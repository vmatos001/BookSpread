package com.example.calibretv.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.calibretv.data.model.CurlSpeed
import com.example.calibretv.data.model.ReadingSettings
import com.example.calibretv.data.model.ReadingTheme
import com.example.calibretv.data.model.ServerConfig
import com.example.calibretv.data.model.UserProfile

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("calibre_tv_prefs", Context.MODE_PRIVATE)

    fun isSetupCompleted(): Boolean {
        return prefs.getBoolean("setup_completed_v2", false)
    }

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean("setup_completed_v2", completed).apply()
    }

    fun getServerConfig(): ServerConfig {
        return ServerConfig(
            serverUrl = prefs.getString("server_url", "") ?: "",
            username = prefs.getString("server_user", "") ?: "",
            password = prefs.getString("server_pass", "") ?: ""
        )
    }

    fun saveServerConfig(config: ServerConfig) {
        prefs.edit()
            .putString("server_url", config.serverUrl)
            .putString("server_user", config.username)
            .putString("server_pass", config.password)
            .apply()
    }

    fun getReadingSettings(): ReadingSettings {
        val themeName = prefs.getString("read_theme", ReadingTheme.PERGAMINO.name) ?: ReadingTheme.PERGAMINO.name
        val theme = try {
            ReadingTheme.valueOf(themeName)
        } catch (_: Exception) {
            ReadingTheme.PERGAMINO
        }

        val speedName = prefs.getString("curl_speed", CurlSpeed.APPLE_BOOKS_SMOOTH.name) ?: CurlSpeed.APPLE_BOOKS_SMOOTH.name
        val speed = try {
            CurlSpeed.valueOf(speedName)
        } catch (_: Exception) {
            CurlSpeed.APPLE_BOOKS_SMOOTH
        }

        val mirror = prefs.getBoolean("vertical_mirror", false)
        val rot = prefs.getBoolean("rotation_180", false)

        return ReadingSettings(
            verticalMirror = mirror,
            rotation180 = rot,
            ceilingMode = mirror || rot,
            curlSpeed = speed,
            theme = theme,
            fontSizeSp = prefs.getInt("font_size", 20),
            overscanPercent = prefs.getInt("overscan", 0)
        )
    }

    fun saveReadingSettings(settings: ReadingSettings) {
        prefs.edit()
            .putBoolean("vertical_mirror", settings.verticalMirror)
            .putBoolean("rotation_180", settings.rotation180)
            .putBoolean("ceiling_mode", settings.verticalMirror || settings.rotation180)
            .putString("curl_speed", settings.curlSpeed.name)
            .putString("read_theme", settings.theme.name)
            .putInt("font_size", settings.fontSizeSp)
            .putInt("overscan", settings.overscanPercent)
            .apply()
    }

    fun getProfiles(): List<UserProfile> {
        val jsonStr = prefs.getString("user_profiles_list", null)
        if (jsonStr.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            val arr = org.json.JSONArray(jsonStr)
            val list = mutableListOf<UserProfile>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    UserProfile(
                        id = o.optString("id", "user_${i + 1}"),
                        name = o.optString("name", "Usuario"),
                        avatarColorHex = o.optString("avatarColorHex", "#FFA000")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveProfiles(profiles: List<UserProfile>) {
        try {
            val arr = org.json.JSONArray()
            for (p in profiles) {
                val o = org.json.JSONObject()
                o.put("id", p.id)
                o.put("name", p.name)
                o.put("avatarColorHex", p.avatarColorHex)
                arr.put(o)
            }
            prefs.edit().putString("user_profiles_list", arr.toString()).apply()
        } catch (_: Exception) {}
    }

    fun createProfile(name: String, colorHex: String = "#FFA000"): UserProfile {
        val current = getProfiles().toMutableList()
        val newId = "user_${System.currentTimeMillis()}"
        val newProfile = UserProfile(newId, name.ifBlank { "Mi Perfil" }, colorHex)
        current.add(newProfile)
        saveProfiles(current)
        saveActiveProfile(newProfile)
        return newProfile
    }

    fun getActiveProfile(): UserProfile {
        val id = prefs.getString("active_profile_id", null)
        val name = prefs.getString("active_profile_name", null)
        val color = prefs.getString("active_profile_color", "#FFA000") ?: "#FFA000"
        if (id != null && name != null) {
            return UserProfile(id, name, color)
        }
        val first = getProfiles().firstOrNull()
        if (first != null) {
            return first
        }
        return UserProfile("user_default", "Mi Perfil", "#FFA000")
    }

    fun saveActiveProfile(profile: UserProfile) {
        prefs.edit()
            .putString("active_profile_id", profile.id)
            .putString("active_profile_name", profile.name)
            .putString("active_profile_color", profile.avatarColorHex)
            .apply()
    }

    fun getFavoriteBookIds(profileId: String): Set<String> {
        return prefs.getStringSet("fav_books_$profileId", emptySet()) ?: emptySet()
    }

    fun isFavorite(profileId: String, bookId: String): Boolean {
        val favs = getFavoriteBookIds(profileId)
        return favs.contains(bookId)
    }

    fun toggleFavorite(profileId: String, bookId: String): Boolean {
        val favs = getFavoriteBookIds(profileId).toMutableSet()
        val willBeFav = if (favs.contains(bookId)) {
            favs.remove(bookId)
            false
        } else {
            favs.add(bookId)
            true
        }
        prefs.edit().putStringSet("fav_books_$profileId", favs).apply()
        return willBeFav
    }

    fun getBookProgress(bookId: String): Int {
        val profileId = prefs.getString("active_profile_id", "user_1") ?: "user_1"
        return prefs.getInt("book_prog_${profileId}_$bookId", 0)
    }

    fun getBookProgressPercent(bookId: String): Int {
        val profileId = prefs.getString("active_profile_id", "user_1") ?: "user_1"
        return prefs.getInt("book_pct_${profileId}_$bookId", 0)
    }

    fun saveBookProgress(bookId: String, spreadIndex: Int, percent: Int = 0) {
        val profileId = prefs.getString("active_profile_id", "user_1") ?: "user_1"
        prefs.edit()
            .putInt("book_prog_${profileId}_$bookId", spreadIndex)
            .putInt("book_pct_${profileId}_$bookId", percent)
            .apply()
    }

    fun getCachedBooks(): List<com.example.calibretv.data.model.Book> {
        val jsonStr = prefs.getString("cached_library_books", null) ?: return emptyList()
        return try {
            val array = org.json.JSONArray(jsonStr)
            val list = mutableListOf<com.example.calibretv.data.model.Book>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val tagsArray = obj.optJSONArray("tags")
                val tags = mutableListOf<String>()
                if (tagsArray != null) {
                    for (j in 0 until tagsArray.length()) {
                        tags.add(tagsArray.getString(j))
                    }
                }
                list.add(
                    com.example.calibretv.data.model.Book(
                        id = obj.optString("id"),
                        title = obj.optString("title"),
                        author = obj.optString("author"),
                        coverUrl = obj.optString("coverUrl").takeIf { it.isNotBlank() },
                        epubUrl = obj.optString("epubUrl").takeIf { it.isNotBlank() },
                        summary = obj.optString("summary"),
                        category = obj.optString("category", "General"),
                        tags = tags,
                        progressPercent = obj.optInt("progressPercent", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCachedBooks(books: List<com.example.calibretv.data.model.Book>) {
        try {
            val array = org.json.JSONArray()
            for (b in books) {
                val obj = org.json.JSONObject()
                obj.put("id", b.id)
                obj.put("title", b.title)
                obj.put("author", b.author)
                obj.put("coverUrl", b.coverUrl ?: "")
                obj.put("epubUrl", b.epubUrl ?: "")
                obj.put("summary", b.summary)
                obj.put("category", b.category)
                val tagsArr = org.json.JSONArray()
                b.tags.forEach { tagsArr.put(it) }
                obj.put("tags", tagsArr)
                obj.put("progressPercent", b.progressPercent)
                array.put(obj)
            }
            prefs.edit().putString("cached_library_books", array.toString()).apply()
        } catch (_: Exception) {}
    }

    fun getLastOpenedBook(): com.example.calibretv.data.model.Book? {
        val jsonStr = prefs.getString("last_opened_book", null) ?: return null
        return try {
            val obj = org.json.JSONObject(jsonStr)
            com.example.calibretv.data.model.Book(
                id = obj.optString("id"),
                title = obj.optString("title"),
                author = obj.optString("author"),
                coverUrl = obj.optString("coverUrl").takeIf { it.isNotBlank() },
                epubUrl = obj.optString("epubUrl").takeIf { it.isNotBlank() },
                summary = obj.optString("summary"),
                category = obj.optString("category", "General"),
                progressPercent = obj.optInt("progressPercent", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveLastOpenedBook(book: com.example.calibretv.data.model.Book) {
        try {
            val obj = org.json.JSONObject()
            obj.put("id", book.id)
            obj.put("title", book.title)
            obj.put("author", book.author)
            obj.put("coverUrl", book.coverUrl ?: "")
            obj.put("epubUrl", book.epubUrl ?: "")
            obj.put("summary", book.summary)
            obj.put("category", book.category)
            obj.put("progressPercent", book.progressPercent)
            prefs.edit().putString("last_opened_book", obj.toString()).apply()
        } catch (_: Exception) {}
    }
}
