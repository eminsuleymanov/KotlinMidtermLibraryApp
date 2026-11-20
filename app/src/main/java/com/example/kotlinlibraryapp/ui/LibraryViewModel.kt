package com.example.kotlinlibraryapp.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.kotlinlibraryapp.LibraryApplication
import com.example.kotlinlibraryapp.data.Book
import com.example.kotlinlibraryapp.data.BookFilter
import com.example.kotlinlibraryapp.data.BookRepository
import com.example.kotlinlibraryapp.data.EventType
import com.example.kotlinlibraryapp.data.HistoryFilter
import com.example.kotlinlibraryapp.data.LoanEvent
import com.example.kotlinlibraryapp.data.SortOption
import com.example.kotlinlibraryapp.domain.BookValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
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

/** Numbers derived from the saved loan history. */
data class HistoryStats(
    val totalLoans: Int = 0,
    val totalReturns: Int = 0,
    /** Percent of returns that were on or before the due date; null until something is returned. */
    val onTimePercent: Int? = null,
    val mostBorrowed: List<Pair<String, Int>> = emptyList()
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
    val history: List<LoanEvent> = emptyList(),
    val visibleHistory: List<LoanEvent> = emptyList(),
    val historyFilter: HistoryFilter = HistoryFilter.All,
    val historyStats: HistoryStats = HistoryStats(),
    val now: Long = 0L
)

/** One-shot UI events (snackbars). Kept out of UiState so they are not replayed on rotation. */
sealed interface LibraryEvent {
    data class Message(val text: String) : LibraryEvent
    data class BookDeleted(val title: String) : LibraryEvent
}

