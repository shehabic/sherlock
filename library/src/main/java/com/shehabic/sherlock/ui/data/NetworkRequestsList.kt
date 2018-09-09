package com.shehabic.sherlock.ui.data

import com.shehabic.sherlock.db.NetworkRequests
import java.util.*

class NetworkRequestsList {

    companion object {
        val ITEMS: MutableList<NetworkRequestItem> = ArrayList()
        val ITEM_MAP: MutableMap<String, NetworkRequestItem> = HashMap()
    }

    fun addItem(item: NetworkRequests) {
        val dummy = createDummyItem(item)
        ITEMS.add(dummy)
        ITEM_MAP.put(dummy.id, dummy)
    }

    fun createDummyItem(request: NetworkRequests): NetworkRequestItem {
        return NetworkRequestItem(
            request.requestId.toString(),
            "${request.method!!} [${request.statusCode}]: ${request.requestUrl}",
            makeDetails(request)
        )
    }

    private fun makeDetails(request: NetworkRequests): String {
        val builder = StringBuilder()
        builder
            .append("------- Request --------\n")
            .append("[Method]: ${request.method}\n")
            .append("[Headers]: \n${request.requestHeaders}\n")
            .append("[URL]: ${request.requestUrl}\n")
            .append("[BODY]: \n${request.requestBody}\n")
            .append("------- Response --------\n")
            .append("[Code]: ${request.statusCode}\n")
            .append("[Total Time]: ${request.responseTime}\n")
            .append("[Headers]: \n${request.responseHeaders}\n")
            .append("[BODY]: \n${request.responseBody}")

        return builder.toString()
    }

    data class NetworkRequestItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}
