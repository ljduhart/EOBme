package app.eob.me

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.CptCategory
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.FirebaseSyncStatus
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.data.asCurrency
import app.eob.me.ui.theme.EOBmeTheme
import kotlinx.coroutines.delay
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EOBmeTheme {
                EobMeApp()
            }
        }
    }
}

@Composable
fun EobMeApp() {
    val appContext = LocalContext.current.applicationContext
    val firebaseRepository = remember { FirebaseEobRepository(appContext) }
    var language by remember { mutableStateOf<AppLanguage?>(null) }
    var introStep by remember { mutableStateOf(0) }
    var profile by remember { mutableStateOf(UserProfile()) }
    var signedIn by remember { mutableStateOf(false) }
    var lastActivityAt by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(signedIn, lastActivityAt) {
        if (signedIn) {
            delay(180_000)
            if (System.currentTimeMillis() - lastActivityAt >= 180_000) {
                signedIn = false
                introStep = 0
            }
        }
    }

    val selectedLanguage = language ?: AppLanguage.English
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { lastActivityAt = System.currentTimeMillis() }
            }
    ) { innerPadding ->
        when {
            language == null -> LanguageScreen(
                modifier = Modifier.padding(innerPadding),
                onSelected = {
                    language = it
                    introStep = 0
                }
            )

            !signedIn && introStep < 3 -> IntroScreen(
                language = selectedLanguage,
                step = introStep,
                modifier = Modifier.padding(innerPadding),
                onNext = { introStep++ }
            )

            !signedIn -> RegistrationScreen(
                language = selectedLanguage,
                profile = profile,
                modifier = Modifier.padding(innerPadding),
                onProfileChanged = {
                    profile = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onContinue = {
                    if (profile.isComplete) {
                        signedIn = true
                        lastActivityAt = System.currentTimeMillis()
                    }
                }
            )

            else -> HomeScreen(
                language = selectedLanguage,
                profile = profile,
                modifier = Modifier.padding(innerPadding),
                firebaseRepository = firebaseRepository,
                onProfileChanged = {
                    profile = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onLanguageChanged = {
                    language = it
                    lastActivityAt = System.currentTimeMillis()
                },
                onLogout = {
                    signedIn = false
                    introStep = 0
                },
                onActivity = { lastActivityAt = System.currentTimeMillis() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EobMeAppPreview() {
    EOBmeTheme {
        EobMeApp()
    }
}

@Composable
private fun LanguageScreen(modifier: Modifier = Modifier, onSelected: (AppLanguage) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("EOBme", style = MaterialTheme.typography.displaySmall)
        Text("Select a language / Seleccione idioma / Choisissez la langue / 选择语言", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        AppLanguage.entries.forEach { option ->
            Button(
                onClick = { onSelected(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(option.displayName)
            }
        }
    }
}

@Composable
private fun IntroScreen(
    language: AppLanguage,
    step: Int,
    modifier: Modifier = Modifier,
    onNext: () -> Unit
) {
    val slides = localizedIntro(language)
    val slide = slides[step]
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(slide.first, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(slide.second, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(if (step == 2) t(language, "createAccount") else t(language, "next"))
        }
    }
}

@Composable
private fun RegistrationScreen(
    language: AppLanguage,
    profile: UserProfile,
    modifier: Modifier = Modifier,
    onProfileChanged: (UserProfile) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(t(language, "profileRequired"), style = MaterialTheme.typography.headlineSmall)
        Text(t(language, "profileRequiredHelp"))
        ProfileFields(language = language, profile = profile, onProfileChanged = onProfileChanged)
        Button(onClick = onContinue, enabled = profile.isComplete, modifier = Modifier.fillMaxWidth()) {
            Text(t(language, "continueHome"))
        }
    }
}

@Composable
private fun HomeScreen(
    language: AppLanguage,
    profile: UserProfile,
    modifier: Modifier = Modifier,
    firebaseRepository: FirebaseEobRepository,
    onProfileChanged: (UserProfile) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onActivity: () -> Unit
) {
    val records = remember {
        mutableStateListOf(
            EobAnalyzer.analyze(sampleEobText, "Sample camera scan", 1)
        )
    }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedRecord by remember { mutableStateOf<EobRecord?>(records.firstOrNull()) }
    var uploadText by remember { mutableStateOf("") }
    var uploadNotice by remember { mutableStateOf("") }
    val appointments = remember { mutableStateListOf<DoctorAppointment>() }
    var appointmentDate by remember { mutableStateOf("") }
    var appointmentProvider by remember { mutableStateOf("") }
    var appointmentNotes by remember { mutableStateOf("") }
    var selectedCptCategory by remember { mutableStateOf(CptCategory.OfficeVisit) }
    var appealLetter by remember { mutableStateOf(AppealLetterGenerator.generate(profile, selectedRecord)) }
    var firebaseStatus by remember { mutableStateOf(firebaseRepository.status()) }
    var firebaseNews by remember { mutableStateOf<List<NewsRelease>>(emptyList()) }
    val tabs = listOf(
        t(language, "home"),
        t(language, "history"),
        t(language, "cptCount"),
        t(language, "appeal"),
        t(language, "profile"),
        t(language, "support")
    )

    LaunchedEffect(profile.email, profile.password, profile.isComplete) {
        if (profile.isComplete) {
            firebaseRepository.signInOrCreate(profile) { status -> firebaseStatus = status }
        }
    }

    DisposableEffect(firebaseStatus.userId) {
        val userId = firebaseStatus.userId
        val profileListener = firebaseRepository.observeProfile(
            userId = userId,
            currentPassword = profile.password,
            onProfile = { remoteProfile ->
                onProfileChanged(remoteProfile)
                onActivity()
            },
            onError = { firebaseStatus = firebaseStatus.copy(message = it) }
        )
        val eobListener = firebaseRepository.observeEobs(
            userId = userId,
            onRecords = { remoteRecords ->
                records.clear()
                records.addAll(EobAnalyzer.compactDuplicateEobs(remoteRecords))
                selectedRecord = records.firstOrNull()
                appealLetter = AppealLetterGenerator.generate(profile, selectedRecord)
                onActivity()
            },
            onError = { firebaseStatus = firebaseStatus.copy(message = it) }
        )
        val newsListener = firebaseRepository.observeInsuranceNews(
            onNews = { news ->
                firebaseNews = news
                onActivity()
            },
            onError = { firebaseStatus = firebaseStatus.copy(message = it) }
        )
        onDispose {
            profileListener?.remove()
            eobListener?.remove()
            newsListener?.remove()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("EOBme", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(onClick = onLogout) { Text(t(language, "logout")) }
        }
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        onActivity()
                    },
                    text = { Text(title) }
                )
            }
        }
        when (selectedTab) {
            0 -> OverviewTab(
                language = language,
                profile = profile,
                records = records.sortedBy { it.serviceDateSortKey },
                selectedRecord = selectedRecord,
                firebaseStatus = firebaseStatus,
                firebaseNews = firebaseNews,
                appointments = appointments.sortedBy { it.date },
                appointmentDate = appointmentDate,
                appointmentProvider = appointmentProvider,
                appointmentNotes = appointmentNotes,
                uploadNotice = uploadNotice,
                uploadText = uploadText,
                onUploadTextChanged = {
                    uploadText = it
                    onActivity()
                },
                onAppointmentDateChanged = {
                    appointmentDate = it
                    onActivity()
                },
                onAppointmentProviderChanged = {
                    appointmentProvider = it
                    onActivity()
                },
                onAppointmentNotesChanged = {
                    appointmentNotes = it
                    onActivity()
                },
                onAddAppointment = {
                    if (appointmentDate.isNotBlank() && appointmentProvider.isNotBlank()) {
                        appointments.add(
                            DoctorAppointment(
                                id = (appointments.maxOfOrNull { it.id } ?: 0) + 1,
                                date = appointmentDate,
                                providerName = appointmentProvider,
                                notes = appointmentNotes
                            )
                        )
                        appointmentDate = ""
                        appointmentProvider = ""
                        appointmentNotes = ""
                    }
                    onActivity()
                },
                onRemoveAppointment = { appointment ->
                    appointments.removeAll { it.id == appointment.id }
                    onActivity()
                },
                onUpload = { source ->
                    val textToAnalyze = uploadText.ifBlank { sampleEobText }
                    val analyzedRecord = EobAnalyzer.analyze(textToAnalyze, source, (records.maxOfOrNull { it.id } ?: 0) + 1)
                    val duplicateIndex = records.indexOfFirst { existing -> EobAnalyzer.isSameEob(existing, analyzedRecord) }
                    val record = if (duplicateIndex >= 0) {
                        uploadNotice = t(language, "duplicateReplaced")
                        analyzedRecord.copy(id = records[duplicateIndex].id)
                    } else {
                        uploadNotice = t(language, "eobAdded")
                        analyzedRecord
                    }
                    if (duplicateIndex >= 0) {
                        records[duplicateIndex] = record
                    } else {
                        records.add(record)
                    }
                    val compactedRecords = EobAnalyzer.compactDuplicateEobs(records)
                    records.clear()
                    records.addAll(compactedRecords)
                    selectedRecord = record
                    appealLetter = AppealLetterGenerator.generate(profile, record)
                    uploadText = ""
                    if (firebaseStatus.userId.isNotBlank()) {
                        firebaseRepository.saveEob(firebaseStatus.userId, record) {
                            firebaseStatus = firebaseStatus.copy(message = it)
                        }
                    }
                    onActivity()
                }
            )

            1 -> HistoryTab(language = language, records = records.sortedBy { it.serviceDateSortKey }, onSelected = {
                selectedRecord = it
                selectedTab = 0
                onActivity()
            })

            2 -> CptCountTab(
                language = language,
                records = records,
                selectedCategory = selectedCptCategory,
                onCategoryChanged = {
                    selectedCptCategory = it
                    onActivity()
                }
            )

            3 -> AppealTab(
                language = language,
                profile = profile,
                selectedRecord = selectedRecord,
                letter = appealLetter,
                onRegenerate = {
                    appealLetter = AppealLetterGenerator.generate(profile, selectedRecord)
                    onActivity()
                },
                onLetterChanged = {
                    appealLetter = it
                    onActivity()
                }
            )

            4 -> ProfileTab(
                language = language,
                profile = profile,
                onProfileChanged = {
                    onProfileChanged(it)
                    if (firebaseStatus.userId.isNotBlank()) {
                        firebaseRepository.saveProfile(firebaseStatus.userId, it) { message ->
                            firebaseStatus = firebaseStatus.copy(message = message)
                        }
                    }
                    onActivity()
                },
                firebaseStatus = firebaseStatus,
                insuranceCardStoragePath = firebaseStatus.userId
                    .takeIf { it.isNotBlank() }
                    ?.let { firebaseRepository.insuranceCardStoragePath(it, "insurance-card.jpg") }
                    .orEmpty(),
                onLanguageChanged = onLanguageChanged
            )

            5 -> SupportTab(language = language)
        }
    }
}

@Composable
private fun OverviewTab(
    language: AppLanguage,
    profile: UserProfile,
    records: List<EobRecord>,
    selectedRecord: EobRecord?,
    firebaseStatus: FirebaseSyncStatus,
    firebaseNews: List<NewsRelease>,
    appointments: List<DoctorAppointment>,
    appointmentDate: String,
    appointmentProvider: String,
    appointmentNotes: String,
    uploadNotice: String,
    uploadText: String,
    onUploadTextChanged: (String) -> Unit,
    onAppointmentDateChanged: (String) -> Unit,
    onAppointmentProviderChanged: (String) -> Unit,
    onAppointmentNotesChanged: (String) -> Unit,
    onAddAppointment: () -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit,
    onUpload: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InsuranceCard(language, profile) }
        item { FirebaseSyncCard(language, firebaseStatus) }
        item {
            QuickActionsCard(
                language = language,
                appointments = appointments,
                appointmentDate = appointmentDate,
                appointmentProvider = appointmentProvider,
                appointmentNotes = appointmentNotes,
                onAppointmentDateChanged = onAppointmentDateChanged,
                onAppointmentProviderChanged = onAppointmentProviderChanged,
                onAppointmentNotesChanged = onAppointmentNotesChanged,
                onAddAppointment = onAddAppointment,
                onRemoveAppointment = onRemoveAppointment
            )
        }
        item { UploadCard(language, uploadText, onUploadTextChanged, onUpload) }
        if (uploadNotice.isNotBlank()) {
            item { Text(uploadNotice, style = MaterialTheme.typography.titleSmall) }
        }
        item { selectedRecord?.let { AnalysisResultsCard(language, it) } }
        item { NewsCard(language, firebaseNews.ifEmpty { EobKnowledgeBase.newsReleases }) }
        item {
            Text("${t(language, "history")}: ${records.size} ${t(language, "eobs")}", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InsuranceCard(language: AppLanguage, profile: UserProfile) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t(language, "insuranceCard"), style = MaterialTheme.typography.titleMedium)
            if (profile.insuranceCardSummary.isNotBlank()) {
                Text(profile.insuranceCardSummary)
            } else if (profile.insuranceCardDownloadUrl.isNotBlank()) {
                Text("${t(language, "firebaseCardFile")}: ${profile.insuranceCardDownloadUrl}")
            } else {
                Text("${t(language, "subscriberId")}: ${profile.subscriberId.ifBlank { t(language, "addSubscriberId") }}")
            }
            Text("${t(language, "member")}: ${profile.fullName.ifBlank { t(language, "profileIncomplete") }}")
        }
    }
}

@Composable
private fun FirebaseSyncCard(language: AppLanguage, firebaseStatus: FirebaseSyncStatus) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(t(language, "firebaseDataSync"), style = MaterialTheme.typography.titleMedium)
            Text(firebaseStatusText(language, firebaseStatus))
            if (firebaseStatus.userId.isNotBlank()) {
                Text("${t(language, "profile")}: users/${firebaseStatus.userId}")
                Text("${t(language, "eobHistory")}: users/${firebaseStatus.userId}/eobs")
                Text("${t(language, "insuranceCards")}: users/${firebaseStatus.userId}/insurance-cards")
                Text("${t(language, "news")}: insuranceNews")
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    language: AppLanguage,
    appointments: List<DoctorAppointment>,
    appointmentDate: String,
    appointmentProvider: String,
    appointmentNotes: String,
    onAppointmentDateChanged: (String) -> Unit,
    onAppointmentProviderChanged: (String) -> Unit,
    onAppointmentNotesChanged: (String) -> Unit,
    onAddAppointment: () -> Unit,
    onRemoveAppointment: (DoctorAppointment) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t(language, "quickActions"), style = MaterialTheme.typography.titleMedium)
            Text(t(language, "appointmentCalendar"))
            OutlinedTextField(
                value = appointmentDate,
                onValueChange = onAppointmentDateChanged,
                label = { Text(t(language, "appointmentDate")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = appointmentProvider,
                onValueChange = onAppointmentProviderChanged,
                label = { Text(t(language, "appointmentProvider")) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = appointmentNotes,
                onValueChange = onAppointmentNotesChanged,
                label = { Text(t(language, "appointmentNotes")) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onAddAppointment,
                enabled = appointmentDate.isNotBlank() && appointmentProvider.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(t(language, "addAppointment"))
            }
            if (appointments.isEmpty()) {
                Text(t(language, "noAppointments"))
            } else {
                appointments.forEach { appointment ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${appointment.date} • ${appointment.providerName}", style = MaterialTheme.typography.titleSmall)
                            if (appointment.notes.isNotBlank()) Text(appointment.notes)
                            OutlinedButton(onClick = { onRemoveAppointment(appointment) }) {
                                Text(t(language, "removeAppointment"))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadCard(
    language: AppLanguage,
    uploadText: String,
    onUploadTextChanged: (String) -> Unit,
    onUpload: (String) -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t(language, "uploadEob"), style = MaterialTheme.typography.titleMedium)
            Text(t(language, "uploadHelp"))
            OutlinedTextField(
                value = uploadText,
                onValueChange = onUploadTextChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text(t(language, "eobText")) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpload(t(language, "libraryUpload")) }) { Text(t(language, "uploadFromLibrary")) }
                OutlinedButton(onClick = { onUpload(t(language, "cameraScan")) }) { Text(t(language, "scanWithCamera")) }
            }
        }
    }
}

@Composable
private fun NewsCard(language: AppLanguage, newsItems: List<NewsRelease>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(t(language, "insuranceNews"), style = MaterialTheme.typography.titleMedium)
            newsItems.forEach { news ->
                Text("${news.company} • ${news.date}", style = MaterialTheme.typography.labelLarge)
                Text(news.headline, style = MaterialTheme.typography.titleSmall)
                Text(news.summary)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AnalysisResultsCard(language: AppLanguage, record: EobRecord) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(t(language, "analysisResults"), style = MaterialTheme.typography.titleLarge)
            Text("${t(language, "insurance")}: ${record.insuranceName}")
            Text("${t(language, "provider")}: ${record.providerName}")
            Text("${t(language, "dateOfService")}: ${record.serviceDate}")
            AmountRow(t(language, "eobBilledAmount"), record.totalBilledAmount)
            AmountRow(t(language, "insurancePaid"), record.totalInsurancePaidAmount)
            AmountRow(t(language, "contractualAdjustment"), record.totalContractualAdjustmentAmount)
            AmountRow(t(language, "copay"), record.totalCopayAmount)
            AmountRow(t(language, "deductible"), record.totalDeductibleAmount)
            AmountRow(t(language, "coinsurance"), record.totalCoinsuranceAmount)
            if (record.duplicateChargeWarnings.isEmpty()) {
                Text(t(language, "noDuplicateCharges"))
            } else {
                record.duplicateChargeWarnings.forEach { Text(it) }
            }
            record.charges.forEach { charge ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${charge.cptCode}: ${charge.cptDescription}", style = MaterialTheme.typography.titleSmall)
                        Text("${cptCategoryLabel(language, charge.category)} • ${t(language, "billed")} ${charge.billedAmount.asCurrency()} • ${t(language, "paid")} ${charge.insurancePaidAmount.asCurrency()}")
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
private fun HistoryTab(language: AppLanguage, records: List<EobRecord>, onSelected: (EobRecord) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(records) { record ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(record) }
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(record.serviceDate, style = MaterialTheme.typography.titleMedium)
                    Text("${t(language, "insurance")}: ${record.insuranceName}")
                    Text("${t(language, "provider")}: ${record.providerName}")
                    Text("${t(language, "billed")}: ${record.totalBilledAmount.asCurrency()} • ${t(language, "paid")}: ${record.totalInsurancePaidAmount.asCurrency()}")
                }
            }
        }
    }
}

@Composable
private fun CptCountTab(
    language: AppLanguage,
    records: List<EobRecord>,
    selectedCategory: CptCategory,
    onCategoryChanged: (CptCategory) -> Unit
) {
    val year = records
        .map { EobAnalyzer.serviceYear(it.serviceDate) }
        .filter { it > 0 }
        .maxOrNull()
        ?: Calendar.getInstance().get(Calendar.YEAR)
    val usage = EobAnalyzer.cptUsage(records, year).filter { it.info.category == selectedCategory }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("${t(language, "cptsBilledIn")} $year", style = MaterialTheme.typography.titleLarge)
            Text(t(language, "cptRule"))
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
                    Text("${t(language, "category")}: ${cptCategoryLabel(language, item.info.category)}")
                }
            }
        }
        item {
            if (usage.isEmpty()) Text("${t(language, "noCptsFound")} ${cptCategoryLabel(language, selectedCategory)} $year")
        }
    }
}

