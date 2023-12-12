package com.github.bmx666.appcachecleaner.ui.compose

import android.app.Activity
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.method.LinkMovementMethodCompat
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.BuildConfig
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.config.SharedPreferencesManager
import com.github.bmx666.appcachecleaner.const.Constant

@Composable
@ReadOnlyComposable
private fun charSequenceResource(@StringRes id: Int): CharSequence {
    val context = LocalContext.current
    val resources = context.resources
    return resources.getText(id)
}

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
private fun StyledText(text: CharSequence, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context -> TextView(context) },
        update = {
            it.text = text
        }
    )
}

@Composable
private fun LabelledCheckBox(
    text: CharSequence,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: ((Boolean) -> Unit),
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(
                enabled = enabled,
                indication = rememberRipple(color = MaterialTheme.colorScheme.primary),
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onCheckedChange(!checked) }
            )
            .fillMaxWidth()
            .defaultMinSize(minHeight = ButtonDefaults.MinHeight)
            .padding(4.dp),
    ) {
        Checkbox(
            checked = checked,
            enabled = enabled,
            onCheckedChange = null
        )
        Spacer(Modifier.size(10.dp))
        StyledText(text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstBootScreen(activity: Activity, navController: NavHostController) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val numberOfCheckboxes = if (BuildConfig.GOOGLEPLAY) 8 else 4
    val checkboxStates = remember { List(numberOfCheckboxes) { false }.toMutableStateList() }

    val isButtonOKEnabled = checkboxStates.all { it }

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
                        text = stringResource(R.string.first_boot_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                CreateHtmlTextView(text = stringResource(id = R.string.first_boot_message))
                Spacer(modifier = Modifier.height(10.dp))

                @StringRes
                fun getLabelResourceId(index: Int): Int {
                    return when (index) {
                        0 -> R.string.first_boot_confirm_1
                        1 -> R.string.first_boot_confirm_2
                        2 -> R.string.first_boot_confirm_3
                        3 -> R.string.first_boot_confirm_4
                        4 -> R.string.first_boot_confirm_googleplay_1
                        5 -> R.string.first_boot_confirm_googleplay_2
                        6 -> R.string.first_boot_confirm_googleplay_3
                        7 -> R.string.first_boot_confirm_googleplay_4
                        else -> 0
                    }
                }

                checkboxStates.forEachIndexed { index, isChecked ->
                    val isEnabled = index == 0 || checkboxStates[index - 1]
                    LabelledCheckBox(
                        text = charSequenceResource(id = getLabelResourceId(index)),
                        checked = isChecked,
                        enabled = isEnabled,
                        onCheckedChange = { checked ->
                            checkboxStates[index] = checked
                            if (!checked) {
                                for (i in index + 1 until numberOfCheckboxes) {
                                    checkboxStates[i] = false
                                }
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(
                        onClick = {
                            activity.finish()
                        }
                    ) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                    Button(
                        enabled = isButtonOKEnabled,
                        onClick = {
                            SharedPreferencesManager.FirstBoot.hideFirstBootConfirmation(activity)
                            navController.popBackStack()
                            navController.navigate(Constant.Navigation.HOME.name) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
    )
}