package com.github.bmx666.appcachecleaner.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.activity.AppCacheCleanerActivity
import com.github.bmx666.appcachecleaner.ui.dialog.PermissionDialogBuilder
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class CacheCleanerTileService: TileService() {

    private var jobGetPermission: Job? = null
    private var jobClick: Job? = null
    private var hasUsageStatsPermission = false
    private var hasAccessibilityPermission = false

    override fun onStartListening() {
        super.onStartListening()

        jobGetPermission?.cancel()
        jobGetPermission = CoroutineScope(Dispatchers.IO).launch {
            requestPermissions(this@CacheCleanerTileService)
        }
        jobGetPermission?.invokeOnCompletion {
            val state: Int
            val subtitle: String?

            if (!hasUsageStatsPermission) {
                state = Tile.STATE_INACTIVE
                subtitle = getString(R.string.text_enable_usage_stats_title)
            } else if (!hasAccessibilityPermission) {
                state = Tile.STATE_INACTIVE
                subtitle = getString(R.string.text_enable_accessibility_title)
            } else {
                state = Tile.STATE_ACTIVE
                subtitle = null
            }

            CoroutineScope(Dispatchers.Main).launch {
                qsTile.label = getString(R.string.tile_name)
                qsTile.contentDescription = getString(R.string.tile_name)
                qsTile.state = state
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    qsTile.subtitle = subtitle
                qsTile.updateTile()
            }
        }
    }

    override fun onStopListening() {
        jobGetPermission?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        jobClick?.cancel()
        jobClick = CoroutineScope(Dispatchers.IO).launch {
            requestPermissions(this@CacheCleanerTileService)
        }
        jobClick?.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                processOnClick(this@CacheCleanerTileService)
            }
        }
    }

    private suspend fun requestPermissions(context: Context) {
        hasUsageStatsPermission =
            PermissionChecker.checkUsageStatsPermission(context)
        hasAccessibilityPermission =
            PermissionChecker.checkAccessibilityPermission(context)
    }

    @UiThread
    @UiContext
    private fun processOnClick(context: Context) {
        if (!hasUsageStatsPermission)
            showDialog(PermissionDialogBuilder.buildUsageStatsPermissionDialog(context))
        else if (!hasAccessibilityPermission)
            showDialog(PermissionDialogBuilder.buildAccessibilityPermissionDialog(context))
        else
            showApp(context)
    }

    @UiThread
    @UiContext
    private fun showApp(context: Context) {
        val intent = Intent(context, AppCacheCleanerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                ))
        else
            startActivityAndCollapse(intent)
    }
}