package com.niwlayr.app.ui.cutter

import com.niwlayr.app.ui.i18n.LocalAppStrings
import com.niwlayr.app.ui.i18n.LocalizedText

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.niwlayr.app.ui.components.SpectrumRule
import com.niwlayr.app.ui.theme.MonoFamily
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.niwlayr.app.cutter.CutExportOrder
import com.niwlayr.app.cutter.CutTileResult
import com.niwlayr.app.cutter.CutterFrame
import com.niwlayr.app.cutter.CutterTemplate
import com.niwlayr.app.cutter.MosaicCutter
import com.niwlayr.app.cutter.MosaicSpec
import com.niwlayr.app.cutter.MosaicTransform
import com.niwlayr.app.cutter.NormalizedRect
import com.niwlayr.app.cutter.SaveDestination
import com.niwlayr.app.cutter.TemplateCutter
import com.niwlayr.app.cutter.TemplateSlot
import com.niwlayr.app.cutter.TemplateSlotInput
import com.niwlayr.app.cutter.TileFormat
import com.niwlayr.app.image.ImageLoader
import com.niwlayr.app.ui.media.GeneratedMediaRegistry
import com.niwlayr.app.ui.media.PhotoLibraryPicker
import com.niwlayr.app.ui.media.PhotoSelectionMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val MaxMosaicColumns = 3
private const val MaxMosaicRows = 5
private const val MaxMosaicTiles = MaxMosaicColumns * MaxMosaicRows
private const val MaxCarouselSlides = 5
private const val MaxTransformScale = 5f
private const val ZoomSensitivity = 2.4
private const val PanSensitivity = 3.2f
private const val TemplatePanFollowRatio = 0.92f

private enum class CutterMode(val label: String) {
    Mosaic("Mosaico"),
    SinglePost("Post singolo"),
    Carousel("Carosello"),
}

private enum class PostSourceMode(val label: String) {
    Photo("Foto"),
    Template("Template"),
}

private const val DefaultTemplateBackground = -1

private val SinglePostTemplates = listOf(
    CutterTemplate(
        id = "single_horizontal_2",
        title = "2 orizzontali",
        slots = listOf(
            TemplateSlot("a", "Foto 1", NormalizedRect(0.08f, 0.12f, 0.84f, 0.32f)),
            TemplateSlot("b", "Foto 2", NormalizedRect(0.08f, 0.56f, 0.84f, 0.32f)),
        ),
    ),
    CutterTemplate(
        id = "single_horizontal_3",
        title = "3 orizzontali",
        slots = listOf(
            TemplateSlot("a", "Foto 1", NormalizedRect(0.08f, 0.09f, 0.84f, 0.23f)),
            TemplateSlot("b", "Foto 2", NormalizedRect(0.08f, 0.385f, 0.84f, 0.23f)),
            TemplateSlot("c", "Foto 3", NormalizedRect(0.08f, 0.68f, 0.84f, 0.23f)),
        ),
    ),
    CutterTemplate(
        id = "single_vertical_2",
        title = "2 verticali",
        slots = listOf(
            TemplateSlot("a", "Foto 1", NormalizedRect(0.08f, 0.14f, 0.39f, 0.72f)),
            TemplateSlot("b", "Foto 2", NormalizedRect(0.53f, 0.14f, 0.39f, 0.72f)),
        ),
    ),
    CutterTemplate(
        id = "single_vertical_3",
        title = "3 verticali",
        slots = listOf(
            TemplateSlot("a", "Foto 1", NormalizedRect(0.06f, 0.14f, 0.27f, 0.72f)),
            TemplateSlot("b", "Foto 2", NormalizedRect(0.365f, 0.14f, 0.27f, 0.72f)),
            TemplateSlot("c", "Foto 3", NormalizedRect(0.67f, 0.14f, 0.27f, 0.72f)),
        ),
    ),
    CutterTemplate(
        id = "single_grid_4",
        title = "4 quadrati",
        slots = listOf(
            TemplateSlot("a", "Foto 1", NormalizedRect(0.12f, 0.18f, 0.34f, 0.272f)),
            TemplateSlot("b", "Foto 2", NormalizedRect(0.54f, 0.18f, 0.34f, 0.272f)),
            TemplateSlot("c", "Foto 3", NormalizedRect(0.12f, 0.548f, 0.34f, 0.272f)),
            TemplateSlot("d", "Foto 4", NormalizedRect(0.54f, 0.548f, 0.34f, 0.272f)),
        ),
    ),
    CutterTemplate(
        id = "single_hero_stack",
        title = "Hero + 3",
        slots = listOf(
            TemplateSlot("a", "Foto 1", NormalizedRect(0.07f, 0.12f, 0.52f, 0.76f)),
            TemplateSlot("b", "Foto 2", NormalizedRect(0.65f, 0.18f, 0.28f, 0.20f)),
            TemplateSlot("c", "Foto 3", NormalizedRect(0.65f, 0.40f, 0.28f, 0.20f)),
            TemplateSlot("d", "Foto 4", NormalizedRect(0.65f, 0.62f, 0.28f, 0.20f)),
        ),
    ),
)

private fun carouselTemplates(slides: Int): List<CutterTemplate> {
    val total = slides.toFloat().coerceAtLeast(2f)

    fun rect(x: Float, y: Float, w: Float, h: Float): NormalizedRect {
        return NormalizedRect(x / total, y, w / total, h)
    }

    val rightStart = (total - 0.96f).coerceAtLeast(1.05f)
    val panoramaWidth = (total - 0.88f).coerceAtLeast(1.0f)
    return listOf(
        CutterTemplate(
            id = "carousel_stack_panorama",
            title = "Stack + panorama",
            slots = listOf(
                TemplateSlot("a", "Foto 1", rect(0.10f, 0.12f, 0.58f, 0.21f)),
                TemplateSlot("b", "Foto 2", rect(0.10f, 0.395f, 0.58f, 0.21f)),
                TemplateSlot("c", "Foto 3", rect(0.10f, 0.67f, 0.58f, 0.21f)),
                TemplateSlot("d", "Foto 4", rect(0.82f, 0.12f, panoramaWidth, 0.76f)),
            ),
        ),
        CutterTemplate(
            id = "carousel_two_rows",
            title = "Doppia riga",
            slots = listOf(
                TemplateSlot("a", "Foto 1", rect(0.08f, 0.12f, total * 0.46f, 0.32f)),
                TemplateSlot("b", "Foto 2", rect(total * 0.50f, 0.12f, total * 0.42f, 0.32f)),
                TemplateSlot("c", "Foto 3", rect(0.08f, 0.56f, total * 0.40f, 0.32f)),
                TemplateSlot("d", "Foto 4", rect(total * 0.52f, 0.56f, total * 0.40f, 0.32f)),
            ),
        ),
        CutterTemplate(
            id = "carousel_hero_grid",
            title = "Hero + griglia",
            slots = listOf(
                TemplateSlot("a", "Foto 1", rect(0.08f, 0.12f, (total - 1.20f).coerceAtLeast(0.9f), 0.76f)),
                TemplateSlot("b", "Foto 2", rect(rightStart, 0.14f, 0.38f, 0.28f)),
                TemplateSlot("c", "Foto 3", rect(rightStart + 0.44f, 0.14f, 0.38f, 0.28f)),
                TemplateSlot("d", "Foto 4", rect(rightStart, 0.58f, 0.38f, 0.28f)),
                TemplateSlot("e", "Foto 5", rect(rightStart + 0.44f, 0.58f, 0.38f, 0.28f)),
            ),
        ),
    )
}

