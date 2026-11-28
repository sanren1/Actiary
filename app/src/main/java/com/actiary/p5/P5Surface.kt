package com.actiary.p5

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

fun Modifier.p5Surface(
    variant: P5ShapeVariant,
    fillColor: Color,
    strokeColor: Color,
    shadowColor: Color,
    strokeWidth: Dp = 2.dp,
    shadowOffset: DpOffset = DpOffset(4.dp, 4.dp),
): Modifier = this
    .drawBehind {
        if (size.width <= 0f || size.height <= 0f) return@drawBehind
        val points = computeP5PolygonPoints(size.width, size.height, variant)

        val shadowDx = shadowOffset.x.toPx()
        val shadowDy = shadowOffset.y.toPx()
        if (shadowColor.alpha > 0f && (shadowDx != 0f || shadowDy != 0f)) {
            val shadowPath = buildP5PolygonPath(points, offsetX = shadowDx, offsetY = shadowDy)
            drawPath(path = shadowPath, color = shadowColor)
        }

        val mainPath = buildP5PolygonPath(points)
        drawPath(path = mainPath, color = fillColor)

        val strokePx = strokeWidth.toPx()
        if (strokePx > 0f && strokeColor.alpha > 0f) {
            drawPath(
                path = mainPath,
                color = strokeColor,
                style = Stroke(width = strokePx, join = StrokeJoin.Miter),
            )
        }
    }
    .clip(p5Shape(variant))
