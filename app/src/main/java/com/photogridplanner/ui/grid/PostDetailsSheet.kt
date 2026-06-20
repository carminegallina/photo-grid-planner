package com.photogridplanner.ui.grid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PostStatus
import com.photogridplanner.ui.components.AsyncUriImage
import com.photogridplanner.ui.i18n.LocalAppStrings
import com.photogridplanner.ui.i18n.LocalizedText

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PostDetailsSheet(
    post: GridPost,
    onDismiss: () -> Unit,
    onSave: (caption: String, hashtags: String, notes: String, status: PostStatus) -> Unit,
    onOpenPreview: () -> Unit,
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var caption by remember(post.id) { mutableStateOf(post.caption) }
    var hashtags by remember(post.id) { mutableStateOf(post.hashtags) }
    var notes by remember(post.id) { mutableStateOf(post.notes) }
    var status by remember(post.id) { mutableStateOf(post.status) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(width = 62.dp, height = 82.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (post.coverUri != null) {
                        AsyncUriImage(
                            uri = post.coverUri.orEmpty(),
                            contentScale = ContentScale.Crop,
                            maxSize = 220,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    LocalizedText(
                        text = if (post.isCarousel) "Carosello" else "Post",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    LocalizedText(
                        text = if (post.isCarousel) {
                            "${post.allMediaUris.size} immagini"
                        } else {
                            "Post singolo"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusPill(status = status)
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(2_200) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Didascalia") },
                minLines = 3,
                maxLines = 6,
            )
            OutlinedTextField(
                value = hashtags,
                onValueChange = { hashtags = it.take(1_200) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Hashtag") },
                minLines = 2,
                maxLines = 4,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CopyButton(
                    label = "Copia didascalia",
                    text = caption,
                    onCopy = { copyToClipboard(context, strings.t("Didascalia"), caption, strings.t("Testo copiato")) },
                    modifier = Modifier.weight(1f),
                )
                CopyButton(
                    label = "Copia hashtag",
                    text = hashtags,
                    onCopy = { copyToClipboard(context, "hashtags", hashtags, strings.t("Hashtag copiati")) },
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(2_000) },
                modifier = Modifier.fillMaxWidth(),
                label = { LocalizedText("Note") },
                minLines = 2,
                maxLines = 5,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocalizedText("Stato", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PostStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { LocalizedText(option.label) },
                        )
                    }
                }
            }

            post.scheduledDate?.let { date ->
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                ) {
                    LocalizedText(
                        text = "Pianificato per $date",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenPreview,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    LocalizedText("Anteprima")
                }
                ElevatedButton(
                    onClick = {
                        onSave(caption, hashtags, notes, status)
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

@Composable
private fun StatusPill(status: PostStatus) {
    val color = when (status) {
        PostStatus.Idea -> MaterialTheme.colorScheme.onSurfaceVariant
        PostStatus.Ready -> MaterialTheme.colorScheme.primary
        PostStatus.Scheduled -> Color(0xFF82C7F4)
        PostStatus.Published -> Color(0xFF82D4A5)
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
    ) {
        LocalizedText(
            text = status.label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String, feedback: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text.trim()))
    Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()
}
