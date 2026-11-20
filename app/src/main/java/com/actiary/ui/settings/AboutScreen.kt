package com.actiary.ui.settings

import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

@Composable
internal fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val n = LocalThemeTokens.current.neutrals
    val version = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val notices = remember(context) {
        context.resources.openRawResource(R.raw.third_party_notices).bufferedReader().use { it.readText() }
    }
    var noticesVisible by rememberSaveable { mutableStateOf(false) }

    SettingsPageLayout(stringResource(R.string.settings_about), onBack) {
        BasicText(
            stringResource(R.string.app_name),
            style = TextStyle(n.textPrimary.asColor(), 22.sp, FontWeight.SemiBold),
        )
        BasicText(stringResource(R.string.settings_about_version, version), style = TextStyle(n.textSecondary.asColor(), 16.sp))
        BasicText(stringResource(R.string.settings_about_local), style = TextStyle(n.textPrimary.asColor(), 16.sp))
        SettingsRow(
            stringResource(if (noticesVisible) R.string.settings_hide_licenses else R.string.settings_licenses),
            Icons.Outlined.Description,
            if (noticesVisible) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
        ) { noticesVisible = !noticesVisible }
        if (noticesVisible) {
            BasicText(notices, style = TextStyle(color = n.textPrimary.asColor(), fontSize = 14.sp, lineHeight = 21.sp))
        }
    }
}
