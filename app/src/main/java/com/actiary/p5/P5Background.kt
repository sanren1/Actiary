package com.actiary.p5

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.actiary.data.appearance.P5Palette
import com.actiary.theme.asColor

@Composable
fun P5Background(
    tokens: P5Tokens,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val isCallingCard = tokens.palette == P5Palette.CALLING_CARD
                val canvasColor = tokens.canvas.asColor()
                val brandColor = tokens.brandFill.asColor()
                val primaryRibbonColor = brandColor.copy(alpha = if (isCallingCard) 0.14f else 0.16f)
                val secondaryRibbonColor = brandColor.copy(alpha = if (isCallingCard) 0.09f else 0.10f)
                val dotColor = if (isCallingCard) {
                    Color(0x14000000)
                } else {
                    tokens.strokeLight.asColor().copy(alpha = 0.05f)
                }

                val w = size.width
                val h = size.height

                val upperRibbon = Path().apply {
                    if (w > 0f && h > 0f) {
                        moveTo(-w * 0.08f, h * 0.18f)
                        lineTo(w * 1.08f, h * 0.04f)
                        lineTo(w * 1.08f, h * 0.14f)
                        lineTo(-w * 0.08f, h * 0.30f)
                        close()
                    }
                }

                val lowerRibbon = Path().apply {
                    if (w > 0f && h > 0f) {
                        moveTo(-w * 0.08f, h * 0.78f)
                        lineTo(w * 1.08f, h * 0.62f)
                        lineTo(w * 1.08f, h * 0.70f)
                        lineTo(-w * 0.08f, h * 0.88f)
                        close()
                    }
                }

                val halftonePath = Path().apply {
                    if (w > 0f && h > 0f) {
                        val step = 18.dp.toPx().coerceAtLeast(8f)
                        val radius = 1.6.dp.toPx().coerceAtLeast(1f)
                        var row = 0
                        var y = step * 0.5f
                        while (y < h) {
                            val xOffset = if (row % 2 == 1) step * 0.5f else 0f
                            var x = step * 0.5f + xOffset
                            while (x < w) {
                                addOval(
                                    Rect(
                                        left = x - radius,
                                        top = y - radius,
                                        right = x + radius,
                                        bottom = y + radius,
                                    ),
                                )
                                x += step
                            }
                            y += step
                            row++
                        }
                    }
                }

                onDrawBehind {
                    drawRect(color = canvasColor)
                    drawPath(path = halftonePath, color = dotColor)
                    drawPath(path = upperRibbon, color = primaryRibbonColor)
                    drawPath(path = lowerRibbon, color = secondaryRibbonColor)
                }
            },
    )
}
