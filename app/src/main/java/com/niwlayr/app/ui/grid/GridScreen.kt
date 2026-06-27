package com.niwlayr.app.ui.grid

import com.niwlayr.app.ui.i18n.LocalAppStrings
import com.niwlayr.app.ui.i18n.LocalizedText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.niwlayr.app.ui.theme.DisplayFamily
import com.niwlayr.app.ui.theme.MonoFamily
import com.niwlayr.app.ui.theme.SpectrumStops
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.niwlayr.app.data.GridPost
import com.niwlayr.app.data.PlannerData
import com.niwlayr.app.data.PlaceholderPresetColors
import com.niwlayr.app.data.PlaceholderType
import com.niwlayr.app.data.PreviewMode
import com.niwlayr.app.data.SavedLayout
import com.niwlayr.app.data.PostKind
import com.niwlayr.app.ui.components.AsyncUriImage
import com.niwlayr.app.ui.components.FullScreenPreview
import com.niwlayr.app.ui.components.GridPostTile
import com.niwlayr.app.ui.media.PhotoLibraryPicker
import com.niwlayr.app.ui.media.PhotoSelectionMode
import com.niwlayr.app.viewmodel.PlannerViewModel
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
    var detailsPostId by remember { mutableStateOf<String?>(null) }
    var pendingImportUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var confirmReset by remember { mutableStateOf(false) }
    var showLayoutsDialog by remember { mutableStateOf(false) }
    var compareLayout by remember { mutableStateOf<SavedLayout?>(null) }
    var renameLayout by remember { mutableStateOf<SavedLayout?>(null) }
    var deleteLayout by remember { mutableStateOf<SavedLayout?>(null) }
    var editPlaceholderPost by remember { mutableStateOf<GridPost?>(null) }
    var placeholderActionsPost by remember { mutableStateOf<GridPost?>(null) }
    var replacePlaceholderId by remember { mutableStateOf<String?>(null) }
    var pendingCandidateImport by remember { mutableStateOf<PendingGridImport?>(null) }
    var resumePreviewPostId by remember { mutableStateOf<String?>(null) }
    var showPhotoLibrary by remember { mutableStateOf(false) }

    // Single entry point for importing picked images, so photos shared into the app go through
    // the same single / mosaic / carousel routing (and ordering) as the in-app picker.
    fun routeImportedPhotos(uris: List<android.net.Uri>) {
        when (uris.size) {
            0 -> Unit
            1 -> if (state.analyzeImports) {
                pendingCandidateImport = PendingGridImport(
                    uris = uris,
                    type = PendingGridImportType.Post,
                )
            } else {
                viewModel.addImages(uris)
            }
            else -> pendingImportUris = uris
        }
    }

    val pendingSharedImport by viewModel.pendingSharedImport.collectAsState()
    LaunchedEffect(pendingSharedImport) {
        if (pendingSharedImport.isNotEmpty()) {
            routeImportedPhotos(pendingSharedImport.map { android.net.Uri.parse(it) })
            viewModel.consumeSharedImport()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfilePreviewHeaderModern(
                postCount = state.visiblePosts.size,
                savedLayoutCount = state.savedLayouts.size,
                onOpenLayouts = { showLayoutsDialog = true },
            )

            AnimatedContent(
                targetState = state.visiblePosts.isEmpty(),
                label = "grid_empty_state",
                transitionSpec = {
                    (
                        fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.985f)
                        ).togetherWith(
                            fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                                scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 0.985f),
                        )
                },
            ) { isEmpty ->
                if (isEmpty) {
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
                        onPlaceholderClick = { post -> placeholderActionsPost = post },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        GridActionDock(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp),
            onReset = { confirmReset = true },
            onSave = viewModel::saveCurrentLayout,
            onPlaceholder = { viewModel.addPlaceholder() },
            onImport = { showPhotoLibrary = true },
        )
    }

    PhotoLibraryPicker(
        visible = showPhotoLibrary,
        mode = if (replacePlaceholderId == null) PhotoSelectionMode.Multiple else PhotoSelectionMode.Single,
        maxSelection = if (replacePlaceholderId == null) 80 else 1,
        title = if (replacePlaceholderId == null) "Importa nella griglia" else "Sostituisci placeholder",
        usedMediaUris = state.posts.flatMap { post -> post.allMediaUris }.toSet(),
        scheduledMediaUris = state.posts
            .filter { post -> !post.scheduledDate.isNullOrBlank() }
            .flatMap { post -> post.allMediaUris }
            .toSet(),
        onDismiss = {
            showPhotoLibrary = false
            replacePlaceholderId = null
        },
        onPhotosSelected = { uris ->
            val placeholderId = replacePlaceholderId
            if (placeholderId != null) {
                uris.firstOrNull()?.let { uri ->
                    viewModel.replacePlaceholderWithImage(placeholderId, uri)
                }
                replacePlaceholderId = null
            } else {
                routeImportedPhotos(uris)
            }
        },
    )

    val previewPost = previewPostId?.let { id ->
        state.posts.firstOrNull { it.id == id }
    }
    previewPost?.takeIf { it.allMediaUris.isNotEmpty() }?.let { post ->
        FullScreenPreview(
            post = post,
            onDismiss = { previewPostId = null },
            onEdit = {
                resumePreviewPostId = post.id
                previewPostId = null
                detailsPostId = post.id
            },
        )
    }

    val detailsPost = detailsPostId?.let { id -> state.posts.firstOrNull { it.id == id } }
    detailsPost?.let { post ->
        PostDetailsSheet(
            post = post,
            onDismiss = {
                detailsPostId = null
                previewPostId = resumePreviewPostId
                resumePreviewPostId = null
            },
            onSave = { description, tags ->
                viewModel.setPostDetails(post.id, description, tags)
            },
        )
    }

    pendingCandidateImport?.let { pendingImport ->
        CandidatePostPreviewDialog(
            pendingImport = pendingImport,
            currentPosts = state.posts,
            onDismiss = { pendingCandidateImport = null },
            onConfirm = { position ->
                when (pendingImport.type) {
                    PendingGridImportType.Post -> {
                        pendingImport.uris.firstOrNull()?.let { uri ->
                            viewModel.insertImage(uri, position)
                        }
                    }

                    PendingGridImportType.Carousel -> {
                        viewModel.insertCarousel(pendingImport.uris, position)
                    }

                    PendingGridImportType.Mosaic -> {
                        viewModel.insertImages(pendingImport.uris.asReversed(), position)
                    }
                }
                pendingCandidateImport = null
            },
        )
    }

    if (pendingImportUris.isNotEmpty()) {
        ImportChoiceDialog(
            count = pendingImportUris.size,
            onDismiss = { pendingImportUris = emptyList() },
            onMosaic = {
                if (state.analyzeImports) {
                    pendingCandidateImport = PendingGridImport(
                        uris = pendingImportUris,
                        type = PendingGridImportType.Mosaic,
                    )
                } else {
                    // The gallery presents generated mosaic tiles in publishing order.
                    // The grid itself shows the most recent post first, so reverse them here.
                    viewModel.addImages(pendingImportUris.reversed())
                }
                pendingImportUris = emptyList()
            },
            onCarousel = {
                if (state.analyzeImports) {
                    pendingCandidateImport = PendingGridImport(
                        uris = pendingImportUris,
                        type = PendingGridImportType.Carousel,
                    )
                } else {
                    viewModel.addCarousel(pendingImportUris)
                }
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
            title = { LocalizedText("Elimina layout") },
            text = { LocalizedText("Vuoi eliminare \"${layout.name}\"? La griglia attuale non verra modificata.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSavedLayout(layout.id)
                        deleteLayout = null
                    },
                ) {
                    LocalizedText("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteLayout = null }) {
                    LocalizedText("Annulla")
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

    placeholderActionsPost?.let { post ->
        PlaceholderActionsDialog(
            post = post,
            onDismiss = { placeholderActionsPost = null },
            onEdit = {
                placeholderActionsPost = null
                editPlaceholderPost = post
            },
            onReplace = {
                placeholderActionsPost = null
                replacePlaceholderId = post.id
                showPhotoLibrary = true
            },
            onToggleVisibility = {
                viewModel.togglePostVisibility(post.id)
                placeholderActionsPost = null
            },
            onDelete = {
                viewModel.deletePost(post.id)
                placeholderActionsPost = null
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { LocalizedText("Svuota griglia") },
            text = { LocalizedText("Tutte le foto e i placeholder nella griglia verranno rimossi. I layout salvati restano disponibili.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        viewModel.clearLocalGrid()
                    },
                ) {
                    LocalizedText("Svuota")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    LocalizedText("Annulla")
                }
            },
        )
    }
}

@Composable
private fun GridActionDock(
    onReset: () -> Unit,
    onSave: () -> Unit,
    onPlaceholder: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = modifier.animateContentSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockIconButton(
                icon = Icons.Rounded.RestartAlt,
                contentDescription = strings.t("Svuota griglia"),
                tint = MaterialTheme.colorScheme.tertiary,
                onClick = onReset,
            )
            DockIconButton(
                icon = Icons.Rounded.Save,
                contentDescription = strings.t("Salva layout"),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onSave,
            )
            DockIconButton(
                icon = Icons.Rounded.Image,
                contentDescription = strings.t("Aggiungi placeholder"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onPlaceholder,
            )
            DockIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = strings.t("Importa immagini"),
                tint = MaterialTheme.colorScheme.onPrimary,
                selected = true,
                onClick = onImport,
            )
        }
    }
}

@Composable
private fun DockIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape,
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(if (selected) 26.dp else 23.dp),
        )
    }
}

