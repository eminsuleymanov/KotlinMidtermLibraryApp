package com.example.kotlinlibraryapp.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.kotlinlibraryapp.ui.LibraryEvent
import com.example.kotlinlibraryapp.ui.LibraryViewModel
import com.example.kotlinlibraryapp.ui.screens.AddBookScreen
import com.example.kotlinlibraryapp.ui.screens.BookDetailsScreen
import com.example.kotlinlibraryapp.ui.screens.LibraryScreen
import com.example.kotlinlibraryapp.ui.screens.LoansScreen
import com.example.kotlinlibraryapp.ui.screens.StatsScreen

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Loans : Screen("loans")
    object Stats : Screen("stats")
    object AddBook : Screen("add_book")
    object Details : Screen("details/{bookId}") {
        const val ARG_BOOK_ID = "bookId"
        fun createRoute(bookId: Int) = "details/$bookId"
    }
}

private data class BottomDestination(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination(Screen.Library, "Shelf", Icons.Default.Home),
    BottomDestination(Screen.Loans, "Loans", Icons.Default.DateRange),
    BottomDestination(Screen.Stats, "Stats", Icons.Default.Info)
)

@Composable
fun LibraryApp(viewModel: LibraryViewModel = viewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle-aware state collection.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Lifecycle-aware collection of one-shot events (only while the screen is STARTED).
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                snackbarHostState.currentSnackbarData?.dismiss()
                when (event) {
                    is LibraryEvent.Message ->
                        snackbarHostState.showSnackbar(event.text)

                    is LibraryEvent.BookDeleted -> {
                        val result = snackbarHostState.showSnackbar(
                            message = "Deleted '${event.title}'",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                    }
                }
            }
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = bottomDestinations.any { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.screen.route } == true
    }

    Scaffold(
        // Each screen's own Scaffold handles top/side insets; this one only hosts bar + snackbars.
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == dest.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (dest.screen == Screen.Loans && uiState.stats.borrowed > 0) {
                                    BadgedBox(badge = {
                                        Badge { Text(uiState.stats.borrowed.toString()) }
                                    }) { Icon(dest.icon, contentDescription = null) }
                                } else {
                                    Icon(dest.icon, contentDescription = null)
                                }
                            },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Library.route) {
                LibraryScreen(
                    uiState = uiState,
                    onQueryChange = viewModel::onQueryChange,
                    onFilterChange = viewModel::onFilterChange,
                    onSortChange = viewModel::onSortChange,
                    onBookClick = { navController.navigate(Screen.Details.createRoute(it)) },
                    onToggleBorrow = viewModel::toggleBorrow,
                    onAddClick = { navController.navigate(Screen.AddBook.route) }
                )
            }

            composable(Screen.Loans.route) {
                LoansScreen(
                    loans = uiState.loans,
                    overdueCount = uiState.stats.overdue,
                    now = uiState.now,
                    onBookClick = { navController.navigate(Screen.Details.createRoute(it)) },
                    onReturn = viewModel::toggleBorrow,
                    onRenew = viewModel::renew
                )
            }

            composable(Screen.Stats.route) {
                StatsScreen(stats = uiState.stats)
            }

            composable(
                route = Screen.Details.route,
                arguments = listOf(
                    navArgument(Screen.Details.ARG_BOOK_ID) { type = NavType.IntType }
                )
            ) { entry ->
                val bookId = entry.arguments?.getInt(Screen.Details.ARG_BOOK_ID) ?: -1
                BookDetailsScreen(
                    book = uiState.books.firstOrNull { it.id == bookId },
                    now = uiState.now,
                    onBack = { navController.popBackStack() },
                    onToggleBorrow = { viewModel.toggleBorrow(bookId) },
                    onRenew = { viewModel.renew(bookId) },
                    onToggleFavorite = { viewModel.toggleFavorite(bookId) },
                    onRatingChange = { viewModel.setRating(bookId, it) },
                    onSaveNotes = { viewModel.saveNotes(bookId, it) },
                    onDelete = {
                        viewModel.deleteBook(bookId)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AddBook.route) {
                AddBookScreen(
                    onBack = { navController.popBackStack() },
                    onSave = { title, author, genre, coverUrl ->
                        if (viewModel.addBook(title, author, genre, coverUrl)) {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}
