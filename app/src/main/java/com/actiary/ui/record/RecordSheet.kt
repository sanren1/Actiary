package com.actiary.ui.record

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.db.DayStatus
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.photo.CameraCapture
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DayDraft
import com.actiary.domain.DraftEntry
import com.actiary.domain.DraftPhoto
import com.actiary.domain.WorkoutMetric
import com.actiary.domain.WorkoutMetrics
import com.actiary.domain.WorkoutScreenshot
import com.actiary.domain.effectiveWorkoutMetricMask
import com.actiary.domain.withRecognizedWorkout
import com.actiary.glass.GlassDialog
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.glass.GlassSheet
import com.actiary.glass.overlayPresent
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.common.isLength
import com.actiary.ui.common.labelId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Encodes a (nullable) draft as primitives so it survives rotation / process death. */
@Suppress("UNCHECKED_CAST")
internal val DraftSaver = Saver<DayDraft?, Any>(
    save = { d ->
        if (d == null) arrayListOf<Any>() else arrayListOf<Any>(
            d.date.toEpochDay(), d.status.name, d.note,
            ArrayList(d.entries.map { it.typeId }),
            ArrayList(d.entries.map { it.durationMinutes ?: -1 }),
            ArrayList(d.entries.map { it.distanceMeters ?: -1 }),
            ArrayList(d.photos.map {
                when (it) {
                    is DraftPhoto.Saved -> "s:${it.fileName}"
                    is DraftPhoto.Picked -> "p:${it.uri}"
                    is DraftPhoto.Staged -> "d:${it.fileName}"
                }
            }),
            ArrayList(d.entries.map { it.calories ?: -1 }),
            ArrayList(d.entries.map { it.avgHeartRate ?: -1 }),
            ArrayList(d.entries.map { it.id }),
            ArrayList(d.entries.map { entry ->
                val metrics = entry.metrics
                arrayListOf(
                    metrics.durationSeconds ?: -1,
                    metrics.cadenceSpm ?: -1,
                    metrics.stepCount ?: -1,
                    metrics.elevationGainCm ?: -1,
                    metrics.poolLengthCm ?: -1,
                    metrics.lapCount ?: -1,
                    metrics.strokeCount ?: -1,
                )
            }),
        )
    },
    restore = { v ->
        val l = v as List<Any>
        if (l.isEmpty()) null else {
            val ids = l[3] as List<Long>
            val dur = l[4] as List<Int>
            val dist = l[5] as List<Int>
            val cal = l.getOrElse(7) { emptyList<Int>() } as List<Int>
            val hr = l.getOrElse(8) { emptyList<Int>() } as List<Int>
            val entryIds = l.getOrElse(9) { emptyList<Long>() } as List<Long>
            val metrics = l.getOrElse(10) { emptyList<List<Int>>() } as List<List<Int>>
            DayDraft(
                date = LocalDate.ofEpochDay(l[0] as Long),
                status = DayStatus.valueOf(l[1] as String),
                entries = ids.mapIndexed { i, id ->
                    val values = metrics.getOrNull(i)
                    fun metric(index: Int) = values?.getOrNull(index)?.takeIf { it >= 0 }
                    DraftEntry(
                        id,
                        dur[i].takeIf { it >= 0 },
                        dist[i].takeIf { it >= 0 },
                        cal.getOrElse(i) { -1 }.takeIf { it >= 0 },
                        hr.getOrElse(i) { -1 }.takeIf { it >= 0 },
                        entryIds.getOrElse(i) { -i.toLong() - 1 },
                        WorkoutMetrics(metric(0), metric(1), metric(2), metric(3), metric(4), metric(5), metric(6)),
                    )
                },
                note = l[2] as String,
                photos = (l[6] as List<String>).map {
                    when {
                        it.startsWith("s:") -> DraftPhoto.Saved(it.drop(2))
                        it.startsWith("d:") -> DraftPhoto.Staged(it.drop(2))
                        else -> DraftPhoto.Picked(it.drop(2))
                    }
                },
            )
        }
    },
)

