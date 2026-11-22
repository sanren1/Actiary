package com.actiary.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DownhillSkiing
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Kayaking
import androidx.compose.material.icons.outlined.Pool
import androidx.compose.material.icons.outlined.Rowing
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Skateboarding
import androidx.compose.material.icons.outlined.Snowboarding
import androidx.compose.material.icons.outlined.Sports
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsFootball
import androidx.compose.material.icons.outlined.SportsGolf
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material.icons.outlined.SportsMartialArts
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.SportsTennis
import androidx.compose.material.icons.outlined.SportsVolleyball
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Surfing
import androidx.compose.material.icons.outlined.Terrain
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.actiary.R
import com.actiary.data.db.ExerciseTypeEntity

@Composable
fun exerciseTypeLabel(type: ExerciseTypeEntity): String = type.name ?: when (type.builtinKey) {
    "running" -> stringResource(R.string.type_running)
    "strength" -> stringResource(R.string.type_strength)
    "cycling" -> stringResource(R.string.type_cycling)
    "swimming" -> stringResource(R.string.type_swimming)
    "yoga" -> stringResource(R.string.type_yoga)
    "hiking" -> stringResource(R.string.type_hiking)
    "ball" -> stringResource(R.string.type_ball)
    "freeform" -> stringResource(R.string.type_freeform)
    else -> ""
}

class TypeIcon(val key: String, val vector: ImageVector, @StringRes val label: Int)

/** Icons offered for custom types; the first is the default. Keys are persisted, never rename them. */
val CUSTOM_TYPE_ICONS = listOf(
    TypeIcon("gym", Icons.Outlined.FitnessCenter, R.string.icon_gym),
    TypeIcon("stretch", Icons.Outlined.SelfImprovement, R.string.icon_stretch),
    TypeIcon("gymnastics", Icons.Outlined.SportsGymnastics, R.string.icon_gymnastics),
    TypeIcon("martial", Icons.Outlined.SportsMartialArts, R.string.icon_martial),
    TypeIcon("racket", Icons.Outlined.SportsTennis, R.string.icon_racket),
    TypeIcon("basketball", Icons.Outlined.SportsBasketball, R.string.icon_basketball),
    TypeIcon("soccer", Icons.Outlined.SportsSoccer, R.string.icon_soccer),
    TypeIcon("volleyball", Icons.Outlined.SportsVolleyball, R.string.icon_volleyball),
    TypeIcon("baseball", Icons.Outlined.SportsBaseball, R.string.icon_baseball),
    TypeIcon("football", Icons.Outlined.SportsFootball, R.string.icon_football),
    TypeIcon("golf", Icons.Outlined.SportsGolf, R.string.icon_golf),
    TypeIcon("climb", Icons.Outlined.Terrain, R.string.icon_climb),
    TypeIcon("ski", Icons.Outlined.DownhillSkiing, R.string.icon_ski),
    TypeIcon("snowboard", Icons.Outlined.Snowboarding, R.string.icon_snowboard),
    TypeIcon("rowing", Icons.Outlined.Rowing, R.string.icon_rowing),
    TypeIcon("kayak", Icons.Outlined.Kayaking, R.string.icon_kayak),
    TypeIcon("surf", Icons.Outlined.Surfing, R.string.icon_surf),
    TypeIcon("walk", Icons.AutoMirrored.Outlined.DirectionsWalk, R.string.icon_walk),
    TypeIcon("skate", Icons.Outlined.Skateboarding, R.string.icon_skate),
    TypeIcon("sports", Icons.Outlined.Sports, R.string.icon_sports),
)

fun exerciseTypeIcon(type: ExerciseTypeEntity): ImageVector = CUSTOM_TYPE_ICONS.firstOrNull { it.key == type.iconKey }?.vector ?: when (type.builtinKey) {
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
