package com.photogridplanner.ui.analysis

import com.photogridplanner.ui.i18n.LocalizedText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photogridplanner.analysis.ColorCluster
import com.photogridplanner.analysis.FeedAnalysisResult
import com.photogridplanner.analysis.FeedAnalyzer
import com.photogridplanner.analysis.FeedRecommendation
import com.photogridplanner.analysis.ImageVisualMetrics
import com.photogridplanner.data.PlannerData
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedAnalysisScreen(
    state: PlannerData,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedCount by rememberSaveable { mutableIntStateOf(9) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<FeedAnalysisResult?>(null) }
    val analysisKey = state.posts.joinToString(separator = "|") { post ->
        "${post.id}:${post.kind}:${post.allMediaUris.joinToString()}:${post.placeholderColor}:${post.hidden}"
    }

    LaunchedEffect(selectedCount, analysisKey) {
        if (state.posts.isEmpty()) {
            result = null
            errorMessage = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        runCatching {
            FeedAnalyzer.analyze(
                context = context,
                posts = state.posts,
                requestedCount = selectedCount,
            )
        }.onSuccess { analysis ->
            result = analysis
        }.onFailure { error ->
            result = null
            errorMessage = error.message ?: "Analisi non riuscita."
        }
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LocalizedText(
            text = "Analisi Feed",
            style = MaterialTheme.typography.headlineMedium,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(9, 12, 18).forEach { count ->
                FilterChip(
                    selected = selectedCount == count,
                    onClick = { selectedCount = count },
                    label = { LocalizedText("Ultimi $count") },
                )
            }
        }

        val viewState = when {
            state.posts.isEmpty() -> "empty"
            isLoading -> "loading"
            errorMessage != null -> "error"
            result != null -> "result"
            else -> "idle"
        }
        AnimatedContent(
            targetState = viewState,
            label = "analysis_state",
            transitionSpec = {
                (
                    fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                        scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.985f)
                    ).togetherWith(
                        fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                            scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 0.985f),
                    )
            },
        ) { stateKey ->
            when (stateKey) {
                "empty" -> EmptyAnalysisCard()
                "loading" -> LoadingCard()
                "error" -> MessageCard(title = "Analisi non disponibile", message = errorMessage.orEmpty())
                "result" -> result?.let { AnalysisContent(result = it) }
                else -> Unit
            }
        }
    }
}

@Composable
private fun AnalysisContent(result: FeedAnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ScoreCard(result = result)
        RecommendationsCard(recommendations = result.recommendations)
        PaletteCard(palette = result.palette)
        HeatmapCard(result = result)
        WarningsCard(warnings = result.warnings)
        ImageMetricsCard(metrics = result.metrics)
    }
}

