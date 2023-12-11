package com.github.bmx666.appcachecleaner.ui.compose

import android.os.Build
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.navigation.compose.rememberNavController
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R

@Composable
private fun CreateHtmlTextView(text: String, hasLink: Boolean = false) {
    val htmlDescription = remember(text) {
        HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    AndroidView(
        factory = { context ->
            TextView(context).apply {
                if (hasLink)
                    movementMethod = LinkMovementMethodCompat.getInstance()
            }
        },
        update = {
            it.text = htmlDescription
        }
    )
}

@Composable
private fun ComposeHelpAbout() {
    val context = LocalContext.current
    val htmlText = context.resources.getString(R.string.help_about)
    CreateHtmlTextView(text = htmlText)
}

@Composable
private fun ComposeHelpAndroid13AccessibilityPermission() {
    if (BuildConfig.GOOGLEPLAY) return
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val htmlText = context.resources.getString(R.string.help_android13_accessibility_permission)
    CreateHtmlTextView(text = htmlText)
}

@Composable
private fun ComposeHelpHowToUse() {
    val context = LocalContext.current
    val htmlText = context.resources.getString(R.string.help_how_to_use)
    CreateHtmlTextView(text = htmlText)
}

@Composable
private fun ComposeHelpCustomizedSettingsUI() {
    val context = LocalContext.current
    val htmlText = context.resources.getString(R.string.help_customized_settings_ui)
    CreateHtmlTextView(text = htmlText)
}

@Composable
private fun ComposeHelpIconCopyright() {
    val context = LocalContext.current
    val htmlText = context.resources.getString(R.string.help_icon_copyright)
    CreateHtmlTextView(text = htmlText, hasLink = true)
}

@Composable
private fun ComposeHelpSubmitBugReport() {
    val context = LocalContext.current
    val htmlText = context.resources.getString(R.string.help_submit_bug_report)
    CreateHtmlTextView(text = htmlText, hasLink = true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeHelp() {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = context.resources.getString(R.string.menu_item_help),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Localized description"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = 16.dp
                    )
                    .padding(horizontal = 16.dp)
            ) {
                ComposeHelpAbout()
                ComposeHelpAndroid13AccessibilityPermission()
                ComposeHelpHowToUse()
                ComposeHelpCustomizedSettingsUI()
                ComposeHelpIconCopyright()
                ComposeHelpSubmitBugReport()
            }
        }
    )
}