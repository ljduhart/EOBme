package app.eob.me.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Gear icon for hub Settings entry in the top-right header. */
object HubSettingsGearIcon {
    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "HubSettingsGear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val fill = SolidColor(app.eob.me.ui.theme.EobBrandBlue)
            path(fill = fill) {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.31f, 0.06f, -0.63f, 0.06f, -0.94f)
                curveToRelative(0f, -0.31f, -0.02f, -0.63f, -0.06f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.62f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineTo(14.4f, 2.81f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0f, -0.44f, 0.17f, -0.48f, 0.41f)
                lineTo(9.25f, 5.35f)
                curveTo(8.66f, 5.59f, 8.12f, 5.91f, 7.63f, 6.29f)
                lineTo(5.24f, 5.33f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
                lineTo(2.73f, 8.87f)
                curveTo(2.62f, 9.08f, 2.66f, 9.34f, 2.86f, 9.48f)
                lineTo(4.89f, 11.06f)
                curveTo(4.85f, 11.37f, 4.83f, 11.69f, 4.83f, 12f)
                reflectiveCurveToRelative(0.02f, 0.63f, 0.06f, 0.94f)
                lineTo(2.86f, 14.52f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.62f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.04f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.48f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.62f)
                close()
                moveTo(12f, 15.6f)
                arcToRelative(3.6f, 3.6f, 0f, true, true, 0f, -7.2f)
                arcToRelative(3.6f, 3.6f, 0f, true, true, 0f, 7.2f)
                close()
            }
        }.build()
    }
}
