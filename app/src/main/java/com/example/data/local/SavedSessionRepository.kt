package com.example.data.local

import kotlinx.coroutines.flow.Flow

class SavedSessionRepository(
    private val dao: SavedEmiSessionDao
) {
    val allSessions: Flow<List<SavedEmiSessionEntity>> = dao.getAllSessions()

    suspend fun saveSession(session: SavedEmiSessionEntity): Long {
        return dao.insertSession(session)
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteSessionById(id)
    }

    suspend fun clearAll() {
        dao.clearAllSessions()
    }
}
