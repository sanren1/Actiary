package com.actiary.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.actiary.R
import com.actiary.data.appearance.UiStyle
import com.actiary.glass.GlassPanel
import com.actiary.glass.GlassSheet
import com.actiary.p5.P5ShapeVariant
import com.actiary.p5.p5Surface
import com.actiary.theme.LocalP5Tokens
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.LocalUiStyle
import com.actiary.theme.asColor

@Composable
internal fun AboutScreen(
    onBack: () -> Unit,
    onOpenNotices: () -> Unit,
    checkingUpdate: Boolean,
    onCheckUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val version = remember(context) {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    val iconRes = if (isP5) R.mipmap.ic_launcher_p5 else R.mipmap.ic_launcher
    val iconBitmap = remember(context, iconRes) {
        ContextCompat.getDrawable(context, iconRes)
            ?.toBitmap(128, 128)
            ?.asImageBitmap()
            ?: context.packageManager.getApplicationIcon(context.applicationInfo)
                .toBitmap(128, 128)
                .asImageBitmap()
    }

    SettingsPageLayout(stringResource(R.string.settings_about), onBack) {
        val heroContent: @Composable () -> Unit = {
            val iconShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(16.dp)
            val iconBorder = if (isP5) {
                Modifier.border(2.dp, if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor(), iconShape)
            } else {
                Modifier
            }
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(iconShape).then(iconBorder),
                )
                BasicText(
                    stringResource(R.string.app_name),
                    style = TextStyle(n.textPrimary.asColor(), 22.sp, if (isP5) FontWeight.Black else FontWeight.Bold),
                )
                BasicText(
                    stringResource(R.string.settings_about_version, version),
                    style = TextStyle(n.textSecondary.asColor(), 14.sp, FontWeight.Medium),
                )
                BasicText(
                    stringResource(R.string.about_tagline),
                    style = TextStyle(n.textSecondary.asColor(), 14.sp),
                )
            }
        }
        if (isP5) {
            val heroFill = if (t.dark) p5.darkCard.asColor() else p5.lightCard.asColor()
            val heroStroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
            Box(
                Modifier
                    .fillMaxWidth()
                    .p5Surface(
                        variant = P5ShapeVariant.CARD,
                        fillColor = heroFill,
                        strokeColor = heroStroke,
                        shadowColor = p5.shadowColor.asColor(),
                        strokeWidth = 2.dp,
                        shadowOffset = DpOffset(4.dp, 4.dp),
                    ),
            ) { heroContent() }
        } else {
            GlassPanel(Modifier.fillMaxWidth()) { heroContent() }
        }
        SettingsGroup { rowShape ->
            val infoRowBg = if (isP5) n.surfaceMuted.asColor() else n.surface.asColor()
            val infoRowBorder = if (isP5) {
                Modifier.border(
                    1.5.dp,
                    if (t.dark) p5.strokeLight.asColor().copy(alpha = 0.35f) else p5.strokeDark.asColor(),
                    rowShape,
                )
            } else {
                Modifier
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(rowShape)
                    .background(infoRowBg, rowShape)
                    .then(infoRowBorder)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Devices, null, tint = n.textPrimary.asColor())
                Column(Modifier.weight(1f)) {
                    BasicText(
                        stringResource(R.string.about_compatibility),
                        style = TextStyle(n.textPrimary.asColor(), 16.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                    )
                    BasicText(stringResource(R.string.about_compatibility_value), style = TextStyle(n.textSecondary.asColor(), 14.sp))
                }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(rowShape)
                    .background(infoRowBg, rowShape)
                    .then(infoRowBorder)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.VerifiedUser, null, tint = n.textPrimary.asColor())
                Column(Modifier.weight(1f)) {
                    BasicText(
                        stringResource(R.string.about_privacy),
                        style = TextStyle(n.textPrimary.asColor(), 16.sp, if (isP5) FontWeight.Bold else FontWeight.Normal),
                    )
                    BasicText(stringResource(R.string.about_privacy_value), style = TextStyle(n.textSecondary.asColor(), 14.sp))
                }
            }
            SettingsRow(
                label = stringResource(R.string.about_check_update),
                icon = Icons.Outlined.SystemUpdate,
                endIcon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                summary = stringResource(
                    if (checkingUpdate) R.string.about_checking_update else R.string.about_check_update_summary,
                ),
                shape = rowShape,
                onClick = { if (!checkingUpdate) onCheckUpdate() },
            )
            SettingsRow(
                label = stringResource(R.string.about_open_source),
                icon = Icons.Outlined.Code,
                endIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                summary = stringResource(R.string.about_open_source_summary),
                shape = rowShape,
                onClick = { openExternalUrl(context, ACTIARY_GITHUB_URL) },
            )
            SettingsRow(
                label = stringResource(R.string.settings_licenses),
                icon = Icons.Outlined.Description,
                endIcon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                summary = stringResource(R.string.about_third_party_summary),
                shape = rowShape,
                onClick = onOpenNotices,
            )
        }
    }
}

