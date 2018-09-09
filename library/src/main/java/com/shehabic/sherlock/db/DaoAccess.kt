package com.shehabic.sherlock.db

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query

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

    @Query("SELECT * FROM NetworkRequests WHERE session_id = :sessionId ORDER BY request_start_time ASC")
    fun getAllRequestsForSession(sessionId: Int): List<NetworkRequests>

    @Insert
    fun insertSession(session: Sessions): Long

    @Query("SELECT * FROM Sessions WHERE session_id = :sessionId")
    fun fetchSession(sessionId: Int): Sessions

    @Query("SELECT * FROM Sessions ORDER BY started_at ASC")
    fun getAllSessions(): List<Sessions>

    @Delete
    fun deleteSession(session: Sessions)

    @Query("DELETE FROM Sessions")
    fun deleteAllSessions()

    @Query("DELETE FROM NetworkRequests")
    fun deleteAllNetworkRequests()

    @Query("Delete FROM NetworkRequests WHERE session_id = :sessionId")
    fun deleteAllCurrentSessionRequests(sessionId: Int)
}
