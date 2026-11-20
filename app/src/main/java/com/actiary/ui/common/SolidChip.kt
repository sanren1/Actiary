package com.actiary.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

/**
 * Solid-color pill: a plain (non-glass) filled background with a stroke when selected.
 * Shared by appearance-page rows; independent of the glass system.
 */
@Composable
fun SolidChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val shape = RoundedCornerShape(50)
    val contentColor = if (isSelected) a.accentAction.asColor() else n.textPrimary.asColor()
    Row(
        modifier.heightIn(min = 40.dp).clip(shape)
            .background(if (isSelected) a.accentSubtle.asColor() else n.surface.asColor())
            .then(if (isSelected) Modifier.border(2.dp, a.accentStroke.asColor(), shape) else Modifier)
            .semantics { selected = isSelected }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isSelected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = contentColor)
        BasicText(label, style = TextStyle(contentColor, 14.sp, FontWeight.Medium), maxLines = 1)
    }
}
