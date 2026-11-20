package com.example.kotlinlibraryapp.domain

/** Outcome of a loan action, so the UI can show a clear message either way. */
data class LoanResult(val success: Boolean, val message: String) {
    companion object {
        fun ok(message: String) = LoanResult(true, message)
        fun rejected(message: String) = LoanResult(false, message)
    }
}

/** The borrowing rules. Plain Kotlin, no Android dependencies. */
object LoanPolicy {
    const val LOAN_DAYS = 14L
    const val RENEW_DAYS = 7L
    const val MAX_ACTIVE_LOANS = 3
    const val MAX_RENEWALS = 2
}
