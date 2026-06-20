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
    private const val CellWidth = 240
    private const val CellHeight = 320
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

        val rows = scheduled.map { it.index / Columns }.distinct().sorted()
        val rowPositions = rows.withIndex().associate { (position, row) -> row to position }
        val output = Bitmap.createBitmap(
            CellWidth * Columns,
            CellHeight * rows.size.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawColor(Color.BLACK)

        try {
            scheduled.forEach { cell ->
                val bitmap = runCatching {
                    ImageLoader.loadBitmap(context, Uri.parse(cell.post.coverUri), maxSize = MaxDecodeSize)
                }.getOrNull() ?: return@forEach
                try {
                    val row = rowPositions[cell.index / Columns] ?: return@forEach
                    val column = cell.index % Columns
                    val destination = RectF(
                        (column * CellWidth).toFloat(),
                        (row * CellHeight).toFloat(),
                        ((column + 1) * CellWidth).toFloat(),
                        ((row + 1) * CellHeight).toFloat(),
                    )
                    canvas.drawBitmap(bitmap, profileSourceRect(bitmap), destination, paint)
                } finally {
                    bitmap.recycle()
                }
            }

            val directory = File(context.cacheDir, "notification_previews").apply { mkdirs() }
            val file = File(directory, "schedule_${date.replace('-', '_')}.png")
            FileOutputStream(file).use { stream ->
                output.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            file
        } finally {
            output.recycle()
        }
    }

    private fun profileSourceRect(bitmap: Bitmap): Rect {
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
