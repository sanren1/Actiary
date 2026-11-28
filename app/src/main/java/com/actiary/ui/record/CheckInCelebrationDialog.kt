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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.UiStyle
import com.actiary.domain.CheckInCelebrationData
import com.actiary.domain.CheckInKind
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.overlayPresent
import com.actiary.p5.P5RansomTitle
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Surface
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
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
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    GlassOverlayPanel(
        visible = data != null,
        onDismiss = onDismiss,
        contentPadding = if (isP5) PaddingValues(0.dp) else PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        title = title,
        visibility = visibility,
    ) {
        if (shown != null) {
            if (isP5) {
                P5CheckInCelebrationContent(
                    shown = shown,
                    trigger = data,
                    title = title,
                    onDismiss = onDismiss,
                )
            } else {
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
}

private data class P5ShardParticle(
    val xNorm: Float,
    val yNorm: Float,
    val radiusDp: Float,
    val angleDeg: Float,
    val isStar: Boolean,
    val phaseOffset: Float,
    val useHighlight: Boolean,
)

private val P5BurstShards = listOf(
    P5ShardParticle(xNorm = 0.14f, yNorm = 0.12f, radiusDp = 8.5f, angleDeg = -18f, isStar = true, phaseOffset = 0.00f, useHighlight = true),
    P5ShardParticle(xNorm = 0.86f, yNorm = 0.11f, radiusDp = 9.0f, angleDeg = 22f, isStar = true, phaseOffset = 0.25f, useHighlight = false),
    P5ShardParticle(xNorm = 0.24f, yNorm = 0.26f, radiusDp = 6.0f, angleDeg = 35f, isStar = false, phaseOffset = 0.50f, useHighlight = false),
    P5ShardParticle(xNorm = 0.78f, yNorm = 0.27f, radiusDp = 6.5f, angleDeg = -28f, isStar = false, phaseOffset = 0.75f, useHighlight = true),
    P5ShardParticle(xNorm = 0.10f, yNorm = 0.36f, radiusDp = 5.0f, angleDeg = 12f, isStar = false, phaseOffset = 0.40f, useHighlight = true),
    P5ShardParticle(xNorm = 0.90f, yNorm = 0.34f, radiusDp = 5.5f, angleDeg = -14f, isStar = true, phaseOffset = 0.65f, useHighlight = false),
)

@Composable
private fun P5CheckInCelebrationContent(
    shown: CheckInCelebrationData,
    trigger: CheckInCelebrationData?,
    title: String,
    onDismiss: () -> Unit,
) {
    val p5 = LocalP5Tokens.current
    val skipLoopMotion = p5.motionLevel == P5MotionLevel.COMPACT || rememberMotionReduced()
    val introProgress = remember { Animatable(if (skipLoopMotion) 1f else 0f) }

    LaunchedEffect(trigger, skipLoopMotion) {
        if (trigger != null) {
            if (skipLoopMotion) {
                introProgress.snapTo(1f)
            } else {
                introProgress.snapTo(0f)
                introProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                )
            }
        }
    }

    val loopPhase = if (skipLoopMotion) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "p5CelebrationBurst")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "p5BurstPhase",
        ).value
    }

    val brand = p5.brandFill.asColor()
    val darkCard = p5.darkCard.asColor()
    val lightCard = p5.lightCard.asColor()
    val strokeLight = p5.strokeLight.asColor()
    val strokeDark = p5.strokeDark.asColor()
    val highlight = p5.highlightBadge.asColor()
    val stripePath = remember { Path() }
    val shardPath = remember { Path() }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Diagonal crimson-black burst stripes + sharp star/diamond burst particles
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            // Base dark fill with slanted crimson burst stripes
            drawRect(color = darkCard)
            val stripeWidth = 28.dp.toPx()
            val slant = h * 0.42f
            val stripeColor = brand.copy(alpha = 0.24f)
            var x = -slant
            var index = 0
            while (x < w + slant) {
                if (index % 2 == 0) {
                    stripePath.reset()
                    stripePath.moveTo(x, h)
                    stripePath.lineTo(x + stripeWidth, h)
                    stripePath.lineTo(x + stripeWidth + slant, 0f)
                    stripePath.lineTo(x + slant, 0f)
                    stripePath.close()
                    drawPath(path = stripePath, color = stripeColor)
                }
                x += stripeWidth
                index++
            }

            // Sharp radial burst wedge behind the emblem
            val cx = w * 0.5f
            val cy = h * 0.20f
            val burstAlpha = 0.30f * introProgress.value
            val wedgeCount = 8
            val outerR = maxOf(w, h) * 0.45f * introProgress.value
            for (i in 0 until wedgeCount) {
                val baseAngle = (i * (360f / wedgeCount) + if (skipLoopMotion) 0f else loopPhase * 15f)
                val a1 = (baseAngle - 6f) * (PI / 180.0).toFloat()
                val a2 = (baseAngle + 6f) * (PI / 180.0).toFloat()
                stripePath.reset()
                stripePath.moveTo(cx, cy)
                stripePath.lineTo(cx + cos(a1) * outerR, cy + sin(a1) * outerR)
                stripePath.lineTo(cx + cos(a2) * outerR, cy + sin(a2) * outerR)
                stripePath.close()
                drawPath(path = stripePath, color = brand.copy(alpha = burstAlpha))
            }

            // Sharp 4-point stars and diamond burst particles
            val twoPi = (2.0 * PI).toFloat()
            val p = introProgress.value
            for (shard in P5BurstShards) {
                val pulse = if (skipLoopMotion) 1f else 0.78f + 0.22f * sin((loopPhase + shard.phaseOffset) * twoPi)
                val sx = lerpFloat(cx, w * shard.xNorm, p)
                val sy = lerpFloat(cy, h * shard.yNorm, p)
                val r = shard.radiusDp.dp.toPx() * pulse * p
                if (r <= 0.5f) continue
                val shardColor = if (shard.useHighlight) highlight else strokeLight
                withTransform({
                    rotate(degrees = shard.angleDeg, pivot = Offset(sx, sy))
                }) {
                    if (shard.isStar) {
                        buildSharpFourPointStarPath(shardPath, sx, sy, r)
                    } else {
                        buildSharpDiamondPath(shardPath, sx, sy, r)
                    }
                    drawPath(path = shardPath, color = shardColor)
                    drawPath(
                        path = shardPath,
                        color = strokeDark,
                        style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(if (shown.kind is CheckInKind.Daily) 84.dp else 96.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (shown.kind) {
                    CheckInKind.Daily -> DailyCheckEmblem(trigger = trigger)
                    is CheckInKind.TotalMilestone,
                    is CheckInKind.StreakMilestone,
                    -> MilestoneFlameEmblem(trigger = trigger)
                }
            }
            P5RansomTitle(
                title = title,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .semantics { heading() },
            )
            BasicText(
                text = celebrationQuote(shown.kind, shown.quoteIndex),
                modifier = Modifier.padding(top = 10.dp),
                style = TextStyle(
                    color = strokeLight.copy(alpha = 0.92f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
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
                P5StatCard(
                    value = stringResource(R.string.checkin_stat_days_value, shown.streakDays),
                    label = stringResource(R.string.checkin_stat_streak),
                    modifier = Modifier.weight(1f),
                )
                P5StatCard(
                    value = stringResource(R.string.checkin_stat_days_value, shown.weekDays),
                    label = stringResource(R.string.checkin_stat_week),
                    modifier = Modifier.weight(1f),
                )
                P5StatCard(
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
                        color = strokeLight.copy(alpha = 0.78f),
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
                    .p5Surface(
                        variant = P5ShapeVariant.PILL,
                        fillColor = brand,
                        strokeColor = lightCard,
                        shadowColor = strokeDark,
                        strokeWidth = 2.dp,
                        shadowOffset = DpOffset(3.dp, 3.dp),
                    )
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = stringResource(R.string.checkin_action_continue),
                    style = TextStyle(
                        color = p5.onBrandFill.asColor(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                    ),
                )
            }
        }
    }
}

@Composable
private fun P5StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val p5 = LocalP5Tokens.current
    Column(
        modifier = modifier
            .p5Surface(
                variant = P5ShapeVariant.CARD,
                fillColor = p5.lightCard.asColor(),
                strokeColor = p5.strokeDark.asColor(),
                shadowColor = p5.shadowColor.asColor(),
                strokeWidth = 2.dp,
                shadowOffset = DpOffset(3.dp, 3.dp),
            )
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        BasicText(
            text = value,
            style = TextStyle(
                color = p5.onLightCard.asColor(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            ),
            maxLines = 1,
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = p5.onLightCardMuted.asColor(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
    }
}

private fun buildSharpFourPointStarPath(
    path: Path,
    cx: Float,
    cy: Float,
    radius: Float,
) {
    val inset = radius * 0.28f
    path.reset()
    path.moveTo(cx, cy - radius)
    path.lineTo(cx + inset, cy - inset)
    path.lineTo(cx + radius, cy)
    path.lineTo(cx + inset, cy + inset)
    path.lineTo(cx, cy + radius)
    path.lineTo(cx - inset, cy + inset)
    path.lineTo(cx - radius, cy)
    path.lineTo(cx - inset, cy - inset)
    path.close()
}

private fun buildSharpDiamondPath(
    path: Path,
    cx: Float,
    cy: Float,
    radius: Float,
) {
    path.reset()
    path.moveTo(cx, cy - radius)
    path.lineTo(cx + radius * 0.65f, cy)
    path.lineTo(cx, cy + radius)
    path.lineTo(cx - radius * 0.65f, cy)
    path.close()
}

private data class P5CheckShard(
    val xDir: Float,
    val yDir: Float,
    val distNorm: Float,
    val radiusDp: Float,
    val angleDeg: Float,
    val useHighlight: Boolean,
)

private val P5CheckCornerShards = listOf(
    P5CheckShard(xDir = -0.82f, yDir = -0.78f, distNorm = 0.43f, radiusDp = 6.5f, angleDeg = -18f, useHighlight = true),
    P5CheckShard(xDir = 0.84f, yDir = -0.76f, distNorm = 0.44f, radiusDp = 7.0f, angleDeg = 22f, useHighlight = false),
    P5CheckShard(xDir = -0.80f, yDir = 0.78f, distNorm = 0.42f, radiusDp = 5.5f, angleDeg = 16f, useHighlight = false),
    P5CheckShard(xDir = 0.82f, yDir = 0.74f, distNorm = 0.43f, radiusDp = 6.0f, angleDeg = -24f, useHighlight = true),
)

private data class P5FlameSpark(
    val xNorm: Float,
    val yNorm: Float,
    val riseNorm: Float,
    val radiusDp: Float,
    val angleDeg: Float,
    val phaseOffset: Float,
    val isStar: Boolean,
    val useCoreColor: Boolean,
)

private val P5FlameSparks = listOf(
    P5FlameSpark(xNorm = 0.17f, yNorm = 0.28f, riseNorm = 0.08f, radiusDp = 6.2f, angleDeg = -20f, phaseOffset = 0.00f, isStar = true, useCoreColor = false),
    P5FlameSpark(xNorm = 0.84f, yNorm = 0.24f, riseNorm = 0.09f, radiusDp = 6.6f, angleDeg = 18f, phaseOffset = 0.22f, isStar = true, useCoreColor = true),
    P5FlameSpark(xNorm = 0.14f, yNorm = 0.58f, riseNorm = 0.07f, radiusDp = 4.6f, angleDeg = -12f, phaseOffset = 0.48f, isStar = false, useCoreColor = true),
    P5FlameSpark(xNorm = 0.87f, yNorm = 0.54f, riseNorm = 0.07f, radiusDp = 4.8f, angleDeg = 24f, phaseOffset = 0.68f, isStar = false, useCoreColor = false),
    P5FlameSpark(xNorm = 0.34f, yNorm = 0.13f, riseNorm = 0.06f, radiusDp = 5.2f, angleDeg = 10f, phaseOffset = 0.84f, isStar = true, useCoreColor = false),
)

@Composable
private fun P5DailyCheckEmblem(trigger: CheckInCelebrationData?) {
    val p5 = LocalP5Tokens.current
    val reduced = rememberMotionReduced() || p5.motionLevel == P5MotionLevel.COMPACT
    val progress = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(trigger, reduced) {
        if (trigger != null) {
            if (reduced) {
                progress.snapTo(1f)
            } else {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = LinearEasing),
                )
            }
        }
    }

    val brand = p5.brandFill.asColor()
    val onBrand = p5.onBrandFill.asColor()
    val strokeDark = p5.strokeDark.asColor()
    val strokeLight = p5.strokeLight.asColor()
    val highlight = p5.highlightBadge.asColor()
    val checkShadowColor = if (p5.palette == P5Palette.ROYAL_GOLD) strokeLight else strokeDark

    val badgePath = remember { Path() }
    val shockPath = remember { Path() }
    val checkPath = remember { Path() }
    val segmentPath = remember { Path() }
    val shardPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    Canvas(Modifier.size(84.dp)) {
        val p = progress.value
        val minDim = size.minDimension
        val cx = center.x
        val cy = center.y
        val badgeRadius = minDim * 0.36f

        // 1. Expanding sharp diamond shockwave frame during intro
        val shockRaw = ((p - 0.06f) / 0.84f).coerceIn(0f, 1f)
        if (!reduced && shockRaw > 0f && shockRaw < 1f) {
            val shockEase = FastOutSlowInEasing.transform(shockRaw)
            val shockRadius = lerpFloat(badgeRadius * 0.92f, minDim * 0.50f, shockEase)
            val shockAlpha = 0.85f * (1f - shockEase)
            buildP5ShockDiamondPath(shockPath, cx, cy, shockRadius)
            withTransform({
                rotate(degrees = -7f, pivot = center)
            }) {
                drawPath(
                    path = shockPath,
                    color = highlight.copy(alpha = shockAlpha),
                    style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Miter),
                )
            }
        }

        // 2. Snappy slam-bounce skewed jagged comic stamp backing
        val (badgeScaleX, badgeScaleY) = if (reduced) {
            1f to 1f
        } else {
            squashBounceScale(
                t = p,
                startScaleX = 0.45f,
                startScaleY = 0.45f,
                peakScaleX = 1.15f,
                peakScaleY = 1.13f,
                squashScaleX = 0.94f,
                squashScaleY = 0.96f,
                peakAt = 0.36f,
                squashAt = 0.66f,
            )
        }
        val badgeAlpha = if (reduced) 1f else (p / 0.14f).coerceIn(0f, 1f)

        if (badgeAlpha > 0f) {
            buildP5StampBadgePath(badgePath, cx, cy, badgeRadius)
            val shadowDx = 4.dp.toPx()
            val shadowDy = 4.dp.toPx()

            withTransform({
                scale(scaleX = badgeScaleX, scaleY = badgeScaleY, pivot = center)
                rotate(degrees = -6f, pivot = center)
            }) {
                // Hard offset ink shadow
                withTransform({
                    translate(left = shadowDx, top = shadowDy)
                }) {
                    drawPath(path = badgePath, color = strokeDark.copy(alpha = badgeAlpha))
                }
                // Crisp outer light rim for contrast on dark card
                drawPath(
                    path = badgePath,
                    color = strokeLight.copy(alpha = badgeAlpha),
                    style = Stroke(width = 5.dp.toPx(), join = StrokeJoin.Miter),
                )
                // Brand fill
                drawPath(path = badgePath, color = brand.copy(alpha = badgeAlpha))
                // Bold miter ink border
                drawPath(
                    path = badgePath,
                    color = strokeDark.copy(alpha = badgeAlpha),
                    style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Miter),
                )
            }

            // 3. Bold sharp-cornered checkmark with hard offset ink shadow
            val checkFraction = if (reduced) {
                1f
            } else {
                FastOutSlowInEasing.transform(((p - 0.22f) / 0.50f).coerceIn(0f, 1f))
            }
            if (checkFraction > 0f) {
                checkPath.reset()
                checkPath.moveTo(cx - badgeRadius * 0.44f, cy + badgeRadius * 0.04f)
                checkPath.lineTo(cx - badgeRadius * 0.10f, cy + badgeRadius * 0.36f)
                checkPath.lineTo(cx + badgeRadius * 0.48f, cy - badgeRadius * 0.32f)

                pathMeasure.setPath(checkPath, false)
                segmentPath.reset()
                pathMeasure.getSegment(0f, pathMeasure.length * checkFraction, segmentPath, true)

                val checkStroke = Stroke(
                    width = 5.5.dp.toPx(),
                    cap = StrokeCap.Square,
                    join = StrokeJoin.Miter,
                )
                val checkShadowDx = 2.5.dp.toPx()
                val checkShadowDy = 2.5.dp.toPx()

                withTransform({
                    scale(scaleX = badgeScaleX, scaleY = badgeScaleY, pivot = center)
                }) {
                    withTransform({
                        translate(left = checkShadowDx, top = checkShadowDy)
                    }) {
                        drawPath(
                            path = segmentPath,
                            color = checkShadowColor,
                            style = checkStroke,
                        )
                    }
                    drawPath(
                        path = segmentPath,
                        color = onBrand,
                        style = checkStroke,
                    )
                }
            }
        }

        // 4. Four sharp 4-point comic sparkle shards popping out at the corners
        val shardT = if (reduced) {
            1f
        } else {
            FastOutSlowInEasing.transform(((p - 0.34f) / 0.56f).coerceIn(0f, 1f))
        }
        if (shardT > 0.01f) {
            val shardDistScale = if (reduced) 1f else lerpFloat(0.62f, 1f, shardT)
            val shardSizeScale = if (reduced) {
                1f
            } else if (shardT < 0.72f) {
                lerpFloat(0f, 1.15f, shardT / 0.72f)
            } else {
                lerpFloat(1.15f, 1f, (shardT - 0.72f) / 0.28f)
            }
            for (shard in P5CheckCornerShards) {
                val sx = cx + shard.xDir * minDim * shard.distNorm * shardDistScale
                val sy = cy + shard.yDir * minDim * shard.distNorm * shardDistScale
                val r = shard.radiusDp.dp.toPx() * shardSizeScale
                if (r <= 0.5f) continue
                val fill = if (shard.useHighlight) highlight else strokeLight
                withTransform({
                    rotate(degrees = shard.angleDeg, pivot = Offset(sx, sy))
                }) {
                    buildSharpFourPointStarPath(shardPath, sx, sy, r)
                    drawPath(path = shardPath, color = fill)
                    drawPath(
                        path = shardPath,
                        color = strokeDark,
                        style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter),
                    )
                }
            }
        }
    }
}

