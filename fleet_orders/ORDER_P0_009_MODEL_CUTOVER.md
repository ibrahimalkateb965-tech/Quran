# ORDER-P0-009 — Cut `:app` over to the shared `domain.model` types (delete `com.example.data.model`)

| Field | Value |
|---|---|
| Order ID | ORDER-P0-009 (rev. A, 2026-09-11) |
| Issued by | Claude Code CLI (Fleet Commander) |
| Assigned to | **OpenCode CLI** (`opencode/muse-spark-1.3-contributor-free`) — non-composable files only, see §2 |
| Protocol | TEMPLATE_02 — Task Delegation |
| Status | **DISPATCHED 2026-09-11** |
| Depends on | ORDER-P0-003b (PASS — `com.aistudio.quranblind.domain.model.{Ayah,Surah,Reciter,SurahData}` exist in `:shared` and are consumed by `shared/.../domain/repository/QuranRepository.kt`); ORDER-P0-008 (PASS — `:app` already imports from `com.aistudio.quranblind.*`) |
| Governing rules | CLAUDE.md §2 (OpenCode never touches `@Composable` files, never runs tests); §4.4 (Uthmanic text is data — never retype it) |

---

## 0. GROUND TRUTH (read from disk 2026-09-11 by Claude Code CLI)

### 0.1 The two copies are field-identical

| Type | `:app` copy (`app/src/main/java/com/example/data/model/`) | `:shared` copy (`shared/src/commonMain/kotlin/com/aistudio/quranblind/domain/model/`) | Difference |
|---|---|---|---|
| `Ayah` | 8 fields: `numberInSurah, globalNumber, textArabic, textTranslation="", audioUrl="", surahId, page=1, juz=1` | same 8 fields, same defaults | shared adds `@Serializable` |
| `Surah` | 7 fields: `id, nameArabic, nameEnglish, translationArabic, ayahCount, revelationType, startPage=1` | same 7 fields, same default | shared adds `@Serializable`; `:app` has a trailing comment on `revelationType` |
| `Reciter` | 4 fields + `companion { BASE_URL, DEFAULT_RECITERS (20), DEFAULT_RECITER = husary_mujawwad }` | character-identical | none |
| `SurahData` | 120 lines, `SURAH_LIST` | `diff` below the package line is **empty** | none |

Nothing in `:app` uses serialization on these types; nothing in `:app` references `Companion.serializer()`.

### 0.2 Every consumer of `com.example.data.model.*` in `app/src` (main + test)

`grep -rn "com\.example\.data\.model" app/src` returns exactly **21 lines**: the 4 `package` lines of the
copies themselves plus these **17 import lines** in **11 files**:

| # | File | Lines | Imports | `@Composable`? | Owner |
|---|---|---|---|---|---|
| 1 | `app/src/main/java/com/example/domain/repository/QuranRepository.kt` | 4, 5 | `Ayah`, `Surah` | no | **OpenCode** |
| 2 | `app/src/main/java/com/example/data/repository/QuranRepository.kt` | 9, 10, 11 | `Ayah`, `Surah`, `SurahData` | no | **OpenCode** |
| 3 | `app/src/main/java/com/example/data/local/AyahEntity.kt` | 5 | `Ayah` | no | **OpenCode** |
| 4 | `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` | 17, 18, 19 | `Ayah`, `Reciter`, `Surah` | no (0 hits) | **OpenCode** |
| 5 | `app/src/test/java/com/example/ReciterOrderTest.kt` | 3 | `Reciter` | no | **OpenCode** |
| 6 | `app/src/test/java/com/example/ReciterDefaultTest.kt` | 3 | `Reciter` | no | **OpenCode** |
| 7 | `app/src/test/java/com/example/SurahIndexSheetTest.kt` | 14 | `Surah` | no (0 hits) | **OpenCode** |
| 8 | `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt` | 128, 129 | `Ayah`, `Reciter` | **yes** | Claude Code |
| 9 | `app/src/main/java/com/example/ui/components/player/AyahCard.kt` | 34 | `Ayah` | **yes** | Claude Code |
| 10 | `app/src/main/java/com/example/ui/components/ReciterSelectorSheet.kt` | 33 | `Reciter` | **yes** | Claude Code |
| 11 | `app/src/main/java/com/example/ui/components/SurahIndexSheet.kt` | 48 | `Surah` | **yes** | Claude Code |

No file uses a wildcard import, no file references these types by fully-qualified name, and no file in
`com.example.data.model` other than the four copies exists. `PlayerControlPanel.kt` and
`QuranRepositoryTest.kt` mention "Ayah"/"Surah" only inside comments and assertion messages.

`:app` depends on `:shared` (`app/build.gradle.kts:90`), so the shared types are already on both the main
and the unit-test classpath — no Gradle change.

---

## 1. OBJECTIVE

Make `com.aistudio.quranblind.domain.model.*` the **only** definition of `Ayah`, `Surah`, `Reciter` and
`SurahData` in the repository: rewrite the 12 import lines you own in place, delete the four `:app` copies.
The four `@Composable` files (rows 8–11) are rewritten by Claude Code CLI in the same commit.

This is a pure package rename for the compiler — every field, default, and constant is identical.

---

## 2. FILES YOU OWN FOR THIS ORDER (write access)

