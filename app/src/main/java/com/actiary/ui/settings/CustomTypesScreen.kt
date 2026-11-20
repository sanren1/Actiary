package com.actiary.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.actiary.R
import com.actiary.data.RecordRepository
import com.actiary.data.db.ExerciseTypeEntity
import com.actiary.ui.common.exerciseTypeLabel
import com.actiary.theme.LocalThemeTokens
import com.actiary.theme.asColor
import kotlinx.coroutines.launch

/**
 * Custom workout type management page. Lists every type (including archived ones), lets the user
 * create a type, and requests archive confirmation for custom types. All surfaces here are solid;
 * the archive confirmation GlassDialog is rendered by the host in the GlassHost overlay slot.
 */
@Composable
fun CustomTypesScreen(
    repository: RecordRepository,
    onBack: () -> Unit,
    onRequestArchive: (ExerciseTypeEntity) -> Unit,
) {
    val t = LocalThemeTokens.current
    val n = t.neutrals
    val a = t.accent
    val scope = rememberCoroutineScope()
    val allTypes by repository.allTypes.collectAsStateWithLifecycle(emptyList())
    var name by rememberSaveable { mutableStateOf("") }
    var hasDistance by rememberSaveable { mutableStateOf(false) }
    val canAdd = name.trim().isNotEmpty()

    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(stringResource(R.string.nav_back), style = TextStyle(n.textPrimary.asColor(), 16.sp))
        }
        BasicText(
            stringResource(R.string.custom_type_title),
            Modifier.semantics { heading() },
            style = TextStyle(n.textPrimary.asColor(), 28.sp, FontWeight.Bold),
        )

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

            val on = hasDistance
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .semantics { toggleableState = ToggleableState(on) }
                    .clickable(role = Role.Switch) { hasDistance = !hasDistance },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BasicText(stringResource(R.string.custom_type_has_distance), style = TextStyle(n.textPrimary.asColor(), 16.sp))
                Box(
                    Modifier.size(width = 48.dp, height = 28.dp)
                        .background(if (on) a.accentAction.asColor() else n.surfaceMuted.asColor(), CircleShape)
                        .border(1.dp, if (on) a.accentAction.asColor() else n.divider.asColor(), CircleShape)
                        .padding(3.dp),
                    contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        Modifier.size(20.dp).background(
                            if (on) a.onAccentAction.asColor() else n.textSecondary.asColor(), CircleShape,
                        ),
                    )
                }
            }

            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canAdd) a.accentAction.asColor() else n.surfaceMuted.asColor())
                    .then(
                        if (canAdd) Modifier.clickable(role = Role.Button) {
                            val trimmed = name.trim()
                            if (trimmed.isNotEmpty()) {
                                name = ""
                                scope.launch { repository.addType(trimmed, hasDistance) }
                            }
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

        allTypes.forEach { type ->
            val archived = type.archived
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(n.surface.asColor())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .then(if (archived) Modifier.alpha(0.5f) else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    BasicText(
                        if (type.builtinKey != null) exerciseTypeLabel(type) else type.name.orEmpty(),
                        style = TextStyle(n.textPrimary.asColor(), 16.sp),
                    )
                    val badge = stringResource(
                        when {
                            archived -> R.string.custom_type_archived
                            type.hasDistance -> R.string.custom_type_with_distance
                            else -> R.string.custom_type_no_distance
                        },
                    )
                    BasicText(badge, style = TextStyle(n.textSecondary.asColor(), 12.sp))
                }
                // Builtin types are seed data and archived types are final: neither can be archived here.
                if (!archived && type.builtinKey == null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(n.surfaceMuted.asColor())
                            .clickable(role = Role.Button) { onRequestArchive(type) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        BasicText(
                            stringResource(R.string.custom_type_archive),
                            style = TextStyle(n.textPrimary.asColor(), 14.sp),
                        )
                    }
                }
            }
        }
    }
}
