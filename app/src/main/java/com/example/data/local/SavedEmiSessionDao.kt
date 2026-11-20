package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedEmiSessionDao {

    @Query("SELECT * FROM saved_emi_sessions ORDER BY createdAtMillis DESC, id DESC")
    fun getAllSessions(): Flow<List<SavedEmiSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SavedEmiSessionEntity): Long

    @Query("DELETE FROM saved_emi_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM saved_emi_sessions")
    suspend fun clearAllSessions()
}
