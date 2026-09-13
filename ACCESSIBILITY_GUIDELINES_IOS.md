# ACCESSIBILITY_GUIDELINES_IOS.md
## Jetpack Compose (TalkBack) → Flutter (VoiceOver) Architectural Mapping
### Blind Quran App — `com.aistudio.quranblind.a11y`

> **Scope.** This document is the normative reference for porting the accessibility
> architecture of the Android app (`app/src/main/java/com/example/**`) to the Flutter
> iOS target. It is a *behavioural contract*, not a style guide: every row in the
> mapping tables corresponds to real code in the Kotlin tree, and every Flutter
> counterpart is expected to be verifiable by a widget test or a device checklist item.

---

## 0. The One Thing That Must Not Be Lost

This app is not "an app that supports a screen reader". It is an app whose **primary
input device is the screen reader itself**, and whose primary output is *audio that
competes with the screen reader*. Two consequences drive the entire architecture:

1. **The UI must disappear from the accessibility tree.** On Android this is done with
   `clearAndSetSemantics` wrapping a `HorizontalPager` (the *Stealth Box*, at
   `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt:287-327`). A blind
   user must never have to hunt for a "play" button — the *entire screen* is one
   actionable node. The single largest porting risk is a Flutter tree that faithfully
   exposes every `Text` and `IconButton` and thereby destroys the interaction model.

2. **Two speech engines must never talk at once.** `SpeechManager` shuts down its own
   `TextToSpeech` the moment TalkBack turns on (`SpeechManager.kt:52-58`) and delegates
   to `announceForAccessibility`. iOS has the same hazard with a different shape:
   AVAudioSession options that duck "other" audio will duck **VoiceOver**.

Everything below serves these two invariants.

---

## 1. Architectural Invariants (must hold on both platforms)

| # | Invariant | Android enforcement | iOS enforcement |
|---|---|---|---|
| **I1** | Exactly one speech source is active at any instant. | `SpeechManager` stops TTS when TalkBack enabled | App TTS layer disabled when `accessibleNavigation == true`; recitation is *not* speech synthesis and is allowed to coexist |
| **I2** | The recitation surface exposes ≤ 1 accessibility node. | `clearAndSetSemantics` on the pager `Box` | `Semantics(container: true)` + `ExcludeSemantics` on the subtree |
| **I3** | Screen-reader state is read *reactively*, never sampled once. | `LocalTalkBackEnabled` CompositionLocal fed by `AccessibilityManager` listeners | `MediaQuery.accessibleNavigationOf(context)` (rebuilds on change) |
| **I4** | Playback never starts without an explicit user act. | `MediaButtonPolicy.shouldConsume` + `CallEndTracker`, 2000 ms window | `AVAudioSession` interruption `.shouldResume` suppression + `CXCallObserver`, same 2000 ms window |
| **I5** | Every state change a sighted user sees is announced to a blind user. | `liveRegion = Assertive` + `announceForAccessibility` | `SemanticsService.announce` with a serialised queue (see §6) |
| **I6** | Audio content type is *speech*, not music. | `C.AUDIO_CONTENT_TYPE_SPEECH` + `C.USAGE_MEDIA` | `AVAudioSession(.playback, mode: .spokenAudio)` |

---

## 2. Core Semantics Mapping

### 2.1 Tree-shaping primitives

