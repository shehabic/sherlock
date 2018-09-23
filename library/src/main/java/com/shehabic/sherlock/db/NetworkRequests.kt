package com.shehabic.sherlock.db

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
data class NetworkRequests(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "request_id") var requestId: Int,
    @ColumnInfo(name = "session_id") var sessionId: Int,

    @ColumnInfo(name = "request_method") var method: String? = "GET",
    @ColumnInfo(name = "request_headers") var requestHeaders: String? = null,
    @ColumnInfo(name = "request_url") var requestUrl: String = "",
    @ColumnInfo(name = "request_body") var requestBody: String? = null,
    @ColumnInfo(name = "request_content_type") var requestContentType: String? = null,
    @ColumnInfo(name = "request_start_time") var requestStartTime: Long = 0,
    @ColumnInfo(name = "protocol") var protocol: String? = null,

    @ColumnInfo(name = "response_status_code") var statusCode: Int = 0,
    @ColumnInfo(name = "response_headers") var responseHeaders: String? = null,
    @ColumnInfo(name = "response_body") var responseBody: String? = null,
    @ColumnInfo(name = "response_content_type") var responseContentType: String? = null,
    @ColumnInfo(name = "response_length") var responseLength: Long = 0,
    @ColumnInfo(name = "response_time") var responseTime: Long = 0,
    @ColumnInfo(name = "response_error") var responseError: String? = null,
    @ColumnInfo(name = "response_message") var responseMessage: String? = null,
    @ColumnInfo(name = "is_redirect") var isRedirect: Boolean = false
)