@Composable
fun CutterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val format = TileFormat.Vertical

    var modeName by rememberSaveable { mutableStateOf(CutterMode.Mosaic.name) }
    val mode = runCatching { CutterMode.valueOf(modeName) }.getOrDefault(CutterMode.Mosaic)

    var mosaicUri by rememberSaveable { mutableStateOf<String?>(null) }
    var postUri by rememberSaveable { mutableStateOf<String?>(null) }
    var carouselUri by rememberSaveable { mutableStateOf<String?>(null) }
    var importTarget by remember { mutableStateOf(CutterMode.Mosaic) }
    var pendingTemplateMode by remember { mutableStateOf<CutterMode?>(null) }
    var pendingTemplateSlotId by remember { mutableStateOf<String?>(null) }
    var showPhotoLibrary by remember { mutableStateOf(false) }

    var columns by rememberSaveable { mutableStateOf(3) }
    var rows by rememberSaveable { mutableStateOf(3) }
    var carouselSlides by rememberSaveable { mutableStateOf(3) }
    var singlePostModeName by rememberSaveable { mutableStateOf(PostSourceMode.Photo.name) }
    val singlePostMode = runCatching {
        PostSourceMode.valueOf(singlePostModeName)
    }.getOrDefault(PostSourceMode.Photo)
    var carouselModeName by rememberSaveable { mutableStateOf(PostSourceMode.Photo.name) }
    val carouselMode = runCatching {
        PostSourceMode.valueOf(carouselModeName)
    }.getOrDefault(PostSourceMode.Photo)

    var mosaicScale by rememberSaveable { mutableStateOf(1f) }
    var mosaicOffsetX by rememberSaveable { mutableStateOf(0f) }
    var mosaicOffsetY by rememberSaveable { mutableStateOf(0f) }
    var postScale by rememberSaveable { mutableStateOf(1f) }
    var postOffsetX by rememberSaveable { mutableStateOf(0f) }
    var postOffsetY by rememberSaveable { mutableStateOf(0f) }
    var carouselScale by rememberSaveable { mutableStateOf(1f) }
    var carouselOffsetX by rememberSaveable { mutableStateOf(0f) }
    var carouselOffsetY by rememberSaveable { mutableStateOf(0f) }
    var mosaicFrameEnabled by rememberSaveable { mutableStateOf(false) }
    var mosaicFramePercent by rememberSaveable { mutableStateOf(5f) }
    var postFrameEnabled by rememberSaveable { mutableStateOf(false) }
    var postFramePercent by rememberSaveable { mutableStateOf(5f) }
    var singleTemplateId by rememberSaveable { mutableStateOf(SinglePostTemplates.first().id) }
    var carouselTemplateId by rememberSaveable { mutableStateOf(carouselTemplates(carouselSlides).first().id) }
    var singleTemplateBackground by rememberSaveable { mutableStateOf(DefaultTemplateBackground) }
    var carouselTemplateBackground by rememberSaveable { mutableStateOf(DefaultTemplateBackground) }
    var singleTemplateUris by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var carouselTemplateUris by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var singleTemplateTransforms by remember { mutableStateOf<Map<String, MosaicTransform>>(emptyMap()) }
    var carouselTemplateTransforms by remember { mutableStateOf<Map<String, MosaicTransform>>(emptyMap()) }
    var selectedSingleTemplateSlot by remember { mutableStateOf<String?>(null) }
    var selectedCarouselTemplateSlot by remember { mutableStateOf<String?>(null) }

    var isCutting by remember { mutableStateOf(false) }
    var isEditingPreview by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<CutTileResult>>(emptyList()) }

    fun transformFor(target: CutterMode): MosaicTransform {
        return when (target) {
            CutterMode.Mosaic -> MosaicTransform(mosaicScale, mosaicOffsetX, mosaicOffsetY)
            CutterMode.SinglePost -> MosaicTransform(postScale, postOffsetX, postOffsetY)
            CutterMode.Carousel -> MosaicTransform(carouselScale, carouselOffsetX, carouselOffsetY)
        }
    }

    fun updateTransform(target: CutterMode, transform: MosaicTransform) {
        when (target) {
            CutterMode.Mosaic -> {
                mosaicScale = transform.safeScale
                mosaicOffsetX = transform.safeOffsetX
                mosaicOffsetY = transform.safeOffsetY
            }

            CutterMode.SinglePost -> {
                postScale = transform.safeScale
                postOffsetX = transform.safeOffsetX
                postOffsetY = transform.safeOffsetY
            }

            CutterMode.Carousel -> {
                carouselScale = transform.safeScale
                carouselOffsetX = transform.safeOffsetX
                carouselOffsetY = transform.safeOffsetY
            }
        }
    }

    fun resetTransform(target: CutterMode) {
        updateTransform(target, MosaicTransform())
    }

    fun clearOutput() {
        resultMessage = null
        results = emptyList()
    }

    fun frameOptions(enabled: Boolean, percent: Float): CutterFrame {
        return CutterFrame(
            enabled = enabled,
            thicknessPercent = percent / 100f,
        )
    }

    fun templateInputs(
        template: CutterTemplate,
        uris: Map<String, String>,
        transforms: Map<String, MosaicTransform>,
    ): List<TemplateSlotInput> {
        return template.slots.mapNotNull { slot ->
            uris[slot.id]?.let { uri ->
                TemplateSlotInput(
                    slotId = slot.id,
                    uri = Uri.parse(uri),
                    transform = transforms[slot.id] ?: MosaicTransform(),
                )
            }
        }
    }

    fun handleImportedPhoto(uri: Uri) {
        val slotId = pendingTemplateSlotId
        val templateMode = pendingTemplateMode
        if (slotId != null && templateMode != null) {
            when (templateMode) {
                CutterMode.SinglePost -> {
                    singleTemplateUris = singleTemplateUris + (slotId to uri.toString())
                    singleTemplateTransforms = singleTemplateTransforms + (slotId to MosaicTransform())
                    selectedSingleTemplateSlot = slotId
                }

                CutterMode.Carousel -> {
                    carouselTemplateUris = carouselTemplateUris + (slotId to uri.toString())
                    carouselTemplateTransforms = carouselTemplateTransforms + (slotId to MosaicTransform())
                    selectedCarouselTemplateSlot = slotId
                }

                CutterMode.Mosaic -> Unit
            }
            pendingTemplateMode = null
            pendingTemplateSlotId = null
        } else {
            when (importTarget) {
                CutterMode.Mosaic -> mosaicUri = uri.toString()
                CutterMode.SinglePost -> postUri = uri.toString()
                CutterMode.Carousel -> carouselUri = uri.toString()
            }
            resetTransform(importTarget)
        }
        clearOutput()
    }

    fun launchPicker(target: CutterMode) {
        importTarget = target
        pendingTemplateMode = null
        pendingTemplateSlotId = null
        showPhotoLibrary = true
    }

    fun launchTemplatePicker(target: CutterMode, slotId: String) {
        pendingTemplateMode = target
        pendingTemplateSlotId = slotId
        showPhotoLibrary = true
    }

    fun startCut(
        uri: String,
        spec: MosaicSpec,
        transform: MosaicTransform,
        frame: CutterFrame = CutterFrame(),
        namePrefix: String,
        exportOrder: CutExportOrder = CutExportOrder.Visual,
        preserveGallerySelectionOrder: Boolean = false,
        doneMessage: (Int) -> String,
    ) {
        scope.launch {
            isCutting = true
            clearOutput()
            runCatching {
                MosaicCutter.cutAndSave(
                    context = context,
                    sourceUri = Uri.parse(uri),
                    spec = spec,
                    format = format,
                    destination = SaveDestination.Gallery,
                    transform = transform,
                    frame = frame,
                    namePrefix = namePrefix,
                    exportOrder = exportOrder,
                    preserveGallerySelectionOrder = preserveGallerySelectionOrder,
                )
            }.onSuccess { cutResults ->
                results = cutResults
                resultMessage = doneMessage(cutResults.size)
                scope.launch {
                    runCatching {
                        GeneratedMediaRegistry.remember(
                            context = context,
                            uris = cutResults.mapNotNull { it.uri },
                        )
                    }
                }
            }.onFailure { error ->
                resultMessage = error.message ?: "Esportazione non riuscita."
            }
            isCutting = false
        }
    }

    fun startTemplateExport(
        template: CutterTemplate,
        slotUris: Map<String, String>,
        slotTransforms: Map<String, MosaicTransform>,
        backgroundColorArgb: Int,
        outputWidth: Int,
        outputHeight: Int,
        slideCount: Int,
        namePrefix: String,
        doneMessage: (Int) -> String,
    ) {
        scope.launch {
            isCutting = true
            clearOutput()
            runCatching {
                TemplateCutter.renderAndSave(
                    context = context,
                    template = template,
                    slotInputs = templateInputs(template, slotUris, slotTransforms),
                    outputWidth = outputWidth,
                    outputHeight = outputHeight,
                    backgroundColorArgb = backgroundColorArgb,
                    destination = SaveDestination.Gallery,
                    namePrefix = namePrefix,
                    slideCount = slideCount,
                )
            }.onSuccess { cutResults ->
                results = cutResults
                resultMessage = doneMessage(cutResults.size)
                scope.launch {
                    runCatching {
                        GeneratedMediaRegistry.remember(
                            context = context,
                            uris = cutResults.mapNotNull { it.uri },
                        )
                    }
                }
            }.onFailure { error ->
                resultMessage = error.message ?: "Esportazione template non riuscita."
            }
            isCutting = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState(), enabled = !isEditingPreview)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LocalizedText(
                text = "Cutter",
                style = MaterialTheme.typography.headlineMedium,
            )
            SpectrumRule()
        }

        ModeTabs(
            mode = mode,
            onModeChange = {
                modeName = it.name
                clearOutput()
            },
        )

        when (mode) {
            CutterMode.Mosaic -> {
                val spec = MosaicSpec(
                    columns = columns.coerceIn(1, MaxMosaicColumns),
                    rows = rows.coerceIn(1, MaxMosaicRows),
                )
                val transform = transformFor(CutterMode.Mosaic)
                val frame = frameOptions(mosaicFrameEnabled, mosaicFramePercent)
                MosaicControls(
                    spec = spec,
                    format = format,
                    onColumnsChange = {
                        columns = it
                        clearOutput()
                    },
                    onRowsChange = {
                        rows = it
                        clearOutput()
                    },
                )
                FrameControls(
                    enabled = mosaicFrameEnabled,
                    thicknessPercent = mosaicFramePercent,
                    onEnabledChange = {
                        mosaicFrameEnabled = it
                        clearOutput()
                    },
                    onThicknessChange = {
                        mosaicFramePercent = it
                        clearOutput()
                    },
                )
                CropWorkspace(
                    uri = mosaicUri,
                    emptyLabel = "Seleziona una foto",
                    changeLabel = "Cambia foto",
                    spec = spec,
                    format = format,
                    profilePreview = true,
                    transform = transform,
                    frame = frame,
                    minPreviewHeight = 0.dp,
                    onImport = { launchPicker(CutterMode.Mosaic) },
                    onReset = {
                        resetTransform(CutterMode.Mosaic)
                        clearOutput()
                    },
                    onTransformChange = {
                        updateTransform(CutterMode.Mosaic, it)
                        clearOutput()
                    },
                    onGestureActiveChange = { isEditingPreview = it },
                )
                mosaicUri?.let { uri ->
                    ElevatedButton(
                        onClick = {
                            startCut(
                                uri = uri,
                                spec = spec,
                                transform = transform,
                                frame = frame,
                                namePrefix = "mosaic",
                                exportOrder = CutExportOrder.ProfilePublish,
                                preserveGallerySelectionOrder = true,
                                doneMessage = { "Creati $it tasselli." },
                            )
                        },
                        enabled = !isCutting && spec.tileCount <= MaxMosaicTiles,
                    ) {
                        CutButtonContent(isCutting = isCutting, idleText = "Taglia mosaico")
                    }
                }
            }

            CutterMode.SinglePost -> {
                val spec = MosaicSpec(columns = 1, rows = 1)
                val transform = transformFor(CutterMode.SinglePost)
                val frame = frameOptions(postFrameEnabled, postFramePercent)
                val template = SinglePostTemplates.firstOrNull { it.id == singleTemplateId }
                    ?: SinglePostTemplates.first()
                SourceModeTabs(
                    mode = singlePostMode,
                    onModeChange = {
                        singlePostModeName = it.name
                        clearOutput()
                    },
                )
                if (singlePostMode == PostSourceMode.Photo) {
                    FrameControls(
                        enabled = postFrameEnabled,
                        thicknessPercent = postFramePercent,
                        onEnabledChange = {
                            postFrameEnabled = it
                            clearOutput()
                        },
                        onThicknessChange = {
                            postFramePercent = it
                            clearOutput()
                        },
                    )
                    CropWorkspace(
                        uri = postUri,
                        emptyLabel = "Seleziona una foto",
                        changeLabel = "Cambia foto",
                        spec = spec,
                        format = format,
                        profilePreview = false,
                        transform = transform,
                        frame = frame,
                        minPreviewHeight = 0.dp,
                        onImport = { launchPicker(CutterMode.SinglePost) },
                        onReset = {
                            resetTransform(CutterMode.SinglePost)
                            clearOutput()
                        },
                        onTransformChange = {
                            updateTransform(CutterMode.SinglePost, it)
                            clearOutput()
                        },
                        onGestureActiveChange = { isEditingPreview = it },
                    )
                    postUri?.let { uri ->
                        ElevatedButton(
                            onClick = {
                                startCut(
                                    uri = uri,
                                    spec = spec,
                                    transform = transform,
                                    frame = frame,
                                    namePrefix = "post",
                                    doneMessage = { "Post 4:5 salvato." },
                                )
                            },
                            enabled = !isCutting,
                        ) {
                            CutButtonContent(isCutting = isCutting, idleText = "Esporta post 4:5")
                        }
                    }
                } else {
                    TemplateWorkflow(
                        templates = SinglePostTemplates,
                        selectedTemplate = template,
                        onTemplateChange = {
                            singleTemplateId = it.id
                            selectedSingleTemplateSlot = null
                            clearOutput()
                        },
                        backgroundColorArgb = singleTemplateBackground,
                        onBackgroundColorChange = {
                            singleTemplateBackground = it
                            clearOutput()
                        },
                        slotUris = singleTemplateUris,
                        slotTransforms = singleTemplateTransforms,
                        selectedSlotId = selectedSingleTemplateSlot,
                        outputWidth = format.width,
                        outputHeight = format.height,
                        minPreviewHeight = 0.dp,
                        showScrollControls = false,
                        onSlotClick = { launchTemplatePicker(CutterMode.SinglePost, it) },
                        onSlotTransformChange = { slotId, slotTransform ->
                            singleTemplateTransforms = singleTemplateTransforms + (slotId to slotTransform)
                            selectedSingleTemplateSlot = slotId
                            clearOutput()
                        },
                        onGestureActiveChange = { isEditingPreview = it },
                    )
                    val allSlotsFilled = template.slots.all { singleTemplateUris[it.id] != null }
                    ElevatedButton(
                        onClick = {
                            startTemplateExport(
                                template = template,
                                slotUris = singleTemplateUris,
                                slotTransforms = singleTemplateTransforms,
                                backgroundColorArgb = singleTemplateBackground,
                                outputWidth = format.width,
                                outputHeight = format.height,
                                slideCount = 1,
                                namePrefix = "post_template",
                                doneMessage = { "Template post salvato." },
                            )
                        },
                        enabled = !isCutting && allSlotsFilled,
                    ) {
                        CutButtonContent(isCutting = isCutting, idleText = "Esporta template")
                    }
                }
            }

            CutterMode.Carousel -> {
                val spec = MosaicSpec(columns = carouselSlides.coerceIn(2, MaxCarouselSlides), rows = 1)
                val transform = transformFor(CutterMode.Carousel)
                val carouselTemplateOptions = carouselTemplates(spec.columns)
                val template = carouselTemplateOptions.firstOrNull { it.id == carouselTemplateId }
                    ?: carouselTemplateOptions.first()
                CarouselControls(
                    spec = spec,
                    format = format,
                    onSlidesChange = {
                        carouselSlides = it
                        clearOutput()
                    },
                )
                SourceModeTabs(
                    mode = carouselMode,
                    onModeChange = {
                        carouselModeName = it.name
                        clearOutput()
                    },
                )
                if (carouselMode == PostSourceMode.Photo) {
                    CropWorkspace(
                        uri = carouselUri,
                        emptyLabel = "Seleziona una foto",
                        changeLabel = "Cambia foto",
                        spec = spec,
                        format = format,
                        profilePreview = false,
                        transform = transform,
                        minPreviewHeight = 160.dp,
                        showScrollControls = true,
                        onImport = { launchPicker(CutterMode.Carousel) },
                        onReset = {
                            resetTransform(CutterMode.Carousel)
                            clearOutput()
                        },
                        onTransformChange = {
                            updateTransform(CutterMode.Carousel, it)
                            clearOutput()
                        },
                        onGestureActiveChange = { isEditingPreview = it },
                    )
                    carouselUri?.let { uri ->
                        ElevatedButton(
                            onClick = {
                                startCut(
                                    uri = uri,
                                    spec = spec,
                                    transform = transform,
                                    namePrefix = "carousel",
                                    preserveGallerySelectionOrder = true,
                                    doneMessage = { "Create $it slide 4:5." },
                                )
                            },
                            enabled = !isCutting,
                        ) {
                            CutButtonContent(isCutting = isCutting, idleText = "Crea carosello")
                        }
                    }
                } else {
                    TemplateWorkflow(
                        templates = carouselTemplateOptions,
                        selectedTemplate = template,
                        onTemplateChange = {
                            carouselTemplateId = it.id
                            selectedCarouselTemplateSlot = null
                            clearOutput()
                        },
                        backgroundColorArgb = carouselTemplateBackground,
                        onBackgroundColorChange = {
                            carouselTemplateBackground = it
                            clearOutput()
                        },
                        slotUris = carouselTemplateUris,
                        slotTransforms = carouselTemplateTransforms,
                        selectedSlotId = selectedCarouselTemplateSlot,
                        outputWidth = spec.outputWidth(format),
                        outputHeight = spec.outputHeight(format),
                        minPreviewHeight = 520.dp,
                        showScrollControls = true,
                        onSlotClick = { launchTemplatePicker(CutterMode.Carousel, it) },
                        onSlotTransformChange = { slotId, slotTransform ->
                            carouselTemplateTransforms = carouselTemplateTransforms + (slotId to slotTransform)
                            selectedCarouselTemplateSlot = slotId
                            clearOutput()
                        },
                        onGestureActiveChange = { isEditingPreview = it },
                    )
                    val allSlotsFilled = template.slots.all { carouselTemplateUris[it.id] != null }
                    ElevatedButton(
                        onClick = {
                            startTemplateExport(
                                template = template,
                                slotUris = carouselTemplateUris,
                                slotTransforms = carouselTemplateTransforms,
                                backgroundColorArgb = carouselTemplateBackground,
                                outputWidth = spec.outputWidth(format),
                                outputHeight = spec.outputHeight(format),
                                slideCount = spec.columns,
                                namePrefix = "carousel_template",
                                doneMessage = { "Create $it slide template." },
                            )
                        },
                        enabled = !isCutting && allSlotsFilled,
                    ) {
                        CutButtonContent(isCutting = isCutting, idleText = "Esporta carosello")
                    }
                }
            }
        }

        resultMessage?.let { message ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            ) {
                LocalizedText(
                    text = message,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (results.isNotEmpty()) {
            ResultList(results = results)
        }
    }

    PhotoLibraryPicker(
        visible = showPhotoLibrary,
        mode = PhotoSelectionMode.Single,
        title = "Seleziona foto",
        onDismiss = {
            showPhotoLibrary = false
            pendingTemplateMode = null
            pendingTemplateSlotId = null
        },
        onPhotosSelected = { uris ->
            uris.firstOrNull()?.let(::handleImportedPhoto)
        },
    )
}

