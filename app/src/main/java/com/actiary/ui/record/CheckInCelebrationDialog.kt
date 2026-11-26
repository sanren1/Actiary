package com.actiary.ui.record

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.domain.CheckInCelebrationData
import com.actiary.domain.CheckInKind
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.overlayPresent
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.motion.rememberMotionReduced
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DailyQuoteResIds = intArrayOf(
    R.string.checkin_daily_quote_0,
    R.string.checkin_daily_quote_1,
    R.string.checkin_daily_quote_2,
    R.string.checkin_daily_quote_3,
    R.string.checkin_daily_quote_4,
    R.string.checkin_daily_quote_5,
    R.string.checkin_daily_quote_6,
    R.string.checkin_daily_quote_7,
    R.string.checkin_daily_quote_8,
    R.string.checkin_daily_quote_9,
    R.string.checkin_daily_quote_10,
    R.string.checkin_daily_quote_11,
)

private val MilestoneQuoteResIds = intArrayOf(
    R.string.checkin_milestone_quote_0,
    R.string.checkin_milestone_quote_1,
    R.string.checkin_milestone_quote_2,
    R.string.checkin_milestone_quote_3,
    R.string.checkin_milestone_quote_4,
    R.string.checkin_milestone_quote_5,
)

private enum class BurstColorRole { Yellow, Orange, Accent }

private data class BurstParticle(
    val angleDeg: Float,
    val startDistNorm: Float,
    val endDistNorm: Float,
    val sizeDp: Float,
    val isStar: Boolean,
    val colorRole: BurstColorRole,
)

private data class TwinkleStar(
    val xNorm: Float,
    val yNorm: Float,
    val radiusDp: Float,
    val phaseOffset: Float,
    val colorRole: BurstColorRole,
)

private val FlameBurstParticles = listOf(
    BurstParticle(angleDeg = -138f, startDistNorm = 0.14f, endDistNorm = 0.44f, sizeDp = 4.6f, isStar = true, colorRole = BurstColorRole.Yellow),
    BurstParticle(angleDeg = -42f, startDistNorm = 0.14f, endDistNorm = 0.45f, sizeDp = 4.8f, isStar = true, colorRole = BurstColorRole.Orange),
    BurstParticle(angleDeg = -112f, startDistNorm = 0.15f, endDistNorm = 0.46f, sizeDp = 2.3f, isStar = false, colorRole = BurstColorRole.Accent),
    BurstParticle(angleDeg = -68f, startDistNorm = 0.15f, endDistNorm = 0.44f, sizeDp = 2.2f, isStar = false, colorRole = BurstColorRole.Yellow),
    BurstParticle(angleDeg = -166f, startDistNorm = 0.16f, endDistNorm = 0.42f, sizeDp = 3.6f, isStar = true, colorRole = BurstColorRole.Accent),
    BurstParticle(angleDeg = -14f, startDistNorm = 0.16f, endDistNorm = 0.43f, sizeDp = 3.8f, isStar = true, colorRole = BurstColorRole.Yellow),
    BurstParticle(angleDeg = -88f, startDistNorm = 0.18f, endDistNorm = 0.46f, sizeDp = 2.0f, isStar = false, colorRole = BurstColorRole.Orange),
)

private val CornerTwinkleStars = listOf(
    TwinkleStar(xNorm = 0.19f, yNorm = 0.24f, radiusDp = 4.0f, phaseOffset = 0.00f, colorRole = BurstColorRole.Yellow),
    TwinkleStar(xNorm = 0.81f, yNorm = 0.21f, radiusDp = 4.2f, phaseOffset = 0.34f, colorRole = BurstColorRole.Orange),
    TwinkleStar(xNorm = 0.29f, yNorm = 0.13f, radiusDp = 3.0f, phaseOffset = 0.67f, colorRole = BurstColorRole.Accent),
)

private val FlameOuterTop = Color(0xFFFF9600)
private val FlameOuterBottom = Color(0xFFFF7A00)
private val FlameInnerBottom = Color(0xFFFFC800)
private val FlameInnerTop = Color(0xFFFFE24D)
private val FlameCoreCream = Color(0xFFFFF3B0)

