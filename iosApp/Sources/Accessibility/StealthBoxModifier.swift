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
    @Environment(\.accessibilityVoiceOverEnabled) private var isVoiceOverRunning: Bool

    public let isPlaying: Bool
    public let surahName: String?
    public let ayahNumber: Int32?
    public let ayahCount: Int32?
    public let onTogglePlay: () -> Void
    public let onReplay: () -> Void
    public let onNextAyah: (() -> Void)?
    public let onPreviousAyah: (() -> Void)?

    public init(
        isPlaying: Bool,
        surahName: String? = nil,
        ayahNumber: Int32? = nil,
        ayahCount: Int32? = nil,
        onTogglePlay: @escaping () -> Void,
        onReplay: @escaping () -> Void,
        onNextAyah: (() -> Void)? = nil,
        onPreviousAyah: (() -> Void)? = nil
    ) {
        self.isPlaying = isPlaying
        self.surahName = surahName
        self.ayahNumber = ayahNumber
        self.ayahCount = ayahCount
        self.onTogglePlay = onTogglePlay
        self.onReplay = onReplay
        self.onNextAyah = onNextAyah
        self.onPreviousAyah = onPreviousAyah
    }

    /// Dynamic Arabic accessibility label with graceful null-safe fallback
    private var surahAyahAccessibilityLabel: String {
        let surah = surahName?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let surah = surah, !surah.isEmpty {
            if let ayah = ayahNumber, let total = ayahCount, total > 0 {
                return "سورة \(surah)، الآية \(ayah) من أصل \(total)"
            } else if let ayah = ayahNumber {
                return "سورة \(surah)، الآية \(ayah)"
            } else {
                return "سورة \(surah)"
            }
        } else if let ayah = ayahNumber {
            return "الآية \(ayah)"
        } else {
            return "تلاوة القرآن الكريم"
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
            .accessibilityValue(isPlaying ? "قيد التشغيل" : "متوقف مؤقتاً")
            .accessibilityHint(isVoiceOverRunning ? "تشغيل أو إيقاف" : "تشغيل أو إيقاف التلاوة")
            .accessibilityAction(named: "إعادة التلاوة") {
                handleReplayAction()
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
