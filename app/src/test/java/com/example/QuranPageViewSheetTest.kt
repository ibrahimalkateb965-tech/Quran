package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextInput
import com.aistudio.quranblind.domain.model.Ayah
import com.example.ui.components.player.QuranPageViewSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class QuranPageViewSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleAyahs = listOf(
        Ayah(numberInSurah = 1, globalNumber = 1, textArabic = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", surahId = 1, page = 1, juz = 1),
        Ayah(numberInSurah = 2, globalNumber = 2, textArabic = "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", surahId = 1, page = 1, juz = 1),
        Ayah(numberInSurah = 3, globalNumber = 3, textArabic = "الرَّحْمَٰنِ الرَّحِيمِ", surahId = 1, page = 1, juz = 1),
        Ayah(numberInSurah = 4, globalNumber = 4, textArabic = "مَالِكِ يَوْمِ الدِّينِ", surahId = 1, page = 1, juz = 1),
        Ayah(numberInSurah = 5, globalNumber = 5, textArabic = "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", surahId = 1, page = 1, juz = 1),
        Ayah(numberInSurah = 6, globalNumber = 6, textArabic = "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", surahId = 1, page = 1, juz = 1),
        Ayah(numberInSurah = 7, globalNumber = 7, textArabic = "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ", surahId = 1, page = 1, juz = 1)
    )

    @Test
    fun testSwitchToTextModeAndSearchByAyahNumber() {
        var selectedSurahId = -1
        var selectedAyahIndex = -1

        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { surahId, ayahIndex ->
                    selectedSurahId = surahId
                    selectedAyahIndex = ayahIndex
                },
                onChangePage = {},
                onDismiss = {},
                onAnnounce = {}
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithText("📜 نص الآيات").performClick()
        composeTestRule.waitForIdle()

        // Verify search field exists
        composeTestRule.onNodeWithTag("ayah_search_text_field").assertExists()

        // Filter by Ayah 5
        composeTestRule.onNodeWithTag("ayah_search_text_field").performTextInput("5")
        composeTestRule.waitForIdle()

        // Ayah 5 should be displayed
        composeTestRule.onNodeWithText("الآية 5", substring = true).assertExists()
        // Ayah 2 should not exist in the filtered list
        composeTestRule.onNodeWithText("الآية 2", substring = true).assertDoesNotExist()

        // Click on Ayah 5
        composeTestRule.onNodeWithText("الآية 5", substring = true).performClick()
        composeTestRule.waitForIdle()

        // Should invoke onSelectAyah with surahId 1 and ayahIndex 4 (0-based)
        assert(selectedSurahId == 1) { "Expected surahId 1 but got $selectedSurahId" }
        assert(selectedAyahIndex == 4) { "Expected ayahIndex 4 but got $selectedAyahIndex" }
    }

    @Test
    fun testArabicDigitsNormalizationInSearch() {
        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { _, _ -> },
                onChangePage = {},
                onDismiss = {},
                onAnnounce = {}
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithText("📜 نص الآيات").performClick()
        composeTestRule.waitForIdle()

        // Enter Eastern Arabic numeral '٣' (3)
        composeTestRule.onNodeWithTag("ayah_search_text_field").performTextInput("٣")
        composeTestRule.waitForIdle()

        // Ayah 3 should exist, others should be filtered
        composeTestRule.onNodeWithText("الآية 3", substring = true).assertExists()
        composeTestRule.onNodeWithText("الآية 1", substring = true).assertDoesNotExist()
    }

    @Test
    fun testEmptyStateWhenNoAyahMatches() {
        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { _, _ -> },
                onChangePage = {},
                onDismiss = {},
                onAnnounce = {}
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithText("📜 نص الآيات").performClick()
        composeTestRule.waitForIdle()

        // Search for a non-existing ayah number on page 1
        composeTestRule.onNodeWithTag("ayah_search_text_field").performTextInput("99")
        composeTestRule.waitForIdle()

        // Empty state should be visible
        composeTestRule.onNodeWithTag("ayah_search_empty_state").assertExists()
        composeTestRule.onNodeWithText("لا توجد آية برقم «99» في هذه الصفحة", substring = true).assertExists()
    }

    @Test
    fun testLeadingZeroSearchNormalization() {
        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { _, _ -> },
                onChangePage = {},
                onDismiss = {},
                onAnnounce = {}
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithTag("uthmani_text_tab").performClick()
        composeTestRule.waitForIdle()

        // Search with leading zero "05"
        composeTestRule.onNodeWithTag("ayah_search_text_field").performTextInput("05")
        composeTestRule.waitForIdle()

        // Ayah 5 should be displayed despite leading zero
        composeTestRule.onNodeWithText("الآية 5", substring = true).assertExists()
        composeTestRule.onNodeWithText("الآية 2", substring = true).assertDoesNotExist()
    }

    @Test
    fun testEmptyStateClearButtonResetsSearch() {
        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { _, _ -> },
                onChangePage = {},
                onDismiss = {},
                onAnnounce = {}
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithTag("uthmani_text_tab").performClick()
        composeTestRule.waitForIdle()

        // Enter non-matching query
        composeTestRule.onNodeWithTag("ayah_search_text_field").performTextInput("99")
        composeTestRule.waitForIdle()

        // Verify empty state is displayed with clear button
        composeTestRule.onNodeWithTag("ayah_search_empty_state").assertExists()
        composeTestRule.onNodeWithTag("clear_search_button").assertExists()

        // Click clear button (scroll first: the empty state is scrollable and the
        // button can sit below the fold on small viewports, as TalkBack would scroll to it)
        composeTestRule.onNodeWithTag("clear_search_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // List should be restored (Ayah 1 and Ayah 2 exist again); the LazyColumn only
        // composes visible items, so scroll to Ayah 2 on small viewports
        composeTestRule.onNodeWithTag("ayah_search_empty_state").assertDoesNotExist()
        composeTestRule.onNodeWithText("الآية 1", substring = true).assertExists()
        composeTestRule.onNodeWithTag("quran_page_view_list")
            .performScrollToNode(hasText("الآية 2", substring = true))
        composeTestRule.onNodeWithText("الآية 2", substring = true).assertExists()
    }

    @Test
    fun testSurahPickerButtonOpensSurahIndex() {
        var selectedSurahId = -1
        var selectedAyahIndex = -1
        var changedPage = -1

        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { surahId, ayahIndex ->
                    selectedSurahId = surahId
                    selectedAyahIndex = ayahIndex
                },
                onChangePage = { page ->
                    changedPage = page
                },
                onDismiss = {},
                onAnnounce = {}
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithTag("uthmani_text_tab").performClick()
        composeTestRule.waitForIdle()

        // Verify Surah picker button exists
        composeTestRule.onNodeWithTag("page_sheet_surah_picker_button").assertExists()

        // Click Surah picker button
        composeTestRule.onNodeWithTag("page_sheet_surah_picker_button").performClick()
        composeTestRule.waitForIdle()

        // Verify Surah selection sheet opened (114 surahs)
        composeTestRule.onNodeWithText("اختيارُ السورة", substring = true).assertExists()
    }

    @Test
    fun testSurahPickerSelectAyahRoutesToSurahPickerCallbackAndKeepsSheetOpen() {
        var pickerSurahId = -1
        var pickerAyahIndex = -1
        var sheetDismissed = false

        composeTestRule.activity.setContent {
            QuranPageViewSheet(
                pageNumber = 1,
                currentSurahId = 1,
                currentAyahNumberInSurah = 1,
                ayahs = sampleAyahs,
                onSelectAyah = { _, _ -> },
                onChangePage = {},
                onDismiss = { sheetDismissed = true },
                onAnnounce = {},
                onSelectAyahFromSurahPicker = { surahId, ayahIndex ->
                    pickerSurahId = surahId
                    pickerAyahIndex = ayahIndex
                }
            )
        }

        // Switch to Text View Tab
        composeTestRule.onNodeWithTag("uthmani_text_tab").performClick()
        composeTestRule.waitForIdle()

        // Click Surah picker button
        composeTestRule.onNodeWithTag("page_sheet_surah_picker_button").performClick()
        composeTestRule.waitForIdle()

        // Click on Surah Al-Fatihah using its unique item tag
        composeTestRule.onNodeWithTag("surah_item_1").performClick()
        composeTestRule.waitForIdle()

        // Click on Ayah 3
        composeTestRule.onNodeWithText("الآية 3", substring = true).performClick()
        composeTestRule.waitForIdle()

        // Assert onSelectAyahFromSurahPicker received surahId 1 and ayahIndex 2 (0-indexed)
        assert(pickerSurahId == 1) { "Expected pickerSurahId 1 but got $pickerSurahId" }
        assert(pickerAyahIndex == 2) { "Expected pickerAyahIndex 2 but got $pickerAyahIndex" }
        // Assert sheet was NOT dismissed
        assert(!sheetDismissed) { "QuranPageViewSheet should NOT be dismissed" }
    }
}
