# ORDER-P0-009 REPORT — OpenCode CLI (non-composable cutover)

| Field | Value |
|---|---|
| Order ID | ORDER-P0-009 (rev. A, 2026-09-11) |
| Executed by | OpenCode CLI (`opencode/muse-spark-1.3-contributor-free`) |
| Scope executed | Steps 1–3 only, §2 files only |
| Date (UTC) | 2026-09-11 |

## BASELINE (`git status --short` before any edit)

```
M .agents/ACTIVE_CONTEXT_INJECTION.md
M .agents/HOOKS_GUIDE.xlsx
M .agents/MEMORY_STORE.md
M CLAUDE.md
M app/src/main/java/com/example/ui/components/player/AyahCard.kt
M fleet_config.json
M fleet_orders/CURRENT_STATE.md
M opencode.json
?? app/hs_err_pid24408.log
?? app/replay_pid24408.log
?? fleet_orders/ORDER_P0_009_MODEL_CUTOVER.md
?? hs_err_pid13492.log
?? hs_err_pid18096.log
?? hs_err_pid19008.log
?? hs_err_pid21684.log
?? hs_err_pid22308.log
?? remote_ios_dev_playbook_diagram.html
?? replay_pid18096.log
```

NOTE: `ui/components/player/AyahCard.kt` (reserved to Claude Code CLI) was already
` M` (unstaged-modified) in the baseline, before OpenCode made any change.

## STEP 1 — import rewrites performed (12 lines, in place, no re-sort, no blank-line changes)

- `app/src/main/java/com/example/domain/repository/QuranRepository.kt` — 2 lines (Ayah, Surah)
- `app/src/main/java/com/example/data/repository/QuranRepository.kt` — 3 lines (Ayah, Surah, SurahData)
- `app/src/main/java/com/example/data/local/AyahEntity.kt` — 1 line (Ayah)
- `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` — 3 lines (Ayah, Reciter, Surah)
- `app/src/test/java/com/example/ReciterOrderTest.kt` — 1 line (Reciter)
- `app/src/test/java/com/example/ReciterDefaultTest.kt` — 1 line (Reciter)
- `app/src/test/java/com/example/SurahIndexSheetTest.kt` — 1 line (Surah)

Mapping applied verbatim:
`import com.example.data.model.X → import com.aistudio.quranblind.domain.model.X`.
No other line in any file was changed.

## STEP 2 — deletions staged via `git rm` (no commit)

```
rm 'app/src/main/java/com/example/data/model/Ayah.kt'
rm 'app/src/main/java/com/example/data/model/Surah.kt'
rm 'app/src/main/java/com/example/data/model/Reciter.kt'
rm 'app/src/main/java/com/example/data/model/SurahData.kt'
```

`ls app/src/main/java/com/example/data/model/` afterwards:
`No such file or directory` (git removed the now-empty directory; no manual `rmdir` needed).

## FULL `git diff` of the seven MODIFY files (verbatim, unstaged)

