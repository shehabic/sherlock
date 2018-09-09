package com.shehabic.sherlock.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.R
import com.shehabic.sherlock.ui.data.NetworkRequestsList
import kotlinx.android.synthetic.main.activity_netrequest_list.*
import kotlinx.android.synthetic.main.netrequest_list.*
import kotlinx.android.synthetic.main.netrequest_list_content.view.*

class NetRequestListActivity : AppCompatActivity() {

    private var twoPane: Boolean = false

    class BGWorkerThread(threadName: String) : HandlerThread(threadName) {
        private lateinit var mWorkerHandler: Handler
        override fun onLooperPrepared() {
            super.onLooperPrepared()
            mWorkerHandler = Handler(looper)
        }
        fun postTask(task: Runnable) {
            mWorkerHandler.post(task)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_netrequest_list)
        worker = BGWorkerThread("bgWorkerThread")
        worker?.start()
        setSupportActionBar(toolbar)
        toolbar.title = title
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        if (netrequest_detail_container != null) {
            twoPane = true
        }

        setupRecyclerView(netrequest_list)
    }

    private var worker: BGWorkerThread? = null

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        val lists = NetworkRequestsList()
        val runnable = Runnable {
            for (request in NetworkSherlock.getInstance().getCurrentRequestsSync()) {
                lists.addItem(request)
            }
            runOnUiThread {
                recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, NetworkRequestsList.ITEMS, twoPane)
            }
        }
        worker?.postTask(runnable)
    }

    override fun onDestroy() {
        worker?.quit()
        super.onDestroy()
    }


    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: NetRequestListActivity,
        private val values: List<NetworkRequestsList.NetworkRequestItem>,
        private val twoPane: Boolean
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as NetworkRequestsList.NetworkRequestItem
                if (twoPane) {
                    val fragment = NetRequestDetailFragment().apply {
                        arguments = Bundle().apply {
                            putString(NetRequestDetailFragment.ARG_ITEM_ID, item.id)
                        }
                    }
                    parentActivity.supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.netrequest_detail_container, fragment)
                        .commit()
                } else {
                    val intent = Intent(v.context, NetRequestDetailActivity::class.java).apply {
                        putExtra(NetRequestDetailFragment.ARG_ITEM_ID, item.id)
                    }
                    v.context.startActivity(intent)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.netrequest_list_content, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.idView.text = item.id
            holder.contentView.text = item.content

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val idView: TextView = view.id_text
            val contentView: TextView = view.content
        }
    }
}
