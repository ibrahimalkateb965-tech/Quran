/**
 * منسق واجهة المستخدم والتفاعل الرئيسي لتطبيق معين على الويب والآيفون
 */

class MueenApp {
  constructor() {
    this.currentSurahId = 1;
    this.currentAyahs = [];
    this.currentAyahIndex = 0;
    this.selectedReciter = RECITERS_LIST[0];
    this.isContinuousPlay = true;

    // DOM Elements
    this.surahNameBtn = null;
    this.ayahNumberBtn = null;
    this.ayahCard = null;
    this.ayahTextElement = null;
    this.equalizerContainer = null;
    this.repeatHintBtn = null;
    this.continuousPlayBtn = null;
    this.recitersBtn = null;
    this.surahsBtn = null;

    // Modals
    this.surahModal = null;
    this.reciterModal = null;
    this.bookmarksModal = null;

    // Touch Swipe Tracking
    this.touchStartX = 0;
    this.touchStartY = 0;
    this.touchEndX = 0;
    this.touchEndY = 0;
    this.minSwipeDistance = 45;
  }

  async init() {
    this.cacheElements();
    this.bindUIEvents();
    this.setupTouchGestures();

    // 1. Initialize Subsystems
    AccessibilityManager.init();
    await QuranDataManager.init();
    AudioPlayer.init();

    // 2. Setup Audio Player Callbacks
    AudioPlayer.onStateChange = (isPlaying) => this.handlePlayStateChange(isPlaying);
    AudioPlayer.onAyahChange = (surahId, ayahIndex) => this.handleAyahChange(surahId, ayahIndex);

    // 3. Load Saved Session
    const session = StorageManager.loadSession();
    this.isContinuousPlay = StorageManager.loadContinuousPlay();
    AudioPlayer.isContinuousPlayEnabled = this.isContinuousPlay;
    this.updateContinuousPlayUI();

    const reciter = QuranDataManager.getReciterById(session.reciterId);
    this.selectedReciter = reciter;
    AudioPlayer.selectedReciter = reciter;

    // 4. Load Initial Surah
    await this.loadSurah(session.surahId || 1, session.ayahIndex || 0, false);

    // 5. Register PWA Service Worker
    this.registerServiceWorker();
  }

  cacheElements() {
    this.surahNameBtn = document.getElementById('surah-name-pill');
    this.ayahNumberBtn = document.getElementById('ayah-number-pill');
    this.ayahCard = document.getElementById('main-ayah-card');
    this.ayahTextElement = document.getElementById('ayah-text-content');
    this.equalizerContainer = document.getElementById('equalizer-bars');
    this.repeatHintBtn = document.getElementById('repeat-hint-action');
    this.continuousPlayBtn = document.getElementById('btn-continuous-play');
    this.recitersBtn = document.getElementById('btn-reciters');
    this.surahsBtn = document.getElementById('btn-surahs');

    this.surahModal = document.getElementById('surah-index-modal');
    this.reciterModal = document.getElementById('reciter-selector-modal');
    this.bookmarksModal = document.getElementById('bookmarks-modal');
  }

