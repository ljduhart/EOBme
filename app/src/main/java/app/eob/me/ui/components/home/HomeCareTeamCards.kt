package app.eob.me.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CareTeamProviderType
import app.eob.me.data.EobStrings
import app.eob.me.data.PreferredDoctor

private val GoldCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFF8E7),
        Color(0xFFF5E6B8),
        Color(0xFFD4AF37),
        Color(0xFFF5E6B8),
        Color(0xFFFFF8E7)
    )
)

private val GoldBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFB8860B),
        Color(0xFFFFD700),
        Color(0xFFB8860B)
    )
)

@Composable
fun HomeCareTeamCards(
    language: AppLanguage,
    preferredDoctors: Map<CareTeamProviderType, PreferredDoctor>,
    onSaveDoctor: (PreferredDoctor) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingType by remember { mutableStateOf<CareTeamProviderType?>(null) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CareTeamProviderType.displayOrder.forEach { type ->
            val doctor = preferredDoctors[type] ?: PreferredDoctor(type = type)
            CareTeamSmartCard(
                language = language,
                doctor = doctor,
                onClick = { editingType = type },
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
    doctor: PreferredDoctor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(width = 1.5.dp, brush = GoldBorderGradient, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GoldCardGradient, RoundedCornerShape(8.dp))
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = careTeamLabel(language, doctor.type),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5C4A1F),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                    Text(
                        text = doctor.name.ifBlank { EobStrings.t(language, "careTeamTapToEdit") },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF3D3220),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
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
            Text(
                EobStrings.tf(language, "careTeamEditTitle", careTeamLabel(language, doctor.type))
            )
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