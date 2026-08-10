package com.example.accessibility

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * CompositionLocal يحمل حالة TalkBack الحالية.
 */
val LocalTalkBackEnabled = compositionLocalOf { false }

class PendingBlindActionManager(private val scope: CoroutineScope) {
    var action by mutableStateOf<(() -> Unit)?>(null)
        private set
    var fallbackAction by mutableStateOf<(() -> Unit)?>(null)
        private set
    private var timeoutJob: Job? = null

    fun registerAction(newAction: () -> Unit) {
        action = newAction
        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(15000) // Expiry timeout
            action = null
        }
    }

    fun setFallback(fallback: () -> Unit) {
        fallbackAction = fallback
    }

    fun clear() {
        action = null
        timeoutJob?.cancel()
    }

    fun execute() {
        if (action != null) {
            action?.invoke()
        } else {
            fallbackAction?.invoke()
        }
    }
}

/**
 * CompositionLocal يحمل الإجراء (Action) الأخير الذي تم التركيز عليه.
 */
val LocalPendingBlindAction = compositionLocalOf<PendingBlindActionManager> {
    error("LocalPendingBlindAction not provided")
}
