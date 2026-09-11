# Fleet Orders — IOS-KMP-PHASE-0
**Issued by:** Claude Code CLI (Opus 5) · **Date:** 2026-09-10
**Protocol:** Template 02 — Task Delegation Protocol
**Language:** English only, in every terminal, diff and report.

> **STANDING RULE FOR ALL DELEGATES**
> You may compile. You may **not** test. `./gradlew test`, `allTests`,
> `iosSimulatorArm64Test`, `connectedAndroidTest` and `maestro test` are the
> exclusive right of Claude Code CLI. Running any of them voids your diff.
> Stop and report after your order. Do not chain into the next one.

---

### ORDER-P0-001
ASSIGNED TO : opencode-cli
MODEL       : Meta Muse Spark 1.3
PRIORITY    : P0
DEPENDS ON  : none

OBJECTIVE
  Convert the single-module Android project into a Kotlin Multiplatform structure
  that compiles for iosSimulatorArm64, with no source code migrated yet.

CONTEXT
  Current state: one module `:app` applying `com.android.application`.
  AGP 9.2.1, Kotlin 2.2.10, Compose BOM 2025.01.00.
  Target topology: `:shared`, `:composeApp`, `:androidApp`, `iosApp/`.
  The existing `:app` module stays untouched and buildable for the whole of Phase 0.
  It is renamed and dismantled only in Phase 1, after the new skeleton is green.

