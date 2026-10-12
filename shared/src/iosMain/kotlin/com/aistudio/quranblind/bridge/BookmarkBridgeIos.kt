package com.aistudio.quranblind.bridge

import com.aistudio.quranblind.domain.model.Bookmark
import com.aistudio.quranblind.store.BookmarkStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Handle Swift keeps to stop receiving bookmark updates. */
class BookmarkSubscription internal constructor(private val job: Job) {
    fun cancel() {
        job.cancel()
    }
}

// Swift cannot collect a Kotlin Flow; this delivers every bookmark list update on the main thread.
fun observeBookmarks(store: BookmarkStore, onUpdate: (List<Bookmark>) -> Unit): BookmarkSubscription {
    val job = CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
        store.bookmarks.collect { onUpdate(it) }
    }
    return BookmarkSubscription(job)
}
