package com.actiary.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.P5Background
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.ThemeTokens
import com.actiary.theme.asColor

@Composable
fun AmbientBackground(tokens: ThemeTokens, modifier: Modifier = Modifier) {
    if (LocalUiStyle.current == UiStyle.PERSONA_5) {
        P5Background(LocalP5Tokens.current, modifier)
        return
    }
    if (!tokens.ambientEnabled) {
        Box(modifier.background(tokens.neutrals.background.asColor()))
        return
    }
    val a = tokens.ambient.blobA.asColor()
    val b = tokens.ambient.blobB.asColor()
    Box(
        modifier.drawBehind {
            drawRect(tokens.ambient.base.asColor())
            val r = size.width * 0.9f
            drawRect(Brush.radialGradient(listOf(a, a.copy(alpha = 0f)), Offset.Zero, r))
            drawRect(Brush.radialGradient(listOf(b, b.copy(alpha = 0f)), Offset(size.width, size.height), r))
        },
    )
}
