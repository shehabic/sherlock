package com.shehabic.sherlock.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.shehabic.sherlock.R
import com.shehabic.sherlock.toSimpleString
import com.shehabic.sherlock.ui.data.NetworkRequestsList
import kotlinx.android.synthetic.main.activity_netrequest_detail.*
import kotlinx.android.synthetic.main.item_detail_title.view.*
import kotlinx.android.synthetic.main.netrequest_detail2.view.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Exception
import java.util.*

class NetRequestDetailFragment : Fragment() {

    private var item: NetworkRequestsList.NetworkRequestItem? = null
    private var requestDetails: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                item = NetworkRequestsList.ITEM_MAP[it.getString(ARG_ITEM_ID)]
                activity?.toolbar_layout?.title = item?.url
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.netrequest_detail2, container, false)
        requestDetails = rootView.request_details
        item?.let { populateItemDetails(it) }

        return rootView
    }

    private fun populateItemDetails(requestItem: NetworkRequestsList.NetworkRequestItem) {
        requestDetails?.let {
            it.adapter = RequestDetailsRecyclerViewAdapter.createFromNetRequest(requestItem)
        }
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
        val TAG = NetRequestDetailFragment::class.java.canonicalName
    }

    fun share() {
        val intent = Intent(Intent.ACTION_SEND, Uri.parse("mailto:"))
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Emailing request/response")
        intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(item?.getDetails()))
        startActivity(Intent.createChooser(intent, "Share via Email"))
    }

    enum class Type { SECTION, TITLE, CONTENT }

    data class DetailItem(val txt: String, val type: Type, val subItem: String? = null)

    class RequestDetailsRecyclerViewAdapter(private val values: List<DetailItem>) :
        RecyclerView.Adapter<RequestDetailsRecyclerViewAdapter.ViewHolder>() {

        companion object {
            fun createFromNetRequest(request: NetworkRequestsList.NetworkRequestItem): RequestDetailsRecyclerViewAdapter {
                val items = ArrayList<DetailItem>()
                val netRequest = request.networkRequest
                // Request
                items.add(DetailItem("Request -> (${netRequest.method.toString()})", Type.SECTION))
                items.add(DetailItem("Url", Type.TITLE))
                items.add(DetailItem(netRequest.requestUrl, Type.CONTENT))
                items.add(DetailItem("Headers", Type.TITLE))
                items.add(DetailItem(netRequest.requestHeaders ?: "---", Type.CONTENT, "headers"))
                items.add(DetailItem("Content-Type", Type.TITLE))
                items.add(DetailItem(netRequest.requestContentType ?: "---", Type.CONTENT))
                items.add(DetailItem("Started at", Type.TITLE))
                items.add(DetailItem(netRequest.requestStartTime.toString(), Type.CONTENT, "date"))
                items.add(DetailItem("Body", Type.TITLE))
                items.add(DetailItem(
                    netRequest.requestBody ?: "---",
                    Type.CONTENT,
                    if (netRequest.requestContentType?.contains("json", true) == true) "json"
                    else null
                ))
                // Response
                items.add(
                    DetailItem(
                        "Response <- (${netRequest.statusCode} ${netRequest.responseMessage})",
                        Type.SECTION
                    )
                )
                items.add(DetailItem("Response time", Type.TITLE))
                items.add(DetailItem("${netRequest.responseTime}ms", Type.CONTENT))
                items.add(DetailItem("Content-Type", Type.TITLE))
                items.add(DetailItem(netRequest.responseContentType ?: "---", Type.CONTENT))
                items.add(DetailItem("Headers", Type.TITLE))
                items.add(DetailItem(netRequest.responseHeaders ?: "---", Type.CONTENT, "headers"))
                items.add(DetailItem("Body", Type.TITLE))
                items.add(DetailItem(
                    netRequest.responseBody ?: "---",
                    Type.CONTENT,
                    if (netRequest.responseContentType?.contains("json", true) == true) "json"
                    else null
                ))
                items.add(DetailItem("Errors", Type.TITLE))
                items.add(DetailItem(netRequest.responseError ?: "---", Type.CONTENT))

                return RequestDetailsRecyclerViewAdapter(items)
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (values[position].type) {
                Type.SECTION -> R.layout.item_detail_section
                Type.TITLE -> R.layout.item_detail_title
                Type.CONTENT -> R.layout.item_detail_content
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(parent.context).inflate(viewType, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            when (item.type) {
                Type.SECTION, Type.TITLE -> holder.text.text = item.txt
                Type.CONTENT -> setContent(holder.text, item)
            }
        }

        private fun setContent(text: TextView, item: DetailItem) {
            when (item.subItem?.toLowerCase() ?: "") {
                "json" -> text.text = formatJson(item.txt)
                "headers" -> text.text = formatHeaders(item.txt)
                "date" -> text.text = formatTime(item.txt)
                else -> text.text = item.txt
            }
        }

        private fun formatTime(txt: String): CharSequence? {
            if (txt.toLong() > 0) {
                val date = Date()
                date.time = txt.toLong()
                return date.toSimpleString()
            }
            return txt
        }

        private fun formatHeaders(txt: String): CharSequence? {
            return Html.fromHtml(
                txt
                    .replace(Regex("([A-Za-z-]+:)"), "<font color=\"#777777\">\$1</font>")
                    .replace(Regex("\n"), "<br>")
            )
        }

        private fun formatJson(txt: String): CharSequence? {
            try {
                if (txt.trim().startsWith("{", true)) {
                    return JSONObject(txt).toString(4)
                } else if (txt.trim().startsWith("[", true)) {
                    return JSONArray(txt).toString(4)
                }
            } catch (e: Exception) {
            }

            return txt
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.item_text
        }
    }


}
