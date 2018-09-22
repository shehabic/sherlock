package com.shehabic.sherlock.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shehabic.sherlock.R
import com.shehabic.sherlock.ui.data.NetworkRequestsList
import kotlinx.android.synthetic.main.activity_netrequest_detail.*
import kotlinx.android.synthetic.main.netrequest_detail.view.*

class NetRequestDetailFragment : Fragment() {

    private var item: NetworkRequestsList.NetworkRequestItem? = null

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
        val rootView = inflater.inflate(R.layout.netrequest_detail, container, false)
        item?.let {
            rootView.netrequest_detail.text = SpannableString(Html.fromHtml(it.getDetails()))
            it.getDetails()
        }

        return rootView
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
        val TAG = NetRequestDetailFragment::class.java.canonicalName
    }

    fun share() {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Emailing request/response")
        intent.putExtra(Intent.EXTRA_HTML_TEXT, Html.fromHtml(item?.getDetails()))
        intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(item?.getDetails()))
        startActivity(Intent.createChooser(intent, "Share via Email"))
    }
}
