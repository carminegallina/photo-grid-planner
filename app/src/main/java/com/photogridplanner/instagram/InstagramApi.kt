package com.photogridplanner.instagram

import com.photogridplanner.data.InstagramPost
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object InstagramApi {
    suspend fun fetchPosts(
        accessToken: String,
        userId: String,
        maxPages: Int = 8,
    ): List<InstagramPost> = withContext(Dispatchers.IO) {
        val cleanToken = accessToken.trim()
        require(cleanToken.isNotBlank()) { "Inserisci un access token Instagram valido." }

        val targetUser = userId.trim().ifBlank { "me" }
        val fields = "id,caption,media_url,thumbnail_url,permalink,timestamp,media_type"
        var nextUrl: String? = "https://graph.instagram.com/v25.0/$targetUser/media" +
            "?fields=${fields.urlEncode()}&access_token=${cleanToken.urlEncode()}"
        val posts = mutableListOf<InstagramPost>()
        var page = 0

        while (nextUrl != null && page < maxPages) {
            val response = getJson(nextUrl)
            response.optJSONArray("data")?.let { data ->
                for (index in 0 until data.length()) {
                    val item = data.getJSONObject(index)
                    val mediaUrl = item.optString("media_url")
                    if (mediaUrl.isNotBlank()) {
                        posts += InstagramPost(
                            id = item.getString("id"),
                            mediaUrl = mediaUrl,
                            thumbnailUrl = item.optString("thumbnail_url").takeIf { it.isNotBlank() },
                            caption = item.optString("caption").takeIf { it.isNotBlank() },
                            permalink = item.optString("permalink").takeIf { it.isNotBlank() },
                            timestamp = item.optString("timestamp").takeIf { it.isNotBlank() },
                            mediaType = item.optString("media_type").takeIf { it.isNotBlank() },
                        )
                    }
                }
            }
            nextUrl = response.optJSONObject("paging")?.optString("next")?.takeIf { it.isNotBlank() }
            page += 1
        }

        posts
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull()
                error(message?.takeIf { it.isNotBlank() } ?: "Errore Instagram API HTTP $code")
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