| Jetpack Compose | Flutter | iOS/VoiceOver effect | Notes |
|---|---|---|---|
| `Modifier.clearAndSetSemantics { … }` | `Semantics(container: true, …, child: ExcludeSemantics(child: …))` | Subtree collapses to one VoiceOver element | **There is no single-widget equivalent.** `Semantics` alone *merges*; it does not *replace*. `ExcludeSemantics` is mandatory or child nodes leak. |
| `Modifier.semantics(mergeDescendants = true) { … }` | `MergeSemantics(child: …)` | Children read as one element | Use for the ayah header/number pair, not for the pager. |
| *(no direct equivalent)* | `BlockSemantics(child: …)` | Hides everything *painted below* in the same layer | Use for the screen-off overlay (`ScreenOffSaverOverlay`) so the dimmed player beneath is unreachable. |
| `Modifier.semantics { invisibleToUser() }` | `ExcludeSemantics(child: …)` | Element absent from the tree | Decorative art, `AudioEqualizerBars`. |
| `Modifier.semantics { traversalIndex = n }` | `Semantics(sortKey: OrdinalSortKey(n))` | Fixes swipe order | Needed for RTL bottom sheets where visual order ≠ logical order. |
| `Modifier.semantics { isTraversalGroup = true }` | `Semantics(container: true, explicitChildNodes: true)` | Groups a region | `SurahIndexSheet` rows. |

### 2.2 Descriptive properties

| Compose | Flutter | VoiceOver surface |
|---|---|---|
| `contentDescription = "…"` | `Semantics(label: '…')` | The spoken name |
| `stateDescription = "…"` | `Semantics(value: '…')` | Spoken after the label ("قيد التشغيل") |
| `Role.Button` / `Role.Switch` | `Semantics(button: true)` / `Semantics(toggled: bool)` | Trait suffix ("زر") |
| `heading()` | `Semantics(header: true)` | Rotor "Headings" navigation |
| `onClick(label = "…") { }` | `Semantics(onTapHint: '…', onTap: () {})` | "Double-tap to …" hint |
| `onLongClickLabel = "…"` | `Semantics(onLongPressHint: '…', onLongPress: () {})` | Long-press hint |
| `liveRegion = LiveRegionMode.Assertive` | `Semantics(liveRegion: true, …)` | Auto-announce on change |
| `disabled()` | `Semantics(enabled: false)` | "معطّل" |

> **RTL gotcha.** `contentDescription` strings in this app are Arabic. Flutter's
> `SemanticsService.announce` takes an explicit `TextDirection`; always pass
> `TextDirection.rtl` or VoiceOver may mis-order embedded Latin digits (reciter names,
> ayah numbers).

### 2.3 Actions

| Compose | Flutter | VoiceOver gesture |
|---|---|---|
| `customActions = listOf(CustomAccessibilityAction("إعادة التلاوة") { … })` | `Semantics(customSemanticsActions: {CustomSemanticsAction(label: 'إعادة التلاوة'): _replay})` | **Rotor → Actions**, then swipe up/down + double-tap |
| `scrollBy(action = { x, y -> … })` | `Semantics(onScrollLeft: …, onScrollRight: …)` | **Three-finger swipe** (see §3) |
| `onClick { … }` | `Semantics(onTap: …)` | One-finger double-tap |
| `dismiss { … }` | `Semantics(onDismiss: …)` | **Two-finger scrub ("Z")** — the iOS Back gesture |
| `expand()` / `collapse()` | `Semantics(onDismiss:)` + `onTap` | Bottom sheets |

> **Naming correction:** the Flutter class is `CustomSemanticsAction`
> (`package:flutter/semantics.dart`), not `SemanticsCustomAction`. It is passed through
> the `customSemanticsActions` map, keyed by the action, valued by a `VoidCallback`.

---

## 3. The Stealth Box — Reference Port

The Android original (`QuranPlayerScreen.kt:287-327`) collapses the pager into one node
that carries: tap → toggle playback, a custom action → replay, and `scrollBy` → page turn.

### 3.1 Gesture-model divergence (critical)

| Intent | TalkBack (Android) | VoiceOver (iOS) |
|---|---|---|
| Activate the focused element | 1-finger double-tap | 1-finger double-tap |
| Scroll / page turn | **2-finger swipe** | **3-finger swipe** |
| Go back / dismiss | 2-finger "Z" scrub or system Back | 2-finger "Z" scrub |
| Global play/pause | *(no system gesture)* | **Magic Tap: 2-finger double-tap** |
| Run a custom action | Local context menu (swipe up-then-right) | Rotor → Actions |

Three consequences:

