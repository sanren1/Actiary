package com.actiary.ui.settings

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Wallpaper
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.ChartPalette
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.ThemeMode
import com.actiary.data.appearance.UiStyle
import com.actiary.data.appearance.activeAccentHex
import com.actiary.data.appearance.builtInAccentColors
import com.actiary.glass.GlassAnchoredMenuItem
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassPreview
import com.actiary.glass.GlassToggle
import com.actiary.glass.LocalGlassAnchoredMenu
import com.actiary.glass.LocalGlassProfile
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Shape
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.theme.contrast
import com.actiary.theme.parseHex
import com.actiary.ui.common.SelectionMenuRow

@Composable
internal fun presetName(p: AccentPreset): String = stringResource(
    when (p) {
        AccentPreset.DYNAMIC -> R.string.preset_dynamic
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
    dynamicSeed: Int,
    onThemeMode: (ThemeMode) -> Unit,
    onAmoledBlack: (Boolean) -> Unit,
    accentColors: List<String>,
    onSelectDynamicAccent: () -> Unit,
    onSelectAccent: (String) -> Unit,
    onRemoveAccent: (String) -> Unit,
    onGlassLevel: (GlassLevel) -> Unit,
    onAmbient: (Boolean) -> Unit,
    onBack: () -> Unit,
    onResetAppearance: () -> Unit,
    onCustomColor: (String?) -> Unit,
    onChartPalette: (ChartPalette) -> Unit,
    onUiStyle: (UiStyle) -> Unit = {},
    onP5Palette: (P5Palette) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onP5RansomTitle: (Boolean) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onP5MotionLevel: (P5MotionLevel) -> Unit = {},
    isP5IconEnabled: Boolean = false,
    onSelectAppIcon: (Boolean) -> Unit = {},
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals

    SettingsPageLayout(stringResource(R.string.appearance_title), onBack) {
        AppearanceSection(stringResource(R.string.appearance_ui_style), Icons.Outlined.Style) {
            UiStyleSelector(
                selected = settings.uiStyle,
                onSelect = onUiStyle,
            )
        }

        AppearanceSection(stringResource(R.string.appearance_preview), Icons.Outlined.Visibility) {
            GlassPreview(t, LocalGlassProfile.current, chartPalette = settings.chartPalette)
        }

        if (settings.uiStyle == UiStyle.LIQUID_GLASS) {
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
                    val amoledLabel = stringResource(R.string.theme_amoled_black)
                    GlassToggle(
                        checked = settings.amoledBlack,
                        onCheckedChange = onAmoledBlack,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        label = amoledLabel,
                    ) {
                        BasicText(amoledLabel, style = style(n.textPrimary))
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
                        val isDynamic = settings.accentPreset == AccentPreset.DYNAMIC
                        DynamicAccentSwatch(
                            seed = dynamicSeed,
                            label = stringResource(R.string.preset_dynamic),
                            isSelected = isDynamic,
                            onClick = onSelectDynamicAccent,
                        )
                        val active = settings.activeAccentHex()
                        accentColors.forEach { hex ->
                            key(hex) {
                                val preset = builtInAccentColors[hex]
                                AccentSwatch(
                                    seed = checkNotNull(parseHex(hex)),
                                    label = if (preset != null) presetName(preset) else stringResource(R.string.accent_color_description, hex),
                                    isSelected = !isDynamic && hex == active,
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
        } else {
            Persona5SettingsSection(
                settings = settings,
                onP5Palette = onP5Palette,
            )
        }
        SelectionMenuRow(
            label = stringResource(R.string.appearance_app_icon),
            options = listOf(
                false to stringResource(R.string.ui_style_liquid_glass),
                true to stringResource(R.string.ui_style_persona_5),
            ),
            selected = isP5IconEnabled,
            onSelect = onSelectAppIcon,
        )
        val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
        val p5 = LocalP5Tokens.current
        if (isP5) {
            val resetShape = RoundedCornerShape(2.dp)
            val resetStroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(resetShape)
                    .background(if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor(), resetShape)
                    .border(2.dp, resetStroke, resetShape)
                    .clickable(role = Role.Button, onClick = onResetAppearance)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.RestartAlt, null, tint = t.accent.accentAction.asColor())
                BasicText(
                    stringResource(R.string.settings_reset_appearance),
                    style = style(n.textPrimary, w = FontWeight.Bold),
                )
            }
        } else {
            SettingsRow(stringResource(R.string.settings_reset_appearance), Icons.Outlined.RestartAlt, onClick = onResetAppearance)
        }
    }
}

@Composable
private fun UiStyleSelector(
    selected: UiStyle,
    onSelect: (UiStyle) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        UiStyleCard(
            title = stringResource(R.string.ui_style_liquid_glass),
            style = UiStyle.LIQUID_GLASS,
            isSelected = selected == UiStyle.LIQUID_GLASS,
            onClick = { onSelect(UiStyle.LIQUID_GLASS) },
            modifier = Modifier.weight(1f),
        )
        UiStyleCard(
            title = stringResource(R.string.ui_style_persona_5),
            style = UiStyle.PERSONA_5,
            isSelected = selected == UiStyle.PERSONA_5,
            onClick = { onSelect(UiStyle.PERSONA_5) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UiStyleCard(
    title: String,
    style: UiStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5Active = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val cardShape = if (isP5Active) RoundedCornerShape(2.dp) else RoundedCornerShape(18.dp)
    val previewShape = if (isP5Active) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
    val accentColor = if (isP5Active) p5.brandFill.asColor() else t.accent.accentAction.asColor()
    val onAccentColor = if (isP5Active) p5.onBrandFill.asColor() else t.accent.onAccentAction.asColor()
    val cardFill = if (isP5Active) {
        if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
    } else {
        n.surface.asColor()
    }
    val borderColor = when {
        isSelected -> accentColor
        isP5Active -> if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.55f) else p5.strokeDark.asColor()
        else -> n.divider.asColor()
    }
    val borderWidth = if (isSelected) 2.5.dp else if (isP5Active) 2.dp else 1.dp

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(cardShape)
            .background(cardFill, cardShape)
            .border(borderWidth, borderColor, cardShape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(previewShape),
            ) {
                when (style) {
                    UiStyle.LIQUID_GLASS -> LiquidGlassMiniPreview(Modifier.fillMaxSize())
                    UiStyle.PERSONA_5 -> Persona5MiniPreview(Modifier.fillMaxSize())
                }
            }
            BasicText(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = style(
                    n.textPrimary,
                    size = 14,
                    w = if (isSelected) FontWeight.Black else if (isP5Active) FontWeight.Bold else FontWeight.Medium,
                ),
            )
        }
        if (isSelected) {
            val badgeShape = if (isP5Active) p5Shape(P5ShapeVariant.BADGE) else CircleShape
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp)
                    .background(accentColor, badgeShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = onAccentColor,
                )
            }
        }
    }
}

@Composable
private fun LiquidGlassMiniPreview(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFDCE8F5), Color(0xFFEDF2F8), Color(0xFFD8E4EE)),
                start = Offset.Zero,
                end = Offset(w, h),
            ),
        )
        drawCircle(
            color = Color(0xFF8AA8C9).copy(alpha = 0.42f),
            radius = h * 0.48f,
            center = Offset(w * 0.22f, h * 0.30f),
        )
        drawCircle(
            color = Color(0xFFB5C7DE).copy(alpha = 0.45f),
            radius = h * 0.42f,
            center = Offset(w * 0.78f, h * 0.72f),
        )
        val cardLeft = w * 0.12f
        val cardTop = h * 0.18f
        val cardWidth = w * 0.76f
        val cardHeight = h * 0.64f
        val radius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        drawRoundRect(
            color = Color.White.copy(alpha = 0.62f),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = radius,
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.85f),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = radius,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFF5A7799).copy(alpha = 0.72f),
            topLeft = Offset(cardLeft + w * 0.08f, cardTop + h * 0.16f),
            size = Size(w * 0.28f, h * 0.14f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFF3A4B5E).copy(alpha = 0.24f),
            topLeft = Offset(cardLeft + w * 0.08f, cardTop + h * 0.38f),
            size = Size(w * 0.54f, h * 0.10f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
        )
    }
}

