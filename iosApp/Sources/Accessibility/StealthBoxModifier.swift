//
//  StealthBoxModifier.swift
//  QuranBlind
//
//  Milestone 3: Accessibility Engine & VoiceOver Infrastructure
//  Feature 7: The Stealth Box Invariant (Invariant I2 & I3)
//  Feature 8: Custom VoiceOver Rotor Action ("إعادة التلاوة")
//

import SwiftUI
import UIKit

/// ViewModifier collapsing the recitation area into a single accessibility node.
/// Enforces Invariant I2 (<= 1 accessibility node on recitation surface) and
/// Invariant I3 (reactive screen-reader state handling).
public struct StealthBoxModifier: ViewModifier {

    public let isPlaying: Bool
    public let surahName: String?
    public let ayahNumber: Int32?
    public let ayahCount: Int32?
    public let ayahText: String?
    public let onTogglePlay: () -> Void
    public let onReplay: () -> Void
    public let onNextAyah: (() -> Void)?
    public let onPreviousAyah: (() -> Void)?

    public init(
        isPlaying: Bool,
        surahName: String? = nil,
        ayahNumber: Int32? = nil,
        ayahCount: Int32? = nil,
        ayahText: String? = nil,
        onTogglePlay: @escaping () -> Void,
        onReplay: @escaping () -> Void,
        onNextAyah: (() -> Void)? = nil,
        onPreviousAyah: (() -> Void)? = nil
    ) {
        self.isPlaying = isPlaying
        self.surahName = surahName
        self.ayahNumber = ayahNumber
        self.ayahCount = ayahCount
        self.ayahText = ayahText
        self.onTogglePlay = onTogglePlay
        self.onReplay = onReplay
        self.onNextAyah = onNextAyah
        self.onPreviousAyah = onPreviousAyah
    }

    /// Dynamic Arabic accessibility label with graceful null-safe fallback
    private var surahAyahAccessibilityLabel: String {
        let surah = surahName?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let surah = surah, !surah.isEmpty {
            if let text = ayahText?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty, let ayah = ayahNumber {
                return "\(A11yLabels.ayah(ayah)) مِنْ سُورَةِ \(surah): \(text)"
            } else if let ayah = ayahNumber, let total = ayahCount, total > 0 {
                return "\(A11yLabels.surah(surah))، \(A11yLabels.ayah(ayah)) مِنْ أَصْلِ \(total)"
            } else if let ayah = ayahNumber {
                return "\(A11yLabels.surah(surah))، \(A11yLabels.ayah(ayah))"
            } else {
                return A11yLabels.surah(surah)
            }
        } else if let ayah = ayahNumber {
            if let text = ayahText?.trimmingCharacters(in: .whitespacesAndNewlines), !text.isEmpty {
                return "\(A11yLabels.ayah(ayah)): \(text)"
            }
            return A11yLabels.ayah(ayah)
        } else {
            return A11yLabels.quranRecitation
        }
    }

    /// Replay action resetting playback position to 00:00 (track beginning) with medium haptic confirmation
    private func handleReplayAction() {
        // Guard against null/empty track state (T2_F08_01)
        guard surahName != nil || ayahNumber != nil || isPlaying else {
            return
        }
        performReplayHaptics()
        // Replay resets playback position to 00:00 (T2_F08_02)
        onReplay()
    }

    private func performReplayHaptics() {
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.prepare()
        generator.impactOccurred()
    }

    public func body(content: Content) -> some View {
        content
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(surahAyahAccessibilityLabel /* سورة والآية */)
            .accessibilityValue(isPlaying ? A11yLabels.reciting : A11yLabels.stopped)
            .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
            .accessibilityAction(named: A11yLabels.replay) {
                handleReplayAction()
            }
            .accessibilityAction(named: A11yLabels.nextAyah) {
                onNextAyah?()
            }
            .accessibilityAction(named: A11yLabels.previousAyah) {
                onPreviousAyah?()
            }
            .accessibilityAction {
                onTogglePlay()
            }
            .accessibilityScrollAction { edge in
                if edge == .leading {
                    onNextAyah?()
                } else if edge == .trailing {
                    onPreviousAyah?()
                }
            }
    }
}

public extension View {
    /// Collapses the recitation surface into a single VoiceOver element with Play/Pause and Replay actions.
    func stealthBox(
        isPlaying: Bool,
        surahName: String? = nil,
        ayahNumber: Int32? = nil,
        ayahCount: Int32? = nil,
        ayahText: String? = nil,
        onTogglePlay: @escaping () -> Void,
        onReplay: @escaping () -> Void,
        onNextAyah: (() -> Void)? = nil,
        onPreviousAyah: (() -> Void)? = nil
    ) -> some View {
        modifier(
            StealthBoxModifier(
                isPlaying: isPlaying,
                surahName: surahName,
                ayahNumber: ayahNumber,
                ayahCount: ayahCount,
                ayahText: ayahText,
                onTogglePlay: onTogglePlay,
                onReplay: onReplay,
                onNextAyah: onNextAyah,
                onPreviousAyah: onPreviousAyah
            )
        )
    }

