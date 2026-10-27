# The Reading Room

A personal library manager for Android, built with Kotlin and Jetpack Compose. Keep track of the books on your shelf, see what is borrowed and when it is due, rate and annotate what you read, and get a quick overview of your collection.

## The problem it solves

Small libraries (a home collection, a classroom shelf, a club) usually run on memory and sticky notes. Who has which book? Is it overdue? What do we own by genre? The Reading Room answers those questions in one place, with a fast search and a clear loan status on every book.

## Features

**Shelf**
- Browse every book with cover, author, genre and loan status
- Search by title, author or genre
- Filter by All, Available, Borrowed or Favorites
- Sort by title, author or rating
- Borrow or return a book straight from the list

**Loans**
- All borrowed books ordered by due date
- 14-day loans with a clear "Due in N days" or "Overdue by N days" label
- Renew a loan for 7 more days, or return it
- Overdue banner and a badge on the Loans tab showing how many books are out

**Book details**
- Large cover, status and due date
- Favorite toggle and 1 to 5 star rating (tap the current rating again to clear it)
- Personal notes with a Save button
- Delete with a confirmation dialog and an Undo snackbar

**Add a book**
- Title (required), author, genre picker and optional cover image link
- Inline validation and duplicate detection (same title and author)
- Live cover preview that tells you if the image link cannot be loaded

**Stats**
- Totals for books, available, borrowed, overdue and favorites
- Average rating
- Genre breakdown with progress bars

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Navigation Compose (bottom navigation plus detail and form destinations)
- ViewModel with `StateFlow`, `SavedStateHandle` and lifecycle-aware collection
- Kotlin coroutines and Flow
- Coil 3 for image loading

## Architecture

The app follows a simple unidirectional data flow: the UI shows state and sends events up, the ViewModel owns the state, and the repository is the single source of truth for data.

```
UI (screens, components)  --events-->  LibraryViewModel  --calls-->  BookRepository
        ^                                    |                           |
        +---------- LibraryUiState <---------+---- StateFlow<List<Book>> +
```

- `LibraryViewModel` combines the repository's book list with the search, filter and sort inputs into one `LibraryUiState` using `combine(...).stateIn(...)`.
- Screens are stateless where possible and receive state plus callbacks (state hoisting). Local UI state such as text field drafts uses `rememberSaveable`, and menu or dialog state uses `remember`.
- One-shot events (snackbar messages, undo prompts) go through a `Channel` and are collected only while the screen is started.
- Search, filter and sort are stored in `SavedStateHandle`, so they survive rotation and process death. The ViewModel is Activity-scoped, so the data survives configuration changes.
- Validation rules live in a plain Kotlin object with no Android dependencies, so they are easy to unit test.


## Running the app

### Requirements
- Android Studio 
- A device or emulator running the minimum SDK your project is configured for
- Internet access on the device, for loading cover images

### Steps
1. Open the project folder in Android Studio and wait for the Gradle sync to finish.
2. Confirm these dependencies are in `app/build.gradle.kts` (use the latest stable versions):
   ```kotlin
   implementation("androidx.navigation:navigation-compose:2.8.9")
   implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
   implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
   implementation("androidx.compose.material:material-icons-core")
   // plus the Compose BOM, Material 3 and Coil 3 (coil-compose and coil-network-okhttp)
   ```
3. Confirm `AndroidManifest.xml` contains:
   ```xml
   <uses-permission android:name="android.permission.INTERNET" />
   ```
4. Choose a device or emulator and press **Run**.

### Trying it out
- The app starts with six sample books. Dune is on loan, and The Martian is overdue so you can see that state.
- Tap **Add book**, then paste a direct image link to test covers, for example `https://covers.openlibrary.org/b/isbn/9780441172719-M.jpg`.
- Rotate the device while searching or filling in the form to see state being preserved.

## Cover images

A cover link must be a direct link to an image (it should open as just the picture in a browser) and must start with `https://`. Links to web pages, or sites that block hotlinking, will show the letter placeholder instead. Open Library returns a blank pixel for ISBNs it has no cover for, so for those books use a `https://covers.openlibrary.org/b/id/<number>-M.jpg` link instead.

## Known limitations

- Data is stored in memory only, so it resets when the app process is killed.
- The overdue status is recalculated whenever the data changes, not on a timer.

## Ideas for next steps

- Persist data with Room or DataStore (only `BookRepository` needs to change)
- Dependency injection with Hilt
- Barcode scanning to add books by ISBN
- Reminders for upcoming due dates
- Unit tests for `BookValidator` and `LibraryViewModel`
<img width="316" height="692" alt="Снимок экрана — 2026-10-27 в 17 24 00" src="https://github.com/user-attachments/assets/c066f9eb-65ec-4f0c-a783-169ef27a6a3b" />
<img width="315" height="693" alt="Снимок экрана — 2026-10-27 в 17 23 49" src="https://github.com/user-attachments/assets/cdbce7fd-5e2b-4e77-89e0-80ad3bc4f4bf" />
<img width="317" height="693" alt="Снимок экрана — 2026-10-27 в 17 23 33" src="https://github.com/user-attachments/assets/b3ba1ddf-c27c-4e59-9203-b715f07e6441" />
