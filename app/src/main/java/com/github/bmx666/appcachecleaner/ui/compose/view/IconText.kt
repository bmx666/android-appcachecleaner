package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.github.bmx666.appcachecleaner.ui.theme.MaterialIconsFont

@Composable
fun IconText(
    iconCode: String,
    modifier: Modifier = Modifier)
{
    Text(
        modifier = modifier,
        text = iconCode,
        style = TextStyle(
            fontFamily = MaterialIconsFont,
            fontSize = 24.sp // Adjust size as needed
        )
    )
}
