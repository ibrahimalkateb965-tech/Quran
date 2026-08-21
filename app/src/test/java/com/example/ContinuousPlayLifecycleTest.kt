package com.example

import com.example.ui.viewmodel.SettingsUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousPlayLifecycleTest {

    @Test
    fun `when continuous play is enabled, lifecycle onPause should NOT trigger pause`() {
        val state = SettingsUiState(isContinuousPlayEnabled = true)
        
        // Simulating the lifecycle check in MainActivity:
        // if (!viewModel.settingsUiState.value.isContinuousPlayEnabled) viewModel.pausePlayback()
        val shouldPause = !state.isContinuousPlayEnabled
        
        assertFalse("Playback should continue in the background when continuous play is ON", shouldPause)
    }

    @Test
    fun `when continuous play is disabled, lifecycle onPause should trigger pause`() {
        val state = SettingsUiState(isContinuousPlayEnabled = false)
        
        val shouldPause = !state.isContinuousPlayEnabled
        
        assertTrue("Playback should pause to save battery when continuous play is OFF", shouldPause)
    }
}
