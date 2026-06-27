package com.niwlayr.app.share

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/**
 * Copies images shared into the app (Android share sheet) from another app into private
 * storage, returning stable file:// uris. Shared content uris are only granted temporary
 * read access, so a copy is what lets the imported post survive after the sharing app — or
 * the app itself — is closed. No network, nothing leaves the device.
 */
object SharedImageImporter {
    private const val DirectoryName = "shared_imports"

    fun importToLocal(context: Context, sources: List<Uri>): List<Uri> {
        if (sources.isEmpty()) return emptyList()
        val directory = File(context.filesDir, DirectoryName).apply { mkdirs() }
        val resolver = context.contentResolver
        return sources.mapNotNull { source ->
            runCatching {
                val extension = when (resolver.getType(source)) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> "jpg"
                }
                val target = File(directory, "${UUID.randomUUID()}.$extension")
                val copied = resolver.openInputStream(source)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                if (copied == null || target.length() == 0L) {
                    target.delete()
                    null
                } else {
                    Uri.fromFile(target)
                }
            }.getOrNull()
        }
    }
}
