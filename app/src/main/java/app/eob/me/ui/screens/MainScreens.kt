package app.eob.me.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.eob.me.ui.components.CalendarPicker
import app.eob.me.ui.components.ProviderAssuranceBadge
import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingIssue
import app.eob.me.data.BillingIssueSeverity
import app.eob.me.data.CptCategory
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobAccuracyReview
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import app.eob.me.data.NewsRelease
import app.eob.me.data.ProviderSummary
import app.eob.me.data.UserProfile
import app.eob.me.data.YearlyHealthCostSummary
import app.eob.me.data.asCurrency
import app.eob.me.data.EobStrings
import java.util.Calendar

@Composable
fun AnalysisScreen(
    language: AppLanguage,
    records: List<EobRecord>,
    selectedRecord: EobRecord?,
    uploadText: String,
    uploadNotice: String,
    onUploadTextChanged: (String) -> Unit,
    onLibraryUpload: () -> Unit,
    onCameraScan: () -> Unit,
    onDeleteEob: (EobRecord) -> Unit,
    onSelected: (EobRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(EobStrings.t(language, "history"), style = MaterialTheme.typography.titleLarge)
            Text(EobStrings.t(language, "analysisHelp"))
        }
        item { UploadCard(language, uploadText, onUploadTextChanged, onLibraryUpload, onCameraScan) }
        if (uploadNotice.isNotBlank()) {
            item { Text(uploadNotice, style = MaterialTheme.typography.titleSmall) }
        }
        items(records) { record ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(record) }
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.serviceDate, style = MaterialTheme.typography.titleMedium)
                    Text("${EobStrings.t(language, "insurance")}: ${record.insuranceName}")
                    Text("${EobStrings.t(language, "provider")}: ${record.providerName}")
                    Text("${EobStrings.t(language, "billed")}: ${record.totalBilledAmount.asCurrency()} • ${EobStrings.t(language, "paid")}: ${record.totalInsurancePaidAmount.asCurrency()}")
                    OutlinedButton(onClick = { onDeleteEob(record) }) {
                        Text(EobStrings.t(language, "deleteEob"))
                    }
                }
            }
        }
        item {
            selectedRecord?.let { AnalysisResultsCard(language, it) }
        }
        item {
            selectedRecord?.let { AccuracyReviewCard(EobAnalyzer.accuracyReview(it)) }
        }
        item {
            selectedRecord?.let { SmartBillingIssuesCard(EobAnalyzer.detectBillingIssues(it)) }
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
            Text("${EobStrings.t(language, "cptsBilledIn")} $year", style = MaterialTheme.typography.titleLarge)
            Text(EobStrings.t(language, "cptRule"))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CptCategory.entries.filter { it != CptCategory.Other }.forEach { category ->
                    AssistChip(
                        onClick = { onCategoryChanged(category) },
                        label = { Text(EobStrings.cptCategoryLabel(language, category)) },
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
                    Text("${EobStrings.t(language, "category")}: ${EobStrings.cptCategoryLabel(language, item.info.category)}")
                }
            }
        }
        item {
            if (usage.isEmpty()) Text("${EobStrings.t(language, "noCptsFound")} ${EobStrings.cptCategoryLabel(language, selectedCategory)} $year")
        }
    }
}

