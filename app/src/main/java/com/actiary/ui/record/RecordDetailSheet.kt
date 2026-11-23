package com.actiary.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.db.DayStatus
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DraftPhoto
import com.actiary.domain.FreeformFocusNote
import com.actiary.domain.FreeformFocusPart
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassSheet
import com.actiary.glass.overlayPresent
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
internal fun RecordDetailSheet(
    date: LocalDate,
    repository: RecordRepository,
    photoStore: PhotoStore,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val resources = LocalContext.current.resources
    val title = date.format(
        DateTimeFormatter.ofPattern(stringResource(R.string.day_accessibility_pattern), LocalConfiguration.current.locales[0]),
    )
    val loaded by remember(repository, date) {
        repository.observeDay(date).combine(repository.allTypes) { day, types ->
            day to types.associateBy { it.id }
        }
    }.collectAsStateWithLifecycle(null)
    val types = loaded?.second.orEmpty()
    val detail = loaded?.first?.let { day ->
        buildDetailModel(day, types) { format, value -> resources.getString(format, value) }
    }
    var visible by rememberSaveable { mutableStateOf(true) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var viewingPhoto by rememberSaveable { mutableStateOf<Int?>(null) }
    val photoIndex = viewingPhoto
    val photo = photoIndex?.let { detail?.photos?.getOrNull(it) }
    val photoVisibility = rememberOverlayVisibility(photo != null)

    GlassSheet(
        visible = visible,
        onRequestClose = { visible = false; true },
        onExited = { if (editing) onEdit() else onDismiss() },
        title = title,
        covered = photoVisibility.overlayPresent,
    ) {
        Column(
            Modifier.align(Alignment.CenterHorizontally).widthIn(max = 560.dp).fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title, Modifier.weight(1f).semantics { heading() },
                    color = primary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                )
                GlassIconButton(
                    Icons.Outlined.Edit, stringResource(R.string.record_detail_edit),
                    { editing = true; visible = false },
                    enabled = visible, glass = false,
                )
            }
            Column(
                Modifier.fillMaxWidth().weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (detail != null) {
                    val rest = detail.status == DayStatus.REST
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(if (rest) Icons.Outlined.Bedtime else Icons.Outlined.CheckCircle, null, Modifier.size(20.dp), tint = primary)
                        Text(stringResource(if (rest) R.string.day_rest else R.string.day_state_logged), color = primary)
                    }
                    val focusLabels = freeformFocusLabels()
                    val detectedFocus = remember(detail.note, focusLabels) {
                        val set = FreeformFocusNote.detectSelected(detail.note, focusLabels)
                        FreeformFocusPart.entries.filter { it in set }
                    }
                    detail.entries.forEach { entry ->
                        types[entry.typeId]?.let { type ->
                            val focus = if (type.builtinKey == "freeform") detectedFocus else emptyList()
                            DetailSession(type, entry, focus, focusLabels)
                        }
                    }
                    if (detail.empty) Text(stringResource(R.string.record_detail_empty), color = secondary)
                    if (detail.note.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.record_note), Modifier.semantics { heading() }, color = secondary, fontSize = 14.sp)
                            Text(detail.note, color = primary, fontSize = 16.sp)
                        }
                    }
                    if (detail.photos.isNotEmpty()) {
                        DetailPhotos(detail.photos, photoStore, visible) { viewingPhoto = it }
                    }
                } else if (loaded != null) {
                    Text(stringResource(R.string.day_unrecorded), color = secondary)
                }
            }
        }
    }
    PhotoViewer(
        photo?.let { DraftPhoto.Saved(it) }, photoStore,
        stringResource(R.string.day_photo, (photoIndex ?: 0) + 1), photoVisibility,
    ) {
        viewingPhoto = null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailSession(
    type: ExerciseTypeEntity,
    entry: DetailEntry,
    focusParts: List<FreeformFocusPart> = emptyList(),
    focusLabels: Map<FreeformFocusPart, String> = emptyMap(),
) {
    val t = LocalThemeTokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val accent = t.accent.accentAction.asColor()
    Column(
        Modifier.fillMaxWidth().background(t.neutrals.surface.asColor(), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(exerciseTypeIcon(type), null, Modifier.size(20.dp), tint = primary)
            Text(exerciseTypeLabel(type), Modifier.semantics { heading() }, color = primary, fontWeight = FontWeight.Medium)
        }
        if (focusParts.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val pillShape = RoundedCornerShape(8.dp)
                focusParts.forEach { part ->
                    Row(
                        Modifier.clip(pillShape)
                            .background(accent.copy(alpha = 0.14f))
                            .border(1.dp, accent.copy(alpha = 0.45f), pillShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(freeformFocusPartIcon(part), null, Modifier.size(16.dp), tint = accent)
                        Text(
                            focusLabels[part].orEmpty(),
                            color = accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
        entry.metrics.forEach { (label, value) ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(label), Modifier.weight(1f), color = secondary, fontSize = 14.sp)
                Text(value, color = primary, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DetailPhotos(photos: List<String>, photoStore: PhotoStore, enabled: Boolean, onView: (Int) -> Unit) {
    val t = LocalThemeTokens.current
    val columns = 3
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.record_photos), Modifier.semantics { heading() },
            color = t.neutrals.textSecondary.asColor(), fontSize = 14.sp,
        )
        photos.chunked(columns).forEachIndexed { row, thumbnails ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                thumbnails.forEachIndexed { column, fileName ->
                    val index = row * columns + column
                    AsyncImage(
                        photoStore.file(fileName), stringResource(R.string.day_photo, index + 1),
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                            .background(t.neutrals.surfaceMuted.asColor())
                            .clickable(enabled = enabled, role = Role.Button) { onView(index) },
                        contentScale = ContentScale.Crop,
                    )
                }
                repeat(columns - thumbnails.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