private val InvalidEntryIdsSaver = Saver<Set<Long>, List<Long>>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private val PhotoPathsSaver = Saver<List<String>, ArrayList<String>>(
    save = { ArrayList(it) },
    restore = { it.toList() },
)

@Composable
internal fun RecordSheet(
    date: LocalDate,
    firstDayOfWeek: DayOfWeek,
    repository: RecordRepository,
    photoStore: PhotoStore,
    onDismiss: () -> Unit,
    onSaved: (LocalDate) -> Unit = {},
    screenshot: WorkoutScreenshot? = null,
) {
    val t = LocalThemeTokens.current
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf<DayDraft?>(null) }
    var original by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf<DayDraft?>(null) }
    var existing by rememberSaveable { mutableStateOf(false) }
    var visible by rememberSaveable { mutableStateOf(true) } // false = sheet sliding out; onDismiss fires once it is gone
    var saving by remember { mutableStateOf(false) } // not saved: the coroutine does not survive recreation
    var failed by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }
    var pendingDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var showRest by rememberSaveable { mutableStateOf(false) }
    var showClear by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var viewing by rememberSaveable { mutableStateOf<Int?>(null) }
    var showInvalid by rememberSaveable { mutableStateOf(false) }
    var waitingForType by rememberSaveable { mutableStateOf(false) }
    var invalidEntryIds by rememberSaveable(stateSaver = InvalidEntryIdsSaver) { mutableStateOf(emptySet<Long>()) }
    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var capturePaths by rememberSaveable(stateSaver = PhotoPathsSaver) { mutableStateOf(emptyList<String>()) }
    var pendingPickerUris by rememberSaveable(stateSaver = PhotoPathsSaver) { mutableStateOf(emptyList<String>()) }
    var photoImportFailed by rememberSaveable { mutableStateOf(false) }
    var changingDate by remember { mutableStateOf(false) }
    val importingPhotos = pendingPickerUris.isNotEmpty()
    fun isBusy() = saving || changingDate || pendingPickerUris.isNotEmpty()
    val busy = isBusy()
    val types by repository.types.collectAsState(emptyList())
    val allTypes by repository.allTypes.collectAsState(emptyList())

    fun cleanupCaptures() {
        capturePaths.forEach { CameraCapture.cleanup(File(it)) }
        capturePaths = emptyList()
    }
    fun cleanupStagedPhotos(photos: List<DraftPhoto>) {
        photos.filterIsInstance<DraftPhoto.Staged>().forEach { photoStore.stagedFile(it.fileName).delete() }
    }

    suspend fun load(d: LocalDate) {
        try {
            val day = repository.observeDay(d).first()
            val base = DayDraft.from(d, day)
            val detectedType = screenshot?.workout?.typeKey?.let { key ->
                repository.types.first().firstOrNull { it.builtinKey == key }
            }
            val next = if (screenshot == null) base else {
                // Keep all existing photos in the draft. A full day needs a removal before saving.
                val imported = base.markExercised().copy(photos = base.photos + DraftPhoto.Picked(screenshot.uri))
                if (detectedType == null) imported else imported.withRecognizedWorkout(screenshot.workout, detectedType)
            }
            cleanupStagedPhotos(draft?.photos.orEmpty())
            cleanupCaptures()
            original = base
            waitingForType = screenshot != null && detectedType == null
            draft = next
            existing = day != null
            photoImportFailed = false
            failed = false
            invalidEntryIds = emptySet()
            showInvalid = false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failed = true // sheet stays open; nothing editable without a loaded draft
        }
    }
    LaunchedEffect(Unit) { if (draft == null) load(date) }
    fun changeDate(date: LocalDate) {
        if (isBusy() || !visible) return
        changingDate = true
        scope.launch {
            try {
                load(date)
            } finally {
                changingDate = false
            }
        }
    }

    val current = draft
    val dirty = current != null && original?.let { current.isChangedFrom(it) } == true
    val photo = viewing?.let { current?.photos?.getOrNull(it) }
    LaunchedEffect(viewing, photo) { if (viewing != null && photo == null) viewing = null }
    val datePickerVisibility = rememberOverlayVisibility(showDatePicker)
    val photoVisibility = rememberOverlayVisibility(photo != null)
    val discardVisibility = rememberOverlayVisibility(showDiscard)
    val restVisibility = rememberOverlayVisibility(showRest)
    val clearVisibility = rememberOverlayVisibility(showClear)
    val covered = datePickerVisibility.overlayPresent || photoVisibility.overlayPresent ||
        discardVisibility.overlayPresent || restVisibility.overlayPresent || clearVisibility.overlayPresent
    fun close() { visible = false }
    fun requestClose(): Boolean {
        if (isBusy() || !visible) return false
        if (dirty) {
            showDiscard = true
            return false
        }
        close()
        return true
    }
    fun update(f: (DayDraft) -> DayDraft) {
        showInvalid = false
        val updated = draft?.let(f) ?: return
        invalidEntryIds = invalidEntryIds.intersect(updated.entries.map { it.id }.toSet())
        draft = updated
    }

    LaunchedEffect(pendingPickerUris) {
        if (pendingPickerUris.isEmpty()) return@LaunchedEffect
        try {
            val photos = repository.stagePickedPhotos(pendingPickerUris)
            update { it.copy(photos = it.photos + photos) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            photoImportFailed = true
        }
        pendingPickerUris = emptyList()
    }
    fun importPhotos(uris: List<String>) {
        val selected = draft ?: return
        if (isBusy() || !visible) return
        val available = (DayDraft.PHOTO_LIMIT - selected.photos.size).coerceAtLeast(0)
        val accepted = uris.take(available)
        if (accepted.isNotEmpty()) {
            photoImportFailed = false
            pendingPickerUris = accepted
        }
    }

    val remaining = DayDraft.PHOTO_LIMIT - (current?.photos?.size ?: 0)
    // PickMultipleVisualMedia requires maxItems >= 2; the single picker covers the last slot.
    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxOf(2, remaining)),
    ) { uris -> importPhotos(uris.map { it.toString() }) }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) importPhotos(listOf(uri.toString()))
    }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCapturePath
        pendingCapturePath = null
        if (path != null) {
            if (ok) {
                val uri = FileProvider.getUriForFile(context, CameraCapture.AUTHORITY, File(path))
                update { it.addPhotos(listOf(uri.toString())) }
            } else {
                CameraCapture.cleanup(File(path))
                capturePaths = capturePaths - path
            }
        }
    }
    fun launchCapture() {
        if (!isBusy() && remaining >= 1) {
            val file = CameraCapture.createTempFile(context.cacheDir)
            pendingCapturePath = file.absolutePath
            capturePaths = capturePaths + file.absolutePath
            cameraLauncher.launch(FileProvider.getUriForFile(context, CameraCapture.AUTHORITY, file))
        }
    }

    fun launchOp(work: suspend () -> Unit, after: () -> Unit) {
        if (isBusy() || !visible) return
        saving = true
        failed = false
        scope.launch {
            try {
                // copy -> commit -> delete must finish even if the activity is recreated.
                withContext(NonCancellable) { work() }
                after()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                failed = true
            } finally {
                saving = false
            }
        }
    }

    GlassSheet(
        visible = visible,
        onRequestClose = ::requestClose,
        onExited = {
            cleanupStagedPhotos(draft?.photos.orEmpty())
            cleanupCaptures()
            onDismiss()
        },
        title = stringResource(R.string.record_title),
        covered = covered,
    ) {
        val primary = t.neutrals.textPrimary.asColor()
        val secondary = t.neutrals.textSecondary.asColor()
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).clickable(enabled = !busy, role = Role.Button) { requestClose() }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { BasicText(stringResource(R.string.record_cancel), style = TextStyle(primary, 16.sp)) }
                BasicText(
                    stringResource(R.string.record_title), Modifier.weight(1f),
                    style = TextStyle(primary, 17.sp, FontWeight.SemiBold, textAlign = TextAlign.Center),
                )
                Spacer(Modifier.width(64.dp)) // balances Cancel so the title stays centered
            }
            if (current != null) Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surfaceMuted.asColor())
                        .clickable(enabled = !busy, role = Role.Button) { showDatePicker = true }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(stringResource(R.string.record_date), Modifier.weight(1f), style = TextStyle(secondary, 15.sp))
                    BasicText(
                        current.date.format(DateTimeFormatter.ofPattern(stringResource(R.string.day_title_pattern), locale)),
                        style = TextStyle(primary, 15.sp, FontWeight.Medium),
                    )
                }
                GlassSegmentedPicker(
                    listOf(stringResource(R.string.record_status_workout), stringResource(R.string.record_status_rest)),
                    if (current.status == DayStatus.REST) 1 else 0,
                    { i ->
                        if (i == 0) update { it.markExercised() }
                        else if (current.status != DayStatus.REST) {
                            if (current.needsRestConfirm) showRest = true else update { it.markRest() }
                        }
                    },
                )
                val rest = current.status == DayStatus.REST
                Section(R.string.record_activities) {
                    if (waitingForType && !rest) {
                        BasicText(
                            stringResource(R.string.record_import_select_type),
                            style = TextStyle(primary, 14.sp, FontWeight.Medium),
                        )
                    }
                    val entryCounts = current.entries.groupingBy { it.typeId }.eachCount()
                    Column(Modifier.alpha(if (rest) 0.4f else 1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.filter { !it.archived }.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { type ->
                                    val count = entryCounts[type.id] ?: 0
                                    val enabled = !rest && (count < 2 || waitingForType)
                                    TypeCell(type, count > 0, enabled, Modifier.weight(1f).alpha(if (!enabled && count > 1) 0.55f else 1f)) {
                                        if (waitingForType && screenshot != null) {
                                            update { it.withRecognizedWorkout(screenshot.workout, type) }
                                            waitingForType = false
                                        } else {
                                            update { it.toggleType(type.id) }
                                        }
                                    }
                                }
                                repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                    if (!waitingForType) {
                        current.entries.forEach { entry ->
                            val type = allTypes.firstOrNull { it.id == entry.typeId } ?: return@forEach
                            key(current.date.toEpochDay(), entry.id, entry.typeId) {
                                EntryFields(
                                    type = type,
                                    entry = entry,
                                    onChange = { e -> update { it.withEntry(e) } },
                                    onRemove = if (entryCounts.getValue(type.id) > 1) {
                                        { update { it.removeEntry(entry.id) } }
                                    } else null,
                                    onValidity = { valid ->
                                        invalidEntryIds = if (valid) invalidEntryIds - entry.id else invalidEntryIds + entry.id
                                    },
                                )
                            }
                        }
                    }
                }
                Section(R.string.record_note) {
                    BasicTextField(
                        value = current.note,
                        onValueChange = { v -> update { it.withNote(v) } },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(12.dp))
                            .background(t.neutrals.surfaceMuted.asColor()).padding(12.dp),
                        textStyle = TextStyle(primary, 15.sp),
                        cursorBrush = SolidColor(t.accent.accentAction.asColor()),
                    )
                    BasicText(
                        stringResource(R.string.record_counter, current.note.length, DayDraft.NOTE_LIMIT),
                        Modifier.fillMaxWidth(), style = TextStyle(secondary, 12.sp, textAlign = TextAlign.End),
                    )
                }
                Section(R.string.record_photos, counter = stringResource(R.string.record_counter, current.photos.size, DayDraft.PHOTO_LIMIT)) {
                    PhotoRow(
                        current.photos, photoStore,
                        onView = { viewing = it },
                        onRemove = { i ->
                            if (!isBusy()) {
                                val removed = draft?.photos?.getOrNull(i)
                                update { it.removePhoto(i) }
                                if (removed is DraftPhoto.Staged) photoStore.stagedFile(removed.fileName).delete()
                            }
                        },
                        onAdd = {
                            if (!isBusy()) {
                                if (remaining >= 2) multiPicker.launch(imageOnly) else singlePicker.launch(imageOnly)
                            }
                        },
                        onCapture = { launchCapture() },
                        onMove = { from, to ->
                            if (!isBusy()) update { it.movePhoto(from, to) }
                        },
                        editingEnabled = !busy,
                    )
                }
                if (existing) {
                    BasicText(
                        stringResource(R.string.record_clear),
                        Modifier.heightIn(min = 48.dp).clickable(enabled = !busy, role = Role.Button) { showClear = true },
                        style = TextStyle(t.accent.accentStroke.asColor(), 15.sp, FontWeight.Medium),
                    )
                }
            }
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (failed) {
                    BasicText(
                        stringResource(R.string.record_save_failed),
                        Modifier.padding(bottom = 8.dp),
                        style = TextStyle(primary, 14.sp, FontWeight.Medium),
                    )
                }
                if (importingPhotos || photoImportFailed) {
                    BasicText(
                        stringResource(if (importingPhotos) R.string.record_photo_importing else R.string.record_photo_import_failed),
                        Modifier.padding(bottom = 8.dp),
                        style = TextStyle(primary, 14.sp, FontWeight.Medium),
                    )
                }
                if (showInvalid) {
                    BasicText(
                        stringResource(R.string.record_invalid_value),
                        Modifier.padding(bottom = 8.dp),
                        style = TextStyle(t.accent.accentStroke.asColor(), 14.sp, FontWeight.Medium),
                    )
                }
                if (remaining < 0) {
                    BasicText(
                        stringResource(R.string.record_import_photo_limit),
                        Modifier.padding(bottom = 8.dp),
                        style = TextStyle(primary, 14.sp, FontWeight.Medium),
                    )
                }
                val canSave = current != null && !busy && visible && remaining >= 0 &&
                    (!waitingForType || current.status == DayStatus.REST)
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).alpha(if (canSave) 1f else 0.5f).clip(RoundedCornerShape(50))
                        .background(t.accent.accentAction.asColor())
                        .clickable(enabled = canSave, role = Role.Button) {
                            current?.let { d ->
                                if (d.entries.any { it.id in invalidEntryIds }) {
                                    showInvalid = true
                                    return@let
                                }
                                showInvalid = false
                                launchOp({ repository.save(d) }) {
                                    onSaved(d.date)
                                    close()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) { BasicText(stringResource(R.string.record_save), style = TextStyle(t.accent.onAccentAction.asColor(), 16.sp, FontWeight.SemiBold)) }
            }
        }
    }

    DatePickerDialog(
        visible = showDatePicker,
        selected = current?.date ?: date,
        firstDayOfWeek = firstDayOfWeek,
        onSelect = { d ->
            showDatePicker = false
            if (d != current?.date) {
                if (dirty) { pendingDate = d.toEpochDay(); showDiscard = true } else changeDate(d)
            }
        },
        onDismiss = { showDatePicker = false },
        visibility = datePickerVisibility,
    )
    PhotoViewer(photo, photoStore, stringResource(R.string.day_photo, (viewing ?: 0) + 1), photoVisibility) {
        viewing = null
    }
    GlassDialog(
        visible = showDiscard,
        text = stringResource(R.string.record_discard_title), confirm = stringResource(R.string.record_discard), dismiss = stringResource(R.string.record_keep_editing),
        onConfirm = {
            showDiscard = false
            val p = pendingDate
            pendingDate = null
            if (p != null) changeDate(LocalDate.ofEpochDay(p)) else close()
        },
        onDismiss = { showDiscard = false; pendingDate = null },
        visibility = discardVisibility,
    )
    GlassDialog(
        visible = showRest,
        text = stringResource(R.string.record_rest_confirm), confirm = stringResource(R.string.record_status_rest), dismiss = stringResource(R.string.record_cancel),
        onConfirm = { showRest = false; update { it.markRest() } },
        onDismiss = { showRest = false },
        visibility = restVisibility,
    )
    GlassDialog(
        visible = showClear,
        text = stringResource(R.string.record_clear_confirm), confirm = stringResource(R.string.record_clear), dismiss = stringResource(R.string.record_cancel),
        onConfirm = {
            showClear = false
            val d = current?.date ?: date
            launchOp({ repository.clearDay(d) }) {
                onSaved(d)
                close()
            }
        },
        onDismiss = { showClear = false },
        visibility = clearVisibility,
    )
}

