package com.photogridplanner.cutter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.photogridplanner.image.ImageLoader
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MosaicCutter {
    suspend fun cutAndSave(
        context: Context,
        sourceUri: Uri,
        spec: MosaicSpec,
        format: TileFormat,
        destination: SaveDestination,
    ): List<CutTileResult> = withContext(Dispatchers.IO) {
        val source = ImageLoader.loadBitmap(context, sourceUri, maxSize = 7200)
        val cropped = centerCropForMosaic(source, spec, format)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

        val results = buildList {
            for (row in 0 until spec.rows) {
                for (column in 0 until spec.columns) {
                    val tileIndex = row * spec.columns + column + 1
                    val tile = createTile(cropped, row, column, spec, format)
                    val displayName = "grid_${timestamp}_${spec.label}_${tileIndex.toString().padStart(2, '0')}"
                    add(saveTile(context, tile, displayName, row, column, tileIndex, destination))
                    tile.recycle()
                }
            }
        }

        if (cropped !== source) cropped.recycle()
        source.recycle()
        results
    }

    private fun centerCropForMosaic(
        source: Bitmap,
        spec: MosaicSpec,
        format: TileFormat,
    ): Bitmap {
        val targetAspect = (spec.columns * format.width).toFloat() /
            (spec.rows * format.height).toFloat()
        val sourceAspect = source.width.toFloat() / source.height.toFloat()

        val cropWidth: Int
        val cropHeight: Int
        if (sourceAspect > targetAspect) {
            cropHeight = source.height
            cropWidth = (cropHeight * targetAspect).roundToInt().coerceAtMost(source.width)
        } else {
            cropWidth = source.width
            cropHeight = (cropWidth / targetAspect).roundToInt().coerceAtMost(source.height)
        }

        val startX = ((source.width - cropWidth) / 2).coerceAtLeast(0)
        val startY = ((source.height - cropHeight) / 2).coerceAtLeast(0)
        return Bitmap.createBitmap(source, startX, startY, cropWidth, cropHeight)
    }

    private fun createTile(
        cropped: Bitmap,
        row: Int,
        column: Int,
        spec: MosaicSpec,
        format: TileFormat,
    ): Bitmap {
        val tileWidth = cropped.width / spec.columns
        val tileHeight = cropped.height / spec.rows
        val x = column * tileWidth
        val y = row * tileHeight
        val rawTile = Bitmap.createBitmap(cropped, x, y, tileWidth, tileHeight)
        val scaled = Bitmap.createScaledBitmap(rawTile, format.width, format.height, true)
        if (scaled !== rawTile) rawTile.recycle()
        return scaled
    }

    private fun saveTile(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        row: Int,
        column: Int,
        publishIndex: Int,
        destination: SaveDestination,
    ): CutTileResult {
        return if (destination == SaveDestination.Gallery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToGallery(context, bitmap, displayName, row, column, publishIndex)
        } else {
            saveToAppFolder(context, bitmap, displayName, row, column, publishIndex)
        }
    }

    private fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        row: Int,
        column: Int,
        publishIndex: Int,
    ): CutTileResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PhotoGridPlanner")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Impossibile creare il file in Galleria.")
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        } ?: error("Impossibile scrivere il file in Galleria.")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return CutTileResult(
            publishIndex = publishIndex,
            row = row,
            column = column,
            displayName = "$displayName.jpg",
            uri = uri,
            file = null,
        )
    }

    private fun saveToAppFolder(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        row: Int,
        column: Int,
        publishIndex: Int,
    ): CutTileResult {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        val outputDir = File(baseDir, "PhotoGridPlanner/Cuts").apply { mkdirs() }
        val file = File(outputDir, "$displayName.jpg")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        }
        return CutTileResult(
            publishIndex = publishIndex,
            row = row,
            column = column,
            displayName = file.name,
            uri = Uri.fromFile(file),
            file = file,
        )
    }
}
