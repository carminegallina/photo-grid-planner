package com.photogridplanner

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.photogridplanner.data.PlannerRepository
import com.photogridplanner.instagram.InstagramApi
import com.photogridplanner.instagram.InstagramOAuth
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
            val message = if (code.isNullOrBlank()) {
                "Token Instagram non ricevuto."
            } else {
                "Ricevuto un codice OAuth. Per convertirlo in token serve un backend Meta sicuro."
            }
            finishAndReturn(message)
            return
        }

        lifecycleScope.launch {
            val repository = PlannerRepository(applicationContext)
            repository.setInstagramConnection(accessToken, "me")
            runCatching {
                InstagramApi.fetchPosts(accessToken = accessToken, userId = "me")
            }.onSuccess { posts ->
                repository.setInstagramPosts(posts)
                finishAndReturn("Instagram collegato: ${posts.size} post sincronizzati.")
            }.onFailure { error ->
                finishAndReturn(error.message ?: "Instagram collegato, ma sincronizzazione non riuscita.")
            }
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
