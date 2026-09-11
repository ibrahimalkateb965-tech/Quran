# ORDER-P0-001 — Module Skeleton & Kotlin/Native Build Enablement

| Field | Value |
|---|---|
| Order ID | ORDER-P0-001 (rev. C) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **EXECUTED 2026-09-10 by Claude Code CLI** (not OpenCode — see report) · gate PENDING |
| Report | `fleet_orders/ORDER_P0_001_EXECUTION_REPORT.md` |
| Supersedes | `ORDER-P0-001` section inside `fleet_orders/IOS_KMP_PHASE0_ORDERS.md` (rev. A was written against unverified assumptions — **do not execute rev. A**) |
| Closes blockers | B-03 (no multiplatform module), B-09 (Gradle memory), B-11 (non-ASCII rootProject.name) |
| Raises | **B-14 (NEW, P0)** — AGP 9 dropped KMP compatibility with `com.android.library`; rev. B Step 4 was unbuildable |

---

## 0. GROUND TRUTH (verified 2026-09-10, read from disk — not assumed)

Rev. A of this order assumed Kotlin 1.9.20 / Compose 1.5.11 / `minSdk 26` / a `:composeApp` module.
**All four assumptions were wrong.** Verified reality:

| Item | Actual value | Source |
|---|---|---|
| Kotlin | **2.2.10** | `gradle/libs.versions.toml` |
| AGP | **9.2.1** | `gradle/libs.versions.toml` |
| Gradle wrapper | **9.4.1** | `gradle/wrapper/gradle-wrapper.properties` |
| KSP | 2.3.5 | `gradle/libs.versions.toml` |
| compileSdk | `release(36) { minorApiLevel = 1 }` (new AGP 9 DSL) | `app/build.gradle.kts` |
| minSdk / targetSdk | **24** / 36 | `app/build.gradle.kts` |
| Java level | 11 | `app/build.gradle.kts` |
| Modules | **`:app` only** | `settings.gradle.kts` |
| rootProject.name | `"القرآن للمكفوفين"` — **non-ASCII** | `settings.gradle.kts` |
| Android namespace | `"com.example"` — placeholder | `app/build.gradle.kts` |
| jvmargs | `-Xmx2048m`, MaxMetaspace 512m | `gradle.properties` |
| Kotlin compiler strategy | **`in-process`** | `gradle.properties` |
| parallel / workers.max | `false` / **2** | `gradle.properties` |
| configuration-cache | `true` | `gradle.properties` |
| Crash evidence | `hs_err_pid14964.log`, `hs_err_pid22496.log` in repo root | directory listing |

**Root-cause finding:** `kotlin.compiler.execution.strategy=in-process` runs the Kotlin compiler inside the
2 GB Gradle daemon. The two `hs_err_pid*.log` files are the JVM already dying under the current Android-only
build. Kotlin/Native linking needs several GB more. Step 1 is therefore not an optimisation — it is a
prerequisite for anything else in Phase 0 to run at all.

---

## 1. ARCHITECTURE DECISIONS TAKEN BY THIS ORDER

These close three previously-open items. Rationale is recorded so it can be challenged at the gate.

**AD-1 — Keep `:app` as-is. Do NOT rename it to `:androidApp`.**
The rename touches CI workflows, `.idea`, signing paths that reference `rootDir`, and Play Store upload
paths, for zero architectural gain. The shipping Android app is production revenue-of-trust; Phase 0 must
not put it at risk. Module renaming is deferred indefinitely.

**AD-2 — Create `:shared` ONLY. Do NOT create `:composeApp` yet.**
An empty `:composeApp` doubles the Kotlin/Native compile surface before a single line of domain code exists,
and Compose Multiplatform pins us to a CMP↔Kotlin compatibility pair we do not yet need. Compose
Multiplatform enters in Phase 1, after domain code compiles for iOS. (Reverses rev. A.)

**AD-3 — No Ktor and no Koin dependencies in this order.**
They are the payload of P0-002 and P0-003 respectively. P0-001 adds *only* the version-catalog plumbing
those orders will consume. Keeps this quality gate small enough to actually diagnose if it fails.

