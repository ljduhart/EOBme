plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val googleServicesConfigFiles = listOf(
    file("google-services.json"),
    file("src/debug/google-services.json"),
    file("src/release/google-services.json")
)
val googleServicesConfigFile = googleServicesConfigFiles.firstOrNull { it.exists() && it.isFile }
val appPackageRegex = Regex(""""package_name"\s*:\s*"app\.eob\.me"""")
val hasValidGoogleServicesConfig = googleServicesConfigFile
    ?.let { configFile ->
        runCatching { appPackageRegex.containsMatchIn(configFile.readText()) }.getOrDefault(false)
    }
    ?: false
val isCiEnvironment = providers.environmentVariable("CI").orNull != null ||
    providers.environmentVariable("GITHUB_ACTIONS").orNull != null

if (hasValidGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "app.eob.me"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.eob.me"
        minSdk = 24
        targetSdk = 36
        versionCode = 144
        versionName = "15.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// RevenueCat 10.8.0 transitively requests billing 8.3.0; force the app's explicit 9.1.0
// so BillingRepository and Purchases.sharedInstance share one Play Billing runtime.
configurations.configureEach {
    resolutionStrategy.force("com.android.billingclient:billing:${libs.versions.billing.get()}")
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
}

tasks.register("verifyGoogleServicesJson") {
    group = "verification"
    description = "Fails release builds when google-services.json is missing or not configured for app.eob.me."
    doLast {
        val configFile = googleServicesConfigFile
        val misnamedConfigs = fileTree(projectDir) {
            include("google-services*.json")
            exclude("google-services.json")
            exclude("src/debug/google-services.json")
            exclude("src/release/google-services.json")
        }.files

        if (configFile == null) {
            val extraHint = if (misnamedConfigs.isNotEmpty()) {
                " Found misnamed Firebase config file(s): ${misnamedConfigs.joinToString { it.name }}. Rename the correct file to google-services.json."
            } else {
                ""
            }
            throw GradleException(
                "Missing Firebase config. Place google-services.json in the app/ module " +
                    "for package app.eob.me before building a release AAB.$extraHint"
            )
        }

        val configText = configFile.readText()
        val appPackageRegex = Regex(""""package_name"\s*:\s*"app\.eob\.me"""")
        if (!appPackageRegex.containsMatchIn(configText)) {
            throw GradleException(
                "Firebase config ${configFile.relativeTo(projectDir)} is not registered for package app.eob.me. " +
                    "Download the Android google-services.json for package app.eob.me from Firebase Console."
            )
        }
    }
}

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    if (hasValidGoogleServicesConfig) {
        dependsOn("verifyGoogleServicesJson")
    }
}

if (hasValidGoogleServicesConfig && isCiEnvironment) {
    afterEvaluate {
        tasks.matching { it.name.startsWith("uploadCrashlyticsMappingFile") }.configureEach {
            enabled = false
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.browser:browser:1.8.0")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.crashlytics)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.billing)
    implementation(libs.revenuecat.purchases)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}