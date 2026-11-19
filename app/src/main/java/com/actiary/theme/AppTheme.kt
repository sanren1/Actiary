package com.actiary.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeTokens = staticCompositionLocalOf<ThemeTokens> { error("no theme") }

fun Int.asColor(): Color = Color(this)

@Composable
fun AppTheme(tokens: ThemeTokens, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeTokens provides tokens, content = content)
}
