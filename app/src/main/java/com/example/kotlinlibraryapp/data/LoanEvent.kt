package com.example.kotlinlibraryapp.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EventType(val label: String, val isLoanRelated: Boolean) {
    ADDED("Added", false),
    EDITED("Edited", false),
    BORROWED("Borrowed", true),
    RENEWED("Renewed", true),
    RETURNED("Returned", true),
    DELETED("Deleted", false),
    RESTORED("Restored", false)
}

enum class HistoryFilter(val label: String) {
    All("All"),
    Loans("Loans"),
    Library("Library")
}

/**
 * One row of the activity history. There is deliberately no foreign key to [Book]: the title is
 * copied in, so the history stays readable after a book has been deleted.
 */
@Entity(tableName = "loan_events", indices = [Index("bookId")])
data class LoanEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Int,
    val bookTitle: String,
    val type: EventType,
    val timestamp: Long,
    val detail: String? = null,
    /** Only meaningful for RETURNED events: was the book back before its due date? */
    val onTime: Boolean = true
)
