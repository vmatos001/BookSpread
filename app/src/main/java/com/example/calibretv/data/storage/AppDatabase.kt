package com.example.calibretv.data.storage

import android.content.Context
import androidx.room.*

// ─── Entidades ───────────────────────────────────────────────────────────────

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val coverUrl: String?,
    val epubUrl: String?,
    val summary: String,
    val category: String,
    val tags: String, // JSON array: ["tag1","tag2"]
    val shelves: String = "[]", // JSON array: ["shelf1","shelf2"]
    val progressPercent: Int = 0,
    val lastReadSpread: Int = 0
)

@Entity(tableName = "reading_progress", primaryKeys = ["profileId", "bookId"])
data class ReadingProgressEntity(
    val profileId: String,
    val bookId: String,
    val spreadIndex: Int = 0,
    val percent: Int = 0,
    val lastReadAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites", primaryKeys = ["profileId", "bookId"])
data class FavoriteEntity(
    val profileId: String,
    val bookId: String
)

// ─── DAOs ─────────────────────────────────────────────────────────────────────

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY title ASC")
    suspend fun getAllBooks(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id IN (:ids)")
    suspend fun getBooksByIds(ids: List<String>): List<BookEntity>

    @Upsert
    suspend fun upsertBooks(books: List<BookEntity>)

    @Query("DELETE FROM books")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int
}

@Dao
interface ProgressDao {
    @Query("SELECT spreadIndex FROM reading_progress WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun getSpreadIndex(profileId: String, bookId: String): Int?

    @Query("SELECT percent FROM reading_progress WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun getPercent(profileId: String, bookId: String): Int?

    @Upsert
    suspend fun upsert(progress: ReadingProgressEntity)
}

@Dao
interface FavoriteDao {
    @Query("SELECT bookId FROM favorites WHERE profileId=:profileId")
    suspend fun getFavoriteIds(profileId: String): List<String>

    @Query("SELECT COUNT(*)>0 FROM favorites WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun isFavorite(profileId: String, bookId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE profileId=:profileId AND bookId=:bookId")
    suspend fun remove(profileId: String, bookId: String)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [BookEntity::class, ReadingProgressEntity::class, FavoriteEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun progressDao(): ProgressDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calibrotv.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