FILES YOU OWN FOR THIS ORDER
  - settings.gradle.kts
  - build.gradle.kts
  - gradle.properties
  - gradle/libs.versions.toml
  - shared/build.gradle.kts            (new)
  - composeApp/build.gradle.kts        (new)
  - androidApp/build.gradle.kts        (new)
  - iosApp/**                          (new)

FILES YOU MUST NOT TOUCH
  - app/**                             (the existing Android module — leave it building)
  - CLAUDE.md, fleet_config.json, fleet_templates/**, fleet_orders/**, docs/**

STEPS
  1. Look up the official Compose Multiplatform / Kotlin compatibility matrix and pin an
     exact CMP version that is verified against Kotlin 2.2.10 and AGP 9.2.1.
     If no verified combination exists, STOP and report the conflict. Do not guess.
  2. Add to `gradle/libs.versions.toml`:
       org.jetbrains.kotlin.multiplatform, org.jetbrains.compose,
       org.jetbrains.kotlin.plugin.serialization, org.jetbrains.compose.compiler,
       ktor-client-core / -okhttp / -darwin / -content-negotiation,
       ktor-serialization-kotlinx-json, kotlinx-serialization-json,
       kotlinx-datetime, koin-core, koin-android, koin-compose-viewmodel.
     Do NOT remove any existing entry. Additive only.
  3. Create `:shared` with targets androidTarget, iosArm64, iosSimulatorArm64, iosX64,
     exporting an XCFramework named `Shared`. Source sets: commonMain, commonTest,
     androidMain, iosMain. Add ktor + serialization + koin-core to commonMain,
     ktor-client-okhttp to androidMain, ktor-client-darwin to iosMain.
  4. Create `:composeApp` applying the Compose Multiplatform plugin, depending on `:shared`.
     Empty `App()` composable placeholder only — no UI logic.
  5. Create `:androidApp` applying com.android.application with
     namespace = "com.aistudio.quranblind.a11y" and
     applicationId = "com.aistudio.quranblind.a11y" (the com.example namespace ends here),
     minSdk 24, targetSdk 36. Carry over both signingConfigs verbatim from
     `app/build.gradle.kts`. Do not print, echo or log any keystore value.
  6. Create `iosApp/` Xcode project, bundle id `com.aistudio.quranblind.a11y`,
     consuming the `Shared` XCFramework. Add to `Info.plist`:
       UIBackgroundModes = [audio]
       NSMicrophoneUsageDescription
       NSSpeechRecognitionUsageDescription
       ITSAppUsesNonExemptEncryption = false
     Arabic-facing strings in these keys are user-visible; keep the keys and structure English.
  7. In `gradle.properties`: raise org.gradle.jvmargs to -Xmx6144m,
     set MaxMetaspaceSize=1024m, raise org.gradle.workers.max to 4,
     and REMOVE kotlin.compiler.execution.strategy=in-process.
     Rationale: two hs_err_pid*.log crash dumps already exist in the project root;
     Kotlin/Native linking is heavier than the current config survives.
  8. Delete `hs_err_pid14964.log` and `hs_err_pid22496.log` from the project root.

ACCEPTANCE CRITERIA
  [ ] `./gradlew projects` lists :shared, :composeApp, :androidApp and the legacy :app
  [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` succeeds
  [ ] `./gradlew :shared:assembleSharedXCFramework` produces an XCFramework
  [ ] `./gradlew :app:assembleDebug` still succeeds — the legacy module is not broken
  [ ] No credential value appears in any diff or log
  [ ] The pinned CMP version is quoted with its source URL in your report

BUILD VERIFICATION YOU MAY RUN
  ./gradlew projects
  ./gradlew :shared:compileKotlinIosSimulatorArm64 --no-daemon
  ./gradlew :app:assembleDebug --no-daemon

FORBIDDEN
  - Any test command.
  - Editing app/** or any file under docs/, fleet_orders/, fleet_templates/.
  - Upgrading Kotlin or AGP. If CMP requires it, STOP and report — that is an ADR, not a task.

REPORT BACK
  Unified diff, verbatim build output, the CMP version chosen with its source, and any deviation.

---

### ORDER-P0-002
ASSIGNED TO : opencode-cli
MODEL       : Meta Muse Spark 1.3
PRIORITY    : P0
DEPENDS ON  : ORDER-P0-001

OBJECTIVE
  Provide a multiplatform network + persistence foundation in `:shared`,
  replacing Retrofit/OkHttp/Moshi and deferring the database decision.

CONTEXT
  Retrofit, OkHttp and Moshi are JVM-only and cannot compile for Kotlin/Native.
  Network surface is small: BASE_URL = https://api.alquran.cloud/v1/
  plus the static audio scheme https://everyayah.com/data/{reciter}/{SSS}{AAA}.mp3
  Per ADR-002, Room is NOT ported in Phase 0. Mutable state is last surah,
  last ayah and selected reciter — three values, not a relational schema.

FILES YOU OWN FOR THIS ORDER
  - shared/src/commonMain/kotlin/**/data/network/**
  - shared/src/commonMain/kotlin/**/data/store/**
  - shared/src/androidMain/kotlin/**/data/**
  - shared/src/iosMain/kotlin/**/data/**
  - shared/build.gradle.kts

FILES YOU MUST NOT TOUCH
  - app/**, composeApp/**, androidApp/**
  - any path under **/domain/** or **/ui/** or **/accessibility/**

STEPS
  1. Create a Ktor `HttpClient` factory as `expect fun createHttpClient(): HttpClient`,
     actual on Android using the OkHttp engine, actual on iOS using the Darwin engine.
     Install ContentNegotiation with kotlinx-serialization Json
     (ignoreUnknownKeys = true, isLenient = false).
  2. Port the API surface currently defined via Retrofit interfaces into suspend
     functions on a `QuranApi` class in commonMain. Same endpoints, same shapes.
     Annotate DTOs with @Serializable. No behaviour change.
  3. Build the audio URL with a single pure function
     `fun ayahAudioUrl(reciter: String, surah: Int, ayah: Int): String`
     producing zero-padded {SSS}{AAA}. Put it in commonMain. Add no test — Claude Code writes it.
  4. Define `expect interface SecureStore { fun getString(key: String): String?;
     fun putString(key: String, value: String); fun remove(key: String) }`.
     Android actual: EncryptedSharedPreferences + MasterKey AES256 (as today).
     iOS actual: Keychain via kSecClassGenericPassword, accessible
     kSecAttrAccessibleAfterFirstUnlock so the last-read position survives a locked boot.
  5. Add NO database dependency. If you believe a database is required, STOP and report — ADR-002 is open.

ACCEPTANCE CRITERIA
  [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` succeeds
  [ ] `./gradlew :shared:compileDebugKotlinAndroid` succeeds
  [ ] No Retrofit, OkHttp-as-a-client, or Moshi import exists anywhere in :shared
  [ ] SecureStore has a working actual on BOTH platforms — no TODO(), no stub throwing
  [ ] ayahAudioUrl(  "Husary_128kbps", 2, 5 ) shape is documented in the report

BUILD VERIFICATION YOU MAY RUN
  ./gradlew :shared:compileKotlinIosSimulatorArm64 --no-daemon
  ./gradlew :shared:compileDebugKotlinAndroid --no-daemon

FORBIDDEN
  - Any test command.
  - Adding Room, SQLDelight or any persistence library while ADR-002 is open.
  - Touching domain, UI or accessibility code.

REPORT BACK
  Unified diff, verbatim build output, and an explicit statement of what SecureStore
  does on iOS when the Keychain item is missing versus when it is inaccessible.

---

### ORDER-P0-003
ASSIGNED TO : antigravity-ide
MODEL       : Gemini 3.7 Flash High
PRIORITY    : P0
DEPENDS ON  : ORDER-P0-001

OBJECTIVE
  Move the platform-independent domain core into `shared/commonMain` and replace
  Hilt with Koin, without importing a single Android API into common code.

CONTEXT
  `VoiceCommandParser.kt` and `sanitizeUthmanicText` are pure Kotlin and are the
  highest-value, lowest-risk code in the repository. They move first, unchanged.
  Hilt (hilt-android 2.60.1) is Android/JVM-only and cannot cross to iOS.
  Everything annotated @HiltViewModel / @AndroidEntryPoint / @Module must become Koin.

FILES YOU OWN FOR THIS ORDER
  - shared/src/commonMain/kotlin/**/domain/**
  - shared/src/commonMain/kotlin/**/di/**
  - shared/src/androidMain/kotlin/**/di/**
  - shared/src/iosMain/kotlin/**/di/**

FILES YOU MUST NOT TOUCH
  - **/build.gradle.kts, gradle/libs.versions.toml, gradle.properties
  - .github/workflows/**
  - shared/src/*/kotlin/**/data/network/**  (owned by opencode-cli under ORDER-P0-002)
  - app/**  (leave the legacy module intact)

STEPS
  1. COPY (do not move, do not edit) `VoiceCommandParser.kt` into
     `shared/src/commonMain/kotlin/.../domain/voice/`. Remove only Android imports.
     If removing an import forces a logic change, STOP and report it — the Arabic
     normalisation rules for hamza and ta marbuta must not drift.
  2. COPY `sanitizeUthmanicText` and its helpers into
     `shared/src/commonMain/kotlin/.../domain/text/`, byte-for-byte on the logic.
     The uni06DF handling is a known, hard-won fix. Do not "improve" it.
  3. Port domain models and the `QuranRepository` interface into commonMain.
     Verify there is not one android.* import left. Replace any java.time usage
     with kotlinx-datetime.
  4. Create a Koin module `sharedModule` in commonMain declaring the repository
     and use-case bindings that AppModule / NetworkModule declare today.
     Add `initKoin()` in commonMain, an Android initializer, and an iOS
     `initKoinIos()` callable from Swift.
  5. Do NOT port ViewModels or any Compose code in this order. UI is Phase 3.
  6. Write NO tests. Claude Code authors and runs every test.

ACCEPTANCE CRITERIA
  [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` succeeds
  [ ] `grep -r "android\." shared/src/commonMain/` returns nothing
  [ ] `grep -r "dagger\|hilt\|javax.inject" shared/src/commonMain/` returns nothing
  [ ] VoiceCommandParser logic is byte-identical apart from imports — prove it with a diff
  [ ] sanitizeUthmanicText logic is byte-identical apart from imports — prove it with a diff
  [ ] The legacy `:app` module still builds

BUILD VERIFICATION YOU MAY RUN
  ./gradlew :shared:compileKotlinIosSimulatorArm64 --no-daemon

FORBIDDEN
  - Any test command.
  - Editing any Gradle file. If you need a dependency, STOP and report; opencode-cli owns build files.
  - Altering Arabic normalisation or Uthmanic sanitisation logic in any way.
  - Writing Arabic in code, comments or commit messages.

REPORT BACK
  Unified diff, verbatim build output, both byte-identity diffs, and the full Koin module definition.

---

## QUEUED — Phase 1 (not yet issued)

| Order | Owner | Content | Blocked by |
| :--- | :--- | :--- | :--- |
| ORDER-P1-004 | claude-code-cli | Author `commonTest` for VoiceCommandParser, sanitizeUthmanicText, ayahAudioUrl; run G2 | P0-002, P0-003 |
| ORDER-P1-005 | antigravity-ide | ADR-001 spike: Uthmanic rendering on a real iOS device, pixel-compared to Android | P0-001 |
| ORDER-P1-006 | antigravity-ide | `expect class AudioEngine` — ExoPlayer / AVPlayer + AVAudioSession + interruption handling | P1-004 |
| ORDER-P1-007 | opencode-cli | Rewrite `.github/workflows/ios_build.yml` for KMP; preserve playbook §4–6 signing plumbing | P0-001 |

**ORDER-P1-005 is the real gate on this project.** If CoreText mispositions Uthmanic
diacritics, every downstream UI decision changes. It runs early on purpose.
