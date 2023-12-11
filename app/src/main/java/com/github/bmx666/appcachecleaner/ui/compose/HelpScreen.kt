package com.github.bmx666.appcachecleaner.ui.compose

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.ui.compose.view.CenterAlignedTopAppBar
import com.github.bmx666.appcachecleaner.ui.compose.view.GoBackIconButton
import com.github.bmx666.appcachecleaner.ui.compose.view.HtmlTextView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.menu_item_help),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    GoBackIconButton(navController)
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
                HtmlTextView(text = stringResource(R.string.help_about))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    HtmlTextView(text = stringResource(R.string.help_android13_accessibility_permission),
                        hasLink = true)
                HtmlTextView(text = stringResource(R.string.help_how_to_use))
                HtmlTextView(text = stringResource(R.string.help_customized_settings_ui))
                HtmlTextView(text = stringResource(R.string.help_icon_copyright),
                    hasLink = true)
                HtmlTextView(text = stringResource(R.string.help_submit_bug_report),
                    hasLink = true)
            }
        }
    )
}