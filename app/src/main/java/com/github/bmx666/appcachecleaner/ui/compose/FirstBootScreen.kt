package com.github.bmx666.appcachecleaner.ui.compose

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.const.Constant
import com.github.bmx666.appcachecleaner.ui.compose.view.CenterAlignedTopAppBar
import com.github.bmx666.appcachecleaner.ui.compose.view.HtmlTextView
import com.github.bmx666.appcachecleaner.ui.compose.view.StyledLabelledCheckBox
import com.github.bmx666.appcachecleaner.ui.viewmodel.FirstBootViewModel
import com.github.bmx666.appcachecleaner.util.charSequenceResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstBootScreen(navController: NavHostController) {
    val context: Context = LocalContext.current
    val activity = context as? Activity

    val firstBootViewModel: FirstBootViewModel = hiltViewModel()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val numberOfCheckboxes = 4
    val checkboxStates = remember { List(numberOfCheckboxes) { false }.toMutableStateList() }

    val isButtonOKEnabled = checkboxStates.all { it }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
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
                HtmlTextView(text = stringResource(id = R.string.first_boot_message))
                Spacer(modifier = Modifier.height(10.dp))

                @StringRes
                fun getLabelResourceId(index: Int): Int {
                    return when (index) {
                        0 -> R.string.first_boot_confirm_1
                        1 -> R.string.first_boot_confirm_2
                        2 -> R.string.first_boot_confirm_3
                        3 -> R.string.first_boot_confirm_4
                        else -> 0
                    }
                }

                checkboxStates.forEachIndexed { index, isChecked ->
                    val isEnabled = index == 0 || checkboxStates[index - 1]
                    StyledLabelledCheckBox(
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
                        onClick = { activity?.finish() },
                    ) {
                        Text(text = stringResource(id = android.R.string.cancel))
                    }
                    Button(
                        enabled = isButtonOKEnabled,
                        onClick = {
                            firstBootViewModel.setFirstBootCompleted()
                            navController.popBackStack()
                            navController.navigate(Constant.Navigation.HOME.name) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        },
                    ) {
                        Text(text = stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
    )
}