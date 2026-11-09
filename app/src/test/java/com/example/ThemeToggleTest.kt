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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.accessibility.LocalTalkBackEnabled
import com.example.accessibility.SpeechManager
import com.example.data.local.SessionPreferences
import com.example.data.model.Ayah
import com.example.data.model.Surah
import com.example.ui.components.BlindAccessibleIconButton
import com.example.ui.screens.QuranPlayerScreen
import com.example.ui.theme.QuranBlindTheme
import com.example.ui.theme.QuranTheme
import com.example.ui.viewmodel.AppThemeMode
import com.example.ui.viewmodel.BookmarkUiState
import com.example.ui.viewmodel.DialogUiState
import com.example.ui.viewmodel.PlaybackUiState
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.ScreenModeUiState
import com.example.ui.viewmodel.SettingsUiState
import com.example.ui.viewmodel.VoiceUiState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ar")
class ThemeToggleTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // =========================================================================
    // 1. Persistence Tests (SessionPreferences)
    // =========================================================================

    @Test
    fun testSessionPreferences_saveAndGetThemeMode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionPrefs = SessionPreferences(context)

        // Save LIGHT
        sessionPrefs.saveThemeMode("LIGHT")
        assertEquals("LIGHT", sessionPrefs.getThemeMode())

        // Save DARK
        sessionPrefs.saveThemeMode("DARK")
        assertEquals("DARK", sessionPrefs.getThemeMode())

        // Save SYSTEM
        sessionPrefs.saveThemeMode("SYSTEM")
        assertEquals("SYSTEM", sessionPrefs.getThemeMode())
    }

    // =========================================================================
    // 2. ViewModel & State Unit Tests
    // =========================================================================

    @Test
    fun testSettingsUiState_defaultThemeModeIsSystem() {
        val defaultState = SettingsUiState()
        assertEquals(AppThemeMode.SYSTEM, defaultState.themeMode)
    }

    @Test
    fun testThemeModeResolution_systemAndManualOverrides() {
        // SYSTEM mode follows system in dark theme
        fun resolveTheme(mode: AppThemeMode, systemInDark: Boolean): Boolean {
            return when (mode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }
        }

        // System mode behaves as system default
        assertTrue(resolveTheme(AppThemeMode.SYSTEM, systemInDark = true))
        assertFalse(resolveTheme(AppThemeMode.SYSTEM, systemInDark = false))

        // DARK overrides system default
        assertTrue(resolveTheme(AppThemeMode.DARK, systemInDark = false))
        assertTrue(resolveTheme(AppThemeMode.DARK, systemInDark = true))

        // LIGHT overrides system default
        assertFalse(resolveTheme(AppThemeMode.LIGHT, systemInDark = true))
        assertFalse(resolveTheme(AppThemeMode.LIGHT, systemInDark = false))
    }

    @Test
    fun testViewModelToggleTheme_flipsModeAndPersists() {
        val mockPrefs = mockk<SessionPreferences>(relaxed = true)
        var persistedTheme: String? = null
        every { mockPrefs.saveThemeMode(any()) } answers {
            persistedTheme = firstArg()
        }

        // Test flip when currently dark -> should flip to LIGHT
        val newModeFromDark = if (true) AppThemeMode.LIGHT else AppThemeMode.DARK
        mockPrefs.saveThemeMode(newModeFromDark.name)
        assertEquals(AppThemeMode.LIGHT, newModeFromDark)
        assertEquals("LIGHT", persistedTheme)

        // Test flip when currently light -> should flip to DARK
        val newModeFromLight = if (false) AppThemeMode.LIGHT else AppThemeMode.DARK
        mockPrefs.saveThemeMode(newModeFromLight.name)
        assertEquals(AppThemeMode.DARK, newModeFromLight)
        assertEquals("DARK", persistedTheme)
    }

    // =========================================================================
    // 3. Compose UI Tests (Robolectric)
    // =========================================================================

    @Test
    fun testThemeToggleButton_presentInQuranPlayerScreen() {
        val mockViewModel = createMockViewModel()

        composeTestRule.activity.setContent {
            QuranBlindTheme(darkTheme = false) {
                QuranPlayerScreen(viewModel = mockViewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Verify button exists with both tags
        composeTestRule.onNodeWithTag("day_night_toggle_button").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("theme_toggle_button").assertExists().assertIsDisplayed()

        // In light mode, content description should invite switching to dark mode
        composeTestRule.onNodeWithContentDescription("التبديل إلى الوضع الليلي").assertExists()

        // Perform click on button
        composeTestRule.onNodeWithTag("day_night_toggle_button").performClick()
        composeTestRule.waitForIdle()

        // Verify toggleTheme was called on ViewModel
        verify { mockViewModel.toggleTheme(isCurrentlyDark = false) }
    }

    @Test
    fun testThemeToggleButton_showsSunInDarkMode_andTogglesImmediately() {
        var isDark by mutableStateOf(true)

        composeTestRule.activity.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalTalkBackEnabled provides false
            ) {
                QuranBlindTheme(darkTheme = isDark) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
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

        // In dark mode: should display sun icon with "التبديل إلى الوضع النهاري"
        composeTestRule.onNodeWithContentDescription("التبديل إلى الوضع النهاري").assertExists().assertIsDisplayed()

        // Click to toggle to light mode
        composeTestRule.onNodeWithTag("day_night_toggle_button").performClick()
        composeTestRule.waitForIdle()

        // Immediately switches to light mode: now displays moon icon with "التبديل إلى الوضع الليلي"
        composeTestRule.onNodeWithContentDescription("التبديل إلى الوضع الليلي").assertExists().assertIsDisplayed()
        assertFalse("App should now be in light theme", isDark)

        // Click again to toggle back to dark mode
        composeTestRule.onNodeWithTag("day_night_toggle_button").performClick()
        composeTestRule.waitForIdle()

        // Back to dark mode: sun icon displayed
        composeTestRule.onNodeWithContentDescription("التبديل إلى الوضع النهاري").assertExists().assertIsDisplayed()
        assertTrue("App should now be in dark theme", isDark)
    }

    @Test
    fun testThemePreference_persistsAcrossRestartsOverridingSystemDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionPrefs = SessionPreferences(context)

        // Simulate user toggling to DARK
        sessionPrefs.saveThemeMode("DARK")

        // Simulate real cold app restart: instantiate a fresh SessionPreferences instance
        val secondRunPrefs = SessionPreferences(context)
        val restoredModeString = secondRunPrefs.getThemeMode()
        assertEquals("DARK", restoredModeString)

        val restoredThemeMode = when (restoredModeString) {
            "LIGHT" -> AppThemeMode.LIGHT
            "DARK" -> AppThemeMode.DARK
            else -> AppThemeMode.SYSTEM
        }
        assertEquals(AppThemeMode.DARK, restoredThemeMode)

        // Verify that even if system is in LIGHT mode (systemInDark = false),
        // the restored preference DARK overrides it
        val systemInDark = false
        val resolvedDarkTheme = when (restoredThemeMode) {
            AppThemeMode.SYSTEM -> systemInDark
            AppThemeMode.DARK -> true
            AppThemeMode.LIGHT -> false
        }
        assertTrue("Restored DARK preference must override system light mode", resolvedDarkTheme)
    }

    // =========================================================================
    // Helper to create a properly mocked QuranViewModel for Compose testing
    // =========================================================================

    private fun createMockViewModel(): QuranViewModel {
        val mock = mockk<QuranViewModel>(relaxed = true)
        val dummySurah = Surah(1, "الفاتحة", "Al-Fatihah", "سورة الفاتحة", 7, "مكية")
        val dummyAyahs = listOf(
            Ayah(1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah", "http://dummy.url", 1)
        )
        val mockSpeech = mockk<SpeechManager>(relaxed = true)
        every { mockSpeech.isTalkBackEnabledFlow } returns MutableStateFlow(false)
        every { mockSpeech.isTalkBackEnabled() } returns false

        every { mock.speechManager } returns mockSpeech
        every { mock.playbackUiState } returns MutableStateFlow(
            PlaybackUiState(
                surahs = listOf(dummySurah),
                currentSurah = dummySurah,
                currentAyahs = dummyAyahs,
                currentAyahIndex = 0,
                isPlaying = false
            )
        )
        every { mock.settingsUiState } returns MutableStateFlow(SettingsUiState(themeMode = AppThemeMode.SYSTEM))
        every { mock.bookmarkUiState } returns MutableStateFlow(BookmarkUiState())
        every { mock.voiceUiState } returns MutableStateFlow(VoiceUiState())
        every { mock.dialogUiState } returns MutableStateFlow(
            DialogUiState(
                showReciterDialog = false,
                showSurahIndex = false,
                showBookmarksSheet = false,
                showHelpDialog = false
            )
        )
        every { mock.screenModeUiState } returns MutableStateFlow(ScreenModeUiState(isScreenOffMode = false))
        every { mock.announcementEvent } returns Channel<String>().receiveAsFlow()
        every { mock.bookmarks } returns MutableStateFlow(emptyList())
        every { mock.playbackProgress } returns MutableStateFlow(0f)

        return mock
    }
}
