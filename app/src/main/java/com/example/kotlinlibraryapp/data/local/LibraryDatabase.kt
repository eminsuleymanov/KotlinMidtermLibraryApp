package com.example.kotlinlibraryapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kotlinlibraryapp.data.Book
import com.example.kotlinlibraryapp.data.DAY_MILLIS
import com.example.kotlinlibraryapp.data.EventType
import com.example.kotlinlibraryapp.data.LoanEvent

@Database(entities = [Book::class, LoanEvent::class], version = 1, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun loanEventDao(): LoanEventDao

    companion object {
        @Volatile
        private var instance: LibraryDatabase? = null

        fun getInstance(context: Context): LibraryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LibraryDatabase::class.java,
                    "reading_room.db"
                )
                    .addCallback(SeedCallback)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * Runs exactly once, when the database file is first created, so the sample books are never
 * re-inserted after the user deletes them.
 */
private object SeedCallback : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        val hobbit = db.seedBook(
            "The Hobbit", "J.R.R. Tolkien", "Fantasy",
            "https://covers.openlibrary.org/b/isbn/9780547928227-M.jpg",
            rating = 5, favorite = true
        )
        val dune = db.seedBook(
            "Dune", "Frank Herbert", "Science Fiction",
            "https://covers.openlibrary.org/b/isbn/9780441172719-M.jpg",
            available = false, due = now + 5 * DAY_MILLIS
        )
        val pride = db.seedBook(
            "Pride and Prejudice", "Jane Austen", "Classic",
            "https://covers.openlibrary.org/b/isbn/9780141439518-M.jpg",
            rating = 4
        )
        val martian = db.seedBook(
            "The Martian", "Andy Weir", "Science Fiction",
            "https://covers.openlibrary.org/b/isbn/9780553418026-M.jpg",
            available = false, due = now - 3 * DAY_MILLIS
        )
        val orient = db.seedBook("Murder on the Orient Express", "Agatha Christie", "Mystery", null)
        val educated = db.seedBook("Educated", "Tara Westover", "Biography", null)

        // A little history so the History tab is not empty on first launch.
        db.seedEvent(hobbit, "The Hobbit", EventType.ADDED, now - 40 * DAY_MILLIS)
        db.seedEvent(dune, "Dune", EventType.ADDED, now - 40 * DAY_MILLIS)
        db.seedEvent(pride, "Pride and Prejudice", EventType.ADDED, now - 40 * DAY_MILLIS)
        db.seedEvent(martian, "The Martian", EventType.ADDED, now - 40 * DAY_MILLIS)
        db.seedEvent(orient, "Murder on the Orient Express", EventType.ADDED, now - 40 * DAY_MILLIS)
        db.seedEvent(educated, "Educated", EventType.ADDED, now - 40 * DAY_MILLIS)
        db.seedEvent(hobbit, "The Hobbit", EventType.BORROWED, now - 30 * DAY_MILLIS, "14-day loan")
        db.seedEvent(hobbit, "The Hobbit", EventType.RETURNED, now - 20 * DAY_MILLIS, "Returned on time")
        db.seedEvent(pride, "Pride and Prejudice", EventType.BORROWED, now - 25 * DAY_MILLIS, "14-day loan")
        db.seedEvent(
            pride, "Pride and Prejudice", EventType.RETURNED, now - 8 * DAY_MILLIS,
            "Returned 3 days late", onTime = false
        )
        db.seedEvent(martian, "The Martian", EventType.BORROWED, now - 17 * DAY_MILLIS, "14-day loan")
        db.seedEvent(dune, "Dune", EventType.BORROWED, now - 9 * DAY_MILLIS, "14-day loan")
    }

    private fun SupportSQLiteDatabase.seedBook(
        title: String,
        author: String,
        genre: String,
        coverUrl: String?,
        available: Boolean = true,
        due: Long? = null,
        rating: Int = 0,
        favorite: Boolean = false
    ): Long {
        val values = ContentValues().apply {
            put("title", title)
            put("author", author)
            put("genre", genre)
            if (coverUrl != null) put("coverUrl", coverUrl) else putNull("coverUrl")
            put("isAvailable", if (available) 1 else 0)
            if (due != null) put("dueDateMillis", due) else putNull("dueDateMillis")
            put("rating", rating)
            put("isFavorite", if (favorite) 1 else 0)
            put("notes", "")
            put("renewCount", 0)
        }
        return insert("books", SQLiteDatabase.CONFLICT_NONE, values)
    }

    private fun SupportSQLiteDatabase.seedEvent(
        bookId: Long,
        title: String,
        type: EventType,
        timestamp: Long,
        detail: String? = null,
        onTime: Boolean = true
    ) {
        val values = ContentValues().apply {
            put("bookId", bookId)
            put("bookTitle", title)
            put("type", type.name)
            put("timestamp", timestamp)
            if (detail != null) put("detail", detail) else putNull("detail")
            put("onTime", if (onTime) 1 else 0)
        }
        insert("loan_events", SQLiteDatabase.CONFLICT_NONE, values)
    }
}
