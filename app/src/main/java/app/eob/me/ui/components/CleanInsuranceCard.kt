package app.eob.me.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
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
private val InsuranceCardMinHeight = 176.dp
private val InsuranceCardContentPadding = 13.dp
private val InsuranceCardSectionSpacing = 9.dp

private enum class InsuranceCardBackMode {
    Hub,
    Medications,
    Notepad
}

@Composable
fun CleanInsuranceCard(
    language: AppLanguage,
    display: InsuranceCardDisplay,
    currentPrescriptions: String,
    medicationDosageSchedule: String,
    medicationAllergies: String,
    doctorQuickNotes: String,
    onCurrentPrescriptionsChange: (String) -> Unit,
    onMedicationDosageScheduleChange: (String) -> Unit,
    onMedicationAllergiesChange: (String) -> Unit,
    onDoctorQuickNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var flipped by remember { mutableStateOf(false) }
    var backMode by remember { mutableStateOf(InsuranceCardBackMode.Hub) }
    var localPrescriptions by remember { mutableStateOf(currentPrescriptions) }
    var localDosageSchedule by remember { mutableStateOf(medicationDosageSchedule) }
    var localAllergies by remember { mutableStateOf(medicationAllergies) }
    var localDoctorNotes by remember { mutableStateOf(doctorQuickNotes) }

    LaunchedEffect(currentPrescriptions, medicationDosageSchedule, medicationAllergies, doctorQuickNotes) {
        if (!flipped || backMode == InsuranceCardBackMode.Hub) {
            localPrescriptions = currentPrescriptions
            localDosageSchedule = medicationDosageSchedule
            localAllergies = medicationAllergies
            localDoctorNotes = doctorQuickNotes
        }
    }

    LaunchedEffect(flipped) {
        if (!flipped) {
            backMode = InsuranceCardBackMode.Hub
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "insurance_card_rotation"
    )
    val density = LocalDensity.current

    BackHandler(enabled = flipped) {
        when (backMode) {
            InsuranceCardBackMode.Medications, InsuranceCardBackMode.Notepad -> backMode = InsuranceCardBackMode.Hub
            InsuranceCardBackMode.Hub -> flipped = false
        }
    }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = InsuranceCardMinHeight)
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
                InsuranceCardBackFace(
                    language = language,
                    mode = backMode,
                    currentPrescriptions = localPrescriptions,
                    medicationDosageSchedule = localDosageSchedule,
                    medicationAllergies = localAllergies,
                    doctorQuickNotes = localDoctorNotes,
                    onModeChange = { backMode = it },
                    onCurrentPrescriptionsChange = { updated ->
                        localPrescriptions = updated
                        onCurrentPrescriptionsChange(updated)
                    },
                    onMedicationDosageScheduleChange = { updated ->
                        localDosageSchedule = updated
                        onMedicationDosageScheduleChange(updated)
                    },
                    onMedicationAllergiesChange = { updated ->
                        localAllergies = updated
                        onMedicationAllergiesChange(updated)
                    },
                    onDoctorQuickNotesChange = { updated ->
                        localDoctorNotes = updated
                        onDoctorQuickNotesChange(updated)
                    },
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
            .padding(InsuranceCardContentPadding),
        verticalArrangement = Arrangement.spacedBy(InsuranceCardSectionSpacing)
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
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = display.memberId.uppercase(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = CardPrimaryText,
                        letterSpacing = 1.sp,
                        fontSize = 24.sp,
                        lineHeight = 28.sp
                    )
                )
            }
            VerifiedInsuranceBadge(modifier = Modifier.size(44.dp))
        }

        HorizontalDivider(color = CardDividerColor, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = EobStrings.t(language, "cleanInsuranceGroupNumberLabel"),
                style = MaterialTheme.typography.labelSmall,
                color = CardSecondaryText,
                letterSpacing = 0.5.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${EobStrings.t(language, "cleanInsuranceCopayLabel")} " +
                    EobStrings.t(language, "cleanInsuranceCopaySectionDetail"),
                style = MaterialTheme.typography.labelSmall,
                color = CardSecondaryText,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = display.groupNumber,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = CardPrimaryText
                ),
                modifier = Modifier.weight(1f)
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
                color = CardPrimaryText,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
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
private fun InsuranceCardBackFace(
    language: AppLanguage,
    mode: InsuranceCardBackMode,
    currentPrescriptions: String,
    medicationDosageSchedule: String,
    medicationAllergies: String,
    doctorQuickNotes: String,
    onModeChange: (InsuranceCardBackMode) -> Unit,
    onCurrentPrescriptionsChange: (String) -> Unit,
    onMedicationDosageScheduleChange: (String) -> Unit,
    onMedicationAllergiesChange: (String) -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mode != InsuranceCardBackMode.Hub) {
                IconButton(onClick = { onModeChange(InsuranceCardBackMode.Hub) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = EobStrings.t(language, "insuranceCardBackToHub"),
                        tint = NotesPrimaryText
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            InsuranceCardFlipButton(
                contentDescription = EobStrings.t(language, "insuranceCardFlipNotes"),
                onClick = onFlip,
                tint = NotesPrimaryText
            )
        }

        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                when {
                    initialState == InsuranceCardBackMode.Hub && targetState == InsuranceCardBackMode.Medications ->
                        (fadeIn(tween(220)) + expandVertically(expandFrom = Alignment.Top))
                            .togetherWith(fadeOut(tween(180)))
                    initialState == InsuranceCardBackMode.Hub && targetState == InsuranceCardBackMode.Notepad ->
                        (fadeIn(tween(220)) + slideInHorizontally { it / 2 })
                            .togetherWith(fadeOut(tween(180)))
                    initialState != InsuranceCardBackMode.Hub && targetState == InsuranceCardBackMode.Hub ->
                        fadeIn(tween(220)).togetherWith(
                            fadeOut(tween(180)) + shrinkVertically(shrinkTowards = Alignment.Top)
                        )
                    else ->
                        fadeIn(tween(180)).togetherWith(fadeOut(tween(160)))
                }
            },
            label = "insurance_card_back_mode"
        ) { activeMode ->
            when (activeMode) {
                InsuranceCardBackMode.Hub -> InsuranceCardBackHub(
                    language = language,
                    onOpenMedications = { onModeChange(InsuranceCardBackMode.Medications) },
                    onOpenNotepad = { onModeChange(InsuranceCardBackMode.Notepad) }
                )
                InsuranceCardBackMode.Medications -> InsuranceCardMedicationsPanel(
                    language = language,
                    currentPrescriptions = currentPrescriptions,
                    medicationDosageSchedule = medicationDosageSchedule,
                    medicationAllergies = medicationAllergies,
                    onCurrentPrescriptionsChange = onCurrentPrescriptionsChange,
                    onMedicationDosageScheduleChange = onMedicationDosageScheduleChange,
                    onMedicationAllergiesChange = onMedicationAllergiesChange
                )
                InsuranceCardBackMode.Notepad -> InsuranceCardDigitalNotepadPanel(
                    language = language,
                    doctorQuickNotes = doctorQuickNotes,
                    onDoctorQuickNotesChange = onDoctorQuickNotesChange
                )
            }
        }
    }
}

