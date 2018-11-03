package com.shehabic.sherlock

import android.annotation.SuppressLint
import android.content.Context

infix fun Any?.ifNull(block: () -> Unit) {
    if (this == null) block()
}

class NetworkSherlock private constructor(private val config: Config) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: NetworkSherlock? = null

        @JvmStatic
        fun getInstance(): NetworkSherlock {
            return INSTANCE ?: getInstance(Config())
        }

        fun getInstance(config: Config): NetworkSherlock {
            INSTANCE.ifNull { INSTANCE = NetworkSherlock(config) }

            return INSTANCE!!
        }
    }

    data class Config(
        val showAnchor: Boolean = true,
        val showNetworkActivity: Boolean = true
    )

    fun init(context: Context) { }

    fun startSession() { }

    fun startRequest() { }

    fun endRequest() { }

    fun clearAll() { }

    fun getSessionId() = 0

    fun pauseRecording() { }

    fun resumeRecording() { }

    fun isRecording() = false

    fun destroy() { }
}
