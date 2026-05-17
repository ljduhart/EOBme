package app.eob.me.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.components.CalendarPicker
import app.eob.me.data.AppLanguage
import app.eob.me.data.CptCategory
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.data.asCurrency
import app.eob.me.localization.Translations
import java.util.Calendar

@Composable
fun HomeScreen(
    language: AppLanguage,
    profile: UserProfile,
    records: List<EobRecord>,
    appointments: List<DoctorAppointment>,
    uploadNotice: String,
    uploadText: String,
    onUploadTextChanged: (String) -> Unit,
    onAddAppointment: (String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    onLibraryUpload: () -> Unit,
    onCameraScan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InsuranceCard(language, profile) }
        item {
            QuickActionsCard(
                language = language,
                appointments = appointments,
                onAddAppointment = onAddAppointment,
                onRemoveAppointment = onRemoveAppointment
            )
        }
        item { UploadCard(language, uploadText, onUploadTextChanged, onLibraryUpload, onCameraScan) }
        if (uploadNotice.isNotBlank()) {
            item { Text(uploadNotice, style = MaterialTheme.typography.titleSmall) }
        }
        item {
            Text(
                "${Translations.t(language, "analysis")}: ${records.size} ${Translations.t(language, "eobs")}",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun AnalysisScreen(
    language: AppLanguage,
    records: List<EobRecord>,
    selectedRecord: EobRecord?,
    onSelected: (EobRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(Translations.t(language, "analysis"), style = MaterialTheme.typography.titleLarge)
            Text(Translations.t(language, "analysisHelp"))
        }
        items(records) { record ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(record) }
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.serviceDate, style = MaterialTheme.typography.titleMedium)
                    Text("${Translations.t(language, "insurance")}: ${record.insuranceName}")
                    Text("${Translations.t(language, "provider")}: ${record.providerName}")
                    Text("${Translations.t(language, "billed")}: ${record.totalBilledAmount.asCurrency()} • ${Translations.t(language, "paid")}: ${record.totalInsurancePaidAmount.asCurrency()}")
                }
            }
        }
        item {
            selectedRecord?.let { AnalysisResultsCard(language, it) }
        }
    }
}

@Composable
fun CptCountScreen(
    language: AppLanguage,
    records: List<EobRecord>,
    selectedCategory: CptCategory,
    onCategoryChanged: (CptCategory) -> Unit
) {
    val year = records.map { EobAnalyzer.serviceYear(it.serviceDate) }.filter { it > 0 }.maxOrNull()
        ?: Calendar.getInstance().get(Calendar.YEAR)
    val usage = EobAnalyzer.cptUsage(records, year).filter { it.info.category == selectedCategory }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("${Translations.t(language, "cptsBilledIn")} $year", style = MaterialTheme.typography.titleLarge)
            Text(Translations.t(language, "cptRule"))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CptCategory.entries.filter { it != CptCategory.Other }.forEach { category ->
                    AssistChip(
                        onClick = { onCategoryChanged(category) },
                        label = { Text(cptCategoryLabel(language, category)) },
                        enabled = category != selectedCategory
                    )
                }
            }
        }
        items(usage) { item ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${item.info.code} (${item.count}x)", style = MaterialTheme.typography.titleMedium)
                    Text(item.info.description)
                    Text("${Translations.t(language, "category")}: ${cptCategoryLabel(language, item.info.category)}")
                }
            }
        }
        item {
            if (usage.isEmpty()) Text("${Translations.t(language, "noCptsFound")} ${cptCategoryLabel(language, selectedCategory)} $year")
        }
    }
}

