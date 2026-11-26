package com.actiary.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val WEEK_START = stringPreferencesKey("week_start")
private val LAST_CELEBRATED_DAY = longPreferencesKey("last_celebrated_epoch_day")

enum class WeekStart { SYSTEM, MONDAY, SUNDAY }

fun WeekStart.resolve(): DayOfWeek = when (this) {
    WeekStart.SYSTEM -> firstDayOfWeekFromLocale()
    WeekStart.MONDAY -> DayOfWeek.MONDAY
    WeekStart.SUNDAY -> DayOfWeek.SUNDAY
}

fun Preferences.toWeekStart(): WeekStart = enumOr(WEEK_START, WeekStart.SYSTEM)

class GeneralPrefsRepository(private val store: DataStore<Preferences>) {
    val weekStart: Flow<WeekStart> = store.data.map { it.toWeekStart() }

    suspend fun setWeekStart(w: WeekStart) { store.edit { it[WEEK_START] = w.name } }

    suspend fun lastCelebratedEpochDay(): Long? = store.data.first()[LAST_CELEBRATED_DAY]

    suspend fun setLastCelebratedEpochDay(epochDay: Long) {
        store.edit { it[LAST_CELEBRATED_DAY] = epochDay }
    }
}
