package com.photogridplanner.ui

import com.photogridplanner.ui.i18n.LocalizedText

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.photogridplanner.ui.analysis.FeedAnalysisScreen
import com.photogridplanner.ui.calendar.CalendarScreen
import com.photogridplanner.ui.cutter.CutterScreen
import com.photogridplanner.ui.grid.GridScreen
import com.photogridplanner.ui.i18n.LocalAppStrings
import com.photogridplanner.ui.i18n.appStringsFor
import com.photogridplanner.ui.settings.SettingsScreen
import com.photogridplanner.ui.startup.AnimatedStartupSplash
import com.photogridplanner.ui.tutorial.AppTutorialDialog
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
    var tutorialDismissedThisLaunch by rememberSaveable { mutableStateOf(false) }
    var forceTutorial by rememberSaveable { mutableStateOf(false) }
    var showStartupAnimation by remember { mutableStateOf(true) }
    val shouldRequestInitialPermissions = !showStartupAnimation && !state.initialPermissionPromptCompleted

    CompositionLocalProvider(
        LocalAppStrings provides appStringsFor(state.language),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onShowTutorial = { forceTutorial = true },
                    )
                    }
                }
            }

            if (shouldRequestInitialPermissions) {
                InitialPermissionRequester(
                    onFinished = { viewModel.setInitialPermissionPromptCompleted() },
                )
            }

            if (!showStartupAnimation && !shouldRequestInitialPermissions &&
                ((state.showTutorialOnLaunch && !tutorialDismissedThisLaunch) || forceTutorial)
            ) {
                AppTutorialDialog(
                    onClose = { dontShowAgain ->
                        tutorialDismissedThisLaunch = true
                        forceTutorial = false
                        if (dontShowAgain) {
                            viewModel.setShowTutorialOnLaunch(false)
                        }
                    },
                )
            }

            if (showStartupAnimation) {
                AnimatedStartupSplash(onFinished = { showStartupAnimation = false })
            }
        }
    }
}

@Composable
private fun InitialPermissionRequester(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    var requestStarted by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onFinished() },
    )
    val photoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            if (context.needsNotificationPermission()) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onFinished()
            }
        },
    )

    LaunchedEffect(Unit) {
        if (requestStarted) return@LaunchedEffect
        requestStarted = true
        when {
            context.needsPhotoLibraryPermission() -> {
                photoPermissionLauncher.launch(initialPhotoLibraryPermissions())
            }

            context.needsNotificationPermission() -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            else -> onFinished()
        }
    }
}

private fun initialPhotoLibraryPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun Context.needsPhotoLibraryPermission(): Boolean {
    val fullAccess = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        }

        else -> checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
    val partialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
    return !fullAccess && !partialAccess
}

private fun Context.needsNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
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
    LocalizedText(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
    )
}
