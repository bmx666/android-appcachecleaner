package com.github.bmx666.appcachecleaner.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isOverlayShown = false
    private var activeJobCount = 0
    private val overlayLock = Any()
    private var showOverlayCallback: (() -> Unit)? = null
    private var hideOverlayCallback: (() -> Unit)? = null

    fun setShowOverlayCallback(callback: () -> Unit) {
        showOverlayCallback = callback
    }

    fun setHideOverlayCallback(callback: () -> Unit) {
        hideOverlayCallback = callback
    }

    @UiContext
    @UiThread
    private fun showOverlay() {
        synchronized(overlayLock) {
            if (!isOverlayShown) {
                isOverlayShown = true
                showOverlayCallback?.invoke()
            }
            activeJobCount++
        }
    }

    @UiContext
    @UiThread
    private fun hideOverlay() {
        synchronized(overlayLock) {
            activeJobCount--
            if (activeJobCount <= 0) {
                isOverlayShown = false
                hideOverlayCallback?.invoke()
            }
        }
    }

    fun addJob(callback: suspend () -> Unit) {
        showOverlay()
        CoroutineScope(Dispatchers.IO).launch {
            callback()
        }.invokeOnCompletion {
            hideOverlay()
        }
    }
}

