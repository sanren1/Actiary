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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
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
import com.actiary.domain.WorkoutScreenshot
import com.actiary.domain.withRecognizedWorkout
import com.actiary.glass.GlassDialog
import com.actiary.glass.GlassSegmentedPicker
import com.actiary.glass.GlassSheet
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
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
            ArrayList(d.photos.map { if (it is DraftPhoto.Saved) "s:${it.fileName}" else "p:${(it as DraftPhoto.Picked).uri}" }),
            ArrayList(d.entries.map { it.calories ?: -1 }),
            ArrayList(d.entries.map { it.avgHeartRate ?: -1 }),
            ArrayList(d.entries.map { it.id }),
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
            DayDraft(
                date = LocalDate.ofEpochDay(l[0] as Long),
                status = DayStatus.valueOf(l[1] as String),
                entries = ids.mapIndexed { i, id ->
                    DraftEntry(
                        id,
                        dur[i].takeIf { it >= 0 },
                        dist[i].takeIf { it >= 0 },
                        cal.getOrElse(i) { -1 }.takeIf { it >= 0 },
                        hr.getOrElse(i) { -1 }.takeIf { it >= 0 },
                        entryIds.getOrElse(i) { -i.toLong() - 1 },
                    )
                },
                note = l[2] as String,
                photos = (l[6] as List<String>).map { if (it.startsWith("s:")) DraftPhoto.Saved(it.drop(2)) else DraftPhoto.Picked(it.drop(2)) },
            )
        }
    },
)

private val InvalidEntryIdsSaver = Saver<Set<Long>, List<Long>>(
    save = { it.toList() },
    restore = { it.toSet() },
)

