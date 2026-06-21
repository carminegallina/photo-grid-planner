package com.niwlayr.app.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.niwlayr.app.data.AppLanguage
import com.niwlayr.app.data.DefaultPlaceholderColor
import com.niwlayr.app.data.GridPost
import com.niwlayr.app.data.PlannerData
import com.niwlayr.app.data.PlannerRepository
import com.niwlayr.app.data.PlaceholderType
import com.niwlayr.app.data.PostKind
import com.niwlayr.app.data.PreviewMode
import com.niwlayr.app.media.hasFullImageLibraryAccess
import com.niwlayr.app.media.mediaUriExists
import com.niwlayr.app.notifications.PublicationReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlannerRepository(application.applicationContext)

    val state: StateFlow<PlannerData> = repository.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = PlannerData(showTutorialOnLaunch = false),
    )

    private var mediaCleanupJob: Job? = null
    private var initialMediaCleanupChecked = false

    init {
        viewModelScope.launch {
            repository.data.collect { data ->
                PublicationReminderScheduler.sync(application.applicationContext, data)
                if (!initialMediaCleanupChecked) {
                    initialMediaCleanupChecked = true
                    removeDeletedMedia(data)
                }
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.addImages(uris.map { it.toString() })
        }
    }

    fun insertImage(uri: Uri, position: Int) {
        persistReadAccess(listOf(uri))
        viewModelScope.launch {
            repository.insertImage(uri.toString(), position)
        }
    }

    fun insertImages(uris: List<Uri>, position: Int) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.insertImages(uris.map { it.toString() }, position)
        }
    }

    fun addCarousel(uris: List<Uri>) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.addCarousel(uris.map { it.toString() })
        }
    }

    fun insertCarousel(uris: List<Uri>, position: Int) {
        persistReadAccess(uris)
        viewModelScope.launch {
            repository.insertCarousel(uris.map { it.toString() }, position)
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

    fun replacePlaceholderWithImage(id: String, uri: Uri) {
        persistReadAccess(listOf(uri))
        viewModelScope.launch { repository.replacePlaceholderWithImage(id, uri.toString()) }
    }

    fun setPostDetails(
        id: String,
        description: String,
        tags: String,
    ) {
        viewModelScope.launch {
            repository.setPostDetails(id, description, tags)
        }
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

    fun setShowTutorialOnLaunch(show: Boolean) {
        viewModelScope.launch { repository.setShowTutorialOnLaunch(show) }
    }

    fun setInitialPermissionPromptCompleted(completed: Boolean = true) {
        viewModelScope.launch { repository.setInitialPermissionPromptCompleted(completed) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setNotificationsEnabled(enabled) }
    }

    fun setAnalyzeImports(enabled: Boolean) {
        viewModelScope.launch { repository.setAnalyzeImports(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { repository.setLanguage(language) }
    }

    /** Checks the device library on launch/resume and removes posts whose file was deleted. */
    fun removeDeletedMedia() {
        removeDeletedMedia(state.value)
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

    suspend fun exportBackupJson(): String = repository.exportBackupJson()

    suspend fun restoreBackupJson(raw: String) {
        repository.restoreBackupJson(raw)
    }

    fun exportOrderText(posts: List<GridPost> = state.value.posts): String {
        val english = state.value.language == AppLanguage.English
        if (posts.isEmpty()) {
            return if (english) {
                "NiwLayr - Creator Studio\nNo posts in the grid."
            } else {
                "NiwLayr - Creator Studio\nNessun post nella griglia."
            }
        }
        return buildString {
            appendLine(
                if (english) "NiwLayr - Creator Studio - post order"
                else "NiwLayr - Creator Studio - ordine post",
            )
            appendLine(
                if (english) {
                    "Publish from bottom right to top left if you are composing a mosaic."
                } else {
                    "Pubblica dal basso a destra verso l'alto a sinistra se stai componendo un mosaico."
                },
            )
            appendLine()
            posts.forEachIndexed { index, post ->
                val label = when (post.kind) {
                    PostKind.Image -> {
                        if (post.isCarousel) {
                            if (english) "carousel (${post.allMediaUris.size} images)"
                            else "carosello (${post.allMediaUris.size} immagini)"
                        } else {
                            post.coverUri ?: if (english) "image" else "immagine"
                        }
                    }
                    PostKind.Placeholder -> "placeholder"
                }
                val visibility = if (post.hidden) {
                    if (english) "hidden" else "nascosto"
                } else {
                    if (english) "visible" else "visibile"
                }
                appendLine("${index + 1}. [$visibility] $label")
            }
        }
    }

    fun shareOrderIntent(): Intent {
        val text = exportOrderText()
        return Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(
                Intent.EXTRA_SUBJECT,
                if (state.value.language == AppLanguage.English) "Grid order" else "Ordine griglia",
            )
            .putExtra(Intent.EXTRA_TEXT, text)
    }

    private fun removeDeletedMedia(data: PlannerData) {
        val context = getApplication<Application>().applicationContext
        if (!context.hasFullImageLibraryAccess() || mediaCleanupJob?.isActive == true) return

        val mediaUris = data.posts
            .flatMap { post -> post.allMediaUris }
            .filter { uri -> uri.isNotBlank() }
            .distinct()
        if (mediaUris.isEmpty()) return

        mediaCleanupJob = viewModelScope.launch(Dispatchers.IO) {
            val missingUris = mediaUris.filter { rawUri ->
                !context.mediaUriExists(Uri.parse(rawUri))
            }
            if (missingUris.isNotEmpty()) {
                repository.removePostsReferencingMedia(missingUris)
            }
        }
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
