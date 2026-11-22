package com.actiary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.db.DayStatus
import com.actiary.data.db.DayWithDetails
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.data.photo.PhotoStore
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.ui.records.entryMetrics

private const val PHOTO_COLUMNS = 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayDetail(
    title: String,
    spokenTitle: String,
    detail: DayWithDetails?,
    types: Map<Long, ExerciseTypeEntity>,
    photoStore: PhotoStore,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val t = LocalThemeTokens.current
    val primary = t.neutrals.textPrimary.asColor()
    val secondary = t.neutrals.textSecondary.asColor()
    val resources = LocalContext.current.resources
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BasicText(
            title,
            Modifier.clearAndSetSemantics {
                contentDescription = spokenTitle
                heading()
            },
            style = TextStyle(primary, 18.sp, FontWeight.SemiBold),
        )
        if (loading) return@Column
        val entries = detail?.entries.orEmpty()
        when {
            detail == null -> BasicText(stringResource(R.string.day_unrecorded), style = TextStyle(secondary, 15.sp))
            detail.record.status == DayStatus.REST ->
                BasicText(stringResource(R.string.day_rest), style = TextStyle(primary, 15.sp, FontWeight.Medium))
            entries.isEmpty() ->
                BasicText(stringResource(R.string.day_checked_in), style = TextStyle(primary, 15.sp, FontWeight.Medium))
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    val type = types[entry.typeId] ?: return@forEach // unknown type: render nothing, keep the state
                    val metrics = entryMetrics(entry, type, includeOptional = true) { id, arg ->
                        resources.getString(id, arg)
                    }
                    Column(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(t.neutrals.surface.asColor().copy(alpha = 0.72f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = primary)
                            BasicText(exerciseTypeLabel(type), style = TextStyle(primary, 15.sp, FontWeight.SemiBold))
                        }
                        if (metrics.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                metrics.forEach { (labelId, value) ->
                                    Column {
                                        BasicText(stringResource(labelId), style = TextStyle(secondary, 11.sp))
                                        BasicText(value, style = TextStyle(primary, 14.sp, FontWeight.Medium))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (detail != null && detail.record.note.isNotBlank()) {
            BasicText(
                detail.record.note,
                Modifier.fillMaxWidth().background(t.neutrals.surface.asColor().copy(alpha = 0.72f), RoundedCornerShape(12.dp)).padding(12.dp),
                style = TextStyle(primary, 15.sp),
            )
        }
        val photos = detail?.photos.orEmpty()
        if (photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.chunked(PHOTO_COLUMNS).forEachIndexed { rowIndex, rowPhotos ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowPhotos.forEachIndexed { colIndex, p ->
                            val index = rowIndex * PHOTO_COLUMNS + colIndex
                            Box(
                                Modifier.weight(1f).aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp)).background(t.neutrals.surfaceMuted.asColor()),
                            ) {
                                AsyncImage(
                                    photoStore.file(p.fileName),
                                    stringResource(R.string.day_photo, index + 1),
                                    Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        repeat(PHOTO_COLUMNS - rowPhotos.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
