package com.actiary.ui.settings

import android.text.format.Formatter
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.actiary.ActiaryApp
import com.actiary.R
import com.actiary.data.RecordStorageUsage
import com.actiary.data.db.ACTIARY_DATABASE_NAME
import com.actiary.data.measureRecordStorage
import com.actiary.data.backup.ImportMode
import com.actiary.glass.GlassDialog
import com.actiary.glass.GlassOverlayPanel
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
internal fun DataScreen(onBack: () -> Unit, onExport: () -> Unit, onImport: () -> Unit, onClean: () -> Unit) {
    val app = LocalContext.current.applicationContext as ActiaryApp
    val owner = LocalActivity.current as ViewModelStoreOwner
    val backup = remember(owner) { ViewModelProvider(owner)[BackupActions::class.java] }
    var storage by remember { mutableStateOf<RecordStorageUsage?>(null) }
    LaunchedEffect(backup.storageRevision) {
        storage = withContext(Dispatchers.IO) {
            val photos = app.container.photoStore
            photos.mutations.withLock { measureRecordStorage(app.getDatabasePath(ACTIARY_DATABASE_NAME), photos) }
        }
    }
    SettingsPageLayout(stringResource(R.string.settings_data), onBack) {
        SettingsGroup {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val neutral = LocalThemeTokens.current.neutrals
                Text(
                    stringResource(R.string.data_storage_usage),
                    modifier = Modifier.semantics { heading() },
                    color = neutral.textPrimary.asColor(),
                    fontWeight = FontWeight.SemiBold,
                )
                val usage = storage
                if (usage != null) {
                    for ((label, bytes) in listOf(
                        R.string.data_storage_total to usage.totalBytes,
                        R.string.data_storage_database to usage.databaseBytes,
                        R.string.data_storage_photos to usage.photoBytes,
                    )) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                stringResource(label),
                                modifier = Modifier.weight(1f),
                                color = neutral.textPrimary.asColor(),
                            )
                            Text(Formatter.formatShortFileSize(app, bytes), color = neutral.textSecondary.asColor())
                        }
                    }
                } else {
                    CircularProgressIndicator(color = LocalThemeTokens.current.accent.accentAction.asColor())
                }
            }
        }
        SettingsGroup { rowShape ->
            SettingsRow(
                stringResource(R.string.backup_export), Icons.Outlined.FileDownload, shape = rowShape, onClick = onExport,
            )
            SettingsRow(
                stringResource(R.string.backup_import), Icons.Outlined.FileUpload, shape = rowShape, onClick = onImport,
            )
            SettingsRow(
                stringResource(R.string.backup_clean_photos), Icons.Outlined.CleaningServices, shape = rowShape, onClick = onClean,
            )
        }
    }
}

@Composable
internal fun BackupOverlay(actions: BackupActions) {
    val tokens = LocalThemeTokens.current
    when (val state = actions.state) {
        BackupState.Idle, BackupState.Picking -> Unit
        is BackupState.Busy -> {
            val label = stringResource(state.label)
            GlassOverlayPanel(true, {}, PaddingValues(20.dp), title = label) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = tokens.accent.accentAction.asColor())
                    Text(label, color = tokens.neutrals.textPrimary.asColor())
                }
            }
        }
        is BackupState.ChooseMode -> {
            val title = stringResource(R.string.backup_import)
            GlassOverlayPanel(true, actions::dismiss, PaddingValues(16.dp), title = title) {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        title,
                        modifier = Modifier.semantics { heading() },
                        color = tokens.neutrals.textPrimary.asColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    SettingsRow(
                        stringResource(R.string.backup_merge), Icons.Outlined.LibraryAdd,
                        summary = stringResource(R.string.backup_merge_summary),
                    ) { actions.apply(ImportMode.MERGE) }
                    SettingsRow(
                        stringResource(R.string.backup_overwrite), Icons.Outlined.WarningAmber,
                        summary = stringResource(R.string.backup_overwrite_summary),
                    ) { actions.apply(ImportMode.OVERWRITE) }
                    TextButton(
                        onClick = actions::dismiss,
                        modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = tokens.neutrals.textSecondary.asColor()),
                    ) { Text(stringResource(R.string.record_cancel)) }
                }
            }
        }
        is BackupState.ConfirmCleanup -> GlassDialog(
            visible = true,
            text = stringResource(
                R.string.backup_cleanup_confirm,
                pluralStringResource(R.plurals.backup_orphan_photo_count, state.count, state.count),
            ),
            confirm = stringResource(R.string.backup_delete),
            dismiss = stringResource(R.string.record_cancel),
            onConfirm = actions::cleanPhotos,
            onDismiss = actions::dismiss,
        )
    }
}
