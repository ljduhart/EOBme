package app.eob.me.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.eob.me.data.SubscriptionTier
import app.eob.me.ui.theme.EobSubscriptionFree
import app.eob.me.ui.theme.EobSubscriptionGold
import app.eob.me.ui.theme.EobSubscriptionSilver

object SubscriptionTierIcon {
    fun tintFor(tier: SubscriptionTier): Color = when (tier) {
        SubscriptionTier.Free -> EobSubscriptionFree
        SubscriptionTier.Silver -> EobSubscriptionSilver
        SubscriptionTier.Gold -> EobSubscriptionGold
    }

    fun iconFor(tier: SubscriptionTier): ImageVector = when (tier) {
        SubscriptionTier.Free -> Shield
        SubscriptionTier.Silver, SubscriptionTier.Gold -> Crown
    }

    private val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "SubscriptionShield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Unspecified)) {
                moveTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(11f)
                curveTo(4f, 16.55f, 7.84f, 21.74f, 12f, 23f)
                curveTo(16.16f, 21.74f, 20f, 16.55f, 20f, 11f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                close()
            }
        }.build()
    }

    private val Crown: ImageVector by lazy {
        ImageVector.Builder(
            name = "SubscriptionCrown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Unspecified)) {
                moveTo(5f, 16f)
                lineTo(3f, 7f)
                lineTo(8.5f, 10f)
                lineTo(12f, 4f)
                lineTo(15.5f, 10f)
                lineTo(21f, 7f)
                lineTo(19f, 16f)
                horizontalLineTo(5f)
                close()
                moveTo(5f, 18f)
                horizontalLineTo(19f)
                verticalLineTo(20f)
                horizontalLineTo(5f)
                verticalLineTo(18f)
                close()
            }
        }.build()
    }
}