1. **The on-screen hint text must be platform-conditional.** The Android string
   `"انقر للتكرار"` and any "اسحب بإصبعين" copy must become "اسحب بثلاثة أصابع" on iOS.
   Ship the hint through a `PlatformHints` abstraction, not a hard-coded literal.
2. **`onScrollLeft`/`onScrollRight` are *visual*, not logical.** The Android code branches
   on `x > 0f → previous page`, which is correct for an RTL pager. In Flutter, with
   `Directionality.rtl`, `onScrollRight` (drag content rightwards) must map to
   **next** ayah. Getting this backwards is the single most likely silent regression;
   §9 has the test that pins it.
3. **Magic Tap is free parity value and Flutter does not expose it.** It requires a
   platform channel (§3.3).

### 3.2 Dart reference implementation

```dart
// lib/ui/player/stealth_surface.dart
import 'package:flutter/material.dart';
import 'package:flutter/semantics.dart';

/// Collapses the recitation pager into a single VoiceOver element.
///
/// Mirrors the Android "Stealth Box" (QuranPlayerScreen.kt:287) — when VoiceOver is
/// off the subtree keeps its natural semantics so sighted users get normal widget
/// behaviour.
class StealthSurface extends StatelessWidget {
  const StealthSurface({
    super.key,
    required this.child,
    required this.onTogglePlayback,
    required this.onReplay,
    required this.onNextAyah,
    required this.onPreviousAyah,
    required this.canGoNext,
    required this.canGoPrevious,
  });

  final Widget child;
  final VoidCallback onTogglePlayback;
  final VoidCallback onReplay;
  final VoidCallback onNextAyah;
  final VoidCallback onPreviousAyah;
  final bool canGoNext;
  final bool canGoPrevious;

  @override
  Widget build(BuildContext context) {
    // I3: reactive — rebuilds when VoiceOver is toggled mid-session.
    final screenReaderOn = MediaQuery.accessibleNavigationOf(context);
    if (!screenReaderOn) return child;

    final rtl = Directionality.of(context) == TextDirection.rtl;

    return Semantics(
      container: true,
      // Deliberately unlabelled: the ayah text is delivered by the live-region
      // announcer (§6), not by focus. A label here would double-speak.
      label: '',
      onTap: onTogglePlayback,
      onTapHint: 'تشغيل أو إيقاف',
      customSemanticsActions: <CustomSemanticsAction, VoidCallback>{
        const CustomSemanticsAction(label: 'إعادة التلاوة'): onReplay,
      },
      // Visual direction → logical direction. In RTL, dragging content to the
      // right advances to the NEXT ayah.
      onScrollRight: rtl
          ? (canGoNext ? onNextAyah : null)
          : (canGoPrevious ? onPreviousAyah : null),
      onScrollLeft: rtl
          ? (canGoPrevious ? onPreviousAyah : null)
          : (canGoNext ? onNextAyah : null),
      // I2: without this the PageView's own nodes leak into the tree.
      child: ExcludeSemantics(child: child),
    );
  }
}
```

### 3.3 Magic Tap platform channel (iOS-only parity win)

VoiceOver's two-finger double-tap is the system-wide "play/pause" gesture. Wiring it
means a blind user can toggle recitation **without locating anything**, which is
strictly better than the Android baseline.

```swift
// ios/Runner/MagicTapViewController.swift
import Flutter
import UIKit

final class MagicTapViewController: FlutterViewController {
  private var channel: FlutterMethodChannel?

  override func viewDidLoad() {
    super.viewDidLoad()
    channel = FlutterMethodChannel(
      name: "quranblind/a11y/magic_tap",
      binaryMessenger: binaryMessenger
    )
  }

  override func accessibilityPerformMagicTap() -> Bool {
    channel?.invokeMethod("magicTap", arguments: nil)
    return true   // claim the gesture; false lets it bubble to the system player
  }

  /// VoiceOver's 2-finger scrub. Mirrors Android's Back handling for sheets.
  override func accessibilityPerformEscape() -> Bool {
    channel?.invokeMethod("escape", arguments: nil)
    return true
  }
}
```

