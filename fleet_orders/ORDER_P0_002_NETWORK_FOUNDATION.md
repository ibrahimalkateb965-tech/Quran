# ORDER-P0-002 — Ktor Network Foundation (client only, no endpoint migration)

| Field | Value |
|---|---|
| Order ID | ORDER-P0-002 (rev. C, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11** — ORDER-P0-001 passed its gate; B-10 and B-13 are resolved for Claude Code CLI |
| rev. C delta | §0 ground truth re-verified from disk 2026-09-11 (unchanged). §4 gains Step 1b / 2b (Koin + coroutines-test in the catalog, because Antigravity may not edit Gradle). §7–§8 are historical. Package root confirmed: `com.aistudio.quranblind` (namespace `com.aistudio.quranblind.shared`). |
| Supersedes | `ORDER-P0-002` in `fleet_orders/IOS_KMP_PHASE0_ORDERS.md` (rev. A bundled `SecureStore` and the endpoint migration into one order — both are now split out) |
| Closes | B-05 (Retrofit/OkHttp/Moshi are JVM-only) — **foundation only** |
| Defers | endpoint + repository migration → **new ORDER-P0-004**; `SecureStore` → **new ORDER-P0-005** |

---

## 0. GROUND TRUTH (read from disk 2026-09-10)

`app/src/main/java/com/example/di/NetworkModule.kt` was read **in full**. It is the entire networking
configuration and it is not what the rev. A order assumed:

```kotlin
object NetworkModule {            // ← a plain Kotlin object.
    private val loggingInterceptor = …      // NOT @Module. NOT @InstallIn.
    private val retryInterceptor = …        // NOT injected by Hilt anywhere.
    private val okHttpClient = …
    private val moshi = …
    private val retrofit = …
    val quranApiService: QuranApiService by lazy { retrofit.create(...) }
}
```

**This changes the migration strategy materially.** The network layer is *not* in the Hilt graph — it is a
hand-wired global with `by lazy` singletons, reached statically. So the Ktor swap does **not** have to fight
Dagger, but it also means every call site references `NetworkModule.quranApiService` directly and those call
sites are unknown to me (see §7, B-13).

Four concrete portability defects, each read from the file:

| ID | Line | Defect | Why it blocks `commonMain` |
|---|---|---|---|
| **B-05a** | 43 | `Thread.sleep((1000 * tryCount).toLong())` | `Thread` does not exist in Kotlin/Native. Also blocks a thread inside a coroutine — wrong even on Android. |
| **B-05b** | 41 | `android.util.Log.w(...)` | Android-only API. |
| **B-05c** | 14, 62 | `com.example.BuildConfig.DEBUG`, `BuildConfig.BASE_URL` | `BuildConfig` is AGP-generated. It does not exist for iOS. |
| **B-05d** | 57–59 | `Moshi` + `KotlinJsonAdapterFactory` | Reflection-based; JVM-only. |

Behaviour worth preserving exactly: **3 attempts**, retry on `IOException` **or** HTTP `>= 500`, linear backoff
`1000ms × attempt`, and connect/read/write timeouts of **15 s** each.

**⚠️ What I could NOT read** (blocker B-13, §7): `data/network/QuranApiService.kt`,
`data/model/AlQuranCloudResponse.kt`, `data/repository/QuranRepository.kt`, `data/local/SessionPreferences.kt`.
They sit 8 folders below the connected folder; staging allows 7.

**This is exactly why the order is scoped the way it is.** I will not specify a migration of endpoint
signatures and response models I have not read — that is the failure mode that made rev. A of ORDER-P0-001
wrong. This order builds the pipe. ORDER-P0-004 moves the water, once I can see the plumbing.

---

## 1. OBJECTIVE

Create a working, tested Ktor `HttpClient` in `shared/src/commonMain` with a platform-specific engine
(OkHttp on Android, Darwin on iOS), preserving the retry and timeout semantics above, and replacing
`BuildConfig` with an injectable config object.

**Explicit non-goals:** touching `QuranApiService`, `QuranRepositoryImpl`, or any Moshi model; removing
Retrofit; changing any call site. At the end of this order Retrofit still serves 100% of production traffic
and the Ktor client is exercised only by tests. That is the intended, safe end state.

---

## 2. FILES YOU OWN

```
shared/src/commonMain/kotlin/com/aistudio/quranblind/network/HttpClientFactory.kt   CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/network/NetworkConfig.kt       CREATE
shared/src/commonMain/kotlin/com/aistudio/quranblind/network/Logger.kt              CREATE (expect)
shared/src/androidMain/kotlin/com/aistudio/quranblind/network/Logger.android.kt     CREATE (actual)
shared/src/iosMain/kotlin/com/aistudio/quranblind/network/Logger.ios.kt             CREATE (actual)
shared/src/commonTest/kotlin/com/aistudio/quranblind/network/HttpClientFactoryTest.kt CREATE
shared/build.gradle.kts                                                             MODIFY (deps only)
gradle/libs.versions.toml                                                           MODIFY (append only)
```

## 3. FORBIDDEN

- ❌ **Do not modify `app/src/main/java/com/example/di/NetworkModule.kt`.** Not one line. It serves production.
- ❌ Do not remove or downgrade Retrofit, OkHttp, Moshi, or the logging interceptor from `app/build.gradle.kts`.
- ❌ Do not touch `QuranApiService`, `AlQuranCloudResponse`, `QuranRepository*`, or any Moshi-annotated model.
- ❌ Do not add secure storage / `SecureStore` / `EncryptedSharedPreferences` work. That is ORDER-P0-005.
- ❌ Do not hardcode `https://api.alquran.cloud/v1/` anywhere except the documented default in `NetworkConfig`.
- ❌ **Do not run any test task**, and do not run `./gradlew build` (it attempts iOS targets, impossible on
  Windows — ORDER-P0-001 §2). Only the probes in Step 6.
- ❌ Do not add a Ktor **Logging** plugin at `LogLevel.BODY` for release configurations. Response bodies here
  are Quran text and reciter metadata, but body logging in a shipped build is a habit this project should not
  acquire. Mirror the existing DEBUG-gated behaviour exactly.

---

## 4. STEPS

### Step 1 — Version catalog

Append to `[versions]`:
```toml
ktor = "3.2.0"
```
Append to `[libraries]`:
```toml
ktor-client-core                = { group = "io.ktor", name = "ktor-client-core",                version.ref = "ktor" }
ktor-client-okhttp              = { group = "io.ktor", name = "ktor-client-okhttp",              version.ref = "ktor" }
ktor-client-darwin              = { group = "io.ktor", name = "ktor-client-darwin",              version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging             = { group = "io.ktor", name = "ktor-client-logging",             version.ref = "ktor" }
ktor-client-mock                = { group = "io.ktor", name = "ktor-client-mock",                version.ref = "ktor" }
```

> `3.2.0` is a **baseline, not a verdict.** Resolve the newest Ktor 3.x compatible with Kotlin 2.2.10 and
> report what you actually pinned. If it fails to resolve, report the failure — do not substitute silently.
> Ktor 2.x is **not** acceptable: its Darwin engine and `HttpTimeout` behaviour differ materially.

### Step 1b — Version catalog, for the orders that follow (rev. C)

Antigravity IDE owns the next two orders (domain core + Koin, SecureStore) and is **forbidden** from
editing Gradle files. Declare their dependencies now so they never have to. Append to `[versions]`:
```toml
koin = "4.1.0"
```
Append to `[libraries]`:
```toml
koin-core = { group = "io.insert-koin", name = "koin-core", version.ref = "koin" }
```
`kotlinx-coroutines-test` already exists in the catalog (`kotlinxCoroutinesTest = "1.10.2"`) — reuse it.
Same rule as Ktor: `4.1.0` is a baseline; resolve the newest Koin 4.x and report what you pinned.

### Step 2b — `shared/build.gradle.kts`, for the orders that follow (rev. C)

```kotlin
commonMain.dependencies {
    implementation(libs.koin.core)
}
commonTest.dependencies {
    implementation(libs.kotlinx.coroutines.test)
}
```
Replace the two-line comment `// androidMain / iosMain intentionally carry no dependencies yet. …` with
nothing — it is no longer true after this order.

### Step 2 — `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    // ...existing from P0-001 / P0-003...
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
}
androidMain.dependencies {
    implementation(libs.ktor.client.okhttp)
}
iosMain.dependencies {
    implementation(libs.ktor.client.darwin)
}
commonTest.dependencies {
    implementation(libs.ktor.client.mock)
}
```

### Step 3 — `NetworkConfig.kt` (replaces `BuildConfig`)

```kotlin
package com.aistudio.quranblind.network

