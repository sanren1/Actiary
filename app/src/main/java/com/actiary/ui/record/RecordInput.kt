package com.actiary.ui.record

private const val MAX_MINUTES = 100_000
private const val MAX_KM_TENTHS = 100_000 // 10,000 km

fun sanitizeMinutes(text: String): String =
    text.filter { it in '0'..'9' }.take(6).let { s -> s.takeIf { (it.toIntOrNull() ?: 0) <= MAX_MINUTES } ?: s.dropLast(1) }

/** Keeps digits and a single separator (normalised to '.') with at most one decimal digit. */
fun sanitizeKm(text: String): String {
    val sb = StringBuilder()
    var sep = false
    var decimals = 0
    for (c in text) {
        when {
            c in '0'..'9' -> if (!sep) sb.append(c) else if (decimals < 1) { sb.append(c); decimals++ }
            (c == '.' || c == ',') && !sep -> { sb.append('.'); sep = true }
        }
    }
    var r = sb.toString()
    // Cap like the parser so a displayed value never silently saves as null.
    while (r.isNotEmpty() && r != "." && parseKmToMeters(r) == null) r = r.dropLast(1)
    return r
}

fun parseMinutes(text: String): Int? =
    text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()?.takeIf { it <= MAX_MINUTES }

fun parseKmToMeters(text: String): Int? {
    val s = text.replace(',', '.')
    if (s.isEmpty() || s.any { it !in '0'..'9' && it != '.' } || s.count { it == '.' } > 1) return null
    val (whole, frac) = s.split('.').let { it[0] to it.getOrElse(1) { "" } }
    if (frac.length > 1 || (whole.isEmpty() && frac.isEmpty())) return null
    val tenths = (whole.ifEmpty { "0" }.toIntOrNull() ?: return null).toLong() * 10 + (frac.ifEmpty { "0" }.toInt())
    return if (tenths > MAX_KM_TENTHS) null else (tenths * 100).toInt()
}

fun formatMinutes(minutes: Int?): String = minutes?.toString().orEmpty()

fun formatKm(meters: Int?): String {
    if (meters == null) return ""
    val tenths = (meters + 50) / 100
    return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}