@Composable
private fun P5MilestoneFlameEmblem(trigger: CheckInCelebrationData?) {
    val p5 = LocalP5Tokens.current
    val reduced = rememberMotionReduced() || p5.motionLevel == P5MotionLevel.COMPACT
    val introProgress = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(trigger, reduced) {
        if (trigger != null) {
            if (reduced) {
                introProgress.snapTo(1f)
            } else {
                introProgress.snapTo(0f)
                introProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 540, easing = LinearEasing),
                )
            }
        }
    }

    val phase = if (reduced) {
        0f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "p5FlameLiving")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "p5FlamePhase",
        ).value
    }

    // Ensure blazing 3-layer contrast across all 4 P5 palettes (swap on ROYAL_GOLD so outer=crimson, inner=gold, core=white)
    val outerColor = if (p5.palette == P5Palette.ROYAL_GOLD) p5.highlightBadge.asColor() else p5.brandFill.asColor()
    val innerColor = if (p5.palette == P5Palette.ROYAL_GOLD) p5.brandFill.asColor() else p5.highlightBadge.asColor()
    val coreColor = p5.strokeLight.asColor()
    val strokeDark = p5.strokeDark.asColor()
    val strokeLight = p5.strokeLight.asColor()

    val outerFlamePath = remember { Path() }
    val innerFlamePath = remember { Path() }
    val coreDiamondPath = remember { Path() }
    val sparkPath = remember { Path() }

    Canvas(Modifier.size(96.dp)) {
        val p = introProgress.value
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val outerBaseY = h * 0.87f
        val innerBaseY = h * 0.82f
        val twoPi = (2.0 * PI).toFloat()

        // Crisp rhythmic angular flicker when motion is enabled
        val flicker1 = if (reduced) 0f else sin(phase * twoPi)
        val flicker2 = if (reduced) 0f else sin(phase * twoPi * 2f + 0.6f)
        val flickerScaleY = if (reduced) 1f else 1.0f + 0.042f * flicker1 + 0.014f * flicker2
        val flickerScaleX = if (reduced) 1f else 1.0f - 0.026f * flicker1
        val tipJitter = if (reduced) 0f else w * (0.026f * flicker1 + 0.010f * flicker2)
        val lickJitter = if (reduced) 0f else w * 0.018f * sin((phase + 0.35f) * twoPi)
        val innerJitter = if (reduced) 0f else w * 0.016f * sin((phase + 0.22f) * twoPi)

        // Snappy comic ignition intro
        val (outerEntryScaleX, outerEntryScaleY) = if (reduced) {
            1f to 1f
        } else {
            squashBounceScale(
                t = p,
                startScaleX = 0.52f,
                startScaleY = 0.22f,
                peakScaleX = 0.90f,
                peakScaleY = 1.16f,
                squashScaleX = 1.05f,
                squashScaleY = 0.95f,
                peakAt = 0.38f,
                squashAt = 0.68f,
            )
        }
        val outerAlpha = if (reduced) 1f else (p / 0.14f).coerceIn(0f, 1f)

        val innerP = if (reduced) 1f else ((p - 0.10f) / 0.90f).coerceIn(0f, 1f)
        val (innerEntryScaleX, innerEntryScaleY) = if (reduced) {
            1f to 1f
        } else {
            squashBounceScale(
                t = innerP,
                startScaleX = 0.15f,
                startScaleY = 0.15f,
                peakScaleX = 0.92f,
                peakScaleY = 1.12f,
                squashScaleX = 1.04f,
                squashScaleY = 0.96f,
                peakAt = 0.44f,
                squashAt = 0.74f,
            )
        }
        val innerAlpha = if (reduced) 1f else (innerP / 0.16f).coerceIn(0f, 1f)

        // 1 & 2. Hard-offset angled shadow polygon + Sharp polygonal jagged outer flame
        if (outerAlpha > 0f) {
            buildP5AngularOuterFlamePath(
                path = outerFlamePath,
                w = w,
                h = h,
                baseY = outerBaseY,
                tipJitter = tipJitter,
                lickJitter = lickJitter,
            )
            val shadowDx = 4.5.dp.toPx()
            val shadowDy = 4.dp.toPx()
            withTransform({
                scale(
                    scaleX = outerEntryScaleX * flickerScaleX,
                    scaleY = outerEntryScaleY * flickerScaleY,
                    pivot = Offset(cx, outerBaseY),
                )
            }) {
                // Hard offset angled shadow polygon (no soft oval shadow)
                withTransform({
                    translate(left = shadowDx, top = shadowDy)
                }) {
                    drawPath(
                        path = outerFlamePath,
                        color = strokeDark.copy(alpha = outerAlpha),
                    )
                }
                // Crisp outer contrast rim
                drawPath(
                    path = outerFlamePath,
                    color = strokeLight.copy(alpha = outerAlpha),
                    style = Stroke(width = 4.5.dp.toPx(), join = StrokeJoin.Miter),
                )
                // Jagged outer flame fill
                drawPath(
                    path = outerFlamePath,
                    color = outerColor.copy(alpha = outerAlpha),
                )
                // Bold miter ink outline
                drawPath(
                    path = outerFlamePath,
                    color = strokeDark.copy(alpha = outerAlpha),
                    style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Miter),
                )
            }
        }

        // 3 & 4. Jagged inner flame + Sharp white diamond core
        if (innerAlpha > 0f) {
            buildP5AngularInnerFlamePath(
                path = innerFlamePath,
                w = w,
                h = h,
                baseY = innerBaseY,
                tipJitter = innerJitter,
            )
            buildSharpDiamondPath(
                path = coreDiamondPath,
                cx = cx + innerJitter * 0.35f,
                cy = h * 0.72f,
                radius = h * 0.105f,
            )
            withTransform({
                scale(
                    scaleX = innerEntryScaleX * flickerScaleX,
                    scaleY = innerEntryScaleY * flickerScaleY,
                    pivot = Offset(cx, innerBaseY),
                )
            }) {
                drawPath(
                    path = innerFlamePath,
                    color = innerColor.copy(alpha = innerAlpha),
                )
                drawPath(
                    path = innerFlamePath,
                    color = strokeDark.copy(alpha = innerAlpha),
                    style = Stroke(width = 1.75.dp.toPx(), join = StrokeJoin.Miter),
                )
                drawPath(
                    path = coreDiamondPath,
                    color = coreColor.copy(alpha = innerAlpha),
                )
                drawPath(
                    path = coreDiamondPath,
                    color = strokeDark.copy(alpha = innerAlpha),
                    style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter),
                )
            }
        }

        // 5. Sharp 4-point comic sparks & embers snapping around the flame
        val sparkIntro = if (reduced) 1f else FastOutSlowInEasing.transform(((p - 0.30f) / 0.55f).coerceIn(0f, 1f))
        if (sparkIntro > 0.01f) {
            for (spark in P5FlameSparks) {
                val cycle = if (reduced) 0.5f else (phase + spark.phaseOffset) % 1f
                val rise = if (reduced) 0f else -h * spark.riseNorm * cycle
                val snapPulse = if (reduced) {
                    1f
                } else {
                    val s = 0.5f + 0.5f * sin(cycle * twoPi)
                    0.68f + 0.42f * s
                }
                val sx = w * spark.xNorm
                val sy = h * spark.yNorm + rise
                val r = spark.radiusDp.dp.toPx() * snapPulse * sparkIntro
                if (r <= 0.5f) continue
                val fill = if (spark.useCoreColor) coreColor else innerColor
                val rot = spark.angleDeg + if (reduced) 0f else (cycle - 0.5f) * 18f
                withTransform({
                    rotate(degrees = rot, pivot = Offset(sx, sy))
                }) {
                    if (spark.isStar) {
                        buildSharpFourPointStarPath(sparkPath, sx, sy, r)
                    } else {
                        buildSharpDiamondPath(sparkPath, sx, sy, r)
                    }
                    drawPath(path = sparkPath, color = fill)
                    drawPath(
                        path = sparkPath,
                        color = strokeDark,
                        style = Stroke(width = 1.dp.toPx(), join = StrokeJoin.Miter),
                    )
                }
            }
        }
    }
}

