package com.actiary.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.actiary.domain.ChartMetric
import com.actiary.domain.RangeKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordsPrefsTest {
    // In-memory store, same reason as AppearancePreferencesTest.
    private fun repo() = RecordsPrefsRepository(object : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val next = transform(state.value)
            state.value = next
            return next
        }
    })

    @Test fun emptyIsMonthDuration() {
        assertEquals(RecordsView(RangeKind.MONTH, ChartMetric.DURATION), emptyPreferences().toRecordsView())
    }

    @Test fun unknownValuesFallBack() {
        val p = mutablePreferencesOf(
            stringPreferencesKey("records_kind") to "DECADE",
            stringPreferencesKey("records_metric") to "STEPS",
        )
        assertEquals(RecordsView(), p.toRecordsView())
    }

    @Test fun roundTrip() = runBlocking {
        val r = repo()
        r.setKind(RangeKind.YEAR)
        r.setMetric(ChartMetric.CALORIES)
        assertEquals(RecordsView(RangeKind.YEAR, ChartMetric.CALORIES), r.view.first())
        r.setKind(RangeKind.WEEK)
        assertEquals(RecordsView(RangeKind.WEEK, ChartMetric.CALORIES), r.view.first())
    }
}