@Composable
private fun ScoreCard(result: FeedAnalysisResult) {
    AnalysisPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                LocalizedText("Feed Score", style = MaterialTheme.typography.titleMedium)
                LocalizedText(
                    text = "${result.visualUnitCount} unita visive da ${result.selectedCount} post",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (result.mosaicGroupCount > 0) {
                    LocalizedText(
                        text = "${result.mosaicGroupCount} mosaici rilevati automaticamente",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Surface(
                color = scoreColor(result.score.finalScore).copy(alpha = 0.18f),
                shape = RoundedCornerShape(999.dp),
            ) {
                LocalizedText(
                    text = "${result.score.finalScore}/100",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    color = scoreColor(result.score.finalScore),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        ScoreRow("Coerenza cromatica", result.score.colorConsistency)
        ScoreRow("Bilanciamento luminosita", result.score.brightnessBalance)
        ScoreRow("Bilanciamento saturazione", result.score.saturationBalance)
        ScoreRow("Armonia generale", result.score.harmony)
    }
}

@Composable
private fun ScoreRow(label: String, value: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocalizedText(label, style = MaterialTheme.typography.bodyMedium)
            LocalizedText(
                text = "$value/100",
                style = MaterialTheme.typography.bodyMedium,
                color = scoreColor(value),
                fontWeight = FontWeight.Bold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value / 100f)
                    .height(7.dp)
                    .background(scoreColor(value)),
            )
        }
    }
}

@Composable
private fun RecommendationsCard(recommendations: List<FeedRecommendation>) {
    AnalysisPanel {
        LocalizedText("Cosa pubblicare dopo", style = MaterialTheme.typography.titleMedium)
        if (recommendations.isEmpty()) {
            LocalizedText(
                text = "Aggiungi almeno un'immagine per ricevere suggerimenti cromatici e di luminosita.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            recommendations.forEachIndexed { index, recommendation ->
                Surface(
                    color = if (index == 0) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LocalizedText(
                            text = recommendation.title,
                            style = if (index == 0) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        LocalizedText(
                            text = recommendation.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LocalizedText(
                            text = recommendation.brightnessTarget,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        LocalizedText(
                            text = "Colori compatibili",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            recommendation.suggestedColors.forEach { color ->
                                ColorSwatch(color = color, modifier = Modifier.size(if (index == 0) 34.dp else 28.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteCard(palette: List<ColorCluster>) {
    AnalysisPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Palette, contentDescription = null)
            LocalizedText("Palette colori", style = MaterialTheme.typography.titleMedium)
        }

        if (palette.isEmpty()) {
            LocalizedText(
                text = "Nessun colore dominante disponibile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { cluster ->
                    ColorClusterRow(cluster = cluster)
                }
            }
        }
    }
}

@Composable
private fun ColorClusterRow(cluster: ColorCluster) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorSwatch(color = cluster.color, modifier = Modifier.size(34.dp))
        Column(modifier = Modifier.weight(1f)) {
            LocalizedText(hexColor(cluster.color), style = MaterialTheme.typography.bodyMedium)
            LocalizedText(
                text = "${percentage(cluster.percentage)} del campione",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(cluster.percentage.coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(Color(cluster.color)),
            )
        }
    }
}

@Composable
private fun HeatmapCard(result: FeedAnalysisResult) {
    AnalysisPanel {
        LocalizedText("Heatmap luminosita", style = MaterialTheme.typography.titleMedium)
        HeatmapGrid(values = result.cellBrightnessValues)

        LocalizedText(
            text = "Medie per riga",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        result.rowBrightnessAverages.forEachIndexed { index, value ->
            AverageLine(label = "Riga ${index + 1}", value = value)
        }

        LocalizedText(
            text = "Medie per colonna",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        result.columnBrightnessAverages.forEachIndexed { index, value ->
            AverageLine(label = columnLabel(index), value = value)
        }
    }
}

@Composable
private fun HeatmapGrid(values: List<Float?>) {
    if (values.isEmpty()) {
        LocalizedText(
            text = "Nessun elemento analizzabile.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        values.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { brightness ->
                    val value = brightness ?: 0f
                    val color = if (brightness == null) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        heatColor(value)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalizedText(
                            text = brightness?.let { "${(it * 100).roundToInt()}%" } ?: "-",
                            color = if (value > 0.58f) Color(0xFF17130C) else Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AverageLine(label: String, value: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalizedText(label, style = MaterialTheme.typography.bodyMedium)
        LocalizedText(
            text = "${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun WarningsCard(warnings: List<String>) {
    AnalysisPanel {
        LocalizedText("Avvisi e suggerimenti", style = MaterialTheme.typography.titleMedium)
        warnings.forEach { warning ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                val isPositive = warning.startsWith("Il feed e")
                Icon(
                    imageVector = if (isPositive) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF8BCF9F) else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                LocalizedText(
                    text = warning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ImageMetricsCard(metrics: List<ImageVisualMetrics>) {
    AnalysisPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Analytics, contentDescription = null)
            LocalizedText("Metriche per unita visiva", style = MaterialTheme.typography.titleMedium)
        }

        if (metrics.isEmpty()) {
            LocalizedText(
                text = "Nessun post analizzabile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            metrics.forEach { metric ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LocalizedText(
                            text = if (metric.tileCount > 1) {
                                "M${metric.tileCount}"
                            } else {
                                "#${metric.gridIndex + 1}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(0.35f),
                        )
                        Column(modifier = Modifier.weight(1.65f)) {
                            LocalizedText(
                                text = metric.visualUnitLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            LocalizedText(
                                text = "Luce ${(metric.averageBrightness * 100).roundToInt()}% - Sat ${(metric.averageSaturation * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            LocalizedText(
                                text = "Temperatura ${metric.temperature.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ColorSwatch(color = metric.dominantColor, modifier = Modifier.size(28.dp))
                        metric.secondaryColor?.let { color ->
                            ColorSwatch(color = color, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAnalysisCard() {
    MessageCard(
        title = "Nessun post da analizzare",
        message = "Importa immagini nella griglia o aggiungi placeholder per calcolare le metriche del feed.",
    )
}

@Composable
private fun LoadingCard() {
    AnalysisPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            LocalizedText("Analisi in corso", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String) {
    AnalysisPanel {
        LocalizedText(title, style = MaterialTheme.typography.titleMedium)
        LocalizedText(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnalysisPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.animateContentSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(color),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        ),
        content = {},
    )
}

private fun scoreColor(score: Int): Color {
    return when {
        score >= 76 -> Color(0xFF8BCF9F)
        score >= 56 -> Color(0xFFE9D8B7)
        else -> Color(0xFFE59A86)
    }
}

private fun heatColor(brightness: Float): Color {
    return lerp(
        start = Color(0xFF171B26),
        stop = Color(0xFFF0D8A8),
        fraction = brightness.coerceIn(0f, 1f),
    )
}

private fun percentage(value: Float): String {
    return "${(value * 100).roundToInt().coerceIn(0, 100)}%"
}

private fun hexColor(color: Int): String {
    return String.format(Locale.US, "#%06X", color and 0xFFFFFF)
}

private fun columnLabel(index: Int): String {
    return when (index) {
        0 -> "Colonna sinistra"
        1 -> "Colonna centrale"
        else -> "Colonna destra"
    }
}
