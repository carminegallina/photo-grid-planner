package com.niwlayr.app.ui.tutorial

import com.niwlayr.app.ui.i18n.LocalizedText

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class TutorialPage(
    val eyebrow: String,
    val title: String,
    val body: String,
    val icon: ImageVector,
    val accent: Color,
)

private val TutorialPages = listOf(
    TutorialPage(
        eyebrow = "Griglia",
        title = "Componi il feed",
        body = "Importa foto dalla libreria, aggiungi placeholder, riordina i post e salva layout diversi da confrontare.",
        icon = Icons.Rounded.ViewModule,
        accent = Color(0xFFEBC16A),
    ),
    TutorialPage(
        eyebrow = "Cutter",
        title = "Prepara post e mosaici",
        body = "Taglia mosaici, crea post 4:5, caroselli e template. Le immagini restano sempre sul dispositivo.",
        icon = Icons.Rounded.ContentCut,
        accent = Color(0xFFE86687),
    ),
    TutorialPage(
        eyebrow = "Analisi",
        title = "Bilancia colori e luce",
        body = "Leggi palette, luminosita e suggerimenti estetici per scegliere cosa pubblicare dopo.",
        icon = Icons.Rounded.Analytics,
        accent = Color(0xFF9A8BE8),
    ),
    TutorialPage(
        eyebrow = "Agenda",
        title = "Pianifica con calma",
        body = "Organizza i post nel calendario e scarica le immagini del giorno quando sei pronto a pubblicare.",
        icon = Icons.Rounded.DateRange,
        accent = Color(0xFF7DAE91),
    ),
    TutorialPage(
        eyebrow = "Privacy",
        title = "Tutto rimane locale",
        body = "Nessun upload, tracking o login. L'app lavora con le foto autorizzate e le elabora offline.",
        icon = Icons.Rounded.Shield,
        accent = Color(0xFFB58ED8),
    ),
)

@Composable
fun AppTutorialDialog(
    onClose: (dontShowAgain: Boolean) -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var dragAmount by remember { mutableFloatStateOf(0f) }
    var dontShowAgain by remember { mutableStateOf(false) }
    val lastPage = TutorialPages.lastIndex
    val swipeThreshold = with(LocalDensity.current) { 68.dp.toPx() }

    fun goNext() {
        if (page < lastPage) page++
    }

    fun goPrevious() {
        if (page > 0) page--
    }

    AlertDialog(
        onDismissRequest = { onClose(dontShowAgain) },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LocalizedText("Guida rapida")
                LocalizedText(
                    text = "Scorri oppure usa Avanti",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 430.dp, max = 600.dp)
                    .pointerInput(page) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAmount = 0f },
                            onHorizontalDrag = { change, amount ->
                                dragAmount += amount
                                change.consume()
                            },
                            onDragEnd = {
                                when {
                                    dragAmount < -swipeThreshold -> goNext()
                                    dragAmount > swipeThreshold -> goPrevious()
                                }
                                dragAmount = 0f
                            },
                            onDragCancel = { dragAmount = 0f },
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AnimatedContent(
                    targetState = page,
                    label = "tutorial_page",
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (
                            fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    tween(320, easing = FastOutSlowInEasing),
                                    initialOffsetX = { it / 4 * direction },
                                ) +
                                scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.985f)
                            ).togetherWith(
                                fadeOut(tween(170, easing = FastOutSlowInEasing)) +
                                    slideOutHorizontally(
                                        tween(260, easing = FastOutSlowInEasing),
                                        targetOffsetX = { -it / 5 * direction },
                                    ),
                            ).using(SizeTransform(clip = false))
                    },
                ) { index ->
                    TutorialPageContent(
                        page = TutorialPages[index],
                        index = index,
                        total = TutorialPages.size,
                    )
                }

                TutorialPagerIndicator(
                    page = page,
                    total = TutorialPages.size,
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                        )
                        LocalizedText(
                            text = "Non mostrare piu all'avvio",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (page < lastPage) {
                        goNext()
                    } else {
                        onClose(dontShowAgain)
                    }
                },
            ) {
                LocalizedText(if (page < lastPage) "Avanti" else "Fine")
            }
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    enabled = page > 0,
                    onClick = ::goPrevious,
                ) {
                    LocalizedText("Indietro")
                }
                OutlinedButton(onClick = { onClose(dontShowAgain) }) {
                    LocalizedText("Salta")
                }
            }
        },
    )
}

@Composable
private fun TutorialPageContent(
    page: TutorialPage,
    index: Int,
    total: Int,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TutorialVisual(page = page, pageIndex = index)
        LocalizedText(
            text = page.eyebrow.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = page.accent,
            fontWeight = FontWeight.SemiBold,
        )
        LocalizedText(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        LocalizedText(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        LocalizedText(
            text = "${index + 1} di $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TutorialVisual(
    page: TutorialPage,
    pageIndex: Int,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(112.dp),
                color = page.accent.copy(alpha = 0.14f),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.dp, page.accent.copy(alpha = 0.32f)),
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(32.dp),
                    tint = page.accent,
                )
            }

            MiniGridPreview(
                accent = page.accent,
                activeIndex = pageIndex,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun MiniGridPreview(
    accent: Color,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            repeat(3) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { column ->
                        val cell = row * 3 + column
                        Surface(
                            modifier = Modifier.size(15.dp),
                            shape = RoundedCornerShape(5.dp),
                            color = if (cell == (activeIndex * 2) % 9) {
                                accent
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            },
                            content = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialPagerIndicator(
    page: Int,
    total: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            Surface(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(height = 7.dp, width = if (index == page) 24.dp else 7.dp),
                shape = CircleShape,
                color = if (index == page) {
                    TutorialPages[index].accent
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                },
                content = {},
            )
        }
    }
}
