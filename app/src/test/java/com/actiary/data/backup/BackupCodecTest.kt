package com.actiary.data.backup

import com.actiary.data.db.DayStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

private const val json = """{
    "format": 1, "appVersion": "0.13.0", "exportedAt": 1700000000000,
    "appearance": { "ambient_background": false },
    "types": [{"id": 40, "name": "攀岩", "sortOrder": 0, "archived": false, "hasDistance": false}],
    "records": [{"epochDay": 20, "status": "EXERCISED", "note": "训练", "updatedAt": 123}],
    "entries": [{"id": 50, "epochDay": 20, "typeId": 40, "durationMinutes": 30, "sortOrder": 0}],
    "photos": [{"epochDay": 20, "fileName": "photo.jpg", "sortOrder": 0, "addedAt": 123}]
}"""

class BackupCodecTest {
    @Test fun roundTrip() {
        val data = BackupData(
            1, "0.13.0", 1_700_000_000_000L, mapOf("ambient_background" to "false", "theme_mode" to "DARK"),
            listOf(BackupType(40, null, "攀岩", 0, false, false, null)),
            listOf(BackupRecord(20, DayStatus.EXERCISED, "训练", 123)),
            listOf(BackupEntry(50, 20, 40, 30, null, null, null, 0)),
            listOf(BackupPhoto(20, "photo.jpg", 0, 123)),
        )
        assertEquals(false, JSONObject(data.toJson()).getJSONObject("appearance").get("ambient_background"))
        assertEquals(data, parseBackup(data.toJson()))
    }

    @Test fun missingOptionalFieldsBecomeNull() {
        val parsed = parseBackup(json)
        assertEquals(BackupType(40, null, "攀岩", 0, false, false, null), parsed.types.single())
        assertEquals(BackupEntry(50, 20, 40, 30, null, null, null, 0), parsed.entries.single())
    }

    @Test fun unknownExtraFieldsIgnored() {
        val extra = JSONObject(json).put("future", "new")
        extra.getJSONObject("appearance").put("future", true)
        extra.getJSONArray("records").getJSONObject(0).put("future", JSONObject())
        assertEquals(parseBackup(json), parseBackup(extra.toString()))
    }

    @Test fun higherFormatRejected() {
        reject(json.replace("\"format\": 1", "\"format\": 2"))
    }

    @Test fun garbageRejected() {
        reject("unreadable")
        reject("$json trailing data")
    }

    @Test fun entryWithUnknownTypeRejected() {
        reject(json.replace("\"typeId\": 40", "\"typeId\": 17"))
    }

    @Test fun photoForUnknownDayRejected() {
        reject(json.replace("20, \"fileName\"", "21, \"fileName\""))
    }

    @Test fun malformedRequiredFieldsRejected() {
        reject(json.replace("\"sortOrder\": 0", "\"sortOrder\": \"0\""))
        reject(json.replace("\"note\": \"训练\", ", ""))
        reject(json.replace("\"EXERCISED\"", "\"UNKNOWN\""))
        reject(json.replace("\"durationMinutes\": 30", "\"durationMinutes\": 3.5"))
        reject(json.replace("1700000000000", "9223372036854775808"))
        reject(json.replace("photo.jpg", "../photo.jpg"))
    }

    @Test fun duplicateIdentitiesRejected() {
        for (table in listOf("types", "records", "entries")) {
            val duplicate = JSONObject(json)
            val rows = duplicate.getJSONArray(table)
            rows.put(rows.getJSONObject(0))
            reject(duplicate.toString())
        }
    }

    @Test fun duplicateAttachmentNamesRejectedAcrossDays() {
        val duplicate = JSONObject(json)
        val days = duplicate.getJSONArray("records")
        days.put(JSONObject(days.getJSONObject(0).toString()).put("epochDay", 21))
        val photos = duplicate.getJSONArray("photos")
        photos.put(JSONObject(photos.getJSONObject(0).toString()).put("epochDay", 21))
        reject(duplicate.toString())
    }

    @Test fun rejectFieldsThatExceedLocalRecordLimits() {
        reject(json.replace("训练", "x".repeat(201)))
        reject(json.replace("攀岩", "攀".repeat(32_769)))

        val crowded = JSONObject(json)
        val photos = crowded.getJSONArray("photos")
        repeat(9) { index ->
            photos.put(JSONObject(photos.getJSONObject(0).toString()).put("fileName", "$index.jpg"))
        }
        reject(crowded.toString())
    }

    @Test fun rejectUnknownBuiltinsAndKeepArchivedYoga() {
        val archive = JSONObject(json)
        val type = archive.getJSONArray("types").getJSONObject(0)
        type.put("builtinKey", "unsupported")
        reject(archive.toString())
        type.put("builtinKey", "yoga")
        reject(archive.toString())
        type.put("archived", true)
        assertEquals("yoga", parseBackup(archive.toString()).types.single().builtinKey)
    }

    private fun reject(input: String) {
        assertThrows(BackupFormatException::class.java) { parseBackup(input) }
    }
}
