package com.actiary.theme

/** Steps the seed's lightness (down in light, up in dark) until [ok] holds; stops at the l bound. */
private fun stepUntil(seed: Int, dark: Boolean, ok: (Int) -> Boolean): Int {
    if (ok(seed)) return seed
    val o = seed.toOklch()
    var l = o.l
    while (true) {
        l = (if (dark) l + 0.005 else l - 0.005).coerceIn(0.0, 1.0)
        val c = Oklch(l, o.c, o.h).toArgb()
        if (ok(c) || l <= 0.0 || l >= 1.0) return c
    }
}

fun deriveAccent(seed: Int, n: NeutralTokens, dark: Boolean): AccentTokens {
    val on = if (dark) n.background else 0xFFFFFFFF.toInt()
    return AccentTokens(
        accentSeed = seed,
        accentAction = stepUntil(seed, dark) { contrast(on, it) >= 4.5 },
        onAccentAction = on,
        accentSubtle = mix(n.surface, seed, if (dark) 0.10 else 0.08),
        accentStroke = stepUntil(seed, dark) { contrast(it, n.surface) >= 3.0 },
        accentChart = stepUntil(seed, dark) { contrast(it, n.background) >= 3.0 },
        glassAccentTint = seed,
    )
}

fun deriveAmbient(seed: Int, n: NeutralTokens, dark: Boolean): AmbientTokens {
    val o = seed.toOklch()
    val shifted = Oklch(o.l, o.c, (o.h + 40) % 360).toArgb()
    return AmbientTokens(
        base = n.background,
        blobA = mix(n.background, seed, if (dark) 0.28 else 0.20),
        blobB = mix(n.background, shifted, if (dark) 0.19 else 0.14),
    )
}
