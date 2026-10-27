package com.example.kotlinlibraryapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.kotlinlibraryapp.data.BookFilter
import com.example.kotlinlibraryapp.data.SortOption
import com.example.kotlinlibraryapp.ui.LibraryStats
import com.example.kotlinlibraryapp.ui.LibraryUiState
import com.example.kotlinlibraryapp.ui.components.BookCard
import com.example.kotlinlibraryapp.ui.components.EmptyState
import com.example.kotlinlibraryapp.ui.components.SectionHeader
import com.example.kotlinlibraryapp.ui.theme.Forest
import com.example.kotlinlibraryapp.ui.theme.ForestDark
import com.example.kotlinlibraryapp.ui.theme.Sage

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (BookFilter) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onBookClick: (Int) -> Unit,
    onToggleBorrow: (Int) -> Unit,
    onAddClick: () -> Unit
) {
    // Text field state stays local (smooth typing); the ViewModel gets every change for filtering.
    var searchText by rememberSaveable { mutableStateOf(uiState.query) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add book") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, top = 12.dp, end = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LibraryHeader(uiState.stats) }

            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                        onQueryChange(it)
                    },
                    label = { Text("Search title, author or genre") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = {
                                searchText = ""
                                onQueryChange("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BookFilter.entries) { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { onFilterChange(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            item {
                ShelfHeader(
                    count = uiState.visibleBooks.size,
                    sort = uiState.sort,
                    onSortChange = onSortChange
                )
            }

            when {
                uiState.isLoading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                uiState.visibleBooks.isEmpty() -> item {
                    EmptyState("No books match your search or filter yet.")
                }

                else -> items(uiState.visibleBooks, key = { it.id }) { book ->
                    BookCard(
                        book = book,
                        now = uiState.now,
                        onClick = { onBookClick(book.id) },
                        onToggleBorrow = { onToggleBorrow(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(
    count: Int,
    sort: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionHeader(title = "Your shelf", subtitle = "$count books")
        Box {
            TextButton(onClick = { menuOpen = true }) { Text("Sort: ${sort.label}") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSortChange(option)
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(stats: LibraryStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp, topStart = 12.dp, topEnd = 12.dp))
            .background(Brush.linearGradient(colors = listOf(Forest, ForestDark)))
            .padding(22.dp)
    ) {
        Text(
            text = "THE READING ROOM",
            style = MaterialTheme.typography.labelLarge,
            color = Sage
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A little space\nfor big stories.",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderStat("BOOKS", stats.total, Modifier.weight(1f))
            HeaderStat("AVAILABLE", stats.available, Modifier.weight(1f))
            HeaderStat("BORROWED", stats.borrowed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeaderStat(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Sage
        )
    }
}
