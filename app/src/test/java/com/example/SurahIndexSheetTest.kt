package com.example

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.ui.components.SurahIndexSheet
import com.example.data.model.Surah
import androidx.test.espresso.Espresso
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SurahIndexSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun testBackButtonInAyahList() {
        val dummySurahs = listOf(
            Surah(1, "الفاتحة", "Al-Fatihah", "سورة الفاتحة", 7, "مكية")
        )
        
        var dismissed = false
        var announced = ""

        composeTestRule.activity.setContent {
            SurahIndexSheet(
                surahs = dummySurahs,
                currentSurahId = 1,
                currentAyahIndex = 0,
                onSelectSurah = { _, _ -> },
                onDismiss = { dismissed = true },
                onAnnounce = { announced = it }
            )
        }

        // Click on the surah to enter ayah list
        composeTestRule.onNodeWithText("سورة الفاتحة", substring = true).performClick()
        composeTestRule.waitForIdle()
        
        // Check that "اختيار الآية" was announced
        assert(announced == "اختيار الآية") { "Expected announcement 'اختيار الآية' but was '$announced'" }

        // Check if we are in ayah list (should see الآية 1)
        composeTestRule.onNodeWithText("الآية 1").assertExists()
        
        // Press back button
        Espresso.pressBack()
        
        composeTestRule.waitForIdle()
        
        // We should be back to surah list
        composeTestRule.onNodeWithText("سورة الفاتحة", substring = true).assertExists()
        
        // It should NOT be dismissed
        assert(!dismissed) { "Sheet was dismissed instead of going back to surahs" }
    }
}
