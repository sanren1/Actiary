package com.actiary.ui.settings

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.actiary.ActiaryApp
import com.actiary.R
import com.actiary.data.RecordStorageUsage
import com.actiary.data.clearDirectoryContents
import com.actiary.data.db.ACTIARY_DATABASE_NAME
import com.actiary.data.measureRecordStorage
import com.actiary.data.backup.ImportMode
import com.actiary.glass.GlassDialog
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.GlassSheet
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
internal fun DataScreen(
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClean: () -> Unit,
    onStorageDetails: () -> Unit,
    storageRefreshKey: Int = 0,
) {
    val app = LocalContext.current.applicationContext as ActiaryApp
    val owner = LocalActivity.current as ViewModelStoreOwner
    val backup = remember(owner) { ViewModelProvider(owner)[BackupActions::class.java] }
    var storage by remember { mutableStateOf<RecordStorageUsage?>(null) }
    LaunchedEffect(backup.storageRevision, storageRefreshKey) {
        storage = withContext(Dispatchers.IO) {
            val photos = app.container.photoStore
            photos.mutations.withLock {
                measureRecordStorage(app.getDatabasePath(ACTIARY_DATABASE_NAME), photos, app.cacheDir)
            }
        }
    }
    SettingsPageLayout(stringResource(R.string.settings_data), onBack) {
        SettingsGroup { rowShape ->
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val t = LocalThemeTokens.current
                val neutral = t.neutrals
                val usage = storage
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.data_storage_usage),
                        modifier = Modifier.weight(1f).semantics { heading() },
                        color = neutral.textPrimary.asColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (usage != null) {
                        Text(
                            Formatter.formatShortFileSize(app, usage.totalBytes),
                            color = neutral.textPrimary.asColor(),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                if (usage != null) {
                    StorageBar(usage)
                    StorageSummaryRow(
                        stringResource(R.string.data_storage_database),
                        Formatter.formatShortFileSize(app, usage.databaseBytes),
                    )
                    StorageSummaryRow(
                        stringResource(R.string.data_storage_photos),
                        stringResource(
                            R.string.data_storage_photos_summary,
                            Formatter.formatShortFileSize(app, usage.photoBytes),
                            usage.photoCount,
                        ),
                    )
                    StorageSummaryRow(
                        stringResource(R.string.data_storage_cache),
                        Formatter.formatShortFileSize(app, usage.cacheBytes),
                    )
                } else {
                    CircularProgressIndicator(color = t.accent.accentAction.asColor())
                }
            }
            SettingsRow(
                stringResource(R.string.data_storage_details),
                Icons.Outlined.Storage,
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                shape = rowShape,
                onClick = onStorageDetails,
            )
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
private fun StorageBar(usage: RecordStorageUsage) {
    val t = LocalThemeTokens.current
    val total = usage.totalBytes.coerceAtLeast(1L).toFloat()
    val photoColor = t.accent.accentAction.asColor()
    val dbColor = t.accent.accentStroke.asColor()
    val cacheColor = t.neutrals.textSecondary.asColor().copy(alpha = 0.55f)
    Canvas(
        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            .background(t.neutrals.surfaceMuted.asColor()),
    ) {
        if (usage.totalBytes <= 0L) return@Canvas
        var x = 0f
        for ((bytes, color) in listOf(
            usage.photoBytes to photoColor,
            usage.databaseBytes to dbColor,
            usage.cacheBytes to cacheColor,
        )) {
            if (bytes <= 0L) continue
            val w = size.width * (bytes / total)
            drawRect(color, Offset(x, 0f), Size(w, size.height))
            x += w
        }
    }
}

@Composable
private fun StorageSummaryRow(label: String, value: String) {
    val neutral = LocalThemeTokens.current.neutrals
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), color = neutral.textPrimary.asColor())
        Text(value, color = neutral.textSecondary.asColor())
    }
}

@Composable
internal fun StorageDetailsSheet(
    onCacheCleared: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as ActiaryApp
    val scope = rememberCoroutineScope()
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val title = stringResource(R.string.data_storage_details)
    var visible by rememberSaveable { mutableStateOf(true) }
    var refreshTick by remember { mutableIntStateOf(0) }
    var clearing by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf<RecordStorageUsage?>(null) }

    LaunchedEffect(refreshTick) {
        usage = withContext(Dispatchers.IO) {
            val photos = app.container.photoStore
            photos.mutations.withLock {
                measureRecordStorage(app.getDatabasePath(ACTIARY_DATABASE_NAME), photos, app.cacheDir)
            }
        }
    }

    fun close(): Boolean {
        visible = false
        return true
    }

    GlassSheet(visible, ::close, onDismiss, title) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(n.textPrimary.asColor(), 20.sp, FontWeight.SemiBold),
                )
                TextButton(
                    onClick = { close() },
                    colors = ButtonDefaults.textButtonColors(contentColor = n.textSecondary.asColor()),
                ) { Text(stringResource(R.string.overlay_close)) }
            }
            val current = usage
            if (current == null) {
                CircularProgressIndicator(color = t.accent.accentAction.asColor())
            } else {
                StorageBar(current)
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(n.surface.asColor().copy(alpha = 0.72f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StorageSummaryRow(stringResource(R.string.data_storage_total), Formatter.formatShortFileSize(app, current.totalBytes))
                    StorageSummaryRow(stringResource(R.string.data_storage_database), Formatter.formatShortFileSize(app, current.databaseBytes))
                    StorageSummaryRow(stringResource(R.string.data_storage_photos), Formatter.formatShortFileSize(app, current.photoBytes))
                    StorageSummaryRow(stringResource(R.string.data_storage_photo_count), current.photoCount.toString())
                    StorageSummaryRow(stringResource(R.string.data_storage_photo_avg), Formatter.formatShortFileSize(app, current.averagePhotoBytes))
                    StorageSummaryRow(stringResource(R.string.data_storage_photo_max), Formatter.formatShortFileSize(app, current.largestPhotoBytes))
                    StorageSummaryRow(stringResource(R.string.data_storage_cache), Formatter.formatShortFileSize(app, current.cacheBytes))
                }
                val canClearCache = !clearing && current.cacheBytes > 0L
                val clearedMessage = stringResource(R.string.data_storage_cache_cleared)
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canClearCache) t.accent.accentAction.asColor() else n.surfaceMuted.asColor())
                        .clickable(enabled = canClearCache, role = Role.Button) {
                            clearing = true
                            scope.launch {
                                withContext(Dispatchers.IO) { clearDirectoryContents(app.cacheDir) }
                                clearing = false
                                refreshTick++
                                onCacheCleared()
                                Toast.makeText(context, clearedMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(R.string.data_storage_clear_cache),
                        style = TextStyle(
                            if (canClearCache) t.accent.onAccentAction.asColor() else n.textSecondary.asColor(),
                            15.sp,
                            FontWeight.Medium,
                        ),
                    )
                }
            }
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
