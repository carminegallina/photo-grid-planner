package com.photogridplanner.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PostKind
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.json.JSONArray
import org.json.JSONObject

object ProjectExporter {
    private val TimestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

    fun defaultZipName(prefix: String = "PhotoGridPlanner"): String {
        return "${safeFilePrefix(prefix)}_${LocalDateTime.now().format(TimestampFormat)}.zip"
    }

    fun exportToZip(
        context: Context,
        zipUri: Uri,
        state: PlannerData,
        orderText: String,
    ): Int {
        return exportPostsToZip(
            context = context,
            zipUri = zipUri,
            posts = state.posts,
            exportTitle = "Photo Grid Planner",
            orderText = orderText,
        )
    }

    fun exportPostsToZip(
        context: Context,
        zipUri: Uri,
        posts: List<GridPost>,
        exportTitle: String,
        orderText: String? = null,
    ): Int {
        var copiedMedia = 0
        context.contentResolver.openOutputStream(zipUri)?.use { output ->
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                orderText?.takeIf { it.isNotBlank() }?.let {
                    putText(zip, "ordine_pubblicazione.txt", it)
                }
                putText(zip, "manifest.json", buildManifest(posts, exportTitle).toString(2))
                posts.forEachIndexed { postIndex, post ->
                    post.allMediaUris.forEachIndexed { mediaIndex, mediaUri ->
                        val sourceUri = Uri.parse(mediaUri)
                        val name = mediaFileName(context, sourceUri, postIndex, mediaIndex)
                        zip.putNextEntry(ZipEntry("media/$name"))
                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            BufferedInputStream(input).copyTo(zip)
                            copiedMedia += 1
                        }
                        zip.closeEntry()
                    }
                }
            }
        } ?: error("Impossibile creare il file ZIP.")
        return copiedMedia
    }

    fun exportToFolder(
        context: Context,
        treeUri: Uri,
        state: PlannerData,
        orderText: String,
    ): Int {
        return exportPostsToFolder(
            context = context,
            treeUri = treeUri,
            posts = state.posts,
            exportTitle = "Photo Grid Planner",
            folderPrefix = "PhotoGridPlanner",
            orderText = orderText,
        )
    }

    fun exportPostsToFolder(
        context: Context,
        treeUri: Uri,
        posts: List<GridPost>,
        exportTitle: String,
        folderPrefix: String = "PhotoGridPlanner",
        orderText: String? = null,
    ): Int {
        val resolver = context.contentResolver
        val treeId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
        val folderName = "${safeFilePrefix(folderPrefix)}_${LocalDateTime.now().format(TimestampFormat)}"
        val folderUri = DocumentsContract.createDocument(
            resolver,
            rootUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            folderName,
        ) ?: error("Impossibile creare la cartella di export.")

        orderText?.takeIf { it.isNotBlank() }?.let {
            writeTextDocument(context, folderUri, "ordine_pubblicazione.txt", it)
        }
        writeTextDocument(context, folderUri, "manifest.json", buildManifest(posts, exportTitle).toString(2))

        var copiedMedia = 0
        posts.forEachIndexed { postIndex, post ->
            post.allMediaUris.forEachIndexed { mediaIndex, mediaUri ->
                val sourceUri = Uri.parse(mediaUri)
                val fileName = mediaFileName(context, sourceUri, postIndex, mediaIndex)
                val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
                val targetUri = DocumentsContract.createDocument(
                    resolver,
                    folderUri,
                    mimeType,
                    fileName,
                ) ?: error("Impossibile creare $fileName.")
                resolver.openOutputStream(targetUri)?.use { output ->
                    BufferedOutputStream(output).use { bufferedOutput ->
                        resolver.openInputStream(sourceUri)?.use { input ->
                            BufferedInputStream(input).copyTo(bufferedOutput)
                            copiedMedia += 1
                        }
                    }
                } ?: error("Impossibile scrivere $fileName.")
            }
        }
        return copiedMedia
    }

    private fun writeTextDocument(
        context: Context,
        folderUri: Uri,
        name: String,
        text: String,
    ) {
        val resolver = context.contentResolver
        val uri = DocumentsContract.createDocument(
            resolver,
            folderUri,
            "text/plain",
            name,
        ) ?: error("Impossibile creare $name.")
        resolver.openOutputStream(uri)?.use { output ->
            output.write(text.toByteArray(Charsets.UTF_8))
        } ?: error("Impossibile scrivere $name.")
    }

    private fun putText(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun mediaFileName(
        context: Context,
        uri: Uri,
        postIndex: Int,
        mediaIndex: Int,
    ): String {
        val displayName = queryDisplayName(context, uri)
        val extension = displayName?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?: extensionFromMime(context.contentResolver.getType(uri))
            ?: "bin"
        return "post_${(postIndex + 1).toString().padStart(2, '0')}_slide_${
            (mediaIndex + 1).toString().padStart(2, '0')
        }.$extension"
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)
                    } else {
                        null
                    }
                }
        }.getOrNull()
    }

    private fun extensionFromMime(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
    }

    private fun buildManifest(posts: List<GridPost>, exportTitle: String): JSONObject {
        return JSONObject()
            .put("app", exportTitle)
            .put("exportedAt", LocalDateTime.now().toString())
            .put("postCount", posts.size)
            .put(
                "posts",
                JSONArray().apply {
                    posts.forEachIndexed { index, post ->
                        put(post.toJson(index))
                    }
                },
            )
    }

    private fun safeFilePrefix(prefix: String): String {
        return prefix
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "PhotoGridPlanner" }
    }

    private fun GridPost.toJson(index: Int): JSONObject {
        return JSONObject()
            .put("index", index + 1)
            .put("id", id)
            .put("kind", kind.name)
            .put("mediaCount", allMediaUris.size)
            .put("hidden", hidden)
            .put("scheduledDate", scheduledDate.orEmpty())
            .put("placeholder", kind == PostKind.Placeholder)
    }
}
