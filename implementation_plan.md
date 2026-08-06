# Implementation Plan: TalkBack Detection & Audio Prompt at Launch

## Architectural Review (Verdict)

**Validated: Option 2 (Firm Audio Prompt) is the only viable strategy.** Android provides no API for third-party apps to disable accessibility services — confirmed correct. No `intent`/`Settings` hack can force-close TalkBack either (accessibility toggle requires explicit user action in system Settings).

**⚠️ CRITICAL TRAP FOUND in current code — naive implementation will silently fail:**
- `SpeechManager.speak()` (SpeechManager.kt:86) **no-ops when TalkBack is enabled**: `if (_isTalkBackEnabled.value) return`
- Worse: the internal TTS engine is **never even initialized** if TalkBack is on at startup (line 44).

→ A warning sent through `SpeechManager` would be swallowed **exactly when it's needed most**.

**The correct channel already exists:** `ViewModel.announce()` pushes to `_announcementEvent` (a `Channel.BUFFERED`), and `QuranPlayerScreen` collects it and routes to `announceForAccessibility(context, msg)` when TalkBack is on. **We use TalkBack itself to tell the user to disable TalkBack.** Zero new infrastructure, guaranteed delivery (buffered channel survives the trial-check delay before composition starts collecting).

**UX refinements to the message:**
- The volume-keys shortcut only works if the user previously assigned TalkBack to the accessibility shortcut — add a Settings fallback to the wording.
- The prompt is advisory only. If the user ignores it, the app continues in its existing TalkBack-compatible dual mode (SpeechManager defers to `announceForAccessibility`). No gating, no blocking dialog — correct for an accessibility app.

---

## Implementation (1 file, ~10 lines)

### `QuranViewModel.kt` — init block only

Add AFTER the `loadSurah(1, autoPlay = false)` fresh-session line (from the lifecycle plan), replacing/merging with the "تم فتح فهرس السور" announcement:

```kotlin
// Fresh session: always start at Surah 1 (no position restore).
loadSurah(1, autoPlay = false)

// TalkBack detection: prompt user to switch to the internal voice assistant.
// announce() routes via announcementEvent -> announceForAccessibility when TalkBack is ON
// (SpeechManager.speak() is a no-op in that state BY DESIGN — do NOT call speak() directly).
if (speechManager.isTalkBackEnabled()) {
    announce(
        "قارئ الشاشة الخاص بالهاتف يعمل الآن. " +
        "لتجربة أفضل مع المساعد الصوتي الخاص بالتطبيق، يُفضّل إيقاف قارئ الشاشة " +
        "بالضغط المطول على زري رفع وخفض الصوت معاً، " +
        "أو من إعدادات الهاتف، إمكانية الوصول، ثم TalkBack."
    )
} else {
    announce("تم فتح فهرس السور")
}
```

**Why `init` is the correct trigger point:**
- Fresh process launch = new ViewModel = prompt fires. ✅
- Rotation / config change = ViewModel survives = NO re-prompt. ✅
- Background → foreground = ViewModel survives = NO re-prompt. ✅
- Matches the session-lifecycle plan's definition of "new session" exactly.

### No other files change
- `MainActivity.kt` — untouched (detection lives in ViewModel; ViewModel is created by `viewModels()` on fresh process).
- `SpeechManager.kt` — untouched (its TalkBack guard is intentional and this plan relies on it).
- `QuranPlayerScreen.kt` — untouched (existing `announcementEvent` collector handles delivery).
- No new `AccessibilityHelper` needed — `speechManager.isTalkBackEnabled()` (synchronous, `AccessibilityManager.isEnabled && isTouchExplorationEnabled`) already exists at SpeechManager.kt:97.

### Optional (Phase 2 — only if user requests re-prompting)
Re-announce when TalkBack is enabled **mid-session**:
```kotlin
// in init:
viewModelScope.launch {
    speechManager.isTalkBackEnabledFlow
        .drop(1)              // skip initial value (already handled above)
        .filter { it }        // only when TalkBack turns ON
        .collect { announce("...same warning...") }
}
```
Skip by default — once-per-session is the requirement.

---

## QA Checklist

- [ ] `compileDebugKotlin` passes.
- [ ] TalkBack ON + cold launch → TalkBack speaks the warning (in Arabic) after the screen loads; internal TTS stays silent (no double voice).
- [ ] TalkBack OFF + cold launch → internal TTS says "تم فتح فهرس السور"; no warning.
- [ ] TalkBack ON + rotate device → warning does NOT repeat.
- [ ] TalkBack ON + Home → return → warning does NOT repeat.
- [ ] Swipe-kill → relaunch with TalkBack ON → warning fires again (new session).
- [ ] User ignores warning → app fully functional via TalkBack (existing dual-mode: `announceForAccessibility` path).
- [ ] Warning is NOT swallowed when fired before composition (Channel.BUFFERED delivers to the collector once `QuranPlayerScreen` subscribes post-trial-check).

## Files Touched
1. `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` — init block only (~10 lines)

**Total: 1 file. No new classes, no manifest changes, no permission changes.**
