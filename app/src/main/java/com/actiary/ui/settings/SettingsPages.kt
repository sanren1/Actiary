package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.UiStyle
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Surface
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.PinnedScrollPage
import com.actiary.ui.common.RootTitle

internal enum class SettingsPage { HOME, APPEARANCE, GENERAL, TYPES, WORKOUT_PLANS, DATA, ABOUT }

/** Navigation parent used by both the system back handler and in-page back buttons. */
internal fun SettingsPage.parent(): SettingsPage? = when (this) {
    SettingsPage.HOME -> null
    else -> SettingsPage.HOME
}

internal fun SettingsPage.showsBottomBar(): Boolean = parent() == null

@Composable
internal fun SettingsPageLayout(
    title: String,
    onBack: (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    PinnedScrollPage(scrollOffset = { scrollState.value }, header = {
        if (onBack == null) {
            RootTitle(title, Modifier.padding(horizontal = 16.dp))
        } else {
            val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
            val t = LocalThemeTokens.current
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp).heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.nav_back), onBack, glass = false)
                if (isP5) {
                    val p5 = LocalP5Tokens.current
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        BasicText(
                            title,
                            Modifier.semantics { heading() },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(t.neutrals.textPrimary.asColor(), 26.sp, FontWeight.Black),
                        )
                        Box(Modifier.width(44.dp).height(3.dp).background(p5.brandFill.asColor()))
                    }
                } else {
                    BasicText(
                        title,
                        Modifier.weight(1f).semantics { heading() },
                        style = TextStyle(t.neutrals.textPrimary.asColor(), 28.sp, FontWeight.Bold),
                    )
                }
            }
        }
    }) { headerHeight, _ ->
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(start = 16.dp, end = 16.dp, top = headerHeight + 16.dp, bottom = LocalFloatingInsets.current.bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

private val SettingsGroupRadius = 28.dp
private val SettingsGroupInset = 8.dp

@Composable
internal fun SettingsSectionHeader(title: String) {
    val n = LocalThemeTokens.current.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    BasicText(
        text = title,
        modifier = Modifier.padding(horizontal = 4.dp).semantics { heading() },
        style = TextStyle(
            color = n.textSecondary.asColor(),
            fontSize = 14.sp,
            fontWeight = if (isP5) FontWeight.Black else FontWeight.SemiBold,
        ),
    )
}

@Composable
internal fun SettingsGroup(content: @Composable (rowShape: Shape) -> Unit) {
    if (LocalUiStyle.current == UiStyle.PERSONA_5) {
        val t = LocalThemeTokens.current
        val p5 = LocalP5Tokens.current
        val cardFill = if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
        val cardStroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
        Box(
            Modifier
                .fillMaxWidth()
                .p5Surface(
                    variant = P5ShapeVariant.CARD,
                    fillColor = cardFill,
                    strokeColor = cardStroke,
                    shadowColor = p5.shadowColor.asColor(),
                    strokeWidth = 2.dp,
                    shadowOffset = DpOffset(3.dp, 3.dp),
                ),
        ) {
            Column(Modifier.padding(SettingsGroupInset), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content(RoundedCornerShape(2.dp))
            }
        }
        return
    }
    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(SettingsGroupRadius)) {
        Column(Modifier.padding(SettingsGroupInset), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content(RoundedCornerShape(SettingsGroupRadius - SettingsGroupInset))
        }
    }
}

@Composable
internal fun SettingsRow(
    label: String,
    icon: ImageVector,
    endIcon: ImageVector? = null,
    summary: String? = null,
    shape: Shape = RoundedCornerShape(12.dp),
    onClick: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    if (isP5) {
        val p5 = LocalP5Tokens.current
        val p5RowShape = RoundedCornerShape(2.dp)
        val rowFill = n.surfaceMuted.asColor()
        val rowStroke = if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.35f) else p5.strokeDark.asColor()
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clip(p5RowShape)
                .background(rowFill, p5RowShape)
                .border(1.5.dp, rowStroke, p5RowShape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, null, tint = n.textPrimary.asColor())
            Column(Modifier.weight(1f)) {
                BasicText(label, style = TextStyle(n.textPrimary.asColor(), 16.sp, FontWeight.Bold))
                if (summary != null) BasicText(summary, style = TextStyle(n.textSecondary.asColor(), 13.sp))
            }
            if (endIcon != null) Icon(endIcon, null, tint = n.textPrimary.asColor())
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clip(shape)
            .background(n.surface.asColor(), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = n.textPrimary.asColor())
        Column(Modifier.weight(1f)) {
            BasicText(label, style = TextStyle(n.textPrimary.asColor(), 16.sp))
            if (summary != null) BasicText(summary, style = TextStyle(n.textSecondary.asColor(), 14.sp))
        }
        if (endIcon != null) Icon(endIcon, null, tint = n.textSecondary.asColor())
    }
}
