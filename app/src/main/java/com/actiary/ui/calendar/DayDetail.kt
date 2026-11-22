package com.actiary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private const val MAX_THUMBS = 3

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
            else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    val type = types[entry.typeId] ?: return@forEach // unknown type: render nothing, keep the state
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(t.neutrals.surfaceMuted.asColor())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(exerciseTypeIcon(type), null, Modifier.size(18.dp), tint = primary)
                        BasicText(exerciseTypeLabel(type), style = TextStyle(primary, 14.sp))
                    }
                }
            }
        }
        if (detail != null && detail.record.note.isNotBlank()) {
            BasicText(
                detail.record.note,
                Modifier.fillMaxWidth().background(t.neutrals.surface.asColor().copy(alpha = 0.72f), RoundedCornerShape(12.dp)).padding(12.dp),
                style = TextStyle(primary, 15.sp), maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
        val photos = detail?.photos.orEmpty()
        if (photos.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                photos.take(MAX_THUMBS).forEachIndexed { i, p ->
                    Box(
                        Modifier.weight(1f, fill = false).widthIn(max = 96.dp).aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp)).background(t.neutrals.surfaceMuted.asColor()),
                    ) {
                        AsyncImage(photoStore.file(p.fileName), stringResource(R.string.day_photo, i + 1), Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                        if (i == MAX_THUMBS - 1 && photos.size > MAX_THUMBS) {
                            Box(Modifier.matchParentSize().background(t.neutrals.background.asColor().copy(alpha = 0.6f)), Alignment.Center) {
                                BasicText(
                                    stringResource(R.string.day_more_photos, photos.size - MAX_THUMBS),
                                    style = TextStyle(primary, 18.sp, FontWeight.SemiBold),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
