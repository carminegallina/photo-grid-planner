package com.photogridplanner.ui.calendar

import com.photogridplanner.ui.i18n.LocalAppStrings
import com.photogridplanner.ui.i18n.LocalizedText

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photogridplanner.data.AppLanguage
import com.photogridplanner.data.CalendarDayPlan
import com.photogridplanner.data.GridPost
import com.photogridplanner.data.PlannerData
import com.photogridplanner.data.PostKind
import com.photogridplanner.export.ProjectExporter
import com.photogridplanner.ui.components.AsyncUriImage
import com.photogridplanner.ui.components.FullScreenPreview
import com.photogridplanner.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalendarScreen(
    state: PlannerData,
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val today = LocalDate.now()
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var weekAnchorKey by rememberSaveable { mutableStateOf(today.toString()) }
    var calendarMode by rememberSaveable { mutableStateOf("month") }
    var showOnlyUnscheduled by rememberSaveable { mutableStateOf(false) }
    val visibleMonth = YearMonth.parse(monthKey)
    val weekAnchor = runCatching { LocalDate.parse(weekAnchorKey) }.getOrDefault(today)
    val weekStart = weekAnchor.minusDays((weekAnchor.dayOfWeek.value - 1).toLong())
    val calendarLocale = if (state.language == AppLanguage.Italian) Locale.ITALIAN else Locale.ENGLISH
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", calendarLocale)
    val dayFormatter = DateTimeFormatter.ofPattern("d MMM", calendarLocale)
    val selectedPostIds = rememberSaveable { mutableStateOf(emptyList<String>()) }
    val selectedIdSet = selectedPostIds.value.toSet()
    val scheduledCount = state.posts.count { !it.scheduledDate.isNullOrBlank() }
    var openDateKey by rememberSaveable { mutableStateOf<String?>(null) }
    var previewUris by androidx.compose.runtime.remember { mutableStateOf<List<String>>(emptyList()) }
    var exportPosts by androidx.compose.runtime.remember { mutableStateOf<List<GridPost>>(emptyList()) }
    var exportDateLabel by androidx.compose.runtime.remember { mutableStateOf("") }
    val zipExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            uri?.let {
                val postsToExport = exportPosts
                val label = exportDateLabel
                scope.launch {
                    val copied = withContext(Dispatchers.IO) {
                        ProjectExporter.exportPostsToZip(
                            context = context,
                            zipUri = it,
                            posts = postsToExport,
                            exportTitle = "Photo Grid Planner - $label",
                        )
                    }
                    Toast.makeText(context, strings.t("ZIP giornata esportato: $copied immagini"), Toast.LENGTH_SHORT).show()
                }
            }
        },
    )
    val folderExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                val postsToExport = exportPosts
                val label = exportDateLabel
                scope.launch {
                    val copied = withContext(Dispatchers.IO) {
                        ProjectExporter.exportPostsToFolder(
                            context = context,
                            treeUri = it,
                            posts = postsToExport,
                            exportTitle = "Photo Grid Planner - $label",
                            folderPrefix = "PhotoGridPlanner_${label.replace('/', '-')}",
                        )
                    }
                    Toast.makeText(context, strings.t("Cartella giornata esportata: $copied immagini"), Toast.LENGTH_SHORT).show()
                }
            }
        },
    )

    LaunchedEffect(state.posts.map { it.id }) {
        val validIds = state.posts.map { it.id }.toSet()
        selectedPostIds.value = selectedPostIds.value.filter { it in validIds }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CalendarHeader()

        CalendarOverviewCard(
            scheduledCount = scheduledCount,
            selectedCount = selectedPostIds.value.size,
            totalCount = state.posts.size,
            canClearSchedule = state.posts.any { !it.scheduledDate.isNullOrBlank() },
            onClearSchedule = {
                selectedPostIds.value = emptyList()
                viewModel.clearSchedule()
            },
        )

        PostSelectionStrip(
            posts = if (showOnlyUnscheduled) {
                state.posts.filter { it.scheduledDate.isNullOrBlank() }
            } else {
                state.posts
            },
            selectedIds = selectedIdSet,
            showOnlyUnscheduled = showOnlyUnscheduled,
            onToggleUnscheduledFilter = { showOnlyUnscheduled = !showOnlyUnscheduled },
            onTogglePost = { post ->
                selectedPostIds.value = if (post.id in selectedIdSet) {
                    selectedPostIds.value.filterNot { it == post.id }
                } else {
                    selectedPostIds.value + post.id
                }
            },
            onClearPostDate = { post ->
                selectedPostIds.value = selectedPostIds.value.filterNot { it == post.id }
                viewModel.setPostSchedule(post.id, null)
            },
        )

        MonthCalendar(
            dates = if (calendarMode == "week") {
                (0 until 7).map { weekStart.plusDays(it.toLong()) }
            } else {
                monthCells(visibleMonth)
            },
            today = today,
            posts = state.posts,
            plans = state.calendarPlans.associateBy { it.date },
            mode = calendarMode,
            selectedCount = selectedPostIds.value.size,
            onModeChange = { calendarMode = it },
            onPrevious = {
                if (calendarMode == "week") {
                    weekAnchorKey = weekAnchor.minusWeeks(1).toString()
                } else {
                    monthKey = visibleMonth.minusMonths(1).toString()
                }
            },
            onNext = {
                if (calendarMode == "week") {
                    weekAnchorKey = weekAnchor.plusWeeks(1).toString()
                } else {
                    monthKey = visibleMonth.plusMonths(1).toString()
                }
            },
            onCurrentPeriod = {
                weekAnchorKey = today.toString()
                monthKey = YearMonth.from(today).toString()
            },
            onAssignDate = { date ->
                viewModel.setPostsSchedule(selectedPostIds.value, date)
                selectedPostIds.value = emptyList()
            },
            onOpenDate = { date -> openDateKey = date.toString() },
            periodTitle = if (calendarMode == "week") {
                "${weekStart.format(dayFormatter)} - ${weekStart.plusDays(6).format(dayFormatter)}"
                    .replaceFirstChar { it.uppercase() }
            } else {
                visibleMonth.format(monthFormatter).replaceFirstChar { it.uppercase() }
            },
        )
    }

    val openDate = openDateKey?.let { key ->
        runCatching { LocalDate.parse(key) }.getOrNull()
    }
    openDate?.let { date ->
        val postsForDate = state.posts.filter { it.scheduledDate == date.toString() }
        val plan = state.calendarPlanFor(date.toString())
        CalendarDayDetailsDialog(
            date = date,
            locale = calendarLocale,
            posts = postsForDate,
            note = plan?.note.orEmpty(),
            recommendedTime = plan?.recommendedTime.orEmpty().ifBlank { suggestedTimeForDate(date) },
            onDismiss = { openDateKey = null },
            onOpenPost = { post -> previewUris = post.allMediaUris },
            onSavePlan = { note, time -> viewModel.setCalendarDayPlan(date, note, time) },
            onClearDayPosts = {
                val idsToClear = postsForDate.map { it.id }.toSet()
                selectedPostIds.value = selectedPostIds.value.filterNot(idsToClear::contains)
                viewModel.setPostsSchedule(idsToClear, null)
                openDateKey = null
            },
            onExportZip = {
                exportPosts = postsForDate
                exportDateLabel = date.toString()
                zipExporter.launch(ProjectExporter.defaultZipName("PhotoGridPlanner_${date}"))
            },
            onExportFolder = {
                exportPosts = postsForDate
                exportDateLabel = date.toString()
                folderExporter.launch(null)
            },
        )
    }

    if (previewUris.isNotEmpty()) {
        FullScreenPreview(
            uris = previewUris,
            onDismiss = { previewUris = emptyList() },
        )
    }
}

