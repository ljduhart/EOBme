package com.eobme.app

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
import com.eobme.app.data.AppLanguage
import com.eobme.app.data.AppealLetterGenerator
import com.eobme.app.data.CptCategory
import com.eobme.app.data.EobAnalyzer
import com.eobme.app.data.EobKnowledgeBase
import com.eobme.app.data.EobRecord
import com.eobme.app.data.FirebaseEobRepository
import com.eobme.app.data.FirebaseSyncStatus
import com.eobme.app.data.NewsRelease
import com.eobme.app.data.UserProfile
import com.eobme.app.data.asCurrency
import com.eobme.app.ui.theme.EOBmeTheme
import kotlinx.coroutines.delay

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
        Text("Select a language before the introduction screens.", style = MaterialTheme.typography.bodyLarge)
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
        ProfileFields(profile = profile, onProfileChanged = onProfileChanged)
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
                records.addAll(remoteRecords.sortedBy { it.serviceDateSortKey })
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
                uploadText = uploadText,
                onUploadTextChanged = {
                    uploadText = it
                    onActivity()
                },
                onUpload = { source ->
                    val textToAnalyze = uploadText.ifBlank { sampleEobText }
                    val record = EobAnalyzer.analyze(textToAnalyze, source, (records.maxOfOrNull { it.id } ?: 0) + 1)
                    records.add(record)
                    records.sortBy { it.serviceDateSortKey }
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

            1 -> HistoryTab(records = records.sortedBy { it.serviceDateSortKey }, onSelected = {
                selectedRecord = it
                selectedTab = 0
                onActivity()
            })

            2 -> CptCountTab(
                records = records,
                selectedCategory = selectedCptCategory,
                onCategoryChanged = {
                    selectedCptCategory = it
                    onActivity()
                }
            )

            3 -> AppealTab(
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
    uploadText: String,
    onUploadTextChanged: (String) -> Unit,
    onUpload: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { InsuranceCard(profile) }
        item { FirebaseSyncCard(firebaseStatus) }
        item { UploadCard(language, uploadText, onUploadTextChanged, onUpload) }
        item { selectedRecord?.let { AnalysisResultsCard(it) } }
        item { NewsCard(firebaseNews.ifEmpty { EobKnowledgeBase.newsReleases }) }
        item {
            Text("${t(language, "history")}: ${records.size} EOBs", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun InsuranceCard(profile: UserProfile) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Insurance card", style = MaterialTheme.typography.titleMedium)
            if (profile.insuranceCardSummary.isNotBlank()) {
                Text(profile.insuranceCardSummary)
            } else if (profile.insuranceCardDownloadUrl.isNotBlank()) {
                Text("Firebase card file: ${profile.insuranceCardDownloadUrl}")
            } else {
                Text("Subscriber ID: ${profile.subscriberId.ifBlank { "Add subscriber ID in profile settings" }}")
            }
            Text("Member: ${profile.fullName.ifBlank { "Profile incomplete" }}")
        }
    }
}

@Composable
private fun FirebaseSyncCard(firebaseStatus: FirebaseSyncStatus) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Firebase data sync", style = MaterialTheme.typography.titleMedium)
            Text(firebaseStatus.message)
            if (firebaseStatus.userId.isNotBlank()) {
                Text("Profile: users/${firebaseStatus.userId}")
                Text("EOB history: users/${firebaseStatus.userId}/eobs")
                Text("Insurance cards: users/${firebaseStatus.userId}/insurance-cards")
                Text("News: insuranceNews")
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
            Text("Paste OCR text from an EOB or use the sample scan. Library uploads use the same duplicate detection as camera scans.")
            OutlinedTextField(
                value = uploadText,
                onValueChange = onUploadTextChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                label = { Text("EOB text") }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onUpload("Library upload") }) { Text("Upload from library") }
                OutlinedButton(onClick = { onUpload("Camera scan") }) { Text("Scan with camera") }
            }
        }
    }
}

@Composable
private fun NewsCard(newsItems: List<NewsRelease>) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Insurance news", style = MaterialTheme.typography.titleMedium)
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
private fun AnalysisResultsCard(record: EobRecord) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Analysis Results", style = MaterialTheme.typography.titleLarge)
            Text("Insurance: ${record.insuranceName}")
            Text("Provider: ${record.providerName}")
            Text("Date of Service: ${record.serviceDate}")
            AmountRow("EOB billed amount", record.totalBilledAmount)
            AmountRow("Insurance paid", record.totalInsurancePaidAmount)
            AmountRow("Contractual adjustment", record.totalContractualAdjustmentAmount)
            AmountRow("Copay", record.totalCopayAmount)
            AmountRow("Deductible", record.totalDeductibleAmount)
            AmountRow("Coinsurance", record.totalCoinsuranceAmount)
            if (record.duplicateChargeWarnings.isEmpty()) {
                Text("No duplicate charges detected.")
            } else {
                record.duplicateChargeWarnings.forEach { Text(it) }
            }
            record.charges.forEach { charge ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${charge.cptCode}: ${charge.cptDescription}", style = MaterialTheme.typography.titleSmall)
                        Text("${charge.category.displayName} • Billed ${charge.billedAmount.asCurrency()} • Paid ${charge.insurancePaidAmount.asCurrency()}")
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
private fun HistoryTab(records: List<EobRecord>, onSelected: (EobRecord) -> Unit) {
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
                    Text("Insurance: ${record.insuranceName}")
                    Text("Provider: ${record.providerName}")
                    Text("Billed: ${record.totalBilledAmount.asCurrency()} • Paid: ${record.totalInsurancePaidAmount.asCurrency()}")
                }
            }
        }
    }
}

