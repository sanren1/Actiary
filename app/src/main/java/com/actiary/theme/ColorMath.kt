package com.actiary.theme

import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val HEX_RE = Regex("^#[0-9A-Fa-f]{6}$")

fun parseHex(hex: String): Int? =
    if (HEX_RE.matches(hex)) (0xFF000000L or hex.substring(1).toLong(16)).toInt() else null

fun Int.toHex(): String = "#%06X".format(this and 0xFFFFFF)

data class Oklch(val l: Double, val c: Double, val h: Double)

private fun decode(v: Int): Double {
    val s = v / 255.0
    return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
}

private fun encode(v: Double): Int {
    val x = v.coerceIn(0.0, 1.0)
    val s = if (x <= 0.0031308) x * 12.92 else 1.055 * x.pow(1 / 2.4) - 0.055
    return (s * 255).roundToInt().coerceIn(0, 255)
}

private fun ch(argb: Int, shift: Int) = (argb shr shift) and 0xFF

fun Int.toOklch(): Oklch {
    val r = decode(ch(this, 16)); val g = decode(ch(this, 8)); val b = decode(ch(this, 0))
    val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val bigL = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    val c = sqrt(a * a + bb * bb)
    val h = if (c < 1e-4) 0.0 else Math.toDegrees(atan2(bb, a)).let { if (it < 0) it + 360 else it }
    return Oklch(bigL, c, h)
}

private fun linearRgb(o: Oklch, c: Double): DoubleArray {
    val hr = Math.toRadians(o.h)
    val a = c * cos(hr); val b = c * sin(hr)
    val l = (o.l + 0.3963377774 * a + 0.2158037573 * b).let { it * it * it }
    val m = (o.l - 0.1055613458 * a - 0.0638541728 * b).let { it * it * it }
    val s = (o.l - 0.0894841775 * a - 1.2914855480 * b).let { it * it * it }
    return doubleArrayOf(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )
}

private const val EPS = 1e-4
private fun inGamut(v: DoubleArray) = v.all { it >= -EPS && it <= 1 + EPS }

fun Oklch.toArgb(): Int {
    var rgb = linearRgb(this, c)
    if (!inGamut(rgb)) {
        var lo = 0.0; var hi = c
        repeat(40) {
            val mid = (lo + hi) / 2
            if (inGamut(linearRgb(this, mid))) lo = mid else hi = mid
        }
        rgb = linearRgb(this, lo)
    }
    return (0xFF shl 24) or (encode(rgb[0]) shl 16) or (encode(rgb[1]) shl 8) or encode(rgb[2])
}

private fun luminance(argb: Int) =
    0.2126 * decode(ch(argb, 16)) + 0.7152 * decode(ch(argb, 8)) + 0.0722 * decode(ch(argb, 0))

fun contrast(a: Int, b: Int): Double {
    val la = luminance(a); val lb = luminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

fun mix(base: Int, overlay: Int, overlayWeight: Double): Int {
    val w = overlayWeight.coerceIn(0.0, 1.0)
    fun c(shift: Int) = (ch(base, shift) * (1 - w) + ch(overlay, shift) * w).roundToInt().coerceIn(0, 255)
    return (0xFF shl 24) or (c(16) shl 16) or (c(8) shl 8) or c(0)
}
