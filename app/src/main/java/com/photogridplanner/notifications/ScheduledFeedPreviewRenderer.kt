package com.photogridplanner.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PostKind
import com.photogridplanner.image.ImageLoader
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Renders scheduled posts in their real grid positions for the publication notification. */
object ScheduledFeedPreviewRenderer {
    private const val Columns = 3
    private const val CellWidth = 160
    private const val CellHeight = 200
    private const val PreviewSize = 420
    private const val PreviewInset = 18f
    private const val MaxDecodeSize = 720

    suspend fun render(
        context: Context,
        posts: List<GridPost>,
        date: String,
    ): File? = withContext(Dispatchers.IO) {
        val scheduled = posts.mapIndexedNotNull { index, post ->
            if (post.kind == PostKind.Image && post.scheduledDate == date && !post.coverUri.isNullOrBlank()) {
                ScheduledCell(index = index, post = post)
            } else {
                null
            }
        }
        if (scheduled.isEmpty()) return@withContext null

        val firstRow = scheduled.minOf { it.index / Columns }
        val lastRow = scheduled.maxOf { it.index / Columns }
        val rowCount = lastRow - firstRow + 1
        val sourceGridWidth = CellWidth * Columns.toFloat()
        val sourceGridHeight = CellHeight * rowCount.toFloat()
        val availableSize = PreviewSize - PreviewInset * 2
        val scale = minOf(availableSize / sourceGridWidth, availableSize / sourceGridHeight)
        val gridWidth = sourceGridWidth * scale
        val gridHeight = sourceGridHeight * scale
        val startX = (PreviewSize - gridWidth) / 2f
        val startY = (PreviewSize - gridHeight) / 2f
        val output = Bitmap.createBitmap(
            PreviewSize,
            PreviewSize,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawColor(Color.rgb(20, 22, 29))

        try {
            scheduled.forEach { cell ->
                val bitmap = runCatching {
                    ImageLoader.loadBitmap(context, Uri.parse(cell.post.coverUri), maxSize = MaxDecodeSize)
                }.getOrNull() ?: return@forEach
                try {
                    val row = cell.index / Columns - firstRow
                    val column = cell.index % Columns
                    val destination = RectF(
                        startX + column * CellWidth * scale,
                        startY + row * CellHeight * scale,
                        startX + (column + 1) * CellWidth * scale,
                        startY + (row + 1) * CellHeight * scale,
                    )
                    canvas.drawBitmap(bitmap, instagramSourceRect(bitmap), destination, paint)
                } finally {
                    bitmap.recycle()
                }
            }

            // Keep scheduled previews outside the cache so they survive app closure and cache cleanup.
            val directory = File(context.filesDir, "notification_previews").apply { mkdirs() }
            val file = File(directory, "schedule_${date.replace('-', '_')}.png")
            FileOutputStream(file).use { stream ->
                output.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            file
        } finally {
            output.recycle()
        }
    }

    private fun instagramSourceRect(bitmap: Bitmap): Rect {
        // Generated tiles are 4:5. Keeping that ratio prevents gaps across a mosaic seam.
        val targetAspect = CellWidth.toFloat() / CellHeight.toFloat()
        val sourceAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (sourceAspect > targetAspect) {
            val width = (bitmap.height * targetAspect).roundToInt().coerceAtMost(bitmap.width)
            val left = ((bitmap.width - width) / 2).coerceAtLeast(0)
            Rect(left, 0, left + width, bitmap.height)
        } else {
            val height = (bitmap.width / targetAspect).roundToInt().coerceAtMost(bitmap.height)
            val top = ((bitmap.height - height) / 2).coerceAtLeast(0)
            Rect(0, top, bitmap.width, top + height)
        }
    }

    private data class ScheduledCell(
        val index: Int,
        val post: GridPost,
    )
}