private fun buildP5StampBadgePath(
    path: Path,
    cx: Float,
    cy: Float,
    radius: Float,
) {
    // Asymmetric 8-point comic diamond-starburst badge with sharp mitered corners
    path.reset()
    path.moveTo(cx + radius * 0.06f, cy - radius * 1.06f) // Top peak
    path.lineTo(cx + radius * 0.44f, cy - radius * 0.56f) // Upper-right notch
    path.lineTo(cx + radius * 1.08f, cy - radius * 0.14f) // Right peak
    path.lineTo(cx + radius * 0.62f, cy + radius * 0.44f) // Lower-right notch
    path.lineTo(cx - radius * 0.06f, cy + radius * 1.04f) // Bottom peak
    path.lineTo(cx - radius * 0.46f, cy + radius * 0.54f) // Lower-left notch
    path.lineTo(cx - radius * 1.06f, cy + radius * 0.12f) // Left peak
    path.lineTo(cx - radius * 0.60f, cy - radius * 0.46f) // Upper-left notch
    path.close()
}

private fun buildP5ShockDiamondPath(
    path: Path,
    cx: Float,
    cy: Float,
    radius: Float,
) {
    path.reset()
    path.moveTo(cx + radius * 0.05f, cy - radius * 1.04f)
    path.lineTo(cx + radius * 1.06f, cy - radius * 0.08f)
    path.lineTo(cx - radius * 0.05f, cy + radius * 1.02f)
    path.lineTo(cx - radius * 1.04f, cy + radius * 0.08f)
    path.close()
}

