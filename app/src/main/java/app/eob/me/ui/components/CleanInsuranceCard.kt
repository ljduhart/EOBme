package app.eob.me.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.RotateLeft
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.InsuranceCardDisplay
import app.eob.me.ui.theme.EobBrandBlue
import app.eob.me.ui.theme.EobCyberTextPrimary
import app.eob.me.ui.theme.EobInsuranceGradientEnd
import app.eob.me.ui.theme.EobInsuranceGradientMid
import app.eob.me.ui.theme.EobInsuranceGradientStart
import app.eob.me.ui.theme.EobInsuranceNameAccent
import app.eob.me.ui.theme.EobInsuranceSecondaryText
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

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
private val NotesCardBackground = Color(0xFF121A24)
private val NotesFieldBackground = Color(0xFF1B2430)
private val NotesPrimaryText = Color(0xFFF2F6FA)
private val NotesSecondaryText = Color(0xFFB8C4D0)

@Composable
fun CleanInsuranceCard(
    language: AppLanguage,
    display: InsuranceCardDisplay,
    currentPrescriptions: String,
    doctorQuickNotes: String,
    onCurrentPrescriptionsChange: (String) -> Unit,
    onDoctorQuickNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "insurance_card_rotation"
    )
    val density = LocalDensity.current

    BackHandler(enabled = flipped) {
        flipped = false
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 220.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            },
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (rotation <= 90f) {
                InsuranceCardFrontFace(
                    language = language,
                    display = display,
                    onFlip = { flipped = true }
                )
            } else {
                InsuranceCardNotesBackFace(
                    language = language,
                    currentPrescriptions = currentPrescriptions,
                    doctorQuickNotes = doctorQuickNotes,
                    onCurrentPrescriptionsChange = onCurrentPrescriptionsChange,
                    onDoctorQuickNotesChange = onDoctorQuickNotesChange,
                    onFlip = { flipped = false },
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}

@Composable
private fun InsuranceCardFrontFace(
    language: AppLanguage,
    display: InsuranceCardDisplay,
    onFlip: () -> Unit
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
                    text = display.insuranceName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EobInsuranceNameAccent,
                        letterSpacing = 0.5.sp
                    )
                )
            }
            InsuranceCardFlipButton(
                contentDescription = EobStrings.t(language, "insuranceCardFlipNotes"),
                onClick = onFlip
            )
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
                    text = display.memberId.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = CardPrimaryText,
                        letterSpacing = 1.sp
                    )
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
                    text = display.groupNumber,
                    style = MaterialTheme.typography.bodyLarge.copy(
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
    }
}

@Composable
private fun InsuranceCardNotesBackFace(
    language: AppLanguage,
    currentPrescriptions: String,
    doctorQuickNotes: String,
    onCurrentPrescriptionsChange: (String) -> Unit,
    onDoctorQuickNotesChange: (String) -> Unit,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NotesCardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            InsuranceCardFlipButton(
                contentDescription = EobStrings.t(language, "insuranceCardFlipNotes"),
                onClick = onFlip,
                tint = NotesPrimaryText
            )
        }

        Text(
            text = EobStrings.t(language, "insuranceCardPrescriptionsLabel"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = NotesPrimaryText
        )
        OutlinedTextField(
            value = currentPrescriptions,
            onValueChange = onCurrentPrescriptionsChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp, max = 132.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = NotesPrimaryText),
            shape = RoundedCornerShape(12.dp),
            colors = notesTextFieldColors(),
            maxLines = 5
        )

        Text(
            text = EobStrings.t(language, "insuranceCardDoctorNotesLabel"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = NotesPrimaryText
        )
        OutlinedTextField(
            value = doctorQuickNotes,
            onValueChange = onDoctorQuickNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 88.dp, max = 132.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = NotesPrimaryText),
            shape = RoundedCornerShape(12.dp),
            colors = notesTextFieldColors(),
            maxLines = 5
        )
    }
}

@Composable
private fun InsuranceCardFlipButton(
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = CardPrimaryText
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.RotateLeft,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
private fun notesTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = NotesFieldBackground,
    unfocusedContainerColor = NotesFieldBackground,
    disabledContainerColor = NotesFieldBackground,
    focusedTextColor = NotesPrimaryText,
    unfocusedTextColor = NotesPrimaryText,
    focusedBorderColor = EobBrandBlue.copy(alpha = 0.85f),
    unfocusedBorderColor = NotesSecondaryText.copy(alpha = 0.35f),
    cursorColor = EobBrandBlue,
    focusedPlaceholderColor = NotesSecondaryText,
    unfocusedPlaceholderColor = NotesSecondaryText
)

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
