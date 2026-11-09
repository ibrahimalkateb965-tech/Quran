// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  // alias(libs.plugins.google.services) apply false
  alias(libs.plugins.hilt) apply false

  // ORDER-P0-001 rev. D / B-15 — the Kotlin Gradle Plugin must be resolved ONCE, here.
  // :app pulls KGP in transitively (compose plugin + AGP 9's built-in Kotlin), which puts it
  // on the classpath with a version Gradle cannot attribute. A subproject then requesting
  // org.jetbrains.kotlin.multiplatform:2.2.10 fails the compatibility check with
  // "already on the classpath with an unknown version". Declaring it here with `apply false`
  // makes the root own the version and lets :shared apply it without a fresh version request.
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.serialization) apply false
  alias(libs.plugins.android.kotlin.multiplatform.library) apply false
}
