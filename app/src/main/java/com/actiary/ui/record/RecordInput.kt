package com.actiary.ui.record

import com.actiary.domain.WorkoutMetric
import com.actiary.ui.common.isLength
import java.math.BigDecimal

private const val MAX_MINUTES = 100_000
private const val MAX_KM_HUNDREDTHS = 1_000_000 // 10,000 km
private const val MAX_CALORIES = 9999
private const val MIN_HEART_RATE = 30
private const val MAX_HEART_RATE = 230

fun sanitizeMinutes(text: String): String =
    text.filter { it in '0'..'9' }.take(6).let { s -> s.takeIf { (it.toIntOrNull() ?: 0) <= MAX_MINUTES } ?: s.dropLast(1) }

/** Keeps digits and a single separator (normalised to '.') with at most two decimal digits. */
fun sanitizeKm(text: String): String {
    val sb = StringBuilder()
    var sep = false
    var decimals = 0
    for (c in text) {
        when {
            c in '0'..'9' -> if (!sep) sb.append(c) else if (decimals < 2) { sb.append(c); decimals++ }
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
    if (frac.length > 2 || (whole.isEmpty() && frac.isEmpty())) return null
    val hundredths = (whole.ifEmpty { "0" }.toIntOrNull() ?: return null).toLong() * 100 + frac.padEnd(2, '0').toInt()
    return if (hundredths > MAX_KM_HUNDREDTHS) null else (hundredths * 10).toInt()
}

fun formatMinutes(minutes: Int?): String = minutes?.toString().orEmpty()

fun formatKm(meters: Int?): String {
    if (meters == null) return ""
    val hundredths = (meters.toLong() + 5) / 10
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return if (fraction.isEmpty()) (hundredths / 100).toString() else "${hundredths / 100}.$fraction"
}

fun sanitizeCalories(text: String): String = text.filter { it in '0'..'9' }.take(4)

fun parseCalories(text: String): Int? =
    text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()?.takeIf { it in 1..MAX_CALORIES }

fun sanitizeHeartRate(text: String): String = text.filter { it in '0'..'9' }.take(3)

fun parseHeartRate(text: String): Int? =
    text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()?.takeIf { it in MIN_HEART_RATE..MAX_HEART_RATE }

fun formatCalories(v: Int?): String = v?.toString().orEmpty()

fun formatHeartRate(v: Int?): String = v?.toString().orEmpty()

fun formatMeters(meters: Int?): String = meters?.toString().orEmpty()

fun parseMeters(text: String): Int? =
    text.takeIf { it.isNotEmpty() && it.all { c -> c in '0'..'9' } }?.toIntOrNull()?.takeIf { it > 0 }

fun formatDistance(meters: Int?, typeKey: String?, hasDistance: Boolean): String {
    if (!hasDistance || meters == null) return ""
    return if (typeKey == "swimming") formatMeters(meters) else formatKm(meters)
}

private val decimalMeters = Regex("(?:[0-9]+(?:[.,][0-9]{0,2})?|[.,][0-9]{1,2})")

internal fun parseCentimeters(text: String): Int? {
    if (!decimalMeters.matches(text)) return null
    val normalized = text.replace(',', '.')
    val meters = normalized.substringBefore('.').ifEmpty { "0" }.toIntOrNull() ?: return null
    val fraction = normalized.substringAfter('.', "").padEnd(2, '0').toInt()
    return (meters.toLong() * 100 + fraction).takeIf { it <= Int.MAX_VALUE }?.toInt()
}

/** Null when [text] is malformed or above the metric's limit. */
internal fun parseWorkoutMetric(metric: WorkoutMetric, text: String): Int? =
    (if (metric.isLength) parseCentimeters(text) else text.toIntOrNull()?.takeIf { it >= 0 })?.takeIf { it <= metric.max }

internal fun formatCentimeters(value: Int?): String = value?.let {
    BigDecimal.valueOf(it.toLong(), 2).stripTrailingZeros().toPlainString()
}.orEmpty()

internal fun formatWorkoutClock(totalSeconds: Long): String {
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    val minutes = totalSeconds / 60
    if (minutes < 60) return "$minutes:$seconds"
    return "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:$seconds"
}

internal fun paceSeconds(durationSeconds: Int?, distanceMeters: Int?, distanceUnit: Int): Long? {
    val time = durationSeconds?.takeIf { it > 0 } ?: return null
    val distance = distanceMeters?.takeIf { it > 0 } ?: return null
    return (time.toLong() * distanceUnit + distance / 2) / distance
}
