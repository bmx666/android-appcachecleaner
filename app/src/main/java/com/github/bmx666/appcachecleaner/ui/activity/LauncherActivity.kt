package com.github.bmx666.appcachecleaner.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager

class LauncherActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(
            Intent(this,
                // It's the first boot, show FirstBootActivity
                if (!BuildConfig.DEBUG
                    && SharedPreferencesManager
                        .FirstBoot
                        .showFirstBootConfirmation(this))
                    FirstBootActivity::class.java
                // Not the first boot, show AppCacheCleanerActivity
                else AppCacheCleanerActivity::class.java
            )
        )

        // Close this activity so it's not in the back stack
        finishAfterTransition()
    }
}