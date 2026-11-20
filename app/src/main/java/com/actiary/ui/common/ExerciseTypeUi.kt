package com.actiary.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.actiary.R
import com.actiary.data.db.ExerciseTypeEntity

@Composable
fun exerciseTypeLabel(type: ExerciseTypeEntity): String = when (type.builtinKey) {
    "running" -> stringResource(R.string.type_running)
    "strength" -> stringResource(R.string.type_strength)
    "cycling" -> stringResource(R.string.type_cycling)
    "swimming" -> stringResource(R.string.type_swimming)
    "yoga" -> stringResource(R.string.type_yoga)
    "hiking" -> stringResource(R.string.type_hiking)
    "ball" -> stringResource(R.string.type_ball)
    "freeform" -> stringResource(R.string.type_freeform)
    else -> type.name.orEmpty()
}

fun exerciseTypeIcon(type: ExerciseTypeEntity): ImageVector = when (type.builtinKey) {
    "running" -> Icons.AutoMirrored.Outlined.DirectionsRun
    "strength" -> Icons.Outlined.FitnessCenter
    "cycling" -> Icons.AutoMirrored.Outlined.DirectionsBike
    "swimming" -> Icons.Outlined.Pool
    "yoga" -> Icons.Outlined.SelfImprovement
    "hiking" -> Icons.Outlined.Hiking
    "ball" -> Icons.Outlined.SportsBasketball
    "freeform" -> Icons.Outlined.FitnessCenter
    else -> Icons.Outlined.Star
}