@Composable
private fun Persona5MiniPreview(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val dark = Color(0xFF0D0D0D)
        val red = Color(0xFFD91E26)
        val paper = Color(0xFFF7F6F2)
        val badge = Color(0xFFFFE600)

        drawRect(color = dark)

        val stripe = Path().apply {
            moveTo(-w * 0.08f, h * 0.78f)
            lineTo(w * 0.82f, -h * 0.08f)
            lineTo(w * 1.08f, h * 0.18f)
            lineTo(w * 0.18f, h * 1.08f)
            close()
        }
        drawPath(stripe, color = red)

        val shadowShift = 3.dp.toPx()
        val card = Path().apply {
            moveTo(w * 0.16f, h * 0.20f)
            lineTo(w * 0.88f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.82f)
            lineTo(w * 0.12f, h * 0.78f)
            close()
        }
        val cardShadow = Path().apply {
            moveTo(w * 0.16f + shadowShift, h * 0.20f + shadowShift)
            lineTo(w * 0.88f + shadowShift, h * 0.14f + shadowShift)
            lineTo(w * 0.82f + shadowShift, h * 0.82f + shadowShift)
            lineTo(w * 0.12f + shadowShift, h * 0.78f + shadowShift)
            close()
        }
        drawPath(cardShadow, color = dark)
        drawPath(card, color = paper)
        drawPath(card, color = dark, style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Miter))

        val bar = Path().apply {
            moveTo(w * 0.24f, h * 0.36f)
            lineTo(w * 0.62f, h * 0.33f)
            lineTo(w * 0.60f, h * 0.47f)
            lineTo(w * 0.22f, h * 0.50f)
            close()
        }
        drawPath(bar, color = red)

        val accent = Path().apply {
            moveTo(w * 0.66f, h * 0.56f)
            lineTo(w * 0.78f, h * 0.55f)
            lineTo(w * 0.76f, h * 0.69f)
            lineTo(w * 0.64f, h * 0.70f)
            close()
        }
        drawPath(accent, color = badge)
    }
}

