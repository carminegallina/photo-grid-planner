package com.photogridplanner.ui.cutter

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.photogridplanner.cutter.CutTileResult
import com.photogridplanner.cutter.MosaicSpec
import com.photogridplanner.cutter.MosaicCutter
import com.photogridplanner.cutter.SaveDestination
import com.photogridplanner.cutter.TileFormat
import com.photogridplanner.ui.components.AsyncUriImage
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by rememberSaveable { mutableStateOf<String?>(null) }
    var columns by rememberSaveable { mutableStateOf(3) }
    var rows by rememberSaveable { mutableStateOf(3) }
    val format = TileFormat.Vertical
    var isCutting by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<CutTileResult>>(emptyList()) }
    val spec = MosaicSpec(columns = columns, rows = rows)

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                persistReadAccess(context, it)
                selectedUri = it.toString()
                resultMessage = null
                results = emptyList()
            }
        },
    )
    val launchPicker = {
        picker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Grid Cutter",
            style = MaterialTheme.typography.headlineMedium,
        )

        MosaicControls(
            columns = columns,
            rows = rows,
            onColumnsChange = { columns = it },
            onRowsChange = { rows = it },
        )

        selectedUri?.let { uri ->
            CutterPreview(
                uri = uri,
                spec = spec,
                format = format,
                onClick = launchPicker,
                modifier = Modifier.fillMaxWidth(),
            )

            ElevatedButton(
                onClick = {
                    scope.launch {
                        isCutting = true
                        resultMessage = null
                        results = emptyList()
                        runCatching {
                            MosaicCutter.cutAndSave(
                                context = context,
                                sourceUri = Uri.parse(uri),
                                spec = spec,
                                format = format,
                                destination = SaveDestination.Gallery,
                            )
                        }.onSuccess { cutResults ->
                            results = cutResults
                            resultMessage = "Creati ${cutResults.size} tasselli."
                        }.onFailure { error ->
                            resultMessage = error.message ?: "Taglio non riuscito."
                        }
                        isCutting = false
                    }
                },
                enabled = !isCutting,
            ) {
                if (isCutting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.size(8.dp))
                Text(if (isCutting) "Taglio..." else "Taglia immagine")
            }
        } ?: EmptyCutterPreview(onImport = launchPicker)

        resultMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (results.isNotEmpty()) {
            ResultList(results = results)
        }
    }
}

@Composable
private fun MosaicControls(
    columns: Int,
    rows: Int,
    onColumnsChange: (Int) -> Unit,
    onRowsChange: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Mosaico", style = MaterialTheme.typography.titleMedium)
            SliderSetting(
                label = "Colonne",
                value = columns,
                onValueChange = onColumnsChange,
            )
            SliderSetting(
                label = "Righe",
                value = rows,
                onValueChange = onRowsChange,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${columns * rows} tasselli",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = TileFormat.Vertical.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt().coerceIn(1, 6)) },
            valueRange = 1f..6f,
            steps = 4,
        )
    }
}

@Composable
private fun CutterPreview(
    uri: String,
    spec: MosaicSpec,
    format: TileFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val aspectRatio = (spec.columns * format.width).toFloat() /
        (spec.rows * format.height).toFloat()
    BoxWithConstraints(modifier = modifier) {
        val maxPreviewHeight = 520.dp
        val previewModifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxPreviewHeight)
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)

        Box(modifier = previewModifier) {
            AsyncUriImage(
                uri = uri,
                contentScale = ContentScale.Crop,
                maxSize = 2200,
                modifier = Modifier.fillMaxSize(),
            )
            CutLinesOverlay(
                columns = spec.columns,
                rows = spec.rows,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CutLinesOverlay(
    columns: Int,
    rows: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val lineColor = Color.White.copy(alpha = 0.82f)
        for (column in 1 until columns) {
            val x = size.width * column / columns
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square,
            )
        }
        for (row in 1 until rows) {
            val y = size.height * row / rows
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square,
            )
        }
    }
}

@Composable
private fun EmptyCutterPreview(onImport: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedButton(onClick = onImport) {
            Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Seleziona una foto")
        }
    }
}

@Composable
private fun ResultList(results: List<CutTileResult>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Tasselli creati", style = MaterialTheme.typography.titleMedium)
        results.forEach { result ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = result.layoutLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

private fun persistReadAccess(context: android.content.Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
}
