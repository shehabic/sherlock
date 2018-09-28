package com.shehabic.testsherlock

import android.app.Application
import com.shehabic.sherlock.NetworkSherlock

class SherlockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkSherlock.getInstance().init(this)
    }
}