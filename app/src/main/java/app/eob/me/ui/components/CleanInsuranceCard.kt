package app.eob.me.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings

private val CardBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1A3D63),
        Color(0xFF102A47),
        Color(0xFF0A1E38)
    ),
    start = Offset(0f, 0f),
    end = Offset(900f, 1200f)
)
private val InsuranceNameAccent = Color(0xFF7DD3FC)
private val CardPrimaryText = Color.White
private val CardSecondaryText = Color(0xFFB8D4EA)
private val CardDividerColor = Color.White.copy(alpha = 0.22f)

@Composable
fun CleanInsuranceCard(
    language: AppLanguage,
    insuranceName: String,
    memberId: String,
    groupNumber: String,
    pcpCopay: String,
    specialistCopay: String,
    footerLocation: String,
    verificationCode: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackgroundGradient)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EobInsuranceCardMark(modifier = Modifier.size(36.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = EobStrings.t(language, "cleanInsuranceCompanySectionLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = CardSecondaryText,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = insuranceName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InsuranceNameAccent,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            HorizontalDivider(color = CardDividerColor, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = EobStrings.t(language, "cleanInsuranceMemberIdLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        color = CardSecondaryText
                    )
                    Text(
                        text = EobStrings.t(language, "cleanInsuranceMemberIdSectionLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CardSecondaryText,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = memberId.uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = CardPrimaryText,
                        letterSpacing = 1.sp
                    )
                }
                VerifiedInsuranceBadge(modifier = Modifier.size(44.dp))
            }

            HorizontalDivider(color = CardDividerColor, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${EobStrings.t(language, "cleanInsuranceGroupNumberLabel")} " +
                            EobStrings.t(language, "cleanInsuranceGroupSectionLabel"),
                        style = MaterialTheme.typography.labelSmall,
                        color = CardSecondaryText,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = groupNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CardPrimaryText
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${EobStrings.t(language, "cleanInsuranceCopayLabel")} " +
                            EobStrings.t(language, "cleanInsuranceCopaySectionDetail"),
                        style = MaterialTheme.typography.labelSmall,
                        color = CardSecondaryText,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = EobStrings.tf(
                            language,
                            "cleanInsuranceCopayFormat",
                            pcpCopay,
                            specialistCopay
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = CardPrimaryText
                    )
                }
            }

            HorizontalDivider(color = CardDividerColor, thickness = 1.dp)

            Text(
                text = EobStrings.tf(
                    language,
                    "cleanInsuranceVerificationFooter",
                    footerLocation,
                    verificationCode
                ),
                style = MaterialTheme.typography.labelSmall,
                color = CardSecondaryText
            )
        }
    }
}

@Composable
private fun EobInsuranceCardMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2498EA), Color(0xFF0E45BE))
                ),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "E",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun VerifiedInsuranceBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = radius
        )
        drawCircle(
            color = Color.White,
            radius = radius * 0.72f,
            style = Stroke(width = radius * 0.08f)
        )
        val checkPath = Path().apply {
            moveTo(center.x - radius * 0.28f, center.y)
            lineTo(center.x - radius * 0.04f, center.y + radius * 0.24f)
            lineTo(center.x + radius * 0.30f, center.y - radius * 0.22f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round)
        )
        listOf(
            Offset(center.x + radius * 0.82f, center.y - radius * 0.72f),
            Offset(center.x + radius * 0.95f, center.y - radius * 0.18f),
            Offset(center.x - radius * 0.88f, center.y - radius * 0.62f)
        ).forEach { sparkle ->
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = radius * 0.06f, center = sparkle)
        }
    }
}
