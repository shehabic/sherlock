package com.shehabic.sherlock.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PointF
import android.graphics.PorterDuff
import android.os.SystemClock
import android.support.v4.widget.ImageViewCompat
import android.support.v7.widget.AppCompatImageView
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.shehabic.sherlock.R
import com.shehabic.sherlock.ifNull
import android.util.DisplayMetrics

class NetworkSherlockAnchor {
    private var lastX = -1.0f
    private var lastY = -1.0f
    private var busyCount: Int = 0
    private val anchors: MutableMap<String, View> = HashMap()
    private var currentAnchor: AppCompatImageView? = null
    private var maxX = -1f
    private var maxY = -1f

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: NetworkSherlockAnchor? = null

        fun getInstance(): NetworkSherlockAnchor {
            INSTANCE.ifNull { INSTANCE = NetworkSherlockAnchor() }

            return INSTANCE!!
        }
    }

    fun onRequestStarted() {
        busyCount++
        if (currentAnchor != null && busyCount >= 1) {
            (currentAnchor?.context as? Activity)?.runOnUiThread {
                currentAnchor?.let {
                    ImageViewCompat.setImageTintMode(it, PorterDuff.Mode.MULTIPLY)
                    ImageViewCompat.setImageTintList(it, ColorStateList.valueOf(Color.GREEN))
                }
            }
        }
    }

    fun onRequestEnded() {
        busyCount--
        busyCount = Math.max(busyCount, 0)
        if (currentAnchor != null && busyCount == 0) {
            (currentAnchor?.context as? Activity)?.runOnUiThread {
                currentAnchor?.let { ImageViewCompat.setImageTintList(it, null) }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun addUI(activity: Activity?) {
        activity?.let {
            if (it.isFinishing) return
            val viewGroup = it.window.decorView as ViewGroup
            val display = it.windowManager.defaultDisplay
            val buttonSize = Math.min(display.width, display.height) / 8
            val view = AppCompatImageView(it)
            val metrics = DisplayMetrics()
            it.windowManager.defaultDisplay.getMetrics(metrics)
            maxY = metrics.heightPixels.toFloat() - buttonSize
            maxX = metrics.widthPixels.toFloat() - buttonSize

            view.alpha = 0.7f
            view.setImageResource(R.drawable.ic_sherlock_wifi)
            view.setOnClickListener { v ->
                v.context.startActivity(Intent(v.context, NetRequestListActivity::class.java))
            }
            val layoutParams: FrameLayout.LayoutParams?
            if (lastX != -1f || lastY != -1f) {
                layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.NO_GRAVITY or Gravity.NO_GRAVITY)
                view.x = Math.min(Math.max(0f, lastX), maxX)
                view.y = Math.min(Math.max(0f, lastY), maxY)
            } else {
                layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize, Gravity.END or Gravity.TOP)
                layoutParams.rightMargin = 0
                layoutParams.topMargin = buttonSize / 2
            }
            view.layoutParams = layoutParams
            view.setOnTouchListener(object : View.OnTouchListener {
                var touchTime: Long = 0
                var touchPos = PointF()

                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            touchTime = SystemClock.uptimeMillis()
                            touchPos.set(event.x, event.y)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val diffX = touchPos.x - event.x
                            val diffY = touchPos.y - event.y
                            lastX = Math.min(Math.max(0f, view.x - diffX), maxX)
                            lastY = Math.min(Math.max(0f, view.y - diffY), maxY)
                            view.x = lastX
                            view.y = lastY
                            lastX = view.x
                            lastY = view.y
                        }
                    }
                    return SystemClock.uptimeMillis() - touchTime > 200
                }
            })
            viewGroup.addView(view)
            anchors[it::class.java.simpleName] = view
            currentAnchor = view
        }
    }

    fun removeUI(activity: Activity?) {
        activity?.let {
            if (it.isFinishing) return
            anchors[it::class.java.simpleName]?.let { anchor ->
                (anchor.parent as? ViewGroup)?.removeView(anchor)
                anchors.remove(it::class.java.simpleName)
                currentAnchor = null
            }
        }
    }
}
