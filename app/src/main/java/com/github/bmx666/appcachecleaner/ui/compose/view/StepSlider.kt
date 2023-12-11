package com.github.bmx666.appcachecleaner.ui.compose.view

import androidx.annotation.IntRange
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun StepSlider(
    onLabelUpdate: @Composable (Float) -> String,
    onSummaryUpdate: @Composable ((Float) -> String)?,
    value: Number?,
    valueRange: ClosedFloatingPointRange<Float>,
    @IntRange(from = 0)
    steps: Int = 0,
    onValueChangeFinished: ((Float) -> Unit)? = null,
) {
    value ?: return

    var sliderPosition by remember {
        mutableFloatStateOf(value.toFloat())
    }

    LaunchedEffect(value) {
        sliderPosition = value.toFloat()
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = onLabelUpdate(sliderPosition),
            style = MaterialTheme.typography.titleMedium
        )
        onSummaryUpdate?.let {
            Text(
                text = onSummaryUpdate(sliderPosition),
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Clip,
            )
        }
        Slider(
            modifier = Modifier.semantics {
                contentDescription = ""
            },
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = valueRange,
            onValueChangeFinished = {
                onValueChangeFinished?.invoke(sliderPosition)
            },
            steps = steps,
        )
    }
}