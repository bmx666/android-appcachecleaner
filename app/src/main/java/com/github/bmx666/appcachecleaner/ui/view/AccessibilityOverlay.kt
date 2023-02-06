package com.github.bmx666.appcachecleaner.ui.view

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageButton
import com.github.bmx666.appcachecleaner.R

class AccessibilityOverlay(
    private val callback: () -> Unit
) {

    private var imageButton: ImageButton? = null

    private fun getImageButton(context: Context): ImageButton? {
        return LayoutInflater.from(context).inflate(R.layout.accessibility_overlay_button, null)
                as ImageButton?
    }

    private fun getWindowManager(context: Context): WindowManager? {
        return context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager?
    }

    fun show(context: Context) {
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
            getImageButton(context)?.let { button ->
                button.setOnClickListener { callback.invoke() }
                imageButton = button
                getWindowManager(context)?.addView(imageButton, params)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            imageButton = null
        }
    }

    fun hide(context: Context) {
        imageButton ?: return
        try {
            getWindowManager(context)?.removeView(imageButton)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageButton = null
        }
    }
}