private val CapturePathsSaver = Saver<MutableList<String>, List<String>>(
    save = { it.toList() },
    restore = { it.toMutableList() },
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
    var capturePaths by rememberSaveable(stateSaver = CapturePathsSaver) { mutableStateOf(mutableListOf<String>()) }
    val types by repository.types.collectAsState(emptyList())
    val allTypes by repository.allTypes.collectAsState(emptyList())

    suspend fun load(d: LocalDate) {
        try {
            val day = repository.observeDay(d).first()
            val base = DayDraft.from(d, day)
            val detectedType = screenshot?.workout?.typeKey?.let { key ->
                repository.types.first().firstOrNull { it.builtinKey == key }
            }
            original = base
            waitingForType = screenshot != null && detectedType == null
            draft = if (screenshot == null) base else {
                // Keep all existing photos in the draft. A full day needs a removal before saving.
                val imported = base.markExercised().copy(photos = base.photos + DraftPhoto.Picked(screenshot.uri))
                if (detectedType == null) imported else imported.withRecognizedWorkout(screenshot.workout, detectedType)
            }
            existing = day != null
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

    val current = draft
    val dirty = current != null && original?.let { current.isChangedFrom(it) } == true
    var entered by remember { mutableStateOf(false) } // starts hidden so the slide-in plays
    LaunchedEffect(Unit) { entered = true }
    fun cleanupCaptures() {
        capturePaths.forEach { CameraCapture.cleanup(File(it)) }
        capturePaths = mutableListOf()
    }
    fun close() { cleanupCaptures(); visible = false }
    fun requestClose() { if (saving || !visible) return; if (dirty) showDiscard = true else close() }
    fun update(f: (DayDraft) -> DayDraft) {
        showInvalid = false
        val updated = draft?.let(f) ?: return
        invalidEntryIds = invalidEntryIds.intersect(updated.entries.map { it.id }.toSet())
        draft = updated
    }

    val remaining = DayDraft.PHOTO_LIMIT - (current?.photos?.size ?: 0)
    // PickMultipleVisualMedia requires maxItems >= 2; the single picker covers the last slot.
    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxOf(2, remaining)),
    ) { uris -> update { it.addPhotos(uris.map { u -> u.toString() }) } }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) update { it.addPhotos(listOf(uri.toString())) }
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
                capturePaths = capturePaths.filterNot { it == path }.toMutableList()
            }
        }
    }
    fun launchCapture() {
        if (remaining >= 1) {
            val file = CameraCapture.createTempFile(context.cacheDir)
            pendingCapturePath = file.absolutePath
            capturePaths = (capturePaths + file.absolutePath).toMutableList()
            cameraLauncher.launch(FileProvider.getUriForFile(context, CameraCapture.AUTHORITY, file))
        }
    }

    fun launchOp(work: suspend () -> Unit, after: () -> Unit) {
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

    GlassSheet(visible && entered, onRequestClose = { requestClose() }, onExited = { if (!visible) onDismiss() }, title = stringResource(R.string.record_title)) {
        val primary = t.neutrals.textPrimary.asColor()
        val secondary = t.neutrals.textSecondary.asColor()
        Column(
            Modifier.fillMaxWidth().semantics { if (viewing != null) hideFromAccessibility() }.then(
                // closing: swallow all input until the exit animation ends
                if (visible) Modifier else Modifier.pointerInput(Unit) {
                    awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
                },
            ),
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.widthIn(min = 64.dp).heightIn(min = 48.dp).clickable(enabled = !saving, role = Role.Button) { requestClose() }.padding(horizontal = 8.dp),
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
                        .clickable(role = Role.Button) { showDatePicker = true }.padding(horizontal = 16.dp),
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
                        onRemove = { i -> update { it.removePhoto(i) } },
                        onAdd = { if (remaining >= 2) multiPicker.launch(imageOnly) else singlePicker.launch(imageOnly) },
                        onCapture = { launchCapture() },
                    )
                }
                if (existing) {
                    BasicText(
                        stringResource(R.string.record_clear),
                        Modifier.heightIn(min = 48.dp).clickable(enabled = !saving, role = Role.Button) { showClear = true },
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
                val canSave = current != null && !saving && visible && remaining >= 0 &&
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

    if (showDatePicker) {
        DatePickerDialog(
            selected = current?.date ?: date,
            firstDayOfWeek = firstDayOfWeek,
            onSelect = { d ->
                showDatePicker = false
                if (d != current?.date) {
                    if (dirty) { pendingDate = d.toEpochDay(); showDiscard = true } else scope.launch { load(d) }
                }
            },
            onDismiss = { showDatePicker = false },
        )
    }
    viewing?.let { i ->
        val photo = current?.photos?.getOrNull(i)
        if (photo == null) viewing = null
        else PhotoViewer(photo, photoStore, stringResource(R.string.day_photo, i + 1)) { viewing = null }
    }
    GlassDialog(
        visible = showDiscard,
        text = stringResource(R.string.record_discard_title), confirm = stringResource(R.string.record_discard), dismiss = stringResource(R.string.record_keep_editing),
        onConfirm = {
            showDiscard = false
            val p = pendingDate
            pendingDate = null
            if (p != null) { cleanupCaptures(); scope.launch { load(LocalDate.ofEpochDay(p)) } } else close()
        },
        onDismiss = { showDiscard = false; pendingDate = null },
    )
    GlassDialog(
        visible = showRest,
        text = stringResource(R.string.record_rest_confirm), confirm = stringResource(R.string.record_status_rest), dismiss = stringResource(R.string.record_cancel),
        onConfirm = { showRest = false; update { it.markRest() } },
        onDismiss = { showRest = false },
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

    fun report() = onValidity(!entryInputInvalid(distance, hasDistance, swimming, calories, heartRate))
    LaunchedEffect(Unit) { report() }

    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicText(
                exerciseTypeLabel(type), Modifier.width(72.dp).padding(bottom = 12.dp),
                style = TextStyle(t.neutrals.textPrimary.asColor(), 14.sp, FontWeight.Medium), maxLines = 1,
            )
            NumberField(R.string.record_duration, minutes, Modifier.weight(1f), KeyboardType.Number) {
                minutes = sanitizeMinutes(it)
                onChange(entry.copy(durationMinutes = parseMinutes(minutes)))
            }
            if (hasDistance) {
                NumberField(
                    if (swimming) R.string.record_distance_meters else R.string.record_distance,
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
            NumberField(R.string.record_calories, calories, Modifier.weight(1f), KeyboardType.Number) {
                calories = sanitizeCalories(it)
                onChange(entry.copy(calories = parseCalories(calories)))
                report()
            }
            NumberField(R.string.record_heart_rate, heartRate, Modifier.weight(1f), KeyboardType.Number) {
                heartRate = sanitizeHeartRate(it)
                onChange(entry.copy(avgHeartRate = parseHeartRate(heartRate)))
                report()
            }
        }
    }
}

@Composable
private fun NumberField(label: Int, value: String, modifier: Modifier, keyboard: KeyboardType, onChange: (String) -> Unit) {
    val t = LocalThemeTokens.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        BasicText(stringResource(label), style = TextStyle(t.neutrals.textSecondary.asColor(), 11.sp), maxLines = 1)
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
