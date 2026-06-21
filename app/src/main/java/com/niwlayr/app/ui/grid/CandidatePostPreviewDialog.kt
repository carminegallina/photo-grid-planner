package com.niwlayr.app.ui.grid

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
import com.niwlayr.app.analysis.FeedAnalysisResult
import com.niwlayr.app.analysis.FeedAnalyzer
import com.niwlayr.app.analysis.ImageVisualMetrics
import com.niwlayr.app.data.GridPost
import com.niwlayr.app.data.PostKind
import com.niwlayr.app.ui.components.AsyncUriImage
import com.niwlayr.app.ui.i18n.LocalizedText
import kotlin.math.roundToInt

enum class PendingGridImportType {
    Post,
    Carousel,
    Mosaic,
}

data class PendingGridImport(
    val uris: List<Uri>,
    val type: PendingGridImportType,
)

private const val CandidatePostIdPrefix = "candidate-preview-post"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CandidatePostPreviewDialog(
    pendingImport: PendingGridImport,
    currentPosts: List<GridPost>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val context = LocalContext.current
    var insertionIndex by remember(pendingImport, currentPosts) { mutableIntStateOf(0) }
    var selectedCount by remember { mutableIntStateOf(9) }
    var currentAnalysis by remember { mutableStateOf<FeedAnalysisResult?>(null) }
    var projectedAnalysis by remember { mutableStateOf<FeedAnalysisResult?>(null) }
    var isAnalyzing by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val candidatePosts = remember(pendingImport) { pendingImport.toCandidatePosts() }
    val candidateIds = remember(candidatePosts) { candidatePosts.map { it.id }.toSet() }
    val maxInsertionIndex = currentPosts.size
    val safeIndex = insertionIndex.coerceIn(0, maxInsertionIndex)
    val proposedPosts = remember(currentPosts, safeIndex, candidatePosts) {
        currentPosts.toMutableList().apply { addAll(safeIndex, candidatePosts) }
    }
    val previewStartIndex = ((safeIndex / 3 - 1).coerceAtLeast(0)) * 3
    val analysisCount = maxOf(selectedCount, safeIndex + candidatePosts.size)
    val analysisKey = currentPosts.joinToString(separator = "|") { post ->
        "${post.id}:${post.coverUri}:${post.placeholderColor}:${post.hidden}"
    }

    LaunchedEffect(pendingImport, safeIndex, analysisCount, analysisKey) {
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
                    posts = proposedPosts.drop(previewStartIndex).take(9),
                    candidateIds = candidateIds,
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
                        candidateIds = candidateIds,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(safeIndex) }) {
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
    candidateIds: Set<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        posts.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                row.forEach { post ->
                    val isCandidate = post.id in candidateIds
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
    candidateIds: Set<String>,
) {
    val candidateMetric = projected.metrics.firstOrNull { metric ->
        metric.coveredPostIds.any { it in candidateIds }
    }
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

private fun PendingGridImport.toCandidatePosts(): List<GridPost> {
    return when (type) {
        PendingGridImportType.Post -> uris.take(1).map { uri ->
            GridPost(
                id = "$CandidatePostIdPrefix-0",
                kind = PostKind.Image,
                uri = uri.toString(),
            )
        }

        PendingGridImportType.Carousel -> uris.takeIf { it.isNotEmpty() }?.let { media ->
            listOf(
                GridPost(
                    id = "$CandidatePostIdPrefix-carousel",
                    kind = PostKind.Image,
                    uri = media.first().toString(),
                    mediaUris = media.map { it.toString() },
                ),
            )
        }.orEmpty()

        PendingGridImportType.Mosaic -> uris.asReversed().mapIndexed { index, uri ->
            GridPost(
                id = "$CandidatePostIdPrefix-mosaic-$index",
                kind = PostKind.Image,
                uri = uri.toString(),
            )
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
