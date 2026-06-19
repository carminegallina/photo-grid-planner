package com.photogridplanner.ui.tutorial

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class TutorialPage(
    val title: String,
    val body: String,
    val icon: ImageVector,
)

private val TutorialPages = listOf(
    TutorialPage(
        title = "Costruisci la griglia",
        body = "Importa foto dalla libreria, aggiungi placeholder, riordina i post e salva più layout per confrontarli.",
        icon = Icons.Rounded.ViewModule,
    ),
    TutorialPage(
        title = "Taglia e crea",
        body = "Usa Cutter per mosaici, post singoli, caroselli e template. Tutto resta sul dispositivo.",
        icon = Icons.Rounded.ContentCut,
    ),
    TutorialPage(
        title = "Pianifica il feed",
        body = "Organizza i post nel calendario e usa l'analisi estetica per capire colori, luce e coerenza visiva.",
        icon = Icons.Rounded.DateRange,
    ),
    TutorialPage(
        title = "Privacy locale",
        body = "Le immagini vengono lette dalla libreria e lavorate localmente. Nessun upload, tracking o login.",
        icon = Icons.Rounded.Shield,
    ),
)

@Composable
fun AppTutorialDialog(
    onClose: (dontShowAgain: Boolean) -> Unit,
) {
    var page by remember { mutableIntStateOf(0) }
    var dontShowAgain by remember { mutableStateOf(false) }
    val lastPage = TutorialPages.lastIndex

    AlertDialog(
        onDismissRequest = { onClose(dontShowAgain) },
        title = { Text("Guida rapida") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 360.dp, max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                AnimatedContent(
                    targetState = page,
                    label = "tutorial_page",
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (
                            fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(
                                    tween(280, easing = FastOutSlowInEasing),
                                    initialOffsetX = { it / 5 * direction },
                                )
                            ).togetherWith(
                                fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                                    slideOutHorizontally(
                                        tween(220, easing = FastOutSlowInEasing),
                                        targetOffsetX = { -it / 6 * direction },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TutorialPages.indices.forEach { index ->
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (index == page) 18.dp else 7.dp),
                            shape = CircleShape,
                            color = if (index == page) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
                            },
                            content = {},
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(8.dp),
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
                        Text(
                            text = "Non mostrare più all'avvio",
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
                        page++
                    } else {
                        onClose(dontShowAgain)
                    }
                },
            ) {
                Text(if (page < lastPage) "Avanti" else "Fine")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (page > 0) {
                    OutlinedButton(onClick = { page-- }) {
                        Text("Indietro")
                    }
                }
                TextButton(onClick = { onClose(dontShowAgain) }) {
                    Text("Salta")
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
        Surface(
            modifier = Modifier.size(96.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)),
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.padding(26.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = "${index + 1}/$total",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (index == total - 1) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = "Pronto per pianificare il feed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
