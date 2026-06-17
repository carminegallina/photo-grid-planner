package com.photogridplanner.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.InstagramPost
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PlannerRepository
import com.photogridplanner.data.PostKind
import com.photogridplanner.data.PreviewMode
import com.photogridplanner.instagram.InstagramApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstagramSyncState(
    val loading: Boolean = false,
    val message: String? = null,
)

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlannerRepository(application.applicationContext)
    private val _instagramSyncState = MutableStateFlow(InstagramSyncState())

    val state: StateFlow<PlannerData> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PlannerData(),
    )
    val instagramSyncState: StateFlow<InstagramSyncState> = _instagramSyncState.asStateFlow()

    fun addImages(uris: List<Uri>) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.addImages(uris.map { it.toString() })
        }
    }

    fun addPlaceholder() {
        viewModelScope.launch { repository.addPlaceholder() }
    }

    fun deletePost(id: String) {
        viewModelScope.launch { repository.deletePost(id) }
    }

    fun togglePostVisibility(id: String) {
        viewModelScope.launch { repository.togglePostVisibility(id) }
    }

    fun movePost(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch { repository.movePost(fromIndex, toIndex) }
    }

    fun setPostOrder(posts: List<GridPost>) {
        viewModelScope.launch { repository.setPostOrder(posts.map { it.id }) }
    }

    fun connectInstagram(accessToken: String, userId: String) {
        viewModelScope.launch {
            repository.setInstagramConnection(accessToken, userId)
            syncInstagramProfile(accessToken, userId)
        }
    }

    fun setInstagramClientId(clientId: String) {
        viewModelScope.launch { repository.setInstagramClientId(clientId) }
    }

    fun setInstagramClientSecret(clientSecret: String) {
        viewModelScope.launch { repository.setInstagramClientSecret(clientSecret) }
    }

    fun saveInstagramCredentials(
        clientId: String,
        clientSecret: String,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            repository.setInstagramCredentials(clientId, clientSecret)
            onSaved()
        }
    }

    fun syncInstagramProfile(
        accessToken: String = state.value.instagramAccessToken,
        userId: String = state.value.instagramUserId,
    ) {
        viewModelScope.launch {
            _instagramSyncState.value = InstagramSyncState(loading = true)
            runCatching {
                InstagramApi.fetchPosts(accessToken = accessToken, userId = userId)
            }.onSuccess { posts ->
                repository.setInstagramPosts(posts)
                _instagramSyncState.value = InstagramSyncState(
                    message = "Sincronizzati ${posts.size} post Instagram.",
                )
            }.onFailure { error ->
                _instagramSyncState.value = InstagramSyncState(
                    message = error.message ?: "Connessione Instagram non riuscita.",
                )
            }
        }
    }

    fun setInstagramOrder(posts: List<InstagramPost>) {
        viewModelScope.launch { repository.setInstagramOrder(posts.map { it.id }) }
    }

    fun restoreInstagramOriginalOrder() {
        viewModelScope.launch { repository.restoreInstagramOriginalOrder() }
    }

    fun saveCurrentProfileLayout() {
        viewModelScope.launch { repository.saveCurrentProfileLayout() }
    }

    fun applyProfileLayout(layoutId: String) {
        viewModelScope.launch { repository.applyProfileLayout(layoutId) }
    }

    fun setPreviewMode(mode: PreviewMode) {
        viewModelScope.launch { repository.setPreviewMode(mode) }
    }

    fun setShowHiddenPosts(show: Boolean) {
        viewModelScope.launch { repository.setShowHiddenPosts(show) }
    }

    fun resetProject() {
        viewModelScope.launch { repository.reset() }
    }

    fun exportOrderText(posts: List<GridPost> = state.value.posts): String {
        if (posts.isEmpty()) return "Photo Grid Planner\nNessun post nella griglia."
        return buildString {
            appendLine("Photo Grid Planner - ordine post")
            appendLine("Pubblica dal basso a destra verso l'alto a sinistra se stai componendo un mosaico.")
            appendLine()
            posts.forEachIndexed { index, post ->
                val label = when (post.kind) {
                    PostKind.Image -> post.uri ?: "immagine"
                    PostKind.Placeholder -> "placeholder"
                }
                val visibility = if (post.hidden) "nascosto" else "visibile"
                appendLine("${index + 1}. [$visibility] $label")
            }
        }
    }

    fun shareOrderIntent(): Intent {
        val text = exportOrderText()
        return Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, "Ordine griglia Instagram")
            .putExtra(Intent.EXTRA_TEXT, text)
    }

    private fun persistReadAccess(uris: List<Uri>) {
        val resolver = getApplication<Application>().contentResolver
        uris.forEach { uri ->
            runCatching {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }
}