@Composable
fun NewsScreen(language: AppLanguage, newsItems: List<NewsRelease>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text(Translations.t(language, "insuranceNews"), style = MaterialTheme.typography.titleLarge) }
        items(newsItems) { news ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${news.company} • ${news.date}", style = MaterialTheme.typography.labelLarge)
                    Text(news.headline, style = MaterialTheme.typography.titleSmall)
                    Text(news.summary)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun AppealScreen(
    language: AppLanguage,
    profile: UserProfile,
    selectedRecord: EobRecord?,
    letter: String,
    onRegenerate: () -> Unit,
    onLetterChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(Translations.t(language, "appealLetter"), style = MaterialTheme.typography.titleLarge)
        Text(Translations.t(language, "appealHelp"))
        Text("${Translations.t(language, "currentMember")}: ${profile.fullName.ifBlank { Translations.t(language, "profileIncomplete") }}")
        Text("${Translations.t(language, "selectedEob")}: ${selectedRecord?.providerName ?: Translations.t(language, "noEobSelected")}")
        Button(onClick = onRegenerate) { Text(Translations.t(language, "autoFillAppeal")) }
        OutlinedTextField(
            value = letter,
            onValueChange = onLetterChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 14,
            label = { Text(Translations.t(language, "editAppealLetter")) }
        )
    }
}

@Composable
fun ProfileScreen(
    language: AppLanguage,
    profile: UserProfile,
    onProfileChanged: (UserProfile) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit
) {
    var showSupport by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(Translations.t(language, "userProfile"), style = MaterialTheme.typography.titleLarge)
        Text(Translations.t(language, "editSavedDetails"))
        ProfileFields(language = language, profile = profile, onProfileChanged = onProfileChanged)
        Text(Translations.t(language, "languageSettings"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { option ->
                AssistChip(
                    onClick = { onLanguageChanged(option) },
                    label = { Text(option.displayName) },
                    enabled = option != language
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showSupport = !showSupport }, modifier = Modifier.fillMaxWidth()) {
            Text(Translations.t(language, "support"))
        }
        if (showSupport) SupportContent(language)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(Translations.t(language, "logout"))
        }
    }
}

@Composable
private fun InsuranceCard(language: AppLanguage, profile: UserProfile) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(Translations.t(language, "insuranceCard"), style = MaterialTheme.typography.titleMedium)
            if (profile.insuranceCardSummary.isNotBlank()) {
                Text(profile.insuranceCardSummary)
            } else {
                Text("${Translations.t(language, "subscriberId")}: ${profile.subscriberId.ifBlank { Translations.t(language, "addSubscriberId") }}")
            }
            Text("${Translations.t(language, "member")}: ${profile.fullName.ifBlank { Translations.t(language, "profileIncomplete") }}")
        }
    }
}

