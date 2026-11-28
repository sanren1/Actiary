package com.actiary.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.actiary.R
import com.actiary.data.appearance.UiStyle
import com.actiary.data.update.AppUpdater
import com.actiary.data.update.ReleaseUpdate
import com.actiary.glass.GlassOverlayPanel
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun UpdateDialog(
    update: ReleaseUpdate?,
    visibility: MutableTransitionState<Boolean>,
    onDismiss: () -> Unit,
    onIgnore: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val primaryText = n.textPrimary.asColor()
    val secondaryText = n.textSecondary.asColor()
    val accentText = t.accent.accentAction.asColor()
    var held by remember { mutableStateOf<ReleaseUpdate?>(null) }
    if (update != null) held = update
    val shown = update ?: held
    var downloading by remember(shown?.version) { mutableStateOf(false) }
    var progress by remember(shown?.version) { mutableStateOf<Float?>(null) }
    var downloadError by remember(shown?.version) { mutableStateOf(false) }

    val title = shown?.let { stringResource(R.string.update_available_title, it.version) }.orEmpty()
    GlassOverlayPanel(
        visible = update != null,
        onDismiss = { if (!downloading) onDismiss() },
        contentPadding = PaddingValues(20.dp),
        title = title,
        visibility = visibility,
    ) {
        if (shown != null) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    title,
                    Modifier.semantics { heading() },
                    style = TextStyle(primaryText, 19.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
                )
                val notesText = shown.notes.ifBlank { stringResource(R.string.update_default_notes) }
                Column(
                    Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                ) {
                    BasicText(
                        notesText,
                        style = TextStyle(secondaryText, 14.sp, lineHeight = 20.sp),
                    )
                }
                if (downloading) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val p = progress
                        val label = if (p != null) {
                            stringResource(R.string.update_downloading_percent, (p * 100f).roundToInt())
                        } else {
                            stringResource(R.string.update_downloading)
                        }
                        BasicText(label, style = TextStyle(primaryText, 13.sp, if (isP5) FontWeight.Bold else FontWeight.Medium))
                        val progressClip = if (isP5) RectangleShape else RoundedCornerShape(4.dp)
                        val progressColor = if (isP5) p5.brandFill.asColor() else t.accent.accentAction.asColor()
                        val trackColor = if (isP5 && t.dark) Color(0xFF2B2B32) else n.surfaceMuted.asColor()
                        if (p != null) {
                            LinearProgressIndicator(
                                progress = { p },
                                modifier = Modifier.fillMaxWidth().clip(progressClip),
                                color = progressColor,
                                trackColor = trackColor,
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().clip(progressClip),
                                color = progressColor,
                                trackColor = trackColor,
                            )
                        }
                    }
                }
                if (downloadError) {
                    BasicText(
                        stringResource(R.string.update_download_failed),
                        style = TextStyle(accentText, 13.sp),
                    )
                }
                val btnShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
                val btnBg = if (isP5) {
                    if (downloading) {
                        if (t.dark) Color(0xFF26262C) else n.surfaceMuted.asColor()
                    } else {
                        p5.brandFill.asColor()
                    }
                } else {
                    if (downloading) n.surfaceMuted.asColor() else t.accent.accentAction.asColor()
                }
                val btnBorder = if (isP5) {
                    Modifier.border(
                        2.dp,
                        if (downloading) p5Stroke.copy(alpha = 0.35f) else p5Stroke,
                        btnShape,
                    )
                } else {
                    Modifier
                }
                val btnTextColor = if (isP5) {
                    if (downloading) n.textSecondary.asColor() else p5.onBrandFill.asColor()
                } else {
                    if (downloading) n.textSecondary.asColor() else t.accent.onAccentAction.asColor()
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 46.dp)
                        .clip(btnShape)
                        .background(btnBg, btnShape)
                        .then(btnBorder)
                        .clickable(enabled = !downloading, role = Role.Button) {
                            val apkUrl = shown.apkUrl
                            if (apkUrl == null) {
                                openReleaseInBrowser(context, shown.htmlUrl)
                                onDismiss()
                            } else {
                                downloading = true
                                downloadError = false
                                progress = 0f
                                scope.launch {
                                    val result = AppUpdater.downloadApk(context, apkUrl, shown.version) { progress = it }
                                    downloading = false
                                    result.onSuccess { file ->
                                        val launched = AppUpdater.installApk(context, file)
                                        if (launched) {
                                            onDismiss()
                                        } else {
                                            downloadError = true
                                            openReleaseInBrowser(context, shown.htmlUrl)
                                        }
                                    }.onFailure {
                                        downloadError = true
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        stringResource(if (downloadError) R.string.update_retry else R.string.update_action_now),
                        style = TextStyle(
                            btnTextColor,
                            15.sp,
                            if (isP5) FontWeight.Black else FontWeight.SemiBold,
                        ),
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onIgnore(shown.version) },
                        enabled = !downloading,
                    ) {
                        Text(
                            stringResource(R.string.update_action_ignore),
                            color = secondaryText,
                            fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                    TextButton(
                        onClick = onDismiss,
                        enabled = !downloading,
                    ) {
                        Text(
                            stringResource(R.string.record_cancel),
                            color = primaryText,
                            fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

private fun openReleaseInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .apply {
                selector = Intent(Intent.ACTION_VIEW, Uri.fromParts("https", "", null))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.notice_browser_unavailable, Toast.LENGTH_SHORT).show()
    }
}
