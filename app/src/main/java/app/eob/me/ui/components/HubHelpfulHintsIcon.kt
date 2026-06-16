package app.eob.me.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Yellow lightbulb for Settings helpful hints entry beside the Legal tab. */
object HubHelpfulHintsIcon {
    private val HintYellow = Color(0xFFFFC107)

    val Lightbulb: ImageVector by lazy {
        ImageVector.Builder(
            name = "HubHelpfulHintsLightbulb",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val fill = SolidColor(HintYellow)
            path(fill = fill) {
                moveTo(9f, 21f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-1f)
                horizontalLineToRelative(-6f)
                close()
                moveTo(12f, 2f)
                arcTo(7f, 7f, 0f, false, false, 5f, 9f)
                curveToRelative(0f, 2.38f, 1.19f, 4.47f, 3f, 5.74f)
                verticalLineTo(17f)
                arcToRelative(1f, 1f, 0f, false, false, 1f, 1f)
                horizontalLineToRelative(6f)
                arcToRelative(1f, 1f, 0f, false, false, 1f, -1f)
                verticalLineToRelative(-2.26f)
                curveToRelative(1.81f, -1.27f, 3f, -3.36f, 3f, -5.74f)
                arcTo(7f, 7f, 0f, false, false, 12f, 2f)
                close()
                moveTo(10.5f, 18f)
                verticalLineToRelative(-0.5f)
                horizontalLineToRelative(3f)
                verticalLineTo(18f)
                horizontalLineToRelative(-3f)
                close()
            }
        }.build()
    }
}
