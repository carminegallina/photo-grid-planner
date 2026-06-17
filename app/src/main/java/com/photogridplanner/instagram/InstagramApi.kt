package com.photogridplanner.instagram

import com.photogridplanner.data.InstagramPost
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object InstagramApi {
    data class AccessTokenResult(
        val accessToken: String,
        val userId: String,
    )

    suspend fun exchangeCodeForAccessToken(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String,
    ): AccessTokenResult = withContext(Dispatchers.IO) {
        val cleanCode = code.trim().substringBefore("#").removeSuffix("#_").trim()
        val cleanClientId = clientId.trim()
        val cleanClientSecret = clientSecret.trim()

        require(cleanCode.isNotBlank()) { "Codice Instagram non valido." }
        require(cleanClientId.isNotBlank()) { "Inserisci l'Instagram App Client ID." }
        require(cleanClientSecret.isNotBlank()) { "Inserisci l'Instagram App Secret." }

        val body = listOf(
            "client_id" to cleanClientId,
            "client_secret" to cleanClientSecret,
            "grant_type" to "authorization_code",
            "redirect_uri" to redirectUri,
            "code" to cleanCode,
        ).joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }

        val response = postForm(
            url = "https://api.instagram.com/oauth/access_token",
            body = body,
        )

        AccessTokenResult(
            accessToken = response.getString("access_token"),
            userId = response.optString("user_id").takeIf { it.isNotBlank() } ?: "me",
        )
    }

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

    private fun postForm(url: String, body: String): JSONObject {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Length", bodyBytes.size.toString())
        }
        return try {
            connection.outputStream.use { stream -> stream.write(bodyBytes) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(responseBody).optString("error_message")
                        .takeIf { it.isNotBlank() }
                        ?: JSONObject(responseBody).optJSONObject("error")?.optString("message")
                }.getOrNull()
                error(message?.takeIf { it.isNotBlank() } ?: "Errore Instagram OAuth HTTP $code")
            }
            JSONObject(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
