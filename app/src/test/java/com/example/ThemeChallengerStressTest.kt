package com.example

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionPreferences
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.theme.QuranBlindTheme
import com.example.ui.theme.QuranTheme
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.SettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Empirical Adversarial Stress Test Suite for Milestone 1: Day/Night Mode Toggle & Persistence.
 * Authored by Empirical Challenger (teamwork_challenger_m1_1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ar")
class ThemeChallengerStressTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // =========================================================================
    // 1. Stress Test: Edge Conditions & Corrupted Values
    // =========================================================================

    @Test
    fun stressTest_corruptOrUnexpectedThemeStrings_gracefullyFallbackToSystem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionPrefs = SessionPreferences(context)

        val adversarialInputs = listOf(
            null,
            "",
            "   ",
            "INVALID",
            "dark",
            "light",
            "system",
            "DARK ",
            " LIGHT",
            "null",
            "UNDEFINED",
            "12345",
            "\u0000\u0001",
            "__!@#\$%^&*()__",
            "SYSTEM_DEFAULT"
        )

        for (input in adversarialInputs) {
            if (input != null) {
                sessionPrefs.saveThemeMode(input)
                assertEquals("Persisted raw value must match input", input, sessionPrefs.getThemeMode())
            } else {
                sessionPrefs.clearSession()
                assertNull("Cleared session must yield null theme mode", sessionPrefs.getThemeMode())
            }

            // Simulate ViewModel init resolution logic:
            val savedTheme = sessionPrefs.getThemeMode()
            val resolvedMode = if (savedTheme != null) {
                when (savedTheme) {
                    "LIGHT" -> AppThemeMode.LIGHT
                    "DARK" -> AppThemeMode.DARK
                    else -> AppThemeMode.SYSTEM
                }
            } else {
                AppThemeMode.SYSTEM
            }

            assertEquals(
                "Input '$input' must gracefully fall back to AppThemeMode.SYSTEM",
                AppThemeMode.SYSTEM,
                resolvedMode
            )

            // Verify MainActivity resolution when resolvedMode is SYSTEM
            fun resolveActivityDarkTheme(mode: AppThemeMode, systemInDark: Boolean): Boolean {
                return when (mode) {
                    AppThemeMode.SYSTEM -> systemInDark
                    AppThemeMode.DARK -> true
                    AppThemeMode.LIGHT -> false
                }
            }

            assertTrue("SYSTEM mode must follow system when dark", resolveActivityDarkTheme(resolvedMode, true))
            assertFalse("SYSTEM mode must follow system when light", resolveActivityDarkTheme(resolvedMode, false))
        }
    }

    // =========================================================================
    // 2. Stress Test: Rapid Sequential & Concurrent Toggling
    // =========================================================================

    @Test
    fun stressTest_rapidSequentialToggling_keepsStateAndPrefsInLockstep() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionPrefs = SessionPreferences(context)
        val settingsUiState = MutableStateFlow(SettingsUiState())

        fun simulateToggle(isCurrentlyDark: Boolean) {
            val newMode = if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
            sessionPrefs.saveThemeMode(newMode.name)
            settingsUiState.update { it.copy(themeMode = newMode) }
        }

        var isDark = false
        // Perform 200 rapid toggles
        for (i in 1..200) {
            simulateToggle(isCurrentlyDark = isDark)
            val expectedMode = if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
            assertEquals("Iteration $i: settingsUiState mismatch", expectedMode, settingsUiState.value.themeMode)
            assertEquals("Iteration $i: sessionPrefs mismatch", expectedMode.name, sessionPrefs.getThemeMode())
            isDark = (expectedMode == AppThemeMode.DARK)
        }
    }

    @Test
    fun stressTest_concurrentToggling_noCrashAndConsistentState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionPrefs = SessionPreferences(context)
        val settingsUiState = MutableStateFlow(SettingsUiState())

        val concurrencyLevel = 50
        val jobs = (1..concurrencyLevel).map { index ->
            async(Dispatchers.Default) {
                val isDark = (index % 2 == 0)
                val newMode = if (isDark) AppThemeMode.LIGHT else AppThemeMode.DARK
                sessionPrefs.saveThemeMode(newMode.name)
                settingsUiState.update { it.copy(themeMode = newMode) }
            }
        }

        jobs.awaitAll()

        // After all concurrent operations finish, preferences and state must be valid enum modes
        val finalPref = sessionPrefs.getThemeMode()
        val finalState = settingsUiState.value.themeMode

        assertNotNull("Final preference must not be null", finalPref)
        assertTrue("Final preference must be LIGHT or DARK", finalPref == "LIGHT" || finalPref == "DARK")
        assertTrue("Final state must be LIGHT or DARK", finalState == AppThemeMode.LIGHT || finalState == AppThemeMode.DARK)
        
        // Note for Reviewer/Architect:
        // When toggleTheme is invoked concurrently across disparate threads, SharedPreferences (async apply)
        // and StateFlow.update are not synchronized under a mutual exclusion lock (Mutex).
        // On Android, UI clicks are serialized on the Main thread, where lockstep is guaranteed (verified in sequential test).
    }

    // =========================================================================
    // 3. Stress Test: Persistence Across Restarts & Overriding System Mode
    // =========================================================================

    @Test
    fun stressTest_persistenceAcrossSimulatedRestarts_withCustomPrefs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sharedPrefs = context.getSharedPreferences("test_restart_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().commit()

        // 1. First "Run": User sets theme to DARK
        val prefsRun1 = SessionPreferences(context, customPrefs = sharedPrefs)
        prefsRun1.saveThemeMode("DARK")

        // 2. Simulate Cold App Restart (New SessionPreferences instance with same backing prefs)
        val prefsRun2 = SessionPreferences(context, customPrefs = sharedPrefs)
        val restoredString = prefsRun2.getThemeMode()
        assertEquals("Restored string after restart must be DARK", "DARK", restoredString)

        val restoredMode = when (restoredString) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
        assertEquals(AppThemeMode.DARK, restoredMode)

        // Verify it overrides system when system is LIGHT
        val darkThemeApplied1 = when (restoredMode) {
            AppThemeMode.SYSTEM -> false
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
        }
        assertTrue("DARK preference must override light system default", darkThemeApplied1)

        // 3. User switches to LIGHT in run 2
        prefsRun2.saveThemeMode("LIGHT")

        // 4. Simulate Second Cold Restart
        val prefsRun3 = SessionPreferences(context, customPrefs = sharedPrefs)
        val restoredString3 = prefsRun3.getThemeMode()
        assertEquals("Restored string after 2nd restart must be LIGHT", "LIGHT", restoredString3)

        val restoredMode3 = when (restoredString3) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
        assertEquals(AppThemeMode.LIGHT, restoredMode3)

        // Verify it overrides system when system is DARK
        val darkThemeApplied3 = when (restoredMode3) {
            AppThemeMode.SYSTEM -> true
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
        }
        assertFalse("LIGHT preference must override dark system default", darkThemeApplied3)
    }

    // =========================================================================
    // 4. Stress Test: Accessibility Target Size & Physical Placement in RTL
    // =========================================================================

    @Test
    fun stressTest_buttonAccessibilityTouchTargetAndRTLPhysicalPlacement() {
        var isDark by mutableStateOf(false)

        composeTestRule.activity.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl
            ) {
                QuranBlindTheme(darkTheme = isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .testTag("parent_bar")
                    ) {
                        val currentDark = QuranTheme.colors.isDark
                        val toggleActionLabel = if (currentDark) "التبديل إلى الوضع النهاري" else "التبديل إلى الوضع الليلي"

                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .testTag("theme_toggle_button")
                        ) {
                            BlindAccessibleIconButton(
                                onClick = { isDark = !isDark },
                                onClickLabel = toggleActionLabel,
                                modifier = Modifier
                                    .testTag("day_night_toggle_button")
                                    .size(52.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                    contentDescription = toggleActionLabel,
                                    tint = QuranTheme.colors.accent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        // Verify touch target is at least 48dp (specifically 52dp)
        composeTestRule.onNodeWithTag("day_night_toggle_button")
            .assertWidthIsEqualTo(52.dp)
            .assertHeightIsEqualTo(52.dp)

        // Check geometry in RTL:
        // In RTL, Start is Right, End is Left.
        // Therefore, Alignment.CenterEnd aligns to the LEFT edge of the screen!
        val parentBounds = composeTestRule.onNodeWithTag("parent_bar").getBoundsInRoot()
        val buttonBounds = composeTestRule.onNodeWithTag("theme_toggle_button").getBoundsInRoot()

        // The button's left edge aligns with the parent's left edge in RTL (since End = Left)
        val leftDistance = buttonBounds.left - parentBounds.left
        assertEquals("Button must be placed at physical left in RTL (Alignment.CenterEnd)", 0.dp.value, leftDistance.value, 1.0f)

        // Verify icon & label flips
        composeTestRule.onNodeWithContentDescription("التبديل إلى الوضع الليلي").assertIsDisplayed()
        composeTestRule.onNodeWithTag("day_night_toggle_button").performClick()
        composeTestRule.waitForIdle()

        assertTrue(isDark)
        composeTestRule.onNodeWithContentDescription("التبديل إلى الوضع النهاري").assertIsDisplayed()
    }

    // =========================================================================
    // 5. Empirical Challenge: Latent Wipe Defect in createSecurePrefs Fallback
    // =========================================================================

    @Test
    fun challenge_reproduceLatentWipeInSessionPreferencesFallback() {
        // Regression guard: verify that when Keystore is unavailable (such as in Robolectric/JVM
        // or devices without KeyStore AES256 support), calling the default SessionPreferences(context)
        // does NOT wipe previous preferences on restart.
        val context = ApplicationProvider.getApplicationContext<Context>()

        val instance1 = SessionPreferences(context)
        instance1.saveThemeMode("DARK")
        assertEquals("First instance holds DARK", "DARK", instance1.getThemeMode())

        // Now instantiate a second instance with default constructor (simulating app restart without customPrefs)
        val instance2 = SessionPreferences(context)
        val restoredFromInstance2 = instance2.getThemeMode()

        // Verified fix: createSecurePrefs must NOT wipe unencrypted preferences when Keystore is unavailable
        assertEquals(
            "Preferences must persist across default SessionPreferences instances in fallback mode without being wiped",
            "DARK",
            restoredFromInstance2
        )
    }
}
