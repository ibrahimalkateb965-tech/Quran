# ORDER-P0-005 — `SecureStore` expect/actual (Android EncryptedSharedPreferences / iOS Keychain)

| Field | Value |
|---|---|
| Order ID | ORDER-P0-005 (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **Antigravity IDE** (`Gemini 3.7 Flash High`) |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | ISSUED — **blocked until ORDER-P0-003b passes its gate** (sequential: same source-set roots) |
| Depends on | ORDER-P0-002 (`androidx.security.crypto` must be declared for `:shared` androidMain — see Step 0) |
| Closes | B-06 (resolved 2026-09-11: the dependency IS used by `SessionPreferences.kt`) |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

`app/src/main/java/com/example/data/local/SessionPreferences.kt` (read in full) is the only consumer of
`androidx.security.crypto` in the app. It stores three values — `last_reciter_id` (String),
`last_surah_id` (Int), `last_ayah_index` (Int) — in `EncryptedSharedPreferences` named
`"mueen_session_prefs"`, with `MasterKey.KeyScheme.AES256_GCM`, key scheme `AES256_SIV`, value scheme
`AES256_GCM`. `res/xml/backup_rules.xml` excludes `mueen_session_prefs.xml` from Auto Backup.

**Failure behaviour today is fail-closed:** if `EncryptedSharedPreferences.create` throws, the code clears
the prefs file and retries once; if that throws too, it throws `IllegalStateException("Critical Security
Error…")` and the app cannot start. The stored data is a resume position, not a secret.

**Gradle prerequisite (OpenCode, not you):** `:shared` androidMain does not yet declare
`libs.androidx.security.crypto`. Claude Code will have OpenCode add
`implementation(libs.androidx.security.crypto)` to `androidMain.dependencies` in `shared/build.gradle.kts`
**before** this order is dispatched. If you find it missing, **STOP and report** — do not edit Gradle.

---

## 1. OBJECTIVE

A `SecureStore` abstraction in `commonMain` with a working `actual` on both platforms, **fail-open** on
storage-backend failure, and `commonTest` coverage through an in-memory fake. `SessionPreferences.kt` in
`:app` is **not** changed — cut-over is a later order.

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
shared/src/commonMain/kotlin/com/aistudio/quranblind/store/SecureStore.kt                 CREATE (interface + expect factory)
shared/src/commonMain/kotlin/com/aistudio/quranblind/store/SessionStore.kt                CREATE (typed wrapper)
shared/src/androidMain/kotlin/com/aistudio/quranblind/store/SecureStore.android.kt        CREATE (actual)
shared/src/iosMain/kotlin/com/aistudio/quranblind/store/SecureStore.ios.kt                CREATE (actual)
shared/src/commonTest/kotlin/com/aistudio/quranblind/store/InMemorySecureStore.kt         CREATE
shared/src/commonTest/kotlin/com/aistudio/quranblind/store/SessionStoreTest.kt            CREATE
```

## 3. FILES YOU MUST NOT TOUCH

`app/**` (especially `SessionPreferences.kt`), every Gradle file, `network/**`, `domain/**` (P0-003b's),
`.github/**`, `CLAUDE.md`, `fleet_config.json`.

---

## 4. STEPS

### Step 1 — `SecureStore.kt` (commonMain)

```kotlin
package com.aistudio.quranblind.store

interface SecureStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun remove(key: String)
    fun clear()
}

/** Platform factory. [name] is the store namespace; Android maps it to the prefs file name. */
expect fun createSecureStore(name: String): SecureStore
```

`SecureStore` is an **interface**, not an `expect class`, so tests can substitute `InMemorySecureStore`
without platform code. Only the factory is `expect`.

### Step 2 — Android `actual` (`SecureStore.android.kt`)

`createSecureStore(name)` needs a `Context`. `androidMain` may not reach into Hilt, so expose an
initialisation hook:

```kotlin
object SecureStoreAndroid {
    @Volatile internal var appContext: android.content.Context? = null
    fun init(context: android.content.Context) { appContext = context.applicationContext }
}

actual fun createSecureStore(name: String): SecureStore {
    val ctx = checkNotNull(SecureStoreAndroid.appContext) { "SecureStoreAndroid.init(context) must be called first" }
    return AndroidSecureStore(ctx, name)
}
```

`AndroidSecureStore`:
- Try `EncryptedSharedPreferences.create(ctx, name, MasterKey(AES256_GCM), AES256_SIV, AES256_GCM)` —
  the exact scheme triple from `SessionPreferences.kt`.
- On **any** exception: log with `android.util.Log.w("SecureStore", …)`, then **fall back to plain**
  `ctx.getSharedPreferences(name, MODE_PRIVATE)`. Do **not** clear the file, do **not** retry, do **not**
  throw. This is the deliberate change from fail-closed to **fail-open** (see §5 rationale).
- All six interface methods delegate to the chosen `SharedPreferences` with `edit { … }` (`androidx.core`).

### Step 3 — iOS `actual` (`SecureStore.ios.kt`)

`IosSecureStore(service: String)` over the Keychain, `kSecClassGenericPassword`:
- `kSecAttrService = name`, `kSecAttrAccount = key`, `kSecAttrAccessible = kSecAttrAccessibleAfterFirstUnlock`
  (the resume position must be readable after a locked reboot when playback auto-resumes).
- `getString`: `SecItemCopyMatching` with `kSecReturnData`; `errSecItemNotFound` → `null`; any **other**
  `OSStatus` → log via `NSLog` and return `null` (fail-open).
- `putString`: `SecItemAdd`; on `errSecDuplicateItem` → `SecItemUpdate`. Other status → `NSLog`, swallow.
- `getInt`/`putInt`: store as the decimal string; parse with `toIntOrNull() ?: default`.
- `remove`: `SecItemDelete`; `clear`: `SecItemDelete` with only `kSecAttrService` (all accounts).
- Use `kotlinx.cinterop` + `platform.Security.*` / `platform.Foundation.*`. `memScoped`, `CFDictionary`
  via `CFDictionaryCreateMutable` — no third-party library.

You cannot compile this file on Windows. Write it carefully against the Kotlin/Native `platform.Security`
API and state in the report that it is **unverified locally**; CI (`macos-15`) is the first compiler it meets.

### Step 4 — `SessionStore.kt` (commonMain, typed wrapper)

```kotlin
package com.aistudio.quranblind.store

data class SessionState(val reciterId: String, val surahId: Int, val ayahIndex: Int)

class SessionStore(private val store: SecureStore) {
    fun save(state: SessionState) { /* 3 keys below */ }
    fun load(): SessionState? { /* null unless all three present & valid */ }
    fun clear() = store.clear()

    companion object {
        const val STORE_NAME = "mueen_session_prefs"     // same file name as today -> zero-migration cut-over
        const val KEY_LAST_RECITER_ID = "last_reciter_id"
        const val KEY_LAST_SURAH_ID = "last_surah_id"
        const val KEY_LAST_AYAH_INDEX = "last_ayah_index"
    }
}
```

Port the legacy reciter-id migration from `SessionPreferences.getSession()` **verbatim** into `load()`:
`"ar.husary"→"husary"`, `"ar.minshawi"→"minshawi"`, `"ar.abdulbasitmurattal"→"abdulbasit"`.
`load()` returns `null` when `reciterId == null || surahId == -1 || ayahIndex == -1` — identical semantics.

### Step 5 — Tests (commonTest, `kotlin.test` only)

`InMemorySecureStore` = a `MutableMap<String, String>`. `SessionStoreTest`:
- save then load round-trips all three fields
- load on empty store → `null`
- load with only two of three keys → `null`
- legacy `"ar.husary"` → `"husary"` (and the other two mappings)
- `clear()` empties the store
- `putInt`/`getInt` default path

### Step 6 — Compile probes (compile only)

```bash
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :app:assembleDebug
```

---

## 5. WHY FAIL-OPEN (rationale you must not "fix")

For a blind user, an app that refuses to launch because the Keystore hiccuped is a total loss of the
Quran, while the worst case of a plain-text resume position is that someone with the unlocked phone
learns which surah was last played. The encryption stays (parity with today, and it costs nothing when it
works); the crash goes. Claude Code will reject any actual that throws on backend failure.

---

## 6. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | `:shared:compileDebugKotlinAndroid` BUILD SUCCESSFUL (Android actual compiles) |
| A2 | `:app:assembleDebug` BUILD SUCCESSFUL; `git diff --stat -- app/` empty |
| A3 | Android actual never throws from `createSecureStore` once `init` was called — Claude Code reads the catch block |
| A4 | iOS actual: `errSecItemNotFound` → `null`, everything else logged and swallowed; report states it is locally unverified |
| A5 | `SessionStore` key names and `STORE_NAME` are byte-identical to `SessionPreferences.kt` |
| A6 | Legacy id migration ported verbatim, covered by a test |
| A7 | No `android.`/`java.` under `commonMain`/`commonTest`; no test executed by you |

## 7. REPORT-BACK

`fleet_orders/reports/ORDER_P0_005_REPORT_ANTIGRAVITY.md`, sections: BASELINE, FILES CREATED,
`SecureStore.android.kt` FULL SOURCE, `SecureStore.ios.kt` FULL SOURCE, FAIL-OPEN PATHS (one line per
catch/status branch), PROBE OUTPUT verbatim, `git diff --stat -- app/ gradle/`, DEVIATIONS, BLOCKED ON,
SELF-ASSESSMENT A1–A7.

FORBIDDEN: running any test; Gradle edits; touching `app/`; git commit/push; Arabic outside data string
literals. STOP after reporting.
