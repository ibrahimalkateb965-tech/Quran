import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    // NOT com.android.library. AGP 9 dropped KMP compatibility with it; this is the
    // dedicated Android/KMP library plugin and it versions in lockstep with AGP.
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    // AGP >= 8.12 uses `android { }` here (the older `androidLibrary { }` spelling is
    // deprecated as of AGP 9.1). There is deliberately NO top-level `android { }` block:
    // this plugin configures the Android target from inside `kotlin { }`.
    android {
        namespace = "com.aistudio.quranblind.shared"
        compileSdk = 36
        minSdk = 24

        // Android host tests are OFF by default under this plugin. Opting in now means
        // commonTest has somewhere to run on the Android target when P0-002 / P0-003
        // land their tests. No test is executed by this order.
        withHostTestBuilder {}.configure {}

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedKit"   // ASCII, distinct from the module name
            isStatic = true          // simplifies App Store submission
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
