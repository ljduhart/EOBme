# AGENTS.md

## Cursor Cloud specific instructions

### Project overview

EOBme is an Android app (Kotlin + Jetpack Compose + Material 3) built with Gradle 9.4.1 and AGP 9.2.0. There is no backend — the app is fully on-device. The codebase has a single Gradle module (`:app`).

### Environment prerequisites

- **JDK 21** is used (JDK 17+ required by AGP 9.2.0). `JAVA_HOME` must point to `/usr/lib/jvm/java-21-openjdk-amd64`.
- **Android SDK** is installed at `/opt/android-sdk`. `ANDROID_HOME` must point there.
- `local.properties` (gitignored) must contain `sdk.dir=/opt/android-sdk`.

### Common commands

| Task | Command |
|---|---|
| Build debug APK | `./gradlew assembleDebug` |
| Run unit tests | `./gradlew test` |
| Run lint | `./gradlew lint` |
| Clean build | `./gradlew clean` |

All Gradle commands must be run from the repo root (`/workspace`). The Gradle wrapper auto-downloads Gradle 9.4.1 on first use.

### Gotchas

- `compileSdk` uses a `release(36) { minorApiLevel = 1 }` block, which auto-installs `platforms;android-36.1` on first build. This is normal AGP 9.x behavior.
- The `foojay-resolver-convention` plugin in `settings.gradle.kts` auto-provisions the JDK toolchain. If you see a toolchain download during the first build, this is expected.
- There is no Android emulator in the cloud VM, so `./gradlew connectedAndroidTest` (instrumented tests) will fail. Unit tests (`./gradlew test`) run on the host JVM and work fine.
- Lint report is generated at `app/build/reports/lint-results-debug.html`.
