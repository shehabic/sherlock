package com.shehabic.sherlock.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey


@Entity
data class Sessions(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "session_id") var sessionId: Int? = null,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "started_at") var startedAt: Long
)