package com.photogridplanner.ui.startup

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
import androidx.compose.ui.unit.dp
import com.photogridplanner.ui.theme.StartupBackdrop
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val MarkBackground = Color(0xFFF8F7F4)
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

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                progress.animateTo(1f, animationSpec = tween(880, easing = FastOutSlowInEasing))
            }
            launch {
                markAlpha.animateTo(1f, animationSpec = tween(190, easing = FastOutSlowInEasing))
            }
            launch {
                markScale.animateTo(1.025f, animationSpec = tween(540, easing = FastOutSlowInEasing))
                markScale.animateTo(1f, animationSpec = tween(260, easing = FastOutSlowInEasing))
            }
            launch {
                markLift.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing))
            }
        }
        delay(160)
        visible = false
        delay(220)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 1.015f, animationSpec = tween(200, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StartupBackdrop),
            contentAlignment = Alignment.Center,
        ) {
            VectorGridMark(
                progress = progress.value,
                modifier = Modifier
                    .size(156.dp)
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
        val boardInset = size.minDimension * 0.03f
        val boardSize = size.minDimension - boardInset * 2f
        val boardCorner = boardSize * 0.23f
        val boardOffset = Offset(boardInset, boardInset)
        drawRoundRect(
            color = MarkBackground,
            topLeft = boardOffset,
            size = Size(boardSize, boardSize),
            cornerRadius = CornerRadius(boardCorner, boardCorner),
        )

        val gridInset = boardSize * 0.17f
        val gridGap = boardSize * 0.072f
        val cellSize = (boardSize - gridInset * 2f - gridGap * 2f) / 3f
        val strokeWidth = (boardSize * 0.048f).coerceAtLeast(2f)
        val cellCorner = cellSize * 0.20f
        val gridLeft = boardOffset.x + gridInset
        val gridTop = boardOffset.y + gridInset

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
