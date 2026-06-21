package com.niwlayr.app.media

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Checks only definitive missing files. Permission failures stay "available" so a partial
 * photo-library grant never removes a post that still exists on the device.
 */
fun Context.hasFullImageLibraryAccess(): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

fun Context.mediaUriExists(uri: Uri): Boolean {
    return when (uri.scheme) {
        "content" -> runCatching {
            contentResolver.query(
                uri,
                arrayOf(BaseColumns._ID),
                null,
                null,
                null,
            )?.use { cursor -> cursor.moveToFirst() } ?: true
        }.getOrDefault(true)

        "file" -> uri.path?.let(::File)?.exists() ?: true
        else -> true
    }
}
