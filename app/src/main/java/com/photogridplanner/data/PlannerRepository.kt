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
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
            current.copy(
                posts = current.posts + uris.map { uri ->
                    GridPost(
                        id = UUID.randomUUID().toString(),
                        kind = PostKind.Image,
                        uri = uri,
                    )
                },
            )
        }
    }

    suspend fun addPlaceholder() {
        updateData { current ->
            current.copy(
                posts = current.posts + GridPost(
                    id = UUID.randomUUID().toString(),
                    kind = PostKind.Placeholder,
                ),
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

    suspend fun setInstagramConnection(accessToken: String, userId: String) {
        updateData { current ->
            current.copy(
                instagramAccessToken = accessToken.trim(),
                instagramUserId = userId.trim().ifBlank { "me" },
            )
        }
    }

    suspend fun setInstagramClientId(clientId: String) {
        updateData { current -> current.copy(instagramClientId = clientId.trim()) }
    }

    suspend fun setInstagramClientSecret(clientSecret: String) {
        updateData { current -> current.copy(instagramClientSecret = clientSecret.trim()) }
    }

    suspend fun setInstagramCredentials(clientId: String, clientSecret: String) {
        updateData { current ->
            current.copy(
                instagramClientId = clientId.trim(),
                instagramClientSecret = clientSecret.trim(),
            )
        }
    }

    suspend fun setInstagramPosts(posts: List<InstagramPost>) {
        updateData { current ->
            current.copy(
                instagramPosts = posts,
                instagramOrder = emptyList(),
            )
        }
    }

    suspend fun setInstagramOrder(orderedIds: List<String>) {
        updateData { current -> current.copy(instagramOrder = orderedIds) }
    }

    suspend fun restoreInstagramOriginalOrder() {
        updateData { current -> current.copy(instagramOrder = emptyList()) }
    }

    suspend fun saveCurrentProfileLayout() {
        updateData { current ->
            if (current.orderedInstagramPosts.isEmpty()) {
                current
            } else {
                val name = "Layout " + SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
                current.copy(
                    savedProfileLayouts = listOf(
                        ProfileLayout(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            postIds = current.orderedInstagramPosts.map { it.id },
                        ),
                    ) + current.savedProfileLayouts,
                )
            }
        }
    }

    suspend fun applyProfileLayout(layoutId: String) {
        updateData { current ->
            val layout = current.savedProfileLayouts.firstOrNull { it.id == layoutId } ?: return@updateData current
            current.copy(instagramOrder = layout.postIds)
        }
    }

    suspend fun setPreviewMode(mode: PreviewMode) {
        updateData { current -> current.copy(previewMode = mode) }
    }

    suspend fun setShowHiddenPosts(show: Boolean) {
        updateData { current -> current.copy(showHiddenPosts = show) }
    }

    suspend fun reset() {
        updateData { PlannerData() }
    }

    private suspend fun updateData(transform: (PlannerData) -> PlannerData) {
        dataStore.edit { preferences ->
            val next = transform(preferences.toPlannerData())
            preferences[Keys.PostsJson] = encodePosts(next.posts)
            preferences[Keys.PreviewMode] = next.previewMode.name
            preferences[Keys.ShowHiddenPosts] = next.showHiddenPosts
            preferences[Keys.InstagramAccessToken] = next.instagramAccessToken
            preferences[Keys.InstagramClientId] = next.instagramClientId
            preferences[Keys.InstagramClientSecret] = next.instagramClientSecret
            preferences[Keys.InstagramUserId] = next.instagramUserId
            preferences[Keys.InstagramPostsJson] = encodeInstagramPosts(next.instagramPosts)
            preferences[Keys.InstagramOrderJson] = encodeStringList(next.instagramOrder)
            preferences[Keys.SavedProfileLayoutsJson] = encodeProfileLayouts(next.savedProfileLayouts)
        }
    }

    private fun Preferences.toPlannerData(): PlannerData {
        val posts = decodePosts(this[Keys.PostsJson].orEmpty())
        val mode = runCatching {
            PreviewMode.valueOf(this[Keys.PreviewMode] ?: PreviewMode.Vertical.name)
        }.getOrDefault(PreviewMode.Vertical)
        val showHiddenPosts = this[Keys.ShowHiddenPosts] ?: true
        return PlannerData(
            posts = posts,
            previewMode = mode,
            showHiddenPosts = showHiddenPosts,
            instagramAccessToken = this[Keys.InstagramAccessToken].orEmpty(),
            instagramClientId = this[Keys.InstagramClientId].orEmpty(),
            instagramClientSecret = this[Keys.InstagramClientSecret].orEmpty(),
            instagramUserId = this[Keys.InstagramUserId] ?: "me",
            instagramPosts = decodeInstagramPosts(this[Keys.InstagramPostsJson].orEmpty()),
            instagramOrder = decodeStringList(this[Keys.InstagramOrderJson].orEmpty()),
            savedProfileLayouts = decodeProfileLayouts(this[Keys.SavedProfileLayoutsJson].orEmpty()),
        )
    }

    private fun decodePosts(raw: String): List<GridPost> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        GridPost(
                            id = item.getString("id"),
                            kind = PostKind.valueOf(item.optString("kind", PostKind.Image.name)),
                            uri = item.optString("uri").takeIf { it.isNotBlank() },
                            hidden = item.optBoolean("hidden", false),
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
                    .put("hidden", post.hidden)
                    .put("createdAt", post.createdAt),
            )
        }
        return json.toString()
    }

    private fun decodeInstagramPosts(raw: String): List<InstagramPost> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    val mediaUrl = item.optString("mediaUrl")
                    if (mediaUrl.isNotBlank()) {
                        add(
                            InstagramPost(
                                id = item.getString("id"),
                                mediaUrl = mediaUrl,
                                thumbnailUrl = item.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                                caption = item.optString("caption").takeIf { it.isNotBlank() },
                                permalink = item.optString("permalink").takeIf { it.isNotBlank() },
                                timestamp = item.optString("timestamp").takeIf { it.isNotBlank() },
                                mediaType = item.optString("mediaType").takeIf { it.isNotBlank() },
                            ),
                        )
                    }
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun encodeInstagramPosts(posts: List<InstagramPost>): String {
        val json = JSONArray()
        posts.forEach { post ->
            json.put(
                JSONObject()
                    .put("id", post.id)
                    .put("mediaUrl", post.mediaUrl)
                    .put("thumbnailUrl", post.thumbnailUrl.orEmpty())
                    .put("caption", post.caption.orEmpty())
                    .put("permalink", post.permalink.orEmpty())
                    .put("timestamp", post.timestamp.orEmpty())
                    .put("mediaType", post.mediaType.orEmpty()),
            )
        }
        return json.toString()
    }

    private fun decodeProfileLayouts(raw: String): List<ProfileLayout> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        ProfileLayout(
                            id = item.getString("id"),
                            name = item.optString("name", "Layout"),
                            postIds = decodeStringList(item.optJSONArray("postIds")?.toString().orEmpty()),
                            createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                        ),
                    )
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun encodeProfileLayouts(layouts: List<ProfileLayout>): String {
        val json = JSONArray()
        layouts.forEach { layout ->
            json.put(
                JSONObject()
                    .put("id", layout.id)
                    .put("name", layout.name)
                    .put("postIds", JSONArray(layout.postIds))
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

    private fun encodeStringList(values: List<String>): String {
        return JSONArray(values).toString()
    }

    private object Keys {
        val PostsJson = stringPreferencesKey("posts_json")
        val PreviewMode = stringPreferencesKey("preview_mode")
        val ShowHiddenPosts = booleanPreferencesKey("show_hidden_posts")
        val InstagramAccessToken = stringPreferencesKey("instagram_access_token")
        val InstagramClientId = stringPreferencesKey("instagram_client_id")
        val InstagramClientSecret = stringPreferencesKey("instagram_client_secret")
        val InstagramUserId = stringPreferencesKey("instagram_user_id")
        val InstagramPostsJson = stringPreferencesKey("instagram_posts_json")
        val InstagramOrderJson = stringPreferencesKey("instagram_order_json")
        val SavedProfileLayoutsJson = stringPreferencesKey("saved_profile_layouts_json")
    }
}
