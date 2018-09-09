package com.shehabic.sherlock.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Spinner
import android.widget.TextView
import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.R
import com.shehabic.sherlock.db.Db
import com.shehabic.sherlock.db.Sessions
import com.shehabic.sherlock.ui.data.NetworkRequestsList
import kotlinx.android.synthetic.main.activity_netrequest_list.*
import kotlinx.android.synthetic.main.netrequest_list.*
import kotlinx.android.synthetic.main.netrequest_list_content.view.*
import java.lang.ref.WeakReference
import android.widget.EditText
import android.view.LayoutInflater
import android.widget.AdapterView
import com.shehabic.sherlock.db.NetworkRequests


class NetRequestListActivity : AppCompatActivity() {

    private var twoPane: Boolean = false
    private var worker: BGWorkerThread? = null

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
        NetworkSherlock.getInstance().initWithReusingMostRecentSession(this)
        setContentView(R.layout.activity_netrequest_list)
        worker = BGWorkerThread("bgWorkerThread")
        worker?.start()
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false);
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        if (netrequest_detail_container != null) {
            twoPane = true
        }
        setupUI()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sherlock_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_rename -> renameSelectedSession()
            R.id.action_delete_session -> deleteSelectedSession()
            R.id.action_delete_all -> deleteAllData()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteAllData() {
        getCurrentSession()?.let {
            AlertDialog
                .Builder(this)
                .setCancelable(true)
                .setTitle(getString(R.string.delete_current_session))
                .setMessage(getString(R.string.delete_current_session))
                .setPositiveButton(R.string.sherlock_delete) { d: DialogInterface, _: Int ->
                    NetworkSherlock.getInstance().clearAll()
                    if (!isLauncherForNetworkRequestList()) {
                        NetworkSherlock.getInstance().destroy()
                        NetworkSherlock.getInstance().init(this)
                        finish()
                        d.dismiss()
                    }
                }
                .setNegativeButton(R.string.sherlock_cancel) { d: DialogInterface, _: Int -> d.dismiss() }
                .show()
        }
    }

    private fun getCurrentSession(): Sessions? {
        return (session_list?.adapter as SessionsAdapter).getItem(session_list.selectedItemPosition)
    }

    private fun deleteSelectedSession() {
        getCurrentSession()?.let {
            AlertDialog
                .Builder(this)
                .setCancelable(true)
                .setTitle(getString(R.string.delete_current_session))
                .setMessage(getString(R.string.delete_current_session))
                .setPositiveButton(R.string.sherlock_delete) { d: DialogInterface, _: Int ->
                    NetworkSherlock.getInstance().deleteSession(it)
                    setupUI()
                    d.dismiss()
                }
                .setNegativeButton(R.string.sherlock_cancel) { d: DialogInterface, _: Int -> d.dismiss() }
                .show()
        }
    }

    private fun renameSelectedSession() {
        getCurrentSession()?.let {
            val v = LayoutInflater.from(this).inflate(R.layout.user_input_dialog_view, null)
            val sessionText = v.findViewById(R.id.session_name) as EditText
            sessionText.setText(it.name)
            AlertDialog.Builder(this)
                .setView(v)
                .setCancelable(true)
                .setPositiveButton(R.string.sherlock_rename) { d: DialogInterface, _: Int? ->
                    it.name = sessionText.text.toString()
                    NetworkSherlock.getInstance().renameSession(it)
                    setupUI()
                    d.dismiss()
                }
                .setNegativeButton(R.string.sherlock_cancel) { d: DialogInterface, _: Int? -> d.dismiss() }
                .show()
        }
    }


    private fun setupUI() {
        setupSessionList(session_list)
    }

    private fun setupSessionList(session_list: Spinner?) {
        val weakActivity = WeakReference<Activity>(this)
        Runnable {
            NetworkSherlock.getInstance().getSessionList(object : Db.ResultsCallback<List<Sessions>> {
                override fun onResults(results: List<Sessions>?) {
                    weakActivity.get()?.let { activity ->
                        runOnUiThread {

                            session_list?.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    netrequest_list.adapter = null
                                }

                                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                                    getCurrentSession()?.let {
                                        setupRecyclerView(netrequest_list, getCurrentSession())
                                    }
                                }
                            }
                            session_list?.adapter = SessionsAdapter(
                                activity,
                                R.layout.item_session,
                                R.id.session_name,
                                results!!
                            )
                        }
                    }
                }
            })
        }.run()
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, session: Sessions?) {
        val lists = NetworkRequestsList()
        NetworkRequestsList.ITEMS.clear()
        NetworkRequestsList.ITEM_MAP.clear()
        val weakActivity = WeakReference<NetRequestListActivity>(this)
        NetworkSherlock.getInstance().getSessionRequests(session, object : Db.ResultsCallback<List<NetworkRequests>> {
            override fun onResults(results: List<NetworkRequests>?) {
                weakActivity.get()?.let { activity ->
                    runOnUiThread {
                        for (request in results!!) lists.addItem(request)
                        recyclerView.adapter = SimpleItemRecyclerViewAdapter(activity, NetworkRequestsList.ITEMS, twoPane)
                    }
                }
            }
        })
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

    private fun isLauncherForNetworkRequestList(): Boolean = intent?.action == Intent.ACTION_MAIN

    override fun onStop() {
        super.onStop()
        worker?.quit()
        if (isLauncherForNetworkRequestList()) {
            NetworkSherlock.getInstance().destroy()
        }
    }
}
