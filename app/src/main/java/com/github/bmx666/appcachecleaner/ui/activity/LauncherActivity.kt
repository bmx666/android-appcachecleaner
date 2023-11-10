package com.github.bmx666.appcachecleaner.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager

class LauncherActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SharedPreferencesManager.FirstBoot.showFirstBootConfirmation(this)) {
            // It's the first boot, show FirstBootActivity
            val intent = Intent(this, FirstBootActivity::class.java)
            startActivity(intent)
        } else {
            // Not the first boot, show AppCacheCleanerActivity
            val intent = Intent(this, AppCacheCleanerActivity::class.java)
            startActivity(intent)
        }

        // Close this activity so it's not in the back stack
        finishAfterTransition()
    }
}