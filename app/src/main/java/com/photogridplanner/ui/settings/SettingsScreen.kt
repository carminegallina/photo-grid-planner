package com.photogridplanner.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.photogridplanner.data.PlannerData
import com.photogridplanner.instagram.InstagramOAuth
import com.photogridplanner.viewmodel.PlannerViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: PlannerData,
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val syncState by viewModel.instagramSyncState.collectAsState()
    var clientId by remember { mutableStateOf(state.instagramClientId) }

    LaunchedEffect(state.instagramClientId) {
        clientId = state.instagramClientId
    }

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
                    Text("Mostra post nascosti")
                }
                Switch(
                    checked = state.showHiddenPosts,
                    onCheckedChange = viewModel::setShowHiddenPosts,
                )
            }
        }

        SettingsPanel(title = "Instagram") {
            OutlinedTextField(
                value = clientId,
                onValueChange = { clientId = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Instagram App Client ID") },
                singleLine = true,
            )
            OutlinedTextField(
                value = InstagramOAuth.RedirectUri,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Redirect URI Meta") },
                singleLine = true,
                readOnly = true,
            )
            OutlinedTextField(
                value = InstagramOAuth.AppRedirectUri,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Deep link app") },
                singleLine = true,
                readOnly = true,
            )
            Button(
                enabled = clientId.isNotBlank() && !syncState.loading,
                onClick = {
                    viewModel.setInstagramClientId(clientId)
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(InstagramOAuth.buildLoginUrl(clientId)),
                        ),
                    )
                },
            ) {
                if (syncState.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text(if (syncState.loading) "Sincronizzo..." else "Accedi con Instagram")
            }
            OutlinedButton(
                enabled = state.instagramAccessToken.isNotBlank() && !syncState.loading,
                onClick = { viewModel.syncInstagramProfile() },
            ) {
                Text("Sincronizza profilo")
            }
            syncState.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Configura il Redirect URI Meta nella dashboard. La pagina online riapre l'app e il token viene salvato automaticamente dopo il login ufficiale.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsPanel(title = "Export ordine") {
            Text(
                text = "${state.posts.size} elementi salvati localmente",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = state.posts.isNotEmpty(),
                    onClick = {
                        copyOrderToClipboard(context, viewModel.exportOrderText())
                    },
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Copia")
                }
                OutlinedButton(
                    enabled = state.posts.isNotEmpty(),
                    onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                viewModel.shareOrderIntent(),
                                "Esporta ordine",
                            ),
                        )
                    },
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Condividi")
                }
            }
        }

        SettingsPanel(title = "Progetto") {
            Text(
                text = "Salvataggio locale DataStore. Il collegamento Instagram resta opzionale e non modifica il profilo reale.",
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
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private fun copyOrderToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("Ordine griglia Instagram", text))
    Toast.makeText(context, "Ordine copiato", Toast.LENGTH_SHORT).show()
}
