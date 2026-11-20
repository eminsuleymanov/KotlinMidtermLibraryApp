package com.example.kotlinlibraryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.kotlinlibraryapp.data.Book
import com.example.kotlinlibraryapp.data.Genres
import com.example.kotlinlibraryapp.domain.BookValidator
import com.example.kotlinlibraryapp.ui.components.EmptyState
import com.example.kotlinlibraryapp.ui.components.ScreenTopBar

/**
 * One form for both creating and editing a book.
 *  - Create: [isEdit] = false, [initialBook] = null.
 *  - Edit:   [isEdit] = true; the fields start from [initialBook].
 */
@Composable
fun BookFormScreen(
    isEdit: Boolean,
    initialBook: Book?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSave: (title: String, author: String, genre: String, coverUrl: String) -> Unit
) {
    val screenTitle = if (isEdit) "Edit book" else "Add a book"

    // Editing a book that is not (yet) available: still loading, or it was deleted.
    if (isEdit && initialBook == null) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { ScreenTopBar(title = screenTitle, onBack = onBack) }
        ) { innerPadding ->
            if (isLoading) {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                EmptyState("This book is no longer in the library.", Modifier.padding(innerPadding))
            }
        }
        return
    }

    // Keyed by book id so each book starts from its own saved values; rememberSaveable keeps
    // the typed text across rotation and process death.
    val key = initialBook?.id ?: -1
    var title by rememberSaveable(key) { mutableStateOf(initialBook?.title ?: "") }
    var author by rememberSaveable(key) {
        mutableStateOf(initialBook?.author?.takeIf { it != "Unknown author" } ?: "")
    }
    var genre by rememberSaveable(key) { mutableStateOf(initialBook?.genre ?: "General") }
    var coverUrl by rememberSaveable(key) { mutableStateOf(initialBook?.coverUrl ?: "") }
    var submitted by rememberSaveable(key) { mutableStateOf(false) }

    val titleError = BookValidator.titleError(title)
    val authorError = BookValidator.authorError(author)
    val coverError = BookValidator.coverUrlError(coverUrl)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(title = screenTitle, onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                isError = submitted && titleError != null,
                supportingText = { if (submitted && titleError != null) Text(titleError) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author (optional)") },
                isError = authorError != null,
                supportingText = { if (authorError != null) Text(authorError) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Genre", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Genres.all, key = { it }) { option ->
                    FilterChip(
                        selected = genre == option,
                        onClick = { genre = option },
                        label = { Text(option) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            OutlinedTextField(
                value = coverUrl,
                onValueChange = { coverUrl = it },
                label = { Text("Cover image link (optional)") },
                isError = coverError != null,
                supportingText = { if (coverError != null) Text(coverError) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    submitted = true
                    if (titleError == null && authorError == null && coverError == null) {
                        onSave(title, author, genre, coverUrl)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEdit) "Save changes" else "Save book") }
        }
    }
}
