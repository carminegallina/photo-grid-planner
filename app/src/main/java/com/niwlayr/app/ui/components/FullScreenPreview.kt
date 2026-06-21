package com.niwlayr.app.ui.components

import com.niwlayr.app.ui.i18n.LocalAppStrings
import com.niwlayr.app.ui.i18n.LocalizedText

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.niwlayr.app.data.GridPost

@Composable
fun FullScreenPreview(
    uri: String,
    onDismiss: () -> Unit,
) {
    FullScreenPreview(
        uris = listOf(uri),
        onDismiss = onDismiss,
    )
}

@Composable
fun FullScreenPreview(
    uris: List<String>,
    onDismiss: () -> Unit,
) {
    PostPreviewDialog(
        uris = uris,
        description = "",
        tags = "",
        onDismiss = onDismiss,
        onEdit = null,
    )
}

@Composable
fun FullScreenPreview(
    post: GridPost,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    PostPreviewDialog(
        uris = post.allMediaUris,
        description = post.description,
        tags = post.tags,
        onDismiss = onDismiss,
        onEdit = onEdit,
    )
}

@Composable
private fun PostPreviewDialog(
    uris: List<String>,
    description: String,
    tags: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)?,
) {
    if (uris.isEmpty()) return

    val strings = LocalAppStrings.current
    val pagerState = rememberPagerState(pageCount = { uris.size })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            LocalizedText(
                                text = "PG",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        LocalizedText(
                            text = "photo.grid",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                        )
                        LocalizedText(
                            text = if (uris.size > 1) {
                                "Carosello ${pagerState.currentPage + 1}/${uris.size}"
                            } else {
                                "Post"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f),
                        )
                    }
                    if (onEdit == null) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.86f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(24.dp),
                        )
                    } else {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = strings.t("Modifica info"),
                                tint = Color.White.copy(alpha = 0.90f),
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(999.dp),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = strings.t("Chiudi"),
                            tint = Color.White,
                        )
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncUriImage(
                            uri = uris[page],
                            contentScale = ContentScale.Fit,
                            maxSize = 4096,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (uris.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            uris.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                        .background(
                                            color = if (index == pagerState.currentPage) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.White.copy(alpha = 0.36f)
                                            },
                                            shape = CircleShape,
                                        ),
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(27.dp),
                            )
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(27.dp),
                            )
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.BookmarkBorder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(27.dp),
                        )
                    }
                    if (description.isNotBlank()) {
                        LocalizedText(
                            text = "photo.grid  ${description.trim()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                    if (tags.isNotBlank()) {
                        LocalizedText(
                            text = tags.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    onEdit?.let { edit ->
                        TextButton(onClick = edit) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            LocalizedText("Modifica info")
                        }
                    }
                }
            }
        }
    }
}
