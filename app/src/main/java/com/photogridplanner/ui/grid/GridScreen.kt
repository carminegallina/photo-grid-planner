package com.photogridplanner.ui.grid

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PlaceholderType
import com.photogridplanner.data.PreviewMode
import com.photogridplanner.data.SavedLayout
import com.photogridplanner.data.PostKind
import com.photogridplanner.ui.components.AsyncUriImage
import com.photogridplanner.ui.components.FullScreenPreview
import com.photogridplanner.ui.components.GridPostTile
import com.photogridplanner.viewmodel.PlannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridScreen(
    state: PlannerData,
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier,
) {
    var previewPostId by remember { mutableStateOf<String?>(null) }
    var pendingImportUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var confirmReset by remember { mutableStateOf(false) }
    var showLayoutsDialog by remember { mutableStateOf(false) }
    var compareLayout by remember { mutableStateOf<SavedLayout?>(null) }
    var renameLayout by remember { mutableStateOf<SavedLayout?>(null) }
    var deleteLayout by remember { mutableStateOf<SavedLayout?>(null) }
    var editPlaceholderPost by remember { mutableStateOf<GridPost?>(null) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 80),
        onResult = { uris ->
            when (uris.size) {
                0 -> Unit
                1 -> viewModel.addImages(uris)
                else -> pendingImportUris = uris
            }
        },
    )
    val launchPicker = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfilePreviewHeader(
                postCount = state.visiblePosts.size,
                savedLayoutCount = state.savedLayouts.size,
                onOpenLayouts = { showLayoutsDialog = true },
            )

            if (state.visiblePosts.isEmpty()) {
                EmptyGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                )
            } else {
                ReorderableGrid(
                    posts = state.visiblePosts,
                    aspectRatio = PreviewMode.Vertical.aspectRatio,
                    onReorderFinished = viewModel::setPostOrder,
                    onOpen = { post -> previewPostId = post.id },
                    onToggleVisibility = { post -> viewModel.togglePostVisibility(post.id) },
                    onDelete = { post -> viewModel.deletePost(post.id) },
                    onPlaceholderColorChange = { post, color ->
                        viewModel.setPlaceholderColor(post.id, color)
                    },
                    onEditPlaceholder = { post -> editPlaceholderPost = post },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FloatingActionButton(
                onClick = { confirmReset = true },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Svuota griglia",
                    modifier = Modifier.size(28.dp),
                )
            }
            FloatingActionButton(
                onClick = viewModel::saveCurrentLayout,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = "Salva layout",
                    modifier = Modifier.size(28.dp),
                )
            }
            FloatingActionButton(
                onClick = { viewModel.addPlaceholder() },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = "Aggiungi placeholder",
                    modifier = Modifier.size(28.dp),
                )
            }
            FloatingActionButton(
                onClick = launchPicker,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Importa immagini",
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }

    val previewPost = previewPostId?.let { id ->
        state.posts.firstOrNull { it.id == id }
    }
    previewPost?.allMediaUris?.takeIf { it.isNotEmpty() }?.let { uris ->
        FullScreenPreview(uris = uris, onDismiss = { previewPostId = null })
    }

    if (pendingImportUris.isNotEmpty()) {
        ImportChoiceDialog(
            count = pendingImportUris.size,
            onDismiss = { pendingImportUris = emptyList() },
            onMosaic = {
                viewModel.addImages(pendingImportUris)
                pendingImportUris = emptyList()
            },
            onCarousel = {
                viewModel.addCarousel(pendingImportUris)
                pendingImportUris = emptyList()
            },
        )
    }

    if (showLayoutsDialog) {
        LayoutsDialog(
            layouts = state.savedLayouts,
            onDismiss = { showLayoutsDialog = false },
            onApply = { layout ->
                viewModel.applySavedLayout(layout.id)
                showLayoutsDialog = false
            },
            onCompare = { layout ->
                compareLayout = layout
                showLayoutsDialog = false
            },
            onRename = { layout -> renameLayout = layout },
            onDelete = { layout -> deleteLayout = layout },
        )
    }

    compareLayout?.let { layout ->
        CompareLayoutsDialog(
            currentPosts = state.posts,
            savedLayout = layout,
            onDismiss = { compareLayout = null },
        )
    }

    renameLayout?.let { layout ->
        RenameLayoutDialog(
            layout = layout,
            onDismiss = { renameLayout = null },
            onSave = { name ->
                viewModel.renameSavedLayout(layout.id, name)
                renameLayout = null
            },
        )
    }

    deleteLayout?.let { layout ->
        AlertDialog(
            onDismissRequest = { deleteLayout = null },
            title = { Text("Elimina layout") },
            text = { Text("Vuoi eliminare \"${layout.name}\"? La griglia attuale non verra modificata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavedLayout(layout.id)
                        deleteLayout = null
                    },
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteLayout = null }) {
                    Text("Annulla")
                }
            },
        )
    }

    editPlaceholderPost?.let { post ->
        PlaceholderEditorDialog(
            post = post,
            onDismiss = { editPlaceholderPost = null },
            onSave = { color, label, type ->
                viewModel.setPlaceholderDetails(post.id, color, label, type)
                editPlaceholderPost = null
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Svuota griglia") },
            text = { Text("Tutte le foto e i placeholder nella griglia verranno rimossi. I layout salvati restano disponibili.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        viewModel.clearLocalGrid()
                    },
                ) {
                    Text("Svuota")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text("Annulla")
                }
            },
        )
    }
}

