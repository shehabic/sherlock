package com.shehabic.sherlock.db

import android.arch.persistence.room.*

@Dao
interface DaoAccess {
    @Insert
    fun insertRequest(request: NetworkRequests)

    @Query("SELECT * FROM NetworkRequests WHERE request_id = :requestId")
    fun fetchRequest(requestId: Int): NetworkRequests

    @Delete
    fun deleteRequest(request: NetworkRequests)

    @Query("SELECT * FROM NetworkRequests")
    fun getAllRequests(): List<NetworkRequests>

    @Query("SELECT * FROM NetworkRequests WHERE session_id = :sessionId ORDER BY request_start_time DESC")
    fun getAllRequestsForSession(sessionId: Int): List<NetworkRequests>

    @Insert
    fun insertSession(session: Sessions): Long

    @Query("SELECT * FROM Sessions WHERE session_id = :sessionId")
    fun fetchSession(sessionId: Int): Sessions

    @Query("SELECT * FROM Sessions ORDER BY started_at DESC")
    fun getAllSessions(): List<Sessions>

    @Delete
    fun deleteSession(session: Sessions)

    @Query("DELETE FROM sessions WHERE session_id = :sessionId")
    fun deleteSessionById(sessionId: Int)

    @Query("DELETE FROM Sessions")
    fun deleteAllSessions()

    @Query("DELETE FROM NetworkRequests")
    fun deleteAllNetworkRequests()

    @Query("Delete FROM NetworkRequests WHERE session_id = :sessionId")
    fun deleteAllCurrentSessionRequests(sessionId: Int)

    @Query("SELECT * FROM sessions ORDER BY started_at DESC LIMIT 1")
    fun getMostRecentSession() : Sessions?

    @Query("SELECT * FROM networkrequests ORDER BY session_id DESC LIMIT 1")
    fun getRequestsWithTheMostRecentSession() : NetworkRequests?

    @Update
    fun updateSession(session: Sessions)
}
