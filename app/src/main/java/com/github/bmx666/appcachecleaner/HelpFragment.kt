package com.github.bmx666.appcachecleaner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.fragment.app.Fragment
import com.github.bmx666.appcachecleaner.databinding.FragmentHelpBinding


class HelpFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentHelpBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.textHelpAbout.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_about),
            FROM_HTML_MODE_COMPACT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            binding.textHelpAndroid13AccessibilityPermission.text = HtmlCompat.fromHtml(
                resources.getString(R.string.help_android13_accessibility_permission),
                FROM_HTML_MODE_COMPACT)
            binding.textHelpAndroid13AccessibilityPermission.visibility = View.VISIBLE

            binding.btnHelpOpenAppSettings.setOnClickListener {
                context?.packageName.let { packageName ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
            binding.btnHelpOpenAppSettings.visibility = View.VISIBLE
        }

        binding.textHelpHowToUse.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_how_to_use),
            FROM_HTML_MODE_COMPACT)

        binding.textHelpCustomizedSettingsUI.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_customized_settings_ui),
            FROM_HTML_MODE_COMPACT)

        binding.textHelpIconCopyright.text = HtmlCompat.fromHtml(
            resources.getString(R.string.help_icon_copyright),
            FROM_HTML_MODE_COMPACT)

        return view
    }

    companion object {
        @JvmStatic
        fun newInstance() = HelpFragment()
    }
}