@Composable
private fun Section(title: Int, counter: String? = null, content: @Composable () -> Unit) {
    val t = LocalThemeTokens.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            BasicText(stringResource(title), Modifier.weight(1f), style = TextStyle(t.neutrals.textSecondary.asColor(), 13.sp, FontWeight.Medium))
            if (counter != null) BasicText(counter, style = TextStyle(t.neutrals.textSecondary.asColor(), 12.sp))
        }
        content()
    }
}

@Composable
private fun TypeCell(type: ExerciseTypeEntity, selected: Boolean, enabled: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.heightIn(min = 72.dp).clip(shape)
            .background(if (selected) t.accent.accentSubtle.asColor() else t.neutrals.surfaceMuted.asColor())
            .then(if (selected) Modifier.border(1.dp, t.accent.accentStroke.asColor(), shape) else Modifier)
            .toggleable(selected, enabled = enabled, role = Role.Checkbox) { onToggle() }
            .padding(4.dp),
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(exerciseTypeIcon(type), null, Modifier.size(24.dp), tint = t.neutrals.textPrimary.asColor())
            BasicText(exerciseTypeLabel(type), style = TextStyle(t.neutrals.textPrimary.asColor(), 12.sp, textAlign = TextAlign.Center), maxLines = 1)
        }
        if (selected) {
            Icon(Icons.Outlined.Check, null, Modifier.align(Alignment.TopEnd).size(14.dp), tint = t.accent.accentStroke.asColor())
        }
    }
}

