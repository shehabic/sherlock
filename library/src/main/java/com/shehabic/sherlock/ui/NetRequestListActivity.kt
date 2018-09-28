package com.shehabic.sherlock.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.R
import com.shehabic.sherlock.db.Db
import com.shehabic.sherlock.db.NetworkRequests
import com.shehabic.sherlock.db.Sessions
import com.shehabic.sherlock.toSimpleString
import com.shehabic.sherlock.ui.data.NetworkRequestsList
import kotlinx.android.synthetic.main.activity_netrequest_list.*
import kotlinx.android.synthetic.main.netrequest_list.*
import kotlinx.android.synthetic.main.netrequest_list_content.view.*
import java.lang.ref.WeakReference
import java.util.*


class NetRequestListActivity : SherlockActivity() {

    private var twoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_netrequest_list)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false);
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        if (netrequest_detail_container != null) {
            twoPane = true
        }
        handleStartup()
    }

    private fun handleStartup() {
        NetworkSherlock.getInstance().init(this)
        setupUI()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleStartup()
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
            R.id.delete_session_requests -> deleteAllSessionRequests()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteAllSessionRequests() {
        getCurrentSession()?.let {
            AlertDialog
                .Builder(this)
                .setCancelable(true)
                .setMessage(getString(R.string.sherlock_delete_all_requests_in_session))
                .setPositiveButton(R.string.sherlock_delete) { d: DialogInterface, _: Int ->
                    NetworkSherlock.getInstance().deleteRequests(it)
                    NetworkRequestsList.ITEMS.clear()
                    NetworkRequestsList.ITEM_MAP.clear()
                    netrequest_list.adapter?.notifyDataSetChanged()
                }
                .setNegativeButton(R.string.sherlock_cancel) { d: DialogInterface, _: Int -> d.dismiss() }
                .show()
        }
    }

    private fun deleteAllData() {
        getCurrentSession()?.let {
            AlertDialog
                .Builder(this)
                .setCancelable(true)
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
        return (session_list?.adapter as? SessionsAdapter)?.let {
            return if (session_list.selectedItemPosition > -1 && it.count > session_list.selectedItemPosition) {
                it.getItem(session_list.selectedItemPosition)
            } else null
        }
    }

    private fun deleteSelectedSession() {
        getCurrentSession()?.let {
            AlertDialog
                .Builder(this)
                .setCancelable(true)
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
                            session_list?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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

        private fun getErrorDrawable(ctx: Context) = AppCompatResources.getDrawable(ctx, R.drawable.ic_sherlock_error)
        private fun getSuccessDrawable(ctx: Context) = AppCompatResources.getDrawable(ctx, R.drawable.ic_sherlock_success)
        private fun getRedirectDrawable(ctx: Context) = AppCompatResources.getDrawable(ctx, R.drawable.ic_sherlock_redirect)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            val ctx = holder.statusImage.context
            holder.statusImage.setImageDrawable(
                if (item.isSuccess) {
                    getSuccessDrawable(ctx)
                } else {
                    if (item.isRedirect) {
                        getRedirectDrawable(ctx)
                    } else {
                        getErrorDrawable(ctx)
                    }
                }
            )
            holder.statusCode.text = item.statusCode
            holder.requestUrl.text = item.url
            holder.method.text = item.networkRequest.method
            val dt = Date()
            dt.time = item.networkRequest.requestStartTime
            holder.time.text = dt.toSimpleString()
            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val statusCode: TextView = view.status_code
            val statusImage: ImageView = view.request_status
            val requestUrl: TextView = view.request_url
            val method: TextView = view.method
            val time: TextView = view.time
        }
    }

    private fun isLauncherForNetworkRequestList(): Boolean = intent?.action == Intent.ACTION_MAIN

    override fun onDestroy() {
        super.onDestroy()
        if (isLauncherForNetworkRequestList()) {
            NetworkSherlock.getInstance().destroy()
        }
    }
}
