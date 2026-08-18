/**
 * مشغل الصوتيات المركزي المتوافق مع متصفح Safari و iOS MediaSession
 * يدعم محرك التحميل المسبق (Gapless Audio Prefetch Buffer) لإلغاء أي تأخير بين الآيات
 */

class AudioPlayerEngine {
  constructor() {
    this.audioElement = null;
    this.prefetchAudioElement = null;
    this.isUnlocked = false;
    this.currentSurahId = 1;
    this.currentAyahIndex = 0;
    this.currentAyahsList = [];
    this.selectedReciter = RECITERS_LIST[0];
    this.isContinuousPlayEnabled = true;
    this.playbackSessionId = 0;
    this.isPlaying = false;

    // UI Callbacks
    this.onStateChange = null;
    this.onAyahChange = null;
  }

  init() {
    this.audioElement = document.getElementById('quran-audio-core');
    if (!this.audioElement) {
      this.audioElement = document.createElement('audio');
      this.audioElement.id = 'quran-audio-core';
      this.audioElement.preload = 'auto';
      this.audioElement.playsInline = true;
      document.body.appendChild(this.audioElement);
    }

    // Prefetch audio element for gapless continuous playback
    this.prefetchAudioElement = document.createElement('audio');
    this.prefetchAudioElement.preload = 'auto';
    this.prefetchAudioElement.muted = true;
    this.prefetchAudioElement.playsInline = true;

    this.bindEvents();
    this.setupMediaSession();
    this.setupUnlockPriming();
  }

  setupUnlockPriming() {
    const unlockHandler = () => {
      if (this.isUnlocked) return;
      this.isUnlocked = true;
      // Safari Audio Unlock: play & pause to grant uninterrupted background playback
      const playPromise = this.audioElement.play();
      if (playPromise !== undefined) {
        playPromise.then(() => {
          this.audioElement.pause();
        }).catch(() => {
          this.audioElement.load();
        });
      }
      window.removeEventListener('touchstart', unlockHandler, true);
      window.removeEventListener('click', unlockHandler, true);
    };

    window.addEventListener('touchstart', unlockHandler, true);
    window.addEventListener('click', unlockHandler, true);
  }

  bindEvents() {
    this.audioElement.addEventListener('play', () => {
      this.isPlaying = true;
      if (this.onStateChange) this.onStateChange(true);
      this.updateMediaSession();
    });

    this.audioElement.addEventListener('pause', () => {
      this.isPlaying = false;
      if (this.onStateChange) this.onStateChange(false);
    });

    this.audioElement.addEventListener('ended', () => {
      this.handleTrackEnded();
    });

    this.audioElement.addEventListener('error', (e) => {
      console.warn('Audio playback error encountered:', e);
      this.isPlaying = false;
      if (this.onStateChange) this.onStateChange(false);
    });

    // Handle iOS page visibility transitions smoothly
    document.addEventListener('visibilitychange', () => {
      if (!document.hidden && this.isPlaying) {
        this.updateMediaSession();
      }
    });
  }

  setupMediaSession() {
    if (!('mediaSession' in navigator)) return;

    try {
      navigator.mediaSession.setActionHandler('play', () => this.play());
      navigator.mediaSession.setActionHandler('pause', () => this.pause());
      navigator.mediaSession.setActionHandler('previoustrack', () => this.previousAyah(true));
      navigator.mediaSession.setActionHandler('nexttrack', () => this.nextAyah(true));
    } catch (err) {
      console.warn('MediaSession handler setup failed:', err);
    }
  }

