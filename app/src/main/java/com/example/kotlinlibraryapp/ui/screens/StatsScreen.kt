package com.example.kotlinlibraryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.kotlinlibraryapp.ui.LibraryStats
import com.example.kotlinlibraryapp.ui.components.EmptyState
import com.example.kotlinlibraryapp.ui.components.ScreenTopBar
import com.example.kotlinlibraryapp.ui.components.SectionHeader
import com.example.kotlinlibraryapp.ui.components.StatTile

@Composable
fun StatsScreen(stats: LibraryStats) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(title = "Library stats") }
    ) { innerPadding ->
        if (stats.total == 0) {
            EmptyState("Add some books to see your stats.", Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Books", stats.total.toString(), Modifier.weight(1f))
                StatTile("Available", stats.available.toString(), Modifier.weight(1f))
                StatTile("Borrowed", stats.borrowed.toString(), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(
                    label = "Overdue",
                    value = stats.overdue.toString(),
                    modifier = Modifier.weight(1f),
                    containerColor = if (stats.overdue > 0) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.surface,
                    contentColor = if (stats.overdue > 0) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                StatTile("Favorites", stats.favorites.toString(), Modifier.weight(1f))
                StatTile(
                    label = "Avg rating",
                    value = stats.averageRating?.let { "%.1f".format(it) } ?: "–",
                    modifier = Modifier.weight(1f)
                )
            }

            SectionHeader(title = "By genre", subtitle = "Where your shelf leans")

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    stats.genreCounts.forEach { (genre, count) ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(genre, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    count.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LinearProgressIndicator(
                                progress = { count / stats.total.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