@Composable
private fun ModeTabs(
    mode: CutterMode,
    onModeChange: (CutterMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CutterMode.entries.forEach { item ->
            FilterChip(
                selected = mode == item,
                onClick = { onModeChange(item) },
                label = { LocalizedText(item.label) },
            )
        }
    }
}

@Composable
private fun SourceModeTabs(
    mode: PostSourceMode,
    onModeChange: (PostSourceMode) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PostSourceMode.entries.forEach { item ->
            FilterChip(
                selected = mode == item,
                onClick = { onModeChange(item) },
                label = { LocalizedText(item.label) },
            )
        }
    }
}

@Composable
private fun FrameControls(
    enabled: Boolean,
    thicknessPercent: Float,
    onEnabledChange: (Boolean) -> Unit,
    onThicknessChange: (Float) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
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
                LocalizedText(text = "Cornice", style = MaterialTheme.typography.titleMedium)
                FilterChip(
                    selected = enabled,
                    onClick = { onEnabledChange(!enabled) },
                    label = { LocalizedText("Bianca") },
                )
            }
            if (enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LocalizedText(
                        text = "Spessore",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    LocalizedText(
                        text = "${thicknessPercent.roundToInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = thicknessPercent,
                    onValueChange = { onThicknessChange(it.coerceIn(2f, 10f)) },
                    valueRange = 2f..10f,
                    steps = 7,
                )
            }
        }
    }
}