```swift
// ios/Runner/AppDelegate.swift — register the subclass instead of the default
let controller = MagicTapViewController(project: nil, nibName: nil, bundle: nil)
```

```dart
// lib/platform/magic_tap.dart
const _channel = MethodChannel('quranblind/a11y/magic_tap');

void bindMagicTap({required VoidCallback onToggle, required VoidCallback onEscape}) {
  _channel.setMethodCallHandler((call) async {
    switch (call.method) {
      case 'magicTap': onToggle();
      case 'escape':   onEscape();
    }
  });
}
```

---

## 4. Screen-Reader Detection

`LocalTalkBackEnabled` (`TalkBackCompositionLocal.kt`) is fed by two
`AccessibilityManager` listeners so the UI adapts live. The Flutter equivalent is
already reactive — but only if read through `MediaQuery`, never through
`WidgetsBinding.instance.platformDispatcher.accessibilityFeatures`, which does **not**
trigger a rebuild.

| Android | Flutter | Rebuilds on change? |
|---|---|---|
| `LocalTalkBackEnabled.current` | `MediaQuery.accessibleNavigationOf(context)` | ✅ yes |
| `AccessibilityManager.isEnabled && isTouchExplorationEnabled` | `accessibleNavigation` (already the conjunction of both on iOS) | — |
| `SpeechManager.isTalkBackEnabledFlow` | `WidgetsBindingObserver.didChangeAccessibilityFeatures()` | ✅ for non-widget listeners (audio layer, analytics) |
| `Settings.Global.TRANSITION_ANIMATION_SCALE` | `MediaQuery.disableAnimationsOf(context)` | ✅ |
| — | `MediaQuery.boldTextOf(context)` | ✅ (iOS-specific; honour it) |

The `blindAccessibleClickable` modifier (`BlindAccessibleClickable.kt`) branches labels
on TalkBack state. Its Flutter port:

```dart
// lib/ui/a11y/blind_accessible.dart
class BlindAccessible extends StatelessWidget {
  const BlindAccessible({
    super.key,
    required this.child,
    required this.onTap,
    this.onTapHint = 'اختيار',
    this.onLongPress,
    this.onLongPressHint,
    this.isButton = true,
  });

  final Widget child;
  final VoidCallback onTap;
  final String onTapHint;
  final VoidCallback? onLongPress;
  final String? onLongPressHint;
  final bool isButton;

  @override
  Widget build(BuildContext context) {
    final screenReaderOn = MediaQuery.accessibleNavigationOf(context);
    return Semantics(
      button: isButton,
      // Hints are noise for sighted users and are dropped when VoiceOver is off,
      // exactly as the Kotlin original drops onClickLabel.
      onTapHint: screenReaderOn ? onTapHint : null,
      onLongPressHint: screenReaderOn ? onLongPressHint : null,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        onLongPress: onLongPress,
        child: ExcludeSemantics(excluding: screenReaderOn, child: child),
      ),
    );
  }
}
```

---

## 5. Audio Engine Isolation — the highest-risk subsystem

`QuranAudioService` is a `MediaSessionService` wrapping ExoPlayer with a disk cache, a
controller allow-list, and a ghost-resume suppressor. Each piece has an iOS counterpart
with materially different failure modes.

### 5.1 Component mapping

