package app.eob.me.navigation

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import app.eob.me.app.EobViewModel
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.UserProfile
import app.eob.me.data.EobStrings
import app.eob.me.ui.screens.AnalysisScreen
import app.eob.me.ui.screens.AppealScreen
import app.eob.me.ui.screens.CameraCaptureScreen
import app.eob.me.ui.screens.CptCountScreen
import app.eob.me.ui.screens.HomeScreen
import app.eob.me.ui.screens.NewsScreen
import app.eob.me.ui.screens.ProfileScreen
import app.eob.me.util.OcrProcessor
import kotlinx.coroutines.launch

@Composable
fun EobNavHost(
    language: AppLanguage,
    profile: UserProfile,
    firebaseUserId: String,
    firebaseRepository: FirebaseEobRepository,
    onProfileChanged: (UserProfile) -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onActivity: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val viewModel: EobViewModel = viewModel()

    fun navigateToAnalysis() {
        navController.navigate(EobRoute.Analysis.route) {
            launchSingleTop = true
            popUpTo(EobRoute.Home.route)
        }
        onActivity()
    }

    fun processScannedDocument(uri: Uri, sourceName: String) {
        scope.launch {
            runCatching {
                val preparedUri = OcrProcessor.prepareUriForUpload(context, uri)
                val ocrText = OcrProcessor.recognizeFromUri(context, preparedUri).trim()
                if (ocrText.isBlank()) {
                    error("empty_ocr")
                }
                preparedUri to ocrText
            }.onSuccess { (preparedUri, ocrText) ->
                val record = viewModel.importEobFromText(ocrText, sourceName, profile, language)
                val userId = firebaseRepository.currentUserId().ifBlank { firebaseUserId }
                if (firebaseRepository.canSyncToCloud()) {
                    firebaseRepository.saveEob(userId, record) { message ->
                        if (message.isNotBlank()) viewModel.updateUploadNotice(message)
                    }
                    viewModel.uploadEobFile(
                        repository = firebaseRepository,
                        userId = userId,
                        uri = preparedUri,
                        sourceName = sourceName,
                        language = language
                    )
                }
                navigateToAnalysis()
            }.onFailure { error ->
                val message = if (error.message == "empty_ocr") {
                    EobStrings.t(language, "ocrEmpty")
                } else {
                    EobStrings.t(language, "ocrFailed")
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                navigateToAnalysis()
            }
        }
    }

    val libraryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            processScannedDocument(uri, EobStrings.t(language, "libraryUpload"))
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) navController.navigate(EobRoute.CameraCapture.route)
        else Toast.makeText(context, EobStrings.t(language, "cameraPermissionRequired"), Toast.LENGTH_SHORT).show()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(firebaseUserId) {
        val userId = firebaseRepository.currentUserId().ifBlank { firebaseUserId }
        viewModel.syncFirebaseStatus(firebaseRepository, userId)
    }

    LaunchedEffect(profile.email, profile.password, profile.isComplete, firebaseUserId) {
        val userId = firebaseRepository.currentUserId().ifBlank { firebaseUserId }
        if (profile.isComplete && userId.isNotBlank()) {
            firebaseRepository.saveProfile(userId, profile) {}
            firebaseRepository.saveInsuranceCardMetadata(userId, profile) {}
        }
    }

    DisposableEffect(firebaseUserId) {
        val userId = firebaseRepository.currentUserId().ifBlank { firebaseUserId }
        val profileListener = firebaseRepository.observeProfile(
            userId = userId,
            currentPassword = profile.password,
            onProfile = {
                onProfileChanged(it)
                onActivity()
            },
            onError = { viewModel.firebaseStatus = viewModel.firebaseStatus.copy(message = it) }
        )
        val eobListener = firebaseRepository.observeEobs(
            userId = userId,
            onRecords = {
                viewModel.replaceRecords(it, profile)
                onActivity()
            },
            onError = { viewModel.firebaseStatus = viewModel.firebaseStatus.copy(message = it) }
        )
        val newsListener = firebaseRepository.observeInsuranceNews(
            onNews = {
                viewModel.firebaseNews = it
                onActivity()
            },
            onError = { viewModel.firebaseStatus = viewModel.firebaseStatus.copy(message = it) }
        )
        onDispose {
            profileListener?.remove()
            eobListener?.remove()
            newsListener?.remove()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: EobRoute.Home.route
    val selectedTabIndex = primaryRoutes.indexOfFirst { it.route == currentRoute }
        .takeIf { it >= 0 }
        ?: primaryRoutes.indexOf(EobRoute.Analysis).coerceAtLeast(0)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text(EobStrings.t(language, "scanBill"), modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Header(language, onProfile = { navController.navigate(EobRoute.Profile.route) }, onLogout = onLogout)
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
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
                        records = viewModel.records.sortedBy { it.serviceDateSortKey },
                        appointments = viewModel.appointments.sortedBy { it.date },
                        uploadNotice = viewModel.uploadNotice,
                        onAddAppointment = { date, provider, time, notes ->
                            viewModel.addAppointment(date, provider, time, notes)
                            onActivity()
                        },
                        onRemoveAppointment = {
                            viewModel.removeAppointment(it)
                            onActivity()
                        }
                    )
                }
                composable(EobRoute.Analysis.route) {
                    AnalysisScreen(
                        language = language,
                        records = viewModel.records.sortedBy { it.serviceDateSortKey },
                        selectedRecord = viewModel.selectedRecord,
                        uploadText = viewModel.uploadText,
                        uploadNotice = viewModel.uploadNotice,
                        onUploadTextChanged = {
                            viewModel.uploadText = it
                            onActivity()
                        },
                        onLibraryUpload = { libraryLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onCameraScan = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    ) {
                        viewModel.selectRecord(it, profile)
                        onActivity()
                    }
                }
                composable(EobRoute.CameraCapture.route) {
                    CameraCaptureScreen(
                        language = language,
                        onImageCaptured = { uri ->
                            navController.popBackStack()
                            processScannedDocument(uri, EobStrings.t(language, "cameraScan"))
                        },
                        onClose = { navController.popBackStack() }
                    )
                }
                composable(EobRoute.CptCount.route) {
                    CptCountScreen(language, viewModel.records, viewModel.selectedCptCategory) {
                        viewModel.selectedCptCategory = it
                        onActivity()
                    }
                }
                composable(EobRoute.News.route) {
                    NewsScreen(language, EobKnowledgeBase.currentNewsReleases(viewModel.firebaseNews.ifEmpty { EobKnowledgeBase.newsReleases }))
                }
                composable(EobRoute.Appeal.route) {
                    AppealScreen(language, profile, viewModel.selectedRecord, viewModel.appealLetter, {
                        viewModel.regenerateAppeal(profile)
                        onActivity()
                    }, {
                        viewModel.updateAppeal(it)
                        onActivity()
                    })
                }
                composable(EobRoute.Profile.route) {
                    ProfileScreen(
                        language = language,
                        profile = profile,
                        onProfileChanged = {
                            onProfileChanged(it)
                            val userId = firebaseRepository.currentUserId().ifBlank { firebaseUserId }
                            if (userId.isNotBlank()) {
                                firebaseRepository.saveProfile(userId, it) {}
                                firebaseRepository.saveInsuranceCardMetadata(userId, it) {}
                            }
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
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("EOBme", style = MaterialTheme.typography.headlineMedium)
        OutlinedButton(onClick = { expanded = true }) { Text("👤") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(EobStrings.t(language, "profile")) },
                onClick = {
                    expanded = false
                    onProfile()
                }
            )
            DropdownMenuItem(
                text = { Text(EobStrings.t(language, "support")) },
                onClick = {
                    expanded = false
                    onProfile()
                }
            )
            DropdownMenuItem(
                text = { Text(EobStrings.t(language, "logout")) },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

private fun EobRoute.title(language: AppLanguage): String {
    return when (this) {
        EobRoute.Home -> EobStrings.t(language, "home")
        EobRoute.Analysis -> "Eob/Analysis"
        EobRoute.CptCount -> EobStrings.t(language, "cptCount")
        EobRoute.News -> EobStrings.t(language, "news")
        EobRoute.Appeal -> EobStrings.t(language, "appeal")
        EobRoute.Profile -> EobStrings.t(language, "profile")
        EobRoute.CameraCapture -> EobStrings.t(language, "scanBill")
    }
}

