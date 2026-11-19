package com.actiary

import android.content.Context
import com.actiary.data.appearance.AppearanceRepository
import com.actiary.data.appearance.appearanceDataStore
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
    val photoStore = PhotoStore(File(context.filesDir, "photos").apply { mkdirs() })
    val recordRepository: RecordRepository by lazy {
        RecordRepository(
            database.recordDao(),
            photoStore,
            { context.contentResolver.openInputStream(Uri.parse(it)) ?: throw FileNotFoundException(it) },
        )
    }
}
