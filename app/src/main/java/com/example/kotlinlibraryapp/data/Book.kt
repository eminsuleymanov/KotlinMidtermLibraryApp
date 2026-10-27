package com.example.kotlinlibraryapp.data

const val DAY_MILLIS = 24L * 60 * 60 * 1000

data class Book(
    val id: Int,
    val title: String,
    val author: String = "Unknown author",
    val genre: String = "General",
    val coverUrl: String? = null,
    val isAvailable: Boolean = true,
    val dueDateMillis: Long? = null,
    val rating: Int = 0,
    val isFavorite: Boolean = false,
    val notes: String = ""
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
