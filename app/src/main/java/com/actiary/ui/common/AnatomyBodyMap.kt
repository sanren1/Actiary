package com.actiary.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.domain.MuscleActivation
import com.actiary.domain.MuscleGroup
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.ThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.motion.Motion
import com.actiary.ui.motion.rememberMotionReduced

enum class BodySide { FRONT, BACK }

sealed interface BodyMapStyle {
    data class Highlight(val activations: Map<MuscleGroup, MuscleActivation>) : BodyMapStyle
    data class Heatmap(val levels: Map<MuscleGroup, Int>, val selected: MuscleGroup? = null) : BodyMapStyle
}

/**
 * Maps a muscle's set count relative to [maxSets] into 4 heatmap intensity levels (0..3):
 * 0 = unworked, 1 = <=33%, 2 = <=66%, 3 = >66%.
 */
fun heatmapLevel(sets: Int, maxSets: Int): Int {
    if (sets <= 0 || maxSets <= 0) return 0
    val ratio = sets.toFloat() / maxSets.toFloat()
    return when {
        ratio <= 0.33f -> 1
        ratio <= 0.66f -> 2
        else -> 3
    }
}

internal data class NormPoint(val x: Float, val y: Float)

internal data class MusclePolygon(
    val muscle: MuscleGroup,
    val side: BodySide,
    val points: List<NormPoint>,
)

private fun mirrorPolygon(
    muscle: MuscleGroup,
    side: BodySide,
    leftPoints: List<NormPoint>,
): List<MusclePolygon> {
    val rightPoints = leftPoints.map { NormPoint(1f - it.x, it.y) }.reversed()
    return listOf(
        MusclePolygon(muscle, side, leftPoints),
        MusclePolygon(muscle, side, rightPoints),
    )
}

/**
 * Normalized (0..1 x 0..1 on a 1:2 figure canvas) muscle polygons for all 14 [MuscleGroup]s
 * across front and back anatomical views, symmetrically mirrored around x = 0.5f.
 */
