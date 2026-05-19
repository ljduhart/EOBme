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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import app.eob.me.app.EobViewModel
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobAnalyzer
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.data.FirebaseEobRepository
import app.eob.me.data.UserProfile
import app.eob.me.localization.Translations
import app.eob.me.screens.AnalysisScreen
import app.eob.me.screens.AppealScreen
import app.eob.me.screens.CptCountScreen
import app.eob.me.screens.HomeScreen
import app.eob.me.screens.NewsScreen
import app.eob.me.screens.ProfileScreen

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
    val navController = rememberNavController()
    val viewModel: EobViewModel = viewModel()

    val libraryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.uploadEobFile(
                repository = firebaseRepository,
                userId = viewModel.firebaseStatus.userId,
                uri = uri,
                sourceName = Translations.t(language, "libraryUpload"),
                language = language
            )
            navController.navigate(EobRoute.Analysis.route)
            onActivity()
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.uploadEobBitmap(
                repository = firebaseRepository,
                userId = viewModel.firebaseStatus.userId,
                bitmap = bitmap,
                sourceName = Translations.t(language, "cameraScan"),
                language = language
            )
            navController.navigate(EobRoute.Analysis.route)
            onActivity()
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
        if (profile.isComplete && viewModel.firebaseStatus.userId.isNotBlank()) {
            firebaseRepository.saveProfile(viewModel.firebaseStatus.userId, profile) {}
        }
    }

    DisposableEffect(viewModel.firebaseStatus.userId) {
        val userId = viewModel.firebaseStatus.userId
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
                        records = viewModel.records.sortedBy { it.serviceDateSortKey },
                        appointments = viewModel.appointments.sortedBy { it.date },
                        uploadNotice = viewModel.uploadNotice,
                        uploadText = viewModel.uploadText,
                        onUploadTextChanged = {
                            viewModel.uploadText = it
                            onActivity()
                        },
                        onAddAppointment = { date, provider, notes ->
                            viewModel.addAppointment(date, provider, notes)
                            onActivity()
                        },
                        onRemoveAppointment = {
                            viewModel.removeAppointment(it)
                            onActivity()
                        },
                        onLibraryUpload = { libraryLauncher.launch(arrayOf("image/*", "application/pdf")) },
                        onCameraScan = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                    )
                }
                composable(EobRoute.Analysis.route) {
                    AnalysisScreen(language, viewModel.records.sortedBy { it.serviceDateSortKey }, viewModel.selectedRecord) {
                        viewModel.selectRecord(it, profile)
                        onActivity()
                    }
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
                            if (viewModel.firebaseStatus.userId.isNotBlank()) firebaseRepository.saveProfile(viewModel.firebaseStatus.userId, it) {}
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

