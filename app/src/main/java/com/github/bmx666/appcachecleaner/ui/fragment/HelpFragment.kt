package com.github.bmx666.appcachecleaner.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.databinding.FragmentHelpBinding
import com.github.bmx666.appcachecleaner.util.ActivityHelper

class HelpFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentHelpBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.textHelpAbout.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_about),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.textHelpAndroid13AccessibilityPermission.text = HtmlCompat.fromHtml(
                resources.getString(R.string.help_android13_accessibility_permission),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            binding.textHelpAndroid13AccessibilityPermission.visibility = View.VISIBLE

            binding.btnHelpOpenAppSettings.setOnClickListener {
                ActivityHelper.startApplicationDetailsActivity(context, context?.packageName)
            }
            binding.btnHelpOpenAppSettings.visibility = View.VISIBLE
        }

        binding.textHelpHowToUse.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_how_to_use),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.textHelpCustomizedSettingsUI.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_customized_settings_ui),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        binding.textHelpIconCopyright.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_icon_copyright),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }
}