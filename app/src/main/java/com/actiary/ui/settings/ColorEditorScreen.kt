package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
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
import com.actiary.data.appearance.activeAccentHex
import com.actiary.glass.GlassPreview
import com.actiary.glass.GlassSheet
import com.actiary.glass.LocalGlassProfile
import com.actiary.glass.rememberGlassDegraded
import com.actiary.glass.resolveGlass
import com.actiary.theme.Hsv
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.theme.parseHex
import com.actiary.theme.resolveTheme
import com.actiary.theme.toArgb
import com.actiary.theme.toHsv
import kotlin.math.abs
import kotlin.math.roundToInt

private val HsvSaver = Saver<Hsv, ArrayList<Double>>(
    save = { arrayListOf(it.h, it.s, it.v) },
    restore = { Hsv(it[0], it[1], it[2]) },
)

/** Encodes the draft as two strings so it survives rotation / process death. */
private val ColorDraftSaver = Saver<ColorDraft, ArrayList<String>>(
    save = { arrayListOf(it.hex, it.input) },
    restore = { ColorDraft(hex = it[0], input = it[1]) },
)

@Composable
internal fun ColorEditorSheet(
    settings: AppearanceSettings,
    initialHex: String?,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(if (initialHex == null) R.string.accent_add else R.string.accent_edit)
    val focus = LocalFocusManager.current
    var visible by rememberSaveable { mutableStateOf(true) }
    fun close(): Boolean {
        focus.clearFocus()
        visible = false
        return true
    }
    GlassSheet(visible, ::close, onDismiss, title) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val n = LocalThemeTokens.current.neutrals
                BasicText(
                    title, Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(n.textPrimary.asColor(), 20.sp, FontWeight.SemiBold),
                )
                androidx.compose.material3.TextButton(
                    onClick = { close() },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = n.textSecondary.asColor()),
                ) {
                    androidx.compose.material3.Text(stringResource(R.string.record_cancel))
                }
            }
            ColorEditorContent(settings, initialHex ?: settings.activeAccentHex()) { hex ->
                onApply(hex)
                close()
            }
        }
    }
}

@Composable
private fun ColorEditorContent(
    settings: AppearanceSettings,
    initialHex: String,
    onApply: (String) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val degraded = rememberGlassDegraded() || LocalGlassProfile.current.degraded
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
    // Slider-owned HSV: hue survives S/V = 0; re-derived only when hex changes from elsewhere.
    var hsvState by rememberSaveable(stateSaver = HsvSaver) { mutableStateOf(parseHex(initialHex)!!.toHsv()) }
    val hsv = syncHsv(hsvState, draft.hex)
    val setHsv = { new: Hsv ->
        hsvState = new
        draft = draft.withHsv(new)
    }
    val valid = draft.canApply
    val invalidMessage = stringResource(R.string.color_hex_invalid)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassPreview(draftTokens, draftProfile, chartPalette = settings.chartPalette)

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
            onValueChange = { setHsv(hsv.copy(h = it.toDouble())) },
        )
        HsvSlider(
            label = stringResource(R.string.color_saturation),
            valueText = "${(hsv.s * 100).roundToInt()}%",
            value = (hsv.s * 100).toFloat(),
            max = 100f,
            brush = Brush.horizontalGradient(
                listOf(Color(Hsv(hsv.h, 0.0, hsv.v).toArgb()), Color(Hsv(hsv.h, 1.0, hsv.v).toArgb())),
            ),
            onValueChange = { setHsv(hsv.copy(s = (it / 100f).toDouble())) },
        )
        HsvSlider(
            label = stringResource(R.string.color_value),
            valueText = "${(hsv.v * 100).roundToInt()}%",
            value = (hsv.v * 100).toFloat(),
            max = 100f,
            brush = Brush.horizontalGradient(
                listOf(Color(Hsv(hsv.h, hsv.s, 0.0).toArgb()), Color(Hsv(hsv.h, hsv.s, 1.0).toArgb())),
            ),
            onValueChange = { setHsv(hsv.copy(v = (it / 100f).toDouble())) },
        )

        Box(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (valid) a.accentAction.asColor() else n.surfaceMuted.asColor())
                .clickable(enabled = valid, role = Role.Button) { onApply(draft.hex) },
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
            Modifier.fillMaxWidth().height(48.dp)
                .pointerInput(max) {
                    fun setFromX(x: Float) {
                        currentOnChange((x / size.width).coerceIn(0f, 1f) * max)
                    }
                    // Wait for horizontal intent so the surrounding page keeps vertical drags.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var dragging = false
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            if (change.changedToUp()) {
                                if (!dragging) {
                                    setFromX(change.position.x)
                                    change.consume()
                                }
                                break
                            }
                            if (!dragging) {
                                val delta = change.position - down.position
                                val horizontal = abs(delta.x)
                                val vertical = abs(delta.y)
                                if (maxOf(horizontal, vertical) < viewConfiguration.touchSlop) continue
                                if (vertical >= horizontal) break
                                dragging = true
                            }
                            setFromX(change.position.x)
                            change.consume()
                        }
                    }
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
