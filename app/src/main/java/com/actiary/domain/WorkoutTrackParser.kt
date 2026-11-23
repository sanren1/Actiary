package com.actiary.domain

import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-JDK parser for GPX and TCX workout/route files exported by Huawei Health and other sports apps.
 * Returns null if the XML is malformed or contains no recognizable workout metrics.
 */
object WorkoutTrackParser {
    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val MIN_ELEVATION_STEP_METERS = 1.0

    internal fun parse(input: InputStream, zone: ZoneId = ZoneId.systemDefault()): ParsedWorkout? = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        val doc = factory.newDocumentBuilder().parse(input)
        val root = doc.documentElement ?: return@runCatching null
        when (root.localName?.lowercase() ?: root.tagName.lowercase()) {
            "gpx" -> parseGpx(root, zone)
            "trainingcenterdatabase" -> parseTcx(root, zone)
            else -> null
        }
    }.getOrNull()

    private fun parseGpx(root: Element, zone: ZoneId): ParsedWorkout? {
        val trkName = firstChildText(root, "trk", "name") ?: firstChildText(root, "metadata", "name")
        val trkType = firstChildText(root, "trk", "type")
        val typeKey = inferTypeKey(trkType, trkName)

        val trkpts = elementsByLocalName(root, "trkpt")
        if (trkpts.isEmpty()) return null

        var firstTime: Instant? = null
        var lastTime: Instant? = null
        var prevLat: Double? = null
        var prevLon: Double? = null
        var prevEle: Double? = null
        var totalDistance = 0.0
        var totalElevationGain = 0.0
        var hrSum = 0L
        var hrCount = 0
        var cadSum = 0L
        var cadCount = 0

        for (pt in trkpts) {
            val lat = pt.getAttribute("lat").toDoubleOrNull()
            val lon = pt.getAttribute("lon").toDoubleOrNull()
            if (lat != null && lon != null) {
                if (prevLat != null && prevLon != null) {
                    totalDistance += haversineMeters(prevLat, prevLon, lat, lon)
                }
                prevLat = lat
                prevLon = lon
            }
            val ele = directChildText(pt, "ele")?.toDoubleOrNull()
            if (ele != null) {
                if (prevEle != null) {
                    val delta = ele - prevEle
                    if (delta >= MIN_ELEVATION_STEP_METERS) {
                        totalElevationGain += delta
                        prevEle = ele
                    } else if (delta <= -MIN_ELEVATION_STEP_METERS) {
                        prevEle = ele
                    }
                } else {
                    prevEle = ele
                }
            }
            val time = directChildText(pt, "time")?.let(::parseInstant)
            if (time != null) {
                if (firstTime == null || time.isBefore(firstTime)) firstTime = time
                if (lastTime == null || time.isAfter(lastTime)) lastTime = time
            }
            // Check optional extensions (gpxtpx:hr / hr, gpxtpx:cad / cad)
            val ext = directChildElement(pt, "extensions")
            if (ext != null) {
                firstDescendantText(ext, "hr")?.toDoubleOrNull()?.roundToInt()?.takeIf { it in 30..240 }?.let {
                    hrSum += it
                    hrCount++
                }
                firstDescendantText(ext, "cad")?.toDoubleOrNull()?.roundToInt()?.takeIf { it in 20..260 }?.let {
                    cadSum += it
                    cadCount++
                }
            }
        }

        val durationSeconds = if (firstTime != null && lastTime != null && lastTime.isAfter(firstTime)) {
            Duration.between(firstTime, lastTime).seconds.toInt().coerceIn(1, 24 * 3600)
        } else null
        val durationMinutes = durationSeconds?.let { ((it + 30) / 60).coerceAtLeast(1) }
        val distanceMeters = totalDistance.roundToInt().takeIf { it > 0 }
        val elevationGainCm = (totalElevationGain * 100.0).roundToInt().takeIf { it > 0 }
        val avgHr = if (hrCount > 0) (hrSum.toDouble() / hrCount).roundToInt() else null
        val avgCad = if (cadCount > 0) (cadSum.toDouble() / cadCount).roundToInt() else null
        val date = (firstTime ?: firstChildText(root, "metadata", "time")?.let(::parseInstant))
            ?.atZone(zone)?.toLocalDate()

        if (durationMinutes == null && distanceMeters == null && avgHr == null) return null
        val metrics = WorkoutMetrics(
            durationSeconds = durationSeconds,
            cadenceSpm = avgCad,
            elevationGainCm = elevationGainCm,
        ).takeUnless { it == WorkoutMetrics() }
        return ParsedWorkout(
            date = date,
            typeKey = typeKey,
            durationMinutes = durationMinutes,
            distanceMeters = distanceMeters,
            calories = null,
            avgHeartRate = avgHr,
            metrics = metrics,
        )
    }

    private fun parseTcx(root: Element, zone: ZoneId): ParsedWorkout? {
        val activities = elementsByLocalName(root, "Activity")
        val activity = activities.firstOrNull() ?: return null
        val sportAttr = activity.getAttribute("Sport")
        val notes = directChildText(activity, "Notes")
        val typeKey = inferTypeKey(sportAttr, notes)

        val idTime = directChildText(activity, "Id")?.let(::parseInstant)
        val laps = elementsByLocalName(activity, "Lap")
        var totalSeconds = 0.0
        var totalDistance = 0.0
        var totalCalories = 0
        var hasCalories = false
        var weightedHr = 0.0
        var hrWeight = 0.0
        var firstLapStart: Instant? = null

        for (lap in laps) {
            val lapStart = lap.getAttribute("StartTime").takeIf { it.isNotBlank() }?.let(::parseInstant)
            if (lapStart != null && (firstLapStart == null || lapStart.isBefore(firstLapStart))) {
                firstLapStart = lapStart
            }
            val sec = directChildText(lap, "TotalTimeSeconds")?.toDoubleOrNull() ?: 0.0
            totalSeconds += sec
            totalDistance += directChildText(lap, "DistanceMeters")?.toDoubleOrNull() ?: 0.0
            directChildText(lap, "Calories")?.toDoubleOrNull()?.roundToInt()?.takeIf { it > 0 }?.let {
                totalCalories += it
                hasCalories = true
            }
            val lapHr = directChildElement(lap, "AverageHeartRateBpm")?.let { directChildText(it, "Value") }
                ?.toDoubleOrNull()
            if (lapHr != null && lapHr in 30.0..240.0) {
                val w = if (sec > 0.0) sec else 1.0
                weightedHr += lapHr * w
                hrWeight += w
            }
        }

        // Trackpoints for elevation gain & fallback HR/cadence
        val trackpoints = elementsByLocalName(activity, "Trackpoint")
        var prevAlt: Double? = null
        var totalElevationGain = 0.0
        var tpHrSum = 0L
        var tpHrCount = 0
        var cadSum = 0L
        var cadCount = 0
        for (tp in trackpoints) {
            val alt = directChildText(tp, "AltitudeMeters")?.toDoubleOrNull()
            if (alt != null) {
                if (prevAlt != null) {
                    val delta = alt - prevAlt
                    if (delta >= MIN_ELEVATION_STEP_METERS) {
                        totalElevationGain += delta
                        prevAlt = alt
                    } else if (delta <= -MIN_ELEVATION_STEP_METERS) {
                        prevAlt = alt
                    }
                } else {
                    prevAlt = alt
                }
            }
            if (hrWeight == 0.0) {
                directChildElement(tp, "HeartRateBpm")?.let { directChildText(it, "Value") }
                    ?.toDoubleOrNull()?.roundToInt()?.takeIf { it in 30..240 }?.let {
                        tpHrSum += it
                        tpHrCount++
                    }
            }
            (directChildText(tp, "Cadence") ?: firstDescendantText(tp, "RunCadence"))
                ?.toDoubleOrNull()?.roundToInt()?.takeIf { it in 20..260 }?.let {
                    cadSum += it
                    cadCount++
                }
        }

        val durationSeconds = totalSeconds.roundToInt().takeIf { it > 0 }
        val durationMinutes = durationSeconds?.let { ((it + 30) / 60).coerceAtLeast(1) }
        val distanceMeters = totalDistance.roundToInt().takeIf { it > 0 }
        val calories = totalCalories.takeIf { hasCalories && it > 0 }
        val avgHr = when {
            hrWeight > 0.0 -> (weightedHr / hrWeight).roundToInt()
            tpHrCount > 0 -> (tpHrSum.toDouble() / tpHrCount).roundToInt()
            else -> null
        }
        val avgCad = if (cadCount > 0) (cadSum.toDouble() / cadCount).roundToInt() else null
        val elevationGainCm = (totalElevationGain * 100.0).roundToInt().takeIf { it > 0 }
        val date: LocalDate? = (firstLapStart ?: idTime)?.atZone(zone)?.toLocalDate()

        if (durationMinutes == null && distanceMeters == null && calories == null && avgHr == null) return null
        val metrics = WorkoutMetrics(
            durationSeconds = durationSeconds,
            cadenceSpm = avgCad,
            elevationGainCm = elevationGainCm,
        ).takeUnless { it == WorkoutMetrics() }
        return ParsedWorkout(
            date = date,
            typeKey = typeKey,
            durationMinutes = durationMinutes,
            distanceMeters = distanceMeters,
            calories = calories,
            avgHeartRate = avgHr,
            metrics = metrics,
        )
    }

    private fun inferTypeKey(vararg hints: String?): String? {
        val combined = hints.filterNotNull().joinToString(" ").lowercase()
        if (combined.isBlank()) return null
        return when {
            listOf("run", "jog", "跑").any { it in combined } -> "running"
            listOf("cycl", "bik", "ride", "骑").any { it in combined } -> "cycling"
            listOf("swim", "游").any { it in combined } -> "swimming"
            listOf("hik", "walk", "trail", "mountain", "徒步", "健走", "登山", "步行").any { it in combined } -> "hiking"
            listOf("ball", "basket", "soccer", "tennis", "badminton", "球").any { it in combined } -> "ball"
            listOf("strength", "gym", "free", "workout", "fitness", "力量", "自由", "健身").any { it in combined } -> "freeform"
            else -> null
        }
    }

    private fun parseInstant(raw: String): Instant? = runCatching { Instant.parse(raw.trim()) }.getOrNull()

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    private fun elementsByLocalName(parent: Element, localName: String): List<Element> {
        val list = parent.getElementsByTagNameNS("*", localName)
        val out = ArrayList<Element>(list.length)
        for (i in 0 until list.length) {
            val node = list.item(i)
            if (node is Element) out.add(node)
        }
        return out
    }

    private fun directChildElement(parent: Element, localName: String): Element? {
        var child: Node? = parent.firstChild
        while (child != null) {
            if (child is Element && (child.localName == localName || child.tagName == localName)) return child
            child = child.nextSibling
        }
        return null
    }

    private fun directChildText(parent: Element, localName: String): String? =
        directChildElement(parent, localName)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

    private fun firstChildText(root: Element, parentName: String, childName: String): String? =
        elementsByLocalName(root, parentName).firstOrNull()?.let { directChildText(it, childName) }

    private fun firstDescendantText(parent: Element, localName: String): String? =
        elementsByLocalName(parent, localName).firstOrNull()?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}
