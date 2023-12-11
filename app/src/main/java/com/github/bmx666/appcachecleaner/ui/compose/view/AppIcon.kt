package com.github.bmx666.appcachecleaner.ui.compose.view

import android.content.pm.PackageInfo
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.github.bmx666.appcachecleaner.util.PackageManagerHelper
import kotlinx.coroutines.runBlocking

@Composable
fun AppIcon(modifier: Modifier, pkgInfo: PackageInfo) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(runBlocking { PackageManagerHelper.getApplicationIcon(context, pkgInfo) })
        .build()

    AsyncImage(
        modifier = modifier.size(48.dp),
        model = request,
        contentDescription = null,
    )
}