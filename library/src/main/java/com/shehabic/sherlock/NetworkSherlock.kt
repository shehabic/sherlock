package com.shehabic.sherlock

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.shehabic.sherlock.db.Db
import com.shehabic.sherlock.db.DbWorkerThread
import com.shehabic.sherlock.db.NetworkRequests
import com.shehabic.sherlock.db.Sessions
import com.shehabic.sherlock.ui.NetworkSherlockAnchor
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
fun Date.toSimpleString(): String = SimpleDateFormat("yyy-dd-MM hh:mm:ss").format(this)

infix fun Any?.ifNull(block: () -> Unit) {
    if (this == null) block()
}

class NetworkSherlock private constructor(private val config: Config) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: NetworkSherlock? = null

        fun getInstance(): NetworkSherlock {
            return INSTANCE ?: getInstance(Config())
        }

        fun getInstance(config: Config): NetworkSherlock {
            INSTANCE.ifNull { INSTANCE = NetworkSherlock(config) }

            return INSTANCE!!
        }
    }

    private var captureRequests: Boolean = true
    private var uiAnchor: NetworkSherlockAnchor = NetworkSherlockAnchor.getInstance()
    private var appContext: Context? = null
    private var sessionId: Int? = null
    private var dbWorkerThread: DbWorkerThread? = null
    private val activityCycleCallbacks = NetworkSherlock.NetworkSherlockLifecycleHandler()
    private var busyCreatingSession = false

    data class Config(
        val showAnchor: Boolean = true,
        val showNetworkActivity: Boolean = true
    )

    class NetworkSherlockLifecycleHandler : Application.ActivityLifecycleCallbacks {
        override fun onActivityPaused(activity: Activity?) {
            NetworkSherlock.getInstance().onActivityPaused(activity)
        }

        override fun onActivityResumed(activity: Activity?) {
            NetworkSherlock.getInstance().onActivityResumed(activity)
        }

        override fun onActivityStarted(activity: Activity?) {
        }

        override fun onActivityDestroyed(activity: Activity?) {
        }

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
        }

        override fun onActivityStopped(activity: Activity?) {
        }

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
            NetworkSherlock.getInstance().onActivityCreated(activity)
        }
    }

    fun onActivityCreated(activity: Activity?) {
        activity?.let {
            if (sessionId != null
                && it.intent.action == Intent.ACTION_MAIN
                && it::class.java.canonicalName.startsWith("com.shehabic.sherlock")) {
                // delete session if it's empty
                resetSessionToLastOneWithRequests()
            }
        }
    }

    private fun resetSessionToLastOneWithRequests() {
        busyCreatingSession = true
        dbWorkerThread?.postTask(Runnable {
            val lastNonEmptySession = getDb().dao().getRequestsWithTheMostRecentSession()
            lastNonEmptySession?.let {
                sessionId?.let { sId ->
                    if (sId != it.sessionId) {
                        getDb().dao().deleteSessionById(sId)
                    }
                }
                sessionId = it.sessionId
            }
            busyCreatingSession = false
        })
    }

    fun onActivityResumed(activity: Activity?) {
        if (config.showAnchor && isNonLibScreen(activity)) {
            uiAnchor.addUI(activity)
        }
    }

    fun onActivityPaused(activity: Activity?) {
        if (config.showAnchor && isNonLibScreen(activity)) {
            uiAnchor.removeUI(activity)
        }
    }

    private fun isNonLibScreen(activity: Activity?): Boolean {
        return !activity!!::class.java.canonicalName.contains(BuildConfig.APPLICATION_ID)
    }

    private fun init(context: Context, reuseLastSession: Boolean) {
        appContext?.let { return }
        appContext = context.applicationContext
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(
            activityCycleCallbacks
        )
        dbWorkerThread = DbWorkerThread("dbWorkerThread")
        dbWorkerThread?.start()
        dbWorkerThread?.prepareHandler()
        if (!reuseLastSession) {
            startNewSession()
        } else {
            reuseLastSession()
        }
    }

    fun init(context: Context) {
        init(context, reuseLastSession = false)
    }

    fun initWithReusingMostRecentSession(context: Context) {
        init(context = context, reuseLastSession = true)
    }

    private fun reuseLastSession() {
        busyCreatingSession = true
        dbWorkerThread?.postTask(Runnable {
            val mostRecentSession: Sessions? = getDb().dao().getMostRecentSession()
            busyCreatingSession = false
            mostRecentSession?.let { sessionId = it.sessionId } ?: run { startNewSession() }
        })
    }

    private fun getDb(): Db {
        validateInitialization()
        return Db.getInstance(appContext!!)!!
    }

    fun startNewSession() {
        validateInitialization()
        busyCreatingSession = true
        dbWorkerThread?.postTask(Runnable {
            val startedAt = Date()
            val session = Sessions(
                startedAt = startedAt.time,
                name = "Started: ${startedAt.toSimpleString()}"
            )
            sessionId = getDb().dao().insertSession(session).toInt()
            busyCreatingSession = false
        })
    }

    private fun validateInitialization() {
        appContext.ifNull { throw RuntimeException("NetworkSherlock not initialized") }
    }

    fun startRequest() {
        if (config.showNetworkActivity) {
            uiAnchor.onRequestStarted()
        }
    }

    fun endRequest() {
        if (config.showNetworkActivity) {
            uiAnchor.onRequestEnded()
        }
    }

    fun addRequest(request: NetworkRequests) {
        if (!captureRequests) return
        validateInitialization()
        request.sessionId = sessionId!!
        getDb().dao().insertRequest(request)
    }

    fun deleteSession(session: Sessions) {
        dbWorkerThread?.postTask(Runnable { getDb().dao().deleteSession(session) })
    }

    fun renameSession(session: Sessions) {
        dbWorkerThread?.postTask(Runnable { getDb().dao().updateSession(session) })
    }

    fun getCurrentRequestsSync(): List<NetworkRequests> {
        waitUntilSessionIsReady()
        return getDb().dao().getAllRequestsForSession(sessionId!!)
    }

    fun clearAll() {
        dbWorkerThread?.postTask(Runnable {
            getDb().dao().getAllRequests()
            getDb().dao().deleteAllSessions()
        })
    }

    fun getSessionId(): Int {
        return sessionId ?: 0
    }

    fun pauseRecording() {
        captureRequests = false
    }

    fun resumeRecording() {
        captureRequests = true
    }

    fun isRecording(): Boolean {
        return captureRequests
    }

    fun destroy() {
        validateInitialization()
        (appContext as Application).unregisterActivityLifecycleCallbacks(activityCycleCallbacks)
        sessionId = null
        appContext = null
    }

    fun getSessionList(callback: Db.ResultsCallback<List<Sessions>>) {
        dbWorkerThread?.postTask(Runnable { callback.onResults(getDb().dao().getAllSessions()) })
    }

    fun getSessionRequests(session: Sessions?, callback: Db.ResultsCallback<List<NetworkRequests>>) {
        waitUntilSessionIsReady()
        dbWorkerThread?.postTask(Runnable {
            val requests = getDb().dao().getAllRequestsForSession(session?.sessionId ?: sessionId!!)
            callback.onResults(requests)
        })
    }

    fun waitUntilSessionIsReady() {
        validateInitialization()
        while (busyCreatingSession) Thread.sleep(100)
    }
}