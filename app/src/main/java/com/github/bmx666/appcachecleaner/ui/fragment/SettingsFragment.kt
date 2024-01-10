package com.github.bmx666.appcachecleaner.ui.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.activity.AppCacheCleanerActivity
import com.github.bmx666.appcachecleaner.ui.dialog.CustomListDialogBuilder
import com.github.bmx666.appcachecleaner.util.LocaleHelper
import java.util.*

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        val context = requireContext()
        val locale = LocaleHelper.getCurrentLocale(context)

        initializeUiNightMode(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_ui_night_mode))
        )

        initializeSettingsMaxWaitTimeout(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_settings_max_wait_app_timeout)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_settings_max_wait_clear_cache_btn_timeout)),
            context,
            { SharedPreferencesManager.Settings.getMaxWaitAppTimeout(context) },
            { timeout -> SharedPreferencesManager.Settings.setMaxWaitAppTimeout(context, timeout) },
            { SharedPreferencesManager.Settings.getMaxWaitClearCacheButtonTimeout(context) },
            { timeout -> SharedPreferencesManager.Settings.setMaxWaitClearCacheButtonTimeout(context, timeout) }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initializeFilterMinCacheSize(
                preferenceManager.findPreference(
                    context.getString(R.string.prefs_key_filter_min_cache_size)),
                context,
                { SharedPreferencesManager.Filter.getMinCacheSize(context) },
                { str ->
                    val value =
                        try { str.trim().toLong() }
                        catch (e: NumberFormatException) { -1L }
                    if (value > 0L) {
                        SharedPreferencesManager.Filter.saveMinCacheSize(context, (value * 1024L * 1024L))
                    } else if (value == 0L) {
                        SharedPreferencesManager.Filter.removeMinCacheSize(context)
                    } else {
                        Toast.makeText(
                            context,
                            context.getText(R.string.prefs_error_convert_filter_min_cache_size),
                            Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )
        }

        initializeExtraSearchText(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_clear_cache)),
            context, locale,
            context.getText(R.string.clear_cache_btn_text),
            { SharedPreferencesManager.ExtraSearchText.getClearCache(context, locale) },
            { value ->
                if (value.trim().isEmpty())
                    SharedPreferencesManager.ExtraSearchText.removeClearCache(context, locale)
                else
                    SharedPreferencesManager.ExtraSearchText.saveClearCache(context, locale, value)
            }
        )

        initializeExtraSearchText(
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_storage)),
            context, locale,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                    context.getText(R.string.storage_settings_for_app)
                else -> context.getText(R.string.storage_label)
            },
            { SharedPreferencesManager.ExtraSearchText.getStorage(context, locale) },
            { value ->
                if (value.trim().isEmpty())
                    SharedPreferencesManager.ExtraSearchText.removeStorage(context, locale)
                else
                    SharedPreferencesManager.ExtraSearchText.saveStorage(context, locale, value)
            }
        )

        initializeCustomList(
            context,
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_custom_list_add)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_custom_list_edit)),
            preferenceManager.findPreference(
                context.getString(R.string.prefs_key_custom_list_remove)),
        )
    }

    private fun initializeUiNightMode(pref: Preference?) {
        pref?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val context = requireContext()
                val nightMode =
                    if (SharedPreferencesManager.UI.getNightMode(context))
                        AppCompatDelegate.MODE_NIGHT_YES
                    else
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                AppCompatDelegate.setDefaultNightMode(nightMode)
                true
            }
        }
    }

    private fun initializeSettingsMaxWaitTimeout(
        prefMaxWaitAppTimeout: SeekBarPreference?,
        prefMaxWaitClearCacheButtonTimeout: SeekBarPreference?,
        context: Context,
        getMaxWaitAppTimeout: () -> Int,
        setMaxWaitAppTimeout: (Int) -> Unit,
        getMaxWaitClearCacheButtonTimeout: () -> Int,
        setMaxWaitClearCacheButtonTimeout: (Int) -> Unit)
    {
        prefMaxWaitAppTimeout?.apply {
            min = Constant.Settings.CacheClean.MIN_WAIT_APP_PERFORM_CLICK_MS / 1000
            max = Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS * 2 / 1000
            setDefaultValue(Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000)
            value = getMaxWaitAppTimeout()
            if (value < Constant.Settings.CacheClean.MIN_WAIT_APP_PERFORM_CLICK_MS / 1000)
                value = Constant.Settings.CacheClean.DEFAULT_WAIT_APP_PERFORM_CLICK_MS / 1000
            showSeekBarValue = true
            title = context.getString(R.string.prefs_title_max_wait_app_timeout, value)
            setOnPreferenceChangeListener { _, newValue ->
                title = context.getString(R.string.prefs_title_max_wait_app_timeout, newValue as Int)
                setMaxWaitAppTimeout(newValue)
                // shift Clear Cache button timeout
                if (newValue < getMaxWaitClearCacheButtonTimeout())
                    setMaxWaitClearCacheButtonTimeout(newValue - 1)
                prefMaxWaitClearCacheButtonTimeout?.apply {
                    max = getMaxWaitAppTimeout() - 1
                    value = getMaxWaitClearCacheButtonTimeout()
                    title = context.getString(R.string.prefs_title_max_wait_clear_cache_btn_timeout, value)
                }
                true
            }
        }

        prefMaxWaitClearCacheButtonTimeout?.apply {
            min = Constant.Settings.CacheClean.MIN_WAIT_CLEAR_CACHE_BUTTON_MS / 1000
            max = getMaxWaitAppTimeout() - 1
            setDefaultValue(Constant.Settings.CacheClean.DEFAULT_WAIT_CLEAR_CACHE_BUTTON_MS / 1000)
            value = getMaxWaitClearCacheButtonTimeout()
            if (value >= getMaxWaitAppTimeout() && value > 0)
                value = getMaxWaitAppTimeout() - 1
            showSeekBarValue = true
            title = context.getString(R.string.prefs_title_max_wait_clear_cache_btn_timeout, value)
            setOnPreferenceChangeListener { _, newValue ->
                title = context.getString(R.string.prefs_title_max_wait_clear_cache_btn_timeout, newValue as Int)
                setMaxWaitClearCacheButtonTimeout(newValue)
                true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeFilterMinCacheSize(pref: EditTextPreference?,
                                          context: Context,
                                          getMinCacheSize: () -> Long,
                                          onChangeMinCacheSize: (String) -> Unit) {
        val showSummary = {
            val value = getMinCacheSize()
            if (value != 0L)
                context.getString(R.string.prefs_summary_filter_min_cache_size,
                    (value / (1024f * 1024f)).toInt())
            else
                null
        }

        pref?.apply {
            dialogTitle = context.getString(R.string.dialog_filter_min_cache_size_message)
            summary = showSummary()
            setSummaryProvider {
                showSummary()
            }
            setOnBindEditTextListener { editText ->
                val value = getMinCacheSize()
                if (value != 0L) {
                    editText.setText(String.format("%d", (value / (1024f * 1024f)).toInt()))
                    editText.hint = null
                } else {
                    editText.text = null
                    editText.hint = "0"
                }
            }
            setOnPreferenceChangeListener { _, newValue ->
                onChangeMinCacheSize(newValue as String)
                true
            }
        }
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

    private fun initializeCustomList(context: Context,
                                     addPref: Preference?,
                                     editPref: Preference?,
                                     removePref: Preference?) {
        SharedPreferencesManager.PackageList.getNames(context).apply {
            editPref?.isVisible = isNotEmpty()
            removePref?.isVisible = isNotEmpty()
        }

        addPref?.apply {
            setOnPreferenceClickListener {
                // show dialog from Settings Fragment for better UX
                CustomListDialogBuilder.buildAddDialog(context) { name ->
                    name ?: return@buildAddDialog

                    // check if entered name already exists
                    val names = SharedPreferencesManager.PackageList.getNames(context)
                    if (names.contains(name)) {
                        Toast.makeText(context,
                            R.string.toast_custom_list_add_already_exists,
                            Toast.LENGTH_SHORT).show()
                        return@buildAddDialog
                    }
                    (activity as AppCacheCleanerActivity?)?.showCustomListPackageFragment(name)
                }.show()
                true
            }
        }

        editPref?.apply {
            setOnPreferenceClickListener {
                // show dialog from Settings Fragment for better UX
                CustomListDialogBuilder.buildEditDialog(context) { name ->
                    name ?: return@buildEditDialog

                    // check if entered name already exists
                    val names = SharedPreferencesManager.PackageList.getNames(context)
                    if (names.contains(name))
                        (activity as AppCacheCleanerActivity?)?.showCustomListPackageFragment(name)
                }.show()
                true
            }
        }

        removePref?.apply {
            setOnPreferenceClickListener {
                // show dialog from Settings Fragment for better UX
                CustomListDialogBuilder.buildRemoveDialog(context) { name ->
                    name?.let {
                        SharedPreferencesManager.PackageList.remove(context, name)
                        SharedPreferencesManager.PackageList.getNames(context).apply {
                            editPref?.isVisible = isNotEmpty()
                            removePref?.isVisible = isNotEmpty()
                        }
                        Toast.makeText(context,
                            getString(R.string.toast_custom_list_has_been_removed, name),
                            Toast.LENGTH_SHORT).show()
                    }
                }.show()
                true
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}