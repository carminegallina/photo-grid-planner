package com.photogridplanner.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
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
import com.photogridplanner.ui.cutter.CutterScreen
import com.photogridplanner.ui.grid.GridScreen
import com.photogridplanner.ui.settings.SettingsScreen
import com.photogridplanner.viewmodel.PlannerViewModel

private enum class Destination(val label: String) {
    Grid("Griglia"),
    Cutter("Cutter"),
    Settings("Impostazioni"),
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
                    label = { Text(Destination.Grid.label) },
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Cutter,
                    onClick = { currentDestination = Destination.Cutter },
                    icon = { Icon(Icons.Rounded.ContentCut, contentDescription = null) },
                    label = { Text(Destination.Cutter.label) },
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Settings,
                    onClick = { currentDestination = Destination.Settings },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                    label = { Text(Destination.Settings.label) },
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