| Android (`service/`) | Flutter / iOS | Migration note |
|---|---|---|
| `MediaSessionService` | `audio_service` `AudioHandler` (`BaseAudioHandler` + `SeekHandler`) | `audio_service` publishes to `MPNowPlayingInfoCenter`; there is no foreground-service notion on iOS — background audio comes from the `UIBackgroundModes: audio` entitlement. |
| `ExoPlayer` | `just_audio` `AudioPlayer` | — |
| `SimpleCache` + `CacheDataSource` + `LeastRecentlyUsedCacheEvictor` | `LockCachingAudioSource` | **`just_audio` has no LRU evictor.** Disk growth is unbounded; a `CacheReaper` (§5.4) must be written. |
| `AudioAttributes(CONTENT_TYPE_SPEECH, USAGE_MEDIA)` | `AVAudioSession.setCategory(.playback, mode: .spokenAudio)` | See §5.2 — this is where VoiceOver conflicts are won or lost. |
| `setHandleAudioBecomingNoisy(true)` | `AVAudioSession.routeChangeNotification` → `.oldDeviceUnavailable` → pause | `audio_session` exposes `becomingNoisyEventStream`. |
| `setWakeMode(WAKE_MODE_NETWORK)` | *(not needed)* | iOS keeps the process alive via the audio background mode. |
| `MediaSession.Callback.onConnect` allow-list | `MPRemoteCommandCenter` — enable only the commands you serve | iOS has no third-party controller handshake; hardening = disabling unserved commands so Siri/CarPlay cannot synthesise them. |
| `MediaButtonPolicy` + `CallEndTracker` | Interruption-window suppressor + `CXCallObserver` (§5.3) | Port the constant, not just the idea. |

### 5.2 AVAudioSession configuration — do not duck

```dart
// lib/audio/session.dart
import 'package:audio_session/audio_session.dart';

Future<void> configureRecitationSession() async {
  final session = await AudioSession.instance;
  await session.configure(const AudioSessionConfiguration(
    // I6: mirrors C.AUDIO_CONTENT_TYPE_SPEECH. `.spokenAudio` makes iOS *pause*
    // rather than duck when another spoken-word source starts — correct for
    // recitation, and it keeps VoiceOver's own ducking preference authoritative.
    avAudioSessionCategory: AVAudioSessionCategory.playback,
    avAudioSessionMode: AVAudioSessionMode.spokenAudio,
    // CRITICAL: no duckOthers, no mixWithOthers.
    //   duckOthers    → attenuates VOICEOVER, making the reader inaudible.
    //   mixWithOthers → recitation and VoiceOver speak simultaneously (I1 broken).
    avAudioSessionCategoryOptions: AVAudioSessionCategoryOptions.none,
    avAudioSessionRouteSharingPolicy: AVAudioSessionRouteSharingPolicy.defaultPolicy,
    avAudioSessionSetActiveOptions: AVAudioSessionSetActiveOptions.none,
  ));
}
```

> **Why this matters.** `duckOthers` is the reflexive choice for "let the screen reader
> be heard over my audio" and it is exactly wrong: on iOS, VoiceOver *is* one of the
> "others". The correct behaviour is to leave the session plain and let VoiceOver's own
> **Settings → Accessibility → VoiceOver → Audio → Audio Ducking** preference govern.
> The user owns that decision, not the app.

### 5.3 Ghost-resume suppression (port of `MediaButtonPolicy`)

`MediaButtonPolicy` exists because Android telephony injects `ACTION_MEDIA_BUTTON`
after a call ends and playback starts unbidden. iOS reproduces the same class of bug
through `AVAudioSessionInterruptionOptions.shouldResume` and through
`MPRemoteCommandCenter.playCommand` firing on call teardown. The policy ports verbatim,
including the 2000 ms window.

```dart
// lib/audio/ghost_resume_policy.dart

/// Port of `MediaButtonPolicy`
/// (app/src/main/java/com/example/service/MediaButtonPolicy.kt).
///
/// The signal that separates a genuine user tap from a telephony-injected resume is
/// timing, not identity — on both platforms the two arrive through the same channel.
class GhostResumePolicy {
  /// Identical to MediaButtonPolicy.CALL_END_SUPPRESSION_WINDOW_MS.
  static const Duration suppressionWindow = Duration(milliseconds: 2000);

  /// Sentinel: no call has ended in this process' lifetime.
  static const Duration noCallRecorded = Duration(days: 3650);

  /// [sinceCallEnded] is Duration.zero while a call is in progress.
  static bool shouldConsume({
    required bool isPlayRequest,
    required bool playWhenReady,
    required Duration sinceCallEnded,
  }) {
    if (!isPlayRequest) return false;
    // Already playing: a play/pause toggle means "pause" here, never a ghost start.
    if (playWhenReady) return false;
    return sinceCallEnded >= Duration.zero && sinceCallEnded < suppressionWindow;
  }
}
```