```diff
diff --git a/app/src/main/java/com/example/data/local/AyahEntity.kt b/app/src/main/java/com/example/data/local/AyahEntity.kt
index 0841dd8..7cd6c33 100644
--- a/app/src/main/java/com/example/data/local/AyahEntity.kt
+++ b/app/src/main/java/com/example/data/local/AyahEntity.kt
@@ -2,7 +2,7 @@ package com.example.data.local
 
 import androidx.room.Entity
 import androidx.room.PrimaryKey
-import com.example.data.model.Ayah
+import com.aistudio.quranblind.domain.model.Ayah
 
 @Entity(tableName = "ayahs")
 data class AyahEntity(
diff --git a/app/src/main/java/com/example/data/repository/QuranRepository.kt b/app/src/main/java/com/example/data/repository/QuranRepository.kt
index b1005d7..02ca8e5 100644
--- a/app/src/main/java/com/example/data/repository/QuranRepository.kt
+++ b/app/src/main/java/com/example/data/repository/QuranRepository.kt
@@ -6,9 +6,9 @@ import com.example.data.local.AyahDao
 import com.example.data.local.AyahEntity
 import com.example.data.local.BookmarkDao
 import com.example.data.local.BookmarkEntity
-import com.example.data.model.Ayah
-import com.example.data.model.Surah
-import com.example.data.model.SurahData
+import com.aistudio.quranblind.domain.model.Ayah
+import com.aistudio.quranblind.domain.model.Surah
+import com.aistudio.quranblind.domain.model.SurahData
 import com.example.domain.repository.QuranRepository
 import dagger.hilt.android.qualifiers.ApplicationContext
 import kotlinx.coroutines.Dispatchers
diff --git a/app/src/main/java/com/example/domain/repository/QuranRepository.kt b/app/src/main/java/com/example/domain/repository/QuranRepository.kt
index f72a3cb..58b3678 100644
--- a/app/src/main/java/com/example/domain/repository/QuranRepository.kt
+++ b/app/src/main/java/com/example/domain/repository/QuranRepository.kt
@@ -1,8 +1,8 @@
 package com.example.domain.repository
 
 import com.example.data.local.BookmarkEntity
-import com.example.data.model.Ayah
-import com.example.data.model.Surah
+import com.aistudio.quranblind.domain.model.Ayah
+import com.aistudio.quranblind.domain.model.Surah
 import kotlinx.coroutines.flow.Flow
 
 interface QuranRepository {
diff --git a/app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt b/app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt
index df636e3..d9d9ee0 100644
--- a/app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt
+++ b/app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt
@@ -14,9 +14,9 @@ import com.example.service.QuranAudioService
 import com.example.accessibility.HapticFeedbackManager
 import com.example.accessibility.SpeechManager
 import com.example.data.local.BookmarkEntity
-import com.example.data.model.Ayah
-import com.example.data.model.Reciter
-import com.example.data.model.Surah
+import com.aistudio.quranblind.domain.model.Ayah
+import com.aistudio.quranblind.domain.model.Reciter
+import com.aistudio.quranblind.domain.model.Surah
 import com.example.domain.repository.QuranRepository
 import com.aistudio.quranblind.store.SessionState
 import com.aistudio.quranblind.store.SessionStore
diff --git a/app/src/test/java/com/example/ReciterDefaultTest.kt b/app/src/test/java/com/example/ReciterDefaultTest.kt
index 94d759f..4d9c930 100644
--- a/app/src/test/java/com/example/ReciterDefaultTest.kt
+++ b/app/src/test/java/com/example/ReciterDefaultTest.kt
@@ -1,6 +1,6 @@
 package com.example
 
-import com.example.data.model.Reciter
+import com.aistudio.quranblind.domain.model.Reciter
 import com.example.ui.viewmodel.SettingsUiState
 import org.junit.Assert.assertEquals
 import org.junit.Assert.assertTrue
diff --git a/app/src/test/java/com/example/ReciterOrderTest.kt b/app/src/test/java/com/example/ReciterOrderTest.kt
index ced9fad..ebd87d3 100644
--- a/app/src/test/java/com/example/ReciterOrderTest.kt
+++ b/app/src/test/java/com/example/ReciterOrderTest.kt
@@ -1,6 +1,6 @@
 package com.example
-import com.example.data.model.Reciter
+import com.aistudio.quranblind.domain.model.Reciter
 import org.junit.Assert.assertTrue
 import org.junit.Test
diff --git a/app/src/test/java/com/example/SurahIndexSheetTest.kt b/app/src/test/java/com/example/SurahIndexSheetTest.kt
index e415e5c..1a9ff6b 100644
--- a/app/src/test/java/com/example/SurahIndexSheetTest.kt
+++ b/app/src/test/java/com/example/SurahIndexSheetTest.kt
@@ -11,7 +11,7 @@ import org.junit.Rule
 import org.junit.Test
 import org.junit.runner.RunWith
 import com.example.ui.components.SurahIndexSheet
-import com.example.data.model.Surah
+import com.aistudio.quranblind.domain.model.Surah
 import androidx.test.espresso.Espresso
 import org.robolectric.annotation.Config
 import org.robolectric.RobolectricTestRunner
```

`git diff --numstat` for the seven MODIFY files (verbatim):
```
1	1	app/src/main/java/com/example/data/local/AyahEntity.kt
3	3	app/src/main/java/com/example/data/repository/QuranRepository.kt
2	2	app/src/main/java/com/example/domain/repository/QuranRepository.kt
3	3	app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt
1	1	app/src/test/java/com/example/ReciterDefaultTest.kt
1	1	app/src/test/java/com/example/ReciterOrderTest.kt
1	1	app/src/test/java/com/example/SurahIndexSheetTest.kt
```

## `git status --short` AFTER (verbatim)