@Composable
private fun ProfilePreviewHeader(
    postCount: Int,
    savedLayoutCount: Int,
    onOpenLayouts: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(0.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "photo.grid",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "⌄",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    OutlinedButton(onClick = onOpenLayouts) {
                        Text("Layout $savedLayoutCount")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                Surface(
                    modifier = Modifier.size(82.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "PG",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    ProfileStat(value = postCount.toString(), label = "post")
                    ProfileStat(value = "1.248", label = "follower")
                    ProfileStat(value = "312", label = "seguiti")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Photo Grid Planner",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Fotografia, palette e composizioni in anteprima.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileButton(text = "Modifica profilo", modifier = Modifier.weight(1f))
                ProfileButton(text = "Condividi profilo", modifier = Modifier.weight(1f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileHighlight(label = "Portfolio")
                ProfileHighlight(label = "Mosaici")
                ProfileHighlight(label = "Palette")
                ProfileHighlight(label = "Bozze")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileTab(selected = true, label = "POST", icon = { 
                    Icon(
                        imageVector = Icons.Rounded.ViewModule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                })
                ProfileTab(selected = false, label = "REELS")
                ProfileTab(selected = false, label = "TAG")
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            )
        }
    }
}

@Composable
private fun ProfileHighlight(label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label.take(1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileTab(
    selected: Boolean,
    label: String,
    icon: @Composable (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.invoke() ?: Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                ),
        )
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(34.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyGrid(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(42.dp),
            )
            Text(
                text = "Tocca + per importare immagini o aggiungere placeholder",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ImportChoiceDialog(
    count: Int,
    onDismiss: () -> Unit,
    onMosaic: () -> Unit,
    onCarousel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importa $count immagini") },
        text = {
            Text("Scegli come inserirle nella griglia.")
        },
        confirmButton = {
            TextButton(onClick = onCarousel) {
                Text("Carosello")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Annulla")
                }
                TextButton(onClick = onMosaic) {
                    Text("Mosaico")
                }
            }
        },
    )
}

@Composable
private fun LayoutsDialog(
    layouts: List<SavedLayout>,
    onDismiss: () -> Unit,
    onApply: (SavedLayout) -> Unit,
    onCompare: (SavedLayout) -> Unit,
    onRename: (SavedLayout) -> Unit,
    onDelete: (SavedLayout) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Layout salvati") },
        text = {
            if (layouts.isEmpty()) {
                Text("Nessun layout salvato.")
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    layouts.forEach { layout ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = layout.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = "${layout.itemCount} elementi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = formatLayoutDate(layout.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                SavedLayoutPreview(posts = layout.posts)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TextButton(onClick = { onApply(layout) }) {
                                            Text("Apri")
                                        }
                                        TextButton(onClick = { onCompare(layout) }) {
                                            Text("Confronta")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        TextButton(onClick = { onRename(layout) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = "Rinomina layout",
                                                modifier = Modifier.size(19.dp),
                                            )
                                        }
                                        TextButton(onClick = { onDelete(layout) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Elimina layout",
                                                modifier = Modifier.size(19.dp),
                                                tint = MaterialTheme.colorScheme.tertiary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        },
    )
}

@Composable
private fun SavedLayoutPreview(posts: List<GridPost>) {
    val previewPosts = posts.take(9)
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        previewPosts.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                row.forEach { post ->
                    MiniPostTile(
                        post = post,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                    )
                }
                repeat(3 - row.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameLayoutDialog(
    layout: SavedLayout,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember(layout.id) { mutableStateOf(layout.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina layout") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(48) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nome layout") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotBlank(),
                onClick = { onSave(name) },
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun PlaceholderEditorDialog(
    post: GridPost,
    onDismiss: () -> Unit,
    onSave: (Int, String, PlaceholderType) -> Unit,
) {
    var label by remember(post.id) { mutableStateOf(post.placeholderLabel) }
    var color by remember(post.id) { mutableStateOf(post.placeholderColor) }
    var type by remember(post.id) { mutableStateOf(post.placeholderType) }

    LaunchedEffect(post.placeholderLabel, post.placeholderColor, post.placeholderType) {
        label = post.placeholderLabel
        color = post.placeholderColor
        type = post.placeholderType
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Placeholder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(28) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nome personalizzato") },
                    singleLine = true,
                )
                Text(
                    text = "Se vuoto, nella griglia viene mostrato il tipo selezionato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tipo", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaceholderType.entries.forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Preset neutri", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeutralPlaceholderColors.forEach { preset ->
                            Surface(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { color = preset },
                                color = Color(preset),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (color == preset) 2.dp else 1.dp,
                                    color = if (color == preset) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                    },
                                ),
                                content = {},
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(color, label, type) }) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        },
    )
}

@Composable
private fun CompareLayoutsDialog(
    currentPosts: List<GridPost>,
    savedLayout: SavedLayout,
    onDismiss: () -> Unit,
) {
    val currentRows = currentPosts.chunked(3)
    val savedRows = savedLayout.posts.chunked(3)
    val rowCount = maxOf(currentRows.size, savedRows.size)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confronto layout") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CompareHeader(
                        title = "Attuale",
                        count = currentPosts.size,
                        modifier = Modifier.weight(1f),
                    )
                    CompareHeader(
                        title = savedLayout.name,
                        count = savedLayout.posts.size,
                        modifier = Modifier.weight(1f),
                    )
                }
                LazyColumn(
                    modifier = Modifier.heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(rowCount) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            MiniGridRow(
                                posts = currentRows.getOrNull(rowIndex).orEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                            MiniGridRow(
                                posts = savedRows.getOrNull(rowIndex).orEmpty(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        },
    )
}

@Composable
private fun CompareHeader(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$count elementi",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MiniGridRow(
    posts: List<GridPost>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        posts.forEach { post ->
            MiniPostTile(
                post = post,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )
        }
        repeat(3 - posts.size) {
            Spacer(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
            )
        }
    }
}

@Composable
private fun MiniPostTile(
    post: GridPost,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            post.kind == PostKind.Placeholder -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(post.placeholderColor)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = post.placeholderDisplayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }

            post.coverUri != null -> AsyncUriImage(
                uri = post.coverUri.orEmpty(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                maxSize = 180,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (post.hidden) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
            )
        }
    }
}

@Composable
private fun ReorderableGrid(
    posts: List<GridPost>,
    aspectRatio: Float,
    onReorderFinished: (List<GridPost>) -> Unit,
    onOpen: (GridPost) -> Unit,
    onToggleVisibility: (GridPost) -> Unit,
    onDelete: (GridPost) -> Unit,
    onPlaceholderColorChange: (GridPost, Int) -> Unit,
    onEditPlaceholder: (GridPost) -> Unit,
    modifier: Modifier = Modifier,
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val boundsById = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    var displayedPosts by remember { mutableStateOf(posts) }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragStartCenter by remember { mutableStateOf(Offset.Zero) }
    var dragCenter by remember { mutableStateOf(Offset.Zero) }
    var dragDistance by remember { mutableStateOf(Offset.Zero) }
    var hasDragged by remember { mutableStateOf(false) }
    var menuPostId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(
        posts.map { it.id },
        posts.map { it.hidden },
        posts.map { it.allMediaUris },
        posts.map { it.placeholderColor },
        posts.map { it.placeholderLabel },
        posts.map { it.placeholderType },
    ) {
        if (draggedId == null) {
            displayedPosts = posts
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val spacing = 1.dp
        val rows = ((displayedPosts.size + 2) / 3).coerceAtLeast(1)
        val cellWidth = (maxWidth - spacing * 2) / 3
        val gridHeight = (cellWidth / aspectRatio) * rows + spacing * (rows - 1)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .height(gridHeight)
                .background(Color.Black),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            userScrollEnabled = false,
        ) {
            itemsIndexed(
                items = displayedPosts,
                key = { _, post -> post.id },
            ) { _, post ->
                val isDragging = draggedId == post.id
                GridPostTile(
                    post = post,
                    menuExpanded = menuPostId == post.id,
                    onOpen = { onOpen(post) },
                    onDismissMenu = { menuPostId = null },
                    onToggleVisibility = { onToggleVisibility(post) },
                    onDelete = { onDelete(post) },
                    onPlaceholderColorChange = { color -> onPlaceholderColorChange(post, color) },
                    onEditPlaceholder = { onEditPlaceholder(post) },
                    modifier = Modifier
                        .height(cellWidth / aspectRatio)
                        .onGloballyPositioned { coordinates ->
                            boundsById[post.id] = coordinates.boundsInRoot()
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            if (isDragging) {
                                val currentCenter = boundsById[post.id]?.center
                                if (currentCenter != null) {
                                    translationX = dragCenter.x - currentCenter.x
                                    translationY = dragCenter.y - currentCenter.y
                                }
                                alpha = 0.92f
                                scaleX = 1.03f
                                scaleY = 1.03f
                            }
                        }
                        .pointerInput(post.id, posts) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = post.id
                                    dragStartCenter = boundsById[post.id]?.center ?: Offset.Zero
                                    dragCenter = dragStartCenter
                                    dragDistance = Offset.Zero
                                    hasDragged = false
                                    menuPostId = null
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragStartCenter = Offset.Zero
                                    dragCenter = Offset.Zero
                                    dragDistance = Offset.Zero
                                    hasDragged = false
                                    displayedPosts = posts
                                },
                                onDragEnd = {
                                    if (hasDragged) {
                                        onReorderFinished(displayedPosts)
                                    } else {
                                        menuPostId = post.id
                                    }
                                    draggedId = null
                                    dragStartCenter = Offset.Zero
                                    dragCenter = Offset.Zero
                                    dragDistance = Offset.Zero
                                    hasDragged = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragDistance += dragAmount
                                    dragCenter += dragAmount

                                    if (!hasDragged && dragDistance.getDistance() < touchSlop) {
                                        return@detectDragGesturesAfterLongPress
                                    }
                                    hasDragged = true

                                    val target = displayedPosts.firstOrNull { candidate ->
                                        candidate.id != post.id &&
                                            boundsById[candidate.id]?.contains(dragCenter) == true
                                    } ?: return@detectDragGesturesAfterLongPress

                                    displayedPosts = displayedPosts.moveItem(
                                        fromIndex = displayedPosts.indexOfFirst { it.id == post.id },
                                        toIndex = displayedPosts.indexOfFirst { it.id == target.id },
                                    )
                                },
                            )
                        },
                )
            }
        }
    }
}

private fun List<GridPost>.moveItem(fromIndex: Int, toIndex: Int): List<GridPost> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}

private val NeutralPlaceholderColors = listOf(
    0xFF34363D.toInt(),
    0xFF4D525C.toInt(),
    0xFF686B70.toInt(),
    0xFF7B7468.toInt(),
    0xFF556258.toInt(),
    0xFF4B6178.toInt(),
)

private fun formatLayoutDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}