@Composable
private fun CptCountTab(
    records: List<EobRecord>,
    selectedCategory: CptCategory,
    onCategoryChanged: (CptCategory) -> Unit
) {
    val year = 2025
    val usage = EobAnalyzer.cptUsage(records, year).filter { it.info.category == selectedCategory }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("CPTs billed in $year", style = MaterialTheme.typography.titleLarge)
            Text("Only five-character CPT/HCPCS codes starting with 1-9 or A-J are stored.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CptCategory.entries.filter { it != CptCategory.Other }.forEach { category ->
                    AssistChip(
                        onClick = { onCategoryChanged(category) },
                        label = { Text(category.displayName) },
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
                    Text("Category: ${item.info.category.displayName}")
                }
            }
        }
        item {
            if (usage.isEmpty()) Text("No ${selectedCategory.displayName} CPTs found for $year yet.")
        }
    }
}

@Composable
private fun AppealTab(
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
        Text("Appeal letter", style = MaterialTheme.typography.titleLarge)
        Text("Auto-filled when profile details are saved and provider information is retrievable from the selected EOB.")
        Text("Current member: ${profile.fullName.ifBlank { "Profile incomplete" }}")
        Text("Selected EOB: ${selectedRecord?.providerName ?: "No EOB selected"}")
        Button(onClick = onRegenerate) { Text("Auto-fill appeal letter") }
        OutlinedTextField(
            value = letter,
            onValueChange = onLetterChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 14,
            label = { Text("Edit appeal letter") }
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
        Text("User profile", style = MaterialTheme.typography.titleLarge)
        Text("Edit saved details")
        Text("Firebase: ${firebaseStatus.message}")
        if (insuranceCardStoragePath.isNotBlank()) {
            Text("Upload insurance card files to Firebase Storage path: $insuranceCardStoragePath")
        }
        ProfileFields(profile = profile, onProfileChanged = onProfileChanged)
        Text("Language settings", style = MaterialTheme.typography.titleMedium)
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
private fun ProfileFields(profile: UserProfile, onProfileChanged: (UserProfile) -> Unit) {
    OutlinedTextField(
        value = profile.firstName,
        onValueChange = { onProfileChanged(profile.copy(firstName = it)) },
        label = { Text("First name") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.lastName,
        onValueChange = { onProfileChanged(profile.copy(lastName = it)) },
        label = { Text("Last name") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.email,
        onValueChange = { onProfileChanged(profile.copy(email = it)) },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.password,
        onValueChange = { onProfileChanged(profile.copy(password = it)) },
        label = { Text("Password") },
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = profile.city,
            onValueChange = { onProfileChanged(profile.copy(city = it)) },
            label = { Text("City") },
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = profile.state,
            onValueChange = { onProfileChanged(profile.copy(state = it)) },
            label = { Text("State") },
            modifier = Modifier.weight(1f)
        )
    }
    OutlinedTextField(
        value = profile.subscriberId,
        onValueChange = { onProfileChanged(profile.copy(subscriberId = it)) },
        label = { Text("Subscriber ID") },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = profile.insuranceCardSummary,
        onValueChange = { onProfileChanged(profile.copy(insuranceCardSummary = it)) },
        label = { Text("Insurance card details") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
    )
    OutlinedTextField(
        value = profile.insuranceCardDownloadUrl,
        onValueChange = { onProfileChanged(profile.copy(insuranceCardDownloadUrl = it)) },
        label = { Text("Firebase insurance card download URL") },
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
        Text("How to use EOBme", style = MaterialTheme.typography.titleMedium)
        Text("1. Select your language, review the three intro screens, and create your profile.")
        Text("2. Upload or scan an EOB, then review the Analysis Results for insurance, provider, CPTs, billed amount, insurance paid, contractual adjustment, copay, deductible, and coinsurance.")
        Text("3. Use History to find EOBs sorted by Date of Service from the beginning of the year.")
        Text("4. Use CPT Count to see yearly counts such as 99215 (5x), with category tabs for OVs, labs, hospital, DME, and injections.")
        Text("5. Open Appeal Letter to auto-fill a draft and edit it before sending.")
        Text("Features", style = MaterialTheme.typography.titleMedium)
        Text("Multilingual onboarding, profile editing, language editing, insurance card/subscriber display, payer recognition, EOB history, CPT/ICD memory, duplicate warning logic, analysis totals, editable appeals, and automatic logout after 3 minutes of inactivity.")
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
        "uploadEob" to "Upload EOB"
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
        "uploadEob" to "Subir EOB"
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
        "uploadEob" to "Téléverser EOB"
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
        "uploadEob" to "上传 EOB"
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