package com.example.kotlinlibraryapp.domain

/** Pure validation rules: no Android or Compose dependencies, so they are trivial to unit test. */
object BookValidator {
    const val MAX_TITLE_LENGTH = 80

    fun titleError(title: String): String? = when {
        title.isBlank() -> "Title is required"
        title.trim().length > MAX_TITLE_LENGTH -> "Keep the title under $MAX_TITLE_LENGTH characters"
        else -> null
    }

    fun coverUrlError(url: String): String? {
        val value = url.trim()
        return if (value.isEmpty() || value.startsWith("https://")) null
        else "Cover link must start with https://"
    }
}
