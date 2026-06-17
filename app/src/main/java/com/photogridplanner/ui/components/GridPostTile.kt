package com.photogridplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PostKind

@Composable
fun GridPostTile(
    post: GridPost,
    modifier: Modifier = Modifier,
    menuExpanded: Boolean,
    onOpen: () -> Unit,
    onDismissMenu: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(enabled = post.kind == PostKind.Image && post.uri != null, onClick = onOpen),
    ) {
        when (post.kind) {
            PostKind.Image -> AsyncUriImage(
                uri = post.uri.orEmpty(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            PostKind.Placeholder -> PlaceholderTile(modifier = Modifier.fillMaxSize())
        }

        if (post.hidden) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.64f)),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
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
private fun PlaceholderTile(modifier: Modifier = Modifier) {
    EmptyImageSlot(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
