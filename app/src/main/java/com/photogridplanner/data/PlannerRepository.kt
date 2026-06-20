package com.photogridplanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.plannerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "photo_grid_planner",
)

class PlannerRepository(context: Context) {
    private val dataStore = context.plannerDataStore

    val data: Flow<PlannerData> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences.toPlannerData() }

    suspend fun addImages(uris: List<String>) {
        if (uris.isEmpty()) return
        updateData { current ->
            val newPosts = uris.map { uri ->
                GridPost(
                    id = UUID.randomUUID().toString(),
                    kind = PostKind.Image,
                    uri = uri,
                )
            }
            current.copy(
                posts = newPosts + current.posts,
            )
        }
    }

    suspend fun insertImage(uri: String, position: Int) {
        updateData { current ->
            val index = position.coerceIn(0, current.posts.size)
            val newPost = GridPost(
                id = UUID.randomUUID().toString(),
                kind = PostKind.Image,
                uri = uri,
            )
            current.copy(
                posts = current.posts.toMutableList().apply {
                    add(index, newPost)
                },
            )
        }
    }

    suspend fun addCarousel(uris: List<String>) {
        if (uris.isEmpty()) return
        updateData { current ->
            current.copy(
                posts = listOf(
                    GridPost(
                        id = UUID.randomUUID().toString(),
                        kind = PostKind.Image,
                        uri = uris.firstOrNull(),
                        mediaUris = uris,
                    ),
                ) + current.posts,
            )
        }
    }

    suspend fun addPlaceholder(
        color: Int = DefaultPlaceholderColor,
        label: String = "",
        type: PlaceholderType = PlaceholderType.Shot,
    ) {
        updateData { current ->
            current.copy(
                posts = listOf(
                    GridPost(
                        id = UUID.randomUUID().toString(),
                        kind = PostKind.Placeholder,
                        placeholderColor = color,
                        placeholderLabel = label,
                        placeholderType = type,
                    ),
                ) + current.posts,
            )
        }
    }

    suspend fun setPlaceholderColor(id: String, color: Int) {
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == id && post.kind == PostKind.Placeholder) {
                        post.copy(placeholderColor = color)
                    } else {
                        post
                    }
                },
            )
        }
    }

    suspend fun setPlaceholderDetails(
        id: String,
        color: Int,
        label: String,
        type: PlaceholderType,
    ) {
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == id && post.kind == PostKind.Placeholder) {
                        post.copy(
                            placeholderColor = color,
                            placeholderLabel = label.trim().take(28),
                            placeholderType = type,
                        )
                    } else {
                        post
                    }
                },
            )
        }
    }

    suspend fun replacePlaceholderWithImage(id: String, uri: String) {
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == id && post.kind == PostKind.Placeholder) {
                        post.copy(
                            kind = PostKind.Image,
                            uri = uri,
                            mediaUris = emptyList(),
                            status = when {
                                post.scheduledDate != null -> PostStatus.Scheduled
                                post.status == PostStatus.Published -> PostStatus.Idea
                                else -> post.status
                            },
                        )
                    } else {
                        post
                    }
                },
            )
        }
    }

    suspend fun setPostDetails(
        id: String,
        caption: String,
        hashtags: String,
        notes: String,
        status: PostStatus,
    ) {
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == id) {
                        post.copy(
                            caption = caption.trim().take(2_200),
                            hashtags = hashtags.trim().take(1_200),
                            notes = notes.trim().take(2_000),
                            status = status,
                        )
                    } else {
                        post
                    }
                },
            )
        }
    }

    suspend fun deletePost(id: String) {
        updateData { current ->
            current.copy(posts = current.posts.filterNot { it.id == id })
        }
    }

    suspend fun togglePostVisibility(id: String) {
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == id) post.copy(hidden = !post.hidden) else post
                },
            )
        }
    }

    suspend fun movePost(fromIndex: Int, toIndex: Int) {
        updateData { current ->
            if (fromIndex !in current.posts.indices || toIndex !in current.posts.indices) {
                current
            } else {
                val mutable = current.posts.toMutableList()
                val post = mutable.removeAt(fromIndex)
                mutable.add(toIndex, post)
                current.copy(posts = mutable)
            }
        }
    }

    suspend fun setPostOrder(orderedIds: List<String>) {
        updateData { current ->
            val postsById = current.posts.associateBy { it.id }
            val orderedPosts = orderedIds.mapNotNull { id -> postsById[id] }
            val orderedPostIds = orderedPosts.map { it.id }.toSet()

            if (orderedPosts.isEmpty()) {
                current
            } else if (orderedPostIds.size == current.posts.size) {
                current.copy(posts = orderedPosts)
            } else {
                val queue = ArrayDeque(orderedPosts)
                current.copy(
                    posts = current.posts.map { post ->
                        if (post.id in orderedPostIds) queue.removeFirst() else post
                    },
                )
            }
        }
    }

    suspend fun clearLocalPosts() {
        updateData { current -> current.copy(posts = emptyList()) }
    }

    suspend fun setPostSchedule(id: String, date: String?) {
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id == id) post.withSchedule(date) else post
                },
            )
        }
    }

    suspend fun setPostsSchedule(ids: Collection<String>, date: String?) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        updateData { current ->
            current.copy(
                posts = current.posts.map { post ->
                    if (post.id in idSet) post.withSchedule(date) else post
                },
            )
        }
    }

    suspend fun autoSchedule(startDate: LocalDate, spacingDays: Int = 1) {
        updateData { current ->
            var cursor = startDate
            current.copy(
                posts = current.posts.map { post ->
                    if (post.kind == PostKind.Image || post.kind == PostKind.Placeholder) {
                        val scheduled = post.withSchedule(cursor.toString())
                        cursor = cursor.plusDays(spacingDays.toLong().coerceAtLeast(1L))
                        scheduled
                    } else {
                        post
                    }
                },
            )
        }
    }

    suspend fun clearSchedule() {
        updateData { current ->
            current.copy(posts = current.posts.map { it.withSchedule(null) })
        }
    }

    suspend fun saveCurrentLayout() {
        updateData { current ->
            if (current.posts.isEmpty()) {
                current
            } else {
                val name = "Layout " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                current.copy(
                    savedLayouts = (
                        listOf(
                            SavedLayout(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                postIds = current.posts.map { it.id },
                                posts = current.posts,
                            ),
                        ) + current.savedLayouts
                        ).take(20),
                )
            }
        }
    }

    suspend fun applySavedLayout(layoutId: String) {
        updateData { current ->
            val layout = current.savedLayouts.firstOrNull { it.id == layoutId } ?: return@updateData current
            if (layout.posts.isNotEmpty()) {
                return@updateData current.copy(posts = layout.posts)
            }

            val postsById = current.posts.associateBy { it.id }
            val orderedPosts = layout.postIds.mapNotNull { postsById[it] }
            val orderedIds = orderedPosts.map { it.id }.toSet()
            if (orderedPosts.isEmpty()) {
                current
            } else {
                current.copy(posts = orderedPosts + current.posts.filterNot { it.id in orderedIds })
            }
        }
    }

    suspend fun deleteSavedLayout(layoutId: String) {
        updateData { current ->
            current.copy(savedLayouts = current.savedLayouts.filterNot { it.id == layoutId })
        }
    }

    suspend fun renameSavedLayout(layoutId: String, name: String) {
        val trimmedName = name.trim().take(48)
        if (trimmedName.isBlank()) return
        updateData { current ->
            current.copy(
                savedLayouts = current.savedLayouts.map { layout ->
                    if (layout.id == layoutId) layout.copy(name = trimmedName) else layout
                },
            )
        }
    }

    suspend fun setCalendarDayPlan(date: String, note: String, recommendedTime: String) {
        val normalizedDate = date.trim()
        if (normalizedDate.isBlank()) return
        val nextPlan = CalendarDayPlan(
            date = normalizedDate,
            note = note.trim().take(240),
            recommendedTime = recommendedTime.trim().take(5),
        )
        updateData { current ->
            val plans = current.calendarPlans.filterNot { it.date == normalizedDate }
            current.copy(
                calendarPlans = if (nextPlan.note.isBlank() && nextPlan.recommendedTime.isBlank()) {
                    plans
                } else {
                    (plans + nextPlan).sortedBy { it.date }
                },
            )
        }
    }

    suspend fun setPreviewMode(mode: PreviewMode) {
        updateData { current -> current.copy(previewMode = mode) }
    }

    suspend fun setShowHiddenPosts(show: Boolean) {
        updateData { current -> current.copy(showHiddenPosts = show) }
    }

    suspend fun setShowTutorialOnLaunch(show: Boolean) {
        updateData { current -> current.copy(showTutorialOnLaunch = show) }
    }

    suspend fun setInitialPermissionPromptCompleted(completed: Boolean) {
        updateData { current -> current.copy(initialPermissionPromptCompleted = completed) }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        updateData { current -> current.copy(notificationsEnabled = enabled) }
    }

    suspend fun setLanguage(language: AppLanguage) {
        updateData { current -> current.copy(language = language) }
    }

    suspend fun reset() {
        updateData { current ->
            PlannerData(
                showTutorialOnLaunch = current.showTutorialOnLaunch,
                initialPermissionPromptCompleted = current.initialPermissionPromptCompleted,
                notificationsEnabled = current.notificationsEnabled,
                language = current.language,
            )
        }
    }

    suspend fun exportBackupJson(): String {
        val snapshot = data.first()
        return JSONObject()
            .put("format", BackupFormat)
            .put("version", BackupVersion)
            .put("planner", snapshot.toBackupJson())
            .toString(2)
    }

    suspend fun restoreBackupJson(raw: String) {
        val root = JSONObject(raw)
        require(root.optString("format") == BackupFormat) { "File di backup non riconosciuto." }
        require(root.optInt("version", 0) <= BackupVersion) { "Il backup richiede una versione piu recente dell'app." }
        val restored = root.getJSONObject("planner").toPlannerDataFromBackup()
        updateData { restored }
    }

    private suspend fun updateData(transform: (PlannerData) -> PlannerData) {
        dataStore.edit { preferences ->
            val next = transform(preferences.toPlannerData())
            preferences[Keys.PostsJson] = encodePosts(next.posts)
            preferences[Keys.PreviewMode] = next.previewMode.name
            preferences[Keys.ShowHiddenPosts] = next.showHiddenPosts
            preferences[Keys.ShowTutorialOnLaunch] = next.showTutorialOnLaunch
            preferences[Keys.InitialPermissionPromptCompleted] = next.initialPermissionPromptCompleted
            preferences[Keys.NotificationsEnabled] = next.notificationsEnabled
            preferences[Keys.AppLanguage] = next.language.name
            preferences[Keys.SavedLayoutsJson] = encodeSavedLayouts(next.savedLayouts)
            preferences[Keys.CalendarPlansJson] = encodeCalendarPlans(next.calendarPlans)
        }
    }

    private fun Preferences.toPlannerData(): PlannerData {
        val posts = decodePosts(this[Keys.PostsJson].orEmpty())
        val mode = runCatching {
            PreviewMode.valueOf(this[Keys.PreviewMode] ?: PreviewMode.Vertical.name)
        }.getOrDefault(PreviewMode.Vertical)
        return PlannerData(
            posts = posts,
            previewMode = mode,
            showHiddenPosts = this[Keys.ShowHiddenPosts] ?: true,
            showTutorialOnLaunch = this[Keys.ShowTutorialOnLaunch] ?: true,
            initialPermissionPromptCompleted = this[Keys.InitialPermissionPromptCompleted] ?: false,
            notificationsEnabled = this[Keys.NotificationsEnabled] ?: false,
            language = runCatching {
                AppLanguage.valueOf(this[Keys.AppLanguage] ?: defaultAppLanguageForDevice().name)
            }.getOrDefault(defaultAppLanguageForDevice()),
            savedLayouts = decodeSavedLayouts(
                this[Keys.SavedLayoutsJson].orEmpty()
                    .ifBlank { this[Keys.LegacySavedProfileLayoutsJson].orEmpty() },
            ),
            calendarPlans = decodeCalendarPlans(this[Keys.CalendarPlansJson].orEmpty()),
        )
    }

    private fun PlannerData.toBackupJson(): JSONObject {
        return JSONObject()
            .put("posts", JSONArray(encodePosts(posts)))
            .put("previewMode", previewMode.name)
            .put("showHiddenPosts", showHiddenPosts)
            .put("showTutorialOnLaunch", showTutorialOnLaunch)
            .put("notificationsEnabled", notificationsEnabled)
            .put("language", language.name)
            .put("savedLayouts", JSONArray(encodeSavedLayouts(savedLayouts)))
            .put("calendarPlans", JSONArray(encodeCalendarPlans(calendarPlans)))
    }

    private fun JSONObject.toPlannerDataFromBackup(): PlannerData {
        val previewMode = runCatching {
            PreviewMode.valueOf(optString("previewMode", PreviewMode.Vertical.name))
        }.getOrDefault(PreviewMode.Vertical)
        val language = runCatching {
            AppLanguage.valueOf(optString("language", defaultAppLanguageForDevice().name))
        }.getOrDefault(defaultAppLanguageForDevice())
        return PlannerData(
            posts = decodePosts(optJSONArray("posts")?.toString().orEmpty()),
            previewMode = previewMode,
            showHiddenPosts = optBoolean("showHiddenPosts", true),
            showTutorialOnLaunch = optBoolean("showTutorialOnLaunch", true),
            notificationsEnabled = optBoolean("notificationsEnabled", false),
            language = language,
            savedLayouts = decodeSavedLayouts(optJSONArray("savedLayouts")?.toString().orEmpty()),
            calendarPlans = decodeCalendarPlans(optJSONArray("calendarPlans")?.toString().orEmpty()),
        )
    }

    private fun decodePosts(raw: String): List<GridPost> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val scheduledDate = item.optString("scheduledDate").takeIf { it.isNotBlank() }
                    val status = if (!item.has("status") && scheduledDate != null) {
                        PostStatus.Scheduled
                    } else {
                        runCatching {
                            PostStatus.valueOf(item.optString("status", PostStatus.Idea.name))
                        }.getOrDefault(PostStatus.Idea)
                    }
                    add(
                        GridPost(
                            id = item.getString("id"),
                            kind = PostKind.valueOf(item.optString("kind", PostKind.Image.name)),
                            uri = item.optString("uri").takeIf { it.isNotBlank() },
                            mediaUris = decodeStringList(item.optJSONArray("mediaUris")?.toString().orEmpty()),
                            placeholderColor = item.optInt("placeholderColor", DefaultPlaceholderColor),
                            placeholderLabel = item.optString("placeholderLabel").takeIf { it.isNotBlank() }.orEmpty(),
                            placeholderType = runCatching {
                                PlaceholderType.valueOf(item.optString("placeholderType", PlaceholderType.Shot.name))
                            }.getOrDefault(PlaceholderType.Shot),
                            hidden = item.optBoolean("hidden", false),
                            scheduledDate = scheduledDate,
                            caption = item.optString("caption"),
                            hashtags = item.optString("hashtags"),
                            notes = item.optString("notes"),
                            status = status,
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun encodePosts(posts: List<GridPost>): String {
        val json = JSONArray()
        posts.forEach { post ->
            json.put(
                JSONObject()
                    .put("id", post.id)
                    .put("kind", post.kind.name)
                    .put("uri", post.uri.orEmpty())
                    .put("mediaUris", JSONArray(post.allMediaUris))
                    .put("placeholderColor", post.placeholderColor)
                    .put("placeholderLabel", post.placeholderLabel)
                    .put("placeholderType", post.placeholderType.name)
                    .put("hidden", post.hidden)
                    .put("scheduledDate", post.scheduledDate.orEmpty())
                    .put("caption", post.caption)
                    .put("hashtags", post.hashtags)
                    .put("notes", post.notes)
                    .put("status", post.status.name)
                    .put("createdAt", post.createdAt),
            )
        }
        return json.toString()
    }

    private fun decodeCalendarPlans(raw: String): List<CalendarDayPlan> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val date = item.optString("date")
                    if (date.isNotBlank()) {
                        add(
                            CalendarDayPlan(
                                date = date,
                                note = item.optString("note"),
                                recommendedTime = item.optString("recommendedTime"),
                            ),
                        )
                    }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun encodeCalendarPlans(plans: List<CalendarDayPlan>): String {
        val json = JSONArray()
        plans.forEach { plan ->
            json.put(
                JSONObject()
                    .put("date", plan.date)
                    .put("note", plan.note)
                    .put("recommendedTime", plan.recommendedTime),
            )
        }
        return json.toString()
    }

    private fun decodeSavedLayouts(raw: String): List<SavedLayout> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        SavedLayout(
                            id = item.getString("id"),
                            name = item.optString("name", "Layout"),
                            postIds = decodeStringList(item.optJSONArray("postIds")?.toString().orEmpty()),
                            posts = decodePosts(item.optJSONArray("posts")?.toString().orEmpty()),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun encodeSavedLayouts(layouts: List<SavedLayout>): String {
        val json = JSONArray()
        layouts.forEach { layout ->
            json.put(
                JSONObject()
                    .put("id", layout.id)
                    .put("name", layout.name)
                    .put("postIds", JSONArray(layout.postIds))
                    .put("posts", JSONArray(encodePosts(layout.posts)))
                    .put("createdAt", layout.createdAt),
            )
        }
        return json.toString()
    }

    private fun decodeStringList(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    json.optString(index).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun GridPost.withSchedule(date: String?): GridPost {
        val nextStatus = when {
            date != null && status != PostStatus.Published -> PostStatus.Scheduled
            date == null && status == PostStatus.Scheduled -> PostStatus.Ready
            else -> status
        }
        return copy(scheduledDate = date, status = nextStatus)
    }

    private object Keys {
        val PostsJson = stringPreferencesKey("posts_json")
        val PreviewMode = stringPreferencesKey("preview_mode")
        val ShowHiddenPosts = booleanPreferencesKey("show_hidden_posts")
        val ShowTutorialOnLaunch = booleanPreferencesKey("show_tutorial_on_launch")
        val InitialPermissionPromptCompleted = booleanPreferencesKey("initial_permission_prompt_completed")
        val NotificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val AppLanguage = stringPreferencesKey("app_language")
        val SavedLayoutsJson = stringPreferencesKey("saved_layouts_json")
        val LegacySavedProfileLayoutsJson = stringPreferencesKey("saved_profile_layouts_json")
        val CalendarPlansJson = stringPreferencesKey("calendar_plans_json")
    }

    private companion object {
        const val BackupFormat = "photo_grid_planner_backup"
        const val BackupVersion = 1
    }
}
