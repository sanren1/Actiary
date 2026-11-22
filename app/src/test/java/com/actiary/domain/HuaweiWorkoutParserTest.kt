package com.actiary.domain

import com.actiary.data.db.ExerciseTypeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HuaweiWorkoutParserTest {
    @Test fun runningReadsHeadlineDistanceAndTotalCaloriesFromSeparateColumns() {
        val lines = listOf(
            RecognizedLine("个户外跑步", 86, 194, 454, 255),
            RecognizedLine("运动时间", 183, 934, 355, 975),
            RecognizedLine("00:36:51", 183, 1001, 424, 1057),
            RecognizedLine("活动热量", 182, 1161, 357, 1202),
            RecognizedLine("216千卡", 160, 1226, 393, 1292),
            RecognizedLine("6.89公里/小时", 164, 1456, 536, 1518),
            RecognizedLine("平均心率", 182, 1841, 357, 1883),
            RecognizedLine("176次/分钟", 163, 1908, 455, 1973),
            RecognizedLine("2026年9月10日21:04", 379, 538, 884, 586),
            RecognizedLine("4.23-", 439, 649, 879, 834),
            RecognizedLine("公里", 821, 756, 962, 812),
            RecognizedLine("总消耗热量", 734, 934, 954, 976),
            RecognizedLine("263千卡", 708, 998, 948, 1060),
            RecognizedLine("842\"/公里", 714, 1228, 1016, 1289),
            RecognizedLine("平均步频", 734, 1388, 909, 1430),
            RecognizedLine("140步/分钟", 733, 1456, 1015, 1518),
            RecognizedLine("步数", 734, 1616, 819, 1658),
            RecognizedLine("5,187步", 733, 1683, 941, 1746),
            RecognizedLine("累计爬升", 734, 2069, 909, 2110),
            RecognizedLine("2.4米", 732, 2134, 874, 2200),
        )

        assertEquals(
            ParsedWorkout(LocalDate.of(2026, 9, 10), "running", 37, 4230, 263, 176,
                WorkoutMetrics(durationSeconds = 2211, cadenceSpm = 140, stepCount = 5187, elevationGainCm = 240)),
            parseHuaweiWorkout(lines),
        )
    }

    @Test fun swimmingReadsMeterHeadlineWhenUnitIsMissing() {
        val headline = RecognizedLine("1,200.", 444, 650, 948, 847)
        val lines = listOf(
            RecognizedLine("个泳池游泳", 85, 184, 447, 257),
            RecognizedLine("运动时间", 183, 934, 355, 975),
            RecognizedLine("00:57:05", 183, 1006, 443, 1055),
            RecognizedLine("活动热量", 182, 1158, 354, 1202),
            RecognizedLine("286千卡", 159, 1228, 402, 1291),
            RecognizedLine("2026年11月12日 18:37", 373, 538, 888, 586),
            headline,
            RecognizedLine("总消耗热量", 734, 934, 954, 976),
            RecognizedLine("359千卡", 710, 1001, 956, 1067),
            RecognizedLine("泳池长度", 734, 1388, 909, 1429),
            RecognizedLine("25米", 712, 1452, 872, 1518),
            RecognizedLine("划水次数", 182, 1388, 357, 1429),
            RecognizedLine("541次", 182, 1454, 351, 1517),
            RecognizedLine("趟数", 181, 1615, 268, 1657),
            RecognizedLine("48", 181, 1684, 257, 1744),
            RecognizedLine("平均心率", 734, 1615, 909, 1657),
            RecognizedLine("128次/分钟", 714, 1684, 1012, 1744),
        )

        assertEquals(
            ParsedWorkout(LocalDate.of(2026, 11, 12), "swimming", 57, 1200, 359, 128,
                WorkoutMetrics(durationSeconds = 3425, poolLengthCm = 2500, lapCount = 48, strokeCount = 541)),
            parseHuaweiWorkout(lines),
        )
        assertEquals(
            ParsedWorkout(LocalDate.of(2026, 11, 12), "swimming", 57, null, 359, 128,
                WorkoutMetrics(durationSeconds = 3425, poolLengthCm = 2500, lapCount = 48, strokeCount = 541)),
            parseHuaweiWorkout(lines - headline),
        )
    }

    @Test fun freeformReadsTotalCaloriesBesideLabel() {
        val lines = listOf(
            RecognizedLine("个自由训练", 86, 194, 455, 255),
            RecognizedLine("活动热量", 182, 934, 356, 975),
            RecognizedLine("总消耗热量", 254, 752, 557, 813),
            RecognizedLine("106千卡", 159, 991, 398, 1063),
            RecognizedLine("平均心率", 181, 1159, 355, 1202),
            RecognizedLine("2026年11月19日21:28", 372, 539, 890, 586),
            RecognizedLine("124次/分钟", 161, 1226, 459, 1291),
            RecognizedLine("132-", 568, 653, 913, 831),
            RecognizedLine("干卡", 882, 760, 981, 810),
            RecognizedLine("运动时间", 733, 931, 907, 976),
            RecognizedLine("00:20:04", 734, 1002, 997, 1058),
        )

        assertEquals(
            ParsedWorkout(LocalDate.of(2026, 11, 19), "freeform", 20, null, 132, 124, WorkoutMetrics(durationSeconds = 1204)),
            parseHuaweiWorkout(lines),
        )
    }

    @Test fun missingDateAndTypeLeaveRecognizedMetricAvailable() {
        assertEquals(
            ParsedWorkout(null, null, 20, null, null, null, WorkoutMetrics(durationSeconds = 1204)),
            parseHuaweiWorkout(listOf(
                RecognizedLine("运动时间", 733, 931, 907, 976),
                RecognizedLine("00:20:04", 734, 1002, 997, 1058),
            )),
        )
    }

    @Test fun runningKeepsSecondsForShortSessionsAndIgnoresAdjacentColumns() {
        val result = parseHuaweiWorkout(listOf(
            RecognizedLine("户外跑步", 86, 194, 454, 255),
            RecognizedLine("运动时间", 183, 934, 355, 975),
            RecognizedLine("00:00:19", 183, 1001, 424, 1057),
            RecognizedLine("步数", 734, 1616, 819, 1658),
            RecognizedLine("820步", 182, 1683, 340, 1746),
            RecognizedLine("平均步频", 734, 1388, 909, 1430),
            RecognizedLine("140次/分钟", 733, 1456, 1015, 1518),
            RecognizedLine("划水次数", 182, 1840, 357, 1882),
            RecognizedLine("541次", 182, 1910, 351, 1973),
            RecognizedLine("累计爬升", 734, 2069, 909, 2110),
            RecognizedLine("1.25", 732, 2134, 827, 2200),
            RecognizedLine("米", 829, 2156, 874, 2199),
        ))

        assertEquals(0, result?.durationMinutes)
        assertEquals(WorkoutMetrics(durationSeconds = 19, elevationGainCm = 125), result?.metrics)
    }

    @Test fun replacingRoundedTimeDoesNotKeepAnUnrelatedExactDuration() {
        val date = LocalDate.of(2026, 11, 20)
        val type = ExerciseTypeEntity(8, "running", null, 0, hasDistance = true)
        val oldEntry = DraftEntry(8, durationMinutes = 30, id = 1,
            metrics = WorkoutMetrics(durationSeconds = 1801, stepCount = 7000))
        val draft = DayDraft.from(date, null).copy(entries = listOf(oldEntry))

        val update = draft.withRecognizedWorkout(ParsedWorkout(null, "running", 31, null, null, null), type)
        assertEquals(31, update.entries.single().durationMinutes)
        assertEquals(WorkoutMetrics(stepCount = 7000), update.entries.single().metrics)
        assertEquals(oldEntry, draft.withRecognizedWorkout(ParsedWorkout(null, "running", null, null, null, null), type).entries.single())
    }

    @Test fun dateAndStatusBarTimeAloneAreNotAWorkout() {
        assertNull(parseHuaweiWorkout(listOf(
            RecognizedLine("01:23:45", 32, 43, 198, 85),
            RecognizedLine("2026年11月19日21:28", 372, 539, 890, 586),
        )))
    }
}
