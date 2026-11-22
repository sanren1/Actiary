package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassPanel
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.LocalFloatingInsets
import com.actiary.ui.common.PinnedScrollPage
import com.actiary.ui.common.RootTitle

internal enum class SettingsPage { HOME, APPEARANCE, TYPES, DATA, ABOUT }

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
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp).heightIn(min = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassIconButton(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.nav_back), onBack, glass = false)
                BasicText(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(LocalThemeTokens.current.neutrals.textPrimary.asColor(), 28.sp, FontWeight.Bold),
                )
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
internal fun SettingsGroup(content: @Composable (rowShape: Shape) -> Unit) {
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
    val n = LocalThemeTokens.current.neutrals
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
