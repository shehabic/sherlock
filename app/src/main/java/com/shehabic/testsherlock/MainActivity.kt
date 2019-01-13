package com.shehabic.testsherlock

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Toast
import com.shehabic.sherlock.NetworkSherlock
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody


class MainActivity : AppCompatActivity() {

    data class DynamicRequest(
        val url: String,
        val body: String,
        val method: String,
        val contentType: String,
        val auth: String,
        val headerKey: String,
        val headerVal: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkSherlock.getInstance().init(this)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            makeRequest()
        }
        setupUI()
    }

    private fun makeRequest() {
        val request = DynamicRequest(
            url.text.toString().trim(),
            body.text.toString().trim(),
            method.adapter.getItem(method.selectedItemPosition).toString(),
            contenttype.adapter.getItem(contenttype.selectedItemPosition).toString(),
            authentication.text.toString().trim(),
            header.text.toString().trim(),
            value.text.toString().trim()
        )
        Thread(Runnable { networkCall(request) }).start()
    }

    private fun setupUI() {
        val methodsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS")
        )
        methodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        method.adapter = methodsAdapter


        val contentTypesAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf("application/json", "text/plain", "text/html", "application/xml", "application/x-www-form-urlencoded")
        )
        contentTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        contenttype.adapter = contentTypesAdapter
    }

    @SuppressLint("ShowToast")
    private fun networkCall(request: DynamicRequest) {
        val client: OkHttpClient = OkHttpClient
            .Builder()
            // No need for this when using the plugin
//            .addInterceptor(SherlockOkHttpInterceptor())
            .build()
        val body: RequestBody? = when(request.method) {
            "GET", "HEAD", "OPTION" -> null
            else -> RequestBody.create(MediaType.get(request.contentType), request.body)
        }

        val builder = Request.Builder()
            .url(request.url)
            .header("Content-Type", request.contentType)
            .method(request.method, body)
        if (request.auth.isNotEmpty()) {
            builder.header("Authentication", request.auth)
        }
        if (request.headerKey.isNotEmpty() && request.headerVal.isNotEmpty()) {
            builder.header(request.headerKey, request.headerVal)
        }
        val httpRequest = builder.build()
        try {
            client.newCall(httpRequest).execute()
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG)
            }
        }
    }
}
