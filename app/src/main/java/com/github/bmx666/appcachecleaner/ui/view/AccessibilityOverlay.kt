package com.github.bmx666.appcachecleaner.ui.view

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.util.getDayNightModeContext


class AccessibilityOverlay(
    private val callbackClick: () -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private var overlayLayout: LinearLayout? = null
    private var overlayText: TextView? = null
    private var currentIndex = 0

    private fun getOverlayLayout(context: Context): LinearLayout? {
        return LayoutInflater.from(context).inflate(R.layout.accessibility_overlay_layout, null)
                as LinearLayout?
    }

    private fun getWindowManager(context: Context): WindowManager? {
        return context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager?
    }

    fun show(context: Context) {
        overlayLayout = null
        overlayText = null
        currentIndex = 0

        val params = WindowManager.LayoutParams().apply {
            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }

        hide(context)
        try {
            getOverlayLayout(context.getDayNightModeContext())?.apply {
                findViewById<ImageButton>(R.id.overlayButton)?.apply {
                    setOnClickListener { callbackClick.invoke() }
                }
                overlayText = findViewById(R.id.overlayText)
                overlayLayout = this
                getWindowManager(context)?.addView(this, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            overlayLayout = null
            overlayText = null
        }
    }

    fun hide(context: Context) {
        overlayLayout ?: return
        try {
            getWindowManager(context)?.removeView(overlayLayout)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            overlayLayout = null
            overlayText = null
            currentIndex = 0
        }
    }

    fun updateCounter(current: Int, total: Int) {
        overlayLayout ?: return
        if (current > currentIndex)
            currentIndex = current
        handler.post {
            overlayText?.apply {
                // ignore async updates and show only the latest index
                if (current >= currentIndex)
                    text = String.format("%d / %d", current, total)
            }
        }
    }
}