package com.example.kotlinlibraryapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.kotlinlibraryapp.data.LoanEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanEventDao {

    @Query("SELECT * FROM loan_events ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<LoanEvent>>

    @Insert
    suspend fun insert(event: LoanEvent): Long

    @Query("DELETE FROM loan_events")
    suspend fun clear()
}