@Composable
private fun AppealTab(
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
        Text(t(language, "appealLetter"), style = MaterialTheme.typography.titleLarge)
        Text(t(language, "appealHelp"))
        Text("${t(language, "currentMember")}: ${profile.fullName.ifBlank { t(language, "profileIncomplete") }}")
        Text("${t(language, "selectedEob")}: ${selectedRecord?.providerName ?: t(language, "noEobSelected")}")
        Button(onClick = onRegenerate) { Text(t(language, "autoFillAppeal")) }
        OutlinedTextField(
            value = letter,
            onValueChange = onLetterChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 14,
            label = { Text(t(language, "editAppealLetter")) }
        )
    }
}

@Composable
private fun ProfileTab(
    language: AppLanguage,
    profile: UserProfile,
    onProfileChanged: (UserProfile) -> Unit,
    firebaseStatus: FirebaseSyncStatus,
    insuranceCardStoragePath: String,
    onLanguageChanged: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(t(language, "userProfile"), style = MaterialTheme.typography.titleLarge)
        Text(t(language, "editSavedDetails"))
        Text("${t(language, "firebase")}: ${firebaseStatusText(language, firebaseStatus)}")
        if (insuranceCardStoragePath.isNotBlank()) {
            Text("${t(language, "uploadInsuranceCardPath")}: $insuranceCardStoragePath")
        }
        ProfileFields(language = language, profile = profile, onProfileChanged = onProfileChanged)
        Text(t(language, "languageSettings"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { option ->
                AssistChip(
                    onClick = { onLanguageChanged(option) },
                    label = { Text(option.displayName) },
                    enabled = option != language
                )
            }
        }
    }
}