`CallEndTracker`'s iOS counterpart uses CallKit, which reports call state with no
privacy prompt:

```swift
// ios/Runner/CallEndTracker.swift — mirrors service/CallEndTracker.kt
import CallKit

final class CallEndTracker: NSObject, CXCallObserverDelegate {
  private let observer = CXCallObserver()
  /// Exposed to Dart over a channel; nil means "no call recorded".
  private(set) var lastCallEndedAt: Date?
  private(set) var callInProgress = false

  override init() {
    super.init()
    observer.setDelegate(self, queue: .main)
  }

  func callObserver(_ observer: CXCallObserver, callChanged call: CXCall) {
    if call.hasEnded {
      callInProgress = false
      lastCallEndedAt = Date()
    } else if call.isOutgoing || call.hasConnected {
      callInProgress = true
    }
  }
}
```

The interruption handler must **refuse `shouldResume`** while inside the window:

```dart
session.interruptionEventStream.listen((event) async {
  if (event.begin) {
    _wasPlayingBeforeInterruption = player.playing;
    await player.pause();
    return;
  }
  final resumeHinted = event.type == AudioInterruptionType.pause;
  final ghost = GhostResumePolicy.shouldConsume(
    isPlayRequest: resumeHinted,
    playWhenReady: player.playing,
    sinceCallEnded: await callTracker.sinceCallEnded(),
  );
  if (ghost) return;                       // I4 upheld
  if (resumeHinted && _wasPlayingBeforeInterruption) await player.play();
});
```

### 5.4 Cache eviction (no built-in LRU on iOS)

`LeastRecentlyUsedCacheEvictor` has no `just_audio` analogue. Without a replacement, a
user who listens through the muṣḥaf accumulates the full recitation set on device.
Reap on cold start, off the UI isolate:

```dart
// lib/audio/cache_reaper.dart
Future<void> reapCache({int maxBytes = 512 * 1024 * 1024}) async {
  final dir = Directory('${(await getTemporaryDirectory()).path}/just_audio_cache');
  if (!dir.existsSync()) return;
  final files = dir.listSync(recursive: true).whereType<File>().toList()
    ..sort((a, b) => a.statSync().accessed.compareTo(b.statSync().accessed));
  var total = files.fold<int>(0, (sum, f) => sum + f.lengthSync());
  for (final f in files) {
    if (total <= maxBytes) break;
    total -= f.lengthSync();
    await f.delete();
  }
}
```

---

## 6. Announcements & Live Regions

| Android | Flutter/iOS |
|---|---|
| `announceForAccessibility(context, text)` → `AccessibilityEvent.TYPE_ANNOUNCEMENT` | `SemanticsService.announce(text, TextDirection.rtl)` → `UIAccessibility.post(notification: .announcement)` |
| `liveRegion = LiveRegionMode.Assertive` (`PlayerControlPanel.kt:81`) | `Semantics(liveRegion: true, label: …)` |
| `QuranViewModel.announce(text, forceSpeak)` (`QuranViewModel.kt:767`) | `A11yAnnouncer.announce(text, force: …)` |

**iOS-specific hazard:** an announcement posted while VoiceOver is mid-utterance is
*dropped silently* — there is no queue. Android's `AccessibilityManager` coalesces
instead. Because this app announces on every ayah boundary, dropped announcements are
the difference between usable and unusable. Serialise them:

```dart
// lib/ui/a11y/announcer.dart
class A11yAnnouncer {
  final _queue = Queue<String>();
  bool _draining = false;

  /// ~55 ms per Arabic word at VoiceOver's default rate, floored at 900 ms.
  Duration _estimate(String s) => Duration(
        milliseconds: math.max(900, s.split(RegExp(r'\s+')).length * 55),
      );

  void announce(String message, {bool force = false}) {
    if (message.isEmpty) return;
    if (force) _queue.clear();
    _queue.add(message);
    _drain();
  }

  Future<void> _drain() async {
    if (_draining) return;
    _draining = true;
    while (_queue.isNotEmpty) {
      final msg = _queue.removeFirst();
      SemanticsService.announce(msg, TextDirection.rtl);
      await Future<void>.delayed(_estimate(msg));
    }
    _draining = false;
  }
}
```

