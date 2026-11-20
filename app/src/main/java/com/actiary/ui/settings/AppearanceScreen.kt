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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.GlassTintStrength
import com.actiary.data.appearance.ThemeMode
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassPreview
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.glass.LocalGlassProfile
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.contrast
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
    onGlassLevel: (GlassLevel) -> Unit,
    onGlassTint: (GlassTintStrength) -> Unit,
    onAmbient: (Boolean) -> Unit,
    scrollContainer: Boolean = true,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent

    // true: standalone page owns the single scroller + insets. false: host owns them, so this
    // column only carries its own spacing and must not nest another same-direction scroller.
    val outer = if (scrollContainer) {
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
            .navigationBarsPadding()
    } else {
        Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
    }
    Column(outer, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BasicText(
            stringResource(R.string.appearance_title),
            Modifier.height(48.dp).semantics { heading() },
            style = style(n.textPrimary, 28, FontWeight.Bold),
        )
        GlassPreview(t, LocalGlassProfile.current)

        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Segmented(
                    listOf(
                        ThemeMode.LIGHT to R.string.theme_mode_light,
                        ThemeMode.DARK to R.string.theme_mode_dark,
                        ThemeMode.SYSTEM to R.string.theme_mode_system,
                    ),
                    settings.themeMode, onThemeMode,
                )
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
                                val seed = p.seed() ?: 0
                                val checkColor = if (contrast(seed, 0xFFFFFFFF.toInt()) >= contrast(seed, 0xFF000000.toInt())) Color.White else Color.Black
                                if (sel) Canvas(Modifier.size(20.dp)) {
                                    val path = Path().apply {
                                        moveTo(size.width * 0.15f, size.height * 0.55f)
                                        lineTo(size.width * 0.4f, size.height * 0.8f)
                                        lineTo(size.width * 0.85f, size.height * 0.25f)
                                    }
                                    drawPath(path, checkColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                                }
                            }
                            BasicText(
                                presetName(p),
                                style = style(n.textPrimary, 12, if (sel) FontWeight.Bold else FontWeight.Normal),
                            )
                        }
                    }
                }
            }
        }

        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BasicText(stringResource(R.string.glass_level_title), style = style(n.textSecondary, 14))
                Segmented(
                    listOf(
                        GlassLevel.SOFT to R.string.glass_level_soft,
                        GlassLevel.BALANCED to R.string.glass_level_balanced,
                        GlassLevel.CLEAR to R.string.glass_level_clear,
                    ),
                    settings.glassLevel, onGlassLevel,
                )
                BasicText(stringResource(R.string.glass_tint_title), style = style(n.textSecondary, 14))
                Segmented(
                    listOf(
                        GlassTintStrength.LOW to R.string.glass_tint_low,
                        GlassTintStrength.MEDIUM to R.string.glass_tint_medium,
                        GlassTintStrength.HIGH to R.string.glass_tint_high,
                    ),
                    settings.glassTintStrength, onGlassTint,
                )
                val on = settings.ambientBackground
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .semantics { toggleableState = ToggleableState(on) }
                        .clickable(role = Role.Switch) { onAmbient(!on) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    BasicText(stringResource(R.string.ambient_title), style = style(n.textPrimary))
                    Box(
                        Modifier.size(width = 48.dp, height = 28.dp)
                            .background(if (on) a.accentAction.asColor() else n.surfaceMuted.asColor(), CircleShape)
                            .border(1.dp, if (on) a.accentAction.asColor() else n.divider.asColor(), CircleShape)
                            .padding(3.dp),
                        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier.size(20.dp).background(if (on) a.onAccentAction.asColor() else n.textSecondary.asColor(), CircleShape),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, Int>>, current: T, onSelect: (T) -> Unit) {
    GlassSegmentedPicker(
        options.map { stringResource(it.second) },
        options.indexOfFirst { it.first == current },
        { onSelect(options[it].first) },
    )
}
