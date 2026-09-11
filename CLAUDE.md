# CLAUDE.md — Blind App (تطبيق القرآن للمكفوفين)

Project-level operating rules for Claude Code CLI in this repository.
These rules are **permanent** and apply to every session, without needing to be restated.

---

## 1. LANGUAGE RULE (PERMANENT — HIGHEST PRIORITY)

> **Claude Code MUST always reply to the maintainer (Ibrahim) in Arabic.**
> **All technical terms MUST remain in English** — untranslated and unmodified.

Scope and exceptions:

| Channel | Language |
| :--- | :--- |
| Replies to Ibrahim in chat | **Arabic** (technical terms in English) |
| Task Delegation orders to OpenCode CLI / Antigravity IDE | **English only** |
| Code, identifiers, comments, commit messages, PR bodies | **English only** |
| CI logs, CLI terminal commands | **English only** |
| Generated `.md` orders and audits under `fleet_orders/` | **English only** |

Technical terms that stay in English include (non-exhaustive):
`Kotlin Multiplatform`, `Compose Multiplatform`, `expect/actual`, `Koin`, `Hilt`,
`ExoPlayer`, `AVPlayer`, `AVAudioSession`, `VoiceOver`, `TalkBack`, `Ktor`,
`SQLDelight`, `Room`, `XCFramework`, `Gradle`, `Quality Gate`, `Devil's Advocate Audit`.

Do **not** transliterate these into Arabic script.

---

## 2. FLEET COMMAND MODE (Hook 22)

Once Hook 22 is invoked, this session stays in Fleet Command Mode permanently.
Every subsequent request is converted into structured delegation using
`fleet_templates/TEMPLATE_02_TASK_DELEGATION.md` and
`fleet_templates/TEMPLATE_03_CODE_REVIEW_AUDIT.md`.

### Exclusive responsibilities

| Agent | Owns | Absolutely forbidden |
| :--- | :--- | :--- |
| **Claude Code CLI** (`Opus 5`) | Planning, architectural review, Devil's Advocate Audit, **exclusive execution of ALL automated tests**, final approval / Quality Gates | Writing bulk boilerplate that a cheaper model can produce |
| **OpenCode CLI** (`Ox Alpha Unlimited`) | Terminal execution, build files, Gradle / version catalog, database layer, network clients, CI workflows | **Running any test.** Touching UI composables or accessibility semantics |
| **Antigravity IDE** (`Gemini 3.7 Flash High`) | UI building, iOS adaptation, Domain Logic, `expect/actual` platform abstractions, persistent memory | **Running any test.** Editing Gradle files or CI workflows |

### Testing Monopoly
No code is merged until Claude Code has executed the tests itself and issued
an explicit `QUALITY GATE: PASS`. OpenCode and Antigravity **write and build only**.

---

## 3. TARGET STACK (as of 2026-09-10)

- **Shared:** Kotlin Multiplatform + Compose Multiplatform
- **Modules:** `:shared` (domain + data + platform abstractions), `:composeApp` (shared UI), `:androidApp`, `iosApp/` (Xcode)
- **DI:** Koin (Hilt is Android/JVM-only and **cannot** cross to iOS)
- **Network:** Ktor Client (`OkHttp` engine on Android, `Darwin` engine on iOS) + `kotlinx.serialization`
- **Audio:** `expect class AudioEngine` → `Media3/ExoPlayer` (Android) / `AVPlayer` + `AVAudioSession` (iOS)
- **Accessibility:** `expect` platform layer → `TalkBack` (Android) / `VoiceOver` (iOS). **Never share the interaction model.**

## 4. NON-NEGOTIABLE PRODUCT CONSTRAINTS

1. **Blind-first.** Any change that degrades screen-reader behaviour is an automatic Quality Gate failure, regardless of test results.
2. **No paid unlock outside the platform billing system.** The app is fully free; `TrialManager` and PIN gating were removed deliberately.
3. **No data collection.** The Play Store Data Safety declaration and the iOS Privacy Manifest must both stay at "no data collected".
4. **Uthmanic text integrity.** `sanitizeUthmanicText` must be ported byte-for-byte; any regression in diacritic rendering is a P0 defect.
