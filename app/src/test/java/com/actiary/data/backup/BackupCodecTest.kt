package com.actiary.data.backup

import com.actiary.data.db.DayStatus
import com.actiary.domain.ALL_MUSCLE_MASK
import com.actiary.domain.MuscleGroup
import com.actiary.domain.WorkoutMetrics
import com.actiary.domain.muscleMaskOf
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
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
            BACKUP_FORMAT, "0.13.0", 1_700_000_000_000L, mapOf("ambient_background" to "false", "theme_mode" to "DARK"),
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

    @Test fun formatOneRoundTripsExtraMetricsAndExplicitlyDisabledTypeFields() {
        val fixture = JSONObject(json)
        fixture.getJSONArray("types").getJSONObject(0).put("extraMetricMask", 0)
        fixture.getJSONArray("entries").getJSONObject(0)
            .put("durationSeconds", 1799)
            .put("cadenceSpm", 142)
            .put("stepCount", 5300)
            .put("elevationGainCm", 315)
            .put("poolLengthCm", 2500)
            .put("lapCount", 44)
            .put("strokeCount", 511)

        val parsed = parseBackup(fixture.toString())
        assertEquals(0, parsed.types.single().extraMetricMask)
        assertEquals(
            WorkoutMetrics(1799, 142, 5300, 315, 2500, 44, 511),
            parsed.entries.single().metrics,
        )
        assertEquals(parsed, parseBackup(parsed.toJson()))
    }

    @Test fun rejectsMalformedExtraMetricValues() {
        val fixture = JSONObject(json)
        val entry = fixture.getJSONArray("entries").getJSONObject(0)
        entry.put("lapCount", -1)
        reject(fixture.toString())
        entry.put("lapCount", 1.5)
        reject(fixture.toString())
        entry.remove("lapCount")
        fixture.getJSONArray("types").getJSONObject(0).put("extraMetricMask", 64)
        reject(fixture.toString())
    }

    @Test fun exactDurationMustRoundToTheSavedMinutes() {
        val fixture = JSONObject(json)
        val entry = fixture.getJSONArray("entries").getJSONObject(0)
        entry.put("durationSeconds", 1829)
        assertEquals(1829, parseBackup(fixture.toString()).entries.single().metrics.durationSeconds)

        entry.put("durationSeconds", 1830)
        reject(fixture.toString())
        entry.remove("durationMinutes")
        reject(fixture.toString())
    }

    @Test fun unknownExtraFieldsIgnored() {
        val extra = JSONObject(json).put("future", "new")
        extra.getJSONObject("appearance").put("future", true)
        extra.getJSONArray("records").getJSONObject(0).put("future", JSONObject())
        assertEquals(parseBackup(json), parseBackup(extra.toString()))
    }

    @Test fun formatsOneThroughThreeAcceptedOthersRejected() {
        assertEquals(1, parseBackup(json).format)
        assertEquals(2, parseBackup(json.replace("\"format\": 1", "\"format\": 2")).format)
        assertEquals(3, parseBackup(json.replace("\"format\": 1", "\"format\": 3")).format)
        reject(json.replace("\"format\": 1", "\"format\": 4"))
        reject(json.replace("\"format\": 1", "\"format\": 0"))
    }

    @Test fun exportWritesCurrentFormatAndRoundTrips() {
        assertEquals(3, BACKUP_FORMAT)
        val data = parseBackup(json.replace("\"format\": 1", "\"format\": 3"))
        assertEquals(3, JSONObject(data.toJson()).getInt("format"))
        assertEquals(data, parseBackup(data.toJson()))
    }

    @Test fun v3FullRoundTripWithStrengthData() {
        val data = v3SampleBackup()
        val encoded = data.toJson()
        val parsed = parseBackup(encoded)
        assertEquals(3, parsed.format)
        assertEquals(data, parsed)
        assertEquals(201L, parsed.entries.single().planId)
        assertNull(parsed.strengthLogs[0].weightKg)
        assertEquals(22.5f, parsed.strengthLogs[1].weightKg)
        assertFalse(parsed.strengthExercises[0].isTimed)
        assertTrue(parsed.strengthExercises[1].isTimed)
    }

    @Test fun v3WithoutIsTimedDefaultsToFalseAndRejectsNonBoolean() {
        val jsonObj = JSONObject(v3SampleBackup().toJson())
        val exercises = jsonObj.getJSONArray("strengthExercises")
        exercises.getJSONObject(0).remove("isTimed")
        exercises.getJSONObject(1).remove("isTimed")

        val parsed = parseBackup(jsonObj.toString())
        assertTrue(parsed.strengthExercises.none { it.isTimed })

        exercises.getJSONObject(0).put("isTimed", "yes")
        reject(jsonObj.toString())
    }

    @Test fun v1AndV2WithoutStrengthFieldsParseCleanly() {
        for (format in listOf(1, 2)) {
            val parsed = parseBackup(json.replace("\"format\": 1", "\"format\": $format"))
            assertTrue(parsed.strengthExercises.isEmpty())
            assertTrue(parsed.workoutPlans.isEmpty())
            assertTrue(parsed.strengthLogs.isEmpty())
            assertNull(parsed.entries.single().planId)
        }
    }

    @Test fun rejectsInvalidStrengthExerciseFields() {
        val base = v3SampleBackup().toJson()

        // primaryMuscleMask == 0
        val zeroPrimary = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("primaryMuscleMask", 0)
        }
        reject(zeroPrimary.toString())

        // primaryMuscleMask out of ALL_MUSCLE_MASK range
        val overflowPrimary = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("primaryMuscleMask", ALL_MUSCLE_MASK + 1)
        }
        reject(overflowPrimary.toString())

        // secondaryMuscleMask overlaps primaryMuscleMask
        val overlappingSecondary = JSONObject(base).apply {
            val ex = getJSONArray("strengthExercises").getJSONObject(0)
            ex.put("secondaryMuscleMask", ex.getInt("primaryMuscleMask"))
        }
        reject(overlappingSecondary.toString())

        // secondaryMuscleMask out of range
        val overflowSecondary = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("secondaryMuscleMask", 1 shl 14)
        }
        reject(overflowSecondary.toString())

        // invalid bodyRegion
        val badRegion = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("bodyRegion", "neck")
        }
        reject(badRegion.toString())

        // invalid defaultSets / defaultReps
        val zeroSets = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("defaultSets", 0)
        }
        reject(zeroSets.toString())

        val tooManyReps = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("defaultReps", 1000)
        }
        reject(tooManyReps.toString())

        // unknown builtinKey
        val unknownBuiltin = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(0).put("builtinKey", "unknown_lift")
        }
        reject(unknownBuiltin.toString())

        // blank custom exercise names
        val blankCustom = JSONObject(base).apply {
            getJSONArray("strengthExercises").getJSONObject(1)
                .put("nameZh", "   ")
                .put("nameEn", "")
        }
        reject(blankCustom.toString())

        // duplicate strength exercise id
        val dupExercise = JSONObject(base).apply {
            val arr = getJSONArray("strengthExercises")
            arr.put(JSONObject(arr.getJSONObject(1).toString()).put("nameZh", "另一个动作"))
        }
        reject(dupExercise.toString())

        // duplicate builtinKey
        val dupBuiltin = JSONObject(base).apply {
            val arr = getJSONArray("strengthExercises")
            arr.put(JSONObject(arr.getJSONObject(0).toString()).put("id", 999))
        }
        reject(dupBuiltin.toString())
    }

    @Test fun rejectsInvalidWorkoutPlanAndStrengthLogFields() {
        val base = v3SampleBackup().toJson()

        // sets = 0 on strengthLog
        val zeroLogSets = JSONObject(base).apply {
            getJSONArray("strengthLogs").getJSONObject(0).put("sets", 0)
        }
        reject(zeroLogSets.toString())

        // weightKg = 600f (> 500f) on strengthLog
        val heavyLog = JSONObject(base).apply {
            getJSONArray("strengthLogs").getJSONObject(1).put("weightKg", 600.0)
        }
        reject(heavyLog.toString())

        // weightKg = 0.2f (< 0.5f) on plan item
        val tooLightPlanItem = JSONObject(base).apply {
            getJSONArray("workoutPlans").getJSONObject(0)
                .getJSONArray("items").getJSONObject(0)
                .put("defaultWeightKg", 0.2)
        }
        reject(tooLightPlanItem.toString())

        // dangling exerciseId on strengthLog
        val danglingLogEx = JSONObject(base).apply {
            getJSONArray("strengthLogs").getJSONObject(0).put("exerciseId", 9999)
        }
        reject(danglingLogEx.toString())

        // dangling entryId on strengthLog
        val danglingLogEntry = JSONObject(base).apply {
            getJSONArray("strengthLogs").getJSONObject(0).put("entryId", 9999)
        }
        reject(danglingLogEntry.toString())

        // dangling exerciseId on plan item
        val danglingPlanEx = JSONObject(base).apply {
            getJSONArray("workoutPlans").getJSONObject(0)
                .getJSONArray("items").getJSONObject(0)
                .put("exerciseId", 9999)
        }
        reject(danglingPlanEx.toString())

        // dangling planId on entry
        val danglingEntryPlan = JSONObject(base).apply {
            getJSONArray("entries").getJSONObject(0).put("planId", 9999)
        }
        reject(danglingEntryPlan.toString())

        // unknown plan builtinKey
        val badPlanBuiltin = JSONObject(base).apply {
            getJSONArray("workoutPlans").getJSONObject(0).put("builtinKey", "upper")
        }
        reject(badPlanBuiltin.toString())

        // custom plan with blank name
        val blankPlanName = JSONObject(base).apply {
            getJSONArray("workoutPlans").getJSONObject(1).put("name", "  ")
        }
        reject(blankPlanName.toString())
    }

    private fun v3SampleBackup(): BackupData = BackupData(
        format = BACKUP_FORMAT,
        appVersion = "0.14.0",
        exportedAt = 1_700_000_000_000L,
        appearance = mapOf("ambient_background" to "false"),
        types = listOf(BackupType(40, "freeform", null, 0, false, false, null)),
        records = listOf(BackupRecord(20, DayStatus.EXERCISED, "推日", 123)),
        entries = listOf(BackupEntry(50, 20, 40, 45, null, 320, 138, 0, planId = 201)),
        photos = emptyList(),
        strengthExercises = listOf(
            BackupStrengthExercise(
                id = 101,
                builtinKey = "barbell_bench_press",
                nameZh = "杠铃平板卧推",
                nameEn = "Barbell Bench Press",
                bodyRegion = "chest",
                primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
                secondaryMuscleMask = muscleMaskOf(MuscleGroup.TRICEPS),
                defaultSets = 4,
                defaultReps = 10,
                archived = false,
            ),
            BackupStrengthExercise(
                id = 102,
                builtinKey = null,
                nameZh = "器械夹胸",
                nameEn = "Machine Fly",
                bodyRegion = "chest",
                primaryMuscleMask = muscleMaskOf(MuscleGroup.PECTORALS),
                secondaryMuscleMask = 0,
                defaultSets = 3,
                defaultReps = 12,
                archived = false,
                isTimed = true,
            ),
        ),
        workoutPlans = listOf(
            BackupWorkoutPlan(
                id = 201,
                builtinKey = "push",
                name = null,
                sortOrder = 0,
                items = listOf(
                    BackupWorkoutPlanItem(101, 4, 10, 60.5f, 0),
                    BackupWorkoutPlanItem(102, 3, 12, null, 1),
                ),
            ),
            BackupWorkoutPlan(
                id = 202,
                builtinKey = null,
                name = "上肢强化",
                sortOrder = 1,
                items = listOf(BackupWorkoutPlanItem(102, 4, 15, 25f, 0)),
            ),
        ),
        strengthLogs = listOf(
            BackupStrengthLog(entryId = 50, exerciseId = 101, sets = 4, reps = 10, weightKg = null, sortOrder = 0),
            BackupStrengthLog(entryId = 50, exerciseId = 102, sets = 3, reps = 12, weightKg = 22.5f, sortOrder = 1),
        ),
    )

    @Test fun rejectsMetricsAboveLimit() {
        for ((key, over) in listOf("cadenceSpm" to 301, "stepCount" to 200_001, "lapCount" to 10_001,
            "strokeCount" to 100_001, "elevationGainCm" to 2_000_001, "poolLengthCm" to 100_001)) {
            val fixture = JSONObject(json)
            fixture.getJSONArray("entries").getJSONObject(0).put(key, over)
            reject(fixture.toString())
        }
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
