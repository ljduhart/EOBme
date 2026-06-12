package app.eob.me.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceCardDisplay
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobCyberTextSecondary
import app.eob.me.ui.theme.EobInsuranceGradientEnd
import app.eob.me.ui.theme.EobInsuranceGradientMid
import app.eob.me.ui.theme.EobInsuranceGradientStart
import app.eob.me.ui.theme.EobInsuranceNameAccent
import app.eob.me.ui.theme.EobInsuranceSecondaryText

private val CardBackgroundGradient = Brush.linearGradient(
    colors = listOf(
        EobInsuranceGradientStart,
        EobInsuranceGradientMid,
        EobInsuranceGradientEnd
    ),
    start = Offset(0f, 0f),
    end = Offset(900f, 1200f)
)
private val CardPrimaryText = EobCyberTextPrimary
private val CardSecondaryText = EobInsuranceSecondaryText
private val CardDividerColor = EobCyberTextPrimary.copy(alpha = 0.22f)

@Composable
fun CleanInsuranceCard(
    language: AppLanguage,
    display: InsuranceCardDisplay,
    isEditing: Boolean,
    draftInsuranceName: String,
    draftMemberId: String,
    draftGroupNumber: String,
    draftPcpCopay: String,
    draftSpecialistCopay: String,
    canEdit: Boolean,
    onDraftInsuranceNameChange: (String) -> Unit,
    onDraftMemberIdChange: (String) -> Unit,
    onDraftGroupNumberChange: (String) -> Unit,
    onDraftPcpCopayChange: (String) -> Unit,
    onDraftSpecialistCopayChange: (String) -> Unit,
    onEditRequest: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canEdit && !isEditing) {
                    Modifier.clickable(onClick = onEditRequest)
                } else {
                    Modifier
                }
            ),
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
                    InsuranceCardValue(
                        value = if (isEditing) draftInsuranceName else display.insuranceName,
                        onValueChange = onDraftInsuranceNameChange,
                        isEditing = isEditing,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EobInsuranceNameAccent,
                            letterSpacing = 0.5.sp
                        ),
                        displayTransform = { it.uppercase() }
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
                    InsuranceCardValue(
                        value = if (isEditing) draftMemberId else display.memberId,
                        onValueChange = onDraftMemberIdChange,
                        isEditing = isEditing,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = CardPrimaryText,
                            letterSpacing = 1.sp
                        ),
                        displayTransform = { it.uppercase() }
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
                    InsuranceCardValue(
                        value = if (isEditing) draftGroupNumber else display.groupNumber,
                        onValueChange = onDraftGroupNumberChange,
                        isEditing = isEditing,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CardPrimaryText
                        )
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
                    if (isEditing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            InsuranceCardValue(
                                value = draftPcpCopay,
                                onValueChange = onDraftPcpCopayChange,
                                isEditing = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CardPrimaryText
                                ),
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "/",
                                style = MaterialTheme.typography.bodyLarge,
                                color = CardPrimaryText,
                                fontWeight = FontWeight.Bold
                            )
                            InsuranceCardValue(
                                value = draftSpecialistCopay,
                                onValueChange = onDraftSpecialistCopayChange,
                                isEditing = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = CardPrimaryText
                                ),
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    } else {
                        Text(
                            text = EobStrings.tf(
                                language,
                                "cleanInsuranceCopayFormat",
                                display.pcpCopay,
                                display.specialistCopay
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = CardPrimaryText
                        )
                    }
                }
            }

            HorizontalDivider(color = CardDividerColor, thickness = 1.dp)

            Text(
                text = EobStrings.tf(
                    language,
                    "cleanInsuranceVerificationFooter",
                    display.footerLocation,
                    display.verificationCode
                ),
                style = MaterialTheme.typography.labelSmall,
                color = CardSecondaryText
            )

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text(
                            text = EobStrings.t(language, "cleanInsuranceCancelEdit"),
                            color = CardSecondaryText
                        )
                    }
                    TextButton(onClick = onSave) {
                        Text(
                            text = EobStrings.t(language, "cleanInsuranceSaveCard"),
                            color = EobInsuranceNameAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (canEdit) {
                Text(
                    text = EobStrings.t(language, "cleanInsuranceTapToEdit"),
                    style = MaterialTheme.typography.labelSmall,
                    color = CardSecondaryText.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun InsuranceCardValue(
    value: String,
    onValueChange: (String) -> Unit,
    isEditing: Boolean,
    textStyle: TextStyle,
    displayTransform: (String) -> String = { it },
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    if (isEditing) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(EobInsuranceNameAccent),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Text(
            text = displayTransform(value),
            style = textStyle,
            modifier = modifier
        )
    }
}

@Composable
private fun EobInsuranceCardMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(EobBrandBlue, EobInsuranceGradientStart)
                ),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "E",
            color = CardPrimaryText,
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
            color = CardPrimaryText.copy(alpha = 0.18f),
            radius = radius
        )
        drawCircle(
            color = CardPrimaryText,
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
            color = CardPrimaryText,
            style = Stroke(width = radius * 0.12f, cap = StrokeCap.Round)
        )
        listOf(
            Offset(center.x + radius * 0.82f, center.y - radius * 0.72f),
            Offset(center.x + radius * 0.95f, center.y - radius * 0.18f),
            Offset(center.x - radius * 0.88f, center.y - radius * 0.62f)
        ).forEach { sparkle ->
            drawCircle(color = CardPrimaryText.copy(alpha = 0.85f), radius = radius * 0.06f, center = sparkle)
        }
    }
}