@Composable
fun CheckInCelebrationDialog(
    data: CheckInCelebrationData?,
    visibility: MutableTransitionState<Boolean>,
    onDismiss: () -> Unit,
) {
    var held by remember { mutableStateOf<CheckInCelebrationData?>(null) }
    if (data != null) held = data
    LaunchedEffect(data, visibility.overlayPresent) {
        if (data == null && !visibility.overlayPresent) {
            held = null
        }
    }
    val shown = data ?: held

    val view = LocalView.current
    LaunchedEffect(data) {
        if (data != null) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    val title = shown?.let { celebrationTitle(it.kind) }.orEmpty()
    GlassOverlayPanel(
        visible = data != null,
        onDismiss = onDismiss,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        title = title,
        visibility = visibility,
    ) {
        if (shown != null) {
            val t = LocalThemeTokens.current
            val primary = t.neutrals.textPrimary.asColor()
            val secondary = t.neutrals.textSecondary.asColor()
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(if (shown.kind is CheckInKind.Daily) 84.dp else 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when (shown.kind) {
                        CheckInKind.Daily -> DailyCheckEmblem(trigger = data)
                        is CheckInKind.TotalMilestone,
                        is CheckInKind.StreakMilestone,
                        -> MilestoneFlameEmblem(trigger = data)
                    }
                }
                BasicText(
                    text = title,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .semantics { heading() },
                    style = TextStyle(
                        color = primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                )
                BasicText(
                    text = celebrationQuote(shown.kind, shown.quoteIndex),
                    modifier = Modifier.padding(top = 8.dp),
                    style = TextStyle(
                        color = secondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(
                        value = stringResource(R.string.checkin_stat_days_value, shown.streakDays),
                        label = stringResource(R.string.checkin_stat_streak),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stringResource(R.string.checkin_stat_days_value, shown.weekDays),
                        label = stringResource(R.string.checkin_stat_week),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = stringResource(R.string.checkin_stat_days_value, shown.totalDays),
                        label = stringResource(R.string.checkin_stat_total),
                        modifier = Modifier.weight(1f),
                    )
                }
                val summary = shown.workoutSummary?.takeIf { it.isNotBlank() }
                if (summary != null) {
                    BasicText(
                        text = summary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        style = TextStyle(
                            color = secondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(t.accent.accentAction.asColor())
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = stringResource(R.string.checkin_action_continue),
                        style = TextStyle(
                            color = t.accent.onAccentAction.asColor(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val n = LocalThemeTokens.current.neutrals
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(n.surfaceMuted.asColor())
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = value,
            style = TextStyle(
                color = n.textPrimary.asColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = n.textSecondary.asColor(),
                fontSize = 12.sp,
            ),
            maxLines = 1,
        )
    }
}

@Composable
private fun DailyCheckEmblem(trigger: CheckInCelebrationData?) {
    val accent = LocalThemeTokens.current.accent.accentAction.asColor()
    val reduced = rememberMotionReduced()
    val progress = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(trigger, reduced) {
        if (trigger != null) {
            if (reduced) {
                progress.snapTo(1f)
            } else {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 680, easing = LinearEasing),
                )
            }
        }
    }

    val checkPath = remember { Path() }
    val segmentPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    Canvas(Modifier.size(84.dp)) {
        val p = progress.value
        val ringRadius = size.minDimension * 0.34f

        val rippleRaw = ((p - 0.10f) / 0.90f).coerceIn(0f, 1f)
        if (!reduced && rippleRaw > 0f && rippleRaw < 1f) {
            val rippleProgress = FastOutSlowInEasing.transform(rippleRaw)
            val rippleRadius = ringRadius + (size.minDimension * 0.49f - ringRadius) * rippleProgress
            val rippleAlpha = 0.35f * (1f - rippleProgress)
            drawCircle(
                color = accent.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        val ringSweepFraction = if (reduced) 1f else FastOutSlowInEasing.transform((p / 0.62f).coerceIn(0f, 1f))
        if (ringSweepFraction > 0f) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * ringSweepFraction,
                useCenter = false,
                topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                size = Size(ringRadius * 2f, ringRadius * 2f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        val checkFraction = if (reduced) 1f else FastOutSlowInEasing.transform(((p - 0.38f) / 0.62f).coerceIn(0f, 1f))
        if (checkFraction > 0f) {
            checkPath.reset()
            checkPath.moveTo(center.x - ringRadius * 0.40f, center.y + ringRadius * 0.04f)
            checkPath.lineTo(center.x - ringRadius * 0.09f, center.y + ringRadius * 0.34f)
            checkPath.lineTo(center.x + ringRadius * 0.44f, center.y - ringRadius * 0.30f)

            pathMeasure.setPath(checkPath, false)
            segmentPath.reset()
            pathMeasure.getSegment(0f, pathMeasure.length * checkFraction, segmentPath, true)
            drawPath(
                path = segmentPath,
                color = accent,
                style = Stroke(
                    width = 3.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
private fun MilestoneFlameEmblem(trigger: CheckInCelebrationData?) {
    val accentAction = LocalThemeTokens.current.accent.accentAction.asColor()
    val reduced = rememberMotionReduced()
    val introProgress = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(trigger, reduced) {
        if (trigger != null) {
            if (reduced) {
                introProgress.snapTo(1f)
            } else {
                introProgress.snapTo(0f)
                introProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 620, easing = LinearEasing),
                )
            }
        }
    }

    val phase = if (reduced) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "flameLiving")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "flamePhase",
        ).value
    }

    val outerFlamePath = remember { Path() }
    val innerFlamePath = remember { Path() }
    val coreFlamePath = remember { Path() }
    val starPath = remember { Path() }

    Canvas(Modifier.size(96.dp)) {
        val p = introProgress.value
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val outerBaseY = h * 0.86f
        val innerBaseY = h * 0.82f
        val twoPi = (2.0 * PI).toFloat()

        // Gentle rhythmic tip sway + volume-preserving breath (scaleY 0.985..1.025, scaleX 1.015..0.98)
        val breathSin = if (reduced) 0f else sin(phase * twoPi)
        val breathScaleY = if (reduced) 1f else 1.005f + 0.020f * breathSin
        val breathScaleX = if (reduced) 1f else 0.9975f - 0.0175f * breathSin
        val tipSway = if (reduced) 0f else sin(phase * twoPi) * w * 0.020f
        val lickSway = if (reduced) 0f else sin((phase + 0.25f) * twoPi) * w * 0.010f
        val innerTipSway = if (reduced) 0f else sin((phase + 0.32f) * twoPi) * w * 0.014f

        // Snappy squash-and-stretch ignition for outer flame (pivot at baseY = 0.86 * h)
        val (outerEntryScaleX, outerEntryScaleY) = if (reduced) {
            1f to 1f
        } else {
            squashBounceScale(
                t = p,
                startScaleX = 0.55f,
                startScaleY = 0.25f,
                peakScaleX = 0.92f,
                peakScaleY = 1.12f,
                squashScaleX = 1.04f,
                squashScaleY = 0.96f,
                peakAt = 0.42f,
                squashAt = 0.72f,
            )
        }
        val outerAlpha = if (reduced) 1f else (p / 0.16f).coerceIn(0f, 1f)

        // Inner yellow flame pops in ~70ms later (0.113 of 620ms) with its own crisp bounce
        val innerP = if (reduced) 1f else ((p - 0.113f) / 0.887f).coerceIn(0f, 1f)
        val (innerEntryScaleX, innerEntryScaleY) = if (reduced) {
            1f to 1f
        } else {
            squashBounceScale(
                t = innerP,
                startScaleX = 0.12f,
                startScaleY = 0.12f,
                peakScaleX = 0.93f,
                peakScaleY = 1.10f,
                squashScaleX = 1.04f,
                squashScaleY = 0.96f,
                peakAt = 0.48f,
                squashAt = 0.78f,
            )
        }
        val innerAlpha = if (reduced) 1f else (innerP / 0.18f).coerceIn(0f, 1f)

        // 1. Grounded subtle oval pill shadow directly below the base (widens slightly when squashing)
        val totalOuterScaleX = outerEntryScaleX * breathScaleX
        val shadowWidth = w * 0.42f * totalOuterScaleX.coerceAtLeast(0.55f)
        val shadowHeight = h * 0.062f
        val shadowAlpha = 0.14f * outerAlpha
        if (shadowAlpha > 0f) {
            drawOval(
                color = Color.Black.copy(alpha = shadowAlpha),
                topLeft = Offset(cx - shadowWidth * 0.5f, h * 0.90f - shadowHeight * 0.5f),
                size = Size(shadowWidth, shadowHeight),
            )
        }

        // 2. Outer Flame (Vibrant Streak Orange #FF9600 -> #FF7A00)
        buildDuolingoOuterFlamePath(
            path = outerFlamePath,
            w = w,
            h = h,
            baseY = outerBaseY,
            tipSway = tipSway,
            lickSway = lickSway,
        )
        val outerBrush = Brush.verticalGradient(
            colors = listOf(
                FlameOuterTop.copy(alpha = outerAlpha),
                FlameOuterBottom.copy(alpha = outerAlpha),
            ),
            startY = h * 0.14f,
            endY = outerBaseY,
        )
        if (outerAlpha > 0f) {
            withTransform({
                scale(
                    scaleX = totalOuterScaleX,
                    scaleY = outerEntryScaleY * breathScaleY,
                    pivot = Offset(cx, outerBaseY),
                )
            }) {
                drawPath(path = outerFlamePath, brush = outerBrush)
            }
        }

        // 3. Inner Flame (#FFC800 -> #FFE24D) & Warm Cream Core (#FFF3B0)
        if (innerAlpha > 0f) {
            buildDuolingoInnerFlamePath(
                path = innerFlamePath,
                w = w,
                h = h,
                baseY = innerBaseY,
                tipSway = innerTipSway,
            )
            buildDuolingoCorePath(
                path = coreFlamePath,
                w = w,
                h = h,
                tipSway = innerTipSway * 0.45f,
            )
            val innerBrush = Brush.verticalGradient(
                colors = listOf(
                    FlameInnerTop.copy(alpha = innerAlpha),
                    FlameInnerBottom.copy(alpha = innerAlpha),
                ),
                startY = h * 0.44f,
                endY = innerBaseY,
            )
            withTransform({
                scale(
                    scaleX = innerEntryScaleX * breathScaleX,
                    scaleY = innerEntryScaleY * breathScaleY,
                    pivot = Offset(cx, innerBaseY),
                )
            }) {
                drawPath(path = innerFlamePath, brush = innerBrush)
                drawPath(path = coreFlamePath, color = FlameCoreCream.copy(alpha = innerAlpha))
            }
        }

        // 4. Crisp flat-vector radial burst particles during entry (0.20..0.95)
        if (!reduced && p in 0.20f..0.95f) {
            val burstT = (p - 0.20f) / 0.75f
            val distEase = 1f - (1f - burstT) * (1f - burstT)
            val burstScale = if (burstT < 0.22f) {
                burstT / 0.22f
            } else {
                1f - (burstT - 0.22f) / 0.78f
            }.coerceIn(0f, 1f)

            if (burstScale > 0.01f) {
                val originY = h * 0.52f
                for (particle in FlameBurstParticles) {
                    val rad = particle.angleDeg * (PI / 180.0).toFloat()
                    val dist = w * lerpFloat(particle.startDistNorm, particle.endDistNorm, distEase)
                    val px = cx + cos(rad) * dist
                    val py = originY + sin(rad) * dist
                    val color = resolveBurstColor(particle.colorRole, accentAction)
                    val r = particle.sizeDp.dp.toPx() * burstScale
                    if (particle.isStar) {
                        buildFourPointStarPath(starPath, px, py, r)
                        drawPath(path = starPath, color = color)
                    } else {
                        drawCircle(color = color, radius = r, center = Offset(px, py))
                    }
                }
            }
        }

        // 5. Three crisp 4-point stars near the top corners (static under reduced motion, twinkling when idle)
        val twinkleIntro = if (reduced) 1f else ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
        if (twinkleIntro > 0f) {
            for (star in CornerTwinkleStars) {
                val pulse = if (reduced) {
                    1f
                } else {
                    0.5f + 0.5f * sin((phase + star.phaseOffset) * twoPi)
                }
                val starScale = if (reduced) 1f else 0.65f + 0.35f * pulse
                val starAlpha = if (reduced) 0.92f else (0.45f + 0.55f * pulse) * twinkleIntro
                val color = resolveBurstColor(star.colorRole, accentAction).copy(alpha = starAlpha)
                buildFourPointStarPath(
                    path = starPath,
                    cx = w * star.xNorm,
                    cy = h * star.yNorm,
                    radius = star.radiusDp.dp.toPx() * starScale,
                )
                drawPath(path = starPath, color = color)
            }
        }
    }
}

private fun resolveBurstColor(role: BurstColorRole, accentAction: Color): Color = when (role) {
    BurstColorRole.Yellow -> FlameInnerBottom
    BurstColorRole.Orange -> FlameOuterTop
    BurstColorRole.Accent -> accentAction
}

private fun lerpFloat(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

private fun smoothCos(fraction: Float): Float =
    0.5f - 0.5f * cos(fraction.coerceIn(0f, 1f) * PI).toFloat()

private fun squashBounceScale(
    t: Float,
    startScaleX: Float,
    startScaleY: Float,
    peakScaleX: Float,
    peakScaleY: Float,
    squashScaleX: Float,
    squashScaleY: Float,
    peakAt: Float,
    squashAt: Float,
): Pair<Float, Float> {
    val clamped = t.coerceIn(0f, 1f)
    return when {
        clamped <= peakAt -> {
            val u = FastOutSlowInEasing.transform(clamped / peakAt)
            lerpFloat(startScaleX, peakScaleX, u) to lerpFloat(startScaleY, peakScaleY, u)
        }
        clamped <= squashAt -> {
            val u = smoothCos((clamped - peakAt) / (squashAt - peakAt))
            lerpFloat(peakScaleX, squashScaleX, u) to lerpFloat(peakScaleY, squashScaleY, u)
        }
        else -> {
            val u = smoothCos((clamped - squashAt) / (1f - squashAt))
            lerpFloat(squashScaleX, 1f, u) to lerpFloat(squashScaleY, 1f, u)
        }
    }
}

private fun buildDuolingoOuterFlamePath(
    path: Path,
    w: Float,
    h: Float,
    baseY: Float,
    tipSway: Float,
    lickSway: Float,
) {
    val cx = w * 0.50f
    val mainTipX = w * 0.52f + tipSway
    val mainTipY = h * 0.14f
    path.reset()
    path.moveTo(cx, baseY)
    // Plump lower-left bowl (x: 0.50 -> 0.24)
    path.cubicTo(
        w * 0.34f, baseY,
        w * 0.24f, h * 0.77f,
        w * 0.24f, h * 0.64f,
    )
    // Upper-left rounded side-lick (x ≈ 0.30, y ≈ 0.42)
    path.cubicTo(
        w * 0.24f, h * 0.53f,
        w * 0.265f + lickSway * 0.5f, h * 0.42f,
        w * 0.30f + lickSway, h * 0.42f,
    )
    // Smooth tuck into S-curve left edge
    path.cubicTo(
        w * 0.325f + lickSway * 0.6f, h * 0.42f,
        w * 0.345f, h * 0.465f,
        w * 0.37f + tipSway * 0.2f, h * 0.46f,
    )
    // Bold S-curve rising to main flame tip (x ≈ 0.52, y ≈ 0.14)
    path.cubicTo(
        w * 0.40f + tipSway * 0.35f, h * 0.44f,
        w * 0.45f + tipSway * 0.75f, h * 0.17f,
        mainTipX, mainTipY,
    )
    // Full plump rounded right curve
    path.cubicTo(
        w * 0.56f + tipSway * 0.80f, h * 0.155f,
        w * 0.64f + tipSway * 0.35f, h * 0.28f,
        w * 0.675f + tipSway * 0.15f, h * 0.40f,
    )
    path.cubicTo(
        w * 0.71f, h * 0.52f,
        w * 0.76f, h * 0.55f,
        w * 0.76f, h * 0.64f,
    )
    path.cubicTo(
        w * 0.76f, h * 0.77f,
        w * 0.66f, baseY,
        cx, baseY,
    )
    path.close()
}

private fun buildDuolingoInnerFlamePath(
    path: Path,
    w: Float,
    h: Float,
    baseY: Float,
    tipSway: Float,
) {
    val cx = w * 0.50f
    val tipX = w * 0.515f + tipSway
    val tipY = h * 0.44f
    path.reset()
    path.moveTo(cx, baseY)
    path.cubicTo(
        w * 0.40f, baseY,
        w * 0.35f, h * 0.75f,
        w * 0.35f, h * 0.67f,
    )
    path.cubicTo(
        w * 0.35f, h * 0.57f,
        w * 0.44f + tipSway * 0.5f, h * 0.48f,
        tipX, tipY,
    )
    path.cubicTo(
        w * 0.56f + tipSway * 0.5f, h * 0.48f,
        w * 0.65f, h * 0.57f,
        w * 0.65f, h * 0.67f,
    )
    path.cubicTo(
        w * 0.65f, h * 0.75f,
        w * 0.60f, baseY,
        cx, baseY,
    )
    path.close()
}

private fun buildDuolingoCorePath(
    path: Path,
    w: Float,
    h: Float,
    tipSway: Float,
) {
    val cx = w * 0.50f
    val baseY = h * 0.80f
    val tipX = w * 0.505f + tipSway
    val tipY = h * 0.62f
    path.reset()
    path.moveTo(cx, baseY)
    path.cubicTo(
        w * 0.455f, baseY,
        w * 0.43f, h * 0.77f,
        w * 0.43f, h * 0.73f,
    )
    path.cubicTo(
        w * 0.43f, h * 0.68f,
        w * 0.475f + tipSway * 0.4f, h * 0.64f,
        tipX, tipY,
    )
    path.cubicTo(
        w * 0.535f + tipSway * 0.4f, h * 0.64f,
        w * 0.57f, h * 0.68f,
        w * 0.57f, h * 0.73f,
    )
    path.cubicTo(
        w * 0.57f, h * 0.77f,
        w * 0.545f, baseY,
        cx, baseY,
    )
    path.close()
}

private fun buildFourPointStarPath(
    path: Path,
    cx: Float,
    cy: Float,
    radius: Float,
) {
    val inset = radius * 0.20f
    path.reset()
    path.moveTo(cx, cy - radius)
    path.quadraticTo(cx + inset, cy - inset, cx + radius, cy)
    path.quadraticTo(cx + inset, cy + inset, cx, cy + radius)
    path.quadraticTo(cx - inset, cy + inset, cx - radius, cy)
    path.quadraticTo(cx - inset, cy - inset, cx, cy - radius)
    path.close()
}

@Composable
private fun celebrationTitle(kind: CheckInKind): String = when (kind) {
    CheckInKind.Daily -> stringResource(R.string.checkin_title_daily)
    is CheckInKind.TotalMilestone -> if (kind.days == 1) {
        stringResource(R.string.checkin_title_first_day)
    } else {
        stringResource(R.string.checkin_title_total_milestone, kind.days)
    }
    is CheckInKind.StreakMilestone -> stringResource(R.string.checkin_title_streak_milestone, kind.days)
}

@Composable
private fun celebrationQuote(kind: CheckInKind, quoteIndex: Int): String = when (kind) {
    CheckInKind.Daily -> stringResource(DailyQuoteResIds[quoteIndex.mod(DailyQuoteResIds.size)])
    is CheckInKind.TotalMilestone -> if (kind.days == 1) {
        stringResource(R.string.checkin_quote_first_day)
    } else {
        stringResource(MilestoneQuoteResIds[quoteIndex.mod(MilestoneQuoteResIds.size)])
    }
    is CheckInKind.StreakMilestone -> stringResource(MilestoneQuoteResIds[quoteIndex.mod(MilestoneQuoteResIds.size)])
}
