package com.github.bmx666.appcachecleaner.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast

class WidgetBridgeService: Service() {

    inner class WidgetBridgeBinder : Binder() {
        fun getService(): WidgetBridgeService = this@WidgetBridgeService
    }

    private val binder = WidgetBridgeBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    fun start() {
        Toast.makeText(this, "START", Toast.LENGTH_SHORT).show()
    }
}