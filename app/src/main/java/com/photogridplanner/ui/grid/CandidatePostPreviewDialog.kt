package com.photogridplanner.ui.grid

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photogridplanner.analysis.FeedAnalysisResult
import com.photogridplanner.analysis.FeedAnalyzer
import com.photogridplanner.analysis.ImageVisualMetrics
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PostKind
import com.photogridplanner.ui.components.AsyncUriImage
import com.photogridplanner.ui.i18n.LocalizedText
import kotlin.math.roundToInt

private const val CandidatePostId = "candidate-preview-post"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidatePostPreviewDialog(
    uri: Uri,
    currentPosts: List<GridPost>,
    onDismiss: () -> Unit,
    onAddAt: (Int) -> Unit,
) {
    val context = LocalContext.current
    var insertionIndex by remember(uri, currentPosts) { mutableIntStateOf(0) }
    var selectedCount by remember { mutableIntStateOf(9) }
    var currentAnalysis by remember { mutableStateOf<FeedAnalysisResult?>(null) }
    var projectedAnalysis by remember { mutableStateOf<FeedAnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val maxInsertionIndex = minOf(currentPosts.size, 17)
    val safeIndex = insertionIndex.coerceIn(0, maxInsertionIndex)
    val candidate = remember(uri) {
        GridPost(
            id = CandidatePostId,
            kind = PostKind.Image,
            uri = uri.toString(),
        )
    }
    val proposedPosts = remember(currentPosts, safeIndex, candidate) {
        currentPosts.toMutableList().apply { add(safeIndex, candidate) }
    }
    val analysisCount = maxOf(selectedCount, safeIndex + 1).coerceAtMost(18)
    val analysisKey = currentPosts.joinToString(separator = "|") { post ->
        "${post.id}:${post.coverUri}:${post.placeholderColor}:${post.hidden}"
    }

    LaunchedEffect(uri, safeIndex, analysisCount, analysisKey) {
        isAnalyzing = true
        error = null
        runCatching {
            val baseline = if (currentPosts.isEmpty()) null else {
                FeedAnalyzer.analyze(context, currentPosts, analysisCount)
            }
            val projected = FeedAnalyzer.analyze(context, proposedPosts, analysisCount)
            baseline to projected
        }.onSuccess { (baseline, projected) ->
            currentAnalysis = baseline
            projectedAnalysis = projected
        }.onFailure {
            currentAnalysis = null
            projectedAnalysis = null
            error = it.message ?: "Analisi non riuscita."
        }
        isAnalyzing = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { LocalizedText("Anteprima prima di aggiungere") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 590.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CandidateGridPreview(
                    posts = proposedPosts.take(9),
                    candidateId = CandidatePostId,
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LocalizedText("Posizione", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(
                            onClick = { insertionIndex = (safeIndex - 1).coerceAtLeast(0) },
                            enabled = safeIndex > 0,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Posizione precedente")
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            LocalizedText(
                                text = "Post ${safeIndex + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            LocalizedText(
                                text = "Riga ${safeIndex / 3 + 1}, colonna ${safeIndex % 3 + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { insertionIndex = (safeIndex + 1).coerceAtMost(maxInsertionIndex) },
                            enabled = safeIndex < maxInsertionIndex,
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Posizione successiva")
                        }
                    }
                }

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

                when {
                    isAnalyzing -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(30.dp))
                    }

                    error != null -> LocalizedText(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )

                    projectedAnalysis != null -> CandidateImpactContent(
                        baseline = currentAnalysis,
                        projected = projectedAnalysis!!,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAddAt(safeIndex) }) {
                LocalizedText("Aggiungi alla griglia")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                LocalizedText("Annulla")
            }
        },
    )
}

@Composable
private fun CandidateGridPreview(
    posts: List<GridPost>,
    candidateId: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        posts.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { post ->
                    val isCandidate = post.id == candidateId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(4.dp)),
                    ) {
                        when {
                            post.kind == PostKind.Placeholder -> Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(post.placeholderColor)),
                            )

                            post.coverUri != null -> AsyncUriImage(
                                uri = post.coverUri.orEmpty(),
                                contentScale = ContentScale.Crop,
                                maxSize = 180,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (isCandidate) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(4.dp)),
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                                ) {}
                            }
                        }
                    }
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(3f / 4f))
                }
            }
        }
    }
}

@Composable
private fun CandidateImpactContent(
    baseline: FeedAnalysisResult?,
    projected: FeedAnalysisResult,
) {
    val candidateMetric = projected.metrics.firstOrNull { CandidatePostId in it.coveredPostIds }
    val delta = projected.score.finalScore - (baseline?.score?.finalScore ?: projected.score.finalScore)
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    LocalizedText("Feed Score previsto", style = MaterialTheme.typography.titleSmall)
                    baseline?.let {
                        LocalizedText(
                            text = "Attuale ${it.score.finalScore}/100",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                LocalizedText(
                    text = buildString {
                        append("${projected.score.finalScore}/100")
                        if (baseline != null) append(if (delta >= 0) "  +$delta" else "  $delta")
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                )
            }
            CandidateMetricRow(metric = candidateMetric)
            LocalizedText(
                text = "Palette prevista",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                projected.palette.take(4).forEach { cluster ->
                    Surface(
                        modifier = Modifier.size(30.dp),
                        color = Color(cluster.color),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun CandidateMetricRow(metric: ImageVisualMetrics?) {
    if (metric == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LocalizedText(
            text = "Luminosita ${(metric.averageBrightness * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        LocalizedText(
            text = "Saturazione ${(metric.averageSaturation * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
