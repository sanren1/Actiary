package com.actiary.ui.record

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DayDraft
import com.actiary.domain.DraftPhoto
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

private fun model(photo: DraftPhoto, store: PhotoStore): Any = when (photo) {
    is DraftPhoto.Saved -> store.file(photo.fileName)
    is DraftPhoto.Picked -> Uri.parse(photo.uri)
}

@Composable
fun PhotoRow(
    photos: List<DraftPhoto>,
    photoStore: PhotoStore,
    onView: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val t = LocalThemeTokens.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(photos) { i, p ->
            Box(Modifier.size(88.dp)) {
                AsyncImage(
                    model(p, photoStore), stringResource(R.string.day_photo, i + 1),
                    Modifier.size(80.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surfaceMuted.asColor())
                        .clickable(role = Role.Button) { onView(i) },
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.align(Alignment.TopEnd).size(48.dp).clip(CircleShape)
                        .clickable(role = Role.Button) { onRemove(i) },
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Box(
                        Modifier.padding(top = 4.dp, end = 4.dp).size(24.dp).background(t.neutrals.surface.asColor(), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.record_remove_photo), Modifier.size(16.dp), tint = t.neutrals.textPrimary.asColor())
                    }
                }
            }
        }
        if (photos.size < DayDraft.PHOTO_LIMIT) {
            item {
                Box(
                    Modifier.padding(top = 8.dp).size(80.dp).clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surfaceMuted.asColor()).clickable(role = Role.Button, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.record_add_photos), tint = t.neutrals.textPrimary.asColor())
                }
            }
        }
    }
}

@Composable
fun PhotoViewer(photo: DraftPhoto, photoStore: PhotoStore, description: String, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(Modifier.fillMaxSize().background(Color.Black).clickable(interactionSource = null, indication = null, onClick = onDismiss)) {
        AsyncImage(model(photo, photoStore), description, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
