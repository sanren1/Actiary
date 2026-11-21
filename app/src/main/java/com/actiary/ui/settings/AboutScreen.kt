package com.actiary.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
        context.resources.openRawResource(R.raw.third_party_notices).bufferedReader().use {
            readNoticeSections(it.readText())
        }
    }
    var expandedProject by rememberSaveable { mutableStateOf<String?>(null) }

    SettingsPageLayout(stringResource(R.string.settings_about), onBack) {
        BasicText(
            stringResource(R.string.app_name),
            style = TextStyle(n.textPrimary.asColor(), 22.sp, FontWeight.SemiBold),
        )
        BasicText(stringResource(R.string.settings_about_version, version), style = TextStyle(n.textSecondary.asColor(), 16.sp))
        BasicText(
            stringResource(R.string.settings_licenses),
            Modifier.semantics { heading() },
            style = TextStyle(n.textPrimary.asColor(), 18.sp, FontWeight.SemiBold),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val n = LocalThemeTokens.current.neutrals
    val expansionState = stringResource(if (expanded) R.string.notice_expanded else R.string.notice_collapsed)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(n.surface.asColor()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { stateDescription = expansionState }
                .clickable(
                    interactionSource = null,
                    indication = ripple(color = n.textPrimary.asColor()),
                    role = Role.Button,
                    onClick = onToggle,
                )
                .padding(start = 12.dp, top = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BasicText(project.name, style = TextStyle(n.textPrimary.asColor(), 16.sp, FontWeight.SemiBold))
                BasicText(stringResource(project.use), style = TextStyle(n.textSecondary.asColor(), 14.sp))
                BasicText(stringResource(project.license), style = TextStyle(n.textSecondary.asColor(), 14.sp))
            }
            Icon(
                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = n.textSecondary.asColor(),
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
                BasicText(details, style = TextStyle(color = n.textPrimary.asColor(), fontSize = 14.sp, lineHeight = 21.sp))
            }
            for (link in project.termsLinks) {
                BrowserLink(stringResource(link.label), link.url)
            }
        }
    }
}

@Composable
private fun BrowserLink(label: String, url: String) {
    val context = LocalContext.current
    val textColor = LocalThemeTokens.current.neutrals.textPrimary.asColor()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(
                interactionSource = null,
                indication = ripple(color = textColor),
                role = Role.Button,
                onClickLabel = stringResource(R.string.notice_open_browser),
            ) {
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
