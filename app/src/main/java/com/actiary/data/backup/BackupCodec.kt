package com.actiary.data.backup

import com.actiary.data.appearance.AMBIENT_BACKGROUND
import com.actiary.data.appearance.backupAppearanceStringKeys
import com.actiary.data.db.ActiaryDatabase
import com.actiary.data.db.DayStatus
import com.actiary.domain.DayDraft
import com.actiary.domain.WORKOUT_METRIC_MASK
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.WorkoutMetrics
import com.actiary.domain.roundedWorkoutMinutes
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener
import java.time.LocalDate

private fun obj(vararg fields: Pair<String, Any?>) = JSONObject().apply {
    fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
}

private fun <T> List<T>.encode(row: (T) -> JSONObject) = JSONArray().apply {
    for (item in this@encode) put(row(item))
}

fun BackupData.toJson(): String = obj(
    "format" to format, "appVersion" to appVersion, "exportedAt" to exportedAt,
    "appearance" to JSONObject().apply {
        for (key in backupAppearanceStringKeys) appearance[key.name]?.let { put(key.name, it) }
        appearance[AMBIENT_BACKGROUND.name]?.let { put(AMBIENT_BACKGROUND.name, it.toBooleanStrict()) }
    },
    "types" to types.encode {
        obj("id" to it.id, "builtinKey" to it.builtinKey, "name" to it.name, "sortOrder" to it.sortOrder,
            "archived" to it.archived, "hasDistance" to it.hasDistance, "iconKey" to it.iconKey,
            "extraMetricMask" to it.extraMetricMask)
    },
    "records" to records.encode {
        obj("epochDay" to it.epochDay, "status" to it.status.name, "note" to it.note, "updatedAt" to it.updatedAt)
    },
    "entries" to entries.encode {
        obj("id" to it.id, "epochDay" to it.epochDay, "typeId" to it.typeId, "durationMinutes" to it.durationMinutes,
            "distanceMeters" to it.distanceMeters, "calories" to it.calories, "avgHeartRate" to it.avgHeartRate,
            "sortOrder" to it.sortOrder, "durationSeconds" to it.metrics.durationSeconds,
            "cadenceSpm" to it.metrics.cadenceSpm, "stepCount" to it.metrics.stepCount,
            "elevationGainCm" to it.metrics.elevationGainCm, "poolLengthCm" to it.metrics.poolLengthCm,
            "lapCount" to it.metrics.lapCount, "strokeCount" to it.metrics.strokeCount)
    },
    "photos" to photos.encode {
        obj("epochDay" to it.epochDay, "fileName" to it.fileName, "sortOrder" to it.sortOrder, "addedAt" to it.addedAt)
    },
).toString()

private val safePhotoName = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,127}")
internal fun isPhotoFileName(name: String): Boolean = safePhotoName.matches(name)

// Keep each exercise type row comfortably below Android's CursorWindow capacity.
private const val MAX_TYPE_TEXT_CHARS = 32 * 1024

internal fun BackupData.validateLocalRows() {
    for (type in types) {
        val key = type.builtinKey
        if (key == null) {
            if (type.name.isNullOrBlank()) invalid("Missing custom type name")
        } else if (key !in ActiaryDatabase.BUILTIN_TYPE_KEYS && !(key == "yoga" && type.archived)) {
            invalid("Unsupported built-in type")
        }
        val textLength = key.orEmpty().length + type.name.orEmpty().length + type.iconKey.orEmpty().length
        if (textLength > MAX_TYPE_TEXT_CHARS) invalid("Exercise type text is too long")
    }
    if (records.any { it.note.length > DayDraft.NOTE_LIMIT }) invalid("Daily note is too long")
    unique(photos.map { it.fileName }, "attachment file names")
    if (photos.groupingBy { it.epochDay }.eachCount().values.any { it > DayDraft.PHOTO_LIMIT }) {
        invalid("Too many attachments for a date")
    }
}

