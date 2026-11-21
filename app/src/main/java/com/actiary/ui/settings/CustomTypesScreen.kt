package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.glass.GlassIconButton
import com.actiary.glass.GlassOverlayPanel
import com.actiary.glass.GlassToggle
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import com.actiary.ui.common.CUSTOM_TYPE_ICONS
import com.actiary.ui.common.exerciseTypeIcon
import com.actiary.ui.common.exerciseTypeLabel
import kotlinx.coroutines.launch

@Composable
fun CustomTypesScreen(
    repository: RecordRepository,
    onBack: () -> Unit,
    iconKey: String,
    onRequestIconPicker: () -> Unit,
    onResetIcon: () -> Unit,
    onRequestRemoval: (Long) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val types by repository.types.collectAsStateWithLifecycle(emptyList())
    var name by rememberSaveable { mutableStateOf("") }
    var hasDistance by rememberSaveable { mutableStateOf(false) }
    val selectedIcon = CUSTOM_TYPE_ICONS.first { it.key == iconKey }
    val canAdd = name.trim().isNotEmpty()

    SettingsPageLayout(stringResource(R.string.settings_custom_types), onBack) {
        Column(
            Modifier.fillMaxWidth().background(n.surface.asColor(), RoundedCornerShape(16.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BasicText(stringResource(R.string.custom_type_name), style = TextStyle(n.textSecondary.asColor(), 13.sp))
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(n.textPrimary.asColor(), 15.sp),
                cursorBrush = SolidColor(a.accentAction.asColor()),
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(n.surfaceMuted.asColor())
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )

            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.custom_type_choose_icon)) {
                        focus.clearFocus()
                        onRequestIconPicker()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(stringResource(R.string.custom_type_icon), style = TextStyle(n.textPrimary.asColor(), 16.sp))
                Box(
                    Modifier.size(48.dp).background(n.surfaceMuted.asColor(), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(selectedIcon.vector, stringResource(selectedIcon.label), tint = n.textPrimary.asColor())
                }
            }

            val distanceLabel = stringResource(R.string.custom_type_has_distance)
            GlassToggle(
                checked = hasDistance,
                onCheckedChange = { hasDistance = it },
                modifier = Modifier.fillMaxWidth(),
                label = distanceLabel,
            ) {
                BasicText(distanceLabel, style = TextStyle(n.textPrimary.asColor(), 16.sp))
            }

            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canAdd) a.accentAction.asColor() else n.surfaceMuted.asColor())
                    .then(
                        if (canAdd) Modifier.clickable(role = Role.Button) {
                            val trimmed = name.trim()
                            if (trimmed.isEmpty()) return@clickable
                            val trackDistance = hasDistance
                            name = ""
                            hasDistance = false
                            onResetIcon()
                            scope.launch { repository.addType(trimmed, trackDistance, iconKey) }
                        } else Modifier,
                    )
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    stringResource(R.string.custom_type_add),
                    style = TextStyle(if (canAdd) a.onAccentAction.asColor() else n.textSecondary.asColor(), 16.sp),
                )
            }
        }

        types.forEachIndexed { index, type ->
            key(type.id) {
                val label = exerciseTypeLabel(type)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .background(n.surface.asColor(), RoundedCornerShape(12.dp))
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(exerciseTypeIcon(type), null, Modifier.padding(end = 12.dp).size(24.dp), tint = n.textPrimary.asColor())
                    Column(Modifier.weight(1f)) {
                        BasicText(label, style = TextStyle(n.textPrimary.asColor(), 16.sp))
                        BasicText(
                            stringResource(if (type.hasDistance) R.string.custom_type_with_distance else R.string.custom_type_no_distance),
                            style = TextStyle(n.textSecondary.asColor(), 12.sp),
                        )
                    }
                    GlassIconButton(
                        Icons.Outlined.KeyboardArrowUp,
                        stringResource(R.string.custom_type_move_up, label),
                        onClick = { scope.launch { repository.moveType(type.id, -1) } },
                        enabled = index > 0,
                        glass = false,
                    )
                    GlassIconButton(
                        Icons.Outlined.KeyboardArrowDown,
                        stringResource(R.string.custom_type_move_down, label),
                        onClick = { scope.launch { repository.moveType(type.id, 1) } },
                        enabled = index < types.lastIndex,
                        glass = false,
                    )
                    if (type.builtinKey == null) {
                        GlassIconButton(
                            Icons.Outlined.DeleteOutline,
                            stringResource(R.string.custom_type_delete_named, label),
                            onClick = { onRequestRemoval(type.id) },
                            glass = false,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTypeIconPicker(
    visible: Boolean,
    iconKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val title = stringResource(R.string.custom_type_choose_icon)
    GlassOverlayPanel(visible, onDismiss, PaddingValues(16.dp), title) {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, modifier = Modifier.semantics { heading() }, color = n.textPrimary.asColor(), fontWeight = FontWeight.Medium)
            FlowRow(
                Modifier.selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CUSTOM_TYPE_ICONS.forEach { icon ->
                    val selected = icon.key == iconKey
                    val label = stringResource(icon.label)
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        Modifier.size(48.dp)
                            .clip(shape)
                            .background(if (selected) a.accentSubtle.asColor() else n.surfaceMuted.asColor())
                            .border(if (selected) 2.dp else 1.dp, if (selected) a.accentAction.asColor() else n.divider.asColor(), shape)
                            .semantics { contentDescription = label }
                            .selectable(selected, role = Role.RadioButton) { onSelect(icon.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon.vector, null, tint = n.textPrimary.asColor())
                    }
                }
            }
            TextButton(
                onDismiss,
                Modifier.align(Alignment.End).heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = n.textSecondary.asColor()),
            ) { Text(stringResource(R.string.record_cancel)) }
        }
    }
}