  bindUIEvents() {
    // Single Click on Ayah Card: Toggle Play/Pause
    if (this.ayahCard) {
      this.ayahCard.addEventListener('click', (e) => {
        // Prevent toggle if clicking on edge navigation buttons
        if (e.target.closest('.nav-edge-btn')) return;
        AudioPlayer.togglePlayPause();
      });
    }

    // Edge Navigation Buttons
    const prevEdgeBtn = document.getElementById('btn-edge-prev');
    const nextEdgeBtn = document.getElementById('btn-edge-next');
    if (prevEdgeBtn) prevEdgeBtn.addEventListener('click', (e) => { e.stopPropagation(); AudioPlayer.previousAyah(); });
    if (nextEdgeBtn) nextEdgeBtn.addEventListener('click', (e) => { e.stopPropagation(); AudioPlayer.nextAyah(); });

    // Click on Repeat Hint: Replay Current Ayah
    if (this.repeatHintBtn) {
      this.repeatHintBtn.addEventListener('click', () => {
        AudioPlayer.replayCurrentAyah();
        AccessibilityManager.announce('إعادة تلاوة الآية');
      });
    }

    // Header Actions
    if (this.surahNameBtn) {
      this.surahNameBtn.addEventListener('click', () => this.openSurahIndex());
    }
    if (this.ayahNumberBtn) {
      this.ayahNumberBtn.addEventListener('click', () => {
        const surah = QuranDataManager.getSurahById(this.currentSurahId);
        const ayah = this.currentAyahs[this.currentAyahIndex];
        const num = ayah ? ayah.numberInSurah : 1;
        AccessibilityManager.announce(`سورة ${surah.nameArabic}، الآية ${num} من أصل ${surah.ayahCount}`);
      });
    }
    if (this.surahsBtn) {
      this.surahsBtn.addEventListener('click', () => this.openSurahIndex());
    }
    if (this.recitersBtn) {
      this.recitersBtn.addEventListener('click', () => this.openReciterSelector());
    }
    if (this.continuousPlayBtn) {
      this.continuousPlayBtn.addEventListener('click', () => this.toggleContinuousPlay());
    }

    // Modal Close buttons
    document.querySelectorAll('.modal-close-btn').forEach(btn => {
      btn.addEventListener('click', () => this.closeAllModals());
    });

    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) this.closeAllModals();
      });
    });

    // Modal Searches
    const surahSearchInput = document.getElementById('surah-search-input');
    if (surahSearchInput) {
      surahSearchInput.addEventListener('input', (e) => this.filterSurahList(e.target.value));
    }

    const reciterSearchInput = document.getElementById('reciter-search-input');
    if (reciterSearchInput) {
      reciterSearchInput.addEventListener('input', (e) => this.filterReciterList(e.target.value));
    }
  }

  setupTouchGestures() {
    if (!this.ayahCard) return;

    this.ayahCard.addEventListener('touchstart', (e) => {
      this.touchStartX = e.changedTouches[0].screenX;
      this.touchStartY = e.changedTouches[0].screenY;
    }, { passive: true });

    this.ayahCard.addEventListener('touchend', (e) => {
      this.touchEndX = e.changedTouches[0].screenX;
      this.touchEndY = e.changedTouches[0].screenY;
      this.handleSwipeGesture();
    }, { passive: true });
  }

  handleSwipeGesture() {
    const diffX = this.touchEndX - this.touchStartX;
    const diffY = this.touchEndY - this.touchStartY;

    // Only process horizontal swipes
    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > this.minSwipeDistance) {
      if (diffX > 0) {
        // Swiped Right -> In RTL, this is Previous Ayah
        AudioPlayer.previousAyah();
      } else {
        // Swiped Left -> In RTL, this is Next Ayah
        AudioPlayer.nextAyah();
      }
    }
  }

  async loadSurah(surahId, targetAyahIndex = 0, autoPlay = false) {
    this.currentSurahId = surahId;
    const surah = QuranDataManager.getSurahById(surahId);

    // Update Surah Pill
    if (this.surahNameBtn) {
      this.surahNameBtn.textContent = `سورة ${surah.nameArabic}`;
    }

    // Fetch Ayahs
    this.currentAyahs = await QuranDataManager.getAyahsForSurah(surahId);
    this.currentAyahIndex = Math.max(0, Math.min(targetAyahIndex, this.currentAyahs.length - 1));

    AudioPlayer.setContext(surahId, this.currentAyahs, this.currentAyahIndex, autoPlay);
    this.renderCurrentAyah();

    StorageManager.saveSession(this.currentSurahId, this.currentAyahIndex, this.selectedReciter.id);
  }

  renderCurrentAyah() {
    const currentAyah = this.currentAyahs[this.currentAyahIndex];
    if (!currentAyah) return;

    // Update Ayah Text
    if (this.ayahTextElement) {
      this.ayahTextElement.textContent = currentAyah.textArabic;
    }

    // Update Ayah Pill
    if (this.ayahNumberBtn) {
      this.ayahNumberBtn.textContent = `الآية ${currentAyah.numberInSurah}`;
    }

    // Accessibility Announcement
    const surah = QuranDataManager.getSurahById(this.currentSurahId);
    AccessibilityManager.announce(`سورة ${surah.nameArabic}، الآية ${currentAyah.numberInSurah}`);
  }

  handlePlayStateChange(isPlaying) {
    if (this.ayahCard) {
      if (isPlaying) {
        this.ayahCard.classList.add('playing');
        if (this.equalizerContainer) this.equalizerContainer.style.display = 'flex';
      } else {
        this.ayahCard.classList.remove('playing');
        if (this.equalizerContainer) this.equalizerContainer.style.display = 'none';
      }
    }
  }

  handleAyahChange(surahId, ayahIndex) {
    this.currentAyahIndex = ayahIndex;
    this.renderCurrentAyah();
    StorageManager.saveSession(this.currentSurahId, this.currentAyahIndex, this.selectedReciter.id);
  }

  toggleContinuousPlay() {
    this.isContinuousPlay = !this.isContinuousPlay;
    AudioPlayer.isContinuousPlayEnabled = this.isContinuousPlay;
    StorageManager.saveContinuousPlay(this.isContinuousPlay);
    this.updateContinuousPlayUI();
    const msg = this.isContinuousPlay ? 'تم تفعيل التلاوة المتواصلة' : 'تم إيقاف التلاوة المتواصلة';
    AccessibilityManager.announce(msg);
  }

  updateContinuousPlayUI() {
    if (!this.continuousPlayBtn) return;
    if (this.isContinuousPlay) {
      this.continuousPlayBtn.classList.add('active');
    } else {
      this.continuousPlayBtn.classList.remove('active');
    }
  }

  // ==========================================
  // Modal Sheet Handlers
  // ==========================================
  openSurahIndex() {
    AudioPlayer.pause();
    this.populateSurahList(SURAH_LIST);
    if (this.surahModal) {
      this.surahModal.classList.add('open');
      const search = document.getElementById('surah-search-input');
      if (search) { search.value = ''; search.focus(); }
    }
    AccessibilityManager.announce('فهرس سور القرآن الكريم');
  }

  populateSurahList(list) {
    const container = document.getElementById('surah-list-container');
    if (!container) return;
    container.innerHTML = '';

    list.forEach(surah => {
      const card = document.createElement('div');
      card.className = `list-item-card ${surah.id === this.currentSurahId ? 'active' : ''}`;
      card.setAttribute('role', 'button');
      card.setAttribute('tabindex', '0');
      card.setAttribute('aria-label', `سورة ${surah.nameArabic}، ${surah.ayahCount} آيات، ${surah.revelationType}`);

      card.innerHTML = `
        <div class="item-main-info">
          <span class="item-number">${surah.id}</span>
          <div>
            <div class="item-title">سورة ${surah.nameArabic}</div>
            <div class="item-subtitle">${surah.nameEnglish} • ${surah.revelationType}</div>
          </div>
        </div>
        <div class="item-subtitle">${surah.ayahCount} آيات</div>
      `;

      card.addEventListener('click', () => {
        this.loadSurah(surah.id, 0, true);
        this.closeAllModals();
      });

      container.appendChild(card);
    });
  }

  filterSurahList(query) {
    const cleanQuery = query.trim().toLowerCase();
    if (!cleanQuery) {
      this.populateSurahList(SURAH_LIST);
      return;
    }
    const filtered = SURAH_LIST.filter(s =>
      s.nameArabic.includes(cleanQuery) ||
      s.nameEnglish.toLowerCase().includes(cleanQuery) ||
      s.id.toString() === cleanQuery
    );
    this.populateSurahList(filtered);
  }

  openReciterSelector() {
    AudioPlayer.pause();
    this.populateReciterList(RECITERS_LIST);
    if (this.reciterModal) {
      this.reciterModal.classList.add('open');
      const search = document.getElementById('reciter-search-input');
      if (search) { search.value = ''; search.focus(); }
    }
    AccessibilityManager.announce('قائمة القراء');
  }

  populateReciterList(list) {
    const container = document.getElementById('reciter-list-container');
    if (!container) return;
    container.innerHTML = '';

    list.forEach((reciter, idx) => {
      const isSelected = reciter.id === this.selectedReciter.id;
      const card = document.createElement('div');
      card.className = `list-item-card ${isSelected ? 'active' : ''}`;
      card.setAttribute('role', 'button');
      card.setAttribute('tabindex', '0');
      card.setAttribute('aria-label', `القارئ ${reciter.nameArabic}`);

      card.innerHTML = `
        <div class="item-main-info">
          <span class="item-number">${idx + 1}</span>
          <div>
            <div class="item-title">${reciter.nameArabic}</div>
            <div class="item-subtitle">${reciter.nameEnglish}</div>
          </div>
        </div>
      `;

      card.addEventListener('click', () => {
        this.selectedReciter = reciter;
        AudioPlayer.setReciter(reciter, true);
        StorageManager.saveSession(this.currentSurahId, this.currentAyahIndex, reciter.id);
        this.closeAllModals();
      });

      container.appendChild(card);
    });
  }

  filterReciterList(query) {
    const cleanQuery = query.trim().toLowerCase();
    if (!cleanQuery) {
      this.populateReciterList(RECITERS_LIST);
      return;
    }
    const filtered = RECITERS_LIST.filter(r =>
      r.nameArabic.includes(cleanQuery) ||
      r.nameEnglish.toLowerCase().includes(cleanQuery)
    );
    this.populateReciterList(filtered);
  }

  closeAllModals() {
    document.querySelectorAll('.modal-overlay').forEach(modal => {
      modal.classList.remove('open');
    });
  }

  registerServiceWorker() {
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('sw.js').catch(err => {
          console.warn('ServiceWorker registration error:', err);
        });
      });
    }
  }
}

// Global App Launcher
window.addEventListener('DOMContentLoaded', () => {
  window.App = new MueenApp();
  window.App.init();
});
