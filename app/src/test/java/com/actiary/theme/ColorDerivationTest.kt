package com.actiary.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ColorDerivationTest {
    private val presetSeeds = listOf("#758B84", "#77879D", "#8C9683", "#AC9680", "#928A9F", "#82858A")
    private val seeds = presetSeeds + listOf("#FFFFFF", "#000000", "#FF00FF", "#FFFF00")

    private fun hueDiff(a: Double, b: Double) = abs(a - b).let { minOf(it, 360 - it) }

    @Test fun contrastAndSeedInvariants() {
        for (hex in seeds) for (dark in listOf(false, true)) {
            val seed = parseHex(hex)!!
            val n = if (dark) DarkNeutrals else LightNeutrals
            val a = deriveAccent(seed, n, dark)
            val tag = "$hex dark=$dark"
            assertEquals(tag, seed, a.accentSeed)
            assertTrue("$tag action", contrast(a.onAccentAction, a.accentAction) >= 4.5)
            assertTrue("$tag stroke", contrast(a.accentStroke, n.surface) >= 3.0)
            assertTrue("$tag chart", contrast(a.accentChart, n.background) >= 3.0)
        }
    }

    @Test fun hueIsPreservedForChromaticPresets() {
        for (hex in presetSeeds.filter { it != "#82858A" }) for (dark in listOf(false, true)) {
            val seed = parseHex(hex)!!
            val a = deriveAccent(seed, if (dark) DarkNeutrals else LightNeutrals, dark)
            val d = hueDiff(seed.toOklch().h, a.accentAction.toOklch().h)
            assertTrue("$hex dark=$dark hue diff $d", d <= 5.0)
        }
    }

    @Test fun darkIsRegeneratedNotCopied() {
        for (hex in seeds) {
            val seed = parseHex(hex)!!
            assertNotEquals(hex, deriveAccent(seed, LightNeutrals, false).accentAction,
                deriveAccent(seed, DarkNeutrals, true).accentAction)
        }
    }

    @Test fun ambientAndSubtleWeights() {
        for (hex in listOf("#77879D", "#AC9680")) for (dark in listOf(false, true)) {
            val seed = parseHex(hex)!!
            val n = if (dark) DarkNeutrals else LightNeutrals
            val tag = "$hex dark=$dark"
            val amb = deriveAmbient(seed, n, dark)
            val o = seed.toOklch()
            val shifted = Oklch(o.l, o.c, (o.h + 40) % 360).toArgb()
            assertEquals(tag, mix(n.background, seed, if (dark) 0.16 else 0.12), amb.blobA)
            assertEquals(tag, mix(n.background, shifted, if (dark) 0.10 else 0.08), amb.blobB)
            val a = deriveAccent(seed, n, dark)
            assertEquals(tag, mix(n.surface, seed, if (dark) 0.10 else 0.08), a.accentSubtle)
            assertEquals(tag, if (dark) n.background else 0xFFFFFFFF.toInt(), a.onAccentAction)
        }
    }
}