    /// Convenience overload matching .stealthBox(isPlaying:onTogglePlay:onReplay:onNext:onPrevious:)
    func stealthBox(
        isPlaying: Bool,
        onTogglePlay: @escaping () -> Void,
        onReplay: @escaping () -> Void,
        onNext: (() -> Void)? = nil,
        onPrevious: (() -> Void)? = nil
    ) -> some View {
        stealthBox(
            isPlaying: isPlaying,
            surahName: nil,
            ayahNumber: nil,
            ayahCount: nil,
            onTogglePlay: onTogglePlay,
            onReplay: onReplay,
            onNextAyah: onNext,
            onPreviousAyah: onPrevious
        )
    }
}

// MARK: - Silent Recitation Surface (Android AyahCard.kt:66 + QuranPlayerScreen.kt:296-302)

/// Single VoiceOver focus target covering the central ayah pager that speaks ZERO text.
///
/// Mirrors Android's TalkBack stealth box: `clearAndSetSemantics { onClick(label = "") { togglePlayback() }; scrollBy { … } }`.
/// - Selection / touch-exploration: no label, value, hint, traits or custom actions → nothing is spoken
///   (custom actions are deliberately omitted because VoiceOver would append "Actions available").
/// - Double-tap (activate): toggles play/pause.
/// - One-finger only: single tap focuses (silently), double-tap anywhere activates.
///   No `.allowsDirectInteraction` (VoiceOver speaks "direct touch area") and no multi-finger
///   gesture recognizers. Ayah paging uses VoiceOver's own three-finger horizontal swipe.
/// - Magic Tap (two-finger double-tap): toggles play/pause, handled here so it fires exactly once.
/// - Escape (two-finger scrub "Z"): intentionally NOT overridden, so it bubbles to
///   `RootHostingController.accessibilityPerformEscape()` → `.escapeTriggered`.
///
/// Only mount it while VoiceOver is running; with VoiceOver off it would swallow sighted touches.
public struct SilentRecitationSurface: UIViewRepresentable {
    public let onActivate: () -> Void
    public let onNext: () -> Void
    public let onPrevious: () -> Void
    public let onMagicTap: () -> Void

    public init(
        onActivate: @escaping () -> Void,
        onNext: @escaping () -> Void,
        onPrevious: @escaping () -> Void,
        onMagicTap: @escaping () -> Void
    ) {
        self.onActivate = onActivate
        self.onNext = onNext
        self.onPrevious = onPrevious
        self.onMagicTap = onMagicTap
    }

    public func makeUIView(context: Context) -> SilentAccessibilityElementView {
        let view = SilentAccessibilityElementView()
        updateUIView(view, context: context)
        return view
    }

    public func updateUIView(_ view: SilentAccessibilityElementView, context: Context) {
        view.onActivate = onActivate
        view.onNext = onNext
        view.onPrevious = onPrevious
        view.onMagicTap = onMagicTap
    }
}

/// UIKit element whose every speakable accessibility property is pinned to empty.
/// Getters are overridden (setters ignored) so nothing upstream — SwiftUI bridging,
/// a stray modifier, or a future edit — can inject spoken text.
public final class SilentAccessibilityElementView: UIView {
    var onActivate: (() -> Void)?
    var onNext: (() -> Void)?
    var onPrevious: (() -> Void)?
    var onMagicTap: (() -> Void)?

    public override init(frame: CGRect) {
        super.init(frame: frame)
        backgroundColor = .clear
        isAccessibilityElement = true
        accessibilityIdentifier = "silent_recitation_surface"
    }

    @available(*, unavailable)
    public required init?(coder: NSCoder) {
        fatalError("init(coder:) is not supported")
    }

    // MARK: Zero speech

    public override var accessibilityLabel: String? { get { nil } set {} }
    public override var accessibilityAttributedLabel: NSAttributedString? { get { nil } set {} }
    public override var accessibilityValue: String? { get { nil } set {} }
    public override var accessibilityAttributedValue: NSAttributedString? { get { nil } set {} }
    public override var accessibilityHint: String? { get { nil } set {} }
    public override var accessibilityAttributedHint: NSAttributedString? { get { nil } set {} }
    /// No traits at all — `.allowsDirectInteraction` made VoiceOver say "direct touch area".
    public override var accessibilityTraits: UIAccessibilityTraits { get { .none } set {} }
    public override var accessibilityLanguage: String? { get { A11yLabels.speechLanguage } set {} }
    public override var accessibilityCustomActions: [UIAccessibilityCustomAction]? { get { nil } set {} }

    // MARK: Gestures

    /// VoiceOver double-tap → play/pause (Android `onClick(label = "")`).
    public override func accessibilityActivate() -> Bool {
        onActivate?()
        return true
    }

    /// Three-finger horizontal swipe → page the ayah (the only paging gesture VoiceOver delivers
    /// without a "direct touch area"). RTL pager: swiping right reveals the next ayah
    /// (Android `scrollBy`: x < 0 → next, x > 0 → previous). No `.pageScrolled` status is
    /// posted, so VoiceOver stays silent. Vertical scrolls are swallowed silently.
    public override func accessibilityScroll(_ direction: UIAccessibilityScrollDirection) -> Bool {
        switch direction {
        case .right, .next:
            onNext?()
        case .left, .previous:
            onPrevious?()
        default:
            break
        }
        return true
    }

    /// Two-finger double-tap → play/pause. Returning true stops the responder walk so the
    /// root controller's handler doesn't toggle a second time.
    public override func accessibilityPerformMagicTap() -> Bool {
        onMagicTap?()
        return true
    }
}
