/**
 * منسق واجهة المستخدم والتفاعل الرئيسي لتطبيق معين على الويب والآيفون
 * يدعم التشغيل الفوري للسور (Zero-Delay) وفهرس الآيات (Ayah Index Sheet)
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
    this.ayahModal = null;
    this.reciterModal = null;

    // Pre-built DOM Nodes Cache
    this.surahCardElements = [];
    this.reciterCardElements = [];
    this.ayahCardElements = [];

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
    this.setupVisualViewport();

    // 1. Initialize Subsystems (Eager loading)
    AccessibilityManager.init();
    QuranDataManager.init();
    AudioPlayer.init();

    // 2. Pre-build Static DOM Lists (Zero Layout Thrashing)
    this.buildSurahListDOM();
    this.buildReciterListDOM();

    // 3. Setup Audio Player Callbacks
    AudioPlayer.onStateChange = (isPlaying) => this.handlePlayStateChange(isPlaying);
    AudioPlayer.onAyahChange = (surahId, ayahIndex) => this.handleAyahChange(surahId, ayahIndex);

    // 4. Load Saved Session
    const session = StorageManager.loadSession();
    this.isContinuousPlay = StorageManager.loadContinuousPlay();
    AudioPlayer.isContinuousPlayEnabled = this.isContinuousPlay;
    this.updateContinuousPlayUI();

    const reciter = QuranDataManager.getReciterById(session.reciterId);
    this.selectedReciter = reciter;
    AudioPlayer.selectedReciter = reciter;
    this.updateActiveReciterItem();

    // 5. Load Initial Surah
    await this.loadSurah(session.surahId || 1, session.ayahIndex || 0, false);

    // 6. Register PWA Service Worker
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
    this.ayahModal = document.getElementById('ayah-index-modal');
    this.reciterModal = document.getElementById('reciter-selector-modal');
  }

  setupVisualViewport() {
    if (!window.visualViewport) return;

    const handleViewport = () => {
      const activeModal = document.querySelector('.modal-overlay.open .modal-sheet');
      if (activeModal) {
        const availableHeight = window.visualViewport.height;
        activeModal.style.maxHeight = `${Math.min(availableHeight * 0.88, 650)}px`;
      }
    };

    window.visualViewport.addEventListener('resize', handleViewport);
    window.visualViewport.addEventListener('scroll', handleViewport);
  }

  bindUIEvents() {
    // Single Click on Ayah Card: Toggle Play/Pause
    if (this.ayahCard) {
      this.ayahCard.addEventListener('click', (e) => {
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
      this.ayahNumberBtn.addEventListener('click', () => this.openAyahIndex());
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

    const ayahSearchInput = document.getElementById('ayah-search-input');
    if (ayahSearchInput) {
      ayahSearchInput.addEventListener('input', (e) => this.filterAyahList(e.target.value));
    }

    const reciterSearchInput = document.getElementById('reciter-search-input');
    if (reciterSearchInput) {
      reciterSearchInput.addEventListener('input', (e) => this.filterReciterList(e.target.value));
    }

    // Keyboard Shortcuts
    window.addEventListener('keydown', (e) => {
      if (document.activeElement && ['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) return;
      if (e.code === 'Space') {
        e.preventDefault();
        AudioPlayer.togglePlayPause();
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        AudioPlayer.nextAyah();
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        AudioPlayer.previousAyah();
      } else if (e.key === 'r' || e.key === 'ق') {
        e.preventDefault();
        AudioPlayer.replayCurrentAyah();
      }
    });
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

    if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > this.minSwipeDistance) {
      if (diffX > 0) {
        // Swiped Right -> Previous Ayah
        AudioPlayer.previousAyah();
      } else {
        // Swiped Left -> Next Ayah
        AudioPlayer.nextAyah();
      }
    }
  }

  async loadSurah(surahId, targetAyahIndex = 0, autoPlay = false) {
    this.currentSurahId = surahId;
    const surah = QuranDataManager.getSurahById(surahId);

    // Update Surah Pill immediately
    if (this.surahNameBtn) {
      this.surahNameBtn.textContent = `سورة ${surah.nameArabic}`;
    }

    // Zero-Delay Kickoff: Play audio immediately on user tap before waiting for full json fetch
    if (autoPlay) {
      AudioPlayer.playSurahAyahImmediate(surahId, targetAyahIndex + 1, this.currentAyahs);
    }

    // Fetch / Resolve Ayahs list
    this.currentAyahs = await QuranDataManager.getAyahsForSurah(surahId);
    this.currentAyahIndex = Math.max(0, Math.min(targetAyahIndex, this.currentAyahs.length - 1));

    AudioPlayer.setContext(surahId, this.currentAyahs, this.currentAyahIndex, false);
    this.renderCurrentAyah();
    this.updateActiveSurahItem();

    StorageManager.saveSession(this.currentSurahId, this.currentAyahIndex, this.selectedReciter.id);
  }

  renderCurrentAyah() {
    const currentAyah = this.currentAyahs[this.currentAyahIndex];
    if (!currentAyah) return;

    if (this.ayahCard) {
      this.ayahCard.scrollTop = 0;
    }

    if (this.ayahTextElement) {
      this.ayahTextElement.textContent = currentAyah.textArabic;
    }

    if (this.ayahNumberBtn) {
      this.ayahNumberBtn.textContent = `الآية ${currentAyah.numberInSurah}`;
    }

    if (this.ayahCard) {
      this.ayahCard.setAttribute('aria-label', `الآية ${currentAyah.numberInSurah}`);
    }

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
    const label = `الاستماع المتواصل. حالياً ${this.isContinuousPlay ? 'مفعل' : 'معطل'}`;
    this.continuousPlayBtn.setAttribute('aria-label', label);
    this.continuousPlayBtn.setAttribute('title', label);
    if (this.isContinuousPlay) {
      this.continuousPlayBtn.classList.add('active');
    } else {
      this.continuousPlayBtn.classList.remove('active');
    }
  }

  // ==========================================
  // Pre-built DOM Optimization
  // ==========================================
  buildSurahListDOM() {
    const container = document.getElementById('surah-list-container');
    if (!container) return;
    container.innerHTML = '';
    this.surahCardElements = [];

    const fragment = document.createDocumentFragment();

    SURAH_LIST.forEach(surah => {
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

      card._searchMetadata = {
        nameArabic: surah.nameArabic,
        nameEnglish: surah.nameEnglish.toLowerCase(),
        id: surah.id.toString()
      };

      this.surahCardElements.push({ id: surah.id, element: card, meta: card._searchMetadata });
      fragment.appendChild(card);
    });

    container.appendChild(fragment);
  }

  buildReciterListDOM() {
    const container = document.getElementById('reciter-list-container');
    if (!container) return;
    container.innerHTML = '';
    this.reciterCardElements = [];

    const fragment = document.createDocumentFragment();

    RECITERS_LIST.forEach((reciter, idx) => {
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
        this.updateActiveReciterItem();
        StorageManager.saveSession(this.currentSurahId, this.currentAyahIndex, reciter.id);
        this.closeAllModals();
      });

      card._searchMetadata = {
        nameArabic: reciter.nameArabic,
        nameEnglish: reciter.nameEnglish.toLowerCase(),
        id: (idx + 1).toString()
      };

      this.reciterCardElements.push({ id: reciter.id, element: card, meta: card._searchMetadata });
      fragment.appendChild(card);
    });

    container.appendChild(fragment);
  }

  populateAyahList() {
    const container = document.getElementById('ayah-list-container');
    if (!container) return;
    container.innerHTML = '';
    this.ayahCardElements = [];

    const fragment = document.createDocumentFragment();
    const currentAyahs = this.currentAyahs;

    currentAyahs.forEach((ayah, idx) => {
      const isSelected = idx === this.currentAyahIndex;
      const card = document.createElement('div');
      card.className = `list-item-card ${isSelected ? 'active' : ''}`;
      card.setAttribute('role', 'button');
      card.setAttribute('tabindex', '0');
      card.setAttribute('aria-label', `الآية ${ayah.numberInSurah}`);

      const textSnippet = ayah.textArabic.length > 55 ? ayah.textArabic.substring(0, 55) + '...' : ayah.textArabic;

      card.innerHTML = `
        <div class="item-main-info">
          <span class="item-number">${ayah.numberInSurah}</span>
          <div>
            <div class="item-title">الآية ${ayah.numberInSurah}</div>
            <div class="item-subtitle">${textSnippet}</div>
          </div>
        </div>
      `;

      card.addEventListener('click', () => {
        AudioPlayer.goToAyah(idx, true);
        this.closeAllModals();
      });

      const meta = {
        number: ayah.numberInSurah.toString(),
        text: ayah.textArabic
      };

      this.ayahCardElements.push({ index: idx, element: card, meta });
      fragment.appendChild(card);
    });

    container.appendChild(fragment);
  }

  updateActiveSurahItem() {
    this.surahCardElements.forEach(item => {
      if (item.id === this.currentSurahId) {
        item.element.classList.add('active');
      } else {
        item.element.classList.remove('active');
      }
    });
  }

  updateActiveReciterItem() {
    this.reciterCardElements.forEach(item => {
      if (item.id === this.selectedReciter.id) {
        item.element.classList.add('active');
      } else {
        item.element.classList.remove('active');
      }
    });
  }

  // ==========================================
  // Modal Sheet Handlers
  // ==========================================
  openSurahIndex() {
    AudioPlayer.pause();
    this.updateActiveSurahItem();
    this.filterSurahList('');
    if (this.surahModal) {
      this.surahModal.classList.add('open');
      const search = document.getElementById('surah-search-input');
      if (search) {
        search.value = '';
        setTimeout(() => search.focus(), 80);
      }
    }
    AccessibilityManager.announce('فهرس سور القرآن الكريم');
  }

  filterSurahList(query) {
    const cleanQuery = query.trim().toLowerCase();
    this.surahCardElements.forEach(({ element, meta }) => {
      if (!cleanQuery) {
        element.style.display = 'flex';
      } else {
        const match = meta.nameArabic.includes(cleanQuery) ||
                      meta.nameEnglish.includes(cleanQuery) ||
                      meta.id === cleanQuery;
        element.style.display = match ? 'flex' : 'none';
      }
    });
  }

  openAyahIndex() {
    AudioPlayer.pause();
    this.populateAyahList();
    if (this.ayahModal) {
      this.ayahModal.classList.add('open');
      const search = document.getElementById('ayah-search-input');
      if (search) {
        search.value = '';
        setTimeout(() => search.focus(), 80);
      }
    }
    const surah = QuranDataManager.getSurahById(this.currentSurahId);
    AccessibilityManager.announce(`فهرس آيات سورة ${surah.nameArabic}`);
  }

  filterAyahList(query) {
    const cleanQuery = query.trim().toLowerCase();
    this.ayahCardElements.forEach(({ element, meta }) => {
      if (!cleanQuery) {
        element.style.display = 'flex';
      } else {
        const match = meta.number === cleanQuery || meta.text.includes(cleanQuery);
        element.style.display = match ? 'flex' : 'none';
      }
    });
  }

  openReciterSelector() {
    AudioPlayer.pause();
    this.updateActiveReciterItem();
    this.filterReciterList('');
    if (this.reciterModal) {
      this.reciterModal.classList.add('open');
      const search = document.getElementById('reciter-search-input');
      if (search) {
        search.value = '';
        setTimeout(() => search.focus(), 80);
      }
    }
    AccessibilityManager.announce('قائمة القراء');
  }

  filterReciterList(query) {
    const cleanQuery = query.trim().toLowerCase();
    this.reciterCardElements.forEach(({ element, meta }) => {
      if (!cleanQuery) {
        element.style.display = 'flex';
      } else {
        const match = meta.nameArabic.includes(cleanQuery) ||
                      meta.nameEnglish.includes(cleanQuery) ||
                      meta.id === cleanQuery;
        element.style.display = match ? 'flex' : 'none';
      }
    });
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
