# iosApp — Xcode shell for QuranBlind

## The Xcode project is generated, never committed

There is no `.pbxproj` in this repository and there must not be one. The project is
declared in `project.yml` and generated with [XcodeGen](https://github.com/yonaskolb/XcodeGen):

```bash
cd iosApp
xcodegen generate
open QuranBlind.xcodeproj
```

`iosApp/QuranBlind.xcodeproj/` is git-ignored. A hand-written `pbxproj` cannot be
validated on a Windows host and is unreviewable in a diff; `project.yml` is plain text
that reviews cleanly and regenerates deterministically.

## This can only be built on macOS

Kotlin/Native cannot compile iOS targets on Windows — not slowly, not at all. The
maintainer's machine is Windows, so:

- iOS compilation and linking happen exclusively on the GitHub Actions `macos-15` runner.
- Locally, `kotlin.native.ignoreDisabledTargets=true` keeps Gradle *configuration*
  succeeding. It does not make iOS builds possible.
- **Never run `./gradlew build` on this repo from Windows.** It will attempt
  `linkDebugFrameworkIosArm64` and fail. Use narrowly-scoped tasks.

## How the framework reaches Xcode

The `QuranBlind` target runs a pre-build script that calls:

```
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

That task is provided by the Kotlin Multiplatform plugin and relies on environment
variables Xcode sets during a build (`CONFIGURATION`, `SDK_NAME`, `TARGET_BUILD_DIR`,
`FRAMEWORKS_FOLDER_PATH`). It therefore works when Xcode invokes it and is *not*
expected to work when run by hand from a terminal.

`basedOnDependencyAnalysis: false` is required — without it Xcode skips the phase
because it declares no output files.

## What is in this directory

```
iosApp/
├── project.yml                  XcodeGen spec — the only project definition that is committed
├── Sources/
│   ├── App.swift                @main entry; starts Koin via SharedKit
│   ├── ContentView.swift        SwiftUI surah/ayah screen; loads text through QuranBridgeIosKt
│   └── SurahPlayer.swift        ObservableObject over the shared AudioEngine (AudioBridgeIosKt)
└── Resources/
    └── PrivacyInfo.xcprivacy    App privacy manifest: no tracking, no data collected
```

`Resources/PrivacyInfo.xcprivacy` is copied to the bundle root by a `resources` build
phase declared in `project.yml`; CI lints it (`plutil` + `PlistBuddy`) and asserts it is
present inside the archived `.app`. The Uthmanic dataset is **not** duplicated here:
`project.yml` references `../app/src/main/assets/quran/quran_uthmani_tanzil.json` as a
resource so the repository holds one copy of the text (CLAUDE.md §4.4), and
`BundleQuranJsonSource` (`shared/src/iosMain`) reads it from the main bundle.

## Which workflow proves what

| Workflow | Trigger | Proves |
| :--- | :--- | :--- |
| `.github/workflows/ios_shared_compile.yml` | push to `feat/ios-**`, `chore/ios-**` | `:shared` iosMain compiles and links; `xcodegen generate` + unsigned simulator build succeed; privacy manifest, ATS and required-reason API audits pass |
| `.github/workflows/ios_testflight.yml` | manual `Run workflow` (input `upload`) or tag `ios-v*` | signed archive + `.ipa` export; with `upload=true`, validation and upload to TestFlight. Needs the eight Apple secrets listed in `REMOTE_IOS_DEV_PLAYBOOK.md §6` |

## Status

All three Swift files `import SharedKit` and call into the shared bridges
(ORDER-P1-008 text, ORDER-P1-006-IOS audio). The simulator build is green on CI
(`ios_shared_compile.yml`). A device or TestFlight build additionally needs an Apple
Developer Program membership so the secrets can exist; until then `ios_testflight.yml`
cannot run past its preflight step. Uthmanic rendering on a real device (ADR-001) is
still unverified.
