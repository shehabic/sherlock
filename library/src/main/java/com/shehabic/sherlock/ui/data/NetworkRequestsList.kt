package com.shehabic.sherlock.ui.data

import com.shehabic.sherlock.db.NetworkRequests
import java.util.*

class NetworkRequestsList {

    companion object {
        val ITEMS: MutableList<NetworkRequestItem> = ArrayList()
        val ITEM_MAP: MutableMap<String, NetworkRequestItem> = HashMap()
    }

    fun addItem(request: NetworkRequests) {
        val item = createRequestItem(request)
        ITEMS.add(item)
        ITEM_MAP.put(item.id, item)
    }

    fun createRequestItem(request: NetworkRequests) = NetworkRequestItem(
        request.requestId.toString(),
        request.requestUrl,
        request.statusCode.toString(),
        request.statusCode in 200..399,
        request
    )

    data class NetworkRequestItem(
        val id: String,
        val url: String,
        val statusCode: String,
        val isSuccess: Boolean,
        val networkRequest: NetworkRequests
    ) {
        override fun toString(): String = url

        fun getDetails(): String {
            val builder = StringBuilder()
            val headers = networkRequest.requestHeaders?.replace("\n", "<br>", true) ?: ""
            val responseHeaders = networkRequest.responseHeaders?.replace("\n", "<br>", true) ?: ""
            builder
                .append("<b>------- Request --------</b><br>")
                .append("<b>Method</b> ${networkRequest.method}<br>")
                .append("<b>Headers</b><br>$headers<br>")
                .append("<b>URL</b> ${networkRequest.requestUrl}<br>")
                .append("<b>BODY</b> <br>${networkRequest.requestBody}<br>")
                .append("<b>------- Response --------</b><br>")
                .append("<b>Code</b> ${networkRequest.statusCode}<br>")
                .append("<b>Total Time</b> ${networkRequest.responseTime}<br>")
                .append("<b>Headers</b> <br>$responseHeaders<br>")
                .append("<b>BODY</b> <br>${networkRequest.responseBody}")

            return builder.toString()
        }

    }
}
