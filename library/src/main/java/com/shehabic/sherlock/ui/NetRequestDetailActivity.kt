package com.shehabic.sherlock.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.NavUtils
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.shehabic.sherlock.NetworkSherlock
import com.shehabic.sherlock.R
import kotlinx.android.synthetic.main.activity_netrequest_detail.*

class NetRequestDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NetworkSherlock.getInstance().init(applicationContext)
        setContentView(R.layout.activity_netrequest_detail)
        setSupportActionBar(detail_toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own detail action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = NetRequestDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(NetRequestDetailFragment.ARG_ITEM_ID,
                        intent.getStringExtra(NetRequestDetailFragment.ARG_ITEM_ID))
                }
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.netrequest_detail_container, fragment)
                .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                NavUtils.navigateUpTo(this, Intent(this, NetRequestListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
