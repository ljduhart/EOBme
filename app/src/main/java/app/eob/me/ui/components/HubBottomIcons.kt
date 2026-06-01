package app.eob.me.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val BrandBlue = Color(0xFF2498EA)

/** Monochromatic vector icons for the hub bottom navigation bar. */
object hubBottomIcons {
    val Dashboard: ImageVector by lazy {
        ImageVector.Builder(
            name = "HubDashboard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(BrandBlue)) {
                moveTo(4f, 4f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(4f)
                close()
                moveTo(14f, 4f)
                horizontalLineTo(20f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
                close()
                moveTo(4f, 14f)
                horizontalLineTo(10f)
                verticalLineTo(20f)
                horizontalLineTo(4f)
                close()
                moveTo(14f, 14f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
    }

    val ScanEob: ImageVector by lazy {
        ImageVector.Builder(
            name = "HubScanEob",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(BrandBlue)) {
                moveTo(9f, 3f)
                lineTo(7.17f, 5f)
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                verticalLineTo(18f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
                horizontalLineTo(20f)
                arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
                verticalLineTo(7f)
                arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
                horizontalLineTo(16.83f)
                lineTo(15f, 3f)
                close()
                moveTo(12f, 17f)
                arcToRelative(5f, 5f, 0f, true, true, 0f, -10f)
                arcToRelative(5f, 5f, 0f, true, true, 0f, 10f)
                close()
            }
        }.build()
    }

    val Profile: ImageVector by lazy {
        ImageVector.Builder(
            name = "HubProfile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(BrandBlue)) {
                moveTo(12f, 12f)
                arcToRelative(4.5f, 4.5f, 0f, true, true, 0f, -9f)
                arcToRelative(4.5f, 4.5f, 0f, true, true, 0f, 9f)
                close()
                moveTo(6f, 20f)
                arcToRelative(6f, 6f, 0f, false, true, 12f, 0f)
                verticalLineTo(19f)
                arcToRelative(4f, 4f, 0f, false, false, -8f, 0f)
                close()
            }
        }.build()
    }
}