**AD-4 — iOS project is declared in `project.yml` (XcodeGen), not a hand-written `project.pbxproj`.**
A hand-written `pbxproj` cannot be validated on Windows and is unreviewable in diff. XcodeGen is declarative
text, reviewable on Windows, and regenerates the Xcode project deterministically on the macOS-15 CI runner.

---

## 2. ⚠️ HARD PLATFORM CONSTRAINT — READ BEFORE STARTING

**Kotlin/Native cannot compile iOS targets on Windows. Ever.**

This order *declares* the iOS targets so the module topology is correct and CI can build them. It does not,
and cannot, compile them on Ibrahim's machine. Consequences you must respect:

- **Never run `./gradlew build`** on this repo. It will attempt `linkDebugFrameworkIosArm64` and fail. Use
  the narrowly-scoped task list in §6 instead.
- `kotlin.native.ignoreDisabledTargets=true` (added in Step 1) makes configuration succeed on Windows.
- Any iOS *compilation* result is produced by the macOS-15 GitHub Actions runner, not locally.
- If you report "iOS build failed" from a Windows shell, that is expected behaviour, not a defect.

---

## 3. FILES YOU OWN (exclusive write access for this order)

```
gradle.properties                       MODIFY
gradle/libs.versions.toml               MODIFY  (add entries only — do not bump existing versions)
settings.gradle.kts                     MODIFY
shared/build.gradle.kts                 CREATE
shared/src/commonMain/kotlin/.gitkeep   CREATE
shared/src/androidMain/kotlin/.gitkeep  CREATE
shared/src/iosMain/kotlin/.gitkeep      CREATE
shared/src/commonTest/kotlin/.gitkeep   CREATE
app/build.gradle.kts                    MODIFY  (ONE line only — see Step 5)
iosApp/project.yml                      CREATE
iosApp/Sources/App.swift                CREATE
iosApp/Sources/ContentView.swift        CREATE
iosApp/README.md                        CREATE
```

## 4. FORBIDDEN

- ❌ Any file under `app/src/**` — no source, resource, or manifest edits.
- ❌ Bumping any **existing** version in `libs.versions.toml` (Kotlin, AGP, Room, Hilt, Media3, Retrofit…).
  Add new keys only. Version bumps are a separate order with their own regression gate.
- ❌ Removing Hilt, Retrofit, OkHttp, Moshi, or Room. They stay until their KMP replacements land and pass a gate.
- ❌ Changing `namespace = "com.example"` in `app/build.gradle.kts`. It is wrong, it is logged as I-09, and it
  is out of scope here because fixing it moves generated `BuildConfig`/`R` packages and touches source imports.
- ❌ Running `./gradlew build`, `./gradlew test`, `./gradlew connectedAndroidTest`, or any Roborazzi task.
  **Test execution is exclusively Claude Code's.** You run only the configuration/compile probes in §6.
- ❌ Deleting the `hs_err_pid*.log` files — they are diagnostic evidence for the gate.

---

## 5. STEPS

### Step 0 — Baseline capture (do this first)

Record and include in your report:
```
git rev-parse HEAD
git status --porcelain
```
Total physical RAM of the machine (`wmic ComputerSystem get TotalPhysicalMemory` or Task Manager).
**Step 1 depends on this number.**

---

### Step 1 — `gradle.properties`: memory + compiler strategy

Replace the `org.gradle.jvmargs`, `org.gradle.parallel`, `org.gradle.workers.max`, and
`kotlin.compiler.execution.strategy` lines. Keep every other line untouched.

**If total RAM ≥ 32 GB:**
```properties
org.gradle.jvmargs=-Xmx6g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=768m
org.gradle.parallel=true
org.gradle.workers.max=4
```

**If total RAM is 16 GB (or less):**
```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=768m -XX:+UseG1GC -Dfile.encoding=UTF-8
kotlin.daemon.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.workers.max=3
```

Then, in both cases, change the strategy and append the Native flags:
```properties
# Kotlin/Native requires an out-of-process compiler daemon.
# in-process + a 2 GB heap is what produced hs_err_pid14964.log / hs_err_pid22496.log.
kotlin.compiler.execution.strategy=daemon

# Allow Gradle configuration to succeed on a Windows host where iOS targets cannot be built.
kotlin.native.ignoreDisabledTargets=true
```

> **Report the exact heap values you chose and the RAM figure they were derived from.**

