package com.niwlayr.app.ui.components

import com.niwlayr.app.ui.i18n.LocalAppStrings
import com.niwlayr.app.ui.i18n.LocalizedText

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.niwlayr.app.data.GridPost
import com.niwlayr.app.data.PostKind

@Composable
fun GridPostTile(
    post: GridPost,
    modifier: Modifier = Modifier,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
    onPlaceholderClick: () -> Unit = {},
) {
    val strings = LocalAppStrings.current
    val tileDescription = buildString {
        append(
            when (post.kind) {
                PostKind.Image -> if (post.isCarousel) {
                    "${strings.t("Carosello")}, ${post.allMediaUris.size} ${strings.t("Foto").lowercase()}"
                } else {
                    strings.t("Post")
                }

                PostKind.Placeholder -> "${strings.t("Placeholder")}: ${post.placeholderDisplayLabel}"
            },
        )
        if (post.hidden) append(", ${strings.t("Nascosto")}")
    }
    Box(
        modifier = modifier
            .background(Color.Black)
            .semantics {
                contentDescription = tileDescription
                role = Role.Button
            }
            .clickable(
                enabled = post.kind == PostKind.Placeholder || post.coverUri != null,
                onClick = {
                    if (post.kind == PostKind.Placeholder) onPlaceholderClick() else onOpen()
                },
            ),
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
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.9f),
            exit = fadeOut(tween(150, easing = FastOutSlowInEasing)) +
                scaleOut(tween(150, easing = FastOutSlowInEasing), targetScale = 0.9f),
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
                    LocalizedText(
                        text = post.allMediaUris.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = post.hidden,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(160, easing = FastOutSlowInEasing)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.64f)),
            )
        }
        AnimatedVisibility(
            visible = post.hidden,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.94f),
            exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                scaleOut(tween(160, easing = FastOutSlowInEasing), targetScale = 0.94f),
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
                    LocalizedText(
                        text = "Nascosto",
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        if (post.kind == PostKind.Image) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = onDismissMenu,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
            DropdownMenuItem(
                text = { LocalizedText(if (post.hidden) "Mostra nella preview" else "Oscura dalla preview") },
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
                text = { LocalizedText("Elimina") },
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
            LocalizedText(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}