private fun buildP5AngularOuterFlamePath(
    path: Path,
    w: Float,
    h: Float,
    baseY: Float,
    tipJitter: Float,
    lickJitter: Float,
) {
    val cx = w * 0.50f
    path.reset()
    // Angular faceted base & jagged comic outer flame silhouette
    path.moveTo(cx, baseY)
    path.lineTo(w * 0.30f, h * 0.82f)
    path.lineTo(w * 0.21f, h * 0.64f)
    path.lineTo(w * 0.26f + lickJitter, h * 0.39f) // Left jagged flame horn
    path.lineTo(w * 0.37f + lickJitter * 0.4f, h * 0.48f) // Sharp inward notch
    path.lineTo(w * 0.43f + tipJitter * 0.55f, h * 0.26f)
    path.lineTo(w * 0.53f + tipJitter, h * 0.10f) // Main razor peak
    path.lineTo(w * 0.61f + tipJitter * 0.45f, h * 0.34f)
    path.lineTo(w * 0.67f - lickJitter * 0.3f, h * 0.29f) // Right secondary spike
    path.lineTo(w * 0.72f - lickJitter * 0.5f, h * 0.47f)
    path.lineTo(w * 0.79f, h * 0.63f)
    path.lineTo(w * 0.70f, h * 0.82f)
    path.close()
}

