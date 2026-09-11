pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

// ORDER-P0-001 / B-11: ASCII only. This value feeds Kotlin/Native framework naming,
// derived build paths and the Xcode embedAndSign build phase. The user-visible app name
// is unaffected (res/values*/strings.xml on Android, CFBundleDisplayName on iOS).
rootProject.name = "QuranBlind"

include(":app", ":shared")
