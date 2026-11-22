package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.activeAccentHex
import com.actiary.data.appearance.builtInAccentColors
import com.actiary.glass.GlassAnchoredMenuItem
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassPreview
import com.actiary.glass.GlassToggle
import com.actiary.glass.LocalGlassAnchoredMenu
import com.actiary.glass.LocalGlassProfile
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.contrast
import com.actiary.theme.parseHex
import com.actiary.ui.common.SelectionMenuRow

@Composable
internal fun presetName(p: AccentPreset): String = stringResource(
    when (p) {
        AccentPreset.MIST_TEAL -> R.string.preset_mist_teal
        AccentPreset.SLATE_BLUE -> R.string.preset_slate_blue
        AccentPreset.SAGE -> R.string.preset_sage
        AccentPreset.SAND -> R.string.preset_sand
        AccentPreset.DUSTY_LAVENDER -> R.string.preset_dusty_lavender
        AccentPreset.GRAPHITE -> R.string.preset_graphite
        AccentPreset.CUSTOM -> R.string.preset_custom
    },
)

private fun style(c: Int, size: Int = 16, w: FontWeight = FontWeight.Normal) =
    TextStyle(color = c.asColor(), fontSize = size.sp, fontWeight = w)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(
    settings: AppearanceSettings,
    onThemeMode: (ThemeMode) -> Unit,
    accentColors: List<String>,
    onSelectAccent: (String) -> Unit,
    onRemoveAccent: (String) -> Unit,
    onGlassLevel: (GlassLevel) -> Unit,
    onAmbient: (Boolean) -> Unit,
    onBack: () -> Unit,
    onResetAppearance: () -> Unit,
    onCustomColor: (String?) -> Unit,
    onChartPalette: (ChartPalette) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals

    SettingsPageLayout(stringResource(R.string.appearance_title), onBack) {
        GlassPreview(t, LocalGlassProfile.current, chartPalette = settings.chartPalette)

        AppearanceSection(stringResource(R.string.appearance_theme_mode), Icons.Outlined.BrightnessAuto) {
            SettingsGroup { rowShape ->
                Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                        val (label, icon) = when (mode) {
                            ThemeMode.SYSTEM -> R.string.theme_mode_system to Icons.Outlined.BrightnessAuto
                            ThemeMode.LIGHT -> R.string.theme_mode_light to Icons.Outlined.LightMode
                            ThemeMode.DARK -> R.string.theme_mode_dark to Icons.Outlined.DarkMode
                        }
                        val selected = mode == settings.themeMode
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clip(rowShape).background(n.surface.asColor())
                                .selectable(selected, role = Role.RadioButton) { onThemeMode(mode) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, contentDescription = null, tint = n.textSecondary.asColor())
                            BasicText(stringResource(label), Modifier.weight(1f), style = style(n.textPrimary))
                            if (selected) Icon(Icons.Outlined.Check, null, tint = t.accent.accentAction.asColor())
                        }
                    }
                }
            }
        }
        AppearanceSection(stringResource(R.string.accent_title), Icons.Outlined.Palette) {
            GlassPanel(Modifier.fillMaxWidth()) {
                FlowRow(
                    Modifier.padding(12.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val active = settings.activeAccentHex()
                    accentColors.forEach { hex ->
                        key(hex) {
                            val preset = builtInAccentColors[hex]
                            AccentSwatch(
                                seed = checkNotNull(parseHex(hex)),
                                label = if (preset != null) presetName(preset) else stringResource(R.string.accent_color_description, hex),
                                isSelected = hex == active,
                                onClick = { onSelectAccent(hex) },
                                onEdit = { onCustomColor(hex) },
                                onDelete = { onRemoveAccent(hex) },
                            )
                        }
                    }
                    val addLabel = stringResource(R.string.accent_add)
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = addLabel) { onCustomColor(null) }
                            .semantics { contentDescription = addLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(34.dp).background(n.surfaceMuted.asColor(), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(20.dp), tint = n.textSecondary.asColor())
                        }
                    }
                }
            }
        }
        val glassLabel = stringResource(R.string.glass_level_title)
        AppearanceSection(glassLabel, Icons.Outlined.WaterDrop) {
            SettingsGroup {
                SelectionMenuRow(
                    label = stringResource(R.string.appearance_glass_style),
                    options = listOf(
                        GlassLevel.SOFT to stringResource(R.string.glass_level_soft),
                        GlassLevel.BALANCED to stringResource(R.string.glass_level_balanced),
                        GlassLevel.CLEAR to stringResource(R.string.glass_level_clear),
                    ),
                    selected = settings.glassLevel,
                    onSelect = onGlassLevel,
                )
                val ambientLabel = stringResource(R.string.ambient_title)
                GlassToggle(
                    checked = settings.ambientBackground,
                    onCheckedChange = onAmbient,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    label = ambientLabel,
                ) {
                    BasicText(ambientLabel, style = style(n.textPrimary))
                }
            }
        }
        SelectionMenuRow(
            label = stringResource(R.string.chart_palette_title),
            options = listOf(
                ChartPalette.NEUTRAL_FIRST to stringResource(R.string.chart_palette_neutral),
                ChartPalette.FOLLOW_ACCENT to stringResource(R.string.chart_palette_accent),
                ChartPalette.MUTED_CATEGORIES to stringResource(R.string.chart_palette_muted),
            ),
            selected = settings.chartPalette,
            onSelect = onChartPalette,
        )
        SettingsRow(stringResource(R.string.settings_reset_appearance), Icons.Outlined.RestartAlt, onClick = onResetAppearance)
    }
}

@Composable
private fun AppearanceSection(label: String, icon: ImageVector, content: @Composable () -> Unit) {
    val color = LocalThemeTokens.current.neutrals.textPrimary
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.padding(horizontal = 4.dp).semantics(mergeDescendants = true) { heading() },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = color.asColor())
            BasicText(label, style = style(color, 14, FontWeight.SemiBold))
        }
        content()
    }
}

@Composable
private fun AccentSwatch(
    seed: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val menu = LocalGlassAnchoredMenu.current
    val optionsLabel = stringResource(R.string.accent_options)
    val editLabel = stringResource(R.string.accent_edit_action)
    val deleteLabel = stringResource(R.string.accent_delete)
    var anchor by remember { mutableStateOf<Rect?>(null) }
    Box(
        Modifier.size(48.dp)
            .onGloballyPositioned { anchor = it.boundsInRoot() }
            .clip(CircleShape)
            .combinedClickable(
                role = Role.RadioButton,
                onClick = onClick,
                onLongClickLabel = optionsLabel,
                onLongClick = {
                    anchor?.let { bounds ->
                        menu.show(
                            bounds,
                            listOf(
                                GlassAnchoredMenuItem(editLabel, Icons.Outlined.Edit, onClick = onEdit),
                                GlassAnchoredMenuItem(deleteLabel, Icons.Outlined.Delete, destructive = true, onClick = onDelete),
                            ),
                        )
                    }
                },
            )
            .semantics {
                contentDescription = label
                selected = isSelected
            }
            .then(if (isSelected) Modifier.border(2.dp, t.accent.accentStroke.asColor(), CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).background(seed.asColor(), CircleShape), contentAlignment = Alignment.Center) {
            if (isSelected) {
                val tint = if (contrast(seed, 0xFFFFFFFF.toInt()) >= contrast(seed, 0xFF000000.toInt())) Color.White else Color.Black
                Icon(Icons.Outlined.Check, null, Modifier.size(20.dp), tint = tint)
            }
        }
    }
}
