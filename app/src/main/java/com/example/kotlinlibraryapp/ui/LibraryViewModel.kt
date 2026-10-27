package com.example.kotlinlibraryapp.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlinlibraryapp.data.Book
import com.example.kotlinlibraryapp.data.BookFilter
import com.example.kotlinlibraryapp.data.BookRepository
import com.example.kotlinlibraryapp.data.DAY_MILLIS
import com.example.kotlinlibraryapp.data.SortOption
import com.example.kotlinlibraryapp.domain.BookValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryStats(
    val total: Int = 0,
    val available: Int = 0,
    val borrowed: Int = 0,
    val overdue: Int = 0,
    val favorites: Int = 0,
    val averageRating: Float? = null,
    val genreCounts: List<Pair<String, Int>> = emptyList()
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val visibleBooks: List<Book> = emptyList(),
    val loans: List<Book> = emptyList(),
    val query: String = "",
    val filter: BookFilter = BookFilter.All,
    val sort: SortOption = SortOption.Title,
    val stats: LibraryStats = LibraryStats(),
    val now: Long = 0L
)

/** One-shot UI events (snackbars). Kept out of UiState so they are not replayed on rotation. */
sealed interface LibraryEvent {
    data class Message(val text: String) : LibraryEvent
    data class BookDeleted(val title: String) : LibraryEvent
}

class LibraryViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val repository = BookRepository()
    private val isLoading = MutableStateFlow(true)
    private var lastDeleted: Pair<Int, Book>? = null

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    // Search / filter / sort live in SavedStateHandle so they also survive process death.
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val filterName = savedStateHandle.getStateFlow(KEY_FILTER, BookFilter.All.name)
    private val sortName = savedStateHandle.getStateFlow(KEY_SORT, SortOption.Title.name)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.books, query, filterName, sortName, isLoading
    ) { books, q, f, s, loading ->
        buildUiState(books, q, BookFilter.valueOf(f), SortOption.valueOf(s), loading)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    init {
        viewModelScope.launch {
            repository.loadInitialBooks(System.currentTimeMillis())
            isLoading.value = false
        }
    }

    // ---- UI intents -------------------------------------------------------------------------

    fun onQueryChange(value: String) { savedStateHandle[KEY_QUERY] = value }
    fun onFilterChange(filter: BookFilter) { savedStateHandle[KEY_FILTER] = filter.name }
    fun onSortChange(sort: SortOption) { savedStateHandle[KEY_SORT] = sort.name }

    fun toggleBorrow(bookId: Int) {
        val book = repository.find(bookId) ?: return
        if (book.isAvailable) {
            repository.update(bookId) {
                it.copy(
                    isAvailable = false,
                    dueDateMillis = System.currentTimeMillis() + LOAN_DAYS * DAY_MILLIS
                )
            }
            send(LibraryEvent.Message("Borrowed '${book.title}' for $LOAN_DAYS days"))
        } else {
            repository.update(bookId) { it.copy(isAvailable = true, dueDateMillis = null) }
            send(LibraryEvent.Message("Returned '${book.title}'"))
        }
    }

    fun renew(bookId: Int) {
        val book = repository.find(bookId) ?: return
        val due = book.dueDateMillis ?: return
        val newDue = maxOf(due, System.currentTimeMillis()) + RENEW_DAYS * DAY_MILLIS
        repository.update(bookId) { it.copy(dueDateMillis = newDue) }
        send(LibraryEvent.Message("Renewed '${book.title}' for $RENEW_DAYS more days"))
    }

    fun toggleFavorite(bookId: Int) =
        repository.update(bookId) { it.copy(isFavorite = !it.isFavorite) }

    fun setRating(bookId: Int, rating: Int) =
        repository.update(bookId) { it.copy(rating = rating.coerceIn(0, 5)) }

    fun saveNotes(bookId: Int, notes: String) {
        repository.update(bookId) { it.copy(notes = notes.trim()) }
        send(LibraryEvent.Message("Notes saved"))
    }

    /** Returns true when the book was added, so the caller can navigate back. */
    fun addBook(title: String, author: String, genre: String, coverUrl: String): Boolean {
        val cleanTitle = title.trim()
        val cleanAuthor = author.trim().ifEmpty { "Unknown author" }
        if (BookValidator.titleError(cleanTitle) != null ||
            BookValidator.coverUrlError(coverUrl) != null
        ) return false

        val duplicate = repository.books.value.any {
            it.title.equals(cleanTitle, ignoreCase = true) &&
                    it.author.equals(cleanAuthor, ignoreCase = true)
        }
        if (duplicate) {
            send(LibraryEvent.Message("'$cleanTitle' by $cleanAuthor is already in the library"))
            return false
        }

        repository.add(
            title = cleanTitle,
            author = cleanAuthor,
            genre = genre,
            coverUrl = coverUrl.trim().takeIf { it.isNotEmpty() }
        )
        send(LibraryEvent.Message("Added '$cleanTitle'"))
        return true
    }

    fun deleteBook(bookId: Int) {
        repository.remove(bookId)?.let { removed ->
            lastDeleted = removed
            send(LibraryEvent.BookDeleted(removed.second.title))
        }
    }

    fun undoDelete() {
        lastDeleted?.let { (index, book) ->
            repository.restore(index, book)
            lastDeleted = null
        }
    }

    // ---- Helpers ----------------------------------------------------------------------------

    private fun send(event: LibraryEvent) {
        _events.trySend(event)
    }

    private fun buildUiState(
        books: List<Book>,
        query: String,
        filter: BookFilter,
        sort: SortOption,
        loading: Boolean
    ): LibraryUiState {
        val now = System.currentTimeMillis()
        val trimmed = query.trim()

        val matching = books.filter { book ->
            val matchesQuery = trimmed.isEmpty() ||
                    listOf(book.title, book.author, book.genre)
                        .any { it.contains(trimmed, ignoreCase = true) }
            val matchesFilter = when (filter) {
                BookFilter.All -> true
                BookFilter.Available -> book.isAvailable
                BookFilter.Borrowed -> !book.isAvailable
                BookFilter.Favorites -> book.isFavorite
            }
            matchesQuery && matchesFilter
        }

        val visible = when (sort) {
            SortOption.Title -> matching.sortedBy { it.title.lowercase() }
            SortOption.Author -> matching.sortedBy { it.author.lowercase() }
            SortOption.Rating -> matching.sortedWith(
                compareByDescending<Book> { it.rating }.thenBy { it.title.lowercase() }
            )
        }

        val loans = books.filter { !it.isAvailable }
            .sortedBy { it.dueDateMillis ?: Long.MAX_VALUE }
        val rated = books.filter { it.rating > 0 }

        val stats = LibraryStats(
            total = books.size,
            available = books.size - loans.size,
            borrowed = loans.size,
            overdue = loans.count { it.isOverdue(now) },
            favorites = books.count { it.isFavorite },
            averageRating = if (rated.isEmpty()) null else rated.map { it.rating }.average().toFloat(),
            genreCounts = books.groupingBy { it.genre }.eachCount()
                .toList().sortedByDescending { it.second }
        )

        return LibraryUiState(
            isLoading = loading,
            books = books,
            visibleBooks = visible,
            loans = loans,
            query = query,
            filter = filter,
            sort = sort,
            stats = stats,
            now = now
        )
    }

    private companion object {
        const val KEY_QUERY = "query"
        const val KEY_FILTER = "filter"
        const val KEY_SORT = "sort"
        const val LOAN_DAYS = 14L
        const val RENEW_DAYS = 7L
    }
}
