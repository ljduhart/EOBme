package app.eob.me.ui.components.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamCardDisplayState
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.EobStrings
import app.eob.me.data.NetworkAssuranceState
import app.eob.me.data.PreferredDoctor
import app.eob.me.data.TherapistNetworkStatus
import app.eob.me.ui.components.AssuranceCrimson

private val GoldCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFF8E7),
        Color(0xFFF5E6B8),
        Color(0xFFD4AF37),
        Color(0xFFF5E6B8),
        Color(0xFFFFF8E7)
    )
)

private val GoldInk = Color(0xFF5C4A1F)
private val GoldInkDark = Color(0xFF3D3220)

private const val CARD_HEIGHT_DP = 100

@Composable
fun HomeCareTeamCards(
    language: AppLanguage,
    careTeamCards: List<CareTeamCardDisplayState>,
    preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
    onSaveDoctor: (PreferredDoctor) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingType by remember { mutableStateOf<CareTeamProviderType?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        careTeamCards.forEach { cardState ->
            CareTeamSmartCard(
                language = language,
                cardState = cardState,
                doctor = preferredDoctors[cardState.type] ?: PreferredDoctor(type = cardState.type),
                onEdit = { editingType = cardState.type },
                modifier = Modifier.weight(1f)
            )
        }
    }

    editingType?.let { type ->
        val doctor = preferredDoctors[type] ?: PreferredDoctor(type = type)
        PreferredDoctorDialog(
            language = language,
            doctor = doctor,
            onDismiss = { editingType = null },
            onSave = { saved ->
                onSaveDoctor(saved)
                editingType = null
            }
        )
    }
}

@Composable
private fun CareTeamSmartCard(
    language: AppLanguage,
    cardState: CareTeamCardDisplayState,
    doctor: PreferredDoctor,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFlipped by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    LaunchedEffect(cardState.type, cardState.primaryLine) {
        isFlipped = false
    }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "careTeamPressScale"
    )
    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "careTeamFlipRotation"
    )

    Card(
        modifier = modifier
            .height(CARD_HEIGHT_DP.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
            .pointerInput(cardState.phoneDialUri) {
                detectTapGestures(
                    onTap = { isFlipped = !isFlipped },
                    onLongPress = { onEdit() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .graphicsLayer {
                        rotationY = flipRotation
                        cameraDistance = 8f * density.density
                    },
                contentAlignment = Alignment.Center
            ) {
                if (flipRotation <= 90f) {
                    CareTeamCardFront(
                        language = language,
                        cardState = cardState,
                        onCall = cardState.phoneDialUri?.let { uri ->
                            {
                                context.startActivity(
                                    Intent(Intent.ACTION_DIAL, Uri.parse(uri))
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                    ) {
                        CareTeamCardBack(language = language, doctor = doctor)
                    }
                }
            }
            if (!cardState.isAssigned) {
                GoldCardShimmerOverlay(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

@Composable
private fun CareTeamCardFront(
    language: AppLanguage,
    cardState: CareTeamCardDisplayState,
    onCall: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldCardGradient, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = careTeamLabel(language, cardState.type),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GoldInk,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 10.sp
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = cardState.primaryLine,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldInkDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 9.sp
                )
                Text(
                    text = cardState.secondaryLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryLineColor(cardState),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 8.sp,
                    fontWeight = if (isCallToActionLine(cardState)) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.pointerInput(onCall) {
                        if (onCall != null) {
                            detectTapGestures(onTap = { onCall() })
                        }
                    }
                )
                cardState.tertiaryLine?.let { tertiary ->
                    Text(
                        text = tertiary,
                        style = MaterialTheme.typography.labelSmall,
                        color = tertiaryLineColor(cardState),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 7.sp
                    )
                }
            }
            Text(
                text = formatMicroMetrics(language, cardState),
                style = MaterialTheme.typography.labelSmall,
                color = GoldInk.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = 7.sp,
                lineHeight = 8.sp
            )
        }
    }
}

@Composable
private fun GoldCardShimmerOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "goldShimmer")
    val progress by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = androidx.compose.animation.core.StartOffset(300)
        ),
        label = "goldShimmerProgress"
    )
    Canvas(modifier = modifier) {
        val streakWidth = size.width * 0.48f
        val x = size.width * progress - streakWidth / 2f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFFFBF0).copy(alpha = 0.12f),
                    Color(0xFFFFE082).copy(alpha = 0.55f),
                    Color(0xFFD4AF37).copy(alpha = 0.45f),
                    Color(0xFFFFE082).copy(alpha = 0.35f),
                    Color.Transparent
                ),
                start = Offset(x, 0f),
                end = Offset(x + streakWidth, size.height)
            ),
            size = size
        )
    }
}

