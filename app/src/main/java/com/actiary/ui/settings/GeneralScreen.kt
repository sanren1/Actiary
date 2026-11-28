package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.data.appearance.AppLanguage
import com.actiary.data.appearance.UiStyle
import com.actiary.data.appearance.WeekStart
import com.actiary.glass.GlassToggle
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import com.actiary.ui.common.SelectionMenuRow

@Composable
internal fun weekStartName(value: WeekStart): String = stringResource(
    when (value) {
        WeekStart.SYSTEM -> R.string.settings_week_system
        WeekStart.MONDAY -> R.string.settings_week_monday
        WeekStart.SUNDAY -> R.string.settings_week_sunday
    },
)

@Composable
internal fun appLanguageName(value: AppLanguage): String = stringResource(
    when (value) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ZH -> R.string.language_zh
        AppLanguage.EN -> R.string.language_en
    },
)

@Composable
internal fun GeneralScreen(
    appLanguage: AppLanguage,
    onAppLanguage: (AppLanguage) -> Unit,
    weekStart: WeekStart,
    onWeekStart: (WeekStart) -> Unit,
    reduceInAppMotion: Boolean,
    onReduceInAppMotion: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current

    SettingsPageLayout(stringResource(R.string.settings_general), onBack) {
        SelectionMenuRow(
            label = stringResource(R.string.settings_language),
            options = AppLanguage.entries.map { it to appLanguageName(it) },
            selected = appLanguage,
            onSelect = onAppLanguage,
        )
        SelectionMenuRow(
            label = stringResource(R.string.settings_week_start),
            options = WeekStart.entries.map { it to weekStartName(it) },
            selected = weekStart,
            onSelect = onWeekStart,
        )
        val reduceMotionLabel = stringResource(R.string.appearance_reduce_motion)
        val toggleRowShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(16.dp)
        val toggleRowFill = if (isP5) {
            if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
        } else {
            n.surface.asColor()
        }
        val toggleRowBorder = if (isP5) {
            Modifier.border(2.dp, if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor(), toggleRowShape)
        } else {
            Modifier
        }
        Box(
            Modifier
                .fillMaxWidth()
                .clip(toggleRowShape)
                .background(toggleRowFill, toggleRowShape)
                .then(toggleRowBorder)
                .padding(horizontal = 16.dp, vertical = 2.dp),
        ) {
            GlassToggle(
                checked = reduceInAppMotion,
                onCheckedChange = onReduceInAppMotion,
                modifier = Modifier.fillMaxWidth(),
                label = reduceMotionLabel,
            ) {
                BasicText(
                    reduceMotionLabel,
                    style = TextStyle(
                        color = n.textPrimary.asColor(),
                        fontSize = 16.sp,
                        fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
