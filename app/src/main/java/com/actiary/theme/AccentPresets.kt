package com.actiary.theme

import com.actiary.data.appearance.AccentPreset

val DEFAULT_SEED = 0xFF77879D.toInt()

fun AccentPreset.seed(): Int? = when (this) {
    AccentPreset.MIST_TEAL -> 0xFF758B84.toInt()
    AccentPreset.SLATE_BLUE -> 0xFF77879D.toInt()
    AccentPreset.SAGE -> 0xFF8C9683.toInt()
    AccentPreset.SAND -> 0xFFAC9680.toInt()
    AccentPreset.DUSTY_LAVENDER -> 0xFF928A9F.toInt()
    AccentPreset.GRAPHITE -> 0xFF82858A.toInt()
    AccentPreset.CUSTOM -> null
}
