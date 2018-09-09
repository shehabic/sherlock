package com.shehabic.sherlock.ui.dummy

import com.shehabic.sherlock.db.NetworkRequests
import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
class NetworkRequestsList {

    companion object {
        /**
         * An array of sample (dummy) items.
         */
        val ITEMS: MutableList<NetworkRequestItem> = ArrayList()

        /**
         * A map of sample (dummy) items, by ID.
         */
        val ITEM_MAP: MutableMap<String, NetworkRequestItem> = HashMap()
    }

    private val COUNT = 25

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
            .append(request)

        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    data class NetworkRequestItem(val id: String, val content: String, val details: String) {
        override fun toString(): String = content
    }
}
