package app.eob.me.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobStrings
import app.eob.me.ui.theme.EobCareDentistBlue
import app.eob.me.ui.theme.EobCarePcpGreen
import app.eob.me.ui.theme.EobCareSpecialistYellow
import app.eob.me.ui.theme.EobCareTherapistRed

object CareTeamColors {
    val pcpGreen = EobCarePcpGreen
    val dentistBlue = EobCareDentistBlue
    val specialistYellow = EobCareSpecialistYellow
    val therapistRed = EobCareTherapistRed

    fun colorFor(type: CareTeamProviderType): Color = when (type) {
        CareTeamProviderType.Pcp -> pcpGreen
        CareTeamProviderType.Dentist -> dentistBlue
        CareTeamProviderType.Specialist -> specialistYellow
        CareTeamProviderType.Therapist -> therapistRed
    }
}

fun careTeamLabel(language: AppLanguage, type: CareTeamProviderType): String {
    val key = when (type) {
        CareTeamProviderType.Pcp -> "careTeamPcp"
        CareTeamProviderType.Dentist -> "careTeamDentist"
        CareTeamProviderType.Specialist -> "careTeamSpecialist"
        CareTeamProviderType.Therapist -> "careTeamTherapist"
    }
    return EobStrings.t(language, key)
}

fun appointmentTypesOnDate(
    appointments: List<DoctorAppointment>,
    date: String
): List<CareTeamProviderType> {
    return appointments
        .filter { it.date == date }
        .map { it.providerType }
        .distinctBy { it.ordinal }
        .sortedBy { it.ordinal }
}

@Composable
fun AppointmentDayColorBackground(
    types: List<CareTeamProviderType>,
    modifier: Modifier = Modifier
) {
    val distinct = types.distinctBy { it.ordinal }.sortedBy { it.ordinal }
    when (distinct.size) {
        0 -> Unit
        1 -> Box(
            modifier = modifier.background(CareTeamColors.colorFor(distinct.first()).copy(alpha = 0.88f))
        )
        else -> Row(modifier = modifier) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(CareTeamColors.colorFor(distinct[0]).copy(alpha = 0.88f))
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(CareTeamColors.colorFor(distinct[1]).copy(alpha = 0.88f))
            )
        }
    }
}

@Composable
fun CalendarDayCellContent(
    dayNumber: Int,
    hasAppointmentMarker: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (hasAppointmentMarker) "$dayNumber •" else dayNumber.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProviderTypeChipBar(
    language: AppLanguage,
    selected: CareTeamProviderType,
    onSelected: (CareTeamProviderType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CareTeamProviderType.displayOrder.chunked(2).forEach { rowTypes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTypes.forEach { type ->
                    ProviderTypeChipCell(
                        language = language,
                        type = type,
                        selected = selected == type,
                        onClick = { onSelected(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTypes.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProviderTypeChipCell(
    language: AppLanguage,
    type: CareTeamProviderType,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipColor = CareTeamColors.colorFor(type)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(chipColor, CircleShape)
                )
                Text(
                    text = careTeamLabel(language, type),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
