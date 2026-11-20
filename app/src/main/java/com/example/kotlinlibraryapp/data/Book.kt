package com.example.kotlinlibraryapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Room entity: one row per book. `id = 0` means "not inserted yet" (Room generates the id). */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val author: String = "Unknown author",
    val genre: String = "General",
    val coverUrl: String? = null,
    val isAvailable: Boolean = true,
    val dueDateMillis: Long? = null,
    val rating: Int = 0,
    val isFavorite: Boolean = false,
    val notes: String = "",
    /** How many times the current loan has been renewed. Reset on return. */
    val renewCount: Int = 0
) {
    fun isOverdue(now: Long): Boolean =
        !isAvailable && (dueDateMillis?.let { it < now } == true)

    /** Whole days until due (rounded up). Negative when overdue. Null when not on loan. */
    fun daysUntilDue(now: Long): Int? {
        val due = dueDateMillis ?: return null
        val diff = due - now
        return if (diff >= 0) {
            ((diff + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
        } else {
            -(((-diff) + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
        }
    }
}

enum class BookFilter(val label: String) {
    All("All"),
    Available("Available"),
    Borrowed("Borrowed"),
    Favorites("Favorites")
}

enum class SortOption(val label: String) {
    Title("Title"),
    Author("Author"),
    Rating("Rating")
}

object Genres {
    val all = listOf(
        "Fantasy", "Science Fiction", "Classic", "Mystery",
        "Biography", "Non-fiction", "Romance", "General"
    )
}