@Composable
private fun MosaicControls(
    spec: MosaicSpec,
    format: TileFormat,
    onColumnsChange: (Int) -> Unit,
    onRowsChange: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LocalizedText(text = "Mosaico", style = MaterialTheme.typography.titleMedium)
            IntSliderSetting(
                label = "Colonne",
                value = spec.columns,
                valueRange = 1..MaxMosaicColumns,
                onValueChange = onColumnsChange,
            )
            IntSliderSetting(
                label = "Righe",
                value = spec.rows,
                valueRange = 1..MaxMosaicRows,
                onValueChange = onRowsChange,
            )
            OutputSizeRow(spec = spec, format = format)
        }
    }
}

@Composable
private fun CarouselControls(
    spec: MosaicSpec,
    format: TileFormat,
    onSlidesChange: (Int) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LocalizedText(text = "Carosello", style = MaterialTheme.typography.titleMedium)
            IntSliderSetting(
                label = "Slide",
                value = spec.columns,
                valueRange = 2..MaxCarouselSlides,
                onValueChange = onSlidesChange,
            )
            OutputSizeRow(spec = spec, format = format)
        }
    }
}

@Composable
private fun TemplateWorkflow(
    templates: List<CutterTemplate>,
    selectedTemplate: CutterTemplate,
    onTemplateChange: (CutterTemplate) -> Unit,
    backgroundColorArgb: Int,
    onBackgroundColorChange: (Int) -> Unit,
    slotUris: Map<String, String>,
    slotTransforms: Map<String, MosaicTransform>,
    selectedSlotId: String?,
    outputWidth: Int,
    outputHeight: Int,
    minPreviewHeight: Dp,
    showScrollControls: Boolean,
    onSlotClick: (String) -> Unit,
    onSlotTransformChange: (String, MosaicTransform) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LocalizedText(text = "Template", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                templates.forEach { template ->
                    FilterChip(
                        selected = selectedTemplate.id == template.id,
                        onClick = { onTemplateChange(template) },
                        label = { LocalizedText(template.title) },
                    )
                }
            }
            BackgroundColorPicker(
                colorArgb = backgroundColorArgb,
                onColorChange = onBackgroundColorChange,
            )
        }
    }

    TemplateCropPreview(
        template = selectedTemplate,
        slotUris = slotUris,
        slotTransforms = slotTransforms,
        selectedSlotId = selectedSlotId,
        backgroundColorArgb = backgroundColorArgb,
        outputWidth = outputWidth,
        outputHeight = outputHeight,
        minPreviewHeight = minPreviewHeight,
        showScrollControls = showScrollControls,
        onSlotClick = onSlotClick,
        onSlotTransformChange = onSlotTransformChange,
        onGestureActiveChange = onGestureActiveChange,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun BackgroundColorPicker(
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
) {
    var hexText by remember(colorArgb) { mutableStateOf(colorArgb.toHexColor()) }
    val hsv = colorArgb.toHsv()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LocalizedText(text = "Sfondo", style = MaterialTheme.typography.bodyLarge)
        VisualColorArea(
            hue = hsv.hue,
            saturation = hsv.saturation,
            value = hsv.value,
            onColorChange = { saturation, value ->
                onColorChange(hsvColor(hsv.hue, saturation, value))
            },
        )
        HuePicker(
            hue = hsv.hue,
            onHueChange = { hue ->
                val nextSaturation = if (hsv.saturation < 0.05f) 1f else hsv.saturation
                val nextValue = if (hsv.value < 0.05f) 1f else hsv.value
                onColorChange(hsvColor(hue, nextSaturation, nextValue))
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(colorArgb))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
            OutlinedTextField(
                value = hexText,
                onValueChange = { value ->
                    val normalized = normalizeHexInput(value)
                    hexText = normalized
                    parseHexColor(normalized)?.let(onColorChange)
                },
                label = { LocalizedText("Codice colore") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VisualColorArea(
    hue: Float,
    saturation: Float,
    value: Float,
    onColorChange: (Float, Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .pointerInput(hue) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(position: Offset) {
                        val nextSaturation = (position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val nextValue = (1f - position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        onColorChange(nextSaturation, nextValue)
                    }
                    down.consume()
                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        change.consume()
                        update(change.position)
                    }
                }
            },
    ) {
        val hueColor = Color(hsvColor(hue, 1f, 1f))
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color.White, hueColor)),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
        )
        val selector = Offset(
            x = size.width * saturation,
            y = size.height * (1f - value),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.55f),
            radius = 9.dp.toPx(),
            center = selector,
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = selector,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun HuePicker(
    hue: Float,
    onHueChange: (Float) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun update(position: Offset) {
                        onHueChange((position.x / size.width.toFloat() * 360f).coerceIn(0f, 359.9f))
                    }
                    down.consume()
                    update(down.position)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.pressed } ?: break
                        change.consume()
                        update(change.position)
                    }
                }
            },
    ) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color(hsvColor(0f, 1f, 1f)),
                    Color(hsvColor(60f, 1f, 1f)),
                    Color(hsvColor(120f, 1f, 1f)),
                    Color(hsvColor(180f, 1f, 1f)),
                    Color(hsvColor(240f, 1f, 1f)),
                    Color(hsvColor(300f, 1f, 1f)),
                    Color(hsvColor(359.9f, 1f, 1f)),
                ),
            ),
            cornerRadius = CornerRadius(999.dp.toPx(), 999.dp.toPx()),
        )
        val x = size.width * (hue.coerceIn(0f, 359.9f) / 359.9f)
        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(x, size.height / 2f),
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = 10.dp.toPx(),
            center = Offset(x, size.height / 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

private fun hsvColor(hue: Float, saturation: Float, value: Float): Int {
    return AndroidColor.HSVToColor(
        floatArrayOf(
            hue.coerceIn(0f, 359.9f),
            saturation.coerceIn(0f, 1f),
            value.coerceIn(0f, 1f),
        ),
    )
}

private fun Int.toHsv(): HsvColor {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(this, hsv)
    return HsvColor(
        hue = hsv[0].coerceIn(0f, 359.9f),
        saturation = hsv[1].coerceIn(0f, 1f),
        value = hsv[2].coerceIn(0f, 1f),
    )
}

private fun Int.channel(shift: Int): Int = (this ushr shift) and 0xFF

private fun Int.toHexColor(): String {
    return "#${channel(16).toHexPair()}${channel(8).toHexPair()}${channel(0).toHexPair()}"
}

private fun Int.toHexPair(): String = toString(16).uppercase().padStart(2, '0')

private fun normalizeHexInput(value: String): String {
    val clean = value
        .trim()
        .removePrefix("#")
        .filter { it.isDigit() || it.uppercaseChar() in 'A'..'F' }
        .take(8)
        .uppercase()
    return "#$clean"
}

private fun parseHexColor(value: String): Int? {
    val clean = value.trim().removePrefix("#")
    if (clean.length != 6 && clean.length != 8) return null
    if (!clean.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) return null
    val parsed = clean.toLongOrNull(16) ?: return null
    return if (clean.length == 6) {
        ((0xFFL shl 24) or parsed).toInt()
    } else {
        parsed.toInt()
    }
}

@Composable
private fun OutputSizeRow(
    spec: MosaicSpec,
    format: TileFormat,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LocalizedText(
            text = "${spec.tileCount} file",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LocalizedText(
            text = "${spec.outputWidth(format)} x ${spec.outputHeight(format)} px",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MonoFamily),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CropWorkspace(
    uri: String?,
    emptyLabel: String,
    changeLabel: String,
    spec: MosaicSpec,
    format: TileFormat,
    profilePreview: Boolean,
    transform: MosaicTransform,
    frame: CutterFrame = CutterFrame(),
    minPreviewHeight: Dp,
    showScrollControls: Boolean = false,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onTransformChange: (MosaicTransform) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit,
) {
    uri?.let { imageUri ->
        PlacementToolbar(
            changeLabel = changeLabel,
            onImport = onImport,
            onReset = onReset,
        )
        GestureCropPreview(
            uri = imageUri,
            spec = spec,
            format = format,
            profilePreview = profilePreview,
            transform = transform,
            frame = frame,
            minPreviewHeight = minPreviewHeight,
            showScrollControls = showScrollControls,
            onTransformChange = onTransformChange,
            onGestureActiveChange = onGestureActiveChange,
            modifier = Modifier.fillMaxWidth(),
        )
    } ?: EmptyCutterPreview(
        label = emptyLabel,
        onImport = onImport,
    )
}

@Composable
private fun PlacementToolbar(
    changeLabel: String,
    onImport: () -> Unit,
    onReset: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onImport) {
            Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            LocalizedText(changeLabel)
        }
        OutlinedButton(onClick = onReset) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            LocalizedText("Centra")
        }
    }
}

