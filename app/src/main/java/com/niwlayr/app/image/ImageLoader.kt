package com.niwlayr.app.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.net.URL
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageLoader {
    suspend fun loadBitmap(
        context: Context,
        uri: Uri,
        maxSize: Int = 2048,
    ): Bitmap = withContext(Dispatchers.IO) {
        if (uri.scheme == "http" || uri.scheme == "https") {
            return@withContext loadFromUrl(uri.toString(), maxSize)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            loadWithImageDecoder(context, uri, maxSize)
        } else {
            loadWithBitmapFactory(context, uri, maxSize)
        }
    }

    private fun loadFromUrl(url: String, maxSize: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        URL(url).openStream().use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }
        return URL(url).openStream().use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: error("Impossibile leggere l'immagine remota.")
    }

    private fun loadWithImageDecoder(context: Context, uri: Uri, maxSize: Int): Bitmap {
        val filePath = uri.path
        val source = if (uri.scheme == "file" && filePath != null) {
            ImageDecoder.createSource(java.io.File(filePath))
        } else {
            ImageDecoder.createSource(context.contentResolver, uri)
        }
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val longest = max(width, height)
            if (longest > maxSize) {
                val scale = maxSize.toFloat() / longest.toFloat()
                decoder.setTargetSize(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    }

    private fun loadWithBitmapFactory(context: Context, uri: Uri, maxSize: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxSize)
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: error("Impossibile leggere l'immagine selezionata.")
    }

    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sample = 1
        var currentWidth = width
        var currentHeight = height
        while (currentWidth / 2 >= maxSize || currentHeight / 2 >= maxSize) {
            currentWidth /= 2
            currentHeight /= 2
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