```
M .agents/ACTIVE_CONTEXT_INJECTION.md
M .agents/HOOKS_GUIDE.xlsx
M .agents/MEMORY_STORE.md
M CLAUDE.md
M app/src/main/java/com/example/data/local/AyahEntity.kt
D  app/src/main/java/com/example/data/model/Ayah.kt
D  app/src/main/java/com/example/data/model/Reciter.kt
D  app/src/main/java/com/example/data/model/Surah.kt
D  app/src/main/java/com/example/data/model/SurahData.kt
M app/src/main/java/com/example/data/repository/QuranRepository.kt
M app/src/main/java/com/example/domain/repository/QuranRepository.kt
M app/src/main/java/com/example/ui/components/ReciterSelectorSheet.kt
M app/src/main/java/com/example/ui/components/SurahIndexSheet.kt
M app/src/main/java/com/example/ui/components/player/AyahCard.kt
M app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt
M app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt
M app/src/test/java/com/example/ReciterDefaultTest.kt
M app/src/test/java/com/example/ReciterOrderTest.kt
M app/src/test/java/com/example/SurahIndexSheetTest.kt
M fleet_config.json
M fleet_orders/CURRENT_STATE.md
M opencode.json
?? app/hs_err_pid24408.log
?? app/replay_pid24408.log
?? fleet_orders/ORDER_P0_009_MODEL_CUTOVER.md
?? hs_err_pid13492.log
?? hs_err_pid18096.log
?? hs_err_pid19008.log
?? hs_err_pid21684.log
?? hs_err_pid22308.log
?? remote_ios_dev_playbook_diagram.html
?? replay_pid18096.log
```

## STEP 3 GREP OUTPUT (verbatim)

Grep 1 — `grep -rn "com\.example\.data\.model" app/src`:
```
(no output; exit code 1 — zero matches)
```

Grep 2 — `grep -rn "com\.aistudio\.quranblind\.domain\.model" app/src`:
```
app/src/main/java/com/example/data/local/AyahEntity.kt:5:import com.aistudio.quranblind.domain.model.Ayah
app/src/main/java/com/example/data/repository/QuranRepository.kt:9:import com.aistudio.quranblind.domain.model.Ayah
app/src/main/java/com/example/data/repository/QuranRepository.kt:10:import com.aistudio.quranblind.domain.model.Surah
app/src/main/java/com/example/data/repository/QuranRepository.kt:11:import com.aistudio.quranblind.domain.model.SurahData
app/src/main/java/com/example/domain/repository/QuranRepository.kt:4:import com.aistudio.quranblind.domain.model.Ayah
app/src/main/java/com/example/domain/repository/QuranRepository.kt:5:import com.aistudio.quranblind.domain.model.Surah
app/src/main/java/com/example/ui/components/player/AyahCard.kt:34:import com.aistudio.quranblind.domain.model.Ayah
app/src/main/java/com/example/ui/components/ReciterSelectorSheet.kt:33:import com.aistudio.quranblind.domain.model.Reciter
app/src/main/java/com/example/ui/components/SurahIndexSheet.kt:48:import com.aistudio.quranblind.domain.model.Surah
app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt:128:import com.aistudio.quranblind.domain.model.Ayah
app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt:129:import com.aistudio.quranblind.domain.model.Reciter
app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt:17:import com.aistudio.quranblind.domain.model.Ayah
app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt:18:import com.aistudio.quranblind.domain.model.Reciter
app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt:19:import com.aistudio.quranblind.domain.model.Surah
app/src/test/java/com/example/ReciterDefaultTest.kt:3:import com.aistudio.quranblind.domain.model.Reciter
app/src/test/java/com/example/ReciterOrderTest.kt:3:import com.aistudio.quranblind.domain.model.Reciter
app/src/test/java/com/example/SurahIndexSheetTest.kt:14:import com.aistudio.quranblind.domain.model.Surah
```

## DEVIATIONS

