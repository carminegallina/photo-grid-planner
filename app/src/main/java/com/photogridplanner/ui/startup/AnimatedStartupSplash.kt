package com.photogridplanner.ui.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.photogridplanner.R
import com.photogridplanner.ui.theme.StartupBackdrop
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A short, local startup animation that lets the app icon settle into the interface. */
@Composable
fun AnimatedStartupSplash(
    onFinished: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val logoScale = remember { Animatable(0.78f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoLift = remember { Animatable(14f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                logoAlpha.animateTo(1f, animationSpec = tween(280, easing = FastOutSlowInEasing))
            }
            launch {
                logoScale.animateTo(1.04f, animationSpec = tween(440, easing = FastOutSlowInEasing))
                logoScale.animateTo(1f, animationSpec = tween(380, easing = FastOutSlowInEasing))
            }
            launch {
                logoLift.animateTo(0f, animationSpec = tween(620, easing = FastOutSlowInEasing))
            }
        }
        delay(420)
        visible = false
        delay(260)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut(tween(220, easing = FastOutSlowInEasing)) +
            scaleOut(targetScale = 1.03f, animationSpec = tween(240, easing = FastOutSlowInEasing)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StartupBackdrop),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_art),
                contentDescription = null,
                modifier = Modifier
                    .size(148.dp)
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        translationY = logoLift.value
                    },
            )
        }
    }
}
