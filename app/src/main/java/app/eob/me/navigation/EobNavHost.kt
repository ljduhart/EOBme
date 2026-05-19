package app.eob.me.navigation

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.eob.me.data.AppLanguage
import app.eob.me.data.AppealLetterGenerator
import app.eob.me.data.CptCategory
import app.eob.me.data.DoctorAppointment
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.EobRecord
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.NewsRelease
import app.eob.me.data.UserProfile
import app.eob.me.localization.Translations
import app.eob.me.screens.AnalysisScreen
import app.eob.me.screens.AppealScreen
import app.eob.me.screens.CptCountScreen
import app.eob.me.screens.HomeScreen
import app.eob.me.screens.NewsScreen
import app.eob.me.screens.ProfileScreen
import app.eob.me.util.OcrProcessor
import kotlinx.coroutines.launch

@Composable
fun EobNavHost(
    language: AppLanguage,
    profile: UserProfile,
    firebaseRepository: FirebaseEobRepository,
    onProfileChanged: (UserProfile) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onActivity: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val records = remember { mutableStateListOf(EobAnalyzer.analyze(sampleEobText, "Sample camera scan", 1)) }
    val appointments = remember { mutableStateListOf<DoctorAppointment>() }
    var selectedRecord by remember { mutableStateOf<EobRecord?>(records.firstOrNull()) }
    var uploadText by remember { mutableStateOf("") }
    var uploadNotice by remember { mutableStateOf("") }
    var selectedCptCategory by remember { mutableStateOf(CptCategory.OfficeVisit) }
    var appealLetter by remember { mutableStateOf(AppealLetterGenerator.generate(profile, selectedRecord)) }
    var firebaseStatus by remember { mutableStateOf(firebaseRepository.status()) }
    var firebaseNews by remember { mutableStateOf<List<NewsRelease>>(emptyList()) }

    fun saveAnalyzedEob(source: String, rawText: String) {
        val analyzedRecord = EobAnalyzer.analyze(rawText, source, (records.maxOfOrNull { it.id } ?: 0) + 1)
        val duplicateIndex = records.indexOfFirst { existing -> EobAnalyzer.isSameEob(existing, analyzedRecord) }
        val record = if (duplicateIndex >= 0) {
            uploadNotice = Translations.t(language, "duplicateReplaced")
            analyzedRecord.copy(id = records[duplicateIndex].id)
        } else {
            uploadNotice = Translations.t(language, "eobAdded")
            analyzedRecord
        }
        if (duplicateIndex >= 0) records[duplicateIndex] = record else records.add(record)
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
        navController.navigate(EobRoute.Analysis.route)
    }

    fun handleOcrResult(source: String, text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, Translations.t(language, "ocrEmpty"), Toast.LENGTH_SHORT).show()
        } else {
            saveAnalyzedEob(source, text)
        }
    }

    val libraryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { OcrProcessor.recognizeFromUri(context, uri) }
                    .onSuccess { handleOcrResult(Translations.t(language, "libraryUpload"), it.ifBlank { uploadText }) }
                    .onFailure { Toast.makeText(context, Translations.t(language, "ocrFailed"), Toast.LENGTH_SHORT).show() }
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            scope.launch {
                runCatching { OcrProcessor.recognizeFromBitmap(bitmap) }
                    .onSuccess { handleOcrResult(Translations.t(language, "cameraScan"), it.ifBlank { uploadText }) }
                    .onFailure { Toast.makeText(context, Translations.t(language, "ocrFailed"), Toast.LENGTH_SHORT).show() }
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(null)
        else Toast.makeText(context, Translations.t(language, "cameraPermissionRequired"), Toast.LENGTH_SHORT).show()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(profile.email, profile.password, profile.isComplete) {
        if (profile.isComplete && firebaseStatus.userId.isNotBlank()) {
            firebaseRepository.saveProfile(firebaseStatus.userId, profile) {}
        }
    }

    DisposableEffect(firebaseStatus.userId) {
        val userId = firebaseStatus.userId
        val profileListener = firebaseRepository.observeProfile(
            userId = userId,
            currentPassword = profile.password,
            onProfile = {
                onProfileChanged(it)
                onActivity()
            },
            onError = { firebaseStatus = firebaseStatus.copy(message = it) }
        )
        val eobListener = firebaseRepository.observeEobs(
            userId = userId,
            onRecords = { newRecords ->
                val compacted = EobAnalyzer.compactDuplicateEobs(newRecords)
                records.clear()
                records.addAll(compacted)
                
                // Smart Selection Logic: Only reset if current selection is invalid
                val current = selectedRecord
                if (current == null || compacted.none { it.id == current.id }) {
                    selectedRecord = compacted.firstOrNull()
                } else {
                    // Update selection with latest cloud data for this record
                    compacted.find { it.id == current.id }?.let { selectedRecord = it }
                }
                
                appealLetter = AppealLetterGenerator.generate(profile, selectedRecord)
                onActivity()
            },
            onError = { firebaseStatus = firebaseStatus.copy(message = it) }
        )
        val newsListener = firebaseRepository.observeInsuranceNews(
            onNews = {
                firebaseNews = it
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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: EobRoute.Home.route

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(Translations.t(language, "scanBill"), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(language, onProfile = { navController.navigate(EobRoute.Profile.route) }, onLogout = onLogout)
            ScrollableTabRow(
                selectedTabIndex = primaryRoutes.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0),
                edgePadding = 8.dp
            ) {
                primaryRoutes.forEach { route ->
                    Tab(
                        selected = currentRoute == route.route,
                        onClick = {
                            navController.navigate(route.route) {
                                launchSingleTop = true
                                popUpTo(EobRoute.Home.route)
                            }
                            onActivity()
                        },
                        text = { Text(route.title(language)) }
                    )
                }
            }
            NavHost(navController = navController, startDestination = EobRoute.Home.route) {
                composable(EobRoute.Home.route) {
                    HomeScreen(
                        language = language,
                        profile = profile,
                        records = records.sortedBy { it.serviceDateSortKey },
                        appointments = appointments.sortedBy { it.date },
                        uploadNotice = uploadNotice,
                        uploadText = uploadText,
                        onUploadTextChanged = {
                            uploadText = it
                            onActivity()
                        },
                        onAddAppointment = { date, provider, notes ->
                            appointments.add(DoctorAppointment((appointments.maxOfOrNull { it.id } ?: 0) + 1, date, provider, notes))
                            onActivity()
                        },
                        onRemoveAppointment = {
                            appointments.removeAll { appointment -> appointment.id == it.id }
                            onActivity()
                        },
                        onLibraryUpload = { libraryLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onCameraScan = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
                composable(EobRoute.Analysis.route) {
                    AnalysisScreen(language, records.sortedBy { it.serviceDateSortKey }, selectedRecord) {
                        selectedRecord = it
                        appealLetter = AppealLetterGenerator.generate(profile, it)
                        uploadNotice = "" // Clear stale real-time notification
                        onActivity()
                    }
                }
                composable(EobRoute.CptCount.route) {
                    CptCountScreen(language, records, selectedCptCategory) {
                        selectedCptCategory = it
                        onActivity()
                    }
                }
                composable(EobRoute.News.route) {
                    NewsScreen(language, EobKnowledgeBase.currentNewsReleases(firebaseNews.ifEmpty { EobKnowledgeBase.newsReleases }))
                }
                composable(EobRoute.Appeal.route) {
                    AppealScreen(language, profile, selectedRecord, appealLetter, {
                        appealLetter = AppealLetterGenerator.generate(profile, selectedRecord)
                        onActivity()
                    }, {
                        appealLetter = it
                        onActivity()
                    })
                }
                composable(EobRoute.Profile.route) {
                    ProfileScreen(
                        language = language,
                        profile = profile,
                        onProfileChanged = {
                            onProfileChanged(it)
                            if (firebaseStatus.userId.isNotBlank()) firebaseRepository.saveProfile(firebaseStatus.userId, it) {}
                            onActivity()
                        },
                        onLanguageChanged = onLanguageChanged,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(language: AppLanguage, onProfile: () -> Unit, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("EOBme", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onProfile) { Text(Translations.t(language, "profile")) }
            OutlinedButton(onClick = onLogout) { Text(Translations.t(language, "logout")) }
        }
    }
}

private fun EobRoute.title(language: AppLanguage): String {
    return when (this) {
        EobRoute.Home -> Translations.t(language, "home")
        EobRoute.Analysis -> Translations.t(language, "analysis")
        EobRoute.CptCount -> Translations.t(language, "cptCount")
        EobRoute.News -> Translations.t(language, "news")
        EobRoute.Appeal -> Translations.t(language, "appeal")
        EobRoute.Profile -> Translations.t(language, "profile")
    }
}

private val sampleEobText = """
    UnitedHealthcare Explanation of Benefits
    Provider: Lakeside Family Medical Clinic
    Date of Service: 01/12/2025
    99215 billed $265.00 insurance paid $120.00 contractual adjustment $95.00 copay $25.00 deductible $20.00 coinsurance $5.00
    80053 billed $48.00 insurance paid $22.00 contractual adjustment $18.00 copay $0.00 deductible $8.00 coinsurance $0.00
""".trimIndent()
