package com.actiary.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.ThemeTokens
import com.actiary.theme.asColor
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop

/** Self-contained: uses the given [tokens] / [profile] (not the locals) so drafts can be previewed. */
@Composable
fun GlassPreview(tokens: ThemeTokens, profile: GlassProfile, modifier: Modifier = Modifier) {
    val bg = rememberGlassLayer()
    val content = rememberGlassLayer()
    val combined = rememberCombinedBackdrop(bg, content)
    var tab by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(
        LocalThemeTokens provides tokens,
        LocalGlassProfile provides profile,
        LocalGlassLayers provides GlassLayers(bg, combined),
    ) {
        val n = tokens.neutrals
        Box(modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(24.dp)).clearAndSetSemantics {}) {
            AmbientBackground(tokens, Modifier.fillMaxSize().layerBackdrop(bg))
            Column(Modifier.fillMaxSize().layerBackdrop(content).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    (12..16).forEach { d ->
                        val on = d == 14
                        Box(
                            Modifier.size(32.dp).clip(CircleShape)
                                .background(if (on) tokens.accent.accentAction.asColor() else n.surface.asColor()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$d", fontSize = 13.sp, color = (if (on) tokens.accent.onAccentAction else n.textPrimary).asColor())
                        }
                    }
                }
                Box(Modifier.padding(top = 16.dp).fillMaxWidth(0.6f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(n.textSecondary.asColor()))
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassBottomBar(
                    listOf(GlassTab(Icons.Filled.CalendarMonth, ""), GlassTab(Icons.Filled.Settings, "")),
                    tab, { tab = it }, Modifier.weight(1f),
                )
                Box(
                    Modifier.height(48.dp).clip(RoundedCornerShape(50)).background(tokens.accent.accentAction.asColor())
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(16.dp).clip(CircleShape).background(tokens.accent.onAccentAction.asColor())) }
            }
        }
    }
}
