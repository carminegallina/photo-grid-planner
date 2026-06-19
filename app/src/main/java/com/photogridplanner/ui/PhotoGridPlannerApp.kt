package com.photogridplanner.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun PhotoGridPlannerApp(viewModel: PlannerViewModel) {
    val state by viewModel.state.collectAsState()
    var currentDestination by rememberSaveable { mutableStateOf(Destination.Grid) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                NavigationBarItem(
                    selected = currentDestination == Destination.Grid,
                    onClick = { currentDestination = Destination.Grid },
                    icon = { Icon(Icons.Rounded.ViewModule, contentDescription = null) },
                    label = { NavLabel(Destination.Grid.label) },
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Analysis,
                    onClick = { currentDestination = Destination.Analysis },
                    icon = { Icon(Icons.Rounded.Analytics, contentDescription = null) },
                    label = { NavLabel(Destination.Analysis.label) },
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Calendar,
                    onClick = { currentDestination = Destination.Calendar },
                    icon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
                    label = { NavLabel(Destination.Calendar.label) },
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Cutter,
                    onClick = { currentDestination = Destination.Cutter },
                    icon = { Icon(Icons.Rounded.ContentCut, contentDescription = null) },
                    label = { NavLabel(Destination.Cutter.label) },
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Settings,
                    onClick = { currentDestination = Destination.Settings },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { NavLabel(Destination.Settings.label) },
                )
            }
        },
    ) { padding ->
        when (currentDestination) {
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

@Composable
private fun NavLabel(text: String) {
    Text(
        text = text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
    )
}
