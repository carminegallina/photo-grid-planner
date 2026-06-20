package com.photogridplanner.ui.settings

import com.photogridplanner.ui.i18n.LocalAppStrings
import com.photogridplanner.ui.i18n.LocalizedText

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.photogridplanner.data.AppLanguage
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
    onShowTutorial: () -> Unit = {},
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()
    val needsExactAlarmAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == false
    var pendingBackupRestore by remember { mutableStateOf<String?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.setNotificationsEnabled(granted)
            if (!granted) {
                Toast.makeText(context, strings.t("Permesso notifiche non concesso"), Toast.LENGTH_SHORT).show()
            }
        },
    )
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
                    Toast.makeText(context, strings.t("ZIP esportato: $copied immagini"), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, strings.t("Cartella esportata: $copied immagini"), Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
    val backupExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    runCatching {
                        val backup = viewModel.exportBackupJson()
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(it)?.use { output ->
                                output.write(backup.toByteArray(Charsets.UTF_8))
                            } ?: error("Impossibile creare il backup.")
                        }
                    }.onSuccess {
                        Toast.makeText(context, strings.t("Backup progetto creato"), Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(context, strings.t("Creazione backup non riuscita"), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
    )
    val backupImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader ->
                                reader.readText()
                            } ?: error("Impossibile leggere il backup.")
                        }
                    }.onSuccess { backup ->
                        pendingBackupRestore = backup
                    }.onFailure {
                        Toast.makeText(context, strings.t("File di backup non valido"), Toast.LENGTH_SHORT).show()
                    }
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
        LocalizedText(
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
                    LocalizedText("Mostra post oscurati")
                }
                Switch(
                    checked = state.showHiddenPosts,
                    onCheckedChange = viewModel::setShowHiddenPosts,
                )
            }
        }

        SettingsPanel(title = "Notifiche") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                    LocalizedText("Promemoria pubblicazione")
                }
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        val notificationPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                        if (enabled && !notificationPermissionGranted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setNotificationsEnabled(enabled)
                            if (enabled && needsExactAlarmAccess) {
                                openExactAlarmSettings(context)
                            }
                        }
                    },
                )
            }
            LocalizedText(
                text = "Ricevi un avviso all'orario scelto nel calendario quando ci sono post pianificati per la giornata.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.notificationsEnabled && needsExactAlarmAccess) {
                LocalizedText(
                    text = "Consenti gli allarmi esatti per ricevere il promemoria anche con l'app chiusa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { openExactAlarmSettings(context) }) {
                    Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Consenti promemoria puntuali")
                }
            }
        }

        SettingsPanel(title = "Tutorial") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Info, contentDescription = null)
                    LocalizedText("Mostra tutorial all'avvio")
                }
                Switch(
                    checked = state.showTutorialOnLaunch,
                    onCheckedChange = viewModel::setShowTutorialOnLaunch,
                )
            }
            OutlinedButton(onClick = onShowTutorial) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                LocalizedText("Apri tutorial")
            }
        }

        SettingsPanel(title = "Lingua") {
            LocalizedText(
                text = "Scegli la lingua dell'app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LocalizedText(
                text = "Rilevata automaticamente al primo avvio: italiano per dispositivi in italiano, inglese per tutte le altre lingue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguage.values().forEach { language ->
                    FilterChip(
                        selected = state.language == language,
                        onClick = { viewModel.setLanguage(language) },
                        label = { LocalizedText(language.label) },
                    )
                }
            }
        }

        SettingsPanel(title = "Privacy") {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                LocalizedText(
                    text = "L'app richiede l'accesso alla libreria fotografica per permetterti di visualizzare, selezionare, organizzare, tagliare e pianificare le immagini nella griglia. Le foto restano sul dispositivo e vengono elaborate localmente. Nessuna immagine viene caricata online o condivisa con terze parti senza una tua azione esplicita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsPanel(title = "Backup progetto") {
            LocalizedText(
                text = "Salva griglia, layout, calendario e impostazioni in un file locale. Le foto non vengono duplicate e restano sul dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { backupExporter.launch("PhotoGridPlanner_backup.json") }) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Crea backup")
                }
                OutlinedButton(onClick = { backupImporter.launch(arrayOf("application/json", "text/plain")) }) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Ripristina backup")
                }
            }
        }

        SettingsPanel(title = "Export pacchetto") {
            LocalizedText(
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
                    LocalizedText("File ZIP")
                }
                OutlinedButton(
                    enabled = state.posts.isNotEmpty(),
                    onClick = { folderExporter.launch(null) },
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Cartella")
                }
            }
        }

        SettingsPanel(title = "Progetto") {
            LocalizedText(
                text = "Salvataggio locale DataStore. L'app lavora solo con foto importate dal dispositivo e placeholder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsPanel(title = "Copyright") {
            LocalizedText(
                text = "Photo Grid Planner (c) 2026 Carmine Gallina. Tutti i diritti riservati.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LocalizedText(
                text = "Versione beta privata concessa solo per test. Vietata la redistribuzione, modifica, vendita o ripubblicazione dell'app o dell'APK senza autorizzazione.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    pendingBackupRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingBackupRestore = null },
            title = { LocalizedText("Ripristinare il progetto?") },
            text = {
                LocalizedText(
                    text = "La griglia, i layout e il calendario attuali verranno sostituiti dal contenuto del backup.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching { viewModel.restoreBackupJson(backup) }
                                .onSuccess {
                                    pendingBackupRestore = null
                                    Toast.makeText(context, strings.t("Backup ripristinato"), Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    pendingBackupRestore = null
                                    Toast.makeText(context, strings.t("Ripristino backup non riuscito"), Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                ) {
                    LocalizedText("Ripristina")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackupRestore = null }) {
                    LocalizedText("Annulla")
                }
            },
        )
    }
}

private fun openExactAlarmSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    context.startActivity(
        Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@Composable
private fun SettingsPanel(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.animateContentSize(),
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
            LocalizedText(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
