package com.shehabic.sherlock.ui

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.shehabic.sherlock.R
import com.shehabic.sherlock.db.Sessions

class SessionsAdapter(
    context: Activity,
    resouceId: Int,
    textviewId: Int,
    list: List<Sessions>
) : ArrayAdapter<Sessions>(context, resouceId, textviewId, list) {

    private val flater: LayoutInflater = context.layoutInflater

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView ?: flater.inflate(R.layout.item_session, parent, false)
        val rowItem = getItem(position)
        val txtTitle = v.findViewById(R.id.session_name) as TextView
        txtTitle.text = rowItem.name

        return v
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView as? TextView ?: flater.inflate(R.layout.selected_session, parent, false)
        v.findViewById<TextView>(R.id.session_name).text = getItem(position).name

        return v
    }
}
