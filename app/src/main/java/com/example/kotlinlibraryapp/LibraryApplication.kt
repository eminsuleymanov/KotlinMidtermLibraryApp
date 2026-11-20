package com.example.kotlinlibraryapp

import android.app.Application
import com.example.kotlinlibraryapp.data.BookRepository
import com.example.kotlinlibraryapp.data.local.LibraryDatabase

/** Holds the app-wide singletons (manual dependency injection). */
class LibraryApplication : Application() {
    val repository: BookRepository by lazy {
        BookRepository(LibraryDatabase.getInstance(this))
    }
}