data class NetworkConfig(
    val baseUrl: String = DEFAULT_BASE_URL,
    val isDebug: Boolean = false,
    val timeoutMillis: Long = 15_000,
    val maxRetries: Int = 3,
) {
    companion object {
        // Mirrors app/build.gradle.kts: buildConfigField BASE_URL
        const val DEFAULT_BASE_URL = "https://api.alquran.cloud/v1/"
    }
}
```
A plain data class, constructor-injected — not a global. `BuildConfig` was reachable from anywhere, which is
precisely what made it un-portable. Android will pass `BuildConfig.DEBUG` in at the composition root; iOS will
pass its own flag.

### Step 4 — `Logger.kt` (expect/actual, replaces `android.util.Log`)

`commonMain`:
```kotlin
package com.aistudio.quranblind.network

expect object PlatformLogger {
    fun warn(tag: String, message: String)
}
```
`androidMain` — delegate to `android.util.Log.w`.
`iosMain` — delegate to `NSLog` (`platform.Foundation.NSLog`).

### Step 5 — `HttpClientFactory.kt`

Requirements, each traceable to the read source:

1. `ContentNegotiation` with `kotlinx.serialization` `Json { ignoreUnknownKeys = true; isLenient = true }`.
   `ignoreUnknownKeys` is mandatory: the alquran.cloud response envelope carries fields the app does not model,
   and a strict parser turns an upstream additive change into a crash for a blind user mid-recitation.
2. `HttpTimeout` — `requestTimeoutMillis`, `connectTimeoutMillis`, `socketTimeoutMillis` all from
   `config.timeoutMillis` (15 s), matching the OkHttp builder.
3. `HttpRequestRetry`:
   - `maxRetries = config.maxRetries - 1` — ⚠️ **read this carefully.** The OkHttp loop makes **3 total
     attempts** (`while (tryCount < 3)`). Ktor's `maxRetries` counts *retries after* the first attempt. Setting
     `maxRetries = 3` would produce 4 attempts and change production behaviour. Get this off-by-one right.
   - retry on `IOException`-equivalent transport failures **and** on HTTP status `>= 500`.
   - `delayMillis { attempt -> 1000L * attempt }` — linear backoff via Ktor's suspending delay.
     **This is the B-05a fix:** `delay()`, never `Thread.sleep()`.
   - log each retry through `PlatformLogger.warn("HttpClientFactory", …)`, mirroring the existing
     `"Network failure. Retrying... (Attempt n/3)"` message.
4. `Logging` plugin at `LogLevel.BODY` **only** when `config.isDebug`, otherwise `LogLevel.NONE`.
   Mirrors `NetworkModule` lines 13–19 exactly.
5. Signature: `fun createHttpClient(config: NetworkConfig = NetworkConfig()): HttpClient`.
   No `expect/actual` on the factory itself — Ktor resolves the engine from the classpath, so one common
   implementation is correct. Do **not** write an `expect fun httpClientEngine()`; it is redundant complexity.

### Step 6 — Tests (`commonTest`) — write them, do not run them

Using `MockEngine`, cover:
- **T1** a 200 response deserialises through `ContentNegotiation`
- **T2** an unknown JSON field does not throw (proves `ignoreUnknownKeys`)
- **T3** two consecutive `500`s then a `200` → succeeds, and the engine was called **exactly 3 times**
- **T4** three consecutive `500`s → fails, and the engine was called **exactly 3 times, not 4**
  (this is the off-by-one guard from Step 5.3)
- **T5** `isDebug = false` produces `LogLevel.NONE`

T3 and T4 are the load-bearing tests. Retry semantics are the one thing being reimplemented rather than moved,
and an off-by-one there triples upstream load under an outage.

### Step 7 — Compile probes

```bash
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :app:assembleDebug
```
Nothing else. No `build`, no `test`, no `*Ios*` task.

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion | Evidence |
|---|---|---|
| A1 | `:shared:compileDebugKotlinAndroid` → BUILD SUCCESSFUL | verbatim output |
| A2 | `:app:assembleDebug` → BUILD SUCCESSFUL (**no Android regression**) | verbatim output |
| A3 | `git diff -- app/src` is **empty** | diff output |
| A4 | `app/build.gradle.kts` unchanged — Retrofit/OkHttp/Moshi all still declared | diff output |
| A5 | No `Thread.`, `android.`, `java.`, or `BuildConfig` reference under `shared/src/commonMain` | grep output |
| A6 | `PlatformLogger` has one `expect` and exactly two `actual`s (android + ios) | file listing |
| A7 | 5 tests written (T1–T5); **none executed by you** | file content |
| A8 | Ktor version actually pinned is reported and is 3.x | report §2 |
| A9 | Retry produces exactly 3 total attempts — the off-by-one is handled explicitly in code | file content + your written explanation |

---

## 6. REPORT-BACK

```
ORDER-P0-002 REPORT — OpenCode CLI