  updateMediaSession() {
    if (!('mediaSession' in navigator)) return;

    const surah = QuranDataManager.getSurahById(this.currentSurahId);
    const currentAyah = this.currentAyahsList[this.currentAyahIndex];
    const ayahNum = currentAyah ? currentAyah.numberInSurah : (this.currentAyahIndex + 1);

    navigator.mediaSession.metadata = new MediaMetadata({
      title: `سورة ${surah.nameArabic} - الآية ${ayahNum}`,
      artist: this.selectedReciter.nameArabic,
      album: 'القرآن الكريم - معين',
      artwork: [
        { src: 'assets/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
        { src: 'assets/icons/icon-512.png', sizes: '512x512', type: 'image/png' }
      ]
    });

    navigator.mediaSession.playbackState = this.isPlaying ? 'playing' : 'paused';
  }

  setContext(surahId, ayahsList, startIndex = 0, autoPlay = false) {
    this.currentSurahId = surahId;
    this.currentAyahsList = ayahsList;
    this.currentAyahIndex = Math.max(0, Math.min(startIndex, ayahsList.length - 1));

    if (autoPlay) {
      this.loadAndPlayCurrentAyah();
    }
  }

  setReciter(reciter, restartPlay = false) {
    this.selectedReciter = reciter;
    if (this.isPlaying || restartPlay) {
      this.loadAndPlayCurrentAyah();
    }
  }

  togglePlayPause() {
    if (this.isPlaying) {
      this.pause();
    } else {
      this.play();
    }
  }

  play() {
    if (!this.audioElement.src || this.audioElement.src === window.location.href) {
      this.loadAndPlayCurrentAyah();
      return;
    }
    const playPromise = this.audioElement.play();
    if (playPromise !== undefined) {
      playPromise.catch(err => {
        console.warn('Playback play() was rejected by browser:', err);
        this.loadAndPlayCurrentAyah();
      });
    }
  }

  pause() {
    if (this.audioElement) {
      this.audioElement.pause();
    }
  }

  replayCurrentAyah() {
    this.loadAndPlayCurrentAyah();
  }

  loadAndPlayCurrentAyah() {
    const sessionId = ++this.playbackSessionId;
    const currentAyah = this.currentAyahsList[this.currentAyahIndex];
    if (!currentAyah) return;

    const audioUrl = QuranDataManager.getAudioUrl(
      this.selectedReciter.serverIdentifier,
      this.currentSurahId,
      currentAyah.numberInSurah
    );

    this.audioElement.src = audioUrl;
    this.audioElement.load();

    const playPromise = this.audioElement.play();
    if (playPromise !== undefined) {
      playPromise.then(() => {
        if (this.playbackSessionId !== sessionId) {
          // A newer request has replaced this one
          this.audioElement.pause();
          return;
        }
        this.isPlaying = true;
        if (this.onStateChange) this.onStateChange(true);
        this.updateMediaSession();

        // Prefetch upcoming track in buffer to avoid network silence
        this.prefetchNextTrack();
      }).catch(err => {
        if (this.playbackSessionId === sessionId) {
          console.warn('Audio play() failed for URL:', audioUrl, err);
          this.isPlaying = false;
          if (this.onStateChange) this.onStateChange(false);
        }
      });
    }

    if (this.onAyahChange) {
      this.onAyahChange(this.currentSurahId, this.currentAyahIndex);
    }
  }

  prefetchNextTrack() {
    if (!this.isContinuousPlayEnabled || !this.prefetchAudioElement) return;

    let nextSurahId = this.currentSurahId;
    let nextAyahNum = 1;

    if (this.currentAyahIndex < this.currentAyahsList.length - 1) {
      const nextAyah = this.currentAyahsList[this.currentAyahIndex + 1];
      nextAyahNum = nextAyah ? nextAyah.numberInSurah : (this.currentAyahIndex + 2);
    } else if (this.currentSurahId < 114) {
      nextSurahId = this.currentSurahId + 1;
      nextAyahNum = 1;
    } else {
      return;
    }

    const nextAudioUrl = QuranDataManager.getAudioUrl(
      this.selectedReciter.serverIdentifier,
      nextSurahId,
      nextAyahNum
    );

    this.prefetchAudioElement.src = nextAudioUrl;
    this.prefetchAudioElement.load();
  }

  goToAyah(index, autoPlay = true) {
    if (index < 0 || index >= this.currentAyahsList.length) return;
    this.currentAyahIndex = index;
    if (autoPlay) {
      this.loadAndPlayCurrentAyah();
    } else {
      if (this.onAyahChange) {
        this.onAyahChange(this.currentSurahId, this.currentAyahIndex);
      }
    }
  }

  nextAyah(forceAutoPlay = null) {
    const shouldPlay = forceAutoPlay !== null ? forceAutoPlay : this.isPlaying;
    if (this.currentAyahIndex < this.currentAyahsList.length - 1) {
      this.goToAyah(this.currentAyahIndex + 1, shouldPlay);
    } else if (this.isContinuousPlayEnabled && this.currentSurahId < 114) {
      // Advance to next Surah
      if (window.App) {
        window.App.loadSurah(this.currentSurahId + 1, 0, shouldPlay);
      }
    }
  }

  previousAyah(forceAutoPlay = null) {
    const shouldPlay = forceAutoPlay !== null ? forceAutoPlay : this.isPlaying;
    if (this.currentAyahIndex > 0) {
      this.goToAyah(this.currentAyahIndex - 1, shouldPlay);
    } else if (this.isContinuousPlayEnabled && this.currentSurahId > 1) {
      // Go to previous Surah
      if (window.App) {
        window.App.loadSurah(this.currentSurahId - 1, 0, shouldPlay);
      }
    }
  }

  handleTrackEnded() {
    if (this.isContinuousPlayEnabled) {
      // Browser automatically pauses on ended, so force autoPlay = true to continue playback immediately
      this.nextAyah(true);
    } else {
      this.isPlaying = false;
      if (this.onStateChange) this.onStateChange(false);
    }
  }
}

const AudioPlayer = new AudioPlayerEngine();
