package com.photogridplanner.analysis

enum class ColorTemperature(val label: String) {
    Warm("Calda"),
    Cool("Fredda"),
    Neutral("Neutra"),
}

data class ColorCluster(
    val color: Int,
    val percentage: Float,
    val count: Int,
)

data class ImageVisualMetrics(
    val postId: String,
    val gridIndex: Int,
    val coveredPostIds: List<String> = listOf(postId),
    val coveredIndexes: List<Int> = listOf(gridIndex),
    val visualUnitLabel: String = "Post ${gridIndex + 1}",
    val averageBrightness: Float,
    val averageSaturation: Float,
    val temperature: ColorTemperature,
    val dominantColor: Int,
    val secondaryColor: Int?,
    val colorClusters: List<ColorCluster>,
) {
    val tileCount: Int
        get() = coveredIndexes.size
}

data class FeedScore(
    val colorConsistency: Int,
    val brightnessBalance: Int,
    val saturationBalance: Int,
    val harmony: Int,
    val finalScore: Int,
)

data class FeedRecommendation(
    val title: String,
    val message: String,
    val suggestedColors: List<Int>,
    val brightnessTarget: String,
)

data class FeedAnalysisResult(
    val requestedCount: Int,
    val selectedCount: Int,
    val analyzedCount: Int,
    val visualUnitCount: Int,
    val mosaicGroupCount: Int,
    val groupedPostCount: Int,
    val metrics: List<ImageVisualMetrics>,
    val cellBrightnessValues: List<Float?>,
    val rowBrightnessAverages: List<Float>,
    val columnBrightnessAverages: List<Float>,
    val palette: List<ColorCluster>,
    val recommendations: List<FeedRecommendation>,
    val warnings: List<String>,
    val score: FeedScore,
)
