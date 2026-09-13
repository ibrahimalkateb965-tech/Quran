package com.example.domain.repository

import com.aistudio.quranblind.domain.repository.QuranRepository as SharedQuranRepository

/** Android-side alias of the shared read contract. Bookmarks live in BookmarkStore (ADR-005). */
interface QuranRepository : SharedQuranRepository
