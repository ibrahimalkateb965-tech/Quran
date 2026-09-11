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

## Status

Skeleton only, created by ORDER-P0-001. `ContentView` renders a placeholder and does
not import `SharedKit` yet. Wiring the framework into Swift is a later order, gated on
the first successful CI framework build.
