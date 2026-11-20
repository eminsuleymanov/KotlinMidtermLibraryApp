package com.example.kotlinlibraryapp.data

import androidx.room.withTransaction
import com.example.kotlinlibraryapp.data.local.LibraryDatabase
import com.example.kotlinlibraryapp.domain.LoanPolicy
import com.example.kotlinlibraryapp.domain.LoanResult
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for books and activity history, backed by Room.
 *
 * Every multi-step change (for example "borrow a book" = update the book + write a history
 * entry) runs inside one database transaction, so the two tables can never disagree.
 */
class BookRepository(private val database: LibraryDatabase) {

    private val bookDao = database.bookDao()
    private val eventDao = database.loanEventDao()

    val books: Flow<List<Book>> = bookDao.observeAll()
    val events: Flow<List<LoanEvent>> = eventDao.observeAll()

    suspend fun getBook(id: Int): Book? = bookDao.getById(id)

    suspend fun isDuplicate(title: String, author: String, excludeId: Int = 0): Boolean =
        bookDao.countDuplicates(title, author, excludeId) > 0

    // ---- Create / update / delete -----------------------------------------------------------

    suspend fun addBook(title: String, author: String, genre: String, coverUrl: String?) {
        database.withTransaction {
            val id = bookDao.insert(
                Book(title = title, author = author, genre = genre, coverUrl = coverUrl)
            ).toInt()
            log(id, title, EventType.ADDED)
        }
    }

    /** Returns false when the book no longer exists. */
    suspend fun updateDetails(
        id: Int,
        title: String,
        author: String,
        genre: String,
        coverUrl: String?
    ): Boolean = database.withTransaction {
        val existing = bookDao.getById(id) ?: return@withTransaction false
        bookDao.update(
            existing.copy(title = title, author = author, genre = genre, coverUrl = coverUrl)
        )
        log(id, title, EventType.EDITED, "Updated details")
        true
    }

    suspend fun deleteBook(book: Book) {
        database.withTransaction {
            bookDao.delete(book)
            log(book.id, book.title, EventType.DELETED)
        }
    }

    /** Undo for delete: re-inserts the book with its original id and loan state. */
    suspend fun restoreBook(book: Book) {
        database.withTransaction {
            bookDao.insert(book)
            log(book.id, book.title, EventType.RESTORED)
        }
    }

    suspend fun toggleFavorite(id: Int) = bookDao.toggleFavorite(id)
    suspend fun setRating(id: Int, rating: Int) = bookDao.setRating(id, rating.coerceIn(0, 5))
    suspend fun setNotes(id: Int, notes: String) = bookDao.setNotes(id, notes.trim())

    // ---- Loan workflow: Available -> Borrowed -> (Renewed up to 2x) -> Returned ------------

    suspend fun borrow(bookId: Int, now: Long = System.currentTimeMillis()): LoanResult =
        database.withTransaction<LoanResult> {
            val book = bookDao.getById(bookId)
                ?: return@withTransaction LoanResult.rejected("That book is no longer in the library")
            if (!book.isAvailable) {
                return@withTransaction LoanResult.rejected("'${book.title}' is already borrowed")
            }
            if (bookDao.countOnLoan() >= LoanPolicy.MAX_ACTIVE_LOANS) {
                return@withTransaction LoanResult.rejected(
                    "Loan limit reached: you can have ${LoanPolicy.MAX_ACTIVE_LOANS} books out at once. Return one first."
                )
            }
            bookDao.update(
                book.copy(
                    isAvailable = false,
                    dueDateMillis = now + LoanPolicy.LOAN_DAYS * DAY_MILLIS,
                    renewCount = 0
                )
            )
            log(book.id, book.title, EventType.BORROWED, "${LoanPolicy.LOAN_DAYS}-day loan", now = now)
            LoanResult.ok("Borrowed '${book.title}' for ${LoanPolicy.LOAN_DAYS} days")
        }

    suspend fun returnBook(bookId: Int, now: Long = System.currentTimeMillis()): LoanResult =
        database.withTransaction<LoanResult> {
            val book = bookDao.getById(bookId)
                ?: return@withTransaction LoanResult.rejected("That book is no longer in the library")
            if (book.isAvailable) {
                return@withTransaction LoanResult.rejected("'${book.title}' is not on loan")
            }
            val daysLate = book.daysUntilDue(now)?.let { if (it < 0) -it else 0 } ?: 0
            val onTime = daysLate == 0
            bookDao.update(book.copy(isAvailable = true, dueDateMillis = null, renewCount = 0))
            log(
                book.id, book.title, EventType.RETURNED,
                detail = if (onTime) "Returned on time"
                else "Returned $daysLate day${if (daysLate == 1) "" else "s"} late",
                onTime = onTime,
                now = now
            )
            LoanResult.ok("Returned '${book.title}'")
        }

    suspend fun renew(bookId: Int, now: Long = System.currentTimeMillis()): LoanResult =
        database.withTransaction<LoanResult> {
            val book = bookDao.getById(bookId)
                ?: return@withTransaction LoanResult.rejected("That book is no longer in the library")
            val due = book.dueDateMillis
            if (book.isAvailable || due == null) {
                return@withTransaction LoanResult.rejected("'${book.title}' is not on loan")
            }
            if (book.renewCount >= LoanPolicy.MAX_RENEWALS) {
                return@withTransaction LoanResult.rejected(
                    "'${book.title}' was already renewed ${LoanPolicy.MAX_RENEWALS} times. Please return it."
                )
            }
            val newDue = maxOf(due, now) + LoanPolicy.RENEW_DAYS * DAY_MILLIS
            val used = book.renewCount + 1
            bookDao.update(book.copy(dueDateMillis = newDue, renewCount = used))
            log(
                book.id, book.title, EventType.RENEWED,
                "Renewal $used of ${LoanPolicy.MAX_RENEWALS}, +${LoanPolicy.RENEW_DAYS} days",
                now = now
            )
            LoanResult.ok("Renewed '${book.title}' for ${LoanPolicy.RENEW_DAYS} more days")
        }

    // ---- History ----------------------------------------------------------------------------

    suspend fun clearHistory() = eventDao.clear()

    private suspend fun log(
        bookId: Int,
        title: String,
        type: EventType,
        detail: String? = null,
        onTime: Boolean = true,
        now: Long = System.currentTimeMillis()
    ) {
        eventDao.insert(
            LoanEvent(
                bookId = bookId, bookTitle = title, type = type,
                timestamp = now, detail = detail, onTime = onTime
            )
        )
    }
}
