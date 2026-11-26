package com.actiary.ui.record

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.actiary.R
import com.actiary.data.photo.PhotoStore
import com.actiary.domain.DayDraft
import com.actiary.domain.DraftPhoto
import com.actiary.glass.OverlayBackHandler
import com.actiary.glass.overlayEnter
import com.actiary.glass.overlayExit
import com.actiary.glass.overlayFadeIn
import com.actiary.glass.overlayFadeOut
import com.actiary.glass.overlayInteraction
import com.actiary.glass.rememberOverlayVisibility
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.motion.rememberMotionReduced
import kotlin.math.roundToInt

private fun model(photo: DraftPhoto, store: PhotoStore): Any = when (photo) {
    is DraftPhoto.Saved -> store.file(photo.fileName)
    is DraftPhoto.Picked -> Uri.parse(photo.uri)
    is DraftPhoto.Staged -> store.stagedFile(photo.fileName)
}

private val PhotoSlot = 88.dp
private val PhotoGap = 8.dp

private data class PhotoDrag(val from: Int, val grabOffset: Float, val pointer: Float) {
    fun start(scroll: Int): Float = pointer + scroll - grabOffset
    fun target(scroll: Int, stride: Float, last: Int): Int =
        (start(scroll) / stride).roundToInt().coerceIn(0, last)
}

@Composable
fun PhotoRow(
    photos: List<DraftPhoto>,
    photoStore: PhotoStore,
    onView: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    onCapture: () -> Unit,
    onMove: (Int, Int) -> Unit,
    editingEnabled: Boolean = true,
) {
    val t = LocalThemeTokens.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val stride = with(density) { (PhotoSlot.roundToPx() + PhotoGap.roundToPx()).toFloat() }
    val slotWidth = with(density) { PhotoSlot.roundToPx() }
    val edge = with(density) { 36.dp.toPx() }
    val speed = with(density) { 360.dp.toPx() }
    val scroll = rememberScrollState()
    val move by rememberUpdatedState(onMove)
    var viewportWidth by remember { mutableIntStateOf(0) }
    var drag by remember(photos) { mutableStateOf<PhotoDrag?>(null) }

    LaunchedEffect(drag != null, viewportWidth, edge, speed) {
        if (drag == null) return@LaunchedEffect
        var previousFrame = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val holding = drag ?: break
            val edgeWidth = minOf(edge, viewportWidth / 2f)
            if (edgeWidth > 0f) {
                val start = ((edgeWidth - holding.pointer) / edgeWidth).coerceIn(0f, 1f)
                val end = ((holding.pointer - viewportWidth + edgeWidth) / edgeWidth).coerceIn(0f, 1f)
                val seconds = (frame - previousFrame) / 1_000_000_000f
                if (start != end) scroll.scrollBy((end - start) * speed * seconds)
            }
            previousFrame = frame
        }
    }

    val holding = drag
    val target = holding?.target(scroll.value, stride, photos.lastIndex)
    val order = photos.indices.toMutableList()
    if (holding != null && target != null) order.add(target, order.removeAt(holding.from))

    Box(
        Modifier.fillMaxWidth()
            .onSizeChanged { viewportWidth = it.width }
            .pointerInput(photos, editingEnabled, stride, isRtl) {
                if (!editingEnabled || photos.size < 2) return@pointerInput
                fun logical(x: Float): Float = if (isRtl) size.width - x else x
                detectDragGesturesAfterLongPress(
                    onDragStart = { pos ->
                        val pointer = logical(pos.x)
                        val content = pointer + scroll.value
                        val index = (content / stride).toInt()
                        val grab = content - index * stride
                        if (index in photos.indices && grab >= 0 && grab < slotWidth) {
                            drag = PhotoDrag(index, grab, pointer)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    onDrag = { change, _ -> drag = drag?.copy(pointer = logical(change.position.x)) },
                    onDragEnd = {
                        val finished = drag
                        drag = null
                        if (finished != null) {
                            val destination = finished.target(scroll.value, stride, photos.lastIndex)
                            if (destination != finished.from) move(finished.from, destination)
                        }
                    },
                    onDragCancel = { drag = null },
                )
            },
    ) {
        Row(
            Modifier.horizontalScroll(scroll, enabled = holding == null),
            horizontalArrangement = Arrangement.spacedBy(PhotoGap),
        ) {
            order.forEachIndexed { position, index ->
                key(index) {
                    val earlier = stringResource(R.string.record_move_photo_earlier)
                    val later = stringResource(R.string.record_move_photo_later)
                    val isMoving = holding?.from == index
                    Box(
                        Modifier.size(PhotoSlot).then(
                            if (isMoving) Modifier.zIndex(1f).graphicsLayer {
                                val active = checkNotNull(holding)
                                val delta = active.start(scroll.value) - position * stride
                                translationX = if (isRtl) -delta else delta
                                scaleX = 1.05f
                                scaleY = 1.05f
                            } else Modifier,
                        ),
                    ) {
                        AsyncImage(
                            model(photos[index], photoStore), stringResource(R.string.day_photo, position + 1),
                            Modifier.size(80.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(12.dp))
                                .background(t.neutrals.surfaceMuted.asColor())
                                .clickable(enabled = holding == null, role = Role.Button) { onView(index) }
                                .semantics {
                                    if (editingEnabled && holding == null) {
                                        customActions = buildList {
                                            if (index > 0) add(CustomAccessibilityAction(earlier) { move(index, index - 1); true })
                                            if (index < photos.lastIndex) add(CustomAccessibilityAction(later) { move(index, index + 1); true })
                                        }
                                    }
                                },
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            Modifier.align(Alignment.TopEnd).size(48.dp).clip(CircleShape)
                                .clickable(enabled = editingEnabled && holding == null, role = Role.Button) { onRemove(index) },
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Box(
                                Modifier.padding(top = 4.dp, end = 4.dp).size(24.dp)
                                    .background(t.neutrals.surface.asColor(), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.record_remove_photo), Modifier.size(16.dp), tint = t.neutrals.textPrimary.asColor())
                            }
                        }
                    }
                }
            }
            if (photos.size < DayDraft.PHOTO_LIMIT) {
                Box(
                    Modifier.padding(top = 8.dp).size(80.dp).clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surfaceMuted.asColor())
                        .clickable(enabled = editingEnabled && holding == null, role = Role.Button, onClick = onCapture),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.PhotoCamera, stringResource(R.string.record_take_photo), tint = t.neutrals.textPrimary.asColor())
                }
                Box(
                    Modifier.padding(top = 8.dp).size(80.dp).clip(RoundedCornerShape(12.dp))
                        .background(t.neutrals.surfaceMuted.asColor())
                        .clickable(enabled = editingEnabled && holding == null, role = Role.Button, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.record_add_photos), tint = t.neutrals.textPrimary.asColor())
                }
            }
        }
    }
}

@Composable
fun PhotoViewer(
    photos: List<DraftPhoto>,
    initialIndex: Int?,
    photoStore: PhotoStore,
    visibility: MutableTransitionState<Boolean> = rememberOverlayVisibility(initialIndex != null && initialIndex in photos.indices),
    onDismiss: () -> Unit,
) {
    OverlayBackHandler(visibility, onDismiss)
    val current = initialIndex?.takeIf { it in photos.indices }?.let { photos to it }
    var last by remember { mutableStateOf<Pair<List<DraftPhoto>, Int>?>(null) }
    SideEffect { if (current != null) last = current }
    val reduced = rememberMotionReduced()
    val pane = stringResource(R.string.record_photos)
    val close = stringResource(R.string.overlay_close)
    AnimatedVisibility(visibility, enter = EnterTransition.None, exit = ExitTransition.None) {
        Box(
            Modifier.fillMaxSize().overlayInteraction(current != null)
                .semantics {
                    paneTitle = pane
                    isTraversalGroup = true
                }.clickable(interactionSource = null, indication = null, onClickLabel = close, onClick = onDismiss),
        ) {
            Box(
                Modifier.matchParentSize()
                    .animateEnterExit(overlayFadeIn(reduced), overlayFadeOut(reduced))
                    .background(Color.Black),
            )
            (current ?: last)?.let { (shownPhotos, shownInitialIndex) ->
                if (shownPhotos.size > 1) {
                    key(shownPhotos, shownInitialIndex) {
                        val pagerState = rememberPagerState(
                            initialPage = shownInitialIndex.coerceIn(0, shownPhotos.lastIndex),
                            pageCount = { shownPhotos.size },
                        )
                        Box(
                            Modifier.fillMaxSize()
                                .animateEnterExit(overlayEnter(reduced), overlayExit(reduced)),
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                AsyncImage(
                                    model(shownPhotos[page], photoStore),
                                    stringResource(R.string.day_photo, page + 1),
                                    Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                            BasicText(
                                "${pagerState.currentPage + 1} / ${shownPhotos.size}",
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                                    .padding(bottom = 20.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                style = TextStyle(Color.White, 13.sp, FontWeight.Medium),
                            )
                        }
                    }
                } else if (shownPhotos.size == 1) {
                    AsyncImage(
                        model(shownPhotos[0], photoStore),
                        stringResource(R.string.day_photo, 1),
                        Modifier.fillMaxSize().animateEnterExit(overlayEnter(reduced), overlayExit(reduced)),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
    }
}