@Composable
private fun ProfilePreviewHeaderModern(
    postCount: Int,
    savedLayoutCount: Int,
    onOpenLayouts: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LocalizedText(
                    text = "photo.grid",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                ) {
                    LocalizedText(
                        text = "+",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                OutlinedButton(onClick = onOpenLayouts) {
                    LocalizedText("Layout $savedLayoutCount", maxLines = 1)
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.86f),
                border = BorderStroke(
                    width = 2.5.dp,
                    brush = Brush.sweepGradient(SpectrumStops + SpectrumStops.first()),
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    LocalizedText(
                        text = "PG",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
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
            LocalizedText(
                text = "NiwLayr - Creator Studio",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            LocalizedText(
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
            ProfileTab(
                selected = true,
                label = "POST",
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ViewModule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
            ProfileTab(selected = false, label = "REELS")
            ProfileTab(selected = false, label = "TAG")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)),
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
                    LocalizedText(
                        text = "photo.grid",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    LocalizedText(
                        text = "âŒ„",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LocalizedText(
                        text = "+",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    OutlinedButton(onClick = onOpenLayouts) {
                        LocalizedText("Layout $savedLayoutCount")
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
                        width = 2.5.dp,
                        brush = Brush.sweepGradient(SpectrumStops + SpectrumStops.first()),
                    ),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        LocalizedText(
                            text = "PG",
                            style = MaterialTheme.typography.titleLarge,
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
                LocalizedText(
                    text = "NiwLayr - Creator Studio",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                LocalizedText(
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
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
            ),
        ) {
            Box(contentAlignment = Alignment.Center) {
                LocalizedText(
                    text = label.take(1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LocalizedText(
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
        icon?.invoke() ?: LocalizedText(
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
        LocalizedText(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFamily),
            color = MaterialTheme.colorScheme.onBackground,
        )
        LocalizedText(
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            LocalizedText(
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
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.84f), RoundedCornerShape(8.dp))
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
            LocalizedText(
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
        title = { LocalizedText("Importa $count immagini") },
        text = {
            LocalizedText("Scegli come inserirle nella griglia.")
        },
        confirmButton = {
            TextButton(onClick = onCarousel) {
                LocalizedText("Carosello")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) {
                    LocalizedText("Annulla")
                }
                TextButton(onClick = onMosaic) {
                    LocalizedText("Mosaico")
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
        title = { LocalizedText("Layout salvati") },
        text = {
            if (layouts.isEmpty()) {
                LocalizedText("Nessun layout salvato.")
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    layouts.forEach { layout ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
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
                                        LocalizedText(
                                            text = layout.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        LocalizedText(
                                            text = "${layout.itemCount} elementi",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    LocalizedText(
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
                                            LocalizedText("Apri")
                                        }
                                        TextButton(onClick = { onCompare(layout) }) {
                                            LocalizedText("Confronta")
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
                LocalizedText("Chiudi")
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
        title = { LocalizedText("Rinomina layout") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(48) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { LocalizedText("Nome layout") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.trim().isNotBlank(),
                onClick = { onSave(name) },
            ) {
                LocalizedText("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                LocalizedText("Annulla")
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
    var selectedType by remember(post.id) {
        mutableStateOf(if (post.placeholderLabel.isBlank()) post.placeholderType else null)
    }

    LaunchedEffect(post.placeholderLabel, post.placeholderColor, post.placeholderType) {
        label = post.placeholderLabel
        color = post.placeholderColor
        selectedType = if (post.placeholderLabel.isBlank()) post.placeholderType else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { LocalizedText("Placeholder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { value ->
                        label = value.take(28)
                        if (label.isNotBlank()) {
                            selectedType = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { LocalizedText("Nome personalizzato") },
                    singleLine = true,
                )
                LocalizedText(
                    text = "Se inserisci un nome, il tipo viene deselezionato. Se scegli un tipo, il nome viene cancellato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocalizedText("Tipo", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaceholderType.entries.forEach { option ->
                            FilterChip(
                                selected = selectedType == option,
                                onClick = {
                                    selectedType = option
                                    label = ""
                                },
                                label = { LocalizedText(option.label) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LocalizedText("Preset neutri", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaceholderPresetColors.forEach { preset ->
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
            TextButton(onClick = { onSave(color, label, selectedType ?: post.placeholderType) }) {
                LocalizedText("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                LocalizedText("Annulla")
            }
        },
    )
}

@Composable
private fun PlaceholderActionsDialog(
    post: GridPost,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onReplace: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { LocalizedText(post.placeholderDisplayLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LocalizedText(
                    text = "Gestisci questo spazio nella griglia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ElevatedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Modifica")
                }
                OutlinedButton(
                    onClick = onReplace,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Sostituisci")
                }
                OutlinedButton(
                    onClick = onToggleVisibility,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    LocalizedText(if (post.hidden) "Mostra nella preview" else "Oscura dalla preview")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    LocalizedText("Elimina")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                LocalizedText("Chiudi")
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
        title = { LocalizedText("Confronto layout") },
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
                LocalizedText("Chiudi")
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
        LocalizedText(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        LocalizedText(
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
                LocalizedText(
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
    onPlaceholderClick: (GridPost) -> Unit,
    modifier: Modifier = Modifier,
) {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val hapticFeedback = LocalHapticFeedback.current
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
                    onPlaceholderClick = { onPlaceholderClick(post) },
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
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggedId = post.id
                                    dragStartCenter = boundsById[post.id]?.center ?: Offset.Zero
                                    dragCenter = dragStartCenter
                                    dragDistance = Offset.Zero
                                    hasDragged = false
                                    // Reveal the per-post menu as soon as the long-press is
                                    // recognised, instead of requiring the finger to be lifted
                                    // perfectly still. A real drag dismisses it again (see onDrag).
                                    menuPostId = if (post.kind == PostKind.Image) post.id else null
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
                                    }
                                    // When the finger is lifted without dragging, the menu
                                    // opened in onDragStart simply stays visible.
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
                                    if (!hasDragged) {
                                        // A real drag has started: leave menu mode and reorder.
                                        hasDragged = true
                                        menuPostId = null
                                    }

                                    val target = displayedPosts.firstOrNull { candidate ->
                                        candidate.id != post.id &&
                                            boundsById[candidate.id]?.contains(dragCenter) == true
                                    } ?: return@detectDragGesturesAfterLongPress

                                    val reorderedPosts = displayedPosts.moveItem(
                                        fromIndex = displayedPosts.indexOfFirst { it.id == post.id },
                                        toIndex = displayedPosts.indexOfFirst { it.id == target.id },
                                    )
                                    if (reorderedPosts !== displayedPosts) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        displayedPosts = reorderedPosts
                                    }
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

private fun formatLayoutDate(timestamp: Long): String {
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}
