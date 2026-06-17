package com.photogridplanner.cutter

import android.net.Uri
import java.io.File

data class MosaicSpec(
    val columns: Int,
    val rows: Int,
) {
    val label: String = "${columns}x${rows}"
    val tileCount: Int = columns * rows
}

enum class TileFormat(
    val width: Int,
    val height: Int,
    val label: String,
) {
    Vertical(1080, 1350, "Verticale 1080x1350"),
}

enum class SaveDestination(val label: String) {
    Gallery("Galleria"),
    AppFolder("Cartella app"),
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