private fun buildP5AngularInnerFlamePath(
    path: Path,
    w: Float,
    h: Float,
    baseY: Float,
    tipJitter: Float,
) {
    val cx = w * 0.50f
    path.reset()
    path.moveTo(cx, baseY)
    path.lineTo(w * 0.36f, h * 0.75f)
    path.lineTo(w * 0.34f, h * 0.59f)
    path.lineTo(w * 0.43f + tipJitter * 0.35f, h * 0.53f)
    path.lineTo(w * 0.52f + tipJitter, h * 0.36f) // Sharp inner peak
    path.lineTo(w * 0.60f + tipJitter * 0.3f, h * 0.54f)
    path.lineTo(w * 0.66f, h * 0.62f)
    path.lineTo(w * 0.63f, h * 0.76f)
    path.close()
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
    if (LocalUiStyle.current == UiStyle.PERSONA_5) {
        P5DailyCheckEmblem(trigger = trigger)
        return
    }
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
    if (LocalUiStyle.current == UiStyle.PERSONA_5) {
        P5MilestoneFlameEmblem(trigger = trigger)
        return
    }
    val accentAction = LocalThemeTokens.current.accent.accentAction.asColor()
    val p5Compact = LocalUiStyle.current == UiStyle.PERSONA_5 && LocalP5Tokens.current.motionLevel == P5MotionLevel.COMPACT
    val reduced = rememberMotionReduced() || p5Compact
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
