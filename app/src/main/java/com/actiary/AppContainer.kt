package com.actiary

import android.content.Context
import com.actiary.data.appearance.AppearanceRepository
import com.actiary.data.appearance.appearanceDataStore
import com.actiary.data.db.ActiaryDatabase
import com.actiary.data.db.build

class AppContainer(context: Context) {
    val database: ActiaryDatabase by lazy { ActiaryDatabase.build(context) }
    val appearanceRepository = AppearanceRepository(context.applicationContext.appearanceDataStore)
}
