package com.actiary.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ThemeMode
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.seed

private val presets = AccentPreset.entries.filter { it != AccentPreset.CUSTOM }

@Composable
private fun presetName(p: AccentPreset): String = stringResource(
    when (p) {
        AccentPreset.MIST_TEAL -> R.string.preset_mist_teal
        AccentPreset.SLATE_BLUE -> R.string.preset_slate_blue
        AccentPreset.SAGE -> R.string.preset_sage
        AccentPreset.SAND -> R.string.preset_sand
        AccentPreset.DUSTY_LAVENDER -> R.string.preset_dusty_lavender
        AccentPreset.GRAPHITE -> R.string.preset_graphite
        AccentPreset.CUSTOM -> R.string.accent_title
    },
)

private fun style(c: Int, size: Int = 16, w: FontWeight = FontWeight.Normal) =
    TextStyle(color = c.asColor(), fontSize = size.sp, fontWeight = w)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    settings: AppearanceSettings,
    onThemeMode: (ThemeMode) -> Unit,
    onPreset: (AccentPreset) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent

    Column(
        Modifier.fillMaxSize().background(n.background.asColor()).safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BasicText(stringResource(R.string.appearance_title), style = style(n.textPrimary, 28, FontWeight.Bold))

        Row(
            Modifier.fillMaxWidth().background(n.surfaceMuted.asColor(), RoundedCornerShape(12.dp)).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(
                ThemeMode.LIGHT to R.string.theme_mode_light,
                ThemeMode.DARK to R.string.theme_mode_dark,
                ThemeMode.SYSTEM to R.string.theme_mode_system,
            ).forEach { (mode, label) ->
                val sel = settings.themeMode == mode
                Box(
                    Modifier.weight(1f)
                        .background(if (sel) n.surface.asColor() else Color.Transparent, RoundedCornerShape(9.dp))
                        .semantics { selected = sel }
                        .clickable(role = Role.Button) { onThemeMode(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(label),
                        style = style(n.textPrimary, 15, if (sel) FontWeight.Bold else FontWeight.Normal),
                    )
                }
            }
        }

        BasicText(stringResource(R.string.accent_title), style = style(n.textSecondary, 14))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            presets.forEach { p ->
                val sel = settings.accentPreset == p
                Column(
                    Modifier.width(96.dp)
                        .then(if (sel) Modifier.border(2.dp, a.accentStroke.asColor(), RoundedCornerShape(12.dp)) else Modifier)
                        .semantics { selected = sel }
                        .clickable(role = Role.Button) { onPreset(p) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier.size(40.dp).background((p.seed() ?: 0).asColor(), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sel) Canvas(Modifier.size(20.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.15f, size.height * 0.55f)
                                lineTo(size.width * 0.4f, size.height * 0.8f)
                                lineTo(size.width * 0.85f, size.height * 0.25f)
                            }
                            drawPath(path, Color.White, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                    BasicText(
                        presetName(p),
                        style = style(n.textPrimary, 12, if (sel) FontWeight.Bold else FontWeight.Normal),
                    )
                }
            }
        }

        Box(
            Modifier.background(a.accentAction.asColor(), RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) { BasicText(stringResource(R.string.sample_button), style = style(a.onAccentAction)) }
        Box(
            Modifier.background(a.accentSubtle.asColor(), RoundedCornerShape(8.dp))
                .border(1.dp, a.accentStroke.asColor(), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) { BasicText(stringResource(R.string.sample_tag), style = style(n.textPrimary, 14)) }
        BasicText(stringResource(R.string.sample_body), style = style(n.textPrimary))
        BasicText(stringResource(R.string.sample_secondary), style = style(n.textSecondary, 14))
    }
}