@Composable
private fun CalendarHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LocalizedText(
            text = "Agenda feed",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LocalizedText(
            text = "Organizza i post e apri ogni giorno per anteprima ed export.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarOverviewCard(
    scheduledCount: Int,
    selectedCount: Int,
    totalCount: Int,
    canClearSchedule: Boolean,
    onClearSchedule: () -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CalendarStatChip(
                    icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = "Post",
                    value = totalCount.toString(),
                )
                CalendarStatChip(
                    icon = { Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = "Pianificati",
                    value = scheduledCount.toString(),
                )
                CalendarStatChip(
                    icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    label = "Selezionati",
                    value = selectedCount.toString(),
                    emphasized = selectedCount > 0,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = canClearSchedule,
                    onClick = onClearSchedule,
                ) {
                    Icon(Icons.Rounded.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    LocalizedText("Svuota")
                }
            }
        }
    }
}

@Composable
private fun CalendarStatChip(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    emphasized: Boolean = false,
) {
    Surface(
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)
        },
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(
            1.dp,
            if (emphasized) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            LocalizedText(
                text = "$label $value",
                style = MaterialTheme.typography.labelMedium,
                color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PostSelectionStrip(
    posts: List<GridPost>,
    selectedIds: Set<String>,
    showOnlyUnscheduled: Boolean,
    onToggleUnscheduledFilter: () -> Unit,
    onTogglePost: (GridPost) -> Unit,
    onClearPostDate: (GridPost) -> Unit,
) {
    Surface(
        modifier = Modifier.animateContentSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LocalizedText("Post da inserire", style = MaterialTheme.typography.titleMedium)
                LocalizedText(
                    text = "tap per selezionare",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            FilterChip(
                selected = showOnlyUnscheduled,
                onClick = onToggleUnscheduledFilter,
                label = { LocalizedText("Solo non pianificati") },
            )
            if (posts.isEmpty()) {
                LocalizedText(
                    text = if (showOnlyUnscheduled) {
                        "Non ci sono post senza data."
                    } else {
                        "Importa immagini nella griglia prima di pianificare il feed."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = posts,
                        key = { _, post -> post.id },
                    ) { index, post ->
                        CalendarPostThumb(
                            post = post,
                            index = index + 1,
                            selected = post.id in selectedIds,
                            onClick = { onTogglePost(post) },
                            onClearSchedule = post.scheduledDate?.let { { onClearPostDate(post) } },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    dates: List<LocalDate?>,
    today: LocalDate,
    posts: List<GridPost>,
    plans: Map<String, CalendarDayPlan>,
    mode: String,
    selectedCount: Int,
    onModeChange: (String) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCurrentPeriod: () -> Unit,
    onAssignDate: (LocalDate) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    periodTitle: String,
) {
    val strings = LocalAppStrings.current
    val weekLabels = listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom")

    Surface(
        modifier = Modifier.animateContentSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = strings.t("Periodo precedente"),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LocalizedText(
                        text = periodTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = onCurrentPeriod) {
                        LocalizedText("Oggi")
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = strings.t("Periodo successivo"),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == "month",
                    onClick = { onModeChange("month") },
                    label = { LocalizedText("Mese") },
                )
                FilterChip(
                    selected = mode == "week",
                    onClick = { onModeChange("week") },
                    label = { LocalizedText("Settimana") },
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                weekLabels.forEach { label ->
                    LocalizedText(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            dates.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { date ->
                        val postsForDate = date?.let { target ->
                            posts.filter { it.scheduledDate == target.toString() }
                        }.orEmpty()
                        CalendarDayCell(
                            date = date,
                            posts = postsForDate,
                            plan = date?.let { plans[it.toString()] },
                            isToday = date == today,
                            selectedCount = selectedCount,
                            onAssignDate = onAssignDate,
                            onOpenDate = onOpenDate,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            LocalizedText(
                text = if (selectedCount > 0) {
                    "Tocca un giorno per assegnare i post selezionati."
                } else {
                    "Tocca un giorno per aprirlo. Seleziona post sopra per pianificarli."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    posts: List<GridPost>,
    plan: CalendarDayPlan?,
    isToday: Boolean,
    selectedCount: Int,
    onAssignDate: (LocalDate) -> Unit,
    onOpenDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (date == null) {
        Box(
            modifier = modifier
                .height(84.dp)
                .background(Color.Transparent),
        )
        return
    }

    val hasPosts = posts.isNotEmpty()
    val canAssign = selectedCount > 0
    val borderColor = when {
        canAssign -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }
    val background = when {
        hasPosts -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.background.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable {
                if (canAssign) {
                    onAssignDate(date)
                } else {
                    onOpenDate(date)
                }
            },
        color = background,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(if (isToday || canAssign) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        hasPosts -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        else -> Color.Transparent
                    },
                    shape = CircleShape,
                ) {
                    LocalizedText(
                        text = date.dayOfMonth.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isToday || hasPosts) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (posts.size > 3) {
                    LocalizedText(
                        text = "+${posts.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            posts.take(2).forEach { post ->
                CalendarMiniThumb(post = post)
            }
            if (!plan?.recommendedTime.isNullOrBlank()) {
                LocalizedText(
                    text = plan?.recommendedTime.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            } else if (!plan?.note.isNullOrBlank()) {
                CalendarPostDotRow(count = 1)
            }
            if (posts.size == 3) {
                CalendarPostDotRow(count = 1)
            } else if (posts.size > 3) {
                CalendarPostDotRow(count = 3)
            }
        }
    }
}

@Composable
private fun CalendarPostDotRow(count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count.coerceIn(1, 3)) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), CircleShape),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDayDetailsDialog(
    date: LocalDate,
    locale: Locale,
    posts: List<GridPost>,
    note: String,
    recommendedTime: String,
    onDismiss: () -> Unit,
    onOpenPost: (GridPost) -> Unit,
    onSavePlan: (String, String) -> Unit,
    onClearDayPosts: () -> Unit,
    onExportZip: () -> Unit,
    onExportFolder: () -> Unit,
) {
    val hasMedia = posts.any { it.allMediaUris.isNotEmpty() }
    var editableNote by rememberSaveable(date.toString()) { mutableStateOf(note) }
    var editableTime by rememberSaveable(date.toString()) { mutableStateOf(recommendedTime) }
    var showTimePicker by rememberSaveable(date.toString()) { mutableStateOf(false) }
    var showNoteEditor by rememberSaveable(date.toString()) { mutableStateOf(note.isNotBlank()) }
    var hasUnsavedPlanChanges by rememberSaveable(date.toString()) { mutableStateOf(false) }

    fun savePendingPlanChanges() {
        if (hasUnsavedPlanChanges) {
            onSavePlan(editableNote, editableTime)
            hasUnsavedPlanChanges = false
        }
    }

    fun dismissAndSavePlan() {
        savePendingPlanChanges()
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::dismissAndSavePlan,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LocalizedText("Giornata")
                LocalizedText(
                    text = formatFullDate(date, locale),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LocalizedText(
                    text = when (posts.size) {
                        0 -> "Nessun post pianificato per questo giorno."
                        1 -> "1 post pianificato."
                        else -> "${posts.size} post pianificati."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (posts.isNotEmpty()) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        items(
                            items = posts,
                            key = { post -> post.id },
                        ) { post ->
                            CalendarDayGridTile(
                                post = post,
                                onClick = { onOpenPost(post) },
                            )
                        }
                    }
                }

                if (posts.isNotEmpty()) {
                    LocalizedText(
                        text = "L'anteprima usa lo stesso ordine della griglia, cosi i mosaici restano leggibili come nel profilo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LocalizedText("Piano giornata", style = MaterialTheme.typography.titleSmall)
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.50f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    LocalizedText(
                                        "Orario di pubblicazione",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    LocalizedText(
                                        editableTime.ifBlank { suggestedTimeForDate(date) },
                                        style = MaterialTheme.typography.headlineSmall,
                                    )
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        if (showNoteEditor) {
                            OutlinedTextField(
                                value = editableNote,
                                onValueChange = {
                                    editableNote = it.take(240)
                                    hasUnsavedPlanChanges = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                label = { LocalizedText("Note") },
                            )
                        } else {
                            TextButton(onClick = { showNoteEditor = true }) {
                                LocalizedText("Aggiungi nota")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    enabled = hasMedia,
                    onClick = onExportZip,
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    LocalizedText("ZIP")
                }
                OutlinedButton(
                    enabled = hasMedia,
                    onClick = onExportFolder,
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    LocalizedText("Cartella")
                }
                if (posts.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            hasUnsavedPlanChanges = false
                            onClearDayPosts()
                        },
                    ) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(6.dp))
                        LocalizedText(
                            "Svuota giornata",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = ::dismissAndSavePlan) {
                    LocalizedText("Chiudi")
                }
            }
        },
    )

    if (showTimePicker) {
        ClockTimePickerDialog(
            initialTime = editableTime.ifBlank { suggestedTimeForDate(date) },
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                if (editableTime != time) {
                    editableTime = time
                    hasUnsavedPlanChanges = true
                }
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val parsed = parseTime(initialTime)
    val timePickerState = rememberTimePickerState(
        initialHour = parsed.first,
        initialMinute = parsed.second,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { LocalizedText("Scegli orario") },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(formatTime(timePickerState.hour, timePickerState.minute))
                },
            ) {
                LocalizedText("Conferma")
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
private fun CalendarDayGridTile(
    post: GridPost,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .background(Color.Black)
            .clickable(enabled = post.allMediaUris.isNotEmpty(), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PostThumbContent(post = post, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun CalendarPostThumb(
    post: GridPost,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onClearSchedule: (() -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    Surface(
        modifier = Modifier
            .width(70.dp)
            .height(96.dp)
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            PostThumbContent(post = post, modifier = Modifier.fillMaxSize())
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                color = Color.Black.copy(alpha = 0.56f),
                shape = CircleShape,
            ) {
                LocalizedText(
                    text = index.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            if (!post.scheduledDate.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    color = Color.Black.copy(alpha = 0.62f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    LocalizedText(
                        text = formatShortDate(post.scheduledDate),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
            if (onClearSchedule != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clickable(onClick = onClearSchedule),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.88f),
                    shape = CircleShape,
                ) {
                    Icon(
                        Icons.Rounded.Clear,
                        contentDescription = strings.t("Rimuovi data"),
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .padding(3.dp)
                            .size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMiniThumb(post: GridPost) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(17.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PostThumbContent(
                post = post,
                modifier = Modifier
                    .width(18.dp)
                    .height(17.dp),
            )
            Spacer(Modifier.width(3.dp))
            LocalizedText(
                text = if (post.isCarousel) "${post.allMediaUris.size}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PostThumbContent(
    post: GridPost,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        when {
            post.kind == PostKind.Placeholder -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(post.placeholderColor)),
                contentAlignment = Alignment.Center,
            ) {
                LocalizedText(
                    text = post.placeholderDisplayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            post.coverUri != null -> AsyncUriImage(
                uri = post.coverUri.orEmpty(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (post.isCarousel) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Rounded.ViewCarousel,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(3.dp)
                        .size(11.dp),
                )
            }
        }
        if (post.hidden) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.48f)),
            )
        }
    }
}

private fun monthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingBlanks = firstDay.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val cellCount = (((leadingBlanks + daysInMonth + 6) / 7) * 7).coerceAtLeast(35)

    return List(cellCount) { index ->
        val day = index - leadingBlanks + 1
        if (day in 1..daysInMonth) month.atDay(day) else null
    }
}

private fun formatShortDate(raw: String): String {
    return runCatching {
        val date = LocalDate.parse(raw)
        "${date.dayOfMonth}/${date.monthValue}"
    }.getOrDefault(raw)
}

private fun formatFullDate(date: LocalDate, locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale)
    return date.format(formatter).replaceFirstChar { it.uppercase() }
}

private fun suggestedTimeForDate(date: LocalDate): String {
    return when (date.dayOfWeek.value) {
        1 -> "18:30"
        2 -> "19:00"
        3 -> "12:30"
        4 -> "19:00"
        5 -> "18:00"
        6 -> "11:00"
        else -> "10:30"
    }
}

private fun parseTime(value: String): Pair<Int, Int> {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 18
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return hour to minute
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}
