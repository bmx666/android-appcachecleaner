package com.github.bmx666.appcachecleaner.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.annotation.UiContext
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LauncherActivity: AppCompatActivity() {

    private var jobLaunch: Job? = null
    private var nightMode: Boolean = false
    private var firstBoot: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jobLaunch?.cancel()
        jobLaunch = CoroutineScope(Dispatchers.IO).launch {
            nightMode =
                SharedPreferencesManager.UI.getNightMode(this@LauncherActivity)
            firstBoot =
                SharedPreferencesManager.FirstBoot.showFirstBootConfirmation(this@LauncherActivity)
        }
        jobLaunch?.invokeOnCompletion {
            CoroutineScope(Dispatchers.Main).launch {
                setNightMode()
                startIntent()
            }
        }
    }

    @UiContext
    @UiThread
    private fun setNightMode() {
        if (nightMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    @UiContext
    @UiThread
    private fun startIntent() {
        val intent = Intent(
            this,
            // It's the first boot, show FirstBootActivity
            if (!BuildConfig.DEBUG && firstBoot)
                FirstBootActivity::class.java
            // Not the first boot, show AppCacheCleanerActivity
            else AppCacheCleanerActivity::class.java
        )

        startActivity(intent)

        // Close this activity so it's not in the back stack
        finishAfterTransition()
    }
}