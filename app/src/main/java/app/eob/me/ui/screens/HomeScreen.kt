package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.InsuranceArticle
import app.eob.me.data.UserProfile
import app.eob.me.navigation.HubBentoDestination
import app.eob.me.ui.components.bento.BentoGridCell
import app.eob.me.ui.components.home.HomeAppointmentsSection
import app.eob.me.ui.components.home.HomeInsuranceNewsSection
import app.eob.me.ui.components.home.HomeWeekCalendar
import app.eob.me.ui.components.home.InsuranceArticleReaderOverlay
import app.eob.me.ui.components.home.currentNewsYear

/**
 * Scrollable main hub — state from [app.eob.me.viewmodel.EobViewModel].
 */
@Composable
fun HomeScreen(
    language: AppLanguage,
    profile: UserProfile,
    recordCount: Int,
    firebaseStatusLine: String,
    uploadNotice: String,
    appointments: List<DoctorAppointment>,
    calendarExpanded: Boolean,
    insuranceArticles: List<InsuranceArticle>,
    selectedInsuranceArticle: InsuranceArticle?,
    onCalendarExpandedChange: (Boolean) -> Unit,
    onAddAppointment: (String, String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    onInsuranceArticleSelected: (InsuranceArticle) -> Unit,
    onDismissInsuranceArticle: () -> Unit,
    onBentoSelected: (HubBentoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    var appointmentPrefillDate by remember { mutableStateOf("") }
    var openAppointmentDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        val readerOpen = selectedInsuranceArticle != null
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .then(if (readerOpen) Modifier.blur(10.dp) else Modifier),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Welcome, ${profile.firstName.ifBlank { "Member" }}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$recordCount ${if (recordCount == 1) "EOB" else "EOBs"} • $firebaseStatusLine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            if (uploadNotice.isNotBlank()) {
                item {
                    Text(
                        text = uploadNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                HomeWeekCalendar(
                    language = language,
                    appointments = appointments,
                    expanded = calendarExpanded,
                    onExpandedChange = onCalendarExpandedChange,
                    onDateSelected = { date ->
                        appointmentPrefillDate = date
                        openAppointmentDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HomeAppointmentsSection(
                    language = language,
                    appointments = appointments,
                    prefillDate = if (openAppointmentDialog) appointmentPrefillDate else "",
                    onPrefillHandled = { openAppointmentDialog = false },
                    onAddAppointment = { date, provider, time, notes ->
                        onAddAppointment(date, provider, time, notes)
                        appointmentPrefillDate = ""
                        openAppointmentDialog = false
                    },
                    onRemoveAppointment = onRemoveAppointment,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                HomeInsuranceNewsSection(
                    articles = insuranceArticles,
                    year = currentNewsYear(),
                    onArticleSelected = onInsuranceArticleSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            HubBentoDestination.gridRows.forEach { row ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        row.forEach { destination ->
                            BentoGridCell(
                                destination = destination,
                                onClick = { onBentoSelected(destination) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        selectedInsuranceArticle?.let { article ->
            InsuranceArticleReaderOverlay(
                article = article,
                onDismiss = onDismissInsuranceArticle,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
