package com.github.bmx666.appcachecleaner.ui.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.widget.NestedScrollView
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.databinding.ActivityFirstBootBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirstBootActivity: AppCompatActivity() {

    private lateinit var binding: ActivityFirstBootBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFirstBootBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        updateActionBarMenu(R.string.first_boot_title)

        binding.textMessage.text = HtmlCompat.fromHtml(
            resources.getString(R.string.first_boot_message),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.textConfirm1.setOnCheckedChangeListener { _, _ ->
            binding.textConfirm2.isEnabled = true
        }
        binding.textConfirm2.setOnCheckedChangeListener { _, _ ->
            binding.textConfirm3.isEnabled = true
        }
        binding.textConfirm3.setOnCheckedChangeListener { _, _ ->
            binding.textConfirm4.isEnabled = true
        }
        binding.textConfirm4.setOnCheckedChangeListener { _, _ ->
            binding.btnOk.isEnabled = true
        }

        binding.btnOk.setOnClickListener {
            checkConfirm()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun checkConfirm() {
        if (!verifyAllConfirmations())
            return

        CoroutineScope(Dispatchers.IO).launch {
            SharedPreferencesManager.FirstBoot.hideFirstBootConfirmation(this@FirstBootActivity)
        }

        val intent = Intent(this, AppCacheCleanerActivity::class.java)
        startActivity(intent)
        finishAfterTransition()
    }

    private fun updateActionBarMenu(@StringRes resId: Int) {
        supportActionBar?.setTitle(resId)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun verifyAllConfirmations(): Boolean {
        return listOf(
            binding.textConfirm1,
            binding.textConfirm2,
            binding.textConfirm3,
            binding.textConfirm4,
        ).all { verifyCheckbox(it, expected = true) }
    }

    private fun verifyCheckbox(checkBox: AppCompatCheckBox, expected: Boolean): Boolean {
        if (expected && checkBox.isChecked)
            return true

        if (!expected && !checkBox.isChecked)
            return true

        val originalTextColors = checkBox.textColors

        val errorTextColor = ContextCompat.getColor(this, R.color.error)
        checkBox.setTextColor(ColorStateList.valueOf(errorTextColor))

        // restore original text color back
        checkBox.postDelayed({ checkBox.setTextColor(originalTextColors) }, 2000)

        scrollToView(binding.nestedScrollView, checkBox)
        return false
    }

    private fun scrollToView(scrollView: NestedScrollView, view: View) {
        // Calculate the absolute coordinates of the view
        val coordinates = IntArray(2)
        view.getLocationInWindow(coordinates)
        // Use the coordinates to scroll to the view
        scrollView.smoothScrollTo(0, coordinates[1])
    }
}