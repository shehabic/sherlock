package com.shehabic.testsherlock

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.R
import com.shehabic.sherlock.interceptors.SherlockOkHttpInterceptor
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

    var handler = Handler()
    val endPoint = "https://api.github.com/users/shehabic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkSherlock.getInstance().init(this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Thread(Runnable { networkCall() }).start()
        }
    }

    private fun networkCall() {
        val client: OkHttpClient = OkHttpClient
            .Builder()
            .addInterceptor(SherlockOkHttpInterceptor())
            .build()

        // Create request for remote resource.
        val request = Request.Builder().url(endPoint).build()

        client.newCall(request).execute()
    }
}