fun parseBackup(json: String): BackupData {
    try {
        val tokens = JSONTokener(json)
        val root = tokens.nextValue() as? JSONObject ?: invalid("Expected a backup object")
        if (tokens.nextClean() != '\u0000' || '\u0000' in json) invalid("Invalid content after backup object")
        val format = root.int("format")
        if (format !in 1..BACKUP_FORMAT) invalid("Unsupported backup format: $format")

        val types = root.rows("types") {
            BackupType(id("id"), optionalString("builtinKey"), optionalString("name"), int("sortOrder"),
                bool("archived"), bool("hasDistance"), optionalString("iconKey"),
                metric("extraMetricMask")?.also { if ((it and WORKOUT_METRIC_MASK) != it) invalid("Unknown workout metric selection") })
        }
        val typeIds = unique(types.map { it.id }, "type IDs")
        unique(types.mapNotNull { it.builtinKey }, "built-in type keys")

        val records = root.rows("records") {
            val status = string("status")
            val dayStatus = DayStatus.entries.firstOrNull { it.name == status } ?: invalid("Unknown day status")
            BackupRecord(epochDay(), dayStatus, string("note"), long("updatedAt"))
        }
        val days = unique(records.map { it.epochDay }, "record dates")

        val entries = root.rows("entries") {
            val day = epochDay()
            val type = id("typeId")
            if (day !in days || type !in typeIds) invalid("Entry references an unknown date or type")
            val durationMinutes = metric("durationMinutes")
            val durationSeconds = metric("durationSeconds")
            if (durationSeconds != null && roundedWorkoutMinutes(durationSeconds) != durationMinutes) {
                invalid("Workout duration does not match its rounded minutes")
            }
            BackupEntry(id("id"), day, type, durationMinutes, metric("distanceMeters"),
                metric("calories"), metric("avgHeartRate"), int("sortOrder"),
                WorkoutMetrics(
                    durationSeconds = durationSeconds,
                    cadenceSpm = metric("cadenceSpm"),
                    stepCount = metric("stepCount"),
                    elevationGainCm = metric("elevationGainCm"),
                    poolLengthCm = metric("poolLengthCm"),
                    lapCount = metric("lapCount"),
                    strokeCount = metric("strokeCount"),
                ).also { m ->
                    if (WorkoutMetric.entries.any { (it.value(m) ?: 0) > it.max }) invalid("Workout metric out of range")
                })
        }
        unique(entries.map { it.id }, "entry IDs")

        val photos = root.rows("photos") {
            val day = epochDay()
            val name = string("fileName")
            if (day !in days || !isPhotoFileName(name)) invalid("Invalid attachment date or file name")
            BackupPhoto(day, name, int("sortOrder"), long("addedAt"))
        }

        val appearance = root.getJSONObject("appearance")
        val values = buildMap {
            for (key in backupAppearanceStringKeys) appearance.optionalString(key.name)?.let { put(key.name, it) }
            val ambient = AMBIENT_BACKGROUND.name
            if (!appearance.isNull(ambient)) put(ambient, appearance.bool(ambient).toString())
        }
        val version = root.string("appVersion")
        if (version.isBlank()) invalid("Missing app version")
        return BackupData(format, version, root.long("exportedAt"), values, types, records, entries, photos)
            .also { it.validateLocalRows() }
    } catch (e: JSONException) {
        throw BackupFormatException("Malformed backup JSON", e)
    } catch (e: StackOverflowError) {
        // Android's JSONTokener recursively parses nested input without a depth limit.
        throw BackupFormatException("Backup JSON is too deeply nested", e)
    }
}

private fun invalid(message: String): Nothing = throw BackupFormatException(message)

private fun <T> unique(values: List<T>, field: String): Set<T> = values.toHashSet().also {
    if (it.size != values.size) invalid("Duplicate $field")
}

private fun <T> JSONObject.rows(key: String, read: JSONObject.() -> T): List<T> {
    val array = getJSONArray(key)
    return List(array.length()) { array.getJSONObject(it).read() }
}

private fun JSONObject.string(key: String): String = get(key) as? String ?: invalid("Expected text: $key")
private fun JSONObject.bool(key: String): Boolean = get(key) as? Boolean ?: invalid("Expected boolean: $key")
private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else string(key)
private fun JSONObject.long(key: String): Long = when (val value = get(key)) {
    is Int -> value.toLong()
    is Long -> value
    else -> invalid("Expected integer: $key")
}

private fun JSONObject.int(key: String): Int = long(key).takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
    ?: invalid("Integer out of range: $key")

private fun JSONObject.id(key: String): Long = long(key).takeIf { it > 0 } ?: invalid("Invalid ID: $key")

private fun JSONObject.epochDay(): Long = long("epochDay").takeIf {
    it in LocalDate.MIN.toEpochDay()..LocalDate.MAX.toEpochDay()
} ?: invalid("Date out of range")

private fun JSONObject.metric(key: String): Int? = if (isNull(key)) null else {
    int(key).takeIf { it >= 0 } ?: invalid("Negative metric: $key")
}
