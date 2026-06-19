package com.photogridplanner.cutter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.photogridplanner.image.ImageLoader
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TemplateCutter {
    private const val MaxDecodeSize = 5400
    private const val OutputExtension = "png"
    private const val OutputMimeType = "image/png"

    suspend fun renderAndSave(
        context: Context,
        template: CutterTemplate,
        slotInputs: List<TemplateSlotInput>,
        outputWidth: Int,
        outputHeight: Int,
        backgroundColorArgb: Int,
        destination: SaveDestination,
        namePrefix: String,
        slideCount: Int = 1,
    ): List<CutTileResult> = withContext(Dispatchers.IO) {
        require(slotInputs.isNotEmpty()) { "Inserisci almeno una foto nel template." }
        require(outputWidth > 0 && outputHeight > 0) { "Dimensione template non valida." }
        require(slideCount > 0) { "Numero slide non valido." }

        val rendered = renderTemplate(
            context = context,
            template = template,
            slotInputs = slotInputs,
            outputWidth = outputWidth,
            outputHeight = outputHeight,
            backgroundColorArgb = backgroundColorArgb,
        )
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        try {
            if (slideCount == 1) {
                return@withContext listOf(
                    saveBitmap(
                        context = context,
                        bitmap = rendered,
                        displayName = "${namePrefix}_${timestamp}_${template.id}_01",
                        row = 0,
                        column = 0,
                        publishIndex = 1,
                        destination = destination,
                    ),
                )
            }

            val slideWidth = TileFormat.Vertical.width
            val slideHeight = TileFormat.Vertical.height
            val results = mutableListOf<CutTileResult>()
            for (slide in 0 until slideCount) {
                val bitmap = Bitmap.createBitmap(
                    rendered,
                    slide * slideWidth,
                    0,
                    slideWidth,
                    slideHeight,
                )
                try {
                    results += saveBitmap(
                        context = context,
                        bitmap = bitmap,
                        displayName = "${namePrefix}_${timestamp}_${template.id}_${(slide + 1).toString().padStart(2, '0')}",
                        row = 0,
                        column = slide,
                        publishIndex = slide + 1,
                        destination = destination,
                    )
                } finally {
                    bitmap.recycle()
                }
            }
            results
        } finally {
            rendered.recycle()
        }
    }

    private suspend fun renderTemplate(
        context: Context,
        template: CutterTemplate,
        slotInputs: List<TemplateSlotInput>,
        outputWidth: Int,
        outputHeight: Int,
        backgroundColorArgb: Int,
    ): Bitmap {
        val inputBySlot = slotInputs.associateBy { it.slotId }
        val decodeSize = max(outputWidth, outputHeight).coerceAtMost(MaxDecodeSize)
        val bitmaps = mutableMapOf<String, Bitmap>()
        try {
            slotInputs.forEach { input ->
                bitmaps[input.slotId] = ImageLoader.loadBitmap(context, input.uri, maxSize = decodeSize)
            }

            val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(backgroundColorArgb)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

            template.slots.forEach { slot ->
                val source = bitmaps[slot.id] ?: return@forEach
                val input = inputBySlot[slot.id] ?: return@forEach
                drawSlotImage(
                    canvas = canvas,
                    source = source,
                    slotRect = slot.rect.toRect(outputWidth, outputHeight),
                    transform = input.transform,
                    paint = paint,
                )
            }
            return output
        } finally {
            bitmaps.values.forEach { it.recycle() }
        }
    }

    private fun NormalizedRect.toRect(outputWidth: Int, outputHeight: Int): RectF {
        val leftPx = floor(left * outputWidth).toInt().coerceIn(0, outputWidth - 1)
        val topPx = floor(top * outputHeight).toInt().coerceIn(0, outputHeight - 1)
        val rightPx = ceil(right * outputWidth).toInt().coerceIn(leftPx + 1, outputWidth)
        val bottomPx = ceil(bottom * outputHeight).toInt().coerceIn(topPx + 1, outputHeight)
        return RectF(
            leftPx.toFloat(),
            topPx.toFloat(),
            rightPx.toFloat(),
            bottomPx.toFloat(),
        )
    }

    private fun drawSlotImage(
        canvas: Canvas,
        source: Bitmap,
        slotRect: RectF,
        transform: MosaicTransform,
        paint: Paint,
    ) {
        val destination = destinationRect(
            sourceWidth = source.width.toFloat(),
            sourceHeight = source.height.toFloat(),
            outputWidth = slotRect.width(),
            outputHeight = slotRect.height(),
            transform = transform,
        ).apply {
            offset(slotRect.left, slotRect.top)
        }
        val checkpoint = canvas.save()
        canvas.clipRect(slotRect)
        canvas.drawBitmap(source, null, destination, paint)
        canvas.restoreToCount(checkpoint)
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
        val drawWidth = sourceWidth * drawScale + 2f
        val drawHeight = sourceHeight * drawScale + 2f
        val extraX = max(drawWidth - outputWidth, 0f)
        val extraY = max(drawHeight - outputHeight, 0f)
        val left = (outputWidth - drawWidth) / 2f + transform.safeOffsetX * extraX / 2f
        val top = (outputHeight - drawHeight) / 2f + transform.safeOffsetY * extraY / 2f
        return RectF(left, top, left + drawWidth, top + drawHeight)
    }

    private fun saveBitmap(
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
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.$OutputExtension")
            put(MediaStore.Images.Media.MIME_TYPE, OutputMimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PhotoGridPlanner")
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
        val outputDir = File(baseDir, "PhotoGridPlanner/Cuts").apply { mkdirs() }
        val file = File(outputDir, "$displayName.$OutputExtension")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return CutTileResult(
            publishIndex = publishIndex,
            row = row,
            column = column,
            displayName = file.name,
            uri = android.net.Uri.fromFile(file),
            file = file,
        )
    }
}
