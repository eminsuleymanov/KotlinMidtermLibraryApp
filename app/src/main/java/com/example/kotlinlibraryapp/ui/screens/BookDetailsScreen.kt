package com.example.kotlinlibraryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlinlibraryapp.data.Book
import com.example.kotlinlibraryapp.ui.components.BookCover
import com.example.kotlinlibraryapp.ui.components.EmptyState
import com.example.kotlinlibraryapp.ui.components.RatingBar
import com.example.kotlinlibraryapp.ui.components.ScreenTopBar
import com.example.kotlinlibraryapp.ui.components.StatusChip
import com.example.kotlinlibraryapp.ui.components.dueLabel
import com.example.kotlinlibraryapp.ui.components.formatDate

@Composable
fun BookDetailsScreen(
    book: Book?,
    now: Long,
    onBack: () -> Unit,
    onToggleBorrow: () -> Unit,
    onRenew: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onSaveNotes: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = "Details",
                onBack = onBack,
                actions = {
                    if (book != null) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (book.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = if (book.isFavorite) "Remove from favorites"
                                else "Add to favorites",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete book")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (book == null) {
            EmptyState("This book is no longer in the library.", Modifier.padding(innerPadding))
        } else {
            BookDetailsContent(
                book = book,
                now = now,
                onToggleBorrow = onToggleBorrow,
                onRenew = onRenew,
                onRatingChange = onRatingChange,
                onSaveNotes = onSaveNotes,
                modifier = Modifier.padding(innerPadding)
            )

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete this book?") },
                    text = { Text("'${book.title}' will be removed from the library. You can undo right after.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            onDelete()
                        }) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun BookDetailsContent(
    book: Book,
    now: Long,
    onToggleBorrow: () -> Unit,
    onRenew: () -> Unit,
    onRatingChange: (Int) -> Unit,
    onSaveNotes: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Draft text survives rotation and process death; keyed so it resets per book.
    var notes by rememberSaveable(book.id) { mutableStateOf(book.notes) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BookCover(book, Modifier.size(width = 120.dp, height = 168.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = book.genre,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(book, now)
                val due = book.dueDateMillis
                if (due != null) {
                    Text(
                        text = "${book.dueLabel(now)}\n${formatDate(due)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (book.isOverdue(now)) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onToggleBorrow) {
                Text(if (book.isAvailable) "Borrow" else "Return")
            }
            if (!book.isAvailable) {
                OutlinedButton(onClick = onRenew) { Text("Renew +7 days") }
            }
        }

        Column {
            Text("Your rating", style = MaterialTheme.typography.titleMedium)
            RatingBar(
                rating = book.rating,
                onRatingChange = { tapped ->
                    // Tapping the current rating clears it.
                    onRatingChange(if (tapped == book.rating) 0 else tapped)
                }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onSaveNotes(notes) },
                enabled = notes.trim() != book.notes
            ) { Text("Save notes") }
        }
    }
}
