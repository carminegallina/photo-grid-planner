package com.niwlayr.app.cutter

import android.net.Uri
import java.io.File

data class MosaicSpec(
    val columns: Int,
    val rows: Int,
) {
    val label: String = "${columns}x${rows}"
    val tileCount: Int = columns * rows

    fun outputWidth(format: TileFormat, profileVisible: Boolean = false): Int {
        return columns * if (profileVisible) format.profileVisibleWidth else format.width
    }

    fun outputHeight(format: TileFormat): Int = rows * format.height
}

data class MosaicTransform(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    val safeScale: Float = scale.coerceIn(1f, 5f)
    val safeOffsetX: Float = offsetX.coerceIn(-1f, 1f)
    val safeOffsetY: Float = offsetY.coerceIn(-1f, 1f)
}

data class CutterFrame(
    val enabled: Boolean = false,
    val thicknessPercent: Float = DefaultThicknessPercent,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
) {
    val safeThicknessPercent: Float = thicknessPercent.coerceIn(0.02f, 0.10f)

    companion object {
        const val DefaultThicknessPercent: Float = 0.05f
    }
}

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float = left + width
    val bottom: Float = top + height
}

data class TemplateSlot(
    val id: String,
    val label: String,
    val rect: NormalizedRect,
)

data class CutterTemplate(
    val id: String,
    val title: String,
    val slots: List<TemplateSlot>,
)

data class TemplateSlotInput(
    val slotId: String,
    val uri: Uri,
    val transform: MosaicTransform = MosaicTransform(),
)

enum class TileFormat(
    val width: Int,
    val height: Int,
    val profileVisibleWidth: Int,
    val label: String,
) {
    Vertical(1080, 1350, 1012, "Verticale 1080x1350"),
    ;

    val profileSideInset: Int
        get() = ((width - profileVisibleWidth) / 2).coerceAtLeast(0)
}

enum class SaveDestination(val label: String) {
    Gallery("Galleria"),
    AppFolder("Cartella app"),
}

enum class CutExportOrder {
    Visual,
    ProfilePublish,
}

data class CutTileResult(
    val publishIndex: Int,
    val row: Int,
    val column: Int,
    val displayName: String,
    val uri: Uri?,
    val file: File?,
) {
    val layoutLabel: String = "riga ${row + 1}, colonna ${column + 1}"
}