@Composable
private fun ProfileFields(language: AppLanguage, profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
    OutlinedTextField(
        value = profile.firstName,
        onValueChange = { onProfileChanged(profile.copy(firstName = it)) },
        label = { Text(t(language, "firstName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.lastName,
        onValueChange = { onProfileChanged(profile.copy(lastName = it)) },
        label = { Text(t(language, "lastName")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.email,
        onValueChange = { onProfileChanged(profile.copy(email = it)) },
        label = { Text(t(language, "email")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.password,
        onValueChange = { onProfileChanged(profile.copy(password = it)) },
        label = { Text(t(language, "password")) },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = profile.city,
            onValueChange = { onProfileChanged(profile.copy(city = it)) },
            label = { Text(t(language, "city")) },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = profile.state,
            onValueChange = { onProfileChanged(profile.copy(state = it)) },
            label = { Text(t(language, "state")) },
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = profile.subscriberId,
        onValueChange = { onProfileChanged(profile.copy(subscriberId = it)) },
        label = { Text(t(language, "subscriberId")) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.insuranceCardSummary,
        onValueChange = { onProfileChanged(profile.copy(insuranceCardSummary = it)) },
        label = { Text(t(language, "insuranceCardDetails")) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    OutlinedTextField(
        value = profile.insuranceCardDownloadUrl,
        onValueChange = { onProfileChanged(profile.copy(insuranceCardDownloadUrl = it)) },
        label = { Text(t(language, "firebaseInsuranceCardUrl")) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SupportTab(language: AppLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(t(language, "support"), style = MaterialTheme.typography.titleLarge)
        Text(t(language, "howToUse"), style = MaterialTheme.typography.titleMedium)
        Text(t(language, "supportStep1"))
        Text(t(language, "supportStep2"))
        Text(t(language, "supportStep3"))
        Text(t(language, "supportStep4"))
        Text(t(language, "supportStep5"))
        Text(t(language, "features"), style = MaterialTheme.typography.titleMedium)
        Text(t(language, "featuresText"))
    }
}

private fun localizedIntro(language: AppLanguage): List<Pair<String, String>> {
    return when (language) {
        AppLanguage.Spanish -> listOf(
            "Comprenda su EOB" to "EOBme resume beneficios, cargos y responsabilidad del paciente.",
            "Controle códigos y costos" to "Revise CPT, pagos, ajustes, copagos, deducibles y coseguro.",
            "Prepare apelaciones" to "Genere una carta editable con los detalles disponibles."
        )

        AppLanguage.French -> listOf(
            "Comprendre votre EOB" to "EOBme résume les prestations, frais et montants patient.",
            "Suivre les codes et coûts" to "Consultez CPT, paiements, ajustements, franchises et coassurance.",
            "Préparer les appels" to "Générez une lettre modifiable avec les informations disponibles."
        )

        AppLanguage.Chinese -> listOf(
            "了解您的 EOB" to "EOBme 汇总福利说明、费用和患者责任。",
            "跟踪代码和费用" to "查看 CPT、付款、调整、自付额、免赔额和共同保险。",
            "准备申诉" to "使用可用信息生成可编辑的申诉信。"
        )

        AppLanguage.English -> listOf(
            "Understand your EOB" to "EOBme summarizes benefits, charges, and patient responsibility.",
            "Track codes and costs" to "Review CPTs, payments, adjustments, copays, deductibles, and coinsurance.",
            "Prepare appeals" to "Generate an editable appeal letter with available EOB details."
        )
    }
}

private fun firebaseStatusText(language: AppLanguage, status: FirebaseSyncStatus): String {
    return when {
        !status.isConfigured -> t(language, "firebaseNotConfigured")
        status.userId.isNotBlank() -> t(language, "firebaseActive")
        else -> t(language, "firebaseConfigured")
    }
}

private fun cptCategoryLabel(language: AppLanguage, category: CptCategory): String {
    return when (category) {
        CptCategory.OfficeVisit -> t(language, "categoryOfficeVisit")
        CptCategory.Lab -> t(language, "categoryLab")
        CptCategory.Hospital -> t(language, "categoryHospital")
        CptCategory.Dme -> t(language, "categoryDme")
        CptCategory.Injection -> t(language, "categoryInjection")
        CptCategory.Other -> t(language, "categoryOther")
    }
}

private fun t(language: AppLanguage, key: String): String {
    val english = mapOf(
        "next" to "Next",
        "createAccount" to "Create account",
        "profileRequired" to "Create your profile",
        "profileRequiredHelp" to "First name, last name, email, and password are required after the introduction screens.",
        "continueHome" to "Continue to home",
        "home" to "Home",
        "history" to "History",
        "cptCount" to "CPT Count",
        "appeal" to "Appeal",
        "profile" to "Profile",
        "support" to "Support",
        "logout" to "Log out",
        "uploadEob" to "Upload EOB",
        "duplicateReplaced" to "Duplicate EOB found. The original copy was replaced with this upload.",
        "eobAdded" to "EOB added.",
        "eobs" to "EOBs",
        "insuranceCard" to "Insurance card",
        "firebaseCardFile" to "Firebase card file",
        "subscriberId" to "Subscriber ID",
        "addSubscriberId" to "Add subscriber ID in profile settings",
        "member" to "Member",
        "profileIncomplete" to "Profile incomplete",
        "firebaseDataSync" to "Firebase data sync",
        "eobHistory" to "EOB history",
        "insuranceCards" to "Insurance cards",
        "news" to "News",
        "quickActions" to "Quick Actions",
        "appointmentCalendar" to "Appointment calendar",
        "appointmentDate" to "Appointment date",
        "appointmentProvider" to "Provider or doctor",
        "appointmentNotes" to "Appointment notes",
        "addAppointment" to "Add appointment",
        "removeAppointment" to "Remove appointment",
        "noAppointments" to "No doctor appointments added yet.",
        "uploadHelp" to "Paste OCR text from an EOB or use the sample scan. If the upload matches an existing EOB, the original copy will be replaced.",
        "eobText" to "EOB text",
        "libraryUpload" to "Library upload",
        "uploadFromLibrary" to "Upload from library",
        "cameraScan" to "Camera scan",
        "scanWithCamera" to "Scan with camera",
        "insuranceNews" to "Insurance news",
        "analysisResults" to "Analysis Results",
        "insurance" to "Insurance",
        "provider" to "Provider",
        "dateOfService" to "Date of Service",
        "eobBilledAmount" to "EOB billed amount",
        "insurancePaid" to "Insurance paid",
        "contractualAdjustment" to "Contractual adjustment",
        "copay" to "Copay",
        "deductible" to "Deductible",
        "coinsurance" to "Coinsurance",
        "noDuplicateCharges" to "No duplicate charges detected.",
        "billed" to "Billed",
        "paid" to "Paid",
        "cptsBilledIn" to "CPTs billed in",
        "cptRule" to "Only five-character CPT/HCPCS codes starting with 1-9 or A-J are stored.",
        "category" to "Category",
        "noCptsFound" to "No CPTs found for",
        "appealLetter" to "Appeal letter",
        "appealHelp" to "Auto-filled when profile details are saved and provider information is retrievable from the selected EOB.",
        "currentMember" to "Current member",
        "selectedEob" to "Selected EOB",
        "noEobSelected" to "No EOB selected",
        "autoFillAppeal" to "Auto-fill appeal letter",
        "editAppealLetter" to "Edit appeal letter",
        "userProfile" to "User profile",
        "editSavedDetails" to "Edit saved details",
        "firebase" to "Firebase",
        "uploadInsuranceCardPath" to "Upload insurance card files to Firebase Storage path",
        "languageSettings" to "Language settings",
        "firstName" to "First name",
        "lastName" to "Last name",
        "email" to "Email",
        "password" to "Password",
        "city" to "City",
        "state" to "State",
        "insuranceCardDetails" to "Insurance card details",
        "firebaseInsuranceCardUrl" to "Firebase insurance card download URL",
        "howToUse" to "How to use EOBme",
        "supportStep1" to "1. Select your language, review the three intro screens, and create your profile.",
        "supportStep2" to "2. Upload or scan an EOB, then review insurance, provider, CPTs, billed amount, insurance paid, contractual adjustment, copay, deductible, and coinsurance.",
        "supportStep3" to "3. Use History to find EOBs sorted by Date of Service from the beginning of the year.",
        "supportStep4" to "4. Use CPT Count to see yearly counts such as 99215 (5x), with category tabs for OVs, labs, hospital, DME, and injections.",
        "supportStep5" to "5. Use Quick Actions for doctor appointments and Appeal Letter to auto-fill and edit a draft.",
        "features" to "Features",
        "featuresText" to "Multilingual text, appointment calendar, profile editing, language editing, insurance card/subscriber display, payer recognition, EOB history, CPT/ICD memory, duplicate EOB replacement, duplicate charge warnings, analysis totals, editable appeals, Firebase sync, and automatic logout after 3 minutes of inactivity.",
        "firebaseNotConfigured" to "Add app/google-services.json to enable live Firebase sync.",
        "firebaseConfigured" to "Firebase is configured. Sign in to sync EOBme data.",
        "firebaseActive" to "Firebase sync is active.",
        "categoryOfficeVisit" to "OVs",
        "categoryLab" to "Labs",
        "categoryHospital" to "Hospital",
        "categoryDme" to "DME",
        "categoryInjection" to "Injections",
        "categoryOther" to "Other"
    )
    val spanish = mapOf(
        "next" to "Siguiente",
        "createAccount" to "Crear cuenta",
        "profileRequired" to "Cree su perfil",
        "profileRequiredHelp" to "Nombre, apellido, correo electrónico y contraseña son obligatorios.",
        "continueHome" to "Continuar",
        "home" to "Inicio",
        "history" to "Historial",
        "cptCount" to "CPT",
        "appeal" to "Apelación",
        "profile" to "Perfil",
        "support" to "Ayuda",
        "logout" to "Salir",
        "uploadEob" to "Subir EOB",
        "duplicateReplaced" to "EOB duplicado encontrado. La copia original fue reemplazada por esta carga.",
        "eobAdded" to "EOB agregado.",
        "eobs" to "EOBs",
        "insuranceCard" to "Tarjeta de seguro",
        "firebaseCardFile" to "Archivo de tarjeta en Firebase",
        "subscriberId" to "ID de suscriptor",
        "addSubscriberId" to "Agregue el ID de suscriptor en el perfil",
        "member" to "Miembro",
        "profileIncomplete" to "Perfil incompleto",
        "firebaseDataSync" to "Sincronización de Firebase",
        "eobHistory" to "Historial de EOB",
        "insuranceCards" to "Tarjetas de seguro",
        "news" to "Noticias",
        "quickActions" to "Acciones rápidas",
        "appointmentCalendar" to "Calendario de citas",
        "appointmentDate" to "Fecha de cita",
        "appointmentProvider" to "Proveedor o médico",
        "appointmentNotes" to "Notas de cita",
        "addAppointment" to "Agregar cita",
        "removeAppointment" to "Eliminar cita",
        "noAppointments" to "Aún no hay citas médicas.",
        "uploadHelp" to "Pegue texto OCR de un EOB o use el ejemplo. Si coincide con un EOB existente, se reemplazará la copia original.",
        "eobText" to "Texto del EOB",
        "libraryUpload" to "Carga de biblioteca",
        "uploadFromLibrary" to "Subir desde biblioteca",
        "cameraScan" to "Escaneo de cámara",
        "scanWithCamera" to "Escanear con cámara",
        "insuranceNews" to "Noticias de seguros",
        "analysisResults" to "Resultados del análisis",
        "insurance" to "Seguro",
        "provider" to "Proveedor",
        "dateOfService" to "Fecha de servicio",
        "eobBilledAmount" to "Monto facturado del EOB",
        "insurancePaid" to "Seguro pagó",
        "contractualAdjustment" to "Ajuste contractual",
        "copay" to "Copago",
        "deductible" to "Deducible",
        "coinsurance" to "Coseguro",
        "noDuplicateCharges" to "No se detectaron cargos duplicados.",
        "billed" to "Facturado",
        "paid" to "Pagado",
        "cptsBilledIn" to "CPT facturados en",
        "cptRule" to "Solo se guardan códigos CPT/HCPCS de cinco caracteres que comienzan con 1-9 o A-J.",
        "category" to "Categoría",
        "noCptsFound" to "No se encontraron CPT para",
        "appealLetter" to "Carta de apelación",
        "appealHelp" to "Se completa automáticamente cuando el perfil está guardado y el proveedor se recupera del EOB.",
        "currentMember" to "Miembro actual",
        "selectedEob" to "EOB seleccionado",
        "noEobSelected" to "No hay EOB seleccionado",
        "autoFillAppeal" to "Autocompletar apelación",
        "editAppealLetter" to "Editar carta de apelación",
        "userProfile" to "Perfil de usuario",
        "editSavedDetails" to "Editar detalles guardados",
        "firebase" to "Firebase",
        "uploadInsuranceCardPath" to "Suba archivos de tarjeta de seguro a la ruta de Firebase Storage",
        "languageSettings" to "Configuración de idioma",
        "firstName" to "Nombre",
        "lastName" to "Apellido",
        "email" to "Correo electrónico",
        "password" to "Contraseña",
        "city" to "Ciudad",
        "state" to "Estado",
        "insuranceCardDetails" to "Detalles de tarjeta de seguro",
        "firebaseInsuranceCardUrl" to "URL de descarga de tarjeta en Firebase",
        "howToUse" to "Cómo usar EOBme",
        "supportStep1" to "1. Seleccione idioma, revise las tres pantallas de introducción y cree su perfil.",
        "supportStep2" to "2. Suba o escanee un EOB y revise seguro, proveedor, CPT, montos y responsabilidades.",
        "supportStep3" to "3. Use Historial para ver EOBs ordenados por fecha de servicio desde inicio de año.",
        "supportStep4" to "4. Use CPT para ver conteos anuales como 99215 (5x) por categoría.",
        "supportStep5" to "5. Use Acciones rápidas para citas y Apelación para editar una carta.",
        "features" to "Funciones",
        "featuresText" to "Texto multilingüe, calendario de citas, edición de perfil e idioma, tarjeta de seguro, historial, CPT/ICD, reemplazo de EOB duplicado, totales, apelaciones, Firebase y cierre automático.",
        "firebaseNotConfigured" to "Agregue app/google-services.json para activar la sincronización de Firebase.",
        "firebaseConfigured" to "Firebase está configurado. Inicie sesión para sincronizar EOBme.",
        "firebaseActive" to "La sincronización de Firebase está activa.",
        "categoryOfficeVisit" to "Visitas",
        "categoryLab" to "Laboratorios",
        "categoryHospital" to "Hospital",
        "categoryDme" to "DME",
        "categoryInjection" to "Inyecciones",
        "categoryOther" to "Otro"
    )
    val french = mapOf(
        "next" to "Suivant",
        "createAccount" to "Créer un compte",
        "profileRequired" to "Créer votre profil",
        "profileRequiredHelp" to "Prénom, nom, courriel et mot de passe sont requis.",
        "continueHome" to "Continuer",
        "home" to "Accueil",
        "history" to "Historique",
        "cptCount" to "CPT",
        "appeal" to "Appel",
        "profile" to "Profil",
        "support" to "Aide",
        "logout" to "Déconnexion",
        "uploadEob" to "Téléverser EOB",
        "duplicateReplaced" to "EOB en double trouvé. La copie originale a été remplacée par ce téléversement.",
        "eobAdded" to "EOB ajouté.",
        "eobs" to "EOBs",
        "insuranceCard" to "Carte d'assurance",
        "firebaseCardFile" to "Fichier de carte Firebase",
        "subscriberId" to "ID d'abonné",
        "addSubscriberId" to "Ajoutez l'ID d'abonné dans le profil",
        "member" to "Membre",
        "profileIncomplete" to "Profil incomplet",
        "firebaseDataSync" to "Synchronisation Firebase",
        "eobHistory" to "Historique EOB",
        "insuranceCards" to "Cartes d'assurance",
        "news" to "Actualités",
        "quickActions" to "Actions rapides",
        "appointmentCalendar" to "Calendrier des rendez-vous",
        "appointmentDate" to "Date du rendez-vous",
        "appointmentProvider" to "Prestataire ou médecin",
        "appointmentNotes" to "Notes du rendez-vous",
        "addAppointment" to "Ajouter rendez-vous",
        "removeAppointment" to "Supprimer rendez-vous",
        "noAppointments" to "Aucun rendez-vous médical ajouté.",
        "uploadHelp" to "Collez le texte OCR d'un EOB ou utilisez l'exemple. Si l'EOB existe déjà, la copie originale sera remplacée.",
        "eobText" to "Texte EOB",
        "libraryUpload" to "Téléversement bibliothèque",
        "uploadFromLibrary" to "Depuis bibliothèque",
        "cameraScan" to "Scan caméra",
        "scanWithCamera" to "Scanner avec caméra",
        "insuranceNews" to "Actualités assurance",
        "analysisResults" to "Résultats d'analyse",
        "insurance" to "Assurance",
        "provider" to "Prestataire",
        "dateOfService" to "Date de service",
        "eobBilledAmount" to "Montant facturé EOB",
        "insurancePaid" to "Assurance payée",
        "contractualAdjustment" to "Ajustement contractuel",
        "copay" to "Quote-part",
        "deductible" to "Franchise",
        "coinsurance" to "Coassurance",
        "noDuplicateCharges" to "Aucun frais en double détecté.",
        "billed" to "Facturé",
        "paid" to "Payé",
        "cptsBilledIn" to "CPT facturés en",
        "cptRule" to "Seuls les codes CPT/HCPCS de cinq caractères commençant par 1-9 ou A-J sont conservés.",
        "category" to "Catégorie",
        "noCptsFound" to "Aucun CPT trouvé pour",
        "appealLetter" to "Lettre d'appel",
        "appealHelp" to "Remplie automatiquement quand le profil est enregistré et le prestataire est lu dans l'EOB.",
        "currentMember" to "Membre actuel",
        "selectedEob" to "EOB sélectionné",
        "noEobSelected" to "Aucun EOB sélectionné",
        "autoFillAppeal" to "Remplir l'appel",
        "editAppealLetter" to "Modifier la lettre d'appel",
        "userProfile" to "Profil utilisateur",
        "editSavedDetails" to "Modifier les détails",
        "firebase" to "Firebase",
        "uploadInsuranceCardPath" to "Téléversez les cartes d'assurance vers le chemin Firebase Storage",
        "languageSettings" to "Paramètres de langue",
        "firstName" to "Prénom",
        "lastName" to "Nom",
        "email" to "Courriel",
        "password" to "Mot de passe",
        "city" to "Ville",
        "state" to "État",
        "insuranceCardDetails" to "Détails de carte d'assurance",
        "firebaseInsuranceCardUrl" to "URL de téléchargement Firebase",
        "howToUse" to "Comment utiliser EOBme",
        "supportStep1" to "1. Choisissez la langue, lisez les trois écrans et créez votre profil.",
        "supportStep2" to "2. Téléversez ou scannez un EOB et vérifiez assurance, prestataire, CPT et montants.",
        "supportStep3" to "3. Utilisez Historique pour voir les EOB triés par date de service.",
        "supportStep4" to "4. Utilisez CPT pour voir les comptes annuels par catégorie.",
        "supportStep5" to "5. Utilisez Actions rapides pour les rendez-vous et Appel pour éditer une lettre.",
        "features" to "Fonctionnalités",
        "featuresText" to "Texte multilingue, calendrier, profil, langue, carte d'assurance, historique, CPT/ICD, remplacement des EOB en double, totaux, appels, Firebase et déconnexion automatique.",
        "firebaseNotConfigured" to "Ajoutez app/google-services.json pour activer la synchronisation Firebase.",
        "firebaseConfigured" to "Firebase est configuré. Connectez-vous pour synchroniser EOBme.",
        "firebaseActive" to "La synchronisation Firebase est active.",
        "categoryOfficeVisit" to "Consultations",
        "categoryLab" to "Laboratoires",
        "categoryHospital" to "Hôpital",
        "categoryDme" to "DME",
        "categoryInjection" to "Injections",
        "categoryOther" to "Autre"
    )
    val chinese = mapOf(
        "next" to "下一步",
        "createAccount" to "创建账户",
        "profileRequired" to "创建个人资料",
        "profileRequiredHelp" to "介绍后必须输入名字、姓氏、电子邮件和密码。",
        "continueHome" to "继续",
        "home" to "主页",
        "history" to "历史",
        "cptCount" to "CPT",
        "appeal" to "申诉",
        "profile" to "资料",
        "support" to "支持",
        "logout" to "退出",
        "uploadEob" to "上传 EOB",
        "duplicateReplaced" to "发现重复 EOB。原始副本已被此上传替换。",
        "eobAdded" to "EOB 已添加。",
        "eobs" to "EOB",
        "insuranceCard" to "保险卡",
        "firebaseCardFile" to "Firebase 卡文件",
        "subscriberId" to "订阅者 ID",
        "addSubscriberId" to "请在资料中添加订阅者 ID",
        "member" to "会员",
        "profileIncomplete" to "资料未完成",
        "firebaseDataSync" to "Firebase 数据同步",
        "eobHistory" to "EOB 历史",
        "insuranceCards" to "保险卡",
        "news" to "新闻",
        "quickActions" to "快速操作",
        "appointmentCalendar" to "预约日历",
        "appointmentDate" to "预约日期",
        "appointmentProvider" to "提供者或医生",
        "appointmentNotes" to "预约备注",
        "addAppointment" to "添加预约",
        "removeAppointment" to "删除预约",
        "noAppointments" to "尚未添加医生预约。",
        "uploadHelp" to "粘贴 EOB OCR 文本或使用示例。如果与现有 EOB 匹配，将替换原始副本。",
        "eobText" to "EOB 文本",
        "libraryUpload" to "图库上传",
        "uploadFromLibrary" to "从图库上传",
        "cameraScan" to "相机扫描",
        "scanWithCamera" to "用相机扫描",
        "insuranceNews" to "保险新闻",
        "analysisResults" to "分析结果",
        "insurance" to "保险",
        "provider" to "提供者",
        "dateOfService" to "服务日期",
        "eobBilledAmount" to "EOB 账单金额",
        "insurancePaid" to "保险支付",
        "contractualAdjustment" to "合同调整",
        "copay" to "共付额",
        "deductible" to "免赔额",
        "coinsurance" to "共同保险",
        "noDuplicateCharges" to "未检测到重复收费。",
        "billed" to "账单",
        "paid" to "已付",
        "cptsBilledIn" to "计费 CPT 年份",
        "cptRule" to "仅保存以 1-9 或 A-J 开头的五字符 CPT/HCPCS 代码。",
        "category" to "类别",
        "noCptsFound" to "未找到 CPT",
        "appealLetter" to "申诉信",
        "appealHelp" to "保存资料且可从 EOB 读取提供者后自动填写。",
        "currentMember" to "当前会员",
        "selectedEob" to "已选 EOB",
        "noEobSelected" to "未选择 EOB",
        "autoFillAppeal" to "自动填写申诉",
        "editAppealLetter" to "编辑申诉信",
        "userProfile" to "用户资料",
        "editSavedDetails" to "编辑已保存信息",
        "firebase" to "Firebase",
        "uploadInsuranceCardPath" to "将保险卡上传到 Firebase Storage 路径",
        "languageSettings" to "语言设置",
        "firstName" to "名字",
        "lastName" to "姓氏",
        "email" to "电子邮件",
        "password" to "密码",
        "city" to "城市",
        "state" to "州",
        "insuranceCardDetails" to "保险卡详情",
        "firebaseInsuranceCardUrl" to "Firebase 保险卡下载 URL",
        "howToUse" to "如何使用 EOBme",
        "supportStep1" to "1. 选择语言，查看三个介绍屏幕，并创建资料。",
        "supportStep2" to "2. 上传或扫描 EOB，并查看保险、提供者、CPT 和金额。",
        "supportStep3" to "3. 使用历史按服务日期查看 EOB。",
        "supportStep4" to "4. 使用 CPT 查看年度分类计数。",
        "supportStep5" to "5. 使用快速操作管理预约，并编辑申诉信。",
        "features" to "功能",
        "featuresText" to "多语言文本、预约日历、资料和语言编辑、保险卡、历史、CPT/ICD、重复 EOB 替换、总额、申诉、Firebase 和自动退出。",
        "firebaseNotConfigured" to "添加 app/google-services.json 以启用 Firebase 实时同步。",
        "firebaseConfigured" to "Firebase 已配置。请登录以同步 EOBme 数据。",
        "firebaseActive" to "Firebase 同步已启用。",
        "categoryOfficeVisit" to "门诊",
        "categoryLab" to "实验室",
        "categoryHospital" to "医院",
        "categoryDme" to "DME",
        "categoryInjection" to "注射",
        "categoryOther" to "其他"
    )

    return when (language) {
        AppLanguage.English -> english
        AppLanguage.Spanish -> spanish
        AppLanguage.French -> french
        AppLanguage.Chinese -> chinese
    }[key] ?: english.getValue(key)
}

private val sampleEobText = """
    UnitedHealthcare Explanation of Benefits
    Provider: Lakeside Family Medical Clinic
    Date of Service: 01/12/2025
    99215 billed $265.00 insurance paid $120.00 contractual adjustment $95.00 copay $25.00 deductible $20.00 coinsurance $5.00
    80053 billed $48.00 insurance paid $22.00 contractual adjustment $18.00 copay $0.00 deductible $8.00 coinsurance $0.00
""".trimIndent()