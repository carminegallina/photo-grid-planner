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
        uri.getQueryParameter("access_token")?.takeIf { it.isNotBlank() }?.let { return it }
        val fragment = uri.fragment.orEmpty()
        return fragment.split("&")
            .mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size == 2) pieces[0] to Uri.decode(pieces[1]) else null
            }
            .firstOrNull { it.first == "access_token" }
            ?.second
            ?.takeIf { it.isNotBlank() }
    }

    fun extractCode(uri: Uri): String? {
        uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let { return it }
        val fragment = uri.fragment.orEmpty()
        return fragment.split("&")
            .mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size == 2) pieces[0] to Uri.decode(pieces[1]) else null
            }
            .firstOrNull { it.first == "code" }
            ?.second
            ?.takeIf { it.isNotBlank() }
    }

    fun extractError(uri: Uri): String? {
        return uri.getQueryParameter("error_description")
            ?: uri.getQueryParameter("error")
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, Charsets.UTF_8.name())
}
