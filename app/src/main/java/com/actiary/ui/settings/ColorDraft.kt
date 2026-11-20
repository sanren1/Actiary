package com.actiary.ui.settings

import com.actiary.theme.Hsv
import com.actiary.theme.normalizeHex
import com.actiary.theme.toArgb
import com.actiary.theme.toHex

/**
 * Editor draft state. [hex] is the last valid seed ('#RRGGBB'); [input] is the raw text in the
 * HEX field, which may be temporarily invalid. Nothing is persisted until the user applies.
 */
data class ColorDraft(val hex: String, val input: String)

/** Valid input advances the seed and keeps the raw text; invalid input only updates the text. */
fun ColorDraft.withInput(text: String): ColorDraft =
    when (val normalized = normalizeHex(text)) {
        null -> copy(input = text)
        else -> ColorDraft(hex = normalized, input = text)
    }

/** Replaces both the seed and the field text from an HSV change, keeping them in sync. */
fun ColorDraft.withHsv(hsv: Hsv): ColorDraft {
    val hex = hsv.toArgb().toHex()
    return ColorDraft(hex = hex, input = hex)
}

/** True only when the current field text is a valid HEX color. */
val ColorDraft.canApply: Boolean get() = normalizeHex(input) != null
