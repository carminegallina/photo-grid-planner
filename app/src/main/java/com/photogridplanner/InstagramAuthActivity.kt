package com.photogridplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.photogridplanner.data.PlannerRepository
import com.photogridplanner.instagram.InstagramApi
import com.photogridplanner.instagram.InstagramOAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InstagramAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callbackUri = intent?.data
        if (callbackUri == null) {
            finishAndReturn("Login Instagram non completato.")
            return
        }

        InstagramOAuth.extractError(callbackUri)?.let { error ->
            finishAndReturn("Instagram: $error")
            return
        }

        val accessToken = InstagramOAuth.extractAccessToken(callbackUri)
        if (accessToken.isNullOrBlank()) {
            val code = InstagramOAuth.extractCode(callbackUri)
            if (code.isNullOrBlank()) {
                finishAndReturn("Token Instagram non ricevuto.")
                return
            }

            lifecycleScope.launch {
                val repository = PlannerRepository(applicationContext)
                val settings = repository.data.first()
                runCatching {
                    InstagramApi.exchangeCodeForAccessToken(
                        code = code,
                        clientId = settings.instagramClientId,
                        clientSecret = settings.instagramClientSecret,
                        redirectUri = InstagramOAuth.redirectUriForTokenExchange(callbackUri),
                    )
                }.onSuccess { tokenResult ->
                    repository.setInstagramConnection(tokenResult.accessToken, tokenResult.userId)
                    syncPosts(
                        repository = repository,
                        accessToken = tokenResult.accessToken,
                        userId = tokenResult.userId,
                    )
                }.onFailure { error ->
                    finishAndReturn(error.message ?: "Scambio token Instagram non riuscito.")
                }
            }
            return
        }

        lifecycleScope.launch {
            val repository = PlannerRepository(applicationContext)
            repository.setInstagramConnection(accessToken, "me")
            syncPosts(
                repository = repository,
                accessToken = accessToken,
                userId = "me",
            )
        }
    }

    private suspend fun syncPosts(
        repository: PlannerRepository,
        accessToken: String,
        userId: String,
    ) {
        runCatching {
            InstagramApi.fetchPosts(accessToken = accessToken, userId = userId)
        }.onSuccess { posts ->
            repository.setInstagramPosts(posts)
            finishAndReturn("Instagram collegato: ${posts.size} post sincronizzati.")
        }.onFailure { error ->
            finishAndReturn(error.message ?: "Instagram collegato, ma sincronizzazione non riuscita.")
        }
    }

    private fun finishAndReturn(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        )
        finish()
    }
}
