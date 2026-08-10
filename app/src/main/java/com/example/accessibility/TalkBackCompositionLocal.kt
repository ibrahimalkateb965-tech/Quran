package com.example.accessibility

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal يحمل حالة TalkBack الحالية.
 */
val LocalTalkBackEnabled = compositionLocalOf { false }