@Composable
private fun IntSliderSetting(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LocalizedText(text = label, style = MaterialTheme.typography.bodyLarge)
            LocalizedText(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = {
                onValueChange(
                    it.roundToInt().coerceIn(valueRange.first, valueRange.last),
                )
            },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
        )
    }
}

@Composable
private fun GestureCropPreview(
    uri: String,
    spec: MosaicSpec,
    format: TileFormat,
    profilePreview: Boolean,
    transform: MosaicTransform,
    frame: CutterFrame,
    minPreviewHeight: Dp,
    showScrollControls: Boolean,
    onTransformChange: (MosaicTransform) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestTransform by rememberUpdatedState(transform)
    val horizontalScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val aspectRatio = spec.outputWidth(format, profileVisible = profilePreview).toFloat() /
        spec.outputHeight(format).toFloat()
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rawHeight = (maxWidth / aspectRatio).coerceAtMost(520.dp)
        val previewHeight = rawHeight.coerceAtLeast(minPreviewHeight)
        val previewWidth = previewHeight * aspectRatio
        val scrollStep = (constraints.maxWidth * 0.85f).roundToInt().coerceAtLeast(1)
        val cropBox: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .width(previewWidth)
                    .height(previewHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(spec.columns, spec.rows) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            onGestureActiveChange(true)
                            var currentTransform = latestTransform
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressed = event.changes.any { it.pressed }
                                    event.changes.forEach { it.consume() }
                                    if (!pressed) break

                                    val zoom = event.calculateZoom()
                                    val zoomFactor = zoom.toDouble().pow(ZoomSensitivity).toFloat()
                                    val pan = event.calculatePan()
                                    val panX = if (size.width == 0) {
                                        0f
                                    } else {
                                        pan.x / size.width.toFloat() * PanSensitivity
                                    }
                                    val panY = if (size.height == 0) {
                                        0f
                                    } else {
                                        pan.y / size.height.toFloat() * PanSensitivity
                                    }
                                    val nextTransform = currentTransform.copy(
                                        scale = (currentTransform.safeScale * zoomFactor)
                                            .coerceIn(1f, MaxTransformScale),
                                        offsetX = currentTransform.safeOffsetX + panX,
                                        offsetY = currentTransform.safeOffsetY + panY,
                                    )
                                    currentTransform = nextTransform
                                    onTransformChange(nextTransform)
                                }
                            } finally {
                                onGestureActiveChange(false)
                            }
                        }
                    },
            ) {
                TransformedUriImage(
                    uri = uri,
                    transform = transform,
                    frame = frame,
                    modifier = Modifier.fillMaxSize(),
                )
                CutLinesOverlay(
                    columns = spec.columns,
                    rows = spec.rows,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        if (previewWidth > maxWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState),
                ) {
                    cropBox()
                }
                if (showScrollControls) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val target = (horizontalScrollState.value - scrollStep)
                                        .coerceAtLeast(0)
                                    horizontalScrollState.animateScrollTo(target)
                                }
                            },
                            enabled = horizontalScrollState.value > 0,
                        ) {
                            LocalizedText("Indietro")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val target = (horizontalScrollState.value + scrollStep)
                                        .coerceAtMost(horizontalScrollState.maxValue)
                                    horizontalScrollState.animateScrollTo(target)
                                }
                            },
                            enabled = horizontalScrollState.value < horizontalScrollState.maxValue,
                        ) {
                            LocalizedText("Avanti")
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                cropBox()
            }
        }
    }
}

