package com.actiary.domain

import com.actiary.ui.record.parseCalories
import com.actiary.ui.record.parseHeartRate
import com.actiary.ui.record.parseKmToMeters
import com.actiary.ui.record.parseMeters
import com.actiary.ui.record.parseMinutes
import java.io.Serializable
import java.time.DateTimeException
import java.time.LocalDate
import kotlin.math.abs

internal data class RecognizedLine(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val height: Int get() = bottom - top
}

internal data class ParsedWorkout(
    val date: LocalDate?,
    val typeKey: String?,
    val durationMinutes: Int?,
    val distanceMeters: Int?,
    val calories: Int?,
    val avgHeartRate: Int?,
) : Serializable

private const val DURATION_LABEL = "运动时间"
private const val CALORIES_LABEL = "总消耗热量"
private const val HEART_RATE_LABEL = "平均心率"
private val metricLabels = setOf(DURATION_LABEL, CALORIES_LABEL, HEART_RATE_LABEL, "活动热量")
private val workoutTypes = mapOf("户外跑步" to "running", "泳池游泳" to "swimming", "自由训练" to "freeform")
private val chineseDate = Regex("""^([0-9]{4})年([0-9]{1,2})月([0-9]{1,2})日""")
private val elapsedTime = Regex("""([0-9]{1,4}):([0-5][0-9]):([0-5][0-9])""")
private val totalCalories = Regex("""([0-9]+)-?(?:千卡|干卡)?""")
private val averageHeartRate = Regex("""([0-9]+)(?:次/分钟)?""")
private val headlineNumber = Regex("""([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)[.-]?(公里|米)?""")
private val distanceUnits = setOf("公里", "米")

internal fun parseHuaweiWorkout(lines: List<RecognizedLine>): ParsedWorkout? {
    val ordered = lines.map { it.copy(text = it.text.filterNot(Char::isWhitespace)) }.sortedBy { it.top }
    val dateLine = ordered.firstOrNull { chineseDate.containsMatchIn(it.text) }
    val date = dateLine?.text?.let(::parseDate)
    val title = ordered.firstNotNullOfOrNull { line ->
        if (dateLine != null && line.bottom > dateLine.top) null
        else workoutTypes.entries.firstOrNull { line.text.endsWith(it.key) }?.let { line to it.value }
    }
    val typeKey = title?.second
    val duration = ordered.metricValue(DURATION_LABEL)?.let(::parseDuration)
    val calories = ordered.metricValue(CALORIES_LABEL)?.let {
        totalCalories.matchEntire(it)?.groupValues?.get(1)?.let(::parseCalories)
    }
    val heartRate = ordered.metricValue(HEART_RATE_LABEL)?.let {
        averageHeartRate.matchEntire(it)?.groupValues?.get(1)?.let(::parseHeartRate)
    }
    val firstMetric = ordered.firstOrNull { it.text in metricLabels }
    val distance = if (typeKey == "freeform") null else ordered.headlineDistance(typeKey, dateLine ?: title?.first, firstMetric)
    if (typeKey == null && duration == null && distance == null && calories == null && heartRate == null) return null
    return ParsedWorkout(date, typeKey, duration, distance, calories, heartRate)
}

private fun parseDate(text: String): LocalDate? {
    val (year, month, day) = chineseDate.find(text)?.destructured ?: return null
    return try {
        LocalDate.of(year.toInt(), month.toInt(), day.toInt())
    } catch (_: DateTimeException) {
        null
    }
}

private fun parseDuration(text: String): Int? {
    val (hours, minutes, seconds) = elapsedTime.matchEntire(text)?.destructured ?: return null
    val roundedMinutes = hours.toInt() * 60 + minutes.toInt() + (seconds.toInt() + 30) / 60
    return parseMinutes(roundedMinutes.toString())
}

private fun List<RecognizedLine>.metricValue(labelText: String): String? {
    val label = firstOrNull { it.text == labelText } ?: return null
    // The freeform total sits in a taller box immediately to the right of its label.
    val beside = filter { line ->
        line.left in label.right..(label.right + label.height) && line.top < label.bottom && line.bottom > label.top
    }.minByOrNull { it.left }
    val below = firstOrNull { line ->
        line.top in label.bottom..(label.bottom + 2 * label.height) && abs(line.left - label.left) <= label.height
    }
    return (beside ?: below)?.text
}

private fun List<RecognizedLine>.headlineDistance(
    typeKey: String?,
    header: RecognizedLine?,
    firstMetric: RecognizedLine?,
): Int? {
    val referenceHeight = (header ?: firstMetric)?.height ?: return null
    // The distance is the large readout between the date and the metric grid.
    return asSequence().filter { number ->
        number.height >= 2 * referenceHeight &&
            (header == null || number.top >= header.bottom) &&
            (firstMetric == null || number.bottom <= firstMetric.top)
    }.sortedByDescending { it.height }.firstNotNullOfOrNull { number ->
        val (value, inlineUnit) = headlineNumber.matchEntire(number.text)?.destructured ?: return@firstNotNullOfOrNull null
        val separateUnit = firstOrNull { unit ->
            unit.text in distanceUnits && unit.top >= number.top && unit.bottom <= number.bottom &&
                unit.right > number.right && unit.left <= number.right + unit.height
        }?.text
        val digits = value.replace(",", "")
        when (inlineUnit.ifEmpty { separateUnit.orEmpty() }) {
            "公里" -> parseKmToMeters(digits)?.takeIf { it > 0 }
            "米" -> parseMeters(digits)
            else -> if (typeKey == "swimming") parseMeters(digits) else null
        }
    }
}
