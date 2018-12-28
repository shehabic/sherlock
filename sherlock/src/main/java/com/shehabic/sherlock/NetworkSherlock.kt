package com.shehabic.sherlock

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import com.shehabic.sherlock.db.Db
import com.shehabic.sherlock.db.DbWorkerThread
import com.shehabic.sherlock.db.NetworkRequests
import com.shehabic.sherlock.db.Sessions
import com.shehabic.sherlock.ui.NetRequestListActivity
import com.shehabic.sherlock.ui.NetworkSherlockAnchor
import com.shehabic.sherlock.ui.SherlockActivity
import java.text.SimpleDateFormat
import java.util.*


@SuppressLint("SimpleDateFormat")
fun Date.toSimpleString(): String = SimpleDateFormat("yyy-dd-MM hh:mm:ss").format(this)

infix fun Any?.ifNull(block: () -> Unit) {
    if (this == null) block()
}

class NetworkSherlock private constructor(private var config: Config?) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: NetworkSherlock? = null

        @JvmStatic
        fun getInstance(): NetworkSherlock {
            return INSTANCE ?: getInstance(null)
        }

        @JvmStatic
        fun getInstance(config: Config?): NetworkSherlock {
            INSTANCE.ifNull { INSTANCE = NetworkSherlock(config) }
            return INSTANCE!!
        }

        private var KEY_SHOW_ANCHOR = "show_anchor"
    }

    enum class AppStartType {
        AppContext, SherlockActivity, OtherActivity
    }

    enum class FirstLoadedActivity {
        SherlockActivity, OtherActivity
    }

    private var appStartType: AppStartType? = null
    private var firstLoadedActivity: FirstLoadedActivity? = null
    private var captureRequests: Boolean = true
    private var uiAnchor: NetworkSherlockAnchor = NetworkSherlockAnchor.getInstance()
    private var appContext: Context? = null
    private var sessionId: Int? = null
    private var dbWorkerThread: DbWorkerThread? = null
    private val activityCycleCallbacks = NetworkSherlock.NetworkSherlockLifecycleHandler()
    private var busyCreatingSession = false
    private var sharedPrefs: SharedPreferences? = null

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
            firstLoadedActivity.ifNull {
                firstLoadedActivity = if (it is NetRequestListActivity) FirstLoadedActivity.SherlockActivity
                else FirstLoadedActivity.OtherActivity
                if (firstLoadedActivity == FirstLoadedActivity.SherlockActivity
                    && sessionId != null) {
                    resetSessionToLastOneWithRequests()
                }
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
        activity?.let {
            if ((it !is SherlockActivity).and(sessionId == null)) startSession()
            if (shouldShowAnchorItem().and(isNonLibScreen(it))) uiAnchor.addUI(activity)
        }
    }

    fun onActivityPaused(activity: Activity?) {
        if (shouldShowAnchorItem() && isNonLibScreen(activity)) {
            uiAnchor.removeUI(activity)
        }
    }

    private fun isNonLibScreen(activity: Activity?): Boolean {
        return !activity!!::class.java.canonicalName.contains(BuildConfig.APPLICATION_ID)
    }

    fun init(context: Context) {
        appContext?.let { return }
        appStartType = getAppStartType(context)
        appContext = context.applicationContext
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(activityCycleCallbacks)
        dbWorkerThread = DbWorkerThread("dbWorkerThread")
        dbWorkerThread?.start()
        dbWorkerThread?.prepareHandler()
        sharedPrefs = context.getSharedPreferences("com.shehabic.sherlock", Context.MODE_PRIVATE)
        if (this.appStartType == AppStartType.SherlockActivity) {
            reuseLastSession()
        } else {
            startSession()
        }
        config.ifNull {
            this.config = Config(
                showAnchor = sharedPrefs?.getBoolean(KEY_SHOW_ANCHOR, true) ?: true
            )
        }
    }

    private fun getAppStartType(context: Context): AppStartType? {
        return when (context) {
            is NetRequestListActivity -> AppStartType.SherlockActivity
            is Activity -> AppStartType.OtherActivity
            else -> AppStartType.AppContext
        }
    }

    private fun reuseLastSession() {
        busyCreatingSession = true
        dbWorkerThread?.postTask(Runnable {
            val mostRecentSession: Sessions? = getDb().dao().getMostRecentSession()
            busyCreatingSession = false
            mostRecentSession?.let { sessionId = it.sessionId } ?: run { startSession() }
        })
    }

    private fun getDb(): Db {
        validateInitialization()
        return Db.getInstance(appContext!!)!!
    }

    fun startSession() {
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
        if (showNetworkActivity()) {
            uiAnchor.onRequestStarted()
        }
    }

    fun endRequest() {
        if (showNetworkActivity()) {
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
        dbWorkerThread?.postTask(Runnable {
            getDb().dao().deleteSession(session)
            if (session.sessionId == this.sessionId) {
                this.sessionId = null
            }
        })
    }

    fun renameSession(session: Sessions) {
        dbWorkerThread?.postTask(Runnable { getDb().dao().updateSession(session) })
    }

    fun clearAll() {
        dbWorkerThread?.postTask(Runnable {
            getDb().dao().getAllRequests()
            getDb().dao().deleteAllSessions()
            this.sessionId = null
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

    fun deleteRequests(session: Sessions) {
        dbWorkerThread?.postTask(Runnable {
            getDb().dao().deleteAllCurrentSessionRequests(session.sessionId!!)
        })
    }

    private fun showNetworkActivity() = config?.showNetworkActivity ?: true

    internal fun shouldShowAnchorItem() = config?.showAnchor ?: true

    internal fun showAnchorItem() = modifyAnchorVisibility(true)

    internal fun hideAnchorItem() = modifyAnchorVisibility(false)

    private fun modifyAnchorVisibility(visible: Boolean) {
        this.config = config?.copy(showAnchor = visible) ?: Config(showAnchor = visible)
        sharedPrefs?.let { it.edit().putBoolean(KEY_SHOW_ANCHOR, visible).apply() }
    }
}
