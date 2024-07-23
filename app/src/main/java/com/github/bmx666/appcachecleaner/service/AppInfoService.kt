package com.github.bmx666.appcachecleaner.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.log.Logger
import com.github.bmx666.appcachecleaner.ui.application.CacheCleanerApplication
import com.github.bmx666.appcachecleaner.util.ActivityHelper

class AppInfoService : Service() {

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {
            // Start the foreground service with a notification
            val notification =
                NotificationCompat.Builder(this, CacheCleanerApplication.CHANNEL_ID)
                    .setContentTitle("AppInfo Service")
                    .setContentText("Running...")
                    .setSmallIcon(R.drawable.ic_launcher_monochrome)
                    .build()

            val channelId = 1

            val foregroundServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
                }

            ServiceCompat.startForeground(
                this,
                channelId,
                notification,
                foregroundServiceType
            )

        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException) {
                e.message?.let { Logger.e("ForegroundServiceStartNotAllowedException: $it") }
            } else {
                e.message?.let { Logger.e(it) }
            }
        }

        val pkgName = intent.getStringExtra(Constant.Intent.CleanCacheAppInfo.NAME_PACKAGE_NAME)
        ActivityHelper.startApplicationDetailsActivity(this, pkgName)

        // Return START_NOT_STICKY if you don't want the service to be recreated
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
