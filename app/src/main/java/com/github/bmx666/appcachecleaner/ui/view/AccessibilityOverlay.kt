package com.github.bmx666.appcachecleaner.ui.view

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageButton
import com.github.bmx666.appcachecleaner.R

class AccessibilityOverlay(
    private val context: Context,
    private val callback: () -> Unit
) {

    private val button: ImageButton = LayoutInflater.from(context)
        .inflate(R.layout.accessibility_overlay_button, null) as ImageButton

    init {
        button.setOnClickListener { callback.invoke() }
    }

    private fun getWindowManager(): WindowManager {
        return context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    }

    fun show() {
        val params = WindowManager.LayoutParams()
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
        params.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M)
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                else
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        getWindowManager().addView(button, params)
    }

    fun hide() {
        getWindowManager().removeView(button)
    }
}