> The existing `web_ios/js/accessibility.js` PWA already solves the sibling problem —
> clearing the live region before re-setting it so VoiceOver re-announces identical
> consecutive strings. Keep that trick for `liveRegion: true` widgets: toggle the label
> through an empty string on repeated values.

---

## 7. Haptics

`HapticFeedbackManager` uses `VibrationEffect.createWaveform` with per-step amplitudes —
six distinct patterns that let a blind user identify an action without audio. iOS
`HapticFeedback` cannot express these; `HapticFeedback.vibrate` maps to the coarse
`kSystemSoundID_Vibrate`. Full parity requires Core Haptics.

| `HapticFeedbackManager` method | Android pattern (ms / amplitude) | Flutter approximation | Core Haptics port (recommended) |
|---|---|---|---|
| `vibrateClick()` | `EFFECT_CLICK` | `HapticFeedback.selectionClick()` | `CHHapticEvent(.hapticTransient, intensity: 0.6, sharpness: 0.8)` |
| `vibrateDoubleTap()` | `EFFECT_DOUBLE_CLICK` | `lightImpact()` ×2, 60 ms apart | two transients at t=0, t=0.08 |
| `vibrateLongPress()` | `EFFECT_HEAVY_CLICK` | `HapticFeedback.heavyImpact()` | transient, intensity 1.0 |
| `vibrateRepeatOn()` | `[0,50,80,50] @ 255` | `mediumImpact()` ×2 | two transients, 0.13 s apart |
| `vibrateRepeatOff()` | one-shot 200 ms | `HapticFeedback.vibrate()` | `.hapticContinuous`, duration 0.2 |
| `vibrateBookmark()` | `[0,40,40,40,40,60]` triple | `lightImpact()` ×3 | three transients, 0.08 s apart |

Because *repeat-on vs repeat-off must be distinguishable by feel alone*, the two-pulse
vs one-long-pulse contrast is a functional requirement, not polish. Use a Core Haptics
channel; the Flutter-only column is an acceptable interim only where `CHHapticEngine`
reports unsupported (iPhone SE 1st gen, iPad).

---

## 8. Voice Command Layer

`VoiceCommandManager` uses `SpeechRecognizer` + `ToneGenerator` earcons + an explicit
`AudioFocusRequest`, parsing to a 13-case `VoiceCommandResult` sealed class.

| Android | iOS / Flutter |
|---|---|
| `SpeechRecognizer` + `RecognizerIntent` | `SFSpeechRecognizer` via `speech_to_text` |
| *(only `RECORD_AUDIO`)* | **Two** `Info.plist` keys required: `NSSpeechRecognitionUsageDescription` **and** `NSMicrophoneUsageDescription` — a missing key is a launch-time crash, not a denial |
| `AudioFocusRequest` (transient, ducking) | Temporarily `AVAudioSession(.playAndRecord, mode: .measurement)`, then **restore `.playback`/`.spokenAudio`** in a `finally` |
| `ToneGenerator` earcons | `SystemSound.play`, or a bundled 80 ms AIFF on a second `just_audio` instance |
| `VoiceCommandResult` sealed class | Dart `sealed class VoiceCommandResult` (Dart 3 exhaustive `switch`) — 1:1 port, keep all 13 cases |

> Failing to restore the session category after recognition leaves recitation routed to
> the earpiece at reduced volume. Wrap the whole recognition lifecycle so restoration
> happens on every exit path.

---

## 9. Verification Matrix

### 9.1 Automated (runs in CI via `flutter test`)

