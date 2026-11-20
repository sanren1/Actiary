package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.Stroke
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.glass.GlassPreview
import com.actiary.glass.rememberGlassDegraded
import com.actiary.glass.resolveGlass
import com.actiary.theme.Hsv
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.normalizeHex
import com.actiary.theme.parseHex
import com.actiary.theme.resolveTheme
import com.actiary.theme.toArgb
import com.actiary.theme.toHsv
import kotlin.math.roundToInt

/** Encodes the draft as two strings so it survives rotation / process death. */
private val ColorDraftSaver = Saver<ColorDraft, ArrayList<String>>(
    save = { arrayListOf(it.hex, it.input) },
    restore = { ColorDraft(hex = it[0], input = it[1]) },
)

@Composable
internal fun ColorEditorScreen(
    settings: AppearanceSettings,
    recent: List<String>,
    onApply: (String) -> Unit,
    onBack: () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val degraded = rememberGlassDegraded()
    val initialHex = normalizeHex(settings.customAccentHex) ?: AppearanceSettings().customAccentHex
    var draft by rememberSaveable(stateSaver = ColorDraftSaver) {
        mutableStateOf(ColorDraft(hex = initialHex, input = initialHex))
    }

    // Preview must follow the draft, never the currently applied accent (roadmap §8 P3).
    val draftTokens = resolveTheme(
        settings.copy(accentPreset = AccentPreset.CUSTOM, customAccentHex = draft.hex),
        systemDark,
    )
    val draftProfile = resolveGlass(settings.glassLevel, settings.glassTintStrength, draftTokens, degraded)

    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val hsv = parseHex(draft.hex)!!.toHsv()
    val valid = draft.canApply
    val invalidMessage = stringResource(R.string.color_hex_invalid)

    SettingsPageLayout(stringResource(R.string.custom_color_title), onBack) {
        GlassPreview(draftTokens, draftProfile)

        val fieldShape = RoundedCornerShape(if (valid) 12.dp else 4.dp)
        Column(
            Modifier.fillMaxWidth()
                .clip(fieldShape)
                .background(n.surface.asColor())
                .border(
                    if (valid) 1.dp else 2.dp,
                    if (valid) n.divider.asColor() else a.accentStroke.asColor(),
                    fieldShape,
                )
                .semantics { if (!valid) error(invalidMessage) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(stringResource(R.string.color_hex_label), style = TextStyle(n.textSecondary.asColor(), 12.sp))
            BasicTextField(
                value = draft.input,
                onValueChange = { draft = draft.withInput(it) },
                singleLine = true,
                textStyle = TextStyle(n.textPrimary.asColor(), 18.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (draft.input.isEmpty()) {
                        BasicText("#RRGGBB", style = TextStyle(n.textSecondary.asColor(), 18.sp))
                    } else {
                        inner()
                    }
                },
            )
        }
        if (!valid) {
            BasicText(invalidMessage, style = TextStyle(a.accentAction.asColor(), 13.sp))
        }

        HsvSlider(
            label = stringResource(R.string.color_hue),
            valueText = "${hsv.h.roundToInt()}°",
            value = hsv.h.toFloat(),
            max = 360f,
            brush = Brush.horizontalGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
            ),
            onValueChange = { draft = draft.withHsv(hsv.copy(h = it.toDouble())) },
        )
        HsvSlider(
            label = stringResource(R.string.color_saturation),
            valueText = "${(hsv.s * 100).roundToInt()}%",
            value = (hsv.s * 100).toFloat(),
            max = 100f,
            brush = Brush.horizontalGradient(
                listOf(Color(Hsv(hsv.h, 0.0, hsv.v).toArgb()), Color(Hsv(hsv.h, 1.0, hsv.v).toArgb())),
            ),
            onValueChange = { draft = draft.withHsv(hsv.copy(s = (it / 100f).toDouble())) },
        )
        HsvSlider(
            label = stringResource(R.string.color_value),
            valueText = "${(hsv.v * 100).roundToInt()}%",
            value = (hsv.v * 100).toFloat(),
            max = 100f,
            brush = Brush.horizontalGradient(
                listOf(Color(Hsv(hsv.h, hsv.s, 0.0).toArgb()), Color(Hsv(hsv.h, hsv.s, 1.0).toArgb())),
            ),
            onValueChange = { draft = draft.withHsv(hsv.copy(v = (it / 100f).toDouble())) },
        )

        if (recent.isNotEmpty()) {
            BasicText(stringResource(R.string.color_recent), style = TextStyle(n.textSecondary.asColor(), 14.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                recent.take(5).forEach { hex ->
                    val color = parseHex(hex)
                    if (color != null) {
                        Box(
                            Modifier.size(40.dp)
                                .clip(CircleShape)
                                .background(Color(color), CircleShape)
                                .border(1.dp, n.divider.asColor(), CircleShape)
                                .clickable(role = Role.Button) { draft = draft.withInput(hex) }
                                .semantics { contentDescription = hex },
                        )
                    }
                }
            }
        }

        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (valid) a.accentAction.asColor() else n.surfaceMuted.asColor())
                .clickable(enabled = valid, role = Role.Button) {
                    onApply(draft.hex)
                    onBack()
                },
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                stringResource(R.string.color_apply),
                style = TextStyle(
                    color = if (valid) a.onAccentAction.asColor() else n.textSecondary.asColor(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

/** Self-drawn slider: gradient track + thumb, with range semantics and TalkBack progress control. */
@Composable
private fun HsvSlider(
    label: String,
    valueText: String,
    value: Float,
    max: Float,
    brush: Brush,
    onValueChange: (Float) -> Unit,
) {
    val n = LocalThemeTokens.current.neutrals
    // pointerInput is keyed only by max; read the freshest callback so SV values never go stale.
    val currentOnChange by rememberUpdatedState(onValueChange)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(label, style = TextStyle(n.textPrimary.asColor(), 14.sp))
            BasicText(valueText, style = TextStyle(n.textSecondary.asColor(), 14.sp))
        }
        Box(
            Modifier.fillMaxWidth().height(44.dp)
                .pointerInput(max) {
                    fun setFromX(x: Float) {
                        currentOnChange((x / size.width).coerceIn(0f, 1f) * max)
                    }
                    detectTapGestures { setFromX(it.x) }
                    detectHorizontalDragGestures { change, _ -> setFromX(change.position.x) }
                }
                .drawBehind {
                    val trackHeight = 12.dp.toPx()
                    val top = (size.height - trackHeight) / 2f
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(0f, top),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f),
                    )
                    val x = (value / max) * size.width
                    val center = Offset(x, size.height / 2f)
                    val radius = 11.dp.toPx()
                    drawCircle(n.textPrimary.asColor(), radius, center, style = Stroke(2.dp.toPx()))
                    drawCircle(Color.White, radius - 2.dp.toPx(), center)
                }
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..max, 0)
                    setProgress { newValue ->
                        if (newValue in 0f..max) {
                            onValueChange(newValue)
                            true
                        } else {
                            false
                        }
                    }
                },
        )
    }
}
