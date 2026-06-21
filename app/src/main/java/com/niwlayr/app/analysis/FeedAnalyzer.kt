package com.niwlayr.app.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import com.niwlayr.app.data.GridPost
import com.niwlayr.app.data.PostKind
import com.niwlayr.app.image.ImageLoader
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FeedAnalyzer {
    private const val Columns = 3
    private const val MaxAnalysisBitmapSize = 420
    private const val MaxClustersPerImage = 5
    private const val MaxFeedPaletteColors = 3
    private const val ProfileVisibleRatio = 1012f / 1080f
    private const val EdgeSimilarityThreshold = 0.11f
    private const val EdgeSamples = 32

    suspend fun analyze(
        context: Context,
        posts: List<GridPost>,
        requestedCount: Int,
    ): FeedAnalysisResult = withContext(Dispatchers.Default) {
        val selected = posts.take(requestedCount.coerceAtLeast(1))
        val unreadableCount = intArrayOf(0)
        val loadedPosts = selected.mapIndexed { index, post ->
            loadPost(context, post, index, unreadableCount)
        }

        try {
            val visualGroups = buildVisualGroups(loadedPosts)
            val metrics = visualGroups.mapNotNull(::analyzeVisualGroup)
            val cellBrightnessValues = buildCellBrightnessValues(selected.size, metrics)
            val rowAverages = buildRowAverages(cellBrightnessValues)
            val columnAverages = buildColumnAverages(cellBrightnessValues)
            val palette = buildFeedPalette(metrics)
            val recommendations = buildRecommendations(metrics)
            val mosaicGroupCount = metrics.count { it.tileCount > 1 }
            val groupedPostCount = metrics.filter { it.tileCount > 1 }.sumOf { it.tileCount }
            val warnings = buildWarnings(
                requestedCount = requestedCount,
                selectedCount = selected.size,
                metrics = metrics,
                cellBrightnessValues = cellBrightnessValues,
                rowAverages = rowAverages,
                columnAverages = columnAverages,
                unreadableCount = unreadableCount[0],
                mosaicGroupCount = mosaicGroupCount,
                groupedPostCount = groupedPostCount,
            )
            val score = buildScore(metrics, palette, warnings)

            FeedAnalysisResult(
                requestedCount = requestedCount,
                selectedCount = selected.size,
                analyzedCount = metrics.size,
                visualUnitCount = metrics.size,
                mosaicGroupCount = mosaicGroupCount,
                groupedPostCount = groupedPostCount,
                metrics = metrics,
                cellBrightnessValues = cellBrightnessValues,
                rowBrightnessAverages = rowAverages,
                columnBrightnessAverages = columnAverages,
                palette = palette,
                recommendations = recommendations,
                warnings = warnings,
                score = score,
            )
        } finally {
            loadedPosts.forEach { it.bitmap?.recycle() }
        }
    }

    private suspend fun loadPost(
        context: Context,
        post: GridPost,
        index: Int,
        unreadableCount: IntArray,
    ): LoadedPost {
        val coverUri = post.coverUri
        if (post.kind != PostKind.Image || coverUri.isNullOrBlank()) {
            return LoadedPost(post = post, index = index, bitmap = null)
        }

        val bitmap = runCatching {
            ImageLoader.loadBitmap(context, Uri.parse(coverUri), maxSize = MaxAnalysisBitmapSize)
        }.getOrElse {
            unreadableCount[0] += 1
            null
        }
        return LoadedPost(post = post, index = index, bitmap = bitmap)
    }

    private fun buildVisualGroups(posts: List<LoadedPost>): List<VisualGroup> {
        if (posts.isEmpty()) return emptyList()
        val parent = IntArray(posts.size) { it }

        fun find(index: Int): Int {
            var current = index
            while (parent[current] != current) {
                parent[current] = parent[parent[current]]
                current = parent[current]
            }
            return current
        }

        fun union(first: Int, second: Int) {
            val firstRoot = find(first)
            val secondRoot = find(second)
            if (firstRoot != secondRoot) {
                parent[secondRoot] = firstRoot
            }
        }

        posts.forEachIndexed { index, current ->
            val rightIndex = index + 1
            if (index % Columns != Columns - 1 && rightIndex < posts.size) {
                val right = posts[rightIndex]
                if (areProfileEdgesContinuous(current.bitmap, right.bitmap, horizontal = true)) {
                    union(index, rightIndex)
                }
            }

            val belowIndex = index + Columns
            if (belowIndex < posts.size) {
                val below = posts[belowIndex]
                if (areProfileEdgesContinuous(current.bitmap, below.bitmap, horizontal = false)) {
                    union(index, belowIndex)
                }
            }
        }

        return posts
            .groupBy { find(it.index) }
            .values
            .flatMap { group ->
                val sorted = group.sortedBy { it.index }
                if (sorted.size > 1 && isRectangularGroup(sorted)) {
                    listOf(VisualGroup(sorted))
                } else {
                    sorted.map { VisualGroup(listOf(it)) }
                }
            }
            .sortedBy { it.indexes.minOrNull() ?: Int.MAX_VALUE }
    }

    private fun isRectangularGroup(group: List<LoadedPost>): Boolean {
        val rows = group.map { it.index / Columns }
        val columns = group.map { it.index % Columns }
        val height = (rows.maxOrNull() ?: 0) - (rows.minOrNull() ?: 0) + 1
        val width = (columns.maxOrNull() ?: 0) - (columns.minOrNull() ?: 0) + 1
        return width * height == group.size
    }

    private fun areProfileEdgesContinuous(
        first: Bitmap?,
        second: Bitmap?,
        horizontal: Boolean,
    ): Boolean {
        if (first == null || second == null) return false
        return edgeDistance(first, second, horizontal) <= EdgeSimilarityThreshold
    }

    private fun edgeDistance(
        first: Bitmap,
        second: Bitmap,
        horizontal: Boolean,
    ): Float {
        var total = 0.0
        repeat(EdgeSamples) { sample ->
            val t = if (EdgeSamples == 1) 0f else sample.toFloat() / (EdgeSamples - 1).toFloat()
            val firstPoint = if (horizontal) {
                val firstInset = profileSideInset(first.width)
                val secondInset = profileSideInset(second.width)
                PixelPoint(
                    x = first.width - firstInset - 1,
                    y = (t * (first.height - 1)).roundToInt(),
                    otherX = secondInset,
                    otherY = (t * (second.height - 1)).roundToInt(),
                )
            } else {
                val firstInset = profileSideInset(first.width)
                val secondInset = profileSideInset(second.width)
                val firstVisibleWidth = first.width - firstInset * 2
                val secondVisibleWidth = second.width - secondInset * 2
                PixelPoint(
                    x = firstInset + (t * (firstVisibleWidth - 1)).roundToInt(),
                    y = first.height - 1,
                    otherX = secondInset + (t * (secondVisibleWidth - 1)).roundToInt(),
                    otherY = 0,
                )
            }
            total += colorDistance(
                first.getPixel(firstPoint.x, firstPoint.y),
                second.getPixel(firstPoint.otherX, firstPoint.otherY),
            )
        }
        return (total / EdgeSamples.toDouble()).toFloat()
    }

    private fun colorDistance(first: Int, second: Int): Float {
        val red = Color.red(first) - Color.red(second)
        val green = Color.green(first) - Color.green(second)
        val blue = Color.blue(first) - Color.blue(second)
        return (sqrt((red * red + green * green + blue * blue).toDouble()) / 441.6729).toFloat()
    }

    private fun analyzeVisualGroup(group: VisualGroup): ImageVisualMetrics? {
        val first = group.items.firstOrNull() ?: return null
        if (first.post.kind == PostKind.Placeholder) {
            return analyzePlaceholderPost(first.post, first.index)
        }

        val bitmaps = group.items.mapNotNull { it.bitmap }
        if (bitmaps.isEmpty()) return null

        val visualBitmap = if (group.items.size == 1) {
            createProfileCrop(bitmaps.first())
        } else {
            composeProfileGroup(group)
        }

        return try {
            analyzeBitmap(
                postId = group.items.joinToString(separator = "+") { it.post.id },
                index = group.indexes.minOrNull() ?: first.index,
                bitmap = visualBitmap,
                coveredPostIds = group.items.map { it.post.id },
                coveredIndexes = group.indexes,
                visualUnitLabel = group.label,
            )
        } finally {
            visualBitmap.recycle()
        }
    }

    private fun analyzePlaceholderPost(
        post: GridPost,
        index: Int,
    ): ImageVisualMetrics {
        val color = post.placeholderColor
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val hsv = FloatArray(3)
        Color.RGBToHSV(r, g, b, hsv)
        val brightness = perceivedBrightness(r, g, b)
        return ImageVisualMetrics(
            postId = post.id,
            gridIndex = index,
            coveredPostIds = listOf(post.id),
            coveredIndexes = listOf(index),
            visualUnitLabel = "Placeholder ${index + 1}",
            averageBrightness = brightness,
            averageSaturation = hsv[1],
            temperature = temperatureFromChannels(r.toFloat(), b.toFloat()),
            dominantColor = color,
            secondaryColor = null,
            colorClusters = listOf(ColorCluster(color = color, percentage = 1f, count = 1)),
        )
    }

    private fun createProfileCrop(bitmap: Bitmap): Bitmap {
        val inset = profileSideInset(bitmap.width)
        val width = (bitmap.width - inset * 2).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, inset, 0, width, bitmap.height)
    }

    private fun composeProfileGroup(group: VisualGroup): Bitmap {
        val rows = group.items.map { it.index / Columns }
        val columns = group.items.map { it.index % Columns }
        val minRow = rows.minOrNull() ?: 0
        val minColumn = columns.minOrNull() ?: 0
        val widthInCells = (columns.maxOrNull() ?: minColumn) - minColumn + 1
        val heightInCells = (rows.maxOrNull() ?: minRow) - minRow + 1
        val cellWidth = group.items.mapNotNull { it.bitmap?.let { bitmap -> profileVisibleWidth(bitmap.width) } }
            .minOrNull()
            ?.coerceAtLeast(1)
            ?: 1
        val cellHeight = group.items.mapNotNull { it.bitmap?.height }.minOrNull()?.coerceAtLeast(1) ?: 1
        val output = Bitmap.createBitmap(cellWidth * widthInCells, cellHeight * heightInCells, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)

        group.items.forEach { item ->
            val bitmap = item.bitmap ?: return@forEach
            val inset = profileSideInset(bitmap.width)
            val source = Rect(
                inset,
                0,
                bitmap.width - inset,
                bitmap.height,
            )
            val destinationLeft = (item.index % Columns - minColumn) * cellWidth
            val destinationTop = (item.index / Columns - minRow) * cellHeight
            val destination = RectF(
                destinationLeft.toFloat(),
                destinationTop.toFloat(),
                (destinationLeft + cellWidth).toFloat(),
                (destinationTop + cellHeight).toFloat(),
            )
            canvas.drawBitmap(bitmap, source, destination, paint)
        }
        return output
    }

    private fun analyzeBitmap(
        postId: String,
        index: Int,
        bitmap: Bitmap,
        coveredPostIds: List<String>,
        coveredIndexes: List<Int>,
        visualUnitLabel: String,
    ): ImageVisualMetrics {
        val strideX = max(1, bitmap.width / 96)
        val strideY = max(1, bitmap.height / 96)
        var count = 0
        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L
        var brightnessSum = 0.0
        var saturationSum = 0.0
        val colorBuckets = mutableMapOf<Int, MutableColorBucket>()
        val fallbackBuckets = mutableMapOf<Int, MutableColorBucket>()

        val hsv = FloatArray(3)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.alpha(pixel) >= 24) {
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    Color.RGBToHSV(red, green, blue, hsv)
                    val brightness = perceivedBrightness(red, green, blue)

                    count += 1
                    redSum += red
                    greenSum += green
                    blueSum += blue
                    brightnessSum += brightness
                    saturationSum += hsv[1].toDouble()

                    val fallbackKey = quantizeColor(red, green, blue)
                    fallbackBuckets.getOrPut(fallbackKey) { MutableColorBucket() }.add(red, green, blue)

                    val isUsableColor = brightness in 0.07f..0.94f && hsv[1] >= 0.06f
                    if (isUsableColor) {
                        val key = quantizeColor(red, green, blue)
                        colorBuckets.getOrPut(key) { MutableColorBucket() }.add(red, green, blue)
                    }
                }
                x += strideX
            }
            y += strideY
        }

        val safeCount = count.coerceAtLeast(1)
        val clusters = bucketsToClusters(
            buckets = colorBuckets.ifEmpty { fallbackBuckets },
            maxClusters = MaxClustersPerImage,
        )
        val fallbackColor = Color.rgb(
            (redSum / safeCount).toInt().coerceIn(0, 255),
            (greenSum / safeCount).toInt().coerceIn(0, 255),
            (blueSum / safeCount).toInt().coerceIn(0, 255),
        )

        return ImageVisualMetrics(
            postId = postId,
            gridIndex = index,
            coveredPostIds = coveredPostIds,
            coveredIndexes = coveredIndexes,
            visualUnitLabel = visualUnitLabel,
            averageBrightness = (brightnessSum / safeCount).toFloat().coerceIn(0f, 1f),
            averageSaturation = (saturationSum / safeCount).toFloat().coerceIn(0f, 1f),
            temperature = temperatureFromChannels(
                red = redSum.toFloat() / safeCount,
                blue = blueSum.toFloat() / safeCount,
            ),
            dominantColor = clusters.firstOrNull()?.color ?: fallbackColor,
            secondaryColor = clusters.getOrNull(1)?.color,
            colorClusters = clusters.ifEmpty {
                listOf(ColorCluster(color = fallbackColor, percentage = 1f, count = safeCount))
            },
        )
    }

    private fun buildCellBrightnessValues(
        selectedCount: Int,
        metrics: List<ImageVisualMetrics>,
    ): List<Float?> {
        val values = MutableList<Float?>(selectedCount) { null }
        metrics.forEach { metric ->
            metric.coveredIndexes.forEach { index ->
                if (index in values.indices) values[index] = metric.averageBrightness
            }
        }
        return values
    }

    private fun buildRowAverages(values: List<Float?>): List<Float> {
        return values
            .chunked(Columns)
            .map { row -> row.filterNotNull().averageOrNull()?.toFloat() ?: 0f }
    }

    private fun buildColumnAverages(values: List<Float?>): List<Float> {
        return (0 until Columns).map { column ->
            val columnValues = values.mapIndexedNotNull { index, value ->
                if (index % Columns == column) value else null
            }
            columnValues.averageOrNull()?.toFloat() ?: 0f
        }
    }

    private fun buildFeedPalette(metrics: List<ImageVisualMetrics>): List<ColorCluster> {
        val buckets = mutableMapOf<Int, MutableColorBucket>()
        metrics.flatMap { it.colorClusters }.forEach { cluster ->
            val red = Color.red(cluster.color)
            val green = Color.green(cluster.color)
            val blue = Color.blue(cluster.color)
            val key = quantizeColor(red, green, blue)
            repeat(cluster.count.coerceAtLeast(1)) {
                buckets.getOrPut(key) { MutableColorBucket() }.add(red, green, blue)
            }
        }
        return bucketsToClusters(buckets, MaxFeedPaletteColors)
    }

    private fun buildWarnings(
        requestedCount: Int,
        selectedCount: Int,
        metrics: List<ImageVisualMetrics>,
        cellBrightnessValues: List<Float?>,
        rowAverages: List<Float>,
        columnAverages: List<Float>,
        unreadableCount: Int,
        mosaicGroupCount: Int,
        groupedPostCount: Int,
    ): List<String> {
        val warnings = mutableListOf<String>()
        if (selectedCount == 0) {
            warnings += "Aggiungi immagini o placeholder alla griglia per avviare l'analisi."
            return warnings
        }
        if (selectedCount < requestedCount) {
            warnings += "Sono disponibili $selectedCount elementi: l'analisi usa solo quelli presenti."
        }
        if (unreadableCount > 0) {
            warnings += "$unreadableCount immagini non sono leggibili dal dispositivo e sono state escluse."
        }
        if (mosaicGroupCount > 0) {
            warnings += "Rilevati $mosaicGroupCount mosaici: $groupedPostCount post vengono valutati come immagini ricomposte."
        }
        if (metrics.size < 3) {
            warnings += "Con meno di 3 unita visive il bilanciamento del feed e solo indicativo."
        }

        val overallBrightness = cellBrightnessValues.filterNotNull().averageOrNull() ?: 0.0
        rowAverages.forEachIndexed { index, value ->
            if (value == 0f) return@forEachIndexed
            val delta = value - overallBrightness
            if (abs(delta) >= 0.16f) {
                val rowName = rowName(index, rowAverages.size)
                val direction = if (delta > 0) "piu chiara" else "piu scura"
                warnings += "La $rowName e molto $direction rispetto al resto della griglia."
            }
        }
        columnAverages.forEachIndexed { index, value ->
            if (value == 0f) return@forEachIndexed
            val delta = value - overallBrightness
            if (abs(delta) >= 0.15f) {
                val columnName = columnName(index)
                val direction = if (delta > 0) "piu chiara" else "piu scura"
                warnings += "La colonna $columnName risulta molto $direction rispetto alla media."
            }
        }

        val averageSaturation = metrics.map { it.averageSaturation }.averageOrNull() ?: 0.0
        if (averageSaturation < 0.18) {
            warnings += "La palette e molto desaturata: funziona bene per feed minimal, ma puo apparire piatta."
        } else if (averageSaturation > 0.72) {
            warnings += "La saturazione media e alta: valuta alternanza con immagini piu calme."
        }

        return warnings.distinct().ifEmpty {
            listOf("Il feed e visivamente bilanciato: non emergono squilibri forti.")
        }
    }

    private fun buildRecommendations(metrics: List<ImageVisualMetrics>): List<FeedRecommendation> {
        val latest = metrics.minByOrNull { it.gridIndex } ?: return emptyList()
        val dominant = latest.dominantColor
        val brightness = latest.averageBrightness
        val colorFamily = colorFamilyName(dominant)
        val brightnessLabel = brightnessLabel(brightness)
        val similarTarget = similarBrightnessTarget(brightness)
        val transitionTarget = transitionBrightnessTarget(brightness)

        return listOf(
            FeedRecommendation(
                title = "Prossimo post consigliato",
                message = "Continua con una foto $brightnessLabel e dominanza $colorFamily, oppure con colori analoghi. Evita cambi di luce estremi nel post immediatamente successivo.",
                suggestedColors = compatibleColors(dominant, mode = ColorSuggestionMode.Analogous),
                brightnessTarget = similarTarget,
            ),
            FeedRecommendation(
                title = "Transizione morbida",
                message = "Se vuoi cambiare atmosfera, usa prima un contenuto ponte: luce intermedia, saturazione piu calma e una tinta vicina alla palette attuale.",
                suggestedColors = compatibleColors(dominant, mode = ColorSuggestionMode.Transition),
                brightnessTarget = transitionTarget,
            ),
            FeedRecommendation(
                title = "Cambio colore controllato",
                message = "Per passare a un colore diverso senza far stonare il feed, usa complementari attenuati o split complementari. Meglio toni smorzati che colori troppo pieni.",
                suggestedColors = compatibleColors(dominant, mode = ColorSuggestionMode.Contrast),
                brightnessTarget = transitionTarget,
            ),
        )
    }

    private fun buildScore(
        metrics: List<ImageVisualMetrics>,
        palette: List<ColorCluster>,
        warnings: List<String>,
    ): FeedScore {
        if (metrics.isEmpty()) {
            return FeedScore(
                colorConsistency = 0,
                brightnessBalance = 0,
                saturationBalance = 0,
                harmony = 0,
                finalScore = 0,
            )
        }

        val brightnessStd = standardDeviation(metrics.map { it.averageBrightness })
        val saturationStd = standardDeviation(metrics.map { it.averageSaturation })
        val brightnessBalance = (100f - brightnessStd * 260f).roundToScore()
        val saturationBalance = (100f - saturationStd * 280f).roundToScore()
        val hueConsistency = dominantHueConsistency(metrics)
        val topPaletteShare = palette.firstOrNull()?.percentage ?: 0f
        val colorConsistency = (hueConsistency * 72f + topPaletteShare * 28f).roundToScore()
        val harmonyPenalty = (
            warnings.count {
                !it.startsWith("Sono disponibili") &&
                    !it.startsWith("Rilevati")
            } * 4
            ).coerceAtMost(16)
        val harmony = ((colorConsistency + brightnessBalance + saturationBalance) / 3f - harmonyPenalty)
            .roundToScore()
        val finalScore = (
            colorConsistency * 0.35f +
                brightnessBalance * 0.28f +
                saturationBalance * 0.22f +
                harmony * 0.15f
            ).roundToScore()

        return FeedScore(
            colorConsistency = colorConsistency,
            brightnessBalance = brightnessBalance,
            saturationBalance = saturationBalance,
            harmony = harmony,
            finalScore = finalScore,
        )
    }

    private fun bucketsToClusters(
        buckets: Map<Int, MutableColorBucket>,
        maxClusters: Int,
    ): List<ColorCluster> {
        val total = buckets.values.sumOf { it.count }.coerceAtLeast(1)
        return buckets.values
            .sortedByDescending { it.count }
            .take(maxClusters)
            .map { bucket ->
                ColorCluster(
                    color = bucket.averageColor(),
                    percentage = bucket.count.toFloat() / total.toFloat(),
                    count = bucket.count,
                )
            }
    }

    private fun dominantHueConsistency(metrics: List<ImageVisualMetrics>): Float {
        if (metrics.size <= 1) return 1f
        val hsv = FloatArray(3)
        var x = 0.0
        var y = 0.0
        metrics.forEach { metric ->
            Color.colorToHSV(metric.dominantColor, hsv)
            val angle = Math.toRadians(hsv[0].toDouble())
            val weight = metric.averageSaturation.coerceAtLeast(0.18f)
            x += cos(angle) * weight
            y += sin(angle) * weight
        }
        val totalWeight = metrics.sumOf { it.averageSaturation.coerceAtLeast(0.18f).toDouble() }
            .coerceAtLeast(0.01)
        return (sqrt(x.pow(2.0) + y.pow(2.0)) / totalWeight).toFloat().coerceIn(0f, 1f)
    }

    private fun standardDeviation(values: List<Float>): Float {
        if (values.size <= 1) return 0f
        val average = values.average()
        val variance = values.sumOf { (it - average).pow(2.0) } / values.size.toDouble()
        return sqrt(variance).toFloat()
    }

    private fun perceivedBrightness(red: Int, green: Int, blue: Int): Float {
        return ((red * 0.299f + green * 0.587f + blue * 0.114f) / 255f).coerceIn(0f, 1f)
    }

    private fun temperatureFromChannels(red: Float, blue: Float): ColorTemperature {
        return when {
            blue - red > 10f -> ColorTemperature.Cool
            red - blue > 10f -> ColorTemperature.Warm
            else -> ColorTemperature.Neutral
        }
    }

    private fun compatibleColors(color: Int, mode: ColorSuggestionMode): List<Int> {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val hue = hsv[0]
        val saturation = hsv[1].coerceIn(0.18f, 0.72f)
        val value = hsv[2].coerceIn(0.28f, 0.78f)
        return when (mode) {
            ColorSuggestionMode.Analogous -> listOf(
                colorFromHsv(hue - 24f, saturation * 0.90f, value),
                colorFromHsv(hue + 24f, saturation * 0.90f, value),
                colorFromHsv(hue, saturation * 0.55f, (value + 0.08f).coerceAtMost(0.82f)),
                colorFromHsv(hue + 8f, saturation * 0.35f, 0.72f),
            )

            ColorSuggestionMode.Transition -> listOf(
                colorFromHsv(hue + 45f, saturation * 0.48f, 0.58f),
                colorFromHsv(hue - 45f, saturation * 0.48f, 0.58f),
                colorFromHsv(hue + 180f, saturation * 0.28f, 0.62f),
                colorFromHsv(hue, saturation * 0.18f, 0.50f),
            )

            ColorSuggestionMode.Contrast -> listOf(
                colorFromHsv(hue + 180f, saturation * 0.58f, value),
                colorFromHsv(hue + 150f, saturation * 0.52f, value),
                colorFromHsv(hue + 210f, saturation * 0.52f, value),
                colorFromHsv(hue + 180f, saturation * 0.25f, 0.64f),
            )
        }
    }

    private fun colorFromHsv(hue: Float, saturation: Float, value: Float): Int {
        val normalizedHue = ((hue % 360f) + 360f) % 360f
        return Color.HSVToColor(
            floatArrayOf(
                normalizedHue,
                saturation.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f),
            ),
        )
    }

    private fun brightnessLabel(value: Float): String {
        return when {
            value < 0.32f -> "scuro/cupo"
            value < 0.48f -> "medio-scuro"
            value < 0.68f -> "medio"
            else -> "luminoso"
        }
    }

    private fun similarBrightnessTarget(value: Float): String {
        return when {
            value < 0.32f -> "Luce consigliata: bassa, circa 20-38%."
            value < 0.48f -> "Luce consigliata: medio-bassa, circa 32-50%."
            value < 0.68f -> "Luce consigliata: media, circa 45-68%."
            else -> "Luce consigliata: alta ma controllata, circa 62-82%."
        }
    }

    private fun transitionBrightnessTarget(value: Float): String {
        return when {
            value < 0.32f -> "Transizione: sali verso 38-55%, prima di passare a foto molto luminose."
            value > 0.68f -> "Transizione: scendi verso 48-62%, prima di passare a foto molto scure."
            else -> "Transizione: resta tra 42-65%, poi cambia gradualmente atmosfera."
        }
    }

    private fun colorFamilyName(color: Int): String {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        val hue = hsv[0]
        return when {
            hsv[1] < 0.12f -> "neutra"
            hue < 18f || hue >= 345f -> "rossa"
            hue < 45f -> "arancio"
            hue < 70f -> "gialla"
            hue < 165f -> "verde"
            hue < 205f -> "ciano"
            hue < 250f -> "blu"
            hue < 305f -> "viola"
            else -> "magenta"
        }
    }

    private fun quantizeColor(red: Int, green: Int, blue: Int): Int {
        val bucketSize = 32
        val r = ((red / bucketSize) * bucketSize + bucketSize / 2).coerceIn(0, 255)
        val g = ((green / bucketSize) * bucketSize + bucketSize / 2).coerceIn(0, 255)
        val b = ((blue / bucketSize) * bucketSize + bucketSize / 2).coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun profileVisibleWidth(width: Int): Int {
        return (width * ProfileVisibleRatio).roundToInt().coerceIn(1, width)
    }

    private fun profileSideInset(width: Int): Int {
        return ((width - profileVisibleWidth(width)) / 2).coerceAtLeast(0)
    }

    private fun rowName(index: Int, total: Int): String {
        return when {
            total == 1 -> "riga"
            index == 0 -> "riga superiore"
            index == total - 1 -> "riga inferiore"
            else -> "riga ${index + 1}"
        }
    }

    private fun columnName(index: Int): String {
        return when (index) {
            0 -> "sinistra"
            1 -> "centrale"
            else -> "destra"
        }
    }

    private fun List<Float>.averageOrNull(): Double? {
        return if (isEmpty()) null else average()
    }

    private fun Float.roundToScore(): Int = roundToInt().coerceIn(0, 100)

    private enum class ColorSuggestionMode {
        Analogous,
        Transition,
        Contrast,
    }

    private data class LoadedPost(
        val post: GridPost,
        val index: Int,
        val bitmap: Bitmap?,
    )

    private data class VisualGroup(
        val items: List<LoadedPost>,
    ) {
        val indexes: List<Int> = items.map { it.index }.sorted()
        val tileCount: Int = items.size
        val label: String
            get() {
                if (tileCount == 1) return "Post ${indexes.first() + 1}"
                val rows = indexes.map { it / Columns }
                val columns = indexes.map { it % Columns }
                val height = (rows.maxOrNull() ?: 0) - (rows.minOrNull() ?: 0) + 1
                val width = (columns.maxOrNull() ?: 0) - (columns.minOrNull() ?: 0) + 1
                return "Mosaico ${width}x${height} (${tileCount} post)"
            }
    }

    private data class PixelPoint(
        val x: Int,
        val y: Int,
        val otherX: Int,
        val otherY: Int,
    )

    private class MutableColorBucket {
        var count: Int = 0
            private set
        private var redSum: Long = 0
        private var greenSum: Long = 0
        private var blueSum: Long = 0

        fun add(red: Int, green: Int, blue: Int) {
            count += 1
            redSum += red
            greenSum += green
            blueSum += blue
        }

        fun averageColor(): Int {
            val safeCount = count.coerceAtLeast(1)
            return Color.rgb(
                (redSum / safeCount).toInt().coerceIn(0, 255),
                (greenSum / safeCount).toInt().coerceIn(0, 255),
                (blueSum / safeCount).toInt().coerceIn(0, 255),
            )
        }
    }
}