```
app/src/main/java/com/example/domain/repository/QuranRepository.kt     MODIFY — 2 import lines
app/src/main/java/com/example/data/repository/QuranRepository.kt       MODIFY — 3 import lines
app/src/main/java/com/example/data/local/AyahEntity.kt                 MODIFY — 1 import line
app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt           MODIFY — 3 import lines
app/src/test/java/com/example/ReciterOrderTest.kt                      MODIFY — 1 import line
app/src/test/java/com/example/ReciterDefaultTest.kt                    MODIFY — 1 import line
app/src/test/java/com/example/SurahIndexSheetTest.kt                   MODIFY — 1 import line
app/src/main/java/com/example/data/model/Ayah.kt                       DELETE
app/src/main/java/com/example/data/model/Surah.kt                      DELETE
app/src/main/java/com/example/data/model/Reciter.kt                    DELETE
app/src/main/java/com/example/data/model/SurahData.kt                  DELETE
```

**Reserved to Claude Code CLI — do NOT open or edit** (CLAUDE.md §2, `@Composable` files):
`ui/screens/QuranPlayerScreen.kt`, `ui/components/player/AyahCard.kt`, `ui/components/ReciterSelectorSheet.kt`,
`ui/components/SurahIndexSheet.kt`. They will not compile on your tree after you delete the copies —
**that is expected**; the Commander rewrites them before the gate.

## 3. FILES YOU MUST NOT TOUCH

Every other file. In particular: anything else under `ui/**`, `accessibility/**`, `service/**`; `res/**`;
`shared/**` (the canonical types are frozen); `app/build.gradle.kts`; `gradle/libs.versions.toml`;
`proguard-rules.pro`; `.github/**`; `CLAUDE.md`; `fleet_config.json`. Do not delete any
`hs_err_pid*.log` / `replay_pid*.log`.

---

## 4. STEPS

### Step 1 — Rewrite the 12 import lines **in place**

For each file in §2 marked MODIFY, replace the import line(s) on the exact line numbers of §0.2, keeping
the line position (do not re-sort the import block, do not add or remove blank lines):

```
import com.example.data.model.Ayah       →  import com.aistudio.quranblind.domain.model.Ayah
import com.example.data.model.Surah      →  import com.aistudio.quranblind.domain.model.Surah
import com.example.data.model.Reciter    →  import com.aistudio.quranblind.domain.model.Reciter
import com.example.data.model.SurahData  →  import com.aistudio.quranblind.domain.model.SurahData
```

No other line in any of these files changes. In particular, in `data/repository/QuranRepository.kt`
the existing `import com.aistudio.quranblind.domain.text.sanitizeUthmanicText` (line 4) and every usage of
`Ayah(...)`, `Surah`, `SurahData.SURAH_LIST`, `toDomainModel()`, `Reciter.DEFAULT_RECITERS`,
`Reciter.DEFAULT_RECITER` stays byte-identical — they resolve to the shared types via the new imports.

### Step 2 — Delete the four `:app` copies

```bash
git rm app/src/main/java/com/example/data/model/Ayah.kt \
       app/src/main/java/com/example/data/model/Surah.kt \
       app/src/main/java/com/example/data/model/Reciter.kt \
       app/src/main/java/com/example/data/model/SurahData.kt
```

(`git rm` stages the deletion; that is fine — do **not** commit.) The directory
`app/src/main/java/com/example/data/model/` must be empty afterwards; remove it if it lingers.

### Step 3 — Verify by grep only (NO Gradle, NO compile probes)

```bash
grep -rn "com\.example\.data\.model" app/src
```

must show exactly the **5 lines** of the four reserved composable files (rows 8–11 of §0.2: lines 128/129,
34, 33, 48) — nothing else.

```bash
grep -rn "com\.aistudio\.quranblind\.domain\.model" app/src
```

must show exactly **12 lines**, all in the seven MODIFY files of §2.

Do **not** run `./gradlew` for any reason (B-17/B-18: the Commander compiles at the gate, and your tree is
intentionally not compilable until the Commander's four files are rewritten).

---

## 5. ACCEPTANCE CRITERIA

| # | Criterion |
|---|---|
| A1 | `git status --short` beyond the pre-existing baseline shows exactly: 7 ` M` files of §2 and 4 `D ` files of §2 |
| A2 | `git diff --numstat` (unstaged, the 7 MODIFY files) is exactly: `+2/−2`, `+3/−3`, `+1/−1`, `+3/−3`, `+1/−1`, `+1/−1`, `+1/−1` in the order of §2 |
| A3 | Step 3 grep #1 → exactly 5 lines, all in the four reserved composable files |
| A4 | Step 3 grep #2 → exactly 12 lines, all in the seven MODIFY files |
| A5 | `app/src/main/java/com/example/data/model/` no longer exists |
| A6 | `shared/**`, `build.gradle.kts` (both), `proguard-rules.pro`, and the four reserved composable files untouched (`git diff --stat` on them empty) |
| A7 | No Gradle invocation, no test executed by you; no commit, no push |

## 6. REPORT-BACK

`fleet_orders/reports/ORDER_P0_009_REPORT_OPENCODE.md`, sections: BASELINE (`git status --short` before
you start), FULL `git diff` of the seven MODIFY files (verbatim), `git status --short` AFTER, STEP 3 GREP
OUTPUT verbatim (both greps), DEVIATIONS, BLOCKED ON, SELF-ASSESSMENT A1–A7.

FORBIDDEN: any `./gradlew` invocation; running any test; opening any of the four reserved composable
files; touching any file outside §2; deleting log files; git commit/push; Arabic outside data string
literals (the Arabic in the files you delete is data — delete the files, do not retype anything). STOP
after reporting.