@Composable
private fun InsuranceCardBackHub(
    language: AppLanguage,
    onOpenMedications: () -> Unit,
    onOpenNotepad: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = EobStrings.t(language, "insuranceCardBackHubTitle"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = NotesPrimaryText,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsuranceCardBackLauncher(
                label = EobStrings.t(language, "insuranceCardMedicationsLauncher"),
                contentDescription = EobStrings.t(language, "insuranceCardMedicationsLauncherDescription"),
                onClick = onOpenMedications
            ) {
                InsuranceCardPillBottleIcon()
            }
            InsuranceCardBackLauncher(
                label = EobStrings.t(language, "insuranceCardNotepadLauncher"),
                contentDescription = EobStrings.t(language, "insuranceCardNotepadLauncherDescription"),
                onClick = onOpenNotepad
            ) {
                InsuranceCardNotepadIcon()
            }
        }
    }
}

@Composable
private fun InsuranceCardBackLauncher(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = NotesSecondaryText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InsuranceCardMedicationsPanel(
    language: AppLanguage,
    currentPrescriptions: String,
    medicationDosageSchedule: String,
    medicationAllergies: String,
    onCurrentPrescriptionsChange: (String) -> Unit,
    onMedicationDosageScheduleChange: (String) -> Unit,
    onMedicationAllergiesChange: (String) -> Unit
) {
    val keyboardOptions = notesKeyboardOptions()
    val textStyle = notesTextStyle()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsuranceCardPillBottleIcon(modifier = Modifier.size(40.dp))
            Text(
                text = EobStrings.t(language, "insuranceCardMedicationsPanelTitle"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NotesPrimaryText
            )
        }
        InsuranceCardNotesField(
            label = EobStrings.t(language, "insuranceCardActiveMedicationsLabel"),
            value = currentPrescriptions,
            onValueChange = onCurrentPrescriptionsChange,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            minHeight = 88.dp
        )
        InsuranceCardNotesField(
            label = EobStrings.t(language, "insuranceCardDosageScheduleLabel"),
            value = medicationDosageSchedule,
            onValueChange = onMedicationDosageScheduleChange,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            minHeight = 72.dp
        )
        InsuranceCardNotesField(
            label = EobStrings.t(language, "insuranceCardAllergiesLabel"),
            value = medicationAllergies,
            onValueChange = onMedicationAllergiesChange,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            minHeight = 72.dp
        )
    }
}

@Composable
private fun InsuranceCardDigitalNotepadPanel(
    language: AppLanguage,
    doctorQuickNotes: String,
    onDoctorQuickNotesChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsuranceCardNotepadIcon(modifier = Modifier.size(40.dp))
            Text(
                text = EobStrings.t(language, "insuranceCardDigitalNotepadTitle"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NotesPrimaryText
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .background(Color(0xFFFFF8E1), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = doctorQuickNotes,
                onValueChange = onDoctorQuickNotesChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF263238)),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color(0xFFB0BEC5),
                    unfocusedBorderColor = Color(0xFFCFD8DC),
                    cursorColor = EobBrandBlue
                ),
                keyboardOptions = notesKeyboardOptions(),
                singleLine = false,
                maxLines = 8
            )
        }
    }
}

@Composable
private fun InsuranceCardNotesField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    keyboardOptions: KeyboardOptions,
    minHeight: androidx.compose.ui.unit.Dp
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = NotesPrimaryText
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            textStyle = textStyle,
            shape = RoundedCornerShape(12.dp),
            colors = notesTextFieldColors(),
            keyboardOptions = keyboardOptions,
            singleLine = false,
            maxLines = 6
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
private fun notesTextStyle() = MaterialTheme.typography.bodyMedium.copy(
    color = NotesPrimaryText,
    letterSpacing = 0.sp
)

@Composable
private fun notesKeyboardOptions() = KeyboardOptions(
    capitalization = KeyboardCapitalization.Sentences,
    keyboardType = KeyboardType.Text,
    autoCorrectEnabled = true,
    imeAction = ImeAction.Default
)

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
