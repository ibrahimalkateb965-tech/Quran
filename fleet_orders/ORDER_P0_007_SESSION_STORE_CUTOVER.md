# ORDER-P0-007 — Cut `:app` over from `SessionPreferences` to shared `SessionStore`

| Field | Value |
|---|---|
| Order ID | ORDER-P0-007 (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — DI module + Application wiring; B-17 stands |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11** |
| Depends on | ORDER-P0-005 (PASS — `SessionStore`, `createSecureStore`, `SecureStoreAndroid.init`) |
| Decision | B-06 / P0-005: session storage is **fail-open** from now on (Ibrahim, 2026-09-11) |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

- `app/src/main/java/com/example/data/local/SessionPreferences.kt` (101 lines) wraps
  `EncryptedSharedPreferences` named `"mueen_session_prefs"` and exposes `saveSession(reciterId, surahId,
  ayahIndex)`, `getSession(): SessionState?`, `clearSession()`, plus a `SessionState` data class and a
  `getInstance(context)` singleton. It is **fail-closed** (throws `IllegalStateException` after one retry).
- **Consumers — exactly two:**
  - `app/src/main/java/com/example/di/AppModule.kt:44-46` — `provideSessionPreferences(@ApplicationContext
    context)` returning `SessionPreferences.getInstance(context)`.
  - `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` — constructor param
    `private val sessionPrefs: SessionPreferences` (line 78), one read `sessionPrefs.getSession()` (line 236),
    three writes `sessionPrefs.saveSession(reciterId = …, surahId = …, ayahIndex = …)` (lines 282, 499, 555).
    The ViewModel never names the `SessionState` type explicitly; `savedSession` is type-inferred.
- No test under `app/src/test` or `app/src/androidTest` references `SessionPreferences`, `SessionState`
  or constructs `QuranViewModel` directly.
- `:app` already depends on `:shared` (`app/build.gradle.kts:90`). `:shared` already provides
  `com.aistudio.quranblind.store.SessionStore` (same three key names, same `STORE_NAME =
  "mueen_session_prefs"`, same legacy reciter-id migration, verbatim) and
  `com.aistudio.quranblind.store.SecureStoreAndroid.init(context)` + `createSecureStore(name)`.
- `app/src/main/java/com/example/QuranBlindApp.kt` is a bare `@HiltAndroidApp class QuranBlindApp : Application()`
  with no `onCreate`.
- `res/xml/backup_rules.xml` excludes `mueen_session_prefs.xml` — unchanged by this order because the file
  name is identical.

Because file name, key names and value types are byte-identical, **existing users keep their resume position**
with no migration code.

---

## 1. OBJECTIVE

Replace `SessionPreferences` with the shared `SessionStore` in `:app`, delete the Android-only class, and
initialise `SecureStoreAndroid` from the `Application`. No behaviour change except the deliberate
fail-closed → fail-open switch already decided in P0-005.

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
app/src/main/java/com/example/QuranBlindApp.kt                     MODIFY — Step 1
app/src/main/java/com/example/di/AppModule.kt                       MODIFY — Step 2
app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt        MODIFY — Step 3 (5 lines + 1 import; nothing else)
app/src/main/java/com/example/data/local/SessionPreferences.kt      DELETE — Step 4
app/build.gradle.kts                                                MODIFY — Step 5 (one line)
```

## 3. FILES YOU MUST NOT TOUCH

Every other file. In particular: any `@Composable`, anything under `ui/components/**`, `ui/screens/**`,
`accessibility/**`, `service/**`; `res/**` (incl. `backup_rules.xml`); `shared/**`;
`gradle/libs.versions.toml`; `proguard-rules.pro`; `.github/**`; `CLAUDE.md`; `fleet_config.json`.
Do not delete any `hs_err_pid*.log` / `replay_pid*.log`.

---

## 4. STEPS

### Step 1 — `QuranBlindApp.kt`

```kotlin
package com.example

import android.app.Application
import com.aistudio.quranblind.store.SecureStoreAndroid
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuranBlindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureStoreAndroid.init(this)
    }
}
```

`init` must run before Hilt builds the first `QuranViewModel`; `Application.onCreate` is the only place
that is guaranteed earlier than every ViewModel, Service and Activity.

### Step 2 — `AppModule.kt`

Replace the provider

```kotlin
    @Provides
    @Singleton
    fun provideSessionPreferences(@ApplicationContext context: Context): SessionPreferences {
        return SessionPreferences.getInstance(context)
    }
```

with

```kotlin
    @Provides
    @Singleton
    fun provideSessionStore(): SessionStore {
        return SessionStore(createSecureStore(SessionStore.STORE_NAME))
    }
```

Imports: remove `com.example.data.local.SessionPreferences`; add `com.aistudio.quranblind.store.SessionStore`
and `com.aistudio.quranblind.store.createSecureStore`. `@ApplicationContext`/`Context` imports stay — other
providers use them. Nothing else in the file changes.

### Step 3 — `QuranViewModel.kt` (surgical)

1. Import: replace `import com.example.data.local.SessionPreferences` with
   `import com.aistudio.quranblind.store.SessionState` and `import com.aistudio.quranblind.store.SessionStore`
   (keep the file's import ordering style — the imports are not alphabetised, just put them where the old one was).
2. Constructor (line 78): `private val sessionPrefs: SessionPreferences` → `private val sessionStore: SessionStore`.
3. Line 236: `val savedSession = sessionPrefs.getSession()` → `val savedSession = sessionStore.load()`.
4. Lines 282, 499, 555 — each

   ```kotlin
   sessionPrefs.saveSession(
       reciterId = X,
       surahId = Y,
       ayahIndex = Z
   )
   ```
   becomes
   ```kotlin
   sessionStore.save(
       SessionState(
           reciterId = X,
           surahId = Y,
           ayahIndex = Z
       )
   )
   ```
   with `X`, `Y`, `Z` copied **verbatim** from the existing call. Do not touch any other line of this
   1,000-line file — the gate diffs it and rejects anything beyond these edits.

### Step 4 — Delete `SessionPreferences.kt`

`git rm app/src/main/java/com/example/data/local/SessionPreferences.kt`. Its `SessionState` goes with it;
the shared one has the same three fields in the same order.

### Step 5 — `app/build.gradle.kts`

Remove the single line `implementation(libs.androidx.security.crypto)` from `:app` — after Step 4 nothing in
`:app` imports `androidx.security.crypto`; `:shared`'s `androidMain` carries it. Do **not** touch
`proguard-rules.pro` section 7 (`-keep class androidx.security.crypto.**`) — the classes are still in the
release APK via `:shared` and the keep rule still applies.

### Step 6 — Compile probes (compile only; B-18 is resolved, `gradlew` is allowed again)

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:compileDebugUnitTestKotlin
```

Do NOT run `testDebugUnitTest`, `assembleDebug`, or `assembleRelease` — Claude Code runs them at the gate
(B-17: one JVM at a time). Do not pass `--offline` unless the first attempt fails on resolution.

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | Both compile probes BUILD SUCCESSFUL |
| A2 | `git status --short` beyond the pre-existing baseline shows exactly: `D SessionPreferences.kt`, `M QuranBlindApp.kt`, `M AppModule.kt`, `M QuranViewModel.kt`, `M app/build.gradle.kts` |
| A3 | `grep -rn "SessionPreferences\|sessionPrefs\|androidx.security.crypto" app/src` returns nothing |
| A4 | `git diff --numstat app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` is at most **+12 / −8** (import swap, constructor line, 1 read, 3 writes each +2 lines) |
| A5 | `git diff app/build.gradle.kts` is exactly 1 removed line, 0 added |
| A6 | `SecureStoreAndroid.init(this)` is the first statement after `super.onCreate()` in `QuranBlindApp` |
| A7 | No `@Composable`, `res/`, `shared/`, `proguard-rules.pro` change; no test executed; no commit/push |

## 6. REPORT-BACK

`fleet_orders/reports/ORDER_P0_007_REPORT_OPENCODE.md`, sections: BASELINE (`git status --short` before you
start), FULL `git diff` of the four modified files (verbatim), FILE DELETED, PROBE OUTPUT (last 20 lines
each), A3 GREP OUTPUT verbatim, DEVIATIONS, BLOCKED ON, SELF-ASSESSMENT A1–A7.

FORBIDDEN: running any test; `assembleDebug`/`assembleRelease`; touching any file outside §2; deleting log
files; git commit/push; Arabic outside data string literals (the existing Arabic `announce("…")` strings in
the ViewModel are data — leave them exactly as they are). STOP after reporting.
