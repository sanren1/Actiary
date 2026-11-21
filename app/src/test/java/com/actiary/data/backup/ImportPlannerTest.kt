package com.actiary.data.backup

import com.actiary.data.db.DayStatus
import com.actiary.data.db.ExerciseTypeEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportPlannerTest {
    private val backup = BackupData(
        1, "0.13.0", 0, emptyMap(),
        listOf(
            BackupType(10, null, " 瑜伽 ", 0, false, false, null),
            BackupType(11, "running", null, 1, false, true, null),
            BackupType(12, "yoga", null, 2, true, false, null),
        ),
        listOf(BackupRecord(20, DayStatus.REST, "old", 0), BackupRecord(21, DayStatus.REST, "new", 0)),
        emptyList(), emptyList(),
    )
    private val local = LocalSnapshot(
        listOf(
            ExerciseTypeEntity(1, null, "瑜伽", 0),
            ExerciseTypeEntity(2, "running", "本机名称", 1),
            ExerciseTypeEntity(3, null, "yoga", 2),
        ),
        setOf(20L),
    )

    @Test fun mergeKeepsExistingDayAndAddsMissingDay() {
        val plan = planMerge(local, backup)
        assertEquals(listOf(21L), plan.daysToWrite)
        assertEquals(1, plan.skippedDays)
    }

    @Test fun customTypeMatchedByTrimmedName() {
        assertEquals(TypeTarget.Existing(1), planMerge(local, backup).typeMap[10L])
    }

    @Test fun customTypeNameCaseSensitive() {
        val distinct = backup.types.first().copy(name = "Yoga")
        assertEquals(TypeTarget.Create(distinct), planMerge(local, backup.copy(types = listOf(distinct))).typeMap[10L])
    }

    @Test fun builtinMatchedByKeyAndMissingArchivedTypeIsCreated() {
        val plan = planMerge(local, backup)
        assertEquals(TypeTarget.Existing(2), plan.typeMap[11L])
        assertEquals(TypeTarget.Create(backup.types.last()), plan.typeMap[12L])
    }

    @Test fun overwriteWritesAllDaysAndCreatesCustomType() {
        val plan = planOverwrite(local, backup)
        assertEquals(listOf(20L, 21L), plan.daysToWrite)
        assertEquals(0, plan.skippedDays)
        assertEquals(TypeTarget.Create(backup.types.first()), plan.typeMap[10L])
        assertEquals(TypeTarget.Existing(2), plan.typeMap[11L])
    }
}
