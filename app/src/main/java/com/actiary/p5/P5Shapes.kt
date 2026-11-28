package com.actiary.p5

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import kotlin.math.hypot
import kotlin.math.min

enum class P5ShapeVariant {
    PANEL,
    CARD,
    PILL,
    TAB,
    DIALOG,
    BADGE,
}

private data class P5CornerProfile(
    val tlX: Float,
    val tlY: Float,
    val trX: Float,
    val trY: Float,
    val brX: Float,
    val brY: Float,
    val blX: Float,
    val blY: Float,
)

private fun profileFor(variant: P5ShapeVariant): P5CornerProfile = when (variant) {
    P5ShapeVariant.PANEL -> P5CornerProfile(
        tlX = 14f, tlY = 0f,
        trX = 0f, trY = 10f,
        brX = 18f, brY = 0f,
        blX = 0f, blY = 8f,
    )
    P5ShapeVariant.CARD -> P5CornerProfile(
        tlX = 12f, tlY = 4f,
        trX = 0f, trY = 10f,
        brX = 14f, brY = 0f,
        blX = 4f, blY = 8f,
    )
    P5ShapeVariant.PILL -> P5CornerProfile(
        tlX = 20f, tlY = 0f,
        trX = 0f, trY = 0f,
        brX = 20f, brY = 0f,
        blX = 0f, blY = 0f,
    )
    P5ShapeVariant.TAB -> P5CornerProfile(
        tlX = 16f, tlY = 0f,
        trX = 0f, trY = 6f,
        brX = 12f, brY = 0f,
        blX = 0f, blY = 0f,
    )
    P5ShapeVariant.DIALOG -> P5CornerProfile(
        tlX = 20f, tlY = 8f,
        trX = 0f, trY = 16f,
        brX = 20f, brY = 0f,
        blX = 8f, blY = 12f,
    )
    P5ShapeVariant.BADGE -> P5CornerProfile(
        tlX = 10f, tlY = 0f,
        trX = 0f, trY = 4f,
        brX = 10f, brY = 0f,
        blX = 0f, blY = 4f,
    )
}

private fun clampCornerOffset(dx: Float, dy: Float, maxOffset: Float): Pair<Float, Float> {
    if (maxOffset <= 0f) return 0f to 0f
    val cx = dx.coerceIn(0f, maxOffset)
    val cy = dy.coerceIn(0f, maxOffset)
    val dist = hypot(cx, cy)
    return if (dist > maxOffset && dist > 0f) {
        val factor = maxOffset / dist
        (cx * factor) to (cy * factor)
    } else {
        cx to cy
    }
}

internal fun computeP5PolygonPoints(
    width: Float,
    height: Float,
    variant: P5ShapeVariant,
    densityScale: Float = 1f,
): List<Pair<Float, Float>> {
    if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) {
        return listOf(0f to 0f, 0f to 0f, 0f to 0f, 0f to 0f)
    }
    val scale = if (densityScale.isFinite() && densityScale > 0f) densityScale else 1f
    val maxOffset = min(width, height) * 0.14f
    val p = profileFor(variant)

    val tl = clampCornerOffset(p.tlX * scale, p.tlY * scale, maxOffset)
    val tr = clampCornerOffset(p.trX * scale, p.trY * scale, maxOffset)
    val br = clampCornerOffset(p.brX * scale, p.brY * scale, maxOffset)
    val bl = clampCornerOffset(p.blX * scale, p.blY * scale, maxOffset)

    return listOf(
        tl.first.coerceIn(0f, width) to tl.second.coerceIn(0f, height),
        (width - tr.first).coerceIn(0f, width) to tr.second.coerceIn(0f, height),
        (width - br.first).coerceIn(0f, width) to (height - br.second).coerceIn(0f, height),
        bl.first.coerceIn(0f, width) to (height - bl.second).coerceIn(0f, height),
    )
}

internal fun buildP5PolygonPath(
    points: List<Pair<Float, Float>>,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
): Path = Path().apply {
    if (points.isEmpty()) return@apply
    moveTo(points[0].first + offsetX, points[0].second + offsetY)
    for (i in 1 until points.size) {
        lineTo(points[i].first + offsetX, points[i].second + offsetY)
    }
    close()
}

fun p5Shape(variant: P5ShapeVariant): Shape = GenericShape { size, _ ->
    val points = computeP5PolygonPoints(size.width, size.height, variant)
    if (points.isNotEmpty()) {
        moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) {
            lineTo(points[i].first, points[i].second)
        }
        close()
    }
}
