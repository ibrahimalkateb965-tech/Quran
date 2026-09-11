# ORDER-P0-004 — Retire the dead Retrofit/Moshi/OkHttp network path from `:app`

| Field | Value |
|---|---|
| Order ID | ORDER-P0-004 (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — Gradle/catalog/ProGuard are its domain; B-17 stands |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11** |
| Depends on | ORDER-P0-002 (PASS — Ktor in `:shared` is the only network stack from now on) |
| Decision | Ibrahim, 2026-09-11: **option A — retire only, no port** |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

The planned "migrate the network path to Ktor" premise was wrong. `QuranRepositoryImpl.getAyahs()`
reads `assets/quran/quran_uthmani_tanzil.json` and falls back to Room; it never touches the network.
A `grep` over all of `app/src` (main + test) finds **zero consumers** of `QuranApiService`,
`NetworkModule.quranApiService`, `AlQuranCloudResponse`, `SurahApiResponseData` or `AyahApiResponse`
outside the three files that define them. `BuildConfig.BASE_URL` (`https://api.alquran.cloud/v1/`) is
consumed only by `NetworkModule`. Audio URLs come from `Reciter.BASE_URL` (`verse.mp3quran.net`) and go
straight to ExoPlayer — no API call. Nothing else in the repository references `libs.okhttp`,
`libs.retrofit`, `libs.moshi.*`, `libs.converter.moshi` or `libs.logging.interceptor` (`ktor-client-okhttp`
in `:shared` brings its own OkHttp transitively and is unaffected).

So this is a **deletion order**, not a migration. It is the first order that touches `app/`, which is why
the gate is a full regression run (`:app` unit tests + `assembleDebug` + `assembleRelease` because R8 keep
rules change).

---

## 1. OBJECTIVE

Remove the dead Retrofit + Moshi + OkHttp path and everything that exists only to support it, leaving
`:app` behaviour byte-identical for the user. No file outside the list in §2 changes.

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
app/src/main/java/com/example/data/network/QuranApiService.kt        DELETE (and the now-empty network/ directory)
app/src/main/java/com/example/data/model/AlQuranCloudResponse.kt     DELETE
app/src/main/java/com/example/di/NetworkModule.kt                    DELETE
app/build.gradle.kts                                                  MODIFY — Steps 2 and 3 only
app/proguard-rules.pro                                                MODIFY — Step 4 only
gradle/libs.versions.toml                                             MODIFY — Step 5 only
```

## 3. FILES YOU MUST NOT TOUCH

Every other file under `app/` (especially `QuranRepository.kt`, `AppModule.kt`, `Reciter.kt`,
`AndroidManifest.xml` — the `INTERNET` permission stays, audio streams), `shared/**`, `iosApp/**`,
`.github/**`, `CLAUDE.md`, `fleet_config.json`, `settings.gradle.kts`, `build.gradle.kts` (root).
Do not delete any `hs_err_pid*.log` / `replay_pid*.log` — they are evidence for B-17.

---

## 4. STEPS

### Step 1 — Delete the three source files

`git rm` (not plain delete) `QuranApiService.kt`, `AlQuranCloudResponse.kt`, `NetworkModule.kt`.
Remove the `app/src/main/java/com/example/data/network/` directory if it is empty afterwards.

### Step 2 — `app/build.gradle.kts` dependencies

Remove exactly these six lines from the `dependencies { }` block, nothing else:

```kotlin
  implementation(libs.converter.moshi)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  "ksp"(libs.moshi.kotlin.codegen)
```

Keep `alias(libs.plugins.google.devtools.ksp)` and the other two `"ksp"(...)` lines — Hilt and Room
still need KSP.

### Step 3 — `app/build.gradle.kts` build config

Remove the single line
`buildConfigField("String", "BASE_URL", "\"https://api.alquran.cloud/v1/\"")` from `defaultConfig`.
Leave `buildFeatures { buildConfig = true }` (or whatever is there) untouched — `BuildConfig.DEBUG` is
still used elsewhere.

### Step 4 — `app/proguard-rules.pro`

Delete section `# 5. Moshi & Retrofit Serialization` in full — the comment line plus these rules:

```
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.squareup.moshi.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
```

Do **not** renumber the remaining sections and do **not** touch section 6 (`-keep class com.example.data.** { *; }`
stays — Room entities live there).

### Step 5 — `gradle/libs.versions.toml`

Remove these six `[libraries]` aliases: `retrofit`, `converter-moshi`, `logging-interceptor`, `okhttp`,
`moshi-kotlin`, `moshi-kotlin-codegen`. Then remove their now-orphaned `[versions]` keys: `retrofit`,
`converterMoshi`, `loggingInterceptor`, `okhttp`, `moshiKotlin`, `moshiKotlinCodegen`. Before deleting each
version key, `grep` the catalog to confirm no other alias references it. Do not reorder or reformat anything
else in the file.

### Step 6 — Compile probes (compile only — do NOT run tests, do NOT run `assembleDebug`)

```bash
./gradlew :app:compileDebugKotlin --offline
./gradlew :app:compileDebugUnitTestKotlin --offline
```

If `--offline` fails because a dependency is missing, rerun once without it and say so in the report.
`assembleDebug` / `assembleRelease` / `testDebugUnitTest` are Claude Code's at the gate (B-17: one JVM at a time).

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | Both compile probes BUILD SUCCESSFUL |
| A2 | `git status --short` shows exactly: 3 `D` files, `M app/build.gradle.kts`, `M app/proguard-rules.pro`, `M gradle/libs.versions.toml` (plus whatever was already dirty before you started — list it in BASELINE) |
| A3 | `grep -rni "retrofit\|moshi\|alquran.cloud" app/src app/build.gradle.kts app/proguard-rules.pro gradle/libs.versions.toml` returns **nothing**; `grep -rn "BuildConfig.BASE_URL" app/src` returns nothing (`Reciter.BASE_URL` is a different, private constant and stays) |
| A4 | `git diff app/build.gradle.kts` is exactly 7 removed lines, 0 added; `git diff app/proguard-rules.pro` is exactly 13 removed lines (1 comment + 12 rule lines), 0 added |
| A5 | `git diff gradle/libs.versions.toml` is exactly 12 removed lines, 0 added |
| A6 | `AndroidManifest.xml`, `QuranRepository.kt`, `AppModule.kt`, `shared/**` unchanged (`git diff --stat` on them is empty) |
| A7 | No test executed by you; no commit, no push |

## 6. REPORT-BACK

`fleet_orders/reports/ORDER_P0_004_REPORT_OPENCODE.md`, sections: BASELINE (`git status --short` before
you start), FILES DELETED, FULL `git diff` of the three modified files (verbatim, not summarised), PROBE
OUTPUT verbatim (last 20 lines of each), A3 GREP OUTPUT verbatim, DEVIATIONS, BLOCKED ON, SELF-ASSESSMENT
A1–A7.

FORBIDDEN: running any test; `assembleDebug`/`assembleRelease`; touching any file outside §2; deleting
log files; git commit/push; Arabic outside data string literals. STOP after reporting.
