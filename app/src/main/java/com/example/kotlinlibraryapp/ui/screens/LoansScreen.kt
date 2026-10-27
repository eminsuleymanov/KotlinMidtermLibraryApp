package com.example.kotlinlibraryapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlinlibraryapp.data.Book
import com.example.kotlinlibraryapp.ui.components.BookCover
import com.example.kotlinlibraryapp.ui.components.EmptyState
import com.example.kotlinlibraryapp.ui.components.ScreenTopBar
import com.example.kotlinlibraryapp.ui.components.StatusChip
import com.example.kotlinlibraryapp.ui.components.dueLabel
import com.example.kotlinlibraryapp.ui.components.formatDate

@Composable
fun LoansScreen(
    loans: List<Book>,
    overdueCount: Int,
    now: Long,
    onBookClick: (Int) -> Unit,
    onReturn: (Int) -> Unit,
    onRenew: (Int) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(title = "My loans") }
    ) { innerPadding ->
        if (loans.isEmpty()) {
            EmptyState(
                message = "Nothing borrowed right now.\nPick something from the shelf.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (overdueCount > 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (overdueCount == 1) "1 book is overdue. Please return or renew it."
                                else "$overdueCount books are overdue. Please return or renew them.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                items(loans, key = { it.id }) { book ->
                    LoanCard(
                        book = book,
                        now = now,
                        onClick = { onBookClick(book.id) },
                        onReturn = { onReturn(book.id) },
                        onRenew = { onRenew(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    book: Book,
    now: Long,
    onClick: () -> Unit,
    onReturn: () -> Unit,
    onRenew: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                BookCover(book, Modifier.size(width = 56.dp, height = 80.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusChip(book, now)
                    val due = book.dueDateMillis
                    if (due != null) {
                        Text(
                            text = "${book.dueLabel(now)} (${formatDate(due)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (book.isOverdue(now)) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRenew) { Text("Renew +7 days") }
                Button(onClick = onReturn) { Text("Return") }
            }
        }
    }
}
