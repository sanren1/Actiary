package com.actiary.data.appearance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.actiary.domain.ChartMetric
import com.actiary.domain.RangeKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val RECORDS_KIND = stringPreferencesKey("records_kind")
private val RECORDS_METRIC = stringPreferencesKey("records_metric")

/** Last-used Records tab choices; unknown stored values fall back to month / duration. */
data class RecordsView(val kind: RangeKind = RangeKind.MONTH, val metric: ChartMetric = ChartMetric.DURATION)

fun Preferences.toRecordsView(): RecordsView {
    val d = RecordsView()
    return RecordsView(enumOr(RECORDS_KIND, d.kind), enumOr(RECORDS_METRIC, d.metric))
}

class RecordsPrefsRepository(private val store: DataStore<Preferences>) {
    val view: Flow<RecordsView> = store.data.map { it.toRecordsView() }

    suspend fun setKind(k: RangeKind) { store.edit { it[RECORDS_KIND] = k.name } }
    suspend fun setMetric(m: ChartMetric) { store.edit { it[RECORDS_METRIC] = m.name } }
}