private fun entryInputInvalid(
    distance: String,
    hasDistance: Boolean,
    swimming: Boolean,
    calories: String,
    heartRate: String,
): Boolean =
    (hasDistance && distance.isNotEmpty() && if (swimming) parseMeters(distance) == null else parseKmToMeters(distance) == null) ||
        (calories.isNotEmpty() && parseCalories(calories) == null) ||
        (heartRate.isNotEmpty() && parseHeartRate(heartRate) == null)

@Composable
private fun EntryFields(
    type: ExerciseTypeEntity,
    entry: DraftEntry,
    onChange: (DraftEntry) -> Unit,
    onRemove: (() -> Unit)?,
    onValidity: (Boolean) -> Unit,
) {
    val t = LocalThemeTokens.current
    val hasDistance = type.hasDistance
    val swimming = type.builtinKey == "swimming"
    var minutes by rememberSaveable { mutableStateOf(formatMinutes(entry.durationMinutes)) }
    var distance by rememberSaveable {
        mutableStateOf(formatDistance(entry.distanceMeters, type.builtinKey, hasDistance))
    }
    var calories by rememberSaveable { mutableStateOf(formatCalories(entry.calories)) }
    var heartRate by rememberSaveable { mutableStateOf(formatHeartRate(entry.avgHeartRate)) }

    val metricMask = effectiveWorkoutMetricMask(type.builtinKey, type.extraMetricMask)
    var metricInputs by rememberSaveable {
        mutableStateOf(WorkoutMetric.entries.map { metric ->
            val value = metric.value(entry.metrics)
            if (metric.isLength) formatCentimeters(value) else value?.toString().orEmpty()
        })
    }
    val visibleMetrics = WorkoutMetric.entries.filter { metric ->
        (metricMask and metric.bit) != 0 || metricInputs[metric.ordinal].isNotEmpty()
    }

    fun report() {
        val invalidMetric = WorkoutMetric.entries.any { metric ->
            val text = metricInputs[metric.ordinal]
            text.isNotEmpty() && parseWorkoutMetric(metric, text) == null
        }
        onValidity(!invalidMetric && !entryInputInvalid(distance, hasDistance, swimming, calories, heartRate))
    }
    LaunchedEffect(Unit) { report() }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                exerciseTypeLabel(type), Modifier.width(72.dp).padding(bottom = 12.dp),
                style = TextStyle(t.neutrals.textPrimary.asColor(), 14.sp, FontWeight.Medium), maxLines = 1,
            )
            val durationLabel = stringResource(R.string.record_duration)
            val exactTime = entry.metrics.durationSeconds?.let { formatWorkoutClock(it.toLong()) }
            NumberField(
                if (exactTime == null) durationLabel else stringResource(R.string.workout_duration_exact, durationLabel, exactTime),
                minutes, Modifier.weight(1f), KeyboardType.Number,
            ) {
                val edited = sanitizeMinutes(it)
                if (edited != minutes) {
                    minutes = edited
                    onChange(entry.copy(durationMinutes = parseMinutes(minutes), metrics = entry.metrics.copy(durationSeconds = null)))
                }
            }
            if (hasDistance) {
                NumberField(
                    stringResource(if (swimming) R.string.record_distance_meters else R.string.record_distance),
                    distance, Modifier.weight(1f),
                    if (swimming) KeyboardType.Number else KeyboardType.Decimal,
                ) {
                    distance = if (swimming) sanitizeMinutes(it) else sanitizeKm(it)
                    onChange(entry.copy(distanceMeters = if (swimming) parseMeters(distance) else parseKmToMeters(distance)))
                    report()
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(72.dp)) { // aligns this row's fields with the row above
                if (onRemove != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.record_remove_entry), tint = t.neutrals.textPrimary.asColor())
                    }
                }
            }
            NumberField(stringResource(R.string.record_calories), calories, Modifier.weight(1f), KeyboardType.Number) {
                calories = sanitizeCalories(it)
                onChange(entry.copy(calories = parseCalories(calories)))
                report()
            }
            NumberField(stringResource(R.string.record_heart_rate), heartRate, Modifier.weight(1f), KeyboardType.Number) {
                heartRate = sanitizeHeartRate(it)
                onChange(entry.copy(avgHeartRate = parseHeartRate(heartRate)))
                report()
            }
        }
        visibleMetrics.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { metric ->
                    key(metric) {
                        val label = stringResource(metric.labelId)
                        val labelWithUnit = when {
                            metric.isLength -> stringResource(R.string.workout_metric_label_meters, label)
                            metric == WorkoutMetric.CADENCE -> stringResource(R.string.workout_metric_label_steps_per_min, label)
                            else -> label
                        }
                        NumberField(
                            labelWithUnit, metricInputs[metric.ordinal], Modifier.weight(1f),
                            if (metric.isLength) KeyboardType.Decimal else KeyboardType.Number,
                        ) { input ->
                            val text = input.filter { char -> char in '0'..'9' || (metric.isLength && char in ".,") }
                            metricInputs = metricInputs.toMutableList().apply { set(metric.ordinal, text) }
                            val value = parseWorkoutMetric(metric, text)
                            if (value != null || text.isEmpty()) {
                                onChange(entry.copy(metrics = metric.withValue(entry.metrics, value)))
                            }
                            report()
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, keyboard: KeyboardType, onChange: (String) -> Unit) {
    val t = LocalThemeTokens.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(label, style = TextStyle(t.neutrals.textSecondary.asColor(), 11.sp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            textStyle = TextStyle(t.neutrals.textPrimary.asColor(), 15.sp),
            cursorBrush = SolidColor(t.accent.accentAction.asColor()),
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
                .background(t.neutrals.surfaceMuted.asColor()).padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}
