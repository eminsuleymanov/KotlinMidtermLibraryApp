package com.example.kotlinlibraryapp.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for books. In-memory for now; swap the body for Room/DataStore
 * later without touching any UI code.
 */
class BookRepository {

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private var nextId = 1

    suspend fun loadInitialBooks(now: Long) {
        delay(600) // simulates disk / network latency
        if (_books.value.isNotEmpty()) return

        val seed = listOf(
            Book(
                id = 1, title = "The Hobbit", author = "J.R.R. Tolkien", genre = "Fantasy",
                coverUrl = "https://covers.openlibrary.org/b/isbn/9780547928227-M.jpg",
                rating = 5, isFavorite = true
            ),
            Book(
                id = 2, title = "Dune", author = "Frank Herbert", genre = "Science Fiction",
                coverUrl = "https://covers.openlibrary.org/b/isbn/9780441172719-M.jpg",
                isAvailable = false, dueDateMillis = now + 5 * DAY_MILLIS
            ),
            Book(
                id = 3, title = "Pride and Prejudice", author = "Jane Austen", genre = "Classic",
                coverUrl = "https://covers.openlibrary.org/b/isbn/9780141439518-M.jpg",
                rating = 4
            ),
            Book(
                id = 4, title = "The Martian", author = "Andy Weir", genre = "Science Fiction",
                coverUrl = "https://covers.openlibrary.org/b/isbn/9780553418026-M.jpg",
                isAvailable = false, dueDateMillis = now - 3 * DAY_MILLIS
            ),
            Book(id = 5, title = "Murder on the Orient Express", author = "Agatha Christie", genre = "Mystery"),
            Book(id = 6, title = "Educated", author = "Tara Westover", genre = "Biography")
        )
        nextId = seed.size + 1
        _books.value = seed
    }

    fun find(id: Int): Book? = _books.value.firstOrNull { it.id == id }

    fun add(title: String, author: String, genre: String, coverUrl: String?): Book {
        val book = Book(
            id = nextId++,
            title = title,
            author = author,
            genre = genre,
            coverUrl = coverUrl
        )
        _books.update { it + book }
        return book
    }

    fun update(id: Int, transform: (Book) -> Book) {
        _books.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    /** Removes the book and returns (previousIndex, book) so the caller can offer Undo. */
    fun remove(id: Int): Pair<Int, Book>? {
        val current = _books.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return null
        val removed = current[index]
        _books.value = current.toMutableList().apply { removeAt(index) }
        return index to removed
    }

    fun restore(index: Int, book: Book) {
        _books.update { list ->
            list.toMutableList().apply { add(index.coerceIn(0, size), book) }
        }
    }
}