@Composable
private fun Persona5SettingsSection(
    settings: AppearanceSettings,
    onP5Palette: (P5Palette) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val p5 = LocalP5Tokens.current

    AppearanceSection(stringResource(R.string.appearance_p5_palette), Icons.Outlined.Palette) {
        SettingsGroup { rowShape ->
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple(P5Palette.PHANTOM_RED, R.string.p5_palette_phantom_red, 0xFFD91E26.toInt()),
                    Triple(P5Palette.ROYAL_GOLD, R.string.p5_palette_royal_gold, 0xFFE5A910.toInt()),
                    Triple(P5Palette.VELVET_BLUE, R.string.p5_palette_velvet_blue, 0xFF1A4BD8.toInt()),
                    Triple(P5Palette.CALLING_CARD, R.string.p5_palette_calling_card, 0xFFF7F6F2.toInt()),
                ).forEach { (palette, labelRes, swatchArgb) ->
                    val isSelected = palette == settings.p5Palette
                    val rowFill = if (isSelected) p5.brandFill.asColor() else n.surfaceMuted.asColor()
                    val rowStroke = when {
                        isSelected -> if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
                        t.dark -> p5.strokeLight.asColor().copy(alpha = 0.35f)
                        else -> p5.strokeDark.asColor()
                    }
                    val textColor = if (isSelected) p5.onBrandFill else n.textPrimary
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clip(rowShape)
                            .background(rowFill, rowShape)
                            .border(if (isSelected) 2.dp else 1.5.dp, rowStroke, rowShape)
                            .selectable(isSelected, role = Role.RadioButton) { onP5Palette(palette) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val badgeShape = p5Shape(P5ShapeVariant.BADGE)
                        if (palette == P5Palette.CALLING_CARD) {
                            Canvas(
                                Modifier.size(24.dp)
                                    .clip(badgeShape)
                                    .border(1.5.dp, Color(0xFF0D0D0D), badgeShape),
                            ) {
                                drawRect(Color(0xFFF7F6F2))
                                val slash = Path().apply {
                                    moveTo(size.width * 0.42f, 0f)
                                    lineTo(size.width, 0f)
                                    lineTo(size.width, size.height)
                                    lineTo(size.width * 0.14f, size.height)
                                    close()
                                }
                                drawPath(slash, Color(0xFFD91E26))
                            }
                        } else {
                            val swatchBorder = if (isSelected) p5.onBrandFill.asColor() else if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
                            Box(
                                Modifier.size(24.dp)
                                    .clip(badgeShape)
                                    .background(swatchArgb.asColor())
                                    .border(1.5.dp, swatchBorder, badgeShape),
                            )
                        }
                        BasicText(
                            stringResource(labelRes),
                            Modifier.weight(1f),
                            style = style(textColor, w = if (isSelected) FontWeight.Black else FontWeight.Bold),
                        )
                        if (isSelected) Icon(Icons.Outlined.Check, null, tint = p5.onBrandFill.asColor())
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicAccentSwatch(
    seed: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    Box(
        Modifier.size(48.dp)
            .clip(CircleShape)
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .then(if (isSelected) Modifier.border(2.dp, t.accent.accentStroke.asColor(), CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).background(seed.asColor(), CircleShape), contentAlignment = Alignment.Center) {
            val tint = if (contrast(seed, 0xFFFFFFFF.toInt()) >= contrast(seed, 0xFF000000.toInt())) Color.White else Color.Black
            Icon(
                if (isSelected) Icons.Outlined.Check else Icons.Outlined.Wallpaper,
                null,
                Modifier.size(18.dp),
                tint = tint,
            )
        }
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
