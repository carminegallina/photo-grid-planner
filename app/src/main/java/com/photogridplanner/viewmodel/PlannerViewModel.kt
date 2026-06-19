package com.photogridplanner.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photogridplanner.data.DefaultPlaceholderColor
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PlannerRepository
import com.photogridplanner.data.PlaceholderType
import com.photogridplanner.data.PostKind
import com.photogridplanner.data.PreviewMode
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlannerRepository(application.applicationContext)

    val state: StateFlow<PlannerData> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PlannerData(),
    )

    fun addImages(uris: List<Uri>) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.addImages(uris.map { it.toString() })
        }
    }

    fun addCarousel(uris: List<Uri>) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.addCarousel(uris.map { it.toString() })
        }
    }

    fun addPlaceholder(color: Int = DefaultPlaceholderColor) {
        viewModelScope.launch { repository.addPlaceholder(color) }
    }

    fun setPlaceholderColor(id: String, color: Int) {
        viewModelScope.launch { repository.setPlaceholderColor(id, color) }
    }

    fun setPlaceholderDetails(id: String, color: Int, label: String, type: PlaceholderType) {
        viewModelScope.launch { repository.setPlaceholderDetails(id, color, label, type) }
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

    fun saveCurrentLayout() {
        viewModelScope.launch { repository.saveCurrentLayout() }
    }

    fun applySavedLayout(layoutId: String) {
        viewModelScope.launch { repository.applySavedLayout(layoutId) }
    }

    fun deleteSavedLayout(layoutId: String) {
        viewModelScope.launch { repository.deleteSavedLayout(layoutId) }
    }

    fun renameSavedLayout(layoutId: String, name: String) {
        viewModelScope.launch { repository.renameSavedLayout(layoutId, name) }
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

    fun clearLocalGrid() {
        viewModelScope.launch { repository.clearLocalPosts() }
    }

    fun setPostSchedule(id: String, date: LocalDate?) {
        viewModelScope.launch { repository.setPostSchedule(id, date?.toString()) }
    }

    fun setPostsSchedule(ids: Collection<String>, date: LocalDate?) {
        if (ids.isEmpty()) return
        viewModelScope.launch { repository.setPostsSchedule(ids, date?.toString()) }
    }

    fun autoSchedule(startDate: LocalDate = LocalDate.now().plusDays(1), spacingDays: Int = 1) {
        viewModelScope.launch { repository.autoSchedule(startDate, spacingDays) }
    }

    fun clearSchedule() {
        viewModelScope.launch { repository.clearSchedule() }
    }

    fun setCalendarDayPlan(date: LocalDate, note: String, recommendedTime: String) {
        viewModelScope.launch {
            repository.setCalendarDayPlan(
                date = date.toString(),
                note = note,
                recommendedTime = recommendedTime,
            )
        }
    }

    fun exportOrderText(posts: List<GridPost> = state.value.posts): String {
        if (posts.isEmpty()) return "Photo Grid Planner\nNessun post nella griglia."
        return buildString {
            appendLine("Photo Grid Planner - ordine post")
            appendLine("Pubblica dal basso a destra verso l'alto a sinistra se stai componendo un mosaico.")
            appendLine()
            posts.forEachIndexed { index, post ->
                val label = when (post.kind) {
                    PostKind.Image -> {
                        if (post.isCarousel) {
                            "carosello (${post.allMediaUris.size} immagini)"
                        } else {
                            post.coverUri ?: "immagine"
                        }
                    }
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
            .putExtra(Intent.EXTRA_SUBJECT, "Ordine griglia")
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
