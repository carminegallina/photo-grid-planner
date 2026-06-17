package com.photogridplanner.ui.grid

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.photogridplanner.data.InstagramPost
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PreviewMode
import com.photogridplanner.ui.components.AsyncUriImage
import com.photogridplanner.ui.components.FullScreenPreview
import com.photogridplanner.ui.components.GridPostTile
import com.photogridplanner.viewmodel.PlannerViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    state: PlannerData,
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier,
) {
    var previewUri by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 80),
        onResult = { uris -> viewModel.addImages(uris) },
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
            Header()

            if (state.instagramPosts.isNotEmpty()) {
                ProfileLayoutsBar(
                    state = state,
                    onApplyLayout = viewModel::applyProfileLayout,
                )
                ReorderableInstagramGrid(
                    posts = state.orderedInstagramPosts,
                    aspectRatio = PreviewMode.Vertical.aspectRatio,
                    onReorderFinished = viewModel::setInstagramOrder,
                    onOpen = { post -> previewUri = post.displayUrl },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (state.visiblePosts.isEmpty()) {
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
                    onOpen = { post -> post.uri?.let { previewUri = it } },
                    onToggleVisibility = { post -> viewModel.togglePostVisibility(post.id) },
                    onDelete = { post -> viewModel.deletePost(post.id) },
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
                onClick = {
                    if (state.instagramPosts.isNotEmpty()) {
                        viewModel.restoreInstagramOriginalOrder()
                    } else {
                        confirmReset = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.tertiary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset progetto",
                    modifier = Modifier.size(28.dp),
                )
            }
            if (state.instagramPosts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = viewModel::saveCurrentProfileLayout,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = "Salva layout",
                        modifier = Modifier.size(28.dp),
                    )
                }
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

    previewUri?.let { uri ->
        FullScreenPreview(uri = uri, onDismiss = { previewUri = null })
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Svuota progetto") },
            text = { Text("Tutte le foto nella griglia verranno rimosse da questa pianificazione.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        viewModel.resetProject()
                    },
                ) {
                    Text("Reset")
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
private fun Header() {
    Text(
        text = "Photo Grid Planner",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun ProfileLayoutsBar(
    state: PlannerData,
    onApplyLayout: (String) -> Unit,
) {
    if (state.savedProfileLayouts.isEmpty()) {
        Text(
            text = "Profilo Instagram originale",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Layout salvati",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.savedProfileLayouts.take(3).forEach { layout ->
                FilterChip(
                    selected = state.instagramOrder == layout.postIds,
                    onClick = { onApplyLayout(layout.id) },
                    label = { Text(layout.name) },
                )
            }
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
                text = "Tocca + per importare immagini",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ReorderableInstagramGrid(
    posts: List<InstagramPost>,
    aspectRatio: Float,
    onReorderFinished: (List<InstagramPost>) -> Unit,
    onOpen: (InstagramPost) -> Unit,
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

    LaunchedEffect(posts.map { it.id }) {
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
                Box(
                    modifier = Modifier
                        .aspectRatio(aspectRatio)
                        .background(Color.Black)
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
                                        onOpen(post)
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

                                    displayedPosts = displayedPosts.moveInstagramItem(
                                        fromIndex = displayedPosts.indexOfFirst { it.id == post.id },
                                        toIndex = displayedPosts.indexOfFirst { it.id == target.id },
                                    )
                                },
                            )
                        },
                ) {
                    AsyncUriImage(
                        uri = post.displayUrl,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
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

    LaunchedEffect(posts.map { it.id }, posts.map { it.hidden }, posts.map { it.uri }) {
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
                    modifier = Modifier
                        .aspectRatio(aspectRatio)
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

private fun List<InstagramPost>.moveInstagramItem(
    fromIndex: Int,
    toIndex: Int,
): List<InstagramPost> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}

private fun List<GridPost>.moveItem(fromIndex: Int, toIndex: Int): List<GridPost> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    return toMutableList().apply {
        val item = removeAt(fromIndex)
        add(toIndex, item)
    }
}