internal val ANATOMY_POLYGONS: List<MusclePolygon> = buildList {
    // --- FRONT VIEW ---
    // Front & side deltoids
    addAll(
        mirrorPolygon(
            MuscleGroup.FRONT_SIDE_DELTS,
            BodySide.FRONT,
            listOf(
                NormPoint(0.312f, 0.206f),
                NormPoint(0.244f, 0.214f),
                NormPoint(0.206f, 0.246f),
                NormPoint(0.208f, 0.290f),
                NormPoint(0.256f, 0.302f),
                NormPoint(0.296f, 0.274f),
                NormPoint(0.320f, 0.236f),
            ),
        ),
    )
    // Pectorals
    addAll(
        mirrorPolygon(
            MuscleGroup.PECTORALS,
            BodySide.FRONT,
            listOf(
                NormPoint(0.328f, 0.226f),
                NormPoint(0.484f, 0.236f),
                NormPoint(0.488f, 0.314f),
                NormPoint(0.438f, 0.328f),
                NormPoint(0.344f, 0.318f),
                NormPoint(0.308f, 0.284f),
            ),
        ),
    )
    // Biceps
    addAll(
        mirrorPolygon(
            MuscleGroup.BICEPS,
            BodySide.FRONT,
            listOf(
                NormPoint(0.212f, 0.308f),
                NormPoint(0.286f, 0.308f),
                NormPoint(0.266f, 0.356f),
                NormPoint(0.236f, 0.396f),
                NormPoint(0.172f, 0.390f),
                NormPoint(0.188f, 0.344f),
            ),
        ),
    )
    // Forearms (front)
    addAll(
        mirrorPolygon(
            MuscleGroup.FOREARMS,
            BodySide.FRONT,
            listOf(
                NormPoint(0.168f, 0.405f),
                NormPoint(0.232f, 0.408f),
                NormPoint(0.204f, 0.458f),
                NormPoint(0.174f, 0.502f),
                NormPoint(0.122f, 0.498f),
                NormPoint(0.136f, 0.446f),
            ),
        ),
    )
    // Abs & core (unified center polygon for clean hit-testing + 6-pack inner lines)
    add(
        MusclePolygon(
            MuscleGroup.ABS,
            BodySide.FRONT,
            listOf(
                NormPoint(0.376f, 0.338f),
                NormPoint(0.624f, 0.338f),
                NormPoint(0.614f, 0.415f),
                NormPoint(0.594f, 0.484f),
                NormPoint(0.500f, 0.516f),
                NormPoint(0.406f, 0.484f),
                NormPoint(0.386f, 0.415f),
            ),
        ),
    )
    // Quadriceps
    addAll(
        mirrorPolygon(
            MuscleGroup.QUADS,
            BodySide.FRONT,
            listOf(
                NormPoint(0.312f, 0.535f),
                NormPoint(0.468f, 0.555f),
                NormPoint(0.452f, 0.648f),
                NormPoint(0.428f, 0.718f),
                NormPoint(0.342f, 0.718f),
                NormPoint(0.308f, 0.635f),
            ),
        ),
    )
    // Calves (front outer/shin contours)
    addAll(
        mirrorPolygon(
            MuscleGroup.CALVES,
            BodySide.FRONT,
            listOf(
                NormPoint(0.326f, 0.762f),
                NormPoint(0.432f, 0.762f),
                NormPoint(0.438f, 0.818f),
                NormPoint(0.412f, 0.904f),
                NormPoint(0.348f, 0.904f),
                NormPoint(0.314f, 0.818f),
            ),
        ),
    )

    // --- BACK VIEW ---
    // Traps & upper back (classic kite contour)
    add(
        MusclePolygon(
            MuscleGroup.UPPER_BACK,
            BodySide.BACK,
            listOf(
                NormPoint(0.445f, 0.178f),
                NormPoint(0.555f, 0.178f),
                NormPoint(0.654f, 0.215f),
                NormPoint(0.634f, 0.284f),
                NormPoint(0.500f, 0.344f),
                NormPoint(0.366f, 0.284f),
                NormPoint(0.346f, 0.215f),
            ),
        ),
    )
    // Rear deltoids
    addAll(
        mirrorPolygon(
            MuscleGroup.REAR_DELTS,
            BodySide.BACK,
            listOf(
                NormPoint(0.338f, 0.212f),
                NormPoint(0.248f, 0.216f),
                NormPoint(0.206f, 0.248f),
                NormPoint(0.212f, 0.292f),
                NormPoint(0.278f, 0.298f),
                NormPoint(0.334f, 0.268f),
            ),
        ),
    )
    // Lats (V-taper wings)
    addAll(
        mirrorPolygon(
            MuscleGroup.LATS,
            BodySide.BACK,
            listOf(
                NormPoint(0.306f, 0.294f),
                NormPoint(0.362f, 0.296f),
                NormPoint(0.482f, 0.352f),
                NormPoint(0.478f, 0.418f),
                NormPoint(0.356f, 0.415f),
                NormPoint(0.322f, 0.356f),
            ),
        ),
    )
    // Lower back (lumbar / erector spinae)
    add(
        MusclePolygon(
            MuscleGroup.LOWER_BACK,
            BodySide.BACK,
            listOf(
                NormPoint(0.362f, 0.425f),
                NormPoint(0.638f, 0.425f),
                NormPoint(0.646f, 0.482f),
                NormPoint(0.500f, 0.494f),
                NormPoint(0.354f, 0.482f),
            ),
        ),
    )
    // Triceps
    addAll(
        mirrorPolygon(
            MuscleGroup.TRICEPS,
            BodySide.BACK,
            listOf(
                NormPoint(0.212f, 0.306f),
                NormPoint(0.286f, 0.308f),
                NormPoint(0.266f, 0.358f),
                NormPoint(0.236f, 0.396f),
                NormPoint(0.172f, 0.390f),
                NormPoint(0.188f, 0.344f),
            ),
        ),
    )
    // Forearms (back)
    addAll(
        mirrorPolygon(
            MuscleGroup.FOREARMS,
            BodySide.BACK,
            listOf(
                NormPoint(0.168f, 0.405f),
                NormPoint(0.232f, 0.408f),
                NormPoint(0.204f, 0.458f),
                NormPoint(0.174f, 0.502f),
                NormPoint(0.122f, 0.498f),
                NormPoint(0.136f, 0.446f),
            ),
        ),
    )
    // Glutes
    addAll(
        mirrorPolygon(
            MuscleGroup.GLUTES,
            BodySide.BACK,
            listOf(
                NormPoint(0.336f, 0.502f),
                NormPoint(0.490f, 0.506f),
                NormPoint(0.488f, 0.584f),
                NormPoint(0.424f, 0.596f),
                NormPoint(0.322f, 0.580f),
                NormPoint(0.308f, 0.538f),
            ),
        ),
    )
    // Hamstrings
    addAll(
        mirrorPolygon(
            MuscleGroup.HAMSTRINGS,
            BodySide.BACK,
            listOf(
                NormPoint(0.318f, 0.606f),
                NormPoint(0.468f, 0.608f),
                NormPoint(0.446f, 0.678f),
                NormPoint(0.428f, 0.728f),
                NormPoint(0.336f, 0.728f),
                NormPoint(0.312f, 0.662f),
            ),
        ),
    )
    // Calves (back gastrocnemius)
    addAll(
        mirrorPolygon(
            MuscleGroup.CALVES,
            BodySide.BACK,
            listOf(
                NormPoint(0.328f, 0.754f),
                NormPoint(0.432f, 0.754f),
                NormPoint(0.444f, 0.808f),
                NormPoint(0.414f, 0.904f),
                NormPoint(0.346f, 0.904f),
                NormPoint(0.310f, 0.808f),
            ),
        ),
    )
}

