package com.actiary.p5

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.P5MotionLevel
import com.actiary.data.appearance.P5Palette
import com.actiary.data.appearance.UiStyle
import com.actiary.glass.GlassBottomBarHeight
import com.actiary.glass.GlassTab
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.motion.rememberMotionReduced

private val P5ToggleTrackWidth = 60.dp
private val P5ToggleTrackHeight = 28.dp
private val P5ToggleThumbWidth = 28.dp
private val P5ToggleThumbHeight = 20.dp
private val P5ToggleInset = 4.dp
private val P5ToggleTravel = P5ToggleTrackWidth - P5ToggleThumbWidth - P5ToggleInset * 2

@Composable
private fun rememberP5MotionStill(p5: P5Tokens): Boolean =
    p5.motionLevel == P5MotionLevel.COMPACT || rememberMotionReduced()

@Composable
fun P5IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = true,
    iconSize: Dp = 24.dp,
    tint: Color? = null,
) {
    val p5 = LocalP5Tokens.current
    val isCallingCard = p5.palette == P5Palette.CALLING_CARD
    val surfaceModifier = if (outlined) {
        Modifier.p5Surface(
            variant = P5ShapeVariant.BADGE,
            fillColor = p5.darkCard.asColor(),
            strokeColor = if (isCallingCard) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
            shadowColor = p5.shadowColor.asColor(),
            strokeWidth = 2.dp,
            shadowOffset = DpOffset(2.dp, 2.dp),
        )
    } else {
        Modifier.clip(p5Shape(P5ShapeVariant.BADGE))
    }
    val iconTint = tint ?: if (!outlined && isCallingCard) {
        p5.onLightCard.asColor()
    } else {
        p5.strokeLight.asColor()
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .then(surfaceModifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint.copy(alpha = if (enabled) 1f else 0.35f),
        )
    }
}

