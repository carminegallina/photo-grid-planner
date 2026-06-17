package com.photogridplanner.instagram

import android.net.Uri
import java.net.URLEncoder

object InstagramOAuth {
    const val AppRedirectUri = "photogridplanner://instagram-auth"
    const val RedirectUri = "https://carminegallina.github.io/photo-grid-planner/instagram-auth.html"

    fun buildLoginUrl(clientId: String): String {
        val scope = "instagram_business_basic"
        return "https://www.instagram.com/oauth/authorize" +
            "?client_id=${clientId.trim().urlEncode()}" +
            "&redirect_uri=${RedirectUri.urlEncode()}" +
            "&scope=${scope.urlEncode()}" +
            "&response_type=code" +
            "&force_reauth=true" +
            "&enable_fb_login=0"
    }

    fun extractAccessToken(uri: Uri): String? {
        return extractParameter(uri, "access_token")
    }

    fun extractCode(uri: Uri): String? {
        return extractParameter(uri, "code")?.cleanOAuthCode()
    }

    fun extractError(uri: Uri): String? {
        return extractParameter(uri, "error_description")
            ?: extractParameter(uri, "error")
    }

    fun redirectUriForTokenExchange(callbackUri: Uri): String {
        return if (callbackUri.scheme == "https" && callbackUri.host == "carminegallina.github.io") {
            callbackUri.buildUpon()
                .clearQuery()
                .fragment(null)
                .build()
                .toString()
        } else {
            RedirectUri
        }
    }

    private fun extractParameter(uri: Uri, name: String): String? {
        sequenceOf(uri.encodedQuery.orEmpty(), uri.encodedFragment.orEmpty())
            .filter { it.isNotBlank() }
            .forEach { source ->
                source.split("&")
                    .mapNotNull { part ->
                        val pieces = part.split("=", limit = 2)
                        if (pieces.size == 2) Uri.decode(pieces[0]) to Uri.decode(pieces[1]) else null
                    }
                    .firstOrNull { it.first == name }
                    ?.second
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        return null
    }

    private fun String.cleanOAuthCode(): String {
        return trim()
            .substringBefore("#")
            .removeSuffix("#_")
            .trim()
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
