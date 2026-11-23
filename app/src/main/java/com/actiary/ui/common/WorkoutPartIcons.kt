package com.actiary.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 24dp anatomical body-part close-up icons (2A style):
 * airy low-alpha body silhouette + sculpted muscle contour with subtle tint fill +
 * fine multi-line muscle-fiber striations. Inherits single-color [androidx.compose.material3.Icon] tint.
 */
internal object WorkoutPartIcons {
    private val ink = SolidColor(Color.Black)

    private fun buildIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    private fun ImageVector.Builder.bodyOutline(block: PathBuilder.() -> Unit) {
        path(
            stroke = ink,
            strokeAlpha = 0.42f,
            strokeLineWidth = 1.35f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    private fun ImageVector.Builder.muscleOutline(
        strokeWidth: Float = 1.4f,
        fillAlpha: Float = 0.16f,
        block: PathBuilder.() -> Unit,
    ) {
        path(
            fill = ink,
            fillAlpha = fillAlpha,
            stroke = ink,
            strokeAlpha = 1f,
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    private fun ImageVector.Builder.fiberLines(
        strokeWidth: Float = 0.72f,
        alpha: Float = 0.78f,
        block: PathBuilder.() -> Unit,
    ) {
        path(
            stroke = ink,
            strokeAlpha = alpha,
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }

    /** Chest: airy upper torso contour + sculpted dual pectorals with radiating fiber striations. */
    val Chest: ImageVector by lazy {
        buildIcon("WorkoutChest") {
            bodyOutline {
                moveTo(9.6f, 3.5f)
                curveTo(10.5f, 4.4f, 13.5f, 4.4f, 14.4f, 3.5f)
                lineTo(19.2f, 6.2f)
                curveTo(20.4f, 6.9f, 21f, 8.2f, 20.8f, 9.6f)
                lineTo(20.1f, 13.8f)
                lineTo(17.6f, 13.2f)
                lineTo(16.7f, 20.5f)
                moveTo(9.6f, 3.5f)
                lineTo(4.8f, 6.2f)
                curveTo(3.6f, 6.9f, 3f, 8.2f, 3.2f, 9.6f)
                lineTo(3.9f, 13.8f)
                lineTo(6.4f, 13.2f)
                lineTo(7.3f, 20.5f)
            }
            // Left pectoral
            muscleOutline {
                moveTo(6.4f, 8.7f)
                curveTo(7.8f, 8.3f, 9.8f, 8.4f, 11.2f, 9.1f)
                verticalLineTo(12.4f)
                curveTo(10f, 13.2f, 7.8f, 13.3f, 6.6f, 12.1f)
                curveTo(6.1f, 11.5f, 6f, 9.8f, 6.4f, 8.7f)
                close()
            }
            // Right pectoral
            muscleOutline {
                moveTo(17.6f, 8.7f)
                curveTo(16.2f, 8.3f, 14.2f, 8.4f, 12.8f, 9.1f)
                verticalLineTo(12.4f)
                curveTo(14f, 13.2f, 16.2f, 13.3f, 17.4f, 12.1f)
                curveTo(17.9f, 11.5f, 18f, 9.8f, 17.6f, 8.7f)
                close()
            }
            // Fine radiating pectoral fiber striations (3 per side)
            fiberLines {
                // Left pec fibers
                moveTo(7.2f, 9.6f)
                quadTo(8.8f, 9.5f, 10.3f, 9.8f)
                moveTo(7.1f, 10.7f)
                quadTo(8.8f, 10.9f, 10.3f, 10.6f)
                moveTo(7.6f, 11.8f)
                quadTo(9f, 12.1f, 10.2f, 11.6f)
                // Right pec fibers
                moveTo(16.8f, 9.6f)
                quadTo(15.2f, 9.5f, 13.7f, 9.8f)
                moveTo(16.9f, 10.7f)
                quadTo(15.2f, 10.9f, 13.7f, 10.6f)
                moveTo(16.4f, 11.8f)
                quadTo(15f, 12.1f, 13.8f, 11.6f)
            }
        }
    }

    /** Back: airy V-taper rear torso + sculpted lat wings with fan-shaped fiber striations. */
    val Back: ImageVector by lazy {
        buildIcon("WorkoutBack") {
            bodyOutline {
                moveTo(9.7f, 3.2f)
                horizontalLineTo(14.3f)
                lineTo(19.4f, 6f)
                curveTo(20.5f, 6.6f, 21f, 7.9f, 20.7f, 9.2f)
                lineTo(19.9f, 13f)
                lineTo(17.6f, 12.3f)
                lineTo(15.9f, 20.5f)
                moveTo(9.7f, 3.2f)
                lineTo(4.6f, 6f)
                curveTo(3.5f, 6.6f, 3f, 7.9f, 3.3f, 9.2f)
                lineTo(4.1f, 13f)
                lineTo(6.4f, 12.3f)
                lineTo(8.1f, 20.5f)
            }
            // Left lat / upper back wing
            muscleOutline {
                moveTo(11.1f, 6.7f)
                lineTo(6.5f, 8.3f)
                curveTo(5.9f, 10.3f, 6.5f, 12.7f, 8f, 15.1f)
                lineTo(9.3f, 17.2f)
                lineTo(11.1f, 16f)
                verticalLineTo(6.7f)
                close()
            }
            // Right lat / upper back wing
            muscleOutline {
                moveTo(12.9f, 6.7f)
                lineTo(17.5f, 8.3f)
                curveTo(18.1f, 10.3f, 17.5f, 12.7f, 16f, 15.1f)
                lineTo(14.7f, 17.2f)
                lineTo(12.9f, 16f)
                verticalLineTo(6.7f)
                close()
            }
            // Fine fan-shaped lat striations (4 per side) + subtle spine accent
            fiberLines {
                // Left lats
                moveTo(7.4f, 9.1f)
                lineTo(10.1f, 8.6f)
                moveTo(7.3f, 10.6f)
                lineTo(10.1f, 11.4f)
                moveTo(7.9f, 12.4f)
                lineTo(10.1f, 13.4f)
                moveTo(8.7f, 14.3f)
                lineTo(10.2f, 15.2f)
                // Right lats
                moveTo(16.6f, 9.1f)
                lineTo(13.9f, 8.6f)
                moveTo(16.7f, 10.6f)
                lineTo(13.9f, 11.4f)
                moveTo(16.1f, 12.4f)
                lineTo(13.9f, 13.4f)
                moveTo(15.3f, 14.3f)
                lineTo(13.8f, 15.2f)
            }
        }
    }

    /** Shoulders: airy upper torso + sculpted rounded deltoid caps with multi-head fiber arcs. */
    val Shoulders: ImageVector by lazy {
        buildIcon("WorkoutShoulders") {
            bodyOutline {
                moveTo(9.6f, 3.5f)
                curveTo(10.5f, 4.4f, 13.5f, 4.4f, 14.4f, 3.5f)
                lineTo(19.2f, 6.2f)
                curveTo(20.4f, 6.9f, 21f, 8.3f, 20.8f, 9.8f)
                lineTo(20f, 15.2f)
                horizontalLineTo(17.3f)
                lineTo(16.6f, 20.5f)
                moveTo(9.6f, 3.5f)
                lineTo(4.8f, 6.2f)
                curveTo(3.6f, 6.9f, 3f, 8.3f, 3.2f, 9.8f)
                lineTo(4f, 15.2f)
                horizontalLineTo(6.7f)
                lineTo(7.4f, 20.5f)
            }
            // Left deltoid cap
            muscleOutline {
                moveTo(8.4f, 6f)
                curveTo(5.6f, 6.5f, 3.6f, 8.1f, 3.7f, 10.5f)
                curveTo(3.8f, 12f, 4.4f, 13.1f, 5.3f, 13.9f)
                lineTo(7.4f, 11.7f)
                curveTo(7.5f, 9.6f, 8f, 7.6f, 8.4f, 6f)
                close()
            }
            // Right deltoid cap
            muscleOutline {
                moveTo(15.6f, 6f)
                curveTo(18.4f, 6.5f, 20.4f, 8.1f, 20.3f, 10.5f)
                curveTo(20.2f, 12f, 19.6f, 13.1f, 18.7f, 13.9f)
                lineTo(16.6f, 11.7f)
                curveTo(16.5f, 9.6f, 16f, 7.6f, 15.6f, 6f)
                close()
            }
            // Fine deltoid fiber arcs (3 per side)
            fiberLines {
                // Left deltoid heads
                moveTo(7.3f, 7.2f)
                quadTo(6.5f, 9.2f, 6.4f, 11.2f)
                moveTo(6.1f, 7.7f)
                quadTo(5.1f, 9.7f, 5.5f, 12f)
                moveTo(4.8f, 9.1f)
                quadTo(4.5f, 10.7f, 4.9f, 12.1f)
                // Right deltoid heads
                moveTo(16.7f, 7.2f)
                quadTo(17.5f, 9.2f, 17.6f, 11.2f)
                moveTo(17.9f, 7.7f)
                quadTo(18.9f, 9.7f, 18.5f, 12f)
                moveTo(19.2f, 9.1f)
                quadTo(19.5f, 10.7f, 19.1f, 12.1f)
            }
        }
    }

    /** Arms: airy flexed arm contour + sculpted bicep peak with curved muscle-fiber striations. */
    val Arms: ImageVector by lazy {
        buildIcon("WorkoutArms") {
            bodyOutline {
                moveTo(8.2f, 5.2f)
                curveTo(9.8f, 4.8f, 11.4f, 5.6f, 11.6f, 7f)
                curveTo(11.7f, 8f, 11f, 8.8f, 10.1f, 9.2f)
                lineTo(9.4f, 13.3f)
                curveTo(11.1f, 10.9f, 14.8f, 10.1f, 17.6f, 11.8f)
                curveTo(19.6f, 13f, 20.3f, 15.4f, 19.2f, 17.4f)
                curveTo(18.3f, 18.9f, 16.5f, 19.5f, 14.5f, 19.5f)
                horizontalLineTo(7.8f)
                curveTo(5.4f, 19.5f, 4.2f, 17.6f, 4.7f, 15.2f)
                lineTo(6.4f, 7.4f)
                curveTo(6.6f, 6.2f, 7.2f, 5.4f, 8.2f, 5.2f)
                close()
            }
            // Sculpted bicep peak
            muscleOutline {
                moveTo(10.1f, 13.9f)
                curveTo(11.4f, 11.4f, 14.7f, 10.7f, 17.2f, 12.2f)
                curveTo(18.4f, 13f, 19f, 14.2f, 18.8f, 15.5f)
                curveTo(16.5f, 16.2f, 12.4f, 15.8f, 10.1f, 13.9f)
                close()
            }
            // Fine bicep striations (3 curves) + subtle forearm/tricep contour
            fiberLines {
                moveTo(11.6f, 13.1f)
                quadTo(14.3f, 11.9f, 17.2f, 13.3f)
                moveTo(11.4f, 14.1f)
                quadTo(14.5f, 13.4f, 17.7f, 14.4f)
                moveTo(12.3f, 14.9f)
                quadTo(14.8f, 14.9f, 17.1f, 15.2f)
            }
            fiberLines(strokeWidth = 0.68f, alpha = 0.48f) {
                moveTo(7.3f, 10.5f)
                lineTo(6.5f, 14.8f)
                moveTo(8.5f, 17.1f)
                quadTo(12.2f, 17.8f, 16.2f, 17.3f)
            }
        }
    }

    /** Legs: airy athletic thigh-to-calf silhouette + sculpted quadriceps with longitudinal fiber sweeps. */
    val Legs: ImageVector by lazy {
        buildIcon("WorkoutLegs") {
            bodyOutline {
                moveTo(6.8f, 3.2f)
                curveTo(5.2f, 6.8f, 5f, 11f, 6.6f, 15.2f)
                lineTo(7.2f, 20.8f)
                moveTo(10.2f, 20.8f)
                lineTo(10.8f, 14.8f)
                lineTo(12f, 10.2f)
                lineTo(13.2f, 14.8f)
                lineTo(13.8f, 20.8f)
                moveTo(16.8f, 20.8f)
                lineTo(17.4f, 15.2f)
                curveTo(19f, 11f, 18.8f, 6.8f, 17.2f, 3.2f)
            }
            // Left quadriceps
            muscleOutline {
                moveTo(7.7f, 6.3f)
                curveTo(6.5f, 8.5f, 6.3f, 11.4f, 7.5f, 14.2f)
                lineTo(9.4f, 14.5f)
                curveTo(10.4f, 12f, 10.5f, 9.3f, 9.7f, 6.9f)
                lineTo(7.7f, 6.3f)
                close()
            }
            // Right quadriceps
            muscleOutline {
                moveTo(16.3f, 6.3f)
                curveTo(17.5f, 8.5f, 17.7f, 11.4f, 16.5f, 14.2f)
                lineTo(14.6f, 14.5f)
                curveTo(13.6f, 12f, 13.5f, 9.3f, 14.3f, 6.9f)
                lineTo(16.3f, 6.3f)
                close()
            }
            // Fine longitudinal quad fiber sweeps (3 per thigh)
            fiberLines {
                // Left thigh
                moveTo(7.6f, 8f)
                quadTo(7.2f, 10.4f, 7.8f, 13f)
                moveTo(8.5f, 7.6f)
                quadTo(8.3f, 10.5f, 8.6f, 13.4f)
                moveTo(9.4f, 8.3f)
                quadTo(9.5f, 10.8f, 9.2f, 13.1f)
                // Right thigh
                moveTo(16.4f, 8f)
                quadTo(16.8f, 10.4f, 16.2f, 13f)
                moveTo(15.5f, 7.6f)
                quadTo(15.7f, 10.5f, 15.4f, 13.4f)
                moveTo(14.6f, 8.3f)
                quadTo(14.5f, 10.8f, 14.8f, 13.1f)
            }
        }
    }

    /** Abs: airy waist contour + sculpted 6-pack blocks with fine core & oblique striations. */
    val Abs: ImageVector by lazy {
        buildIcon("WorkoutAbs") {
            bodyOutline {
                moveTo(6.2f, 3.2f)
                curveTo(6.9f, 7.6f, 7.2f, 11.8f, 6.8f, 15.8f)
                lineTo(6.2f, 20.8f)
                moveTo(17.8f, 3.2f)
                curveTo(17.1f, 7.6f, 16.8f, 11.8f, 17.2f, 15.8f)
                lineTo(17.8f, 20.8f)
            }
            // Top-left & top-right abs
            muscleOutline(strokeWidth = 1.3f) {
                roundedRect(8.3f, 5.8f, 11.4f, 8.9f, 0.9f)
                roundedRect(12.6f, 5.8f, 15.7f, 8.9f, 0.9f)
                // Mid-left & mid-right abs
                roundedRect(8.5f, 10.2f, 11.4f, 13.3f, 0.9f)
                roundedRect(12.6f, 10.2f, 15.5f, 13.3f, 0.9f)
                // Lower tapered abs
                moveTo(8.8f, 14.6f)
                horizontalLineTo(11.4f)
                verticalLineTo(17.4f)
                curveTo(11.4f, 18f, 10.9f, 18.4f, 10.3f, 18.2f)
                lineTo(9.4f, 17.8f)
                curveTo(9f, 17.6f, 8.8f, 17.1f, 8.8f, 16.6f)
                close()
                moveTo(15.2f, 14.6f)
                horizontalLineTo(12.6f)
                verticalLineTo(17.4f)
                curveTo(12.6f, 18f, 13.1f, 18.4f, 13.7f, 18.2f)
                lineTo(14.6f, 17.8f)
                curveTo(15f, 17.6f, 15.2f, 17.1f, 15.2f, 16.6f)
                close()
            }
            // Fine inner core texture & oblique/serratus striations
            fiberLines {
                // Inner block fiber accents
                moveTo(9.2f, 7.3f)
                horizontalLineTo(10.5f)
                moveTo(13.5f, 7.3f)
                horizontalLineTo(14.8f)
                moveTo(9.3f, 11.7f)
                horizontalLineTo(10.5f)
                moveTo(13.5f, 11.7f)
                horizontalLineTo(14.7f)
                moveTo(9.6f, 16.1f)
                lineTo(10.6f, 16.4f)
                moveTo(14.4f, 16.1f)
                lineTo(13.4f, 16.4f)
            }
            fiberLines(strokeWidth = 0.65f, alpha = 0.50f) {
                // Left oblique / serratus ticks
                moveTo(7.1f, 6.8f)
                lineTo(7.6f, 8.2f)
                moveTo(7.2f, 10.8f)
                lineTo(7.7f, 12.4f)
                moveTo(7.3f, 14.8f)
                lineTo(7.9f, 16.4f)
                // Right oblique / serratus ticks
                moveTo(16.9f, 6.8f)
                lineTo(16.4f, 8.2f)
                moveTo(16.8f, 10.8f)
                lineTo(16.3f, 12.4f)
                moveTo(16.7f, 14.8f)
                lineTo(16.1f, 16.4f)
            }
        }
    }

    private fun PathBuilder.roundedRect(left: Float, top: Float, right: Float, bottom: Float, r: Float) {
        moveTo(left + r, top)
        horizontalLineTo(right - r)
        quadTo(right, top, right, top + r)
        verticalLineTo(bottom - r)
        quadTo(right, bottom, right - r, bottom)
        horizontalLineTo(left + r)
        quadTo(left, bottom, left, bottom - r)
        verticalLineTo(top + r)
        quadTo(left, top, left + r, top)
        close()
    }
}