@Composable
private fun TemplateCropPreview(
    template: CutterTemplate,
    slotUris: Map<String, String>,
    slotTransforms: Map<String, MosaicTransform>,
    selectedSlotId: String?,
    backgroundColorArgb: Int,
    outputWidth: Int,
    outputHeight: Int,
    minPreviewHeight: Dp,
    showScrollControls: Boolean,
    onSlotClick: (String) -> Unit,
    onSlotTransformChange: (String, MosaicTransform) -> Unit,
    onGestureActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    var images by remember(template.id) { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    val latestSlotUris by rememberUpdatedState(slotUris)
    val latestSlotTransforms by rememberUpdatedState(slotTransforms)
    val latestImages by rememberUpdatedState(images)
    val horizontalScrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val selectedOutlineColor = MaterialTheme.colorScheme.primary
    val cutLineColor = MaterialTheme.colorScheme.primary
    val cutColumns = (outputWidth / TileFormat.Vertical.width).coerceAtLeast(1)

    LaunchedEffect(slotUris, template.id) {
        val loaded = mutableMapOf<String, ImageBitmap>()
        slotUris.forEach { (slotId, uri) ->
            runCatching {
                ImageLoader.loadBitmap(context, Uri.parse(uri), maxSize = 2600).asImageBitmap()
            }.onSuccess { bitmap ->
                loaded[slotId] = bitmap
            }
        }
        images = loaded
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val aspectRatio = outputWidth.toFloat() / outputHeight.toFloat()
        val rawHeight = (maxWidth / aspectRatio).coerceAtMost(520.dp)
        val previewHeight = rawHeight.coerceAtLeast(minPreviewHeight)
        val previewWidth = previewHeight * aspectRatio
        val scrollStep = (constraints.maxWidth * 0.85f).roundToInt().coerceAtLeast(1)
        val cropBox: @Composable () -> Unit = {
            Canvas(
                modifier = Modifier
                    .width(previewWidth)
                    .height(previewHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(template.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val slot = template.slotAt(
                                offset = down.position,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                            if (slot == null) {
                                return@awaitEachGesture
                            }
                            val hasImage = latestSlotUris[slot.id] != null
                            if (!hasImage) {
                                val touchSlop = viewConfiguration.touchSlop
                                var moved = false
                                var cancelled = false
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Final)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: event.changes.firstOrNull()
                                        ?: break
                                    val delta = change.position - down.position
                                    if (change.isConsumed) {
                                        cancelled = true
                                    }
                                    if (abs(delta.x) > touchSlop || abs(delta.y) > touchSlop) {
                                        moved = true
                                    }
                                    if (!change.pressed) break
                                }
                                if (!moved && !cancelled) {
                                    onSlotClick(slot.id)
                                }
                                return@awaitEachGesture
                            }

                            down.consume()
                            onGestureActiveChange(true)
                            var gestureChanged = false
                            var currentTransform = latestSlotTransforms[slot.id] ?: MosaicTransform()
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val pressed = event.changes.any { it.pressed }
                                    event.changes.forEach { it.consume() }
                                    if (!pressed) break

                                    if (hasImage) {
                                        val zoom = event.calculateZoom()
                                        val pan = event.calculatePan()
                                        if (
                                            abs(zoom - 1f) > 0.01f ||
                                            abs(pan.x) > 0.75f ||
                                            abs(pan.y) > 0.75f
                                        ) {
                                            gestureChanged = true
                                        }
                                        if (gestureChanged) {
                                            val slotRect = slot.rect.toCanvasRect(
                                                width = size.width.toFloat(),
                                                height = size.height.toFloat(),
                                            )
                                            val zoomFactor = zoom.toDouble().pow(ZoomSensitivity).toFloat()
                                            val slotImage = latestImages[slot.id]
                                            val panOffset = if (slotImage == null) {
                                                Offset.Zero
                                            } else {
                                                templatePanOffset(
                                                    image = slotImage,
                                                    slotRect = slotRect,
                                                    transform = currentTransform,
                                                    pan = pan,
                                                )
                                            }
                                            val nextTransform = currentTransform.copy(
                                                scale = (currentTransform.safeScale * zoomFactor)
                                                    .coerceIn(1f, MaxTransformScale),
                                                offsetX = currentTransform.safeOffsetX + panOffset.x,
                                                offsetY = currentTransform.safeOffsetY + panOffset.y,
                                            )
                                            currentTransform = nextTransform
                                            onSlotTransformChange(slot.id, nextTransform)
                                        }
                                    }
                                }
                            } finally {
                                onGestureActiveChange(false)
                            }
                            if (!gestureChanged) {
                                onSlotClick(slot.id)
                            }
                        }
                    },
            ) {
                drawRect(Color(backgroundColorArgb))
                template.slots.forEach { slot ->
                    val slotRect = slot.rect.toCanvasRect(size.width, size.height)
                    drawRect(
                        color = Color(0xFF7B7B78).copy(alpha = if (images[slot.id] == null) 1f else 0.08f),
                        topLeft = Offset(slotRect.left, slotRect.top),
                        size = Size(slotRect.width, slotRect.height),
                    )
                    val slotImage = images[slot.id]
                    if (slotImage == null) {
                        drawTemplateSlotHint(
                            slotRect = slotRect,
                            label = strings.t("Tocca"),
                            detail = strings.t("per inserire"),
                        )
                    } else {
                        drawTemplateSlotImage(
                            image = slotImage,
                            transform = slotTransforms[slot.id] ?: MosaicTransform(),
                            slotRect = slotRect,
                        )
                    }
                    drawRect(
                        color = if (slot.id == selectedSlotId) {
                            selectedOutlineColor
                        } else {
                            Color.White.copy(alpha = 0.88f)
                        },
                        topLeft = Offset(slotRect.left, slotRect.top),
                        size = Size(slotRect.width, slotRect.height),
                        style = Stroke(width = if (slot.id == selectedSlotId) 3.dp.toPx() else 2.dp.toPx()),
                    )
                }
                if (cutColumns > 1) {
                    drawTemplateCutLines(columns = cutColumns, color = cutLineColor)
                }
            }
        }

        if (previewWidth > maxWidth) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScrollState),
                ) {
                    cropBox()
                }
                if (showScrollControls) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val target = (horizontalScrollState.value - scrollStep)
                                        .coerceAtLeast(0)
                                    horizontalScrollState.animateScrollTo(target)
                                }
                            },
                            enabled = horizontalScrollState.value > 0,
                        ) {
                            LocalizedText("Indietro")
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val target = (horizontalScrollState.value + scrollStep)
                                        .coerceAtMost(horizontalScrollState.maxValue)
                                    horizontalScrollState.animateScrollTo(target)
                                }
                            },
                            enabled = horizontalScrollState.value < horizontalScrollState.maxValue,
                        ) {
                            LocalizedText("Avanti")
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                cropBox()
            }
        }
    }
}