@Composable
private fun CareTeamCardBack(
    language: AppLanguage,
    doctor: PreferredDoctor
) {
    val notSet = EobStrings.t(language, "valueNotSet")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoldCardGradient, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = careTeamLabel(language, doctor.type),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldInk,
                maxLines = 1,
                fontSize = 9.sp
            )
            CareTeamDetailLine(doctor.name.ifBlank { notSet }, fontSize = 8.sp, maxLines = 1)
            CareTeamDetailLine(doctor.specialty.ifBlank { notSet }, fontSize = 8.sp, maxLines = 1)
            CareTeamDetailLine(doctor.address.ifBlank { notSet }, fontSize = 7.sp, maxLines = 2)
            CareTeamDetailLine(doctor.phone.ifBlank { notSet }, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CareTeamDetailLine(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    maxLines: Int
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = GoldInkDark,
        textAlign = TextAlign.Center,
        fontSize = fontSize,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PreferredDoctorDialog(
    language: AppLanguage,
    doctor: PreferredDoctor,
    onDismiss: () -> Unit,
    onSave: (PreferredDoctor) -> Unit
) {
    var name by remember(doctor) { mutableStateOf(doctor.name) }
    var specialty by remember(doctor) { mutableStateOf(doctor.specialty) }
    var address by remember(doctor) { mutableStateOf(doctor.address) }
    var phone by remember(doctor) { mutableStateOf(doctor.phone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(EobStrings.tf(language, "careTeamEditTitle", careTeamLabel(language, doctor.type)))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(EobStrings.t(language, "careTeamDoctorName")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text(EobStrings.t(language, "careTeamSpecialty")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(EobStrings.t(language, "careTeamAddress")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(EobStrings.t(language, "careTeamPhone")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        doctor.copy(
                            name = name.trim(),
                            specialty = specialty.trim(),
                            address = address.trim(),
                            phone = phone.trim()
                        )
                    )
                }
            ) {
                Text(EobStrings.t(language, "careTeamSaveDoctor"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(EobStrings.t(language, "close"))
            }
        }
    )
}

private fun isCallToActionLine(card: CareTeamCardDisplayState): Boolean {
    return card.type == CareTeamProviderType.Pcp ||
        card.type == CareTeamProviderType.Dentist
}

private fun secondaryLineColor(card: CareTeamCardDisplayState): Color {
    return when {
        card.assuranceState == NetworkAssuranceState.OutOfNetworkAlert -> AssuranceCrimson
        card.type == CareTeamProviderType.Therapist &&
            card.therapistNetworkStatus == TherapistNetworkStatus.OutOfNetwork -> AssuranceCrimson
        card.type == CareTeamProviderType.Specialist && card.specialistReferralActive -> Color(0xFF6B5A2E)
        isCallToActionLine(card) && card.phoneDialUri != null -> Color(0xFF6B5A2E)
        else -> GoldInk
    }
}

private fun tertiaryLineColor(card: CareTeamCardDisplayState): Color {
    return when {
        card.type == CareTeamProviderType.Therapist &&
            card.therapistNetworkStatus == TherapistNetworkStatus.OutOfNetwork -> AssuranceCrimson
        card.type == CareTeamProviderType.Therapist &&
            card.therapistNetworkStatus == TherapistNetworkStatus.InNetwork -> Color(0xFF6B5A2E)
        else -> GoldInk.copy(alpha = 0.85f)
    }
}

private fun formatMicroMetrics(language: AppLanguage, card: CareTeamCardDisplayState): String {
    val parts = mutableListOf<String>()
    if (card.metrics.relatedEobCount > 0) {
        parts += EobStrings.tf(language, "careTeamMicroEobs", card.metrics.relatedEobCount)
    }
    if (card.metrics.upcomingAppointments > 0) {
        parts += EobStrings.tf(language, "careTeamMicroAppts", card.metrics.upcomingAppointments)
    }
    if (card.metrics.flaggedIssueCount > 0) {
        parts += EobStrings.tf(language, "careTeamMicroFlags", card.metrics.flaggedIssueCount)
    }
    return parts.joinToString(" · ").ifBlank {
        if (card.isAssigned) {
            EobStrings.t(language, "careTeamTapToFlip")
        } else {
            EobStrings.t(language, "careTeamLongPressEdit")
        }
    }
}
