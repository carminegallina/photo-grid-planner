package com.photogridplanner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photogridplanner.ui.analysis.FeedAnalysisScreen
import com.photogridplanner.ui.calendar.CalendarScreen
import com.photogridplanner.ui.cutter.CutterScreen
import com.photogridplanner.ui.grid.GridScreen
import com.photogridplanner.ui.settings.SettingsScreen
import com.photogridplanner.viewmodel.PlannerViewModel

private enum class Destination(val label: String) {
    Grid("Griglia"),
    Calendar("Agenda"),
    Analysis("Analisi"),
    Cutter("Cutter"),
    Settings("Impost."),
}

private val Destination.index: Int
    get() = when (this) {
        Destination.Grid -> 0
        Destination.Analysis -> 1
        Destination.Calendar -> 2
        Destination.Cutter -> 3
        Destination.Settings -> 4
    }

@Composable
fun PhotoGridPlannerApp(viewModel: PlannerViewModel) {
    val state by viewModel.state.collectAsState()
    var currentDestination by rememberSaveable { mutableStateOf(Destination.Grid) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                tonalElevation = 0.dp,
            ) {
                NavigationBar(
                    modifier = Modifier.height(66.dp),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                ) {
                    AppNavItem(
                        selected = currentDestination == Destination.Grid,
                        onClick = { currentDestination = Destination.Grid },
                        icon = Icons.Rounded.ViewModule,
                        label = Destination.Grid.label,
                    )
                    AppNavItem(
                        selected = currentDestination == Destination.Analysis,
                        onClick = { currentDestination = Destination.Analysis },
                        icon = Icons.Rounded.Analytics,
                        label = Destination.Analysis.label,
                    )
                    AppNavItem(
                        selected = currentDestination == Destination.Calendar,
                        onClick = { currentDestination = Destination.Calendar },
                        icon = Icons.Rounded.DateRange,
                        label = Destination.Calendar.label,
                    )
                    AppNavItem(
                        selected = currentDestination == Destination.Cutter,
                        onClick = { currentDestination = Destination.Cutter },
                        icon = Icons.Rounded.ContentCut,
                        label = Destination.Cutter.label,
                    )
                    AppNavItem(
                        selected = currentDestination == Destination.Settings,
                        onClick = { currentDestination = Destination.Settings },
                        icon = Icons.Rounded.Settings,
                        label = Destination.Settings.label,
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = currentDestination,
            label = "screen_transition",
            transitionSpec = {
                val direction = if (targetState.index > initialState.index) 1 else -1
                (
                    fadeIn(animationSpec = tween(240, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth / 9 * direction },
                        )
                    ).togetherWith(
                        fadeOut(animationSpec = tween(190, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(
                                animationSpec = tween(260, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth / 12 * direction },
                            ),
                    ).using(SizeTransform(clip = false))
            },
        ) { destination ->
            when (destination) {
                Destination.Grid -> GridScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                )

                Destination.Calendar -> CalendarScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                )

                Destination.Analysis -> FeedAnalysisScreen(
                    state = state,
                    modifier = Modifier.padding(padding),
                )

                Destination.Cutter -> CutterScreen(
                    modifier = Modifier.padding(padding),
                )

                Destination.Settings -> SettingsScreen(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun RowScope.AppNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        label = { NavLabel(label) },
        alwaysShowLabel = selected,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun NavLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
    )
}