@Composable
fun P5FloatingAction(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p5 = LocalP5Tokens.current
    val isCallingCard = p5.palette == P5Palette.CALLING_CARD
    Row(
        modifier = modifier
            .p5Surface(
                variant = P5ShapeVariant.PILL,
                fillColor = p5.brandFill.asColor(),
                strokeColor = if (isCallingCard) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                shadowColor = p5.strokeDark.asColor(),
                strokeWidth = 2.5.dp,
                shadowOffset = DpOffset(4.dp, 4.dp),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = p5.onBrandFill.asColor())
        Text(
            text = text,
            color = p5.onBrandFill.asColor(),
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
fun P5BottomBar(
    tabs: List<GlassTab>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p5 = LocalP5Tokens.current
    val still = rememberP5MotionStill(p5)
    val view = LocalView.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GlassBottomBarHeight)
            .p5Surface(
                variant = P5ShapeVariant.PANEL,
                fillColor = p5.darkCard.asColor(),
                strokeColor = p5.brandFill.asColor(),
                shadowColor = p5.strokeDark.asColor(),
                strokeWidth = 2.5.dp,
                shadowOffset = DpOffset(3.dp, 4.dp),
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 5.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selected
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.96f,
                    animationSpec = if (still) snap() else spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "p5TabScale",
                )
                val contentColor = if (isSelected) {
                    p5.onBrandFill.asColor()
                } else {
                    p5.strokeLight.asColor().copy(alpha = 0.74f)
                }
                val itemSurface = if (isSelected) {
                    Modifier.p5Surface(
                        variant = P5ShapeVariant.TAB,
                        fillColor = p5.brandFill.asColor(),
                        strokeColor = p5.strokeLight.asColor(),
                        shadowColor = p5.strokeDark.asColor(),
                        strokeWidth = 1.5.dp,
                        shadowOffset = DpOffset(2.dp, 2.dp),
                    )
                } else {
                    Modifier.clip(p5Shape(P5ShapeVariant.TAB))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .then(itemSurface)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = {
                                if (!isSelected) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                onSelect(index)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(22.dp),
                        )
                        BasicText(
                            text = tab.label,
                            modifier = Modifier.padding(horizontal = 4.dp),
                            style = TextStyle(
                                color = contentColor,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            ),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 8.sp,
                                maxFontSize = 11.sp,
                                stepSize = 0.5.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun P5Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    val p5 = LocalP5Tokens.current
    val still = rememberP5MotionStill(p5)
    val view = LocalView.current
    val fontScale = LocalDensity.current.fontScale
    val allowScroll = scrollable || fontScale >= 1.3f
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .p5Surface(
                variant = P5ShapeVariant.PILL,
                fillColor = p5.darkCard.asColor(),
                strokeColor = if (p5.palette == P5Palette.CALLING_CARD) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                shadowColor = p5.shadowColor.asColor(),
                strokeWidth = 2.dp,
                shadowOffset = DpOffset(3.dp, 3.dp),
            )
            .padding(4.dp),
    ) {
        val rowModifier = if (allowScroll) {
            Modifier.horizontalScroll(scrollState)
        } else {
            Modifier.fillMaxWidth()
        }
        Row(
            modifier = rowModifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selected
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) p5.brandFill.asColor() else Color.Transparent,
                    animationSpec = if (still) snap() else spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "p5SegmentBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        p5.onBrandFill.asColor()
                    } else {
                        p5.strokeLight.asColor().copy(alpha = 0.80f)
                    },
                    animationSpec = if (still) snap() else spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "p5SegmentText",
                )
                val itemModifier = if (allowScroll) {
                    Modifier.widthIn(min = 48.dp)
                } else {
                    Modifier.weight(1f)
                }
                Box(
                    modifier = itemModifier
                        .heightIn(min = 38.dp)
                        .p5Surface(
                            variant = P5ShapeVariant.TAB,
                            fillColor = bgColor,
                            strokeColor = if (isSelected) p5.strokeDark.asColor() else Color.Transparent,
                            shadowColor = Color.Transparent,
                            strokeWidth = if (isSelected) 1.5.dp else 0.dp,
                            shadowOffset = DpOffset.Zero,
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = {
                                if (!isSelected) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                                onSelect(index)
                            },
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
fun P5Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    labelContent: (@Composable () -> Unit)? = null,
) {
    val p5 = LocalP5Tokens.current
    val still = rememberP5MotionStill(p5)
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = if (still) snap() else spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "p5ToggleProgress",
    )
    val trackFill by animateColorAsState(
        targetValue = if (checked) p5.brandFill.asColor() else p5.darkCard.asColor(),
        animationSpec = if (still) snap() else spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "p5ToggleTrack",
    )
    val thumbFill = if (checked) p5.onBrandFill.asColor() else p5.strokeLight.asColor()

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (labelContent != null) {
            Box(Modifier.weight(1f).clearAndSetSemantics {}) {
                labelContent()
            }
        }
        Box(
            modifier = Modifier.size(P5ToggleTrackWidth, 48.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(P5ToggleTrackWidth, P5ToggleTrackHeight)
                    .p5Surface(
                        variant = P5ShapeVariant.PILL,
                        fillColor = trackFill,
                        strokeColor = if (p5.palette == P5Palette.CALLING_CARD) p5.strokeDark.asColor() else p5.strokeLight.asColor(),
                        shadowColor = p5.strokeDark.asColor(),
                        strokeWidth = 2.dp,
                        shadowOffset = DpOffset(2.dp, 2.dp),
                    ),
            )
            Box(
                modifier = Modifier
                    .padding(start = P5ToggleInset)
                    .graphicsLayer {
                        translationX = P5ToggleTravel.toPx() * progress * direction
                    }
                    .size(P5ToggleThumbWidth, P5ToggleThumbHeight)
                    .p5Surface(
                        variant = P5ShapeVariant.BADGE,
                        fillColor = thumbFill,
                        strokeColor = p5.strokeDark.asColor(),
                        shadowColor = Color.Transparent,
                        strokeWidth = 1.5.dp,
                        shadowOffset = DpOffset.Zero,
                    ),
            )
        }
    }
}

@Composable
fun P5Preview(
    p5Tokens: P5Tokens,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableIntStateOf(0) }
    CompositionLocalProvider(
        LocalUiStyle provides UiStyle.PERSONA_5,
        LocalP5Tokens provides p5Tokens,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .p5Surface(
                    variant = P5ShapeVariant.PANEL,
                    fillColor = p5Tokens.canvas.asColor(),
                    strokeColor = if (p5Tokens.palette == P5Palette.CALLING_CARD) {
                        p5Tokens.strokeDark.asColor()
                    } else {
                        p5Tokens.strokeLight.asColor()
                    },
                    shadowColor = p5Tokens.shadowColor.asColor(),
                    strokeWidth = 2.dp,
                    shadowOffset = DpOffset(4.dp, 4.dp),
                )
                .clearAndSetSemantics {},
        ) {
            P5Background(p5Tokens, Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp)
                    .p5Surface(
                        variant = P5ShapeVariant.CARD,
                        fillColor = p5Tokens.lightCard.asColor(),
                        strokeColor = p5Tokens.strokeDark.asColor(),
                        shadowColor = p5Tokens.shadowColor.asColor(),
                        strokeWidth = 2.dp,
                        shadowOffset = DpOffset(3.dp, 3.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    (12..16).forEach { day ->
                        val active = day == 14
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(p5Shape(P5ShapeVariant.BADGE))
                                .background(
                                    if (active) p5Tokens.brandFill.asColor()
                                    else p5Tokens.onLightCard.asColor().copy(alpha = 0.08f),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$day",
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Black else FontWeight.Bold,
                                color = if (active) {
                                    p5Tokens.onBrandFill.asColor()
                                } else {
                                    p5Tokens.onLightCard.asColor()
                                },
                            )
                        }
                    }
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(34.dp),
                ) {
                    val values = listOf(0.42f, 0.68f, 0.52f, 0.92f, 0.58f, 0.74f)
                    val slot = size.width / values.size
                    val barWidth = slot * 0.58f
                    val primaryBar = p5Tokens.accentChartOnLight.asColor()
                    val mutedBar = p5Tokens.onLightCard.asColor().copy(alpha = 0.78f)
                    val highlightBar = p5Tokens.highlightBadge.asColor()
                    values.forEachIndexed { index, fraction ->
                        val barHeight = size.height * fraction
                        val color = when (index) {
                            3 -> primaryBar
                            5 -> highlightBar
                            else -> mutedBar
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(index * slot + (slot - barWidth) / 2f, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                        )
                    }
                }
            }
            P5BottomBar(
                tabs = listOf(
                    GlassTab(Icons.Outlined.CalendarMonth, stringResource(R.string.tab_calendar)),
                    GlassTab(Icons.Outlined.BarChart, stringResource(R.string.tab_records)),
                    GlassTab(Icons.Outlined.Settings, stringResource(R.string.tab_settings)),
                ),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp),
            )
        }
    }
}