```dart
testWidgets('I2: recitation surface exposes exactly one node', (tester) async {
  final handle = tester.ensureSemantics();
  await tester.pumpWidget(_playerUnderVoiceOver());
  final node = tester.getSemantics(find.byType(StealthSurface));
  expect(node.childrenCount, 0, reason: 'ExcludeSemantics missing — child nodes leaked');
  handle.dispose();
});

testWidgets('RTL: onScrollRight advances to the NEXT ayah', (tester) async {
  final handle = tester.ensureSemantics();
  var next = 0;
  await tester.pumpWidget(_rtlPlayer(onNextAyah: () => next++));
  tester.binding.pipelineOwner.semanticsOwner!.performAction(
    tester.getSemantics(find.byType(StealthSurface)).id,
    SemanticsAction.scrollRight,
  );
  expect(next, 1);
  handle.dispose();
});

test('I4: ghost resume inside the 2s window is consumed', () {
  expect(
    GhostResumePolicy.shouldConsume(
      isPlayRequest: true, playWhenReady: false,
      sinceCallEnded: const Duration(milliseconds: 500)),
    isTrue,
  );
  expect(
    GhostResumePolicy.shouldConsume(
      isPlayRequest: true, playWhenReady: false,
      sinceCallEnded: const Duration(milliseconds: 2500)),
    isFalse,
  );
});
```

Plus the accessibility guideline matchers that ship with the framework:

```dart
testWidgets('meets iOS tap-target and contrast guidelines', (tester) async {
  final handle = tester.ensureSemantics();
  await tester.pumpWidget(const QuranApp());
  await expectLater(tester, meetsGuideline(iOSTapTargetGuideline));   // 44×44 pt
  await expectLater(tester, meetsGuideline(textContrastGuideline));
  await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
  handle.dispose();
});
```

### 9.2 Manual device checklist (run on every TestFlight build)

| # | Check | Pass criterion |
|---|---|---|
| 1 | VoiceOver on, open player, swipe right repeatedly | Focus never enters the ayah text, ayah number, or equalizer bars |
| 2 | Double-tap anywhere on the recitation surface | Toggles playback; VoiceOver says nothing extra |
| 3 | Three-finger swipe left / right | Advances / rewinds one ayah **in the RTL sense** |
| 4 | Rotor → Actions | "إعادة التلاوة" present and functional |
| 5 | Two-finger double-tap (Magic Tap) anywhere | Toggles playback |
| 6 | Play recitation, then swipe to a control | VoiceOver is clearly audible over recitation (ducking is the user's setting, never forced) |
| 7 | Receive a phone call mid-recitation, then hang up | Playback stays **paused**; nothing auto-resumes (I4) |
| 8 | Unplug headphones mid-recitation | Pauses immediately |
| 9 | Toggle repeat on, then off, screen off, headphones out | The two haptic patterns are distinguishable by feel alone |
| 10 | Lock screen during playback | Now-playing shows surah + ayah + reciter; controls work |
| 11 | Toggle VoiceOver off *while the app is open* | UI switches to sighted mode without a restart (I3) |
| 12 | Bottom sheet open → two-finger scrub | Sheet dismisses |
| 13 | Settings → Accessibility → VoiceOver → Speech → rate at max | Announcement queue keeps up; no ayah boundary is skipped |

Run Xcode's **Accessibility Inspector → Audit** against the running app for the static
half of this list; it catches unlabelled elements and undersized hit targets that widget
tests miss on real device metrics.

---

## 10. Porting Order (dependency-correct)

1. `MediaQuery.accessibleNavigationOf` plumbing + `BlindAccessible` (§4) — everything depends on it.
2. `A11yAnnouncer` (§6) — needed before any screen, or you will debug a silent UI.
3. Audio session + `GhostResumePolicy` + `CallEndTracker` (§5) — the invariants most likely to be discovered late and expensively.
4. `StealthSurface` (§3) — the interaction model.
5. Magic Tap / escape channel (§3.3).
6. Core Haptics channel (§7).
7. Voice commands (§8) — last; additive, and the only layer that can ship disabled.
