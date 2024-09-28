package com.github.bmx666.appcachecleaner.ui.compose.packagelist

import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.bmx666.appcachecleaner.R
import com.github.bmx666.appcachecleaner.placeholder.PlaceholderContent
import com.github.bmx666.appcachecleaner.ui.compose.view.AppIcon
import com.github.bmx666.appcachecleaner.ui.compose.view.LabelledCheckBox
import com.github.bmx666.appcachecleaner.util.toFormattedString
import kotlinx.coroutines.runBlocking
import org.springframework.util.unit.DataSize

@Composable
internal fun PackageListPackageList(
    pkgList: List<PlaceholderContent.PlaceholderPackage>,
    showCacheSize: Boolean,
    onAppIconClick: () -> Unit,
    onAppIconLongClick: (String) -> Unit,
    onPackageClick: (PlaceholderContent.PlaceholderPackage, Boolean) -> Unit,
) {
    when {
        pkgList.isEmpty() ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Column (
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = stringResource(id = R.string.message_no_matches_found))
                }
            }
        else ->
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(pkgList) { pkg ->
                    ListItem(
                        pkg = pkg,
                        showCacheSize = showCacheSize,
                        onAppIconClick = onAppIconClick,
                        onAppIconLongClick = onAppIconLongClick,
                        onPackageClick = onPackageClick,
                    )
                }
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListItem(
    pkg: PlaceholderContent.PlaceholderPackage,
    showCacheSize: Boolean,
    onAppIconClick: () -> Unit,
    onAppIconLongClick: (String) -> Unit,
    onPackageClick: (PlaceholderContent.PlaceholderPackage, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var isChecked by remember(pkg.pkgInfo.packageName) { mutableStateOf(pkg.checked) }

    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Surface(
            modifier = Modifier
                .align(alignment = Alignment.CenterVertically)
                .combinedClickable(
                    onClick = { onAppIconClick() },
                    onLongClick = { onAppIconLongClick(pkg.pkgInfo.packageName) },
                    indication = ripple(),
                    interactionSource = remember { MutableInteractionSource() }
                )
        ) {
            AppIcon(
                modifier = Modifier.align(
                    alignment = Alignment.CenterVertically
                ),
                pkgInfo = pkg.pkgInfo,
            )
        }
        Surface(
            onClick = {
                isChecked = !isChecked
                onPackageClick(pkg, isChecked)
            },
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) {
                LabelledCheckBox(
                    text = pkg.label,
                    textStyle = MaterialTheme.typography.titleMedium,
                    checked = isChecked,
                    enabled = true,
                    onCheckedChange = { checked ->
                        isChecked = checked
                        onPackageClick(pkg, isChecked)
                    }
                )
                Text(
                    text = pkg.pkgInfo.packageName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showCacheSize && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
                    val sizeStr = runBlocking {
                        DataSize.ofBytes(pkg.getCacheSize()).toFormattedString(context)
                    }
                    Text(
                        text = stringResource(id = R.string.text_cache_size_fmt, sizeStr),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
