package app.eob.me.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.ProviderNetworkAssurance

private val InNetworkBlue = Color(0xFF2498EA)
private val InNetworkCyan = Color(0xFF00E5FF)
private val PendingGold = Color(0xFFD4AF37)
private val PendingAmber = Color(0xFFF9A825)
private val OutOfNetworkRed = Color(0xFFEF4444)
private val OutOfNetworkOrange = Color(0xFFFF6B35)

@Composable
fun ProviderAssuranceBadge(
    language: AppLanguage,
    assurance: ProviderNetworkAssurance,
    modifier: Modifier = Modifier
) {
    val label = when (assurance) {
        ProviderNetworkAssurance.InNetwork -> EobStrings.t(language, "networkAssuranceInNetwork")
        ProviderNetworkAssurance.PendingVerification -> EobStrings.t(language, "networkAssurancePending")
        ProviderNetworkAssurance.OutOfNetwork -> EobStrings.t(language, "networkAssuranceOutOfNetwork")
    }
    val radiantBrush = when (assurance) {
        ProviderNetworkAssurance.InNetwork -> Brush.linearGradient(
            colors = listOf(InNetworkCyan, InNetworkBlue, InNetworkCyan.copy(alpha = 0.7f))
        )
        ProviderNetworkAssurance.PendingVerification -> Brush.linearGradient(
            colors = listOf(PendingAmber, PendingGold, PendingAmber.copy(alpha = 0.75f))
        )
        ProviderNetworkAssurance.OutOfNetwork -> Brush.linearGradient(
            colors = listOf(OutOfNetworkOrange, OutOfNetworkRed, OutOfNetworkOrange.copy(alpha = 0.75f))
        )
    }
    val textColor = when (assurance) {
        ProviderNetworkAssurance.InNetwork -> Color(0xFF0A3D66)
        ProviderNetworkAssurance.PendingVerification -> Color(0xFF5C4A12)
        ProviderNetworkAssurance.OutOfNetwork -> Color(0xFF5C1414)
    }
    val glowColor = when (assurance) {
        ProviderNetworkAssurance.InNetwork -> InNetworkCyan
        ProviderNetworkAssurance.PendingVerification -> PendingGold
        ProviderNetworkAssurance.OutOfNetwork -> OutOfNetworkRed
    }

    Box(
        modifier = modifier
            .border(width = 1.dp, brush = radiantBrush, shape = RoundedCornerShape(10.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.92f)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}
