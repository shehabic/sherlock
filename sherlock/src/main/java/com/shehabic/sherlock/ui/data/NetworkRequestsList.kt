package com.shehabic.sherlock.ui.data

import com.shehabic.sherlock.db.NetworkRequests
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
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

    private fun createRequestItem(request: NetworkRequests) = NetworkRequestItem(
        request.requestId.toString(),
        request.requestUrl,
        request.statusCode.toString(),
        request.statusCode in 200..299,
        request.statusCode in 300..399,
        request
    )

    data class NetworkRequestItem(
        val id: String,
        val url: String,
        val statusCode: String,
        val isSuccess: Boolean,
        val isRedirect: Boolean,
        val networkRequest: NetworkRequests
    ) {
        override fun toString(): String = url

        fun getDetails(): String {
            val builder = StringBuilder()
            val headers = networkRequest.requestHeaders?.replace("\n", "<br>", true) ?: ""
            val responseHeaders = networkRequest.responseHeaders?.replace("\n", "<br>", true) ?: ""
            val title = "<font color=\"#555555\"><span style=\"background-color:#EEEEEE;\">"
            val titleEnd = ":</span></font><br>"
            val formattedRequest = formatResponse(networkRequest.requestBody, networkRequest.requestContentType)
            val formattedResponse = formatResponse(networkRequest.responseBody, networkRequest.responseContentType)
            builder
                .append("<h3><font color=blue>Request</font></h3><br>")
                .append("${title}Method$titleEnd ${networkRequest.method}<br>")
                .append("${title}Headers$titleEnd $headers<br>")
                .append("${title}Url$titleEnd ${networkRequest.requestUrl}<br>")
                .append("${title}Content-type$titleEnd ${networkRequest.requestContentType}<br>")
                .append("${title}Body$titleEnd $formattedRequest<br>")
                .append("<h3><font color=blue>Response</font></h3><br>")
                .append("${title}Code$titleEnd ${networkRequest.statusCode}<br>")
                .append("${title}Total Time$titleEnd ${networkRequest.responseTime}<br>")
                .append("${title}Headers$titleEnd $responseHeaders<br>")
                .append("${title}Content-Type$titleEnd ${networkRequest.responseContentType}<br>")
                .append("${title}Message$titleEnd ${networkRequest.responseMessage ?: "--"}<br>")
                .append("${title}Errors$titleEnd ${networkRequest.responseError ?: "--"}<br>")
                .append("${title}Body$titleEnd $formattedResponse<br>")

            return builder.toString()
        }

        private fun formatResponse(responseBody: String?, responseContentType: String?): String? {
            if (responseContentType?.contains("json", true) == true) {
                try {
                    if (responseBody?.trim()?.startsWith("{", true) == true) {
                        return JSONObject(responseBody).toString(2)
                    } else if (responseBody?.trim()?.startsWith("[", true) == true) {
                        return JSONArray(responseBody).toString(2)
                    }
                } catch (e: Exception) {
                }
            }

            return responseBody?.let {
                return it
            } ?: "---"
        }
    }
}
