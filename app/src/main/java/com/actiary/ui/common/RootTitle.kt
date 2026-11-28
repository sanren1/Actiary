package com.actiary.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.data.appearance.UiStyle
import com.actiary.p5.P5RansomTitle
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor

@Composable
internal fun RootTitle(title: String, modifier: Modifier = Modifier) {
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 48.dp).then(modifier),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (isP5) {
            P5RansomTitle(title, Modifier.semantics { heading() })
        } else {
            BasicText(
                title,
                Modifier.semantics { heading() },
                style = TextStyle(LocalThemeTokens.current.neutrals.textPrimary.asColor(), 28.sp, FontWeight.Bold),
            )
        }
    }
}
