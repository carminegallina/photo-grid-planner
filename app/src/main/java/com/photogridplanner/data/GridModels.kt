package com.photogridplanner.data

import java.util.Locale

enum class PreviewMode(
    val title: String,
    val description: String,
    val aspectRatio: Float,
) {
    Vertical(
        title = "Profilo",
        description = "Profilo verticale moderno",
        aspectRatio = 3f / 4f,
    ),
}

enum class PostKind {
    Image,
    Placeholder,
}

enum class PlaceholderType(val label: String, val shortLabel: String) {
    Shot("Post", "Post"),
    Mosaic("Mosaico", "Mosaico"),
    Carousel("Carosello", "Carosello"),
}

enum class AppLanguage(val label: String) {
    Italian("Italiano"),
    English("English"),
}

fun defaultAppLanguageForDevice(): AppLanguage {
    val language = Locale.getDefault().language.lowercase(Locale.ROOT)
    return if (language == Locale.ITALIAN.language) AppLanguage.Italian else AppLanguage.English
}

val DefaultPlaceholderColor: Int = 0xFF34363D.toInt()

val PlaceholderPresetColors: List<Int> = listOf(
    DefaultPlaceholderColor,
    0xFF4D525C.toInt(),
    0xFF686B70.toInt(),
    0xFF7B7468.toInt(),
    0xFF556258.toInt(),
    0xFF4B6178.toInt(),
)

data class GridPost(
    val id: String,
    val kind: PostKind,
    val uri: String? = null,
    val mediaUris: List<String> = emptyList(),
    val placeholderColor: Int = DefaultPlaceholderColor,
    val placeholderLabel: String = "",
    val placeholderType: PlaceholderType = PlaceholderType.Shot,
    val hidden: Boolean = false,
    val scheduledDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val allMediaUris: List<String>
        get() = when {
            mediaUris.isNotEmpty() -> mediaUris
            !uri.isNullOrBlank() -> listOf(uri)
            else -> emptyList()
        }

    val coverUri: String?
        get() = allMediaUris.firstOrNull()

    val isCarousel: Boolean
        get() = allMediaUris.size > 1

    val placeholderDisplayLabel: String
        get() = placeholderLabel.ifBlank { placeholderType.label }
}

data class SavedLayout(
    val id: String,
    val name: String,
    val postIds: List<String> = emptyList(),
    val posts: List<GridPost> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val itemCount: Int
        get() = if (posts.isNotEmpty()) posts.size else postIds.size
}

data class CalendarDayPlan(
    val date: String,
    val note: String = "",
    val recommendedTime: String = "",
)

data class PlannerData(
    val posts: List<GridPost> = emptyList(),
    val previewMode: PreviewMode = PreviewMode.Vertical,
    val showHiddenPosts: Boolean = true,
    val showTutorialOnLaunch: Boolean = true,
    val initialPermissionPromptCompleted: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val language: AppLanguage = defaultAppLanguageForDevice(),
    val savedLayouts: List<SavedLayout> = emptyList(),
    val calendarPlans: List<CalendarDayPlan> = emptyList(),
) {
    val visiblePosts: List<GridPost>
        get() = if (showHiddenPosts) posts else posts.filterNot { it.hidden }

    fun calendarPlanFor(date: String): CalendarDayPlan? {
        return calendarPlans.firstOrNull { it.date == date }
    }
}
