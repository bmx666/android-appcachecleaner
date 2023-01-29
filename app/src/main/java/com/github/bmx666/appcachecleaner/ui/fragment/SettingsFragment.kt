package com.github.bmx666.appcachecleaner.ui.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        val context = requireContext()
        val locale = LocaleHelper.getCurrentLocale(context)

        initializeExtraSearchText(
            preferenceManager.findPreference("clear_cache"),
            context, locale,
            context.getText(R.string.clear_cache_btn_text),
            { SharedPreferencesManager.ExtraSearchText.getClearCache(context, locale) },
            { value ->
                if (value.isEmpty() or value.trim().isEmpty())
                    SharedPreferencesManager.ExtraSearchText.removeClearCache(context, locale)
                else
                    SharedPreferencesManager.ExtraSearchText.saveClearCache(context, locale, value)
            }
        )

        initializeExtraSearchText(
            preferenceManager.findPreference("storage"),
            context, locale,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    context.getText(R.string.storage_settings_for_app)
                else -> context.getText(R.string.storage_label)
            },
            { SharedPreferencesManager.ExtraSearchText.getStorage(context, locale) },
            { value ->
                if (value.isEmpty() or value.trim().isEmpty())
                    SharedPreferencesManager.ExtraSearchText.removeStorage(context, locale)
                else
                    SharedPreferencesManager.ExtraSearchText.saveStorage(context, locale, value)
            }
        )
    }

    private fun initializeExtraSearchText(pref: EditTextPreference?,
                                          context: Context, locale: Locale,
                                          extraText: CharSequence,
                                          getExtraText: () -> String?,
                                          onChangeExtraText: (String) -> Unit) {
        pref?.apply {
            dialogTitle = context.getString(
                R.string.dialog_extra_search_text_message,
                locale.displayLanguage, locale.displayCountry,
                extraText)
            summary = getExtraText()
            setSummaryProvider {
                getExtraText()
            }
            setOnBindEditTextListener { editText ->
                val value = getExtraText()
                if (value?.isNotEmpty() == true) {
                    editText.setText(value)
                    editText.hint = null
                } else {
                    editText.text = null
                    editText.hint = extraText
                }
            }
            setOnPreferenceChangeListener { _, newValue ->
                onChangeExtraText(newValue as String)
                true
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}