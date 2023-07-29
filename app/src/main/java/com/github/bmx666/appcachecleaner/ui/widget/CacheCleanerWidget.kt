package com.github.bmx666.appcachecleaner.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.RemoteViews
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.service.WidgetBridgeService

class CacheCleanerWidget : AppWidgetProvider() {

    private var bridgeService: WidgetBridgeService? = null
    private var isBridgeServiceBound = false

    private val bridgeServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as WidgetBridgeService.WidgetBridgeBinder
            bridgeService = binder.getService()
            isBridgeServiceBound = true
        }

        override fun onServiceDisconnected(className: ComponentName) {
            isBridgeServiceBound = false
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // Bind to the service
        val intent = Intent(context, WidgetBridgeService::class.java)
        context.bindService(intent, bridgeServiceConnection, Context.BIND_AUTO_CREATE)

        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        // Unbind from the service
        if (isBridgeServiceBound) {
            context?.unbindService(bridgeServiceConnection)
            isBridgeServiceBound = false
        }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // Handle any widget click events here, if applicable.
        // For example, if you have a button in the widget, you can call the service function when the button is clicked.
        if (intent?.action == "Your_Custom_Action") {
            // Call the service function from the widget.
            if (isBridgeServiceBound && bridgeService != null) {
                bridgeService?.start()
            }
        }
        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val widgetText = context.getString(R.string.widget_text)

    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.cache_cleaner_widget)
    views.setTextViewText(R.id.widget_text, widgetText)

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}