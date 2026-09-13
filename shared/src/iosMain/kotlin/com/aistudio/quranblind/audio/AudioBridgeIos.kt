package com.aistudio.quranblind.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handle Swift keeps to stop receiving events. */
class AudioEventSubscription internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

// Swift cannot collect a Kotlin Flow; this delivers every event on the main thread.
fun observeAudioEvents(engine: AudioEngine, onEvent: (AudioEngineEvent) -> Unit): AudioEventSubscription {
    val job = CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
        engine.events.collect { onEvent(it) }
    }
    return AudioEventSubscription(job)
}
