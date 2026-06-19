package com.photogridplanner.ui.media

import com.photogridplanner.ui.i18n.LocalizedText

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.photogridplanner.ui.components.AsyncUriImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor

enum class PhotoSelectionMode {
    Single,
    Multiple,
}

private data class PhotoAsset(
    val id: String,
    val uri: Uri,
    val albumId: String,
    val albumName: String,
    val takenAtMillis: Long,
)

private data class PhotoAlbum(
    val id: String,
    val name: String,
    val coverUri: Uri,
    val count: Int,
)

private enum class PhotoLibrarySection {
    AllPhotos,
    Albums,
}

private const val GeneratedAlbumId = "photogridplanner_generated"
private const val GeneratedAlbumName = "Photo Grid Planner"

private data class PhotoAccessState(
    val hasAccess: Boolean,
    val isPartial: Boolean,
)

@Composable
fun PhotoLibraryPicker(
    visible: Boolean,
    mode: PhotoSelectionMode,
    maxSelection: Int = 80,
    title: String = "Libreria foto",
    onDismiss: () -> Unit,
    onPhotosSelected: (List<Uri>) -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    var accessState by remember { mutableStateOf(context.photoAccessState()) }
    var hasAskedPermission by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var photos by remember { mutableStateOf<List<PhotoAsset>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var selected by remember(mode) { mutableStateOf<List<Uri>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            hasAskedPermission = true
            accessState = context.photoAccessState()
            refreshKey++
        },
    )

    fun requestPhotoAccess() {
        // La richiesta avviene solo dopo un'azione esplicita di import/galleria.
        permissionLauncher.launch(photoLibraryPermissions())
    }

    LaunchedEffect(visible) {
        accessState = context.photoAccessState()
        if (!accessState.hasAccess) {
            requestPhotoAccess()
        }
    }

    LaunchedEffect(accessState, refreshKey) {
        if (accessState.hasAccess) {
            loading = true
            photos = withContext(Dispatchers.IO) {
                queryDeviceImages(context)
            }
            loading = false
        } else {
            photos = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                LocalizedText(title)
            }
        },
        text = {
            AnimatedContent(
                targetState = accessState.hasAccess,
                label = "photo_library_access",
                transitionSpec = {
                    fadeIn(tween(220, easing = FastOutSlowInEasing))
                        .togetherWith(fadeOut(tween(160, easing = FastOutSlowInEasing)))
                },
            ) { hasAccess ->
                if (hasAccess) {
                    PhotoLibraryContent(
                        photos = photos,
                        selected = selected,
                        loading = loading,
                        partialAccess = accessState.isPartial,
                        mode = mode,
                        maxSelection = maxSelection,
                        onManageAccess = {
                            selected = emptyList()
                            requestPhotoAccess()
                        },
                        onTogglePhoto = { uri ->
                            when (mode) {
                                PhotoSelectionMode.Single -> {
                                    onPhotosSelected(listOf(uri))
                                    onDismiss()
                                }

                                PhotoSelectionMode.Multiple -> {
                                    selected = if (uri in selected) {
                                        selected - uri
                                    } else {
                                        (selected + uri).take(maxSelection)
                                    }
                                }
                            }
                        },
                        onAddToSelection = { uris ->
                            if (mode == PhotoSelectionMode.Multiple) {
                                selected = (selected + uris.filterNot { it in selected }).take(maxSelection)
                            }
                        },
                    )
                } else {
                    PhotoPermissionDeniedContent(
                        permissionAsked = hasAskedPermission,
                        onRetry = { requestPhotoAccess() },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            if (accessState.hasAccess && mode == PhotoSelectionMode.Multiple) {
                TextButton(
                    enabled = selected.isNotEmpty(),
                    onClick = {
                        onPhotosSelected(selected)
                        onDismiss()
                    },
                ) {
                    LocalizedText("Importa ${selected.size}")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                LocalizedText("Chiudi")
            }
        },
    )
}

@Composable
private fun PhotoLibraryContent(
    photos: List<PhotoAsset>,
    selected: List<Uri>,
    loading: Boolean,
    partialAccess: Boolean,
    mode: PhotoSelectionMode,
    maxSelection: Int,
    onManageAccess: () -> Unit,
    onTogglePhoto: (Uri) -> Unit,
    onAddToSelection: (List<Uri>) -> Unit,
) {
    val density = LocalDensity.current
    var cellSizePx by remember { mutableIntStateOf(0) }
    val cellStridePx = cellSizePx + with(density) { 6.dp.toPx() }
    val albums = remember(photos) { buildPhotoAlbums(photos) }
    var librarySection by remember { mutableStateOf(PhotoLibrarySection.AllPhotos) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(albums) {
        if (selectedAlbumId != null && albums.none { it.id == selectedAlbumId }) {
            selectedAlbumId = null
        }
    }

    fun photosInSelectionRectangle(
        items: List<PhotoAsset>,
        startIndex: Int,
        endIndex: Int,
    ): List<Uri> {
        val startRow = startIndex / PhotoGridColumns
        val startColumn = startIndex % PhotoGridColumns
        val endRow = endIndex / PhotoGridColumns
        val endColumn = endIndex % PhotoGridColumns
        val firstRow = minOf(startRow, endRow)
        val lastRow = maxOf(startRow, endRow)
        val firstColumn = minOf(startColumn, endColumn)
        val lastColumn = maxOf(startColumn, endColumn)

        return buildList {
            for (row in firstRow..lastRow) {
                for (column in firstColumn..lastColumn) {
                    items.getOrNull(row * PhotoGridColumns + column)?.let { add(it.uri) }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (partialAccess) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.42f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LocalizedText(
                        text = "Accesso parziale",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    LocalizedText(
                        text = "Stai vedendo solo le immagini che hai autorizzato. Puoi modificarle quando vuoi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = onManageAccess) {
                        Icon(Icons.Rounded.Collections, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        LocalizedText("Gestisci foto consentite")
                    }
                }
            }
        }

        if (mode == PhotoSelectionMode.Multiple) {
            LocalizedText(
                text = "Selezionate ${selected.size}/$maxSelection",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LocalizedText(
                text = "Tieni premuto e trascina per selezionare piu foto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            photos.isEmpty() -> EmptyPhotoLibrary()

            else -> {
                PhotoLibrarySectionTabs(
                    section = librarySection,
                    onShowAll = {
                        librarySection = PhotoLibrarySection.AllPhotos
                        selectedAlbumId = null
                    },
                    onShowAlbums = {
                        librarySection = PhotoLibrarySection.Albums
                        selectedAlbumId = null
                    },
                )

                if (librarySection == PhotoLibrarySection.Albums && selectedAlbumId == null) {
                    AlbumGrid(
                        albums = albums,
                        onAlbumSelected = { selectedAlbumId = it },
                    )
                } else {
                    val visiblePhotos = selectedAlbumId?.let { albumId ->
                        photos.filter { it.albumId == albumId }
                    }.orEmpty().ifEmpty {
                        if (selectedAlbumId == null) photos else emptyList()
                    }
                    val selectedAlbum = albums.firstOrNull { it.id == selectedAlbumId }

                    if (selectedAlbum != null) {
                        TextButton(onClick = { selectedAlbumId = null }) {
                            LocalizedText("Tutti gli album")
                        }
                        Text(
                            text = selectedAlbum.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(PhotoGridColumns),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 280.dp, max = 440.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(
                            items = visiblePhotos,
                            key = { it.id },
                        ) { photo ->
                            val index = visiblePhotos.indexOf(photo)
                            val isSelected = photo.uri in selected
                            PhotoCell(
                                photo = photo,
                                index = index,
                                photoCount = visiblePhotos.size,
                                selected = isSelected,
                                dragSelectionEnabled = mode == PhotoSelectionMode.Multiple,
                                cellStridePx = cellStridePx,
                                onCellSizeChanged = { size -> cellSizePx = size },
                                onClick = { onTogglePhoto(photo.uri) },
                                onAddToSelection = { onAddToSelection(listOf(photo.uri)) },
                                onDragSelect = { targetIndex ->
                                    onAddToSelection(
                                        photosInSelectionRectangle(visiblePhotos, index, targetIndex),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoCell(
    photo: PhotoAsset,
    index: Int,
    photoCount: Int,
    selected: Boolean,
    dragSelectionEnabled: Boolean,
    cellStridePx: Float,
    onCellSizeChanged: (Int) -> Unit,
    onClick: () -> Unit,
    onAddToSelection: () -> Unit,
    onDragSelect: (Int) -> Unit,
) {
    val startColumn = index % PhotoGridColumns
    val startRow = index / PhotoGridColumns
    val currentAddToSelection by rememberUpdatedState(onAddToSelection)
    val currentDragSelect by rememberUpdatedState(onDragSelect)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .onSizeChanged { size ->
                if (size.width > 0) onCellSizeChanged(size.width)
            }
            .clip(RoundedCornerShape(8.dp))
            // Long press starts additive selection; dragging across tiles keeps adding them.
            .then(
                if (dragSelectionEnabled) {
                    Modifier.pointerInput(index, photoCount, cellStridePx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { currentAddToSelection() },
                            onDrag = { change, _ ->
                                change.consume()
                                if (cellStridePx <= 0f) return@detectDragGesturesAfterLongPress

                                val column = (
                                    startColumn + floor(change.position.x / cellStridePx).toInt()
                                    ).coerceIn(0, PhotoGridColumns - 1)
                                val row = (
                                    startRow + floor(change.position.y / cellStridePx).toInt()
                                    ).coerceAtLeast(0)
                                val targetIndex = row * PhotoGridColumns + column
                                if (targetIndex in 0 until photoCount) {
                                    currentDragSelect(targetIndex)
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
    ) {
        AsyncUriImage(
            uri = photo.uri.toString(),
            contentScale = ContentScale.Crop,
            maxSize = 480,
            modifier = Modifier.fillMaxSize(),
        )
        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(4.dp),
                )
            }
        }
    }
}

private const val PhotoGridColumns = 3

@Composable
private fun PhotoLibrarySectionTabs(
    section: PhotoLibrarySection,
    onShowAll: () -> Unit,
    onShowAlbums: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = section == PhotoLibrarySection.AllPhotos,
            onClick = onShowAll,
            label = { LocalizedText("Tutte le foto") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
        FilterChip(
            selected = section == PhotoLibrarySection.Albums,
            onClick = onShowAlbums,
            label = { LocalizedText("Album") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
        )
    }
}

@Composable
private fun AlbumGrid(
    albums: List<PhotoAlbum>,
    onAlbumSelected: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 440.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Box(
                modifier = Modifier
                    .aspectRatio(1.08f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAlbumSelected(album.id) },
            ) {
                AsyncUriImage(
                    uri = album.coverUri.toString(),
                    contentScale = ContentScale.Crop,
                    maxSize = 560,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                        LocalizedText(
                            text = "${album.count} foto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun buildPhotoAlbums(photos: List<PhotoAsset>): List<PhotoAlbum> {
    return photos
        .groupBy { it.albumId }
        .map { (id, albumPhotos) ->
            val cover = albumPhotos.maxByOrNull { it.takenAtMillis } ?: return@map null
            PhotoAlbum(
                id = id,
                name = cover.albumName,
                coverUri = cover.uri,
                count = albumPhotos.size,
            )
        }
        .filterNotNull()
        .sortedByDescending { album ->
            photos.firstOrNull { it.albumId == album.id }?.takenAtMillis ?: 0L
        }
}

@Composable
private fun EmptyPhotoLibrary() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            modifier = Modifier.size(42.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        LocalizedText(
            text = "Nessuna immagine disponibile",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        LocalizedText(
            text = "Se hai concesso accesso parziale, usa Gestisci foto consentite per aggiungere altre immagini.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PhotoPermissionDeniedContent(
    permissionAsked: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 420.dp)
            .padding(vertical = 18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            modifier = Modifier.size(46.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(14.dp))
        LocalizedText(
            text = if (permissionAsked) "Accesso alla libreria negato" else "Accesso alla libreria richiesto",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(8.dp))
        LocalizedText(
            text = "La libreria fotografica serve per visualizzare, selezionare, organizzare, tagliare e pianificare le immagini nella griglia. Le foto restano sul dispositivo e vengono elaborate localmente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRetry) {
                LocalizedText("Consenti accesso")
            }
            OutlinedButton(onClick = onOpenSettings) {
                LocalizedText("Impostazioni")
            }
        }
    }
}

private fun photoLibraryPermissions(): Array<String> {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
        )

        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun Context.photoAccessState(): PhotoAccessState {
    val fullAccess = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> hasPermission(Manifest.permission.READ_MEDIA_IMAGES)
        else -> hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    val partialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    return PhotoAccessState(
        hasAccess = fullAccess || partialAccess,
        isPartial = partialAccess && !fullAccess,
    )
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private suspend fun queryDeviceImages(context: Context): List<PhotoAsset> {
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    )
    // Generated cuts receive a deterministic DATE_TAKEN value, so this keeps their
    // intended publishing/slide sequence stable even when they are created together.
    val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC, " +
        "${MediaStore.Images.Media.DATE_ADDED} DESC, " +
        "${MediaStore.Images.Media._ID} DESC"

    // Con accesso parziale su Android 14, MediaStore restituisce solo gli elementi autorizzati.
    val devicePhotos = context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder,
    )?.use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(cursor.toPhotoAsset(collection))
            }
        }
    }.orEmpty()

    // Android can filter MediaStore results after a partial permission grant. Locally-created
    // cuts are remembered separately so they remain available to this app in that situation.
    val visibleUris = devicePhotos.associateBy { it.uri.toString() }
    val generatedPhotos = GeneratedMediaRegistry.read(context).map { record ->
        visibleUris[record.uri.toString()]
            ?: queryPhotoByUri(context, record.uri)
            ?: PhotoAsset(
                id = record.uri.toString(),
                uri = record.uri,
                albumId = GeneratedAlbumId,
                albumName = GeneratedAlbumName,
                takenAtMillis = record.createdAt,
            )
    }

    return (generatedPhotos + devicePhotos)
        .distinctBy { it.uri }
        .sortedByDescending { it.takenAtMillis }
}

private fun queryPhotoByUri(context: Context, uri: Uri): PhotoAsset? {
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_TAKEN,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
    )
    return runCatching {
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.toPhotoAsset(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            } else {
                null
            }
        }
    }.getOrNull()
}

private fun android.database.Cursor.toPhotoAsset(collection: Uri): PhotoAsset {
    val id = getLong(getColumnIndexOrThrow(MediaStore.Images.Media._ID))
    val dateTaken = getLong(getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
    val dateAdded = getLong(getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)) * 1_000L
    val bucketId = getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID))
        ?.takeIf { it.isNotBlank() }
        ?: "other"
    val bucketName = getString(getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
        ?.takeIf { it.isNotBlank() }
        ?: "Altre foto"
    val uri = ContentUris.withAppendedId(collection, id)
    return PhotoAsset(
        id = uri.toString(),
        uri = uri,
        albumId = bucketId,
        albumName = bucketName,
        takenAtMillis = dateTaken.takeIf { it > 0L } ?: dateAdded,
    )
}