class LibraryViewModel(
    private val repository: BookRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var lastDeleted: Book? = null

    private val _events = Channel<LibraryEvent>(Channel.BUFFERED)
    val events: Flow<LibraryEvent> = _events.receiveAsFlow()

    // Search / filter / sort live in SavedStateHandle so they also survive process death.
    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val filterName = savedStateHandle.getStateFlow(KEY_FILTER, BookFilter.All.name)
    private val sortName = savedStateHandle.getStateFlow(KEY_SORT, SortOption.Title.name)
    private val historyFilterName =
        savedStateHandle.getStateFlow(KEY_HISTORY_FILTER, HistoryFilter.All.name)

    private data class Inputs(
        val query: String,
        val filter: BookFilter,
        val sort: SortOption,
        val historyFilter: HistoryFilter
    )

    private val inputs: Flow<Inputs> = combine(
        query, filterName, sortName, historyFilterName
    ) { q, f, s, h ->
        Inputs(q, BookFilter.valueOf(f), SortOption.valueOf(s), HistoryFilter.valueOf(h))
    }

    /**
     * Room's Flows re-emit on every table change, so any screen showing this state updates
     * automatically after a create, edit, delete, borrow, return or renew.
     */
    val uiState: StateFlow<LibraryUiState> = combine(
        repository.books, repository.events, inputs
    ) { books, events, input ->
        buildUiState(books, events, input)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    // ---- UI intents -------------------------------------------------------------------------

    fun onQueryChange(value: String) { savedStateHandle[KEY_QUERY] = value }
    fun onFilterChange(filter: BookFilter) { savedStateHandle[KEY_FILTER] = filter.name }
    fun onSortChange(sort: SortOption) { savedStateHandle[KEY_SORT] = sort.name }
    fun onHistoryFilterChange(filter: HistoryFilter) {
        savedStateHandle[KEY_HISTORY_FILTER] = filter.name
    }

    /** Borrows an available book or returns a borrowed one. */
    fun toggleBorrow(bookId: Int) {
        viewModelScope.launch {
            val book = repository.getBook(bookId) ?: return@launch
            val result = if (book.isAvailable) repository.borrow(bookId)
            else repository.returnBook(bookId)
            send(LibraryEvent.Message(result.message))
        }
    }

    fun renew(bookId: Int) {
        viewModelScope.launch {
            send(LibraryEvent.Message(repository.renew(bookId).message))
        }
    }

    fun toggleFavorite(bookId: Int) {
        viewModelScope.launch { repository.toggleFavorite(bookId) }
    }

    fun setRating(bookId: Int, rating: Int) {
        viewModelScope.launch { repository.setRating(bookId, rating) }
    }

    fun saveNotes(bookId: Int, notes: String) {
        viewModelScope.launch {
            repository.setNotes(bookId, notes)
            send(LibraryEvent.Message("Notes saved"))
        }
    }

    /**
     * Creates a book (bookId == null) or updates an existing one. [onSaved] runs only when the
     * data was actually written, so the form screen knows when it is safe to close.
     */
    fun saveBook(
        bookId: Int?,
        title: String,
        author: String,
        genre: String,
        coverUrl: String,
        onSaved: () -> Unit
    ) {
        val cleanTitle = title.trim()
        val cleanAuthor = author.trim().ifEmpty { "Unknown author" }
        val cleanCover = coverUrl.trim().takeIf { it.isNotEmpty() }
        if (BookValidator.titleError(cleanTitle) != null ||
            BookValidator.authorError(cleanAuthor) != null ||
            BookValidator.coverUrlError(coverUrl) != null
        ) return

        viewModelScope.launch {
            if (repository.isDuplicate(cleanTitle, cleanAuthor, excludeId = bookId ?: 0)) {
                send(LibraryEvent.Message("'$cleanTitle' by $cleanAuthor is already in the library"))
                return@launch
            }
            if (bookId == null) {
                repository.addBook(cleanTitle, cleanAuthor, genre, cleanCover)
                send(LibraryEvent.Message("Added '$cleanTitle'"))
            } else {
                val updated = repository.updateDetails(bookId, cleanTitle, cleanAuthor, genre, cleanCover)
                if (!updated) {
                    send(LibraryEvent.Message("That book no longer exists"))
                    return@launch
                }
                send(LibraryEvent.Message("Saved changes to '$cleanTitle'"))
            }
            onSaved()
        }
    }

    fun deleteBook(bookId: Int) {
        viewModelScope.launch {
            val book = repository.getBook(bookId) ?: return@launch
            repository.deleteBook(book)
            lastDeleted = book
            send(LibraryEvent.BookDeleted(book.title))
        }
    }

    fun undoDelete() {
        val book = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            repository.restoreBook(book)
            send(LibraryEvent.Message("Restored '${book.title}'"))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            send(LibraryEvent.Message("History cleared"))
        }
    }

    // ---- Helpers ----------------------------------------------------------------------------

    private fun send(event: LibraryEvent) {
        _events.trySend(event)
    }

    private fun buildUiState(
        books: List<Book>,
        events: List<LoanEvent>,
        input: Inputs
    ): LibraryUiState {
        val now = System.currentTimeMillis()
        val trimmed = input.query.trim()

        val matching = books.filter { book ->
            val matchesQuery = trimmed.isEmpty() ||
                    listOf(book.title, book.author, book.genre)
                        .any { it.contains(trimmed, ignoreCase = true) }
            val matchesFilter = when (input.filter) {
                BookFilter.All -> true
                BookFilter.Available -> book.isAvailable
                BookFilter.Borrowed -> !book.isAvailable
                BookFilter.Favorites -> book.isFavorite
            }
            matchesQuery && matchesFilter
        }

        val visible = when (input.sort) {
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

        val visibleHistory = when (input.historyFilter) {
            HistoryFilter.All -> events
            HistoryFilter.Loans -> events.filter { it.type.isLoanRelated }
            HistoryFilter.Library -> events.filter { !it.type.isLoanRelated }
        }

        val borrows = events.filter { it.type == EventType.BORROWED }
        val returns = events.filter { it.type == EventType.RETURNED }
        val historyStats = HistoryStats(
            totalLoans = borrows.size,
            totalReturns = returns.size,
            onTimePercent = if (returns.isEmpty()) null
            else returns.count { it.onTime } * 100 / returns.size,
            mostBorrowed = borrows.groupingBy { it.bookTitle }.eachCount()
                .toList().sortedByDescending { it.second }.take(3)
        )

        return LibraryUiState(
            isLoading = false,
            books = books,
            visibleBooks = visible,
            loans = loans,
            query = input.query,
            filter = input.filter,
            sort = input.sort,
            stats = stats,
            history = events,
            visibleHistory = visibleHistory,
            historyFilter = input.historyFilter,
            historyStats = historyStats,
            now = now
        )
    }

    companion object {
        private const val KEY_QUERY = "query"
        private const val KEY_FILTER = "filter"
        private const val KEY_SORT = "sort"
        private const val KEY_HISTORY_FILTER = "history_filter"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as LibraryApplication
                LibraryViewModel(app.repository, createSavedStateHandle())
            }
        }
    }
}