@Composable
fun NewsScreen(language: AppLanguage, newsItems: List<NewsRelease>, onDeleteNews: (NewsRelease) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text(EobStrings.t(language, "insuranceNews"), style = MaterialTheme.typography.titleLarge) }
        items(newsItems) { news ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${news.company} • ${news.date}", style = MaterialTheme.typography.labelLarge)
                    Text(news.headline, style = MaterialTheme.typography.titleSmall)
                    Text(news.summary)
                    OutlinedButton(onClick = { onDeleteNews(news) }) {
                        Text(EobStrings.t(language, "deleteNews"))
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(language: AppLanguage, records: List<EobRecord>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { YearlyHealthCostDashboard(EobAnalyzer.yearlyHealthCostSummary(records)) }
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
        Text(EobStrings.t(language, "appealLetter"), style = MaterialTheme.typography.titleLarge)
        Text(EobStrings.t(language, "appealHelp"))
        Text("${EobStrings.t(language, "currentMember")}: ${profile.fullName.ifBlank { EobStrings.t(language, "profileIncomplete") }}")
        Text("${EobStrings.t(language, "selectedEob")}: ${selectedRecord?.providerName ?: EobStrings.t(language, "noEobSelected")}")
        Button(onClick = onRegenerate) { Text(EobStrings.t(language, "autoFillAppeal")) }
        OutlinedTextField(
            value = letter,
            onValueChange = onLetterChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 14,
            label = { Text(EobStrings.t(language, "editAppealLetter")) }
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
        Text(EobStrings.t(language, "userProfile"), style = MaterialTheme.typography.titleLarge)
        Text(EobStrings.t(language, "editSavedDetails"))
        ProfileFields(language = language, profile = profile, onProfileChanged = onProfileChanged)
        Text(EobStrings.t(language, "languageSettings"), style = MaterialTheme.typography.titleMedium)
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
            Text(EobStrings.t(language, "support"))
        }
        if (showSupport) SupportContent(language)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(EobStrings.t(language, "logout"))
        }
    }
}

@Composable
fun InsuranceCard(language: AppLanguage, profile: UserProfile) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(EobStrings.t(language, "insuranceCard"), style = MaterialTheme.typography.titleMedium)
            Text("${EobStrings.t(language, "insuranceNameField")}: ${profile.insuranceName.ifBlank { EobStrings.t(language, "addInsuranceInfo") }}")
            Text("${EobStrings.t(language, "insuranceId")}: ${profile.insuranceId.ifBlank { "-" }}")
            Text("${EobStrings.t(language, "groupName")}: ${profile.groupName.ifBlank { "-" }}")
            Text("${EobStrings.t(language, "member")}: ${profile.fullName.ifBlank { EobStrings.t(language, "profileIncomplete") }}")
        }
    }
}

