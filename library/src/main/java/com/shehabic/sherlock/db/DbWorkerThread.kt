package com.shehabic.sherlock.db

import android.os.Handler
import android.os.HandlerThread

class DbWorkerThread(threadName: String) : HandlerThread(threadName) {

    private var workerHandler: Handler? = null

    fun postTask(task: Runnable) {
        workerHandler?.post(task)
    }

    fun prepareHandler() {
        workerHandler = Handler(looper)
    }
}