**Known risk — `org.gradle.configuration-cache=true` (already enabled).** Leave it enabled. If, and only if,
Step 6 fails with a configuration-cache serialization error, set it to `false`, re-run, and report **both**
the original error and the fact that you disabled it. Do not disable it pre-emptively.

---

### Step 2 — `gradle/libs.versions.toml`: add KMP plumbing

Under `[versions]`, **append**:
```toml
kotlinxSerialization = "1.9.0"
```

Under `[libraries]`, **append**:
```toml
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }
```

Under `[plugins]`, **append**:
```toml
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
android-library = { id = "com.android.library", version.ref = "agp" }
```

> `kotlinxSerialization = "1.9.0"` is a **baseline, not a verdict.** Resolve the newest version compatible with
> Kotlin 2.2.10 and report what you actually pinned. If 1.9.0 does not resolve, report the failure — do not
> silently substitute.

---

### Step 3 — `settings.gradle.kts`: ASCII root name + include `:shared`

Two changes only:

```kotlin
// BEFORE: rootProject.name = "القرآن للمكفوفين"
rootProject.name = "QuranBlind"

include(":app", ":shared")
```

> **Why (B-11):** the Gradle root project name propagates into Kotlin/Native framework naming, derived
> build-directory paths, and the Xcode `embedAndSignAppleFrameworkForXcode` build-phase script. A non-ASCII
> name on a Windows host whose repo path already contains two spaces (`F:\AI PROJECTS\Blind App`) is a
> reproducible source of Kotlin/Native and Xcode script failures. The **user-visible** app name is unaffected —
> it lives in `app/src/main/res/values*/strings.xml` and in the iOS `CFBundleDisplayName`, both untouched here.

---

> ### ⛔ REV. C CORRECTION — B-14 (read before Step 4)
>
> **Rev. B Step 4 below is WRONG and would have failed at configuration time.** It specified
> `com.android.library` + `androidTarget()`. Per the official Kotlin Multiplatform AGP 9
> migration guide: *"When used along with Android Gradle plugin 9.0 or newer, the Kotlin
> Multiplatform Gradle plugin stops being compatible with the `com.android.application` and
> the `com.android.library` plugins."* This project is on **AGP 9.2.1**.
>
> **What was actually written to disk** (and what any re-run must use):
> - plugin `com.android.kotlin.multiplatform.library` (versions in lockstep with `agp`),
>   catalog alias `android-kotlin-multiplatform-library` — **not** `android-library`
> - the Android target is configured **inside** `kotlin { android { … } }`; there is **no**
>   top-level `android { }` block (`androidLibrary { }` is the pre-8.12 spelling and is
>   deprecated as of AGP 9.1)
> - `compileSdk = 36` as a plain Int — the `release(36) { minorApiLevel = 1 }` form is
>   `com.android.application` DSL and is not accepted here
> - `minSdk = 24` inside that same block
> - Android host tests are **off by default** under this plugin; `withHostTestBuilder {}.configure {}`
>   opts in so P0-002 / P0-003 have somewhere for `commonTest` to run on the Android target
> - source sets are `androidMain` / `androidHostTest` (**not** `src/main` / `src/test`)
>
> See `shared/build.gradle.kts` on disk for the authoritative version.

### Step 4 — `shared/build.gradle.kts`: CREATE ⚠️ SUPERSEDED — see correction above

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        // androidMain / iosMain intentionally have no dependencies yet.
        // Ktor arrives in P0-002. Koin arrives in P0-003.
    }
}

