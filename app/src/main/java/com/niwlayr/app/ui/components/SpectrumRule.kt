package com.niwlayr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.niwlayr.app.ui.theme.SpectrumStops

/** The brand signature: a short spectrum hairline placed under a screen title. */
@Composable
fun SpectrumRule(
    modifier: Modifier = Modifier,
    width: Dp = 58.dp,
) {
    Box(
        modifier = modifier
            .width(width)
            .height(3.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Brush.horizontalGradient(SpectrumStops)),
    )
}
