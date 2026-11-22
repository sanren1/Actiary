package com.actiary.ui.settings

import android.app.Application
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.actiary.ActiaryApp
import com.actiary.R
import com.actiary.data.backup.ImportMode
import com.actiary.data.backup.LoadedBackup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface BackupState {
    data object Idle : BackupState
    data object Picking : BackupState
    data class Busy(@StringRes val label: Int) : BackupState
    data class ChooseMode(val backup: LoadedBackup) : BackupState
    data class ConfirmCleanup(val count: Int) : BackupState
}

internal class BackupActions(application: Application) : AndroidViewModel(application) {
    private val app = application as ActiaryApp
    private val repository by lazy { app.container.backupRepository }

    var state: BackupState by mutableStateOf(BackupState.Idle)
        private set

    var storageRevision by mutableIntStateOf(0)
        private set

    fun pickDocument(launchPicker: () -> Unit) {
        if (state != BackupState.Idle) return
        state = BackupState.Picking
        try {
            launchPicker()
        } catch (e: ActivityNotFoundException) {
            state = BackupState.Idle
            notify(R.string.backup_picker_unavailable)
        }
    }

    fun exportTo(uri: Uri?) {
        if (!finishPicker() || uri == null) return
        runOperation(R.string.backup_exporting, R.string.backup_export_failed) {
            val summary = withContext(Dispatchers.IO) {
                checkNotNull(app.contentResolver.openOutputStream(uri, "wt")).use { repository.export(it) }
            }
            notify(R.string.backup_exported, days(summary.days), photos(summary.photos))
        }
    }

    fun loadFrom(uri: Uri?) {
        if (!finishPicker() || uri == null) return
        runOperation(R.string.backup_reading, R.string.backup_read_failed) {
            var staged: LoadedBackup? = null
            try {
                withContext(Dispatchers.IO) {
                    checkNotNull(app.contentResolver.openInputStream(uri)).use { staged = repository.load(it) }
                }
                state = BackupState.ChooseMode(checkNotNull(staged))
                staged = null
            } finally {
                staged?.release()
            }
        }
    }

    fun apply(mode: ImportMode) {
        val staged = (state as? BackupState.ChooseMode)?.backup ?: return
        runOperation(R.string.backup_restoring, R.string.backup_restore_failed) {
            val summary = try {
                repository.apply(staged, mode)
            } finally {
                staged.release()
            }
            storageRevision++
            when (mode) {
                ImportMode.MERGE -> notify(R.string.backup_merged, days(summary.written), days(summary.skipped))
                ImportMode.OVERWRITE -> notify(R.string.backup_restored, days(summary.written))
            }
        }
    }

    fun checkOrphanPhotos() {
        if (state != BackupState.Idle) return
        runOperation(R.string.backup_scanning_photos, R.string.backup_scan_failed) {
            val count = repository.orphanPhotoCount()
            if (count == 0) notify(R.string.backup_no_orphans)
            else state = BackupState.ConfirmCleanup(count)
        }
    }

    fun cleanPhotos() {
        if (state !is BackupState.ConfirmCleanup) return
        runOperation(R.string.backup_cleaning_photos, R.string.backup_cleanup_failed) {
            val deleted = repository.deleteOrphanPhotos()
            storageRevision++
            notify(R.string.backup_cleaned, photos(deleted))
        }
    }

    fun dismiss() {
        when (val current = state) {
            is BackupState.ChooseMode -> {
                state = BackupState.Idle
                releaseStaged(current.backup)
            }
            is BackupState.ConfirmCleanup -> state = BackupState.Idle
            else -> Unit
        }
    }

    override fun onCleared() {
        (state as? BackupState.ChooseMode)?.let { releaseStaged(it.backup) }
        super.onCleared()
    }

    private fun finishPicker(): Boolean {
        // A document result can also arrive after Android recreates the process.
        if (state != BackupState.Picking && state != BackupState.Idle) return false
        state = BackupState.Idle
        return true
    }

    private fun runOperation(@StringRes progress: Int, @StringRes error: Int, work: suspend () -> Unit) {
        val busy = BackupState.Busy(progress)
        state = busy
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                work()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notify(error)
            } finally {
                if (state == busy) state = BackupState.Idle
            }
        }
    }

    private fun releaseStaged(backup: LoadedBackup) {
        // onCleared runs after the scope is cancelled. Enter release even in that case.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) { backup.release() }
    }

    private suspend fun LoadedBackup.release() = withContext(NonCancellable + Dispatchers.IO) { close() }

    private fun days(count: Int) = app.resources.getQuantityString(R.plurals.backup_day_count, count, count)
    private fun photos(count: Int) = app.resources.getQuantityString(R.plurals.backup_photo_count, count, count)

    private fun notify(@StringRes message: Int, vararg args: Any) {
        Toast.makeText(app, app.getString(message, *args), Toast.LENGTH_LONG).show()
    }
}