1. BASELINE           git HEAD, working tree state
2. VERSIONS PINNED    ktor = <actual>   (baseline 3.2.0; changed? why?)
3. FILES CREATED      path, line count
4. HttpClientFactory.kt   FULL SOURCE (verbatim)
5. RETRY MAPPING      OkHttp loop → Ktor HttpRequestRetry, attempt-count arithmetic shown explicitly.
                      State plainly: how many total HTTP calls occur when the server always returns 500?
6. PROBE OUTPUT       verbatim, both commands, warnings included
7. GREP               output proving A5
8. git diff --stat -- app/src        (must be empty)
9. DEVIATIONS         "NONE" only if literally nothing
10. BLOCKED ON
11. SELF-ASSESSMENT   A1–A9 PASS/FAIL
```

Report §5 in words, not just code. If your Ktor config yields 4 attempts rather than 3, I want to read your
own arithmetic saying so — that is far cheaper to catch here than in an outage.

---

## 7. ORDERS SPAWNED BY THIS REVIEW — *historical as of rev. C; B-06 is resolved (used), B-13 resolved*

**ORDER-P0-004 — Endpoint & repository migration.** Move `QuranApiService`, `AlQuranCloudResponse`, and the
`QuranRepositoryImpl` network path from Retrofit/Moshi to the Ktor client built here; then retire
`NetworkModule`. **Blocked on B-13** — I will not specify it until I can read those four files.

**ORDER-P0-005 — Secure storage `expect/actual`.** The previously logged blocker **B-06 needs re-verification
before this order exists**: `androidx.security.crypto` is declared in `app/build.gradle.kts`, but
`app/src/main/java/com/example/security/` is an **empty directory**, and so is `util/`. So the dependency may
be entirely unused, in which case B-06 is not a blocker at all but a stale dependency to delete. `SessionPreferences.kt`
is the file that settles it, and it is unreadable under B-13.

**Correction to the audit:** `IOS_KMP_READINESS_AUDIT.md` currently states B-06 as a confirmed P0 blocker on
`EncryptedSharedPreferences`. On the evidence above that claim is **unverified**. It must be downgraded to
"needs verification" rather than carried forward as fact.

---

## 8. OPERATIONAL BLOCKERS — *historical as of rev. C; both resolved for Claude Code CLI (see CURRENT_STATE §6)*

**B-10 (open)** — `device_bash` cannot mount the connected folder (`no Plan9 drive shares mounted`),
re-confirmed 2026-09-10. Claude Code cannot execute Gradle; G2 runs on verbatim console output pasted by Ibrahim.

**B-13 (NEW, open)** — staging is limited to **7 folders below the connected folder**;
`app/src/main/java/com/example/data/**` sits at 8. This is what forced P0-002 to be split, and it is what
blocks P0-004 outright.
**Fix:** connect `F:\AI PROJECTS\Blind App\app\src\main\java\com\example` as an additional folder in the
Claude desktop app. This is currently the highest-leverage unblock in the whole programme — it costs one
click and it converts two blind orders into reviewable ones.
