package com.actiary.p5

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.data.appearance.P5Palette
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.asColor
import com.actiary.ui.motion.rememberMotionReduced

internal data class RansomGlyphSpec(
    val text: String,
    val rotationDeg: Float,
    val inverted: Boolean,
    val accent: Boolean,
)

internal fun layoutRansomTitle(
    title: String,
    enabled: Boolean,
    reducedMotion: Boolean,
): List<RansomGlyphSpec> {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return emptyList()
    if (!enabled || reducedMotion) {
        return listOf(
            RansomGlyphSpec(
                text = trimmed,
                rotationDeg = 0f,
                inverted = false,
                accent = false,
            ),
        )
    }

    val titleHash = trimmed.hashCode()
    val result = ArrayList<RansomGlyphSpec>(trimmed.length)
    var glyphIndex = 0
    for (ch in trimmed) {
        if (ch.isWhitespace()) continue
        val mixed = (titleHash * 31) xor (glyphIndex * -1640531527)
        val bucket = (mixed and 0x7FFFFFFF) % 81 // 0..80
        val rotationDeg = (bucket - 40) * 0.1f // -4.0f..+4.0f
        result.add(
            RansomGlyphSpec(
                text = ch.toString(),
                rotationDeg = rotationDeg,
                inverted = glyphIndex % 2 == 1,
                accent = glyphIndex == 0,
            ),
        )
        glyphIndex++
    }
    return result
}

@Composable
fun P5RansomTitle(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
) {
    val tokens = LocalP5Tokens.current
    val reducedMotion = rememberMotionReduced()
    val specs = remember(title, tokens.ransomTitleEnabled, reducedMotion) {
        layoutRansomTitle(
            title = title,
            enabled = tokens.ransomTitleEnabled,
            reducedMotion = reducedMotion,
        )
    }
    if (specs.isEmpty()) return

    val bannerMode = !tokens.ransomTitleEnabled || reducedMotion
    if (bannerMode) {
        val text = specs.first().text
        val isCallingCard = tokens.palette == P5Palette.CALLING_CARD
        Column(
            modifier = modifier
                .width(IntrinsicSize.Max)
                .p5Surface(
                    variant = P5ShapeVariant.BADGE,
                    fillColor = tokens.darkCard.asColor(),
                    strokeColor = if (isCallingCard) tokens.strokeDark.asColor() else tokens.strokeLight.asColor(),
                    shadowColor = tokens.shadowColor.asColor(),
                    strokeWidth = 2.dp,
                    shadowOffset = DpOffset(3.dp, 3.dp),
                ),
        ) {
            Text(
                text = text,
                color = tokens.strokeLight.asColor(),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(tokens.brandFill.asColor()),
            )
        }
        return
    }

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = title.trim()
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        specs.forEach { spec ->
            val bgColor = when {
                spec.accent -> tokens.brandFill.asColor()
                spec.inverted -> tokens.lightCard.asColor()
                else -> tokens.darkCard.asColor()
            }
            val fgColor = when {
                spec.accent -> tokens.onBrandFill.asColor()
                spec.inverted -> tokens.onLightCard.asColor()
                else -> tokens.strokeLight.asColor()
            }
            val strokeColor = if (spec.inverted) {
                tokens.strokeDark.asColor()
            } else {
                tokens.strokeLight.asColor()
            }
            Box(
                modifier = Modifier
                    .graphicsLayer { rotationZ = spec.rotationDeg }
                    .p5Surface(
                        variant = P5ShapeVariant.BADGE,
                        fillColor = bgColor,
                        strokeColor = strokeColor,
                        shadowColor = tokens.strokeDark.asColor(),
                        strokeWidth = 1.5.dp,
                        shadowOffset = DpOffset(2.dp, 2.dp),
                    )
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = spec.text,
                    color = fgColor,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
            }
        }
    }
}