private val FRONT_POLYGONS: List<MusclePolygon> = ANATOMY_POLYGONS.filter { it.side == BodySide.FRONT }
private val BACK_POLYGONS: List<MusclePolygon> = ANATOMY_POLYGONS.filter { it.side == BodySide.BACK }

/**
 * Ray-casting point-in-polygon hit test against [ANATOMY_POLYGONS] for [side].
 * Returns `null` if `(normX, normY)` is outside `0f..1f` or does not fall inside any muscle polygon.
 */
internal fun hitTestMuscle(side: BodySide, normX: Float, normY: Float): MuscleGroup? {
    if (normX !in 0f..1f || normY !in 0f..1f) return null
    val candidates = if (side == BodySide.FRONT) FRONT_POLYGONS else BACK_POLYGONS
    for (polygon in candidates) {
        if (pointInPolygon(normX, normY, polygon.points)) {
            return polygon.muscle
        }
    }
    return null
}

private fun pointInPolygon(x: Float, y: Float, points: List<NormPoint>): Boolean {
    val count = points.size
    if (count < 3) return false
    var inside = false
    var j = count - 1
    for (i in 0 until count) {
        val pi = points[i]
        val pj = points[j]
        val intersects = ((pi.y > y) != (pj.y > y)) &&
            (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x)
        if (intersects) inside = !inside
        j = i
    }
    return inside
}

private data class FigureRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

private fun computeFigureRect(canvasWidth: Float, canvasHeight: Float): FigureRect {
    if (canvasWidth <= 0f || canvasHeight <= 0f) return FigureRect(0f, 0f, 0f, 0f)
    val figureWidth = minOf(canvasWidth, canvasHeight * 0.5f)
    val figureHeight = figureWidth * 2f
    val left = (canvasWidth - figureWidth) * 0.5f
    val top = (canvasHeight - figureHeight) * 0.5f
    return FigureRect(left = left, top = top, width = figureWidth, height = figureHeight)
}

