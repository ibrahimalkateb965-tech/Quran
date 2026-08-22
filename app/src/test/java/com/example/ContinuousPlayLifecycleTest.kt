package com.example

import com.example.ui.viewmodel.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousPlayLifecycleTest {

    @Test
    fun testSettingsUiState_defaultValues() {
        val state = SettingsUiState()
        assertFalse("Continuous play should be disabled by default to save battery", state.isContinuousPlayEnabled)
        assertEquals(1, state.tarkizRepeatMode)
    }

    @Test
    fun testSettingsUiState_toggleContinuousPlay() {
        val initialState = SettingsUiState(isContinuousPlayEnabled = false)
        val updatedState = initialState.copy(isContinuousPlayEnabled = !initialState.isContinuousPlayEnabled)
        
        assertTrue("Toggling continuous play should enable it", updatedState.isContinuousPlayEnabled)
    }
}
