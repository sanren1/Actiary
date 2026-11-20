package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.AccentPreset
import com.actiary.data.appearance.AppearanceSettings
import com.actiary.data.appearance.GlassLevel
import com.actiary.data.appearance.GlassTintStrength
import com.actiary.data.appearance.ThemeMode
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

/**
 * Fourth tab. The appearance groups are rendered verbatim by [AppearanceScreen] so no extra glass
 * surface is added here; the two rows below are solid placeholders, not glass panels.
 */
@Composable
fun SettingsScreen(
    settings: AppearanceSettings,
    onThemeMode: (ThemeMode) -> Unit,
    onPreset: (AccentPreset) -> Unit,
    onGlassLevel: (GlassLevel) -> Unit,
    onGlassTint: (GlassTintStrength) -> Unit,
    onAmbient: (Boolean) -> Unit,
    onCustomTypes: () -> Unit,
) {
    val n = LocalThemeTokens.current.neutrals
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState()),
    ) {
        BasicText(
            stringResource(R.string.settings_title),
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).height(48.dp).semantics { heading() },
            style = TextStyle(n.textPrimary.asColor(), 28.sp, FontWeight.Bold),
        )
        AppearanceScreen(
            settings, onThemeMode, onPreset, onGlassLevel, onGlassTint, onAmbient,
            scrollContainer = false,
        )
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionRow(stringResource(R.string.settings_custom_types), onCustomTypes)
            PlaceholderRow(stringResource(R.string.settings_data))
            PlaceholderRow(stringResource(R.string.settings_about))
        }
    }
}

/** Solid, clickable settings row (not a glass panel). */
@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    val n = LocalThemeTokens.current.neutrals
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(n.surface.asColor(), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(label, style = TextStyle(n.textPrimary.asColor(), 16.sp))
    }
}

@Composable
private fun PlaceholderRow(label: String) {
    val n = LocalThemeTokens.current.neutrals
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .background(n.surface.asColor(), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(label, style = TextStyle(n.textPrimary.asColor(), 16.sp))
        BasicText(stringResource(R.string.coming_soon), style = TextStyle(n.textSecondary.asColor(), 14.sp))
    }
}
