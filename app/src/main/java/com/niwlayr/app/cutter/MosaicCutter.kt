package com.niwlayr.app.cutter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.niwlayr.app.image.ImageLoader
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object MosaicCutter {
    private const val MaxTiles = 20
    private const val MaxDecodeSize = 10800
    private const val OutputExtension = "png"
    private const val OutputMimeType = "image/png"
    private const val GalleryInsertionSpacingMillis = 1_100L

    suspend fun cutAndSave(
        context: Context,
        sourceUri: Uri,
        spec: MosaicSpec,
        format: TileFormat,
        destination: SaveDestination,
        transform: MosaicTransform = MosaicTransform(),
        frame: CutterFrame = CutterFrame(),
        namePrefix: String = "grid",
        exportOrder: CutExportOrder = CutExportOrder.Visual,
        preserveGallerySelectionOrder: Boolean = false,
    ): List<CutTileResult> = withContext(Dispatchers.IO) {
        require(spec.columns > 0 && spec.rows > 0) { "La griglia deve avere almeno una riga e una colonna." }
        require(spec.tileCount <= MaxTiles) { "Riduci il mosaico: massimo $MaxTiles tasselli per taglio." }

        val usesProfileSafeArea = exportOrder == CutExportOrder.ProfilePublish
        val visibleOutputWidth = spec.outputWidth(format, profileVisible = usesProfileSafeArea)
        val outputWidth = if (usesProfileSafeArea) {
            visibleOutputWidth + format.profileSideInset * 2
        } else {
            visibleOutputWidth
        }
        val outputHeight = spec.outputHeight(format)
        val decodeSize = max(outputWidth, outputHeight).coerceIn(7200, MaxDecodeSize)
        val source = ImageLoader.loadBitmap(context, sourceUri, maxSize = decodeSize)
        val mosaic = renderMosaic(
            source = source,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            transform = transform,
            frame = frame,
            profileSideInset = if (usesProfileSafeArea) format.profileSideInset else 0,
        )
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val sessionTakenAt = System.currentTimeMillis()
        val cutPlans = cutCoordinates(spec, exportOrder).mapIndexed { index, coordinate ->
            CutPlan(
                publishIndex = index + 1,
                row = coordinate.first,
                column = coordinate.second,
            )
        }
        // Saving in reverse means the first file in the intended selection order is
        // also the newest media item in galleries that sort by creation time.
        val savePlans = cutPlans.asReversed()

        try {
            val savedResults = MutableList<CutTileResult?>(cutPlans.size) { null }
            savePlans.forEachIndexed { saveIndex, plan ->
                val tile = if (usesProfileSafeArea) {
                    createProfileSafeTile(mosaic, plan.row, plan.column, format)
                } else {
                    Bitmap.createBitmap(
                        mosaic,
                        plan.column * format.width,
                        plan.row * format.height,
                        format.width,
                        format.height,
                    )
                }
                try {
                    val displayName = "${plan.publishIndex.toString().padStart(2, '0')}_${namePrefix}_${timestamp}_${spec.label}"
                    savedResults[plan.publishIndex - 1] = saveTile(
                        context = context,
                        bitmap = tile,
                        displayName = displayName,
                        row = plan.row,
                        column = plan.column,
                        publishIndex = plan.publishIndex,
                        takenAtMillis = sessionTakenAt - ((plan.publishIndex - 1) * 1_000L),
                        destination = destination,
                    )
                } finally {
                    tile.recycle()
                }
                // Instagram can sort freshly-created media by DATE_ADDED, which has second
                // precision on some providers. Distinct insertions avoid unstable slide order.
                if (preserveGallerySelectionOrder &&
                    destination == SaveDestination.Gallery &&
                    saveIndex < savePlans.lastIndex
                ) {
                    delay(GalleryInsertionSpacingMillis)
                }
            }
            savedResults.filterNotNull()
        } finally {
            mosaic.recycle()
            source.recycle()
        }
    }

    private fun createProfileSafeTile(
        mosaic: Bitmap,
        row: Int,
        column: Int,
        format: TileFormat,
    ): Bitmap {
        val sourceLeft = column * format.profileVisibleWidth
        return Bitmap.createBitmap(
            mosaic,
            sourceLeft,
            row * format.height,
            format.width,
            format.height,
        )
    }

    private data class CutPlan(
        val publishIndex: Int,
        val row: Int,
        val column: Int,
    )

    private fun cutCoordinates(
        spec: MosaicSpec,
        exportOrder: CutExportOrder,
    ): List<Pair<Int, Int>> {
        return when (exportOrder) {
            CutExportOrder.Visual -> buildList {
                for (row in 0 until spec.rows) {
                    for (column in 0 until spec.columns) {
                        add(row to column)
                    }
                }
            }

            CutExportOrder.ProfilePublish -> buildList {
                for (row in spec.rows - 1 downTo 0) {
                    for (column in spec.columns - 1 downTo 0) {
                        add(row to column)
                    }
                }
            }
        }
    }

    private fun renderMosaic(
        source: Bitmap,
        outputWidth: Int,
        outputHeight: Int,
        transform: MosaicTransform,
        frame: CutterFrame,
        profileSideInset: Int,
    ): Bitmap {
        val mosaic = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mosaic)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        val imageRect = if (frame.enabled) {
            canvas.drawColor(frame.colorArgb)
            val visibleWidth = (outputWidth - profileSideInset * 2).coerceAtLeast(1)
            val inset = (minOf(visibleWidth, outputHeight) * frame.safeThicknessPercent)
                .roundToInt()
                .coerceAtLeast(0)
            RectF(
                profileSideInset + inset.toFloat(),
                inset.toFloat(),
                profileSideInset + visibleWidth - inset.toFloat(),
                outputHeight - inset.toFloat(),
            )
        } else {
            RectF(0f, 0f, outputWidth.toFloat(), outputHeight.toFloat())
        }
        val destination = destinationRect(
            sourceWidth = source.width.toFloat(),
            sourceHeight = source.height.toFloat(),
            outputWidth = imageRect.width(),
            outputHeight = imageRect.height(),
            transform = transform,
        ).apply {
            offset(imageRect.left, imageRect.top)
        }
        val checkpoint = canvas.save()
        canvas.clipRect(imageRect)
        canvas.drawBitmap(source, null, destination, paint)
        canvas.restoreToCount(checkpoint)
        return mosaic
    }

    private fun destinationRect(
        sourceWidth: Float,
        sourceHeight: Float,
        outputWidth: Float,
        outputHeight: Float,
        transform: MosaicTransform,
    ): RectF {
        val baseScale = max(outputWidth / sourceWidth, outputHeight / sourceHeight)
        val drawScale = baseScale * transform.safeScale
        val drawWidth = sourceWidth * drawScale
        val drawHeight = sourceHeight * drawScale
        val extraX = max(drawWidth - outputWidth, 0f)
        val extraY = max(drawHeight - outputHeight, 0f)
        val left = (outputWidth - drawWidth) / 2f + transform.safeOffsetX * extraX / 2f
        val top = (outputHeight - drawHeight) / 2f + transform.safeOffsetY * extraY / 2f
        return RectF(left, top, left + drawWidth, top + drawHeight)
    }

    private fun saveTile(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        row: Int,
        column: Int,
        publishIndex: Int,
        takenAtMillis: Long,
        destination: SaveDestination,
    ): CutTileResult {
        return if (destination == SaveDestination.Gallery && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToGallery(context, bitmap, displayName, row, column, publishIndex, takenAtMillis)
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
        takenAtMillis: Long,
    ): CutTileResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.$OutputExtension")
            put(MediaStore.Images.Media.MIME_TYPE, OutputMimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NiwLayrCreatorStudio")
            put(MediaStore.Images.Media.DATE_TAKEN, takenAtMillis)
            put(MediaStore.Images.Media.DATE_MODIFIED, takenAtMillis / 1_000L)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Impossibile creare il file in Galleria.")
        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        } ?: error("Impossibile scrivere il file in Galleria.")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return CutTileResult(
            publishIndex = publishIndex,
            row = row,
            column = column,
            displayName = "$displayName.$OutputExtension",
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
        val outputDir = File(baseDir, "NiwLayrCreatorStudio/Cuts").apply { mkdirs() }
        val file = File(outputDir, "$displayName.$OutputExtension")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
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