@Composable
private fun QuickActionsCard(
    language: AppLanguage,
    appointments: List<DoctorAppointment>,
    onAddAppointment: (String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit
) {
    var visibleMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf("") }
    var showAppointmentDialog by remember { mutableStateOf(false) }
    var appointmentProvider by remember { mutableStateOf("") }
    var appointmentNotes by remember { mutableStateOf("") }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Translations.t(language, "quickActions"), style = MaterialTheme.typography.titleMedium)
            Text(Translations.t(language, "appointmentCalendar"))
            CalendarPicker(
                visibleMonth = visibleMonth,
                appointments = appointments,
                onPreviousMonth = { visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) } },
                onNextMonth = { visibleMonth = (visibleMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) } },
                onDateSelected = {
                    selectedDate = it
                    showAppointmentDialog = true
                }
            )
            if (appointments.isEmpty()) {
                Text(Translations.t(language, "noAppointments"))
            } else {
                appointments.forEach { appointment ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${appointment.date} • ${appointment.providerName}", style = MaterialTheme.typography.titleSmall)
                            if (appointment.notes.isNotBlank()) Text(appointment.notes)
                            OutlinedButton(onClick = { onRemoveAppointment(appointment) }) {
                                Text(Translations.t(language, "removeAppointment"))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showAppointmentDialog) {
        AlertDialog(
            onDismissRequest = { showAppointmentDialog = false },
            title = { Text(Translations.t(language, "addAppointment")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = appointmentProvider,
                        onValueChange = { appointmentProvider = it },
                        label = { Text(Translations.t(language, "appointmentProvider")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text(Translations.t(language, "appointmentDate")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appointmentNotes,
                        onValueChange = { appointmentNotes = it },
                        label = { Text(Translations.t(language, "appointmentNotes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddAppointment(selectedDate, appointmentProvider, appointmentNotes)
                        appointmentProvider = ""
                        appointmentNotes = ""
                        showAppointmentDialog = false
                    },
                    enabled = selectedDate.isNotBlank() && appointmentProvider.isNotBlank()
                ) {
                    Text(Translations.t(language, "saveAppointment"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppointmentDialog = false }) {
                    Text(Translations.t(language, "close"))
                }
            }
        )
    }
}

@Composable
private fun UploadCard(
    language: AppLanguage,
    uploadText: String,
    onUploadTextChanged: (String) -> Unit,
    onLibraryUpload: () -> Unit,
    onCameraScan: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Translations.t(language, "uploadEob"), style = MaterialTheme.typography.titleMedium)
            Text(Translations.t(language, "uploadHelp"))
            OutlinedTextField(
                value = uploadText,
                onValueChange = onUploadTextChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text(Translations.t(language, "eobText")) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLibraryUpload) { Text(Translations.t(language, "uploadFromLibrary")) }
                OutlinedButton(onClick = onCameraScan) { Text(Translations.t(language, "scanWithCamera")) }
            }
        }
    }
}

@Composable
private fun AnalysisResultsCard(language: AppLanguage, record: EobRecord) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Translations.t(language, "analysisResults"), style = MaterialTheme.typography.titleLarge)
            Text("${Translations.t(language, "insurance")}: ${record.insuranceName}")
            Text("${Translations.t(language, "provider")}: ${record.providerName}")
            Text("${Translations.t(language, "dateOfService")}: ${record.serviceDate}")
            AmountRow(Translations.t(language, "eobBilledAmount"), record.totalBilledAmount)
            AmountRow(Translations.t(language, "insurancePaid"), record.totalInsurancePaidAmount)
            AmountRow(Translations.t(language, "contractualAdjustment"), record.totalContractualAdjustmentAmount)
            AmountRow(Translations.t(language, "copay"), record.totalCopayAmount)
            AmountRow(Translations.t(language, "deductible"), record.totalDeductibleAmount)
            AmountRow(Translations.t(language, "coinsurance"), record.totalCoinsuranceAmount)
            if (record.duplicateChargeWarnings.isEmpty()) {
                Text(Translations.t(language, "noDuplicateCharges"))
            } else {
                record.duplicateChargeWarnings.forEach { Text(it) }
            }
            record.charges.forEach { charge ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${charge.cptCode}: ${charge.cptDescription}", style = MaterialTheme.typography.titleSmall)
                        Text("${cptCategoryLabel(language, charge.category)} • ${Translations.t(language, "billed")} ${charge.billedAmount.asCurrency()} • ${Translations.t(language, "paid")} ${charge.insurancePaidAmount.asCurrency()}")
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, amount: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(amount.asCurrency())
    }
}

@Composable
private fun SupportContent(language: AppLanguage) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(Translations.t(language, "support"), style = MaterialTheme.typography.titleLarge)
        Text(Translations.t(language, "howToUse"), style = MaterialTheme.typography.titleMedium)
        Text(Translations.t(language, "supportStep1"))
        Text(Translations.t(language, "supportStep2"))
        Text(Translations.t(language, "supportStep3"))
        Text(Translations.t(language, "supportStep4"))
        Text(Translations.t(language, "supportStep5"))
        Text(Translations.t(language, "features"), style = MaterialTheme.typography.titleMedium)
        Text(Translations.t(language, "featuresText"))
    }
}

private fun cptCategoryLabel(language: AppLanguage, category: CptCategory): String {
    return when (category) {
        CptCategory.OfficeVisit -> Translations.t(language, "categoryOfficeVisit")
        CptCategory.Lab -> Translations.t(language, "categoryLab")
        CptCategory.Hospital -> Translations.t(language, "categoryHospital")
        CptCategory.Dme -> Translations.t(language, "categoryDme")
        CptCategory.Injection -> Translations.t(language, "categoryInjection")
        CptCategory.Other -> Translations.t(language, "categoryOther")
    }
}
