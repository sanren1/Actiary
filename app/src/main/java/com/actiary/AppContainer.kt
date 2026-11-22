package com.actiary

import android.content.Context
import com.actiary.data.appearance.AppearanceRepository
import com.actiary.data.appearance.GeneralPrefsRepository
import com.actiary.data.appearance.RecordsPrefsRepository
import com.actiary.data.appearance.appearanceDataStore
import com.actiary.data.backup.BackupRepository
import com.actiary.data.db.ActiaryDatabase
import com.actiary.data.RecordRepository
import com.actiary.data.db.build
import com.actiary.data.photo.PhotoStore
import android.net.Uri
import java.io.File
import java.io.FileNotFoundException

class AppContainer(private val context: Context) {
    val database: ActiaryDatabase by lazy { ActiaryDatabase.build(context) }
    val appearanceRepository = AppearanceRepository(context.applicationContext.appearanceDataStore)
    val recordsPrefs = RecordsPrefsRepository(context.applicationContext.appearanceDataStore)
    val generalPrefs = GeneralPrefsRepository(context.applicationContext.appearanceDataStore)
    val photoStore = PhotoStore(File(context.filesDir, "photos"))
    val backupRepository: BackupRepository by lazy {
        @Suppress("DEPRECATION")
        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        BackupRepository(database, photoStore, appearanceRepository, appVersion, context.cacheDir)
    }
    val recordRepository: RecordRepository by lazy {
        RecordRepository(
            database.recordDao(),
            photoStore,
            { context.contentResolver.openInputStream(Uri.parse(it)) ?: throw FileNotFoundException(it) },
        )
    }

    init {
        photoStore.clearDrafts()
    }
}
