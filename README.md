# The Reading Room

A personal library manager for Android, built with Kotlin and Jetpack Compose. Keep track of the books on your shelf, see what is borrowed and when it is due, and review a saved history of every loan. All data is stored locally with Room and survives app restarts.

## Features

**Shelf**: browse books with cover, author, genre and loan status; search by title, author or genre; filter by All, Available, Borrowed or Favorites; sort by title, author or rating; borrow or return from the list.

**Add and edit books**: one form for both. Title (required), author, genre and optional cover link, with inline validation and duplicate detection. Edit from the pencil icon on the details screen.

**Book details**: cover, status and due date, favorite toggle, 1 to 5 star rating, personal notes, per-book activity list, and delete with a confirmation dialog plus an Undo snackbar.

**Loans (multi-step workflow with rules)**: Available -> Borrowed -> Renewed (up to 2 times) -> Returned.
- 14-day loans, 7-day renewals
- At most 3 books on loan at once (borrowing a 4th is rejected with a message)
- A book can be renewed at most 2 times per loan; the counter resets on return
- Overdue banner, and a badge on the Loans tab

**History (persistent activity log and statistics)**: every add, edit, borrow, renew, return, delete and undo is saved as a history entry. The History tab shows total loans, total returns, on-time return rate, the most borrowed books, and the full log with an All / Loans / Library filter. Entries stay readable after a book is deleted, and tapping an entry opens the book if it still exists. History can be cleared after a confirmation.

**Stats**: totals, average rating and genre breakdown.

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose (bottom navigation plus detail and form destinations)
- MVVM: `ViewModel`, `StateFlow`, `collectAsStateWithLifecycle()`
- Room (entities, DAOs, `Flow` queries, transactions) with KSP
- Coil 3 for image loading

## Architecture

```
UI (screens, components)  --events-->  LibraryViewModel  --calls-->  BookRepository  -->  Room DAOs
        ^                                    |                            |
        +---------- LibraryUiState <---------+-------- Flow<List<...>> <---+
```

| Layer | Files |
|---|---|
| UI | `ui/screens/*`, `ui/components/Components.kt`, `ui/navigation/AppNavigation.kt` |
| ViewModel | `ui/LibraryViewModel.kt` (state, one-shot events, user intents) |
| Repository | `data/BookRepository.kt` (single source of truth; loan rules run in DB transactions) |
| Data source | `data/local/` (`LibraryDatabase`, `BookDao`, `LoanEventDao`), entities `data/Book.kt`, `data/LoanEvent.kt` |
| Domain | `domain/LoanPolicy.kt`, `domain/BookValidator.kt` (plain Kotlin, easy to unit test) |

- Room `Flow` queries re-emit on every table change, so a change made on one screen shows up on all others with no manual refresh.
- Composables never touch data directly: they receive state and call ViewModel methods.
- The ViewModel is created with a `ViewModelProvider.Factory` that reads the repository from `LibraryApplication`.
- Search, filter and sort selections are kept in `SavedStateHandle`; form drafts use `rememberSaveable`.
- Sample books and a few history entries are inserted once, when the database is first created.

## Running the app

1. Open the project in Android Studio and let Gradle sync finish.
2. Press **Run** on a device or emulator (minSdk 24). Internet access is needed only for cover images.

### Trying it out
- Borrow three books, then try a fourth to see the loan limit message.
- Renew a loan twice; the third renewal is blocked and the button is disabled.
- Return The Martian (already overdue) and check History: it is logged as a late return and the on-time rate drops.
- Edit a book, delete it, tap Undo, then open History to see every step.
- Close and reopen the app: books, loans and history are still there.

## Cover images

A cover link must be a direct link to an image and must start with `https://`. Open Library returns a blank pixel for ISBNs it has no cover for; for those use `https://covers.openlibrary.org/b/id/<number>-M.jpg`.

## Known limitations

- Overdue status is recalculated whenever data changes, not on a timer.
- The database schema is version 1 with no migrations yet.

<img width="316" height="692" alt="Снимок экрана — 2026-10-27 в 17 24 00" src="https://github.com/user-attachments/assets/c066f9eb-65ec-4f0c-a783-169ef27a6a3b" />
<img width="315" height="693" alt="Снимок экрана — 2026-10-27 в 17 23 49" src="https://github.com/user-attachments/assets/cdbce7fd-5e2b-4e77-89e0-80ad3bc4f4bf" />
<img width="317" height="693" alt="Снимок экрана — 2026-10-27 в 17 23 33" src="https://github.com/user-attachments/assets/b3ba1ddf-c27c-4e59-9203-b715f07e6441" />