@Composable
fun QuickActionsCard(
    language: AppLanguage,
    appointments: List<DoctorAppointment>,
    onAddAppointment: (String, String, String, String) -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit
) {
    var visibleMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf("") }
    var showAppointmentDialog by remember { mutableStateOf(false) }
    var appointmentProvider by remember { mutableStateOf("") }
    var appointmentTime by remember { mutableStateOf("") }
    var appointmentNotes by remember { mutableStateOf("") }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(EobStrings.t(language, "quickActions"), style = MaterialTheme.typography.titleMedium)
            Text(EobStrings.t(language, "appointmentCalendar"))
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
                Text(EobStrings.t(language, "noAppointments"))
            } else {
                appointments.forEach { appointment ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${appointment.date} • ${appointment.providerName}", style = MaterialTheme.typography.titleSmall)
                            if (appointment.time.isNotBlank()) Text(appointment.time)
                            if (appointment.notes.isNotBlank()) Text(appointment.notes)
                            OutlinedButton(onClick = { onRemoveAppointment(appointment) }) {
                                Text(EobStrings.t(language, "removeAppointment"))
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
            title = { Text(EobStrings.t(language, "addAppointment")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = appointmentProvider,
                        onValueChange = { appointmentProvider = it },
                        label = { Text(EobStrings.t(language, "appointmentProvider")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text(EobStrings.t(language, "appointmentDate")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appointmentTime,
                        onValueChange = { appointmentTime = it },
                        label = { Text(EobStrings.t(language, "time")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = appointmentNotes,
                        onValueChange = { appointmentNotes = it },
                        label = { Text(EobStrings.t(language, "appointmentNotes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddAppointment(selectedDate, appointmentProvider, appointmentTime, appointmentNotes)
                        appointmentProvider = ""
                        appointmentTime = ""
                        appointmentNotes = ""
                        showAppointmentDialog = false
                    },
                    enabled = selectedDate.isNotBlank() && appointmentProvider.isNotBlank()
                ) {
                    Text(EobStrings.t(language, "saveAppointment"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAppointmentDialog = false }) {
                    Text(EobStrings.t(language, "close"))
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
            Text(EobStrings.t(language, "uploadEob"), style = MaterialTheme.typography.titleMedium)
            Text(EobStrings.t(language, "uploadHelp"))
            OutlinedTextField(
                value = uploadText,
                onValueChange = onUploadTextChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text(EobStrings.t(language, "eobText")) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onLibraryUpload) { Text(EobStrings.t(language, "uploadFromLibrary")) }
                OutlinedButton(
                    onClick = {
                        /*
                         * Future Veryfi Android SDK branch:
                         * 1. Launch the camera/document capture SDK and receive the OCR payload or image stream.
                         * 2. If the SDK returns structured OCR text, forward it into EobAnalyzer.analyze(...)
                         *    before saving to Firebase.
                         * 3. If the SDK returns an image/file stream, save it as a local Uri and upload it
                         *    through the existing Firebase Storage path for Cloud Function processing.
                         */
                        onCameraScan()
                    }
                ) { Text(EobStrings.t(language, "scanWithCamera")) }
            }
        }
    }
}

@Composable
private fun AnalysisResultsCard(language: AppLanguage, record: EobRecord) {
    val topDownTotal = (record.totalBilledAmount - record.totalInsurancePaidAmount - record.totalContractualAdjustmentAmount).coerceAtLeast(0.0)
    val bottomUpTotal = record.totalCopayAmount + record.totalDeductibleAmount + record.totalCoinsuranceAmount
    val isBalanced = kotlin.math.abs(topDownTotal - bottomUpTotal) <= 0.05
    
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(EobStrings.t(language, "analysisResults"), style = MaterialTheme.typography.titleLarge)
            Text("${EobStrings.t(language, "insurance")}: ${record.insuranceName}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("${EobStrings.t(language, "provider")}: ${record.providerName}")
            Text("${EobStrings.t(language, "dateOfService")}: ${record.serviceDate}")
            
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            
            AmountRow(EobStrings.t(language, "eobBilledAmount"), record.totalBilledAmount)
            AmountRow("- ${EobStrings.t(language, "insurancePaid")}", record.totalInsurancePaidAmount)
            AmountRow("- ${EobStrings.t(language, "contractualAdjustment")}", record.totalContractualAdjustmentAmount)
            
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            
            Text("Patient Responsibility Breakdown", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            if (record.totalCopayAmount > 0) AmountRow("• Copay", record.totalCopayAmount)
            if (record.totalDeductibleAmount > 0) AmountRow("• Deductible", record.totalDeductibleAmount)
            if (record.totalCoinsuranceAmount > 0) AmountRow("• Coinsurance", record.totalCoinsuranceAmount)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(
                        if (isBalanced) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(EobStrings.t(language, "patientResponsibility"), fontWeight = FontWeight.Bold)
                Text(record.totalPatientResponsibility.asCurrency(), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            }
            
            if (!isBalanced) {
                Text(
                    "⚠️ Billing Math Discrepancy: Extracted totals do not balance. Top-down calculation suggests ${topDownTotal.asCurrency()}.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (record.duplicateChargeWarnings.isNotEmpty()) {
                record.duplicateChargeWarnings.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun AccuracyReviewCard(review: EobAccuracyReview) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Accuracy Review", style = MaterialTheme.typography.titleLarge)
            Text("Overall confidence: ${review.overallConfidencePercent}%")
            review.fields.forEach { field ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(field.fieldName)
                    Text("${field.confidencePercent}%")
                }
                if (field.needsReview) Text("Review: ${field.value}", color = MaterialTheme.colorScheme.error)
            }
            HorizontalDivider()
            Text("Math validation", style = MaterialTheme.typography.titleMedium)
            AmountRow("Expected patient responsibility", review.mathValidation.expectedPatientResponsibility)
            AmountRow("Extracted patient responsibility", review.mathValidation.extractedPatientResponsibility)
            AmountRow("Difference", review.mathValidation.difference)
            Text(
                if (review.mathValidation.isBalanced) "Billing math balances." else "Billing math needs review.",
                color = if (review.mathValidation.isBalanced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            if (review.warnings.isNotEmpty()) {
                Text("Warnings", style = MaterialTheme.typography.titleMedium)
                review.warnings.forEach { Text("• $it") }
            }
        }
    }
}

@Composable
private fun SmartBillingIssuesCard(issues: List<BillingIssue>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Smart Billing Issue Flags", style = MaterialTheme.typography.titleLarge)
            if (issues.isEmpty()) {
                Text("No major billing issues detected.")
            } else {
                issues.forEach { issue ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(issue.title, style = MaterialTheme.typography.titleMedium)
                            Text("Severity: ${issue.severity.label()}")
                            Text(issue.explanation)
                            Text("Action: ${issue.recommendedAction}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearlyHealthCostDashboard(summary: YearlyHealthCostSummary) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Yearly Health Cost Dashboard", style = MaterialTheme.typography.titleLarge)
            Text("Year: ${if (summary.year == 0) "No EOBs yet" else summary.year} • EOBs: ${summary.eobCount}")
            AmountRow("Total billed", summary.totalBilled)
            AmountRow("Insurance paid", summary.totalInsurancePaid)
            AmountRow("Contractual adjustments", summary.totalContractualAdjustment)
            AmountRow("Patient responsibility", summary.totalPatientResponsibility)
            AmountRow("Copays", summary.totalCopay)
            AmountRow("Deductibles", summary.totalDeductible)
            AmountRow("Coinsurance", summary.totalCoinsurance)
        }
    }
}

@Composable
fun ProviderDirectoryCard(
    language: AppLanguage,
    providers: List<ProviderSummary>
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(EobStrings.t(language, "providerDirectoryTitle"), style = MaterialTheme.typography.titleLarge)
            Text(
                EobStrings.t(language, "providerDirectorySubtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (providers.isEmpty()) {
                Text(EobStrings.t(language, "providerDirectoryEmpty"))
            } else {
                providers.take(5).forEach { provider ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    provider.providerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                ProviderAssuranceBadge(
                                    language = language,
                                    assurance = provider.networkAssurance
                                )
                            }
                            Text(
                                EobStrings.tf(
                                    language,
                                    "providerEobSummary",
                                    provider.eobCount,
                                    provider.lastServiceDate
                                )
                            )
                            Text(
                                EobStrings.tf(
                                    language,
                                    "providerBilledPaid",
                                    provider.totalBilled.asCurrency(),
                                    provider.totalInsurancePaid.asCurrency()
                                )
                            )
                            Text(
                                "${EobStrings.t(language, "patientResponsibility")}: ${provider.totalPatientResponsibility.asCurrency()}"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun BillingIssueSeverity.label(): String {
    return when (this) {
        BillingIssueSeverity.Info -> "Info"
        BillingIssueSeverity.Warning -> "Review"
        BillingIssueSeverity.Critical -> "Important"
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
        Text(EobStrings.t(language, "support"), style = MaterialTheme.typography.titleLarge)
        Text(EobStrings.t(language, "howToUse"), style = MaterialTheme.typography.titleMedium)
        Text(EobStrings.t(language, "supportStep1"))
        Text(EobStrings.t(language, "supportStep2"))
        Text(EobStrings.t(language, "supportStep3"))
        Text(EobStrings.t(language, "supportStep4"))
        Text(EobStrings.t(language, "supportStep5"))
        Text(EobStrings.t(language, "features"), style = MaterialTheme.typography.titleMedium)
        Text(EobStrings.t(language, "featuresText"))
    }
}

