package com.photogridplanner.ui.grid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.photogridplanner.data.GridPost
import com.photogridplanner.ui.i18n.LocalAppStrings
import com.photogridplanner.ui.i18n.LocalizedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsSheet(
    post: GridPost,
    onDismiss: () -> Unit,
    onSave: (description: String, tags: String) -> Unit,
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var description by remember(post.id) { mutableStateOf(post.description) }
    var tags by remember(post.id) { mutableStateOf(post.tags) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LocalizedText(
                    text = if (post.isCarousel) "Modifica carosello" else "Modifica post",
                    style = MaterialTheme.typography.headlineSmall,
                )
                LocalizedText(
                    text = "Queste informazioni appariranno nell'anteprima del post.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(2_200) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Descrizione") },
                minLines = 4,
                maxLines = 7,
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it.take(1_200) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Tag") },
                supportingText = {
                    LocalizedText("Aggiungi @utenti, collaboratori o hashtag.")
                },
                minLines = 2,
                maxLines = 4,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyButton(
                    label = "Copia descrizione",
                    text = description,
                    onCopy = {
                        copyToClipboard(context, strings.t("Descrizione"), description, strings.t("Testo copiato"))
                    },
                    modifier = Modifier.weight(1f),
                )
                CopyButton(
                    label = "Copia tag",
                    text = tags,
                    onCopy = {
                        copyToClipboard(context, "tags", tags, strings.t("Tag copiati"))
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    LocalizedText("Annulla")
                }
                ElevatedButton(
                    onClick = {
                        onSave(description, tags)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    LocalizedText("Salva")
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                LocalizedText("Chiudi")
            }
        }
    }
}

@Composable
private fun CopyButton(
    label: String,
    text: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onCopy,
        enabled = text.trim().isNotBlank(),
        modifier = modifier.height(48.dp),
    ) {
        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(6.dp))
        LocalizedText(label, maxLines = 1)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String, feedback: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text.trim()))
    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
}
