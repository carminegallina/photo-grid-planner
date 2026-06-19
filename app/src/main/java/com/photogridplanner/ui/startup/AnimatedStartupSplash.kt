package com.photogridplanner.ui.startup

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MarkStroke = Color(0xFF202124)
private val MarkGradient = listOf(
    Color(0xFFFFB000),
    Color(0xFFFF6A43),
    Color(0xFFFF2F78),
    Color(0xFFC721B8),
)

/**
 * A Compose-drawn version of the launcher mark. Each outlined cell arrives in sequence,
 * then the colored centre cell completes the grid before the app becomes interactive.
 */
@Composable
fun AnimatedStartupSplash(
    onFinished: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val progress = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.84f) }
    val markAlpha = remember { Animatable(0f) }
    val markLift = remember { Animatable(18f) }
    val view = LocalView.current

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.statusBarColor = AndroidColor.WHITE
        window?.navigationBarColor = AndroidColor.WHITE
        window?.let { target ->
            WindowInsetsControllerCompat(target, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
        onDispose {
            window?.statusBarColor = AndroidColor.rgb(13, 15, 20)
            window?.navigationBarColor = AndroidColor.rgb(13, 15, 20)
            window?.let { target ->
                WindowInsetsControllerCompat(target, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                progress.animateTo(1f, animationSpec = tween(1_400, easing = FastOutSlowInEasing))
            }
            launch {
                markAlpha.animateTo(1f, animationSpec = tween(240, easing = FastOutSlowInEasing))
            }
            launch {
                markScale.animateTo(1.025f, animationSpec = tween(800, easing = FastOutSlowInEasing))
                markScale.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
            }
            launch {
                markLift.animateTo(0f, animationSpec = tween(700, easing = FastOutSlowInEasing))
            }
        }
        delay(280)
        visible = false
        delay(280)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(240, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 1.015f, animationSpec = tween(260, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            VectorGridMark(
                progress = progress.value,
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        alpha = markAlpha.value
                        scaleX = markScale.value
                        scaleY = markScale.value
                        translationY = markLift.value
                    },
            )
        }
    }
}

@Composable
private fun VectorGridMark(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val markSize = size.minDimension
        val gridInset = markSize * 0.08f
        val gridGap = markSize * 0.075f
        val cellSize = (markSize - gridInset * 2f - gridGap * 2f) / 3f
        val strokeWidth = (markSize * 0.028f).coerceAtLeast(2f)
        val cellCorner = cellSize * 0.20f
        val gridLeft = (size.width - markSize) / 2f + gridInset
        val gridTop = (size.height - markSize) / 2f + gridInset

        repeat(9) { index ->
            val row = index / 3
            val column = index % 3
            val reveal = ((progress - index * 0.065f) / 0.34f).coerceIn(0f, 1f)
            val eased = FastOutSlowInEasing.transform(reveal)
            val scaledCell = cellSize * (0.76f + eased * 0.24f)
            val cellLeft = gridLeft + column * (cellSize + gridGap) + (cellSize - scaledCell) / 2f
            val cellTop = gridTop + row * (cellSize + gridGap) + (cellSize - scaledCell) / 2f
            val cellRectSize = Size(scaledCell, scaledCell)
            val corner = CornerRadius(cellCorner * eased, cellCorner * eased)

            if (index == 4) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = MarkGradient,
                        start = Offset(cellLeft, cellTop),
                        end = Offset(cellLeft + scaledCell, cellTop + scaledCell),
                    ),
                    topLeft = Offset(cellLeft, cellTop),
                    size = cellRectSize,
                    cornerRadius = corner,
                    style = Stroke(width = strokeWidth * eased),
                    alpha = eased,
                )
            } else {
                drawRoundRect(
                    color = MarkStroke,
                    topLeft = Offset(cellLeft, cellTop),
                    size = cellRectSize,
                    cornerRadius = corner,
                    style = Stroke(width = strokeWidth * eased),
                    alpha = eased,
                )
            }
        }
    }
}
