package com.photogridplanner.data

enum class PreviewMode(
    val title: String,
    val description: String,
    val aspectRatio: Float,
) {
    Vertical(
        title = "4:5",
        description = "Profilo verticale moderno",
        aspectRatio = 1080f / 1350f,
    ),
}

enum class PostKind {
    Image,
    Placeholder,
}

data class GridPost(
    val id: String,
    val kind: PostKind,
    val uri: String? = null,
    val hidden: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class InstagramPost(
    val id: String,
    val mediaUrl: String,
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val permalink: String? = null,
    val timestamp: String? = null,
    val mediaType: String? = null,
) {
    val displayUrl: String
        get() = thumbnailUrl ?: mediaUrl
}

data class ProfileLayout(
    val id: String,
    val name: String,
    val postIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
)

data class PlannerData(
    val posts: List<GridPost> = emptyList(),
    val previewMode: PreviewMode = PreviewMode.Vertical,
    val showHiddenPosts: Boolean = true,
    val instagramAccessToken: String = "",
    val instagramClientId: String = "",
    val instagramUserId: String = "me",
    val instagramPosts: List<InstagramPost> = emptyList(),
    val instagramOrder: List<String> = emptyList(),
    val savedProfileLayouts: List<ProfileLayout> = emptyList(),
) {
    val visiblePosts: List<GridPost>
        get() = if (showHiddenPosts) posts else posts.filterNot { it.hidden }

    val orderedInstagramPosts: List<InstagramPost>
        get() {
            if (instagramOrder.isEmpty()) return instagramPosts
            val byId = instagramPosts.associateBy { it.id }
            val ordered = instagramOrder.mapNotNull { byId[it] }
            val orderedIds = ordered.map { it.id }.toSet()
            return ordered + instagramPosts.filterNot { it.id in orderedIds }
        }
}
