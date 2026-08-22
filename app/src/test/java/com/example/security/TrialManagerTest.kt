package com.example.security

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrialManagerTest {

    private lateinit var context: Context
    private lateinit var trialManager: TrialManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        trialManager = TrialManager.getInstance(context)
    }

    @Test
    fun testValidPermanentUnlockPin() {
        val result = trialManager.unlockPermanently("998877")
        assertTrue("Permanent unlock PIN should return true for valid code", result)
    }

    @Test
    fun testInvalidPermanentUnlockPin() {
        val result = trialManager.unlockPermanently("000000")
        assertFalse("Permanent unlock PIN should return false for invalid code", result)
    }

    @Test
    fun testValidExtensionPin() {
        val result = trialManager.extendTrial("112233")
        assertTrue("Extension PIN should succeed for valid code", result)
    }

    @Test
    fun testReusedPinFails() {
        trialManager.extendTrial("112233")
        val secondAttempt = trialManager.extendTrial("112233")
        assertFalse("Reusing the same extension PIN should fail", secondAttempt)
    }
}
