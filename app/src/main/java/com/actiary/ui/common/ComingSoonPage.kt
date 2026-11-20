package com.actiary.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.actiary.R
import com.actiary.glass.GlassPanel
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor

/** Shared body of the Review / Stats placeholders: page title + one glass panel, no fake data. */
@Composable
fun ComingSoonPage(title: String) {
    val n = LocalThemeTokens.current.neutrals
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp),
    ) {
        BasicText(
            title,
            Modifier.padding(top = 8.dp).height(48.dp).semantics { heading() },
            style = TextStyle(n.textPrimary.asColor(), 28.sp, FontWeight.Bold),
        )
        GlassPanel(Modifier.padding(top = 12.dp).fillMaxWidth()) {
            BasicText(
                stringResource(R.string.coming_soon),
                Modifier.padding(20.dp),
                style = TextStyle(n.textSecondary.asColor(), 16.sp),
            )
        }
    }
}
