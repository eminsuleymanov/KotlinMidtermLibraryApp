package com.example.kotlinlibraryapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kotlinlibraryapp.data.Book
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /** Room re-emits this Flow whenever the books table changes, which drives every screen. */
    @Query("SELECT * FROM books ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Int): Book?

    @Query("SELECT COUNT(*) FROM books WHERE isAvailable = 0")
    suspend fun countOnLoan(): Int

    @Query(
        "SELECT COUNT(*) FROM books WHERE LOWER(title) = LOWER(:title) " +
                "AND LOWER(author) = LOWER(:author) AND id != :excludeId"
    )
    suspend fun countDuplicates(title: String, author: String, excludeId: Int): Int

    /** REPLACE also lets us restore a deleted book with its original id (Undo). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int)

    @Query("UPDATE books SET rating = :rating WHERE id = :id")
    suspend fun setRating(id: Int, rating: Int)

    @Query("UPDATE books SET notes = :notes WHERE id = :id")
    suspend fun setNotes(id: Int, notes: String)
}
