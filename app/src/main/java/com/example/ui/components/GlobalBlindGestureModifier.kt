package com.example.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot

object GlobalBlindGestureState {
    var lastDoubleTapTime: Long = 0L
}

/**
 * A global gesture interceptor that listens for double-taps at the [PointerEventPass.Initial] pass.
 * If a valid double-tap is detected and TalkBack is OFF, it consumes the second tap events to prevent
 * children from reacting, and triggers [onDoubleTap].
 * 
 * Validates against:
 * 1. Accidental drags/scrolls (touch slop).
 * 2. Spatial distance (the two taps must be geographically close).
 */
fun Modifier.interceptBlindDoubleTap(
    isTalkBackEnabled: Boolean,
    doubleTapSlop: Float,
    onDoubleTap: () -> Unit,
    onDragDetected: () -> Unit
): Modifier = this.pointerInput(isTalkBackEnabled) {
    if (isTalkBackEnabled) return@pointerInput

    val maxTouchSlop = viewConfiguration.touchSlop * 2 // Generous slop for blind users

    awaitEachGesture {
        val down1 = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val up1 = waitForUpOrCancellation(pass = PointerEventPass.Initial)
        
        if (up1 != null) {
            val dist1 = hypot(up1.position.x - down1.position.x, up1.position.y - down1.position.y)
            if (dist1 > maxTouchSlop) {
                onDragDetected()
                return@awaitEachGesture // It was a drag/scroll, not a tap
            }

            try {
                withTimeout(viewConfiguration.doubleTapTimeoutMillis) {
                    val down2 = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    
                    // تم إزالة قيود المسافة (spatialDist) لدعم النقر العشوائي الأعمى (Drift Tolerance)
                    
                    val up2 = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (up2 != null) {
                        val dist2 = hypot(up2.position.x - down2.position.x, up2.position.y - down2.position.y)
                        if (dist2 > maxTouchSlop) {
                            onDragDetected()
                            return@withTimeout // Second touch was a drag
                        }
                        
                        // It's a valid double tap! Consume it so children don't trigger.
                        down2.consume()
                        up2.consume()
                        GlobalBlindGestureState.lastDoubleTapTime = System.currentTimeMillis()
                        onDoubleTap()
                    }
                }
            } catch (e: PointerEventTimeoutCancellationException) {
                // Not a double tap (timeout expired), ignore and wait for next gesture
            }
        }
    }
}
