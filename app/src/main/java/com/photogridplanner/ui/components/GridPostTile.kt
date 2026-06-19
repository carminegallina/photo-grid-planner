package com.photogridplanner.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.photogridplanner.data.DefaultPlaceholderColor
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PostKind

private val PlaceholderPalette = listOf(
    DefaultPlaceholderColor,
    0xFF5E6472.toInt(),
    0xFF7D7461.toInt(),
    0xFF6F7F72.toInt(),
    0xFF4B6178.toInt(),
    0xFF8A6F76.toInt(),
)

@Composable
fun GridPostTile(
    post: GridPost,
    modifier: Modifier = Modifier,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
    onPlaceholderColorChange: (Int) -> Unit = {},
    onEditPlaceholder: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(enabled = post.kind == PostKind.Image && post.coverUri != null, onClick = onOpen),
    ) {
        when (post.kind) {
            PostKind.Image -> AsyncUriImage(
                uri = post.coverUri.orEmpty(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            PostKind.Placeholder -> PlaceholderTile(
                color = post.placeholderColor,
                label = post.placeholderDisplayLabel,
                modifier = Modifier.fillMaxSize(),
            )
        }

        AnimatedVisibility(
            visible = post.isCarousel,
            enter = fadeIn(tween(140)) + scaleIn(tween(140), initialScale = 0.86f),
            exit = fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.86f),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Surface(
                modifier = Modifier
                    .padding(7.dp),
                color = Color.Black.copy(alpha = 0.54f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ViewCarousel,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = post.allMediaUris.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = post.hidden,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.64f)),
            )
        }
        AnimatedVisibility(
            visible = post.hidden,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.92f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.92f),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Surface(
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Nascosto",
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            if (post.kind == PostKind.Placeholder) {
                DropdownMenuItem(
                    text = { Text("Modifica placeholder") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        onDismissMenu()
                        onEditPlaceholder()
                    },
                )
                DropdownMenuItem(
                    text = {
                        Column {
                            Text("Colore placeholder")
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlaceholderPalette.forEach { color ->
                                    Surface(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(28.dp)
                                            .clickable {
                                                onDismissMenu()
                                                onPlaceholderColorChange(color)
                                            },
                                        shape = CircleShape,
                                        color = Color(color),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        ),
                                        content = {},
                                    )
                                }
                            }
                        }
                    },
                    onClick = {},
                )
            }
            DropdownMenuItem(
                text = { Text(if (post.hidden) "Mostra nella preview" else "Oscura dalla preview") },
                leadingIcon = {
                    Icon(
                        imageVector = if (post.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                    )
                },
                onClick = {
                    onDismissMenu()
                    onToggleVisibility()
                },
            )
            DropdownMenuItem(
                text = { Text("Elimina") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                },
                onClick = {
                    onDismissMenu()
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun PlaceholderTile(
    color: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.12f),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}
