package com.actiary.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.actiary.domain.BuiltinStrengthCatalog
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

class StrengthMigrationTest {

    private data class RecordedSql(
        val sql: String,
        val args: List<Any?> = emptyList(),
    )

    private fun recordingDb(): Pair<SupportSQLiteDatabase, MutableList<RecordedSql>> {
        val statements = mutableListOf<RecordedSql>()
        val proxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args[0] as String
                    val bindArgs = if (args.size > 1 && args[1] is Array<*>) {
                        (args[1] as Array<*>).toList()
                    } else {
                        emptyList()
                    }
                    statements += RecordedSql(sql, bindArgs)
                    null
                }
                else -> error("Unexpected SupportSQLiteDatabase call: ${method.name}")
            }
        } as SupportSQLiteDatabase
        return proxy to statements
    }

    private fun loadSchema(version: Int): JSONObject {
        val relativePath = "schemas/com.actiary.data.db.ActiaryDatabase/$version.json"
        val file = listOf(File(relativePath), File("app/$relativePath")).firstOrNull { it.isFile }
            ?: error("Could not locate Room schema $relativePath from ${File(".").absolutePath}")
        return JSONObject(file.readText())
    }

    private fun normalizeSql(sql: String): String =
        sql.replace(Regex("\\s+"), " ")
            .replace("( ", "(")
            .replace(" )", ")")
            .replace(" ,", ",")
            .trim()

    @Test
    fun schema8JsonMatchesEntitiesAndMigrationSql() {
        val schema = loadSchema(8).getJSONObject("database")
        assertEquals(8, schema.getInt("version"))

        val entitiesArray = schema.getJSONArray("entities")
        val entitiesByTable = buildMap<String, JSONObject> {
            for (i in 0 until entitiesArray.length()) {
                val entity = entitiesArray.getJSONObject(i)
                put(entity.getString("tableName"), entity)
            }
        }
        assertEquals(
            setOf(
                "daily_record",
                "exercise_type",
                "exercise_entry",
                "photo_attachment",
                "strength_exercise",
                "workout_plan",
                "workout_plan_item",
                "strength_log_item",
            ),
            entitiesByTable.keys,
        )

        val (db, statements) = recordingDb()
        ActiaryDatabase.MIGRATION_7_8.migrate(db)
        val ddlStatements = statements
            .filter { it.args.isEmpty() }
            .map { normalizeSql(it.sql) }

        val newTables = listOf(
            "strength_exercise",
            "workout_plan",
            "workout_plan_item",
            "strength_log_item",
        )
        for (table in newTables) {
            val entity = entitiesByTable.getValue(table)
            val expectedCreate = normalizeSql(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            assertTrue(
                "MIGRATION_7_8 must emit CREATE TABLE matching 8.json for $table.\nExpected: $expectedCreate\nActual DDLs: $ddlStatements",
                expectedCreate in ddlStatements,
            )

            val indices = entity.getJSONArray("indices")
            for (i in 0 until indices.length()) {
                val expectedIndex = normalizeSql(
                    indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table),
                )
                assertTrue(
                    "MIGRATION_7_8 must emit index matching 8.json for $table: $expectedIndex",
                    expectedIndex in ddlStatements,
                )
            }
        }

        // Verify exercise_entry gained nullable planId with SET NULL FK and index
        val exerciseEntry = entitiesByTable.getValue("exercise_entry")
        val fields = exerciseEntry.getJSONArray("fields")
        var foundPlanId = false
        for (i in 0 until fields.length()) {
            val f = fields.getJSONObject(i)
            if (f.getString("columnName") == "planId") {
                foundPlanId = true
                assertEquals("INTEGER", f.getString("affinity"))
                assertFalse(f.optBoolean("notNull", false))
            }
        }
        assertTrue("exercise_entry must contain planId column", foundPlanId)

        val fks = exerciseEntry.getJSONArray("foreignKeys")
        var foundPlanFk = false
        for (i in 0 until fks.length()) {
            val fk = fks.getJSONObject(i)
            if (fk.getString("table") == "workout_plan") {
                foundPlanFk = true
                assertEquals("SET NULL", fk.getString("onDelete"))
                assertEquals("NO ACTION", fk.getString("onUpdate"))
                assertEquals("planId", fk.getJSONArray("columns").getString(0))
                assertEquals("id", fk.getJSONArray("referencedColumns").getString(0))
            }
        }
        assertTrue("exercise_entry must have planId -> workout_plan(id) ON DELETE SET NULL FK", foundPlanFk)

        assertTrue(
            "MIGRATION_7_8 must alter exercise_entry to add planId with FK",
            ddlStatements.any {
                it == "ALTER TABLE `exercise_entry` ADD COLUMN `planId` INTEGER REFERENCES `workout_plan`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
            },
        )
        assertTrue(
            "MIGRATION_7_8 must create index_exercise_entry_planId",
            "CREATE INDEX IF NOT EXISTS `index_exercise_entry_planId` ON `exercise_entry` (`planId`)" in ddlStatements,
        )
    }

    @Test
    fun migration7To8SeedsCatalogAndPlansIdempotentlyWithoutTouchingDailyRecords() {
        val (db, statements) = recordingDb()
        ActiaryDatabase.MIGRATION_7_8.migrate(db)

        // Never touches existing daily_record rows (preserving legacy notes)
        assertTrue(statements.none { it.sql.contains("daily_record", ignoreCase = true) })

        val exerciseInserts = statements.filter { it.sql.contains("INTO `strength_exercise`") }
        val planInserts = statements.filter { it.sql.contains("INTO `workout_plan`") }
        val planItemInserts = statements.filter { it.sql.contains("INTO `workout_plan_item`") }

        assertEquals(BuiltinStrengthCatalog.exercises.size, exerciseInserts.size)
        assertEquals(3, planInserts.size)
        assertEquals(15, planItemInserts.size)

        // Exercises and plans use INSERT OR IGNORE; plan items guard with WHERE NOT EXISTS for idempotency
        assertTrue(exerciseInserts.all { it.sql.startsWith("INSERT OR IGNORE INTO `strength_exercise`") })
        assertTrue(planInserts.all { it.sql.startsWith("INSERT OR IGNORE INTO `workout_plan`") })
        assertTrue(planItemInserts.all { it.sql.contains("NOT EXISTS") })

        // Verify special characters (e.g. apostrophe in Captain's Chair Leg Raise) are bound safely via args
        val captainsChair = exerciseInserts.single { it.args.firstOrNull() == "captains_chair_leg_raise" }
        assertEquals("Captain's Chair Leg Raise", captainsChair.args[2])
        assertEquals(0, captainsChair.args[8])
        val plank = exerciseInserts.single { it.args.firstOrNull() == "plank" }
        assertEquals(1, plank.args[8])

        // Verify plan keys and sortOrders
        assertEquals(
            listOf("push" to 0, "pull" to 1, "legs" to 2),
            planInserts.map { it.args[0] to it.args[1] },
        )

        // Verify all plan item exercise keys exist in BuiltinStrengthCatalog
        val exerciseKeys = BuiltinStrengthCatalog.exercises.map { it.builtinKey }.toSet()
        for (insert in planItemInserts) {
            val exerciseKey = insert.args[4] as String
            assertTrue("Plan item references unknown exercise key: $exerciseKey", exerciseKey in exerciseKeys)
        }

        // Calling seedStrengthDefaults a second time emits the exact same idempotent statements
        val (db2, statements2) = recordingDb()
        ActiaryDatabase.seedStrengthDefaults(db2)
        val firstPassCount = statements2.size
        ActiaryDatabase.seedStrengthDefaults(db2)
        assertEquals(firstPassCount * 2, statements2.size)
        assertEquals(statements2.take(firstPassCount), statements2.drop(firstPassCount))
    }

    @Test
    fun dayWithDetailsCompatibilityBridgesEntriesAndStrengthLogs() {
        val record = DailyRecordEntity(epochDay = 100L, status = DayStatus.EXERCISED, note = "胸：卧推", updatedAt = 1L)
        val entry = ExerciseEntryEntity(
            id = 7L,
            epochDay = 100L,
            typeId = 6L,
            durationMinutes = 45,
            distanceMeters = null,
            calories = 300,
            avgHeartRate = 130,
            sortOrder = 0,
        )
        assertNull(entry.planId)

        val compatDay = DayWithDetails(record, listOf(entry), emptyList())
        assertEquals(listOf(entry), compatDay.entries)
        assertEquals(1, compatDay.entriesWithLogs.size)
        assertTrue(compatDay.entriesWithLogs.single().strengthLogs.isEmpty())

        val log = StrengthLogItemEntity(
            id = 1L,
            entryId = 7L,
            exerciseId = 2L,
            sets = 4,
            reps = 10,
            weightKg = 62.5f,
            sortOrder = 0,
        )
        val richDay = DayWithDetails(
            record = record,
            entriesWithLogs = listOf(EntryWithStrengthLogs(entry.copy(planId = 1L), listOf(log))),
            photos = emptyList(),
        )
        assertEquals(1L, richDay.entries.single().planId)
        assertEquals(listOf(log), richDay.entriesWithLogs.single().strengthLogs)
    }

    @Test
    fun schema9JsonAndMigration8To9AddIsTimedColumnAndFlagTimedExercises() {
        val schema = loadSchema(9).getJSONObject("database")
        assertEquals(9, schema.getInt("version"))

        val entitiesArray = schema.getJSONArray("entities")
        var strengthExerciseEntity: JSONObject? = null
        for (i in 0 until entitiesArray.length()) {
            val entity = entitiesArray.getJSONObject(i)
            if (entity.getString("tableName") == "strength_exercise") {
                strengthExerciseEntity = entity
                break
            }
        }
        val fields = requireNotNull(strengthExerciseEntity).getJSONArray("fields")
        var isTimedField: JSONObject? = null
        for (i in 0 until fields.length()) {
            val f = fields.getJSONObject(i)
            if (f.getString("columnName") == "isTimed") {
                isTimedField = f
                break
            }
        }
        requireNotNull(isTimedField) { "strength_exercise in 9.json must contain isTimed" }
        assertEquals("INTEGER", isTimedField.getString("affinity"))
        assertTrue(isTimedField.getBoolean("notNull"))

        val (db, statements) = recordingDb()
        ActiaryDatabase.MIGRATION_8_9.migrate(db)
        val ddls = statements.map { normalizeSql(it.sql) }
        assertEquals(2, ddls.size)
        assertEquals("ALTER TABLE strength_exercise ADD COLUMN isTimed INTEGER NOT NULL DEFAULT 0", ddls[0])
        assertEquals(
            "UPDATE strength_exercise SET isTimed = 1 WHERE builtinKey IN ('plank','side_plank','wall_sit','dead_hang','hollow_body_hold')",
            ddls[1],
        )
    }
}
