package com.actiary.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.data.appearance.UiStyle
import com.actiary.glass.GlassAnchoredMenuItem
import com.actiary.glass.LocalGlassAnchoredMenu
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor

@Composable
internal fun <T> SelectionMenuRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val menu = LocalGlassAnchoredMenu.current
    var anchor by remember { mutableStateOf<Rect?>(null) }
    val tokens = LocalThemeTokens.current
    val neutral = tokens.neutrals
    val primary = neutral.textPrimary.asColor()
    val secondary = neutral.textSecondary.asColor()
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    if (isP5) {
        val p5 = LocalP5Tokens.current
        val rowShape = RoundedCornerShape(2.dp)
        val fillColor = if (tokens.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
        val strokeColor = if (tokens.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
        val badgeFill = if (tokens.dark) p5.brandFill.asColor().copy(alpha = 0.22f) else neutral.surfaceMuted.asColor()
        val badgeStroke = if (tokens.dark) p5.strokeLight.asColor().copy(alpha = 0.55f) else p5.strokeDark.asColor()
        val arrowTint = if (tokens.dark) p5.accentOnDark.asColor() else tokens.accent.accentAction.asColor()
        Row(
            modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(rowShape)
                .background(fillColor, rowShape)
                .border(2.dp, strokeColor, rowShape)
                .clickable(interactionSource = null, indication = ripple(color = primary), role = Role.Button) {
                    anchor?.let { target ->
                        menu.show(target, options.map { (option, text) ->
                            GlassAnchoredMenuItem(text, selected = option == selected) { onSelect(option) }
                        })
                    }
                }.padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(label, Modifier.weight(1f).padding(vertical = 4.dp), TextStyle(primary, 16.sp, FontWeight.Bold))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Row(
                    Modifier
                        .onGloballyPositioned { anchor = it.boundsInRoot() }
                        .clip(rowShape)
                        .background(badgeFill, rowShape)
                        .border(1.5.dp, badgeStroke, rowShape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(
                        options.firstOrNull { it.first == selected }?.second.orEmpty(),
                        Modifier.weight(1f, fill = false),
                        TextStyle(primary, 13.sp, FontWeight.Bold),
                    )
                    Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(18.dp), tint = arrowTint)
                }
            }
        }
        return
    }
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(neutral.surface.asColor())
            .clickable(interactionSource = null, indication = ripple(color = primary), role = Role.Button) {
                anchor?.let { target ->
                    menu.show(target, options.map { (option, text) ->
                        GlassAnchoredMenuItem(text, selected = option == selected) { onSelect(option) }
                    })
                }
            }.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, Modifier.weight(1f).padding(vertical = 8.dp), TextStyle(primary, 16.sp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            // The trailing value is the anchor even when the left label opened the menu.
            Row(
                Modifier.heightIn(min = 48.dp).onGloballyPositioned { anchor = it.boundsInRoot() },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(
                    options.firstOrNull { it.first == selected }?.second.orEmpty(),
                    Modifier.weight(1f, fill = false).padding(vertical = 8.dp),
                    TextStyle(secondary, 14.sp),
                )
                Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(20.dp), tint = secondary)
            }
        }
    }
}
