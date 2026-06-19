package com.photogridplanner.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.photogridplanner.data.PlannerData
import com.photogridplanner.export.ProjectExporter
import com.photogridplanner.viewmodel.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: PlannerData,
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zipExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val copied = withContext(Dispatchers.IO) {
                        ProjectExporter.exportToZip(
                            context = context,
                            zipUri = it,
                            state = state,
                            orderText = viewModel.exportOrderText(),
                        )
                    }
                    Toast.makeText(context, "ZIP esportato: $copied immagini", Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
    val folderExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    val copied = withContext(Dispatchers.IO) {
                        ProjectExporter.exportToFolder(
                            context = context,
                            treeUri = it,
                            state = state,
                            orderText = viewModel.exportOrderText(),
                        )
                    }
                    Toast.makeText(context, "Cartella esportata: $copied immagini", Toast.LENGTH_SHORT).show()
                }
            }
        },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Impostazioni",
            style = MaterialTheme.typography.headlineMedium,
        )

        SettingsPanel(title = "Preview") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null)
                    Text("Mostra post oscurati")
                }
                Switch(
                    checked = state.showHiddenPosts,
                    onCheckedChange = viewModel::setShowHiddenPosts,
                )
            }
        }

        SettingsPanel(title = "Export pacchetto") {
            Text(
                text = "Esporta ordine, manifest e immagini originali senza ricodificarle.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = state.posts.isNotEmpty(),
                    onClick = { zipExporter.launch(ProjectExporter.defaultZipName()) },
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("File ZIP")
                }
                OutlinedButton(
                    enabled = state.posts.isNotEmpty(),
                    onClick = { folderExporter.launch(null) },
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Cartella")
                }
            }
        }

        SettingsPanel(title = "Progetto") {
            Text(
                text = "Salvataggio locale DataStore. L'app lavora solo con foto importate dal dispositivo e placeholder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsPanel(title = "Copyright") {
            Text(
                text = "Photo Grid Planner (c) 2026 Carmine Gallina. Tutti i diritti riservati.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Versione beta privata concessa solo per test. Vietata la redistribuzione, modifica, vendita o ripubblicazione dell'app o dell'APK senza autorizzazione.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
