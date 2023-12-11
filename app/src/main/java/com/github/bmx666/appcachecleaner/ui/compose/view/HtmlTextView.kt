package com.github.bmx666.appcachecleaner.ui.compose.view

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.core.text.method.LinkMovementMethodCompat

@Composable
internal fun HtmlTextView(text: String, hasLink: Boolean = false) {
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