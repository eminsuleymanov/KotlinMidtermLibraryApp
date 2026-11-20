package com.example.kotlinlibraryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.kotlinlibraryapp.data.HistoryFilter
import com.example.kotlinlibraryapp.data.LoanEvent
import com.example.kotlinlibraryapp.ui.HistoryStats
import com.example.kotlinlibraryapp.ui.components.EmptyState
import com.example.kotlinlibraryapp.ui.components.EventRow
import com.example.kotlinlibraryapp.ui.components.ScreenTopBar
import com.example.kotlinlibraryapp.ui.components.SectionHeader
import com.example.kotlinlibraryapp.ui.components.StatTile

/** Activity history plus loan statistics. All data comes from the Room-backed state. */
@Composable
fun HistoryScreen(
    events: List<LoanEvent>,
    totalEventCount: Int,
    stats: HistoryStats,
    filter: HistoryFilter,
    existingBookIds: Set<Int>,
    onFilterChange: (HistoryFilter) -> Unit,
    onBookClick: (Int) -> Unit,
    onClearHistory: () -> Unit
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar(
                title = "History",
                actions = {
                    if (totalEventCount > 0) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear history")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "summary") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Loans", stats.totalLoans.toString(), Modifier.weight(1f))
                    StatTile("Returns", stats.totalReturns.toString(), Modifier.weight(1f))
                    StatTile(
                        label = "On time",
                        value = stats.onTimePercent?.let { "$it%" } ?: "–",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (stats.mostBorrowed.isNotEmpty()) {
                item(key = "top") {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Most borrowed", style = MaterialTheme.typography.titleMedium)
                            stats.mostBorrowed.forEach { (title, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = if (count == 1) "1 loan" else "$count loans",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item(key = "header") {
                SectionHeader(title = "Activity", subtitle = "${events.size} entries")
            }

            item(key = "filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HistoryFilter.entries, key = { it.name }) { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { onFilterChange(option) },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (events.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        if (totalEventCount == 0) "No activity yet.\nBorrow, return or add a book and it shows up here."
                        else "Nothing to show for this filter."
                    )
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventRow(
                        event = event,
                        onClick = if (event.bookId in existingBookIds) {
                            { onBookClick(event.bookId) }
                        } else null
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all history?") },
            text = { Text("This removes every history entry and resets the loan statistics. Your books are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClearHistory()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}