private fun CutterTemplate.slotAt(
    offset: Offset,
    width: Float,
    height: Float,
): TemplateSlot? {
    return slots.asReversed().firstOrNull { slot ->
        val rect = slot.rect.toCanvasRect(width, height)
        offset.x in rect.left..rect.right && offset.y in rect.top..rect.bottom
    }
}

private fun NormalizedRect.toCanvasRect(width: Float, height: Float): Rect {
    val maxWidth = width.roundToInt().coerceAtLeast(1)
    val maxHeight = height.roundToInt().coerceAtLeast(1)
    val leftPx = floor(left * width).toInt().coerceIn(0, maxWidth - 1)
    val topPx = floor(top * height).toInt().coerceIn(0, maxHeight - 1)
    val rightPx = ceil(right * width).toInt().coerceIn(leftPx + 1, maxWidth)
    val bottomPx = ceil(bottom * height).toInt().coerceIn(topPx + 1, maxHeight)
    return Rect(
        left = leftPx.toFloat(),
        top = topPx.toFloat(),
        right = rightPx.toFloat(),
        bottom = bottomPx.toFloat(),
    )
}

private fun DrawScope.drawTemplateCutLines(
    columns: Int,
    color: Color,
) {
    for (column in 1 until columns) {
        val x = (size.width * column / columns).roundToInt().toFloat()
        drawLine(
            color = Color.White.copy(alpha = 0.95f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Square,
        )
        drawLine(
            color = color.copy(alpha = 0.9f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Square,
        )
    }
}

private fun DrawScope.drawTemplateSlotHint(
    slotRect: Rect,
    label: String,
    detail: String,
) {
    val textSize = min(slotRect.width * 0.12f, slotRect.height * 0.18f)
        .coerceIn(9.dp.toPx(), 16.dp.toPx())
    val centerX = slotRect.left + slotRect.width / 2f
    val centerY = slotRect.top + slotRect.height / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(230, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        this.textSize = textSize
    }
    val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(185, 255, 255, 255)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        this.textSize = textSize * 0.82f
    }
    val labelMetrics = paint.fontMetrics
    val detailMetrics = detailPaint.fontMetrics
    val gap = 2.dp.toPx()
    val labelHeight = labelMetrics.descent - labelMetrics.ascent
    val detailHeight = detailMetrics.descent - detailMetrics.ascent
    val blockTop = centerY - (labelHeight + gap + detailHeight) / 2f

    drawContext.canvas.nativeCanvas.apply {
        drawText(label, centerX, blockTop - labelMetrics.ascent, paint)
        drawText(detail, centerX, blockTop + labelHeight + gap - detailMetrics.ascent, detailPaint)
    }
}

private fun templatePanOffset(
    image: ImageBitmap,
    slotRect: Rect,
    transform: MosaicTransform,
    pan: Offset,
): Offset {
    val sourceWidth = image.width.toFloat().coerceAtLeast(1f)
    val sourceHeight = image.height.toFloat().coerceAtLeast(1f)
    val slotWidth = slotRect.width.coerceAtLeast(1f)
    val slotHeight = slotRect.height.coerceAtLeast(1f)
    val baseScale = max(slotWidth / sourceWidth, slotHeight / sourceHeight)
    val drawScale = baseScale * transform.safeScale
    val drawWidth = sourceWidth * drawScale + 2f
    val drawHeight = sourceHeight * drawScale + 2f
    val extraX = max(drawWidth - slotWidth, 0f)
    val extraY = max(drawHeight - slotHeight, 0f)

    fun axisDelta(panPx: Float, extraPx: Float, slotPx: Float): Float {
        if (extraPx <= 1f) return 0f
        val denominator = max(extraPx, slotPx * 0.45f)
        return panPx * 2f * TemplatePanFollowRatio / denominator
    }

    return Offset(
        x = axisDelta(pan.x, extraX, slotWidth),
        y = axisDelta(pan.y, extraY, slotHeight),
    )
}

private fun DrawScope.drawTemplateSlotImage(
    image: ImageBitmap,
    transform: MosaicTransform,
    slotRect: Rect,
) {
    val sourceWidth = image.width.toFloat()
    val sourceHeight = image.height.toFloat()
    val baseScale = max(slotRect.width / sourceWidth, slotRect.height / sourceHeight)
    val drawScale = baseScale * transform.safeScale
    val drawWidth = sourceWidth * drawScale + 2f
    val drawHeight = sourceHeight * drawScale + 2f
    val extraX = max(drawWidth - slotRect.width, 0f)
    val extraY = max(drawHeight - slotRect.height, 0f)
    val left = slotRect.left + (slotRect.width - drawWidth) / 2f + transform.safeOffsetX * extraX / 2f
    val top = slotRect.top + (slotRect.height - drawHeight) / 2f + transform.safeOffsetY * extraY / 2f

    clipRect(slotRect.left, slotRect.top, slotRect.right, slotRect.bottom) {
        drawImage(
            image = image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(
                drawWidth.roundToInt().coerceAtLeast(1),
                drawHeight.roundToInt().coerceAtLeast(1),
            ),
            filterQuality = FilterQuality.High,
        )
    }
}

@Composable
private fun TransformedUriImage(
    uri: String,
    transform: MosaicTransform,
    frame: CutterFrame,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var image by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        failed = false
        image = null
        runCatching {
            ImageLoader.loadBitmap(context, Uri.parse(uri), maxSize = 2600).asImageBitmap()
        }.onSuccess { loaded ->
            image = loaded
        }.onFailure {
            failed = true
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        when {
            image != null -> Canvas(modifier = Modifier.fillMaxSize()) {
                drawTransformedImage(image = image!!, transform = transform, frame = frame)
            }

            failed -> Icon(
                imageVector = Icons.Rounded.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )

            else -> CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

private fun DrawScope.drawTransformedImage(
    image: ImageBitmap,
    transform: MosaicTransform,
    frame: CutterFrame,
) {
    val imageRect = if (frame.enabled) {
        drawRect(Color(frame.colorArgb))
        val inset = (minOf(size.width, size.height) * frame.safeThicknessPercent)
            .roundToInt()
            .toFloat()
        Rect(
            left = inset,
            top = inset,
            right = size.width - inset,
            bottom = size.height - inset,
        )
    } else {
        Rect(0f, 0f, size.width, size.height)
    }
    val sourceWidth = image.width.toFloat()
    val sourceHeight = image.height.toFloat()
    val baseScale = max(imageRect.width / sourceWidth, imageRect.height / sourceHeight)
    val drawScale = baseScale * transform.safeScale
    val drawWidth = sourceWidth * drawScale
    val drawHeight = sourceHeight * drawScale
    val extraX = max(drawWidth - imageRect.width, 0f)
    val extraY = max(drawHeight - imageRect.height, 0f)
    val left = imageRect.left + (imageRect.width - drawWidth) / 2f + transform.safeOffsetX * extraX / 2f
    val top = imageRect.top + (imageRect.height - drawHeight) / 2f + transform.safeOffsetY * extraY / 2f

    clipRect(imageRect.left, imageRect.top, imageRect.right, imageRect.bottom) {
        drawImage(
            image = image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(
                drawWidth.roundToInt().coerceAtLeast(1),
                drawHeight.roundToInt().coerceAtLeast(1),
            ),
            filterQuality = FilterQuality.High,
        )
    }
}

@Composable
private fun CutLinesOverlay(
    columns: Int,
    rows: Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val lineColor = Color.White.copy(alpha = 0.82f)
        for (column in 1 until columns) {
            val x = size.width * column / columns
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square,
            )
        }
        for (row in 1 until rows) {
            val y = size.height * row / rows
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Square,
            )
        }
    }
}

@Composable
private fun EmptyCutterPreview(
    label: String,
    onImport: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        OutlinedButton(onClick = onImport) {
            Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            LocalizedText(label)
        }
    }
}

@Composable
private fun CutButtonContent(
    isCutting: Boolean,
    idleText: String,
) {
    if (isCutting) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        Icon(Icons.Rounded.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    Spacer(Modifier.size(8.dp))
    LocalizedText(if (isCutting) "Esporto..." else idleText)
}

@Composable
private fun ResultList(results: List<CutTileResult>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LocalizedText(text = "File creati", style = MaterialTheme.typography.titleMedium)
        results.forEach { result ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        LocalizedText(
                            text = result.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        LocalizedText(
                            text = result.layoutLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.Save,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