@Composable
internal fun ThirdPartyNoticesSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val n = LocalThemeTokens.current.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val sheetPrimary = n.textPrimary.asColor()
    val sheetSecondary = n.textSecondary.asColor()
    val title = stringResource(R.string.settings_licenses)
    val notices = remember(context) {
        context.resources.openRawResource(R.raw.third_party_notices).bufferedReader().use {
            readNoticeSections(it.readText())
        }
    }
    var visible by rememberSaveable { mutableStateOf(true) }
    var expandedProject by rememberSaveable { mutableStateOf<String?>(null) }

    fun close(): Boolean {
        visible = false
        return true
    }

    GlassSheet(visible, ::close, onDismiss, title) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    title,
                    Modifier.weight(1f).semantics { heading() },
                    style = TextStyle(sheetPrimary, 20.sp, if (isP5) FontWeight.Black else FontWeight.SemiBold),
                )
                TextButton(
                    onClick = { close() },
                    colors = ButtonDefaults.textButtonColors(contentColor = sheetSecondary),
                ) { Text(stringResource(R.string.overlay_close), fontWeight = if (isP5) FontWeight.Bold else FontWeight.Normal) }
            }
            for (project in thirdPartyProjects) {
                val expanded = expandedProject == project.name
                ThirdPartyProjectRow(project, notices, expanded) {
                    expandedProject = if (expanded) null else project.name
                }
            }
        }
    }
}

@Composable
private fun ThirdPartyProjectRow(
    project: ThirdPartyProject,
    notices: Map<String, String>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val p5 = LocalP5Tokens.current
    val p5Stroke = if (t.dark) p5.strokeLight.asColor() else p5.strokeDark.asColor()
    val rowShape = if (isP5) RoundedCornerShape(2.dp) else RoundedCornerShape(12.dp)
    val rowBg = if (isP5) {
        if (t.dark) Color(0xFF1B1B20) else n.surfaceMuted.asColor()
    } else {
        n.surface.asColor()
    }
    val rowBorder = if (isP5) Modifier.border(1.5.dp, p5Stroke.copy(alpha = if (t.dark) 0.65f else 0.85f), rowShape) else Modifier
    val primaryColor = n.textPrimary.asColor()
    val secondaryColor = n.textSecondary.asColor()
    val expansionState = stringResource(if (expanded) R.string.notice_expanded else R.string.notice_collapsed)
    Column(
        Modifier.fillMaxWidth().clip(rowShape).background(rowBg, rowShape).then(rowBorder),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { stateDescription = expansionState }
                .clickable(
                    interactionSource = null,
                    indication = ripple(color = primaryColor),
                    role = Role.Button,
                    onClick = onToggle,
                )
                .padding(start = 12.dp, top = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(project.name, style = TextStyle(primaryColor, 16.sp, if (isP5) FontWeight.Bold else FontWeight.SemiBold))
                BasicText(stringResource(project.use), style = TextStyle(secondaryColor, 14.sp))
                BasicText(stringResource(project.license), style = TextStyle(secondaryColor, 14.sp))
            }
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = secondaryColor,
            )
        }
        BrowserLink(project.url.removePrefix("https://"), project.url)
        if (expanded) {
            val details = remember(project, notices) {
                val attribution = notices.getValue(project.name).lineSequence()
                    .filterNot { line ->
                        line.startsWith("- Project: ") ||
                            project.termsLinks.any { link -> line.endsWith(": ${link.url}") }
                    }
                    .joinToString("\n") { it.trimStart() }
                val license = project.licenseSection?.let { formatNoticeLicense(notices.getValue(it)) }
                listOfNotNull(attribution, license).joinToString("\n\n")
            }
            SelectionContainer(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                BasicText(details, style = TextStyle(color = primaryColor, fontSize = 14.sp, lineHeight = 21.sp))
            }
            for (link in project.termsLinks) {
                BrowserLink(stringResource(link.label), link.url)
            }
        }
    }
}

private const val ACTIARY_GITHUB_URL = "https://github.com/sanren1/Actiary"

private fun openExternalUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .apply {
                // Match generic HTTPS handlers while delivering the actual project URL.
                selector = Intent(Intent.ACTION_VIEW, Uri.fromParts("https", "", null))
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.notice_browser_unavailable, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun BrowserLink(label: String, url: String) {
    val context = LocalContext.current
    val t = LocalThemeTokens.current
    val isP5 = LocalUiStyle.current == UiStyle.PERSONA_5
    val textColor = if (isP5) t.accent.accentAction.asColor() else t.neutrals.textPrimary.asColor()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(
                interactionSource = null,
                indication = ripple(color = textColor),
                role = Role.Button,
                onClickLabel = stringResource(R.string.notice_open_browser),
            ) { openExternalUrl(context, url) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            label,
            Modifier.weight(1f),
            style = TextStyle(color = textColor, fontSize = 14.sp, textDecoration = TextDecoration.Underline),
        )
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp), tint = textColor)
    }
}