android {
    namespace = "com.aistudio.quranblind.shared"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

Notes:
- `compileSdk { version = release(36) { minorApiLevel = 1 } }` mirrors the AGP 9 DSL already used in `:app`.
  If the KMP Android-library plugin rejects that block form, fall back to `compileSdk = 36` **and report the
  exact rejection message** — that divergence matters for the gate.
- `isStatic = true` avoids embedding a dynamic framework, which simplifies App Store submission.
- `baseName = "SharedKit"` is deliberately ASCII and distinct from the module name.

Also create the four source directories with a `.gitkeep` in each:
```
shared/src/commonMain/kotlin/.gitkeep
shared/src/androidMain/kotlin/.gitkeep
shared/src/iosMain/kotlin/.gitkeep
shared/src/commonTest/kotlin/.gitkeep
```

---

### Step 5 — `app/build.gradle.kts`: ONE line

Inside the existing `dependencies { … }` block, add as the first line:
```kotlin
  implementation(project(":shared"))
```
Change nothing else in this file. `:shared` is empty, so this must be a behaviour-neutral edit — that is
precisely what makes it a valid regression probe in §6.

---

### Step 6 — `iosApp/`: XcodeGen shell

`iosApp/project.yml`:
```yaml
name: QuranBlind
options:
  bundleIdPrefix: com.aistudio.quranblind
  deploymentTarget:
    iOS: "15.0"
  createIntermediateGroups: true

targets:
  QuranBlind:
    type: application
    platform: iOS
    sources:
      - path: Sources
    settings:
      base:
        PRODUCT_BUNDLE_IDENTIFIER: com.aistudio.quranblind.a11y
        SWIFT_VERSION: "5.0"
        FRAMEWORK_SEARCH_PATHS:
          - $(inherited)
          - $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
        OTHER_LDFLAGS:
          - $(inherited)
          - -framework
          - SharedKit
    info:
      path: Info.plist
      properties:
        CFBundleDisplayName: القرآن للمكفوفين
        UILaunchScreen: {}
        UISupportedInterfaceOrientations:
          - UIInterfaceOrientationPortrait
          - UIInterfaceOrientationLandscapeLeft
          - UIInterfaceOrientationLandscapeRight
        UIBackgroundModes:
          - audio
    preBuildScripts:
      - name: Build Kotlin SharedKit framework
        basedOnDependencyAnalysis: false
        script: |
          cd "$SRCROOT/.."
          ./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

> `UIBackgroundModes: [audio]` is present from day one deliberately: continuous Quran playback with the screen
> locked is a core requirement for a blind-first app, and retrofitting background audio after the audio layer
> is written is far more expensive than declaring it now.

`iosApp/Sources/App.swift`:
```swift
import SwiftUI

@main
struct QuranBlindApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

`iosApp/Sources/ContentView.swift`:
```swift
import SwiftUI

struct ContentView: View {
    var body: some View {
        Text("SharedKit wiring pending")
            .accessibilityLabel("قيد الإعداد")
    }
}
```

`iosApp/README.md` — state plainly that: the Xcode project is generated by `xcodegen generate` inside
`iosApp/`; it is never committed as a `.pbxproj`; and it can only be built on macOS (CI runner `macos-15`).
Add `iosApp/QuranBlind.xcodeproj/` to `.gitignore`.

**Do not import SharedKit in Swift yet.** The framework does not exist until CI produces it; importing it now
guarantees a red build on the first CI run for no informational gain.

---

### Step 7 — Verification probes (configuration + compile only)

Run **exactly** these, in order, and capture full verbatim output for each:

```bash
./gradlew --stop
./gradlew projects
./gradlew :shared:tasks --all          # discovery — see note
./gradlew :shared:compileKotlinAndroid
./gradlew :app:assembleDebug
```

> **Rev. C task-name correction.** Rev. B said `:shared:compileDebugKotlinAndroid`. That name
> comes from the old variant-based AGP+KMP integration. Under
> `com.android.kotlin.multiplatform.library` the Android target has a single `main` compilation,
> so the task is `compileKotlinAndroid`. This has **not** been verified against AGP 9.2.1 —
> run the `tasks --all` discovery first and use whatever Kotlin/Android compile task it lists.

Then check whether any **new** `hs_err_pid*.log` appeared in the repo root.

Do not run anything else. In particular do not run `build`, `test`, `check`, or any `*Ios*` task.

---

## 6. ACCEPTANCE CRITERIA

| # | Criterion | Evidence required |
|---|---|---|
| A1 | `./gradlew projects` lists `:app` **and** `:shared`, root project named `QuranBlind` | verbatim output |
| A2 | `./gradlew :shared:compileKotlinAndroid` → `BUILD SUCCESSFUL` (name per discovery) | verbatim output |
| A3 | `./gradlew :app:assembleDebug` → `BUILD SUCCESSFUL` (**no Android regression**) | verbatim output |
| A4 | **No new** `hs_err_pid*.log` in repo root after all probes | file listing before/after |
| A5 | `gradle.properties` shows `strategy=daemon` and the heap values justified by measured RAM | file diff + RAM figure |
| A6 | `libs.versions.toml` gained exactly 3 plugin + 2 library + 1 version key; **no existing version changed**. Third plugin is `android-kotlin-multiplatform-library`, not `android-library` (B-14) | file diff |
| A7 | `shared/src/{commonMain,androidMain,iosMain,commonTest}/kotlin/` all exist | directory listing |
| A8 | `iosApp/project.yml` + 2 Swift files exist; **no `.pbxproj` committed** | directory listing |
| A9 | `app/build.gradle.kts` diff is exactly **one** added line | file diff |

**A3 is the critical one.** If the shipping Android app stops assembling, this order has failed regardless of
how much iOS scaffolding exists. Report it as a failure rather than working around it.

---

## 7. REPORT-BACK (mandatory — TEMPLATE_02 §Report)

Reply with exactly these sections:

```
ORDER-P0-001 REPORT — OpenCode CLI

1. BASELINE
   git HEAD:            <sha>
   Working tree at start: <clean | list>
   Total physical RAM:  <GB>

2. FILES CHANGED
   <path>  <created|modified>  <+lines/-lines>
   ...

3. VERSIONS PINNED
   kotlinxSerialization = <actual>     (baseline was 1.9.0; changed? why?)

4. PROBE OUTPUT  (verbatim, unedited, including warnings)
   $ ./gradlew projects
   ...
   $ ./gradlew :shared:compileDebugKotlinAndroid
   ...
   $ ./gradlew :app:assembleDebug
   ...

5. hs_err_pid*.log
   Before: <list>
   After:  <list>

6. DEVIATIONS
   Anything you changed that this order did not authorise, and why.
   Write "NONE" only if literally nothing deviated.

7. BLOCKED ON
   Anything you could not complete. "NONE" if clean.

8. SELF-ASSESSMENT AGAINST A1–A9
   A1 PASS/FAIL  ... A9 PASS/FAIL
```

**Rules of engagement:**
- If a step fails, **stop and report**. Do not improvise a fix, do not bump versions to make an error go away,
  and do not disable checks. A clean failure report is worth more than a green build reached by unauthorised means.
- Paste output verbatim including warnings. Warnings from AGP 9 + KMP are frequently the leading indicator of
  the next blocker.
- Deviations declared honestly cost nothing. Deviations discovered at the gate fail the whole order.

---

## 8. QUALITY GATE (Claude Code — after your report)

I will run TEMPLATE_03 Devil's Advocate audit against A1–A9, then:

- **G0 compile** — verified from your verbatim probe output.
- **G2 unit tests** — `./gradlew :app:testDebugUnitTest`, my exclusive responsibility.
- ⚠️ **Operational blocker B-10 is still active**: `device_bash` cannot mount `F:\AI PROJECTS\Blind App`
  (`no Plan9 drive shares mounted`), re-confirmed 2026-09-10. I therefore **cannot execute Gradle myself**.
  Until B-10 is resolved, G2 runs by Ibrahim pasting verbatim console output, which I audit. I will not mark
  a gate PASS on summarised or paraphrased output.

**Unblocks on PASS:** ORDER-P0-002 (Ktor + serialization networking) and ORDER-P0-003 (domain core + Koin),
which may then proceed in parallel — they own disjoint paths under `shared/src/commonMain/`.

---

## 9. NEW BLOCKER RAISED BY THIS ORDER

**B-11 — Non-ASCII `rootProject.name` on a Windows host with spaces in the repo path.**
`rootProject.name = "القرآن للمكفوفين"` at `F:\AI PROJECTS\Blind App`. Kotlin/Native framework naming, derived
build paths, and the Xcode `embedAndSignAppleFrameworkForXcode` build-phase script all consume this value.
Severity P0 for iOS; zero impact on Android. Remediated in Step 3. Add to `IOS_KMP_READINESS_AUDIT.md`.

**I-09 (P1, deferred)** — `namespace = "com.example"` in `app/build.gradle.kts` while `applicationId` is
`com.aistudio.quranblind.a11y`. Not an iOS blocker; explicitly out of scope here.
