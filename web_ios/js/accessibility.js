/**
 * إدارة إمكانية الوصول وقارئ الشاشة VoiceOver واختصارات لوحة المفاتيح
 */

const AccessibilityManager = {
  liveRegionElement: null,

  init() {
    this.liveRegionElement = document.getElementById('voiceover-live-region');
    if (!this.liveRegionElement) {
      this.liveRegionElement = document.createElement('div');
      this.liveRegionElement.id = 'voiceover-live-region';
      this.liveRegionElement.className = 'sr-only';
      this.liveRegionElement.setAttribute('aria-live', 'polite');
      this.liveRegionElement.setAttribute('aria-atomic', 'true');
      document.body.appendChild(this.liveRegionElement);
    }

    this.bindKeyboardShortcuts();
  },

  announce(message) {
    if (!message || !this.liveRegionElement) return;
    
    // Clear and set to force VoiceOver to re-announce identical consecutive strings
    this.liveRegionElement.textContent = '';
    setTimeout(() => {
      this.liveRegionElement.textContent = message;
    }, 50);
  },

  bindKeyboardShortcuts() {
    window.addEventListener('keydown', (e) => {
      // Don't intercept when typing in search input
      if (e.target && (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA')) {
        return;
      }

      switch (e.key) {
        case ' ': // Space: Play/Pause
          e.preventDefault();
          if (window.AudioPlayer) {
            AudioPlayer.togglePlayPause();
            const stateText = AudioPlayer.isPlaying ? 'جاري التشغيل' : 'متوقف مؤقتاً';
            this.announce(stateText);
          }
          break;

        case 'ArrowLeft': // Next Ayah (in RTL layout)
          e.preventDefault();
          if (window.AudioPlayer) {
            AudioPlayer.nextAyah();
          }
          break;

        case 'ArrowRight': // Previous Ayah (in RTL layout)
          e.preventDefault();
          if (window.AudioPlayer) {
            AudioPlayer.previousAyah();
          }
          break;

        case 'r':
        case 'R':
        case 'ق': // Repeat
          e.preventDefault();
          if (window.AudioPlayer) {
            AudioPlayer.replayCurrentAyah();
            this.announce('إعادة تلاوة الآية');
          }
          break;

        case 'Escape': // Close modals
          if (window.App) {
            window.App.closeAllModals();
          }
          break;
      }
    });
  }
};
