plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val googleServicesConfigFiles = listOf(
    file("google-services.json"),
    file("src/debug/google-services.json"),
    file("src/release/google-services.json")
)
val googleServicesConfigFile = googleServicesConfigFiles.firstOrNull { it.exists() }
val hasGoogleServicesConfig = googleServicesConfigFile != null

if (hasGoogleServicesConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "app.eob.me"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "app.eob.me"
        minSdk = 24
        targetSdk = 35
        versionCode = 52
        versionName = "6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
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
    dependsOn("verifyGoogleServicesJson")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation("com.android.billingclient:billing:8.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}