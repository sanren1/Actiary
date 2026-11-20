package com.actiary.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import kotlin.math.roundToInt

@Composable
internal fun Modifier.glassSurface(
    shape: CornerBasedShape,
    sampling: GlassSampling,
    exported: GlassLayer? = null,
    backdrop: GlassBackdrop? = null,
): Modifier {
    val layers = LocalGlassLayers.current
    val src = backdrop ?: if (sampling == GlassSampling.Background) layers.background else layers.content
    return glass(src, shape, LocalGlassProfile.current, LocalThemeTokens.current.dark, exported, flat = sampling == GlassSampling.Background)
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(28.dp),
    sampling: GlassSampling = GlassSampling.Background,
    exported: GlassLayer? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.glassSurface(shape, sampling, exported), content = content)
}

/** Default Content sampling: place in GlassHost's overlay slot, or pass [backdrop] / Background sampling inside content. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sampling: GlassSampling = GlassSampling.Content,
    backdrop: GlassBackdrop? = null,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(48.dp)
            .glassSurface(CircleShape, sampling, backdrop = backdrop)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = LocalThemeTokens.current.neutrals.textPrimary.asColor().copy(alpha = if (enabled) 1f else 0.3f))
    }
}

/** Samples the content layer: place in GlassHost's overlay slot. */
@Composable
fun GlassFloatingAction(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .glassSurface(shape, GlassSampling.Content)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = t.accent.accentAction.asColor())
        Text(text, color = t.neutrals.textPrimary.asColor(), fontWeight = FontWeight.Medium)
    }
}

data class GlassTab(val icon: ImageVector, val label: String)

/** Applies no insets or outer padding: the caller supplies them through [modifier]. */
@Composable
fun GlassBottomBar(tabs: List<GlassTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(50)
    val pos by animateFloatAsState(
        selected.toFloat(),
        if (rememberAnimationsOff()) snap() else spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "tab",
    )
    BoxWithConstraints(modifier.fillMaxWidth().height(64.dp).glassSurface(shape, GlassSampling.Content).padding(4.dp)) {
        val tabW = maxWidth / tabs.size
        val tabWpx = with(LocalDensity.current) { tabW.toPx() }
        Box(
            Modifier
                .offset { IntOffset((pos * tabWpx).roundToInt(), 0) }
                .width(tabW).fillMaxHeight()
                .clip(shape).background(t.accent.accentSubtle.asColor()),
        )
        Row(Modifier.fillMaxHeight()) {
            tabs.forEachIndexed { i, tab ->
                val color = (if (i == selected) t.accent.accentAction else t.neutrals.textSecondary).asColor()
                Column(
                    Modifier.width(tabW).fillMaxHeight().clip(shape)
                        .selectable(i == selected, role = Role.Tab) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, null, tint = color, modifier = Modifier.size(24.dp))
                    Text(tab.label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

/** Samples the content layer: place in GlassHost's overlay slot. */
@Composable
fun GlassTopBar(title: String, visible: Boolean, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    val t = LocalThemeTokens.current
    val still = rememberAnimationsOff()
    AnimatedVisibility(
        visible, modifier,
        enter = if (still) EnterTransition.None else fadeIn(),
        exit = if (still) ExitTransition.None else fadeOut(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .glassSurface(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), GlassSampling.Content)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title, Modifier.weight(1f).semantics { heading() }, color = t.neutrals.textPrimary.asColor(),
                fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            )
            actions()
        }
    }
}

@Composable
fun GlassSegmentedPicker(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalThemeTokens.current
    val shape = RoundedCornerShape(50)
    Row(modifier.fillMaxWidth().glassSurface(shape, GlassSampling.Background).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp).clip(shape)
                    .then(if (on) Modifier.background(t.neutrals.surface.asColor()) else Modifier)
                    .selectable(on, role = Role.RadioButton) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, maxLines = 1, fontWeight = FontWeight.Medium,
                    color = (if (on) t.accent.accentAction else t.neutrals.textSecondary).asColor(),
                )
            }
        }
    }
}
