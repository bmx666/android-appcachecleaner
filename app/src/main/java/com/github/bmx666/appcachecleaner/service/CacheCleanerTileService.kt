package com.github.bmx666.appcachecleaner.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.activity.AppCacheCleanerActivity
import com.github.bmx666.appcachecleaner.ui.dialog.PermissionDialogBuilder
import com.github.bmx666.appcachecleaner.util.PermissionChecker
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.N)
class CacheCleanerTileService: TileService() {

    override fun onStartListening() {
        super.onStartListening()

        val state: Int
        val subtitle: String?

        if (!PermissionChecker.checkUsageStatsPermission(this)) {
            state = Tile.STATE_INACTIVE
            subtitle = getString(R.string.text_enable_usage_stats_title)
        } else if (!PermissionChecker.checkAccessibilityPermission(this)) {
            state = Tile.STATE_INACTIVE
            subtitle = getString(R.string.text_enable_accessibility_title)
        } else {
            state = Tile.STATE_ACTIVE
            subtitle = null
        }

        qsTile.label = getString(R.string.tile_name)
        qsTile.contentDescription = getString(R.string.tile_name)
        qsTile.state = state
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            qsTile.subtitle = subtitle
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        if (!PermissionChecker.checkUsageStatsPermission(this)) {
            showDialog(PermissionDialogBuilder.buildUsageStatsPermissionDialog(this))
        } else if (!PermissionChecker.checkAccessibilityPermission(this)) {
            showDialog(PermissionDialogBuilder.buildAccessibilityPermissionDialog(this))
        } else {
            val intent = Intent(this, AppCacheCleanerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    ))
            else
                startActivityAndCollapse(intent)
        }
    }
}