@Composable
fun AnatomyBodyMap(
    style: BodyMapStyle,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    onMuscleClick: ((MuscleGroup) -> Unit)? = null,
) {
    val tokens = LocalThemeTokens.current
    val reduced = rememberMotionReduced()
    val musclePaints = rememberAnimatedMusclePaints(style = style, tokens = tokens, reduced = reduced)
    val selectedMuscle = (style as? BodyMapStyle.Heatmap)?.selected
    BoxWithConstraints(modifier = modifier) {
        val hasBoundedHeight = constraints.hasBoundedHeight
        Row(
            modifier = if (hasBoundedHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnatomyFigureColumn(
                side = BodySide.FRONT,
                label = if (showLabels) stringResource(R.string.strength_body_front) else null,
                musclePaints = musclePaints,
                selectedMuscle = selectedMuscle,
                tokens = tokens,
                hasBoundedHeight = hasBoundedHeight,
                onMuscleClick = onMuscleClick,
                modifier = if (hasBoundedHeight) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f),
            )
            AnatomyFigureColumn(
                side = BodySide.BACK,
                label = if (showLabels) stringResource(R.string.strength_body_back) else null,
                musclePaints = musclePaints,
                selectedMuscle = selectedMuscle,
                tokens = tokens,
                hasBoundedHeight = hasBoundedHeight,
                onMuscleClick = onMuscleClick,
                modifier = if (hasBoundedHeight) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun rememberAnimatedMusclePaints(
    style: BodyMapStyle,
    tokens: ThemeTokens,
    reduced: Boolean,
): Map<MuscleGroup, MusclePaint> {
    val spec = if (reduced) snap() else tween<Color>(Motion.FADE_IN_MS)
    return MuscleGroup.entries.associateWith { muscle ->
        val target = resolveMusclePaint(muscle, style, tokens)
        val fill by animateColorAsState(
            targetValue = target.fill,
            animationSpec = spec,
            label = "muscleFill_${muscle.key}",
        )
        val stroke by animateColorAsState(
            targetValue = target.stroke,
            animationSpec = spec,
            label = "muscleStroke_${muscle.key}",
        )
        target.copy(fill = fill, stroke = stroke)
    }
}

@Composable
private fun AnatomyFigureColumn(
    side: BodySide,
    label: String?,
    musclePaints: Map<MuscleGroup, MusclePaint>,
    selectedMuscle: MuscleGroup?,
    tokens: ThemeTokens,
    hasBoundedHeight: Boolean,
    onMuscleClick: ((MuscleGroup) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val latestOnClick by rememberUpdatedState(onMuscleClick)
    val polygons = remember(side) { if (side == BodySide.FRONT) FRONT_POLYGONS else BACK_POLYGONS }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val canvasBaseModifier = if (hasBoundedHeight) {
            Modifier.weight(1f).fillMaxWidth()
        } else {
            Modifier.height(168.dp).fillMaxWidth()
        }
        val tapModifier = if (onMuscleClick != null) {
            Modifier.pointerInput(side) {
                detectTapGestures { offset ->
                    val rect = computeFigureRect(size.width.toFloat(), size.height.toFloat())
                    if (rect.width > 0f && rect.height > 0f) {
                        val normX = (offset.x - rect.left) / rect.width
                        val normY = (offset.y - rect.top) / rect.height
                        hitTestMuscle(side, normX, normY)?.let { muscle ->
                            latestOnClick?.invoke(muscle)
                        }
                    }
                }
            }
        } else {
            Modifier
        }

        Canvas(modifier = canvasBaseModifier.then(tapModifier)) {
            val rect = computeFigureRect(size.width, size.height)
            if (rect.width <= 0f || rect.height <= 0f) return@Canvas
            drawAnatomicalFigure(
                side = side,
                polygons = polygons,
                musclePaints = musclePaints,
                selectedMuscle = selectedMuscle,
                tokens = tokens,
                rect = rect,
            )
        }

        if (label != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = tokens.neutrals.textSecondary.asColor(),
            )
        }
    }
}

private data class MusclePaint(
    val fill: Color,
    val stroke: Color,
    val strokeWidthDp: Float,
    val hasSheen: Boolean,
)

private fun resolveMusclePaint(
    muscle: MuscleGroup,
    style: BodyMapStyle,
    tokens: ThemeTokens,
): MusclePaint {
    val accent = tokens.accent.accentAction.asColor()
    val neutralText = tokens.neutrals.textSecondary.asColor()
    val neutralFill = neutralText.copy(alpha = if (tokens.dark) 0.12f else 0.085f)
    val neutralStroke = neutralText.copy(alpha = if (tokens.dark) 0.24f else 0.20f)

    return when (style) {
        is BodyMapStyle.Highlight -> {
            when (style.activations[muscle] ?: MuscleActivation.NONE) {
                MuscleActivation.PRIMARY -> MusclePaint(
                    fill = accent.copy(alpha = 0.94f),
                    stroke = accent,
                    strokeWidthDp = 1.15f,
                    hasSheen = true,
                )
                MuscleActivation.SECONDARY -> MusclePaint(
                    fill = accent.copy(alpha = 0.35f),
                    stroke = accent.copy(alpha = 0.68f),
                    strokeWidthDp = 0.9f,
                    hasSheen = false,
                )
                MuscleActivation.NONE -> MusclePaint(
                    fill = neutralFill,
                    stroke = neutralStroke,
                    strokeWidthDp = 0.65f,
                    hasSheen = false,
                )
            }
        }
        is BodyMapStyle.Heatmap -> {
            val level = (style.levels[muscle] ?: 0).coerceIn(0, 3)
            val isSelected = style.selected == muscle
            val fill = when (level) {
                1 -> accent.copy(alpha = 0.28f)
                2 -> accent.copy(alpha = 0.60f)
                3 -> accent.copy(alpha = 0.95f)
                else -> neutralFill
            }
            val baseStroke = when (level) {
                1 -> accent.copy(alpha = 0.52f)
                2 -> accent.copy(alpha = 0.78f)
                3 -> accent
                else -> neutralStroke
            }
            val stroke = if (isSelected) tokens.neutrals.textPrimary.asColor() else baseStroke
            val strokeWidth = if (isSelected) 1.75f else if (level > 0) 0.95f else 0.65f
            MusclePaint(fill = fill, stroke = stroke, strokeWidthDp = strokeWidth, hasSheen = level >= 2)
        }
    }
}

private fun DrawScope.drawAnatomicalFigure(
    side: BodySide,
    polygons: List<MusclePolygon>,
    musclePaints: Map<MuscleGroup, MusclePaint>,
    selectedMuscle: MuscleGroup?,
    tokens: ThemeTokens,
    rect: FigureRect,
) {
    val neutralText = tokens.neutrals.textSecondary.asColor()
    val bodyFill = neutralText.copy(alpha = if (tokens.dark) 0.09f else 0.065f)
    val bodyOutline = neutralText.copy(alpha = if (tokens.dark) 0.44f else 0.38f)
    val detailLine = neutralText.copy(alpha = if (tokens.dark) 0.24f else 0.20f)
    val scaleFactor = (rect.height / 180.dp.toPx()).coerceIn(0.75f, 1.35f)
    val outlineStroke = Stroke(
        width = 1.15.dp.toPx() * scaleFactor,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )

    // 1. Head & full-body anatomical silhouette
    val headPath = buildHeadPath(rect)
    val bodyPath = buildBodySilhouettePath(side, rect)
    drawPath(path = headPath, color = bodyFill)
    drawPath(path = headPath, color = bodyOutline, style = outlineStroke)
    drawPath(path = bodyPath, color = bodyFill)
    drawPath(path = bodyPath, color = bodyOutline, style = outlineStroke)

    // 2. Muscle regions (smooth closed Bezier contours)
    for (polygon in polygons) {
        if (polygon.muscle == selectedMuscle) continue
        musclePaints[polygon.muscle]?.let { paint ->
            drawMusclePolygon(polygon, paint, tokens, rect, scaleFactor)
        }
    }
    // Draw selected muscle last so its highlight stroke sits cleanly on top
    if (selectedMuscle != null) {
        val paint = musclePaints[selectedMuscle]
        if (paint != null) {
            for (polygon in polygons) {
                if (polygon.muscle == selectedMuscle) {
                    drawMusclePolygon(polygon, paint, tokens, rect, scaleFactor)
                }
            }
        }
    }

    // 3. Subtle anatomical detail accents (clavicle / abs / spine / knees)
    drawAnatomicalDetails(side, rect, detailLine, scaleFactor)
}

private fun DrawScope.drawMusclePolygon(
    polygon: MusclePolygon,
    paint: MusclePaint,
    tokens: ThemeTokens,
    rect: FigureRect,
    scaleFactor: Float,
) {
    val path = buildSmoothClosedPath(polygon.points, rect)
    drawPath(path = path, color = paint.fill)
    if (paint.hasSheen) {
        val bounds = path.getBounds()
        if (bounds.height > 0f) {
            val sheenColor = Color.White.copy(alpha = if (tokens.dark) 0.28f else 0.40f)
            val sheenBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to sheenColor,
                    0.48f to Color.Transparent,
                ),
                startY = bounds.top,
                endY = bounds.bottom,
            )
            clipPath(path) {
                drawPath(
                    path = path,
                    brush = sheenBrush,
                    style = Stroke(
                        width = 2.0.dp.toPx() * scaleFactor,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
    drawPath(
        path = path,
        color = paint.stroke,
        style = Stroke(
            width = paint.strokeWidthDp.dp.toPx() * scaleFactor,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun DrawScope.drawAnatomicalDetails(
    side: BodySide,
    rect: FigureRect,
    detailColor: Color,
    scaleFactor: Float,
) {
    val detailStroke = Stroke(
        width = 0.72.dp.toPx() * scaleFactor,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )
    fun px(nx: Float) = rect.left + nx * rect.width
    fun py(ny: Float) = rect.top + ny * rect.height

    val path = Path().apply {
        if (side == BodySide.FRONT) {
            // Subtle clavicle arcs
            moveTo(px(0.46f), py(0.205f))
            quadraticTo(px(0.39f), py(0.198f), px(0.32f), py(0.214f))
            moveTo(px(0.54f), py(0.205f))
            quadraticTo(px(0.61f), py(0.198f), px(0.68f), py(0.214f))
            // Subtle 6-pack segmentation lines inside ABS
            moveTo(px(0.50f), py(0.352f))
            lineTo(px(0.50f), py(0.495f))
            moveTo(px(0.405f), py(0.392f))
            lineTo(px(0.595f), py(0.392f))
            moveTo(px(0.415f), py(0.446f))
            lineTo(px(0.585f), py(0.446f))
            // Subtle kneecap arcs
            moveTo(px(0.348f), py(0.735f))
            quadraticTo(px(0.382f), py(0.745f), px(0.415f), py(0.735f))
            moveTo(px(0.652f), py(0.735f))
            quadraticTo(px(0.618f), py(0.745f), px(0.585f), py(0.735f))
        } else {
            // Subtle spine centerline from neck to lower back
            moveTo(px(0.50f), py(0.190f))
            lineTo(px(0.50f), py(0.482f))
            // Subtle scapular contour hints
            moveTo(px(0.415f), py(0.238f))
            quadraticTo(px(0.395f), py(0.265f), px(0.425f), py(0.292f))
            moveTo(px(0.585f), py(0.238f))
            quadraticTo(px(0.605f), py(0.265f), px(0.575f), py(0.292f))
            // Subtle back-of-knee creases
            moveTo(px(0.345f), py(0.740f))
            lineTo(px(0.415f), py(0.740f))
            moveTo(px(0.655f), py(0.740f))
            lineTo(px(0.585f), py(0.740f))
        }
    }
    drawPath(path = path, color = detailColor, style = detailStroke)
}

private fun buildHeadPath(rect: FigureRect): Path {
    fun px(nx: Float) = rect.left + nx * rect.width
    fun py(ny: Float) = rect.top + ny * rect.height
    return Path().apply {
        moveTo(px(0.500f), py(0.032f))
        cubicTo(
            px(0.562f), py(0.032f),
            px(0.586f), py(0.068f),
            px(0.576f), py(0.106f),
        )
        cubicTo(
            px(0.568f), py(0.132f),
            px(0.538f), py(0.148f),
            px(0.500f), py(0.148f),
        )
        cubicTo(
            px(0.462f), py(0.148f),
            px(0.432f), py(0.132f),
            px(0.424f), py(0.106f),
        )
        cubicTo(
            px(0.414f), py(0.068f),
            px(0.438f), py(0.032f),
            px(0.500f), py(0.032f),
        )
        close()
    }
}

/**
 * Builds a symmetrical, anatomically proportioned full-body silhouette (neck, shoulders,
 * arms, hands, torso, hips, legs, feet) mirrored cleanly around x = 0.50f.
 */
private fun buildBodySilhouettePath(side: BodySide, rect: FigureRect): Path {
    val groinY = if (side == BodySide.FRONT) 0.535f else 0.550f
    // Left half profile from top-center neck down to groin center
    val leftProfile = listOf(
        NormPoint(0.500f, 0.148f),
        NormPoint(0.448f, 0.148f),
        NormPoint(0.440f, 0.176f), // neck base
        NormPoint(0.352f, 0.194f), // trapezius slope
        NormPoint(0.248f, 0.206f), // shoulder top
        NormPoint(0.198f, 0.244f), // outer deltoid
        NormPoint(0.200f, 0.292f), // lower deltoid
        NormPoint(0.174f, 0.350f), // outer upper arm
        NormPoint(0.156f, 0.398f), // outer elbow
        NormPoint(0.124f, 0.446f), // outer forearm
        NormPoint(0.114f, 0.506f), // outer wrist
        NormPoint(0.092f, 0.544f), // hand outer edge
        NormPoint(0.128f, 0.568f), // fingertips
        NormPoint(0.172f, 0.538f), // thumb / inner hand
        NormPoint(0.176f, 0.506f), // inner wrist
        NormPoint(0.208f, 0.448f), // inner forearm
        NormPoint(0.240f, 0.398f), // inner elbow
        NormPoint(0.272f, 0.344f), // inner upper arm
        NormPoint(0.296f, 0.288f), // armpit
        NormPoint(0.320f, 0.352f), // ribcage
        NormPoint(0.344f, 0.422f), // waist
        NormPoint(0.308f, 0.494f), // hip flare
        NormPoint(0.294f, 0.546f), // upper outer thigh
        NormPoint(0.294f, 0.635f), // mid outer thigh
        NormPoint(0.324f, 0.728f), // outer knee
        NormPoint(0.324f, 0.758f), // lower outer knee
        NormPoint(0.302f, 0.816f), // outer calf swell
        NormPoint(0.338f, 0.924f), // outer ankle
        NormPoint(0.302f, 0.962f), // outer foot
        NormPoint(0.334f, 0.974f), // foot sole outer
        NormPoint(0.416f, 0.972f), // foot sole inner
        NormPoint(0.418f, 0.924f), // inner ankle
        NormPoint(0.446f, 0.812f), // inner calf swell
        NormPoint(0.436f, 0.756f), // lower inner knee
        NormPoint(0.436f, 0.728f), // inner knee
        NormPoint(0.458f, 0.638f), // inner thigh
        NormPoint(0.484f, 0.556f), // upper inner thigh
        NormPoint(0.500f, groinY), // groin / glute fold center
    )
    // Mirror right half from groin center back up to top-center neck (excluding shared endpoints)
    val rightProfile = leftProfile
        .subList(1, leftProfile.size - 1)
        .reversed()
        .map { NormPoint(1f - it.x, it.y) }

    return buildSmoothClosedPath(leftProfile + rightProfile, rect)
}

/**
 * Turns a closed list of normalized control points into an organic smooth Bezier [Path]
 * using midpoint-to-midpoint quadratic curves.
 */
private fun buildSmoothClosedPath(points: List<NormPoint>, rect: FigureRect): Path {
    val path = Path()
    val count = points.size
    if (count < 3) return path

    fun px(nx: Float) = rect.left + nx * rect.width
    fun py(ny: Float) = rect.top + ny * rect.height

    val last = points[count - 1]
    val first = points[0]
    path.moveTo(
        px((last.x + first.x) * 0.5f),
        py((last.y + first.y) * 0.5f),
    )
    for (i in 0 until count) {
        val current = points[i]
        val next = points[(i + 1) % count]
        path.quadraticTo(
            px(current.x),
            py(current.y),
            px((current.x + next.x) * 0.5f),
            py((current.y + next.y) * 0.5f),
        )
    }
    path.close()
    return path
}
