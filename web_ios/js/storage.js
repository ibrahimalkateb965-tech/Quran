/**
 * إدارة التخزين المحلي واستعادة الجلسة (Storage Manager)
 */
const STORAGE_KEYS = {
  LAST_SURAH_ID: 'mueen_last_surah_id',
  LAST_AYAH_INDEX: 'mueen_last_ayah_index',
  SELECTED_RECITER_ID: 'mueen_selected_reciter_id',
  CONTINUOUS_PLAY: 'mueen_continuous_play',
  BOOKMARKS: 'mueen_bookmarks'
};

const StorageManager = {
  saveSession(surahId, ayahIndex, reciterId) {
    try {
      localStorage.setItem(STORAGE_KEYS.LAST_SURAH_ID, surahId.toString());
      localStorage.setItem(STORAGE_KEYS.LAST_AYAH_INDEX, ayahIndex.toString());
      if (reciterId) {
        localStorage.setItem(STORAGE_KEYS.SELECTED_RECITER_ID, reciterId);
      }
    } catch (e) {
      console.warn('Storage save failed:', e);
    }
  },

  loadSession() {
    try {
      const surahId = parseInt(localStorage.getItem(STORAGE_KEYS.LAST_SURAH_ID) || '1', 10);
      const ayahIndex = parseInt(localStorage.getItem(STORAGE_KEYS.LAST_AYAH_INDEX) || '0', 10);
      const reciterId = localStorage.getItem(STORAGE_KEYS.SELECTED_RECITER_ID) || 'akhdar';
      return { surahId, ayahIndex, reciterId };
    } catch (e) {
      return { surahId: 1, ayahIndex: 0, reciterId: 'akhdar' };
    }
  },

  saveContinuousPlay(enabled) {
    try {
      localStorage.setItem(STORAGE_KEYS.CONTINUOUS_PLAY, enabled ? 'true' : 'false');
    } catch (e) {}
  },

  loadContinuousPlay() {
    try {
      return localStorage.getItem(STORAGE_KEYS.CONTINUOUS_PLAY) !== 'false';
    } catch (e) {
      return true;
    }
  },

  getBookmarks() {
    try {
      const raw = localStorage.getItem(STORAGE_KEYS.BOOKMARKS);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      return [];
    }
  },

  toggleBookmark(surahId, ayahNumber, surahName) {
    const list = this.getBookmarks();
    const existingIndex = list.findIndex(b => b.surahId === surahId && b.ayahNumber === ayahNumber);
    if (existingIndex >= 0) {
      list.splice(existingIndex, 1);
    } else {
      list.unshift({
        surahId,
        ayahNumber,
        surahName,
        timestamp: Date.now()
      });
    }
    try {
      localStorage.setItem(STORAGE_KEYS.BOOKMARKS, JSON.stringify(list));
    } catch (e) {}
    return this.isBookmarked(surahId, ayahNumber);
  },

  isBookmarked(surahId, ayahNumber) {
    const list = this.getBookmarks();
    return list.some(b => b.surahId === surahId && b.ayahNumber === ayahNumber);
  }
};