1. **Reserved composable files were already migrated by another party before/during this
   execution — NOT by OpenCode.**
   - Baseline (taken before OpenCode's first edit) already showed
     `M app/src/main/java/com/example/ui/components/player/AyahCard.kt`.
   - By AFTER-status time, all four reserved files
     (`QuranPlayerScreen.kt` 2/2, `AyahCard.kt` 1/1, `ReciterSelectorSheet.kt` 1/1,
     `SurahIndexSheet.kt` 1/1 per `git diff --numstat`) carry exactly the 5 import-line
     changes the order reserves to Claude Code CLI.
   - OpenCode issued exactly 7 edit calls (the §2 MODIFY files) and one `git rm` (the 4
     model copies). OpenCode never opened, read, or edited any of the four reserved files;
     the reserved-file import lines above were observed only via the order-mandated Step 3
     grep output, which is read-only.
2. **Consequence for acceptance greps:** Grep 1 returns 0 lines (order §5/A3 anticipated 5,
   i.e. the reserved files still on the old package); Grep 2 returns 17 lines in 11 files
   instead of 12 lines in 7 files (the extra 5 are the Commander's own import lines at the
   exact line numbers §0.2 lists: 128/129, 34, 33, 48). Both differences are fully explained
   by deviation 1 and contain no OpenCode-caused content.
3. **Pre-existing worktree noise (untouched by OpenCode):** the `M` entries for
   `.agents/*`, `CLAUDE.md`, `fleet_config.json`, `fleet_orders/CURRENT_STATE.md`,
   `opencode.json` and the `??` log/artifact files were present in the baseline and were
   not modified by OpenCode. No `hs_err_pid*.log` / `replay_pid*.log` file was deleted.
4. No other deviation: line positions preserved, no import re-sort, no blank-line changes,
   no Gradle/build-file/config changes.

## BLOCKED ON

Nothing. Steps 1–3 complete. No commit, no push performed (as ordered).
The tree intentionally contains the Commander's in-progress reserved-file edits alongside
OpenCode's staged deletions; compilation/gating is the Commander's responsibility.

## SELF-ASSESSMENT A1–A7

| # | Criterion | Result |
|---|---|---|
| A1 | Status beyond pre-existing baseline shows exactly 7 ` M` (§2 MODIFY) + 4 `D ` (§2 DELETE) | **PASS for OpenCode's own changes** (7 ` M` import-only + 4 staged `D ` confirmed). AFTER-status additionally shows the 4 reserved composable ` M` entries caused by the Commander (deviation 1), which are outside OpenCode's ownership. |
| A2 | `git diff --numstat` (unstaged) exactly +2/−2, +3/−3, +1/−1, +3/−3, +1/−1, +1/−1, +1/−1 in §2 order | **PASS** (numstat rows above match per file; §2 order: domain/QuranRepository 2/2, data/QuranRepository 3/3, AyahEntity 1/1, QuranViewModel 3/3, ReciterOrderTest 1/1, ReciterDefaultTest 1/1, SurahIndexSheetTest 1/1) |
| A3 | Step 3 grep #1 → exactly 5 lines, all in reserved composables | **VARIANCE (explained)** — 0 lines, because the Commander had already migrated the reserved files (deviation 1). Zero old-package references remain, which is the strictly stronger end-state. |
| A4 | Step 3 grep #2 → exactly 12 lines in the seven MODIFY files | **VARIANCE (explained)** — 17 lines in 11 files; the 12 OpenCode lines are exact (files + line numbers match §0.2), plus the Commander's 5 reserved-file lines. |
| A5 | `app/src/main/java/com/example/data/model/` no longer exists | **PASS** (`ls` → No such file or directory) |
| A6 | `shared/**`, both `build.gradle.kts`, `proguard-rules.pro`, reserved composables untouched by OpenCode | **PASS for OpenCode** — `git status` shows zero entries under `shared/**`, no Gradle/proguard entries; OpenCode issued no read/edit/write against any of them or the four reserved files (tool history: 7 edits on §2 files only). Reserved-file modifications present in the worktree originate from the Commander (deviation 1). |
| A7 | No Gradle invocation, no test executed, no commit, no push | **PASS** (only `git status`, `git rm` (staged delete, uncommitted), `git diff`, `grep`, `ls` ran; worktree changes remain uncommitted as ordered) |

**FORBIDDEN-action compliance:** no `./gradlew` invocation; no test run; the four reserved
`@Composable` files never opened/edited; no file outside §2 written; no log file deleted;
no commit; no push; no Arabic typed (the only Arabic encountered was pre-existing file
content; the deleted model files were removed whole via `git rm`, never retyped).

---

## COMMANDER VERIFICATION + QUALITY GATE — Claude Code CLI, 2026-09-11 23:00–23:04

### Commander's reserved files (edited in parallel with OpenCode, disjoint file sets)

| File | + | − | Change |
|---|---|---|---|
| `ui/screens/QuranPlayerScreen.kt` | 2 | 2 | lines 128–129 imports `Ayah`, `Reciter` → `com.aistudio.quranblind.domain.model` |
| `ui/components/player/AyahCard.kt` | 1 | 1 | line 34 import `Ayah` |
| `ui/components/ReciterSelectorSheet.kt` | 1 | 1 | line 33 import `Reciter` |
| `ui/components/SurahIndexSheet.kt` | 1 | 1 | line 48 import `Surah` |

No `@Composable` body, semantics, or string touched. OpenCode's A3/A4 "variance" is exactly these four files
being already migrated when it ran its grep — expected, and it never opened them (report: 7 edits, all in §2).

### Whole-order diff check

`git diff -- app/src | grep '^[+-]' | sort | uniq -c` → exactly 17 `-import com.example.data.model.*` lines and
17 `+import com.aistudio.quranblind.domain.model.*` lines (6 `Ayah`, 5 `Reciter`, 5 `Surah`, 1 `SurahData`),
nothing else. Staged: 4 deletions (12 + 38 + 11 + 120 lines). `app/src/main/java/com/example/data/model/`
no longer exists. `grep -rn "com\.example\.data\.model" app/src` → empty.

### Gate (foreground, one Gradle invocation per call, B-17)

| Step | Command | Result | Evidence |
|---|---|---|---|
| 1 | `:app:testDebugUnitTest` | ✅ | `BUILD SUCCESSFUL in 1m 22s`; `compileDebugKotlin` + `compileDebugUnitTestKotlin` executed (not up-to-date); JUnit XML: **35 tests, 0 failures, 0 errors** across 11 classes (23:01); `ReciterDefaultTest`, `ReciterOrderTest`, `SurahIndexSheetTest`, `QuranRepositoryTest` green against the shared types |
| 2 | `:app:assembleDebug` | ✅ | `BUILD SUCCESSFUL in 6s`; `app-debug.apk` 27,111,913 B (23:02) |
| 3 | `:app:assembleRelease` | ✅ | `BUILD SUCCESSFUL in 1m 36s`; `minifyReleaseWithR8` executed, **no "Missing class"** lines; `app-release.apk` 5,867,725 B (23:03); `mapping.txt`: `domain.model.Ayah -> db`, `Reciter -> vk0`, `Surah -> ov0`, `SurahData -> pv0`, plus `Ayah$$serializer -> bb` / `Surah$$serializer -> mv0` (kept by the kotlinx-serialization consumer rules; +16,384 B vs P0-008's 5,851,341 B — acceptable, no keep rule added); **0** residual `com.example.data.model.*` entries |
| 4 | `:shared:testAndroidHostTest` | ✅ | UP-TO-DATE (no `shared/**` input changed); JUnit XML: **29 tests, 0 failures, 0 errors** across 5 classes |

### Devil's Advocate checks

- **Uthmanic integrity (§4.4):** `sanitizeUthmanicText` untouched (P0-008 state). `Ayah.textArabic` is a
  `String` field with the same name and position in both types; the repository → `AyahCard` pipeline is
  byte-identical, only the type's package changed. No text is re-typed anywhere (files deleted whole).
- **Blind-first (§4.1):** the four composable diffs are one import line each; `clearAndSetSemantics`,
  `contentDescription`, TalkBack hints, and haptics are untouched (verified by the whole-order diff check).
- **Reciter audio URLs:** shared `Reciter` is character-identical to the deleted copy (same `BASE_URL`,
  same 20 entries, same `DEFAULT_RECITER = husary_mujawwad`); `ReciterDefaultTest` / `ReciterOrderTest`
  now assert against the shared companion and pass.
- **Room:** `AyahEntity` columns and `toDomainModel()` body unchanged; only the return type's package moved.
  No schema version bump, no migration.
- **`@Serializable` on the shared `Ayah`/`Surah`:** `:app` compiles against them with
  `kotlinx-serialization` only on `:shared`'s `implementation` classpath — confirmed harmless by step 1's
  fresh `compileDebugKotlin`. `:app` never calls `serializer()`.
- **No data collection (§4.3):** no network, storage, or analytics code changed.
- **Test-count parity:** 35 in `:app`, 29 in `:shared` — unchanged.
- **Process:** OpenCode completed and wrote its own report for the first time this session — the
  no-compile-probe rule (CURRENT_STATE §1) removed the B-17 kill entirely. Its chat summary was in Arabic
  (CLAUDE.md §1 says orders/reports are English; the report file itself is English) — noted, no action.

**QUALITY GATE: PASS** — ORDER-P0-009 is accepted for commit.
