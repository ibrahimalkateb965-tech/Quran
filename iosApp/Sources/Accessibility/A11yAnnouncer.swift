//
//  A11yAnnouncer.swift
//  QuranBlind
//
//  Milestone 3: Accessibility Engine & VoiceOver Infrastructure
//  Feature 10: Serialized Live Accessibility Announcements
//

import Foundation
import UIKit

/// Serialized Live Accessibility Announcement Queue for VoiceOver.
///
/// In iOS VoiceOver, posting an announcement notification while VoiceOver is already speaking
/// causes the new announcement to be silently dropped. `A11yAnnouncer` addresses this by maintaining
/// a serialized FIFO queue with utterance duration estimation (~55ms per Arabic word, minimum 900ms floor).
///
/// Complete 1:1 parity with Android `QuranViewModel.kt:407-652` live announcements.
public final class A11yAnnouncer {

    // MARK: - Singleton

    public static let shared = A11yAnnouncer()

    // MARK: - Queue State

    private var queue: [String] = []
    private var isDraining: Bool = false

    // MARK: - Timing Constants

    /// Estimated reading duration per word in seconds (~55ms per Arabic word).
    private let timePerWord: Double = 0.055

    /// Minimum utterance duration floor in seconds (900ms / 0.9s).
    private let minDurationFloor: Double = 0.9

    // MARK: - Initialization

    public init() {}

    // MARK: - Core Announcement API

    /// Enqueues an announcement for VoiceOver speech.
    /// - Parameters:
    ///   - message: The announcement text to be spoken. Rejects empty strings silently.
    ///   - force: If true, flushes all pending announcements in the queue and plays immediately.
    public func announce(_ message: String, force: Bool = false) {
        guard !message.isEmpty else { return }

        let trimmed = message.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }

        if Thread.isMainThread {
            enqueue(trimmed, force: force)
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.enqueue(trimmed, force: force)
            }
        }
    }

    /// Flushes all pending announcements in the queue.
    public func clear() {
        if Thread.isMainThread {
            queue.removeAll()
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.queue.removeAll()
            }
        }
    }

    // MARK: - Queue Processing

    private func enqueue(_ message: String, force: Bool) {
        if force {
            queue.removeAll()
        }
        queue.append(message)
        drainQueue()
    }

    private func drainQueue() {
        guard !isDraining else { return }
        guard !queue.isEmpty else { return }

        isDraining = true
        processNext()
    }

    private func processNext() {
        guard !queue.isEmpty else {
            isDraining = false
            return
        }

        let message = queue.removeFirst()

        // Post announcement to VoiceOver
        UIAccessibility.post(notification: .announcement, argument: message)

        // Calculate spoken duration based on word count with 900ms floor
        let duration = calculateDuration(for: message)

        // Schedule next announcement after calculated duration using [weak self] to avoid retain cycles
        DispatchQueue.main.asyncAfter(deadline: .now() + duration) { [weak self] in
            guard let self = self else { return }
            self.processNext()
        }
    }

    /// Calculates the spoken duration for a message based on Arabic word count (~55ms per word, floored at 0.9s / 900ms).
    private func calculateDuration(for message: String) -> TimeInterval {
        let words = message.split(separator: " ")
        let wordCount = words.count
        let calculated = Double(wordCount) * timePerWord
        return max(0.9, calculated)
    }

    // MARK: - Arabic Announcement Helpers (Matching QuranViewModel.kt:407-652)

    /// Announces playback state toggle ("جاري التشغيل" / "تم الإيقاف المؤقت").
    public func announcePlaybackState(isPlaying: Bool) {
        let message = isPlaying ? "جاري التشغيل" : "تم الإيقاف المؤقت"
        announce(message)
    }

    /// Announces Surah change ("\(name). عدد آياتها \(ayahCount).").
    public func announceSurahChange(name: String, ayahCount: Int32) {
        announce("\(name). عدد آياتها \(ayahCount).")
    }

    /// Announces Surah change with Int overload.
    public func announceSurahChange(name: String, ayahCount: Int) {
        announceSurahChange(name: name, ayahCount: Int32(ayahCount))
    }

    /// Announces Surah completion ("انتهت سورة \(name)").
    public func announceSurahCompletion(name: String) {
        announce("انتهت سورة \(name)")
    }

    /// Announces theme mode toggle ("تم التبديل إلى الوضع الليلي" / "تم التبديل إلى الوضع النهاري").
    public func announceThemeMode(isDark: Bool) {
        let message = isDark ? "تم التبديل إلى الوضع الليلي" : "تم التبديل إلى الوضع النهاري"
        announce(message)
    }

    /// Announces continuous play mode toggle ("وضع الاستماع المتواصل مفعّل" / "تم إيقاف الاستماع المتواصل").
    public func announceContinuousPlay(enabled: Bool) {
        let message = enabled ? "وضع الاستماع المتواصل مفعّل" : "تم إيقاف الاستماع المتواصل"
        announce(message)
    }

    /// Announces reciter change ("تم تغيير القارئ إلى \(name)").
    public func announceReciterChange(name: String) {
        announce("تم تغيير القارئ إلى \(name)")
    }

    /// Announces OLED battery saver screen-off mode toggle.
    public func announceScreenOff(enabled: Bool) {
        let message = enabled
            ? "تم تفعيل وضع إيقاف الشاشة لتوفير البطارية. الشاشة مغلقة الآن مع استمرار الإيماءات والصوت."
            : "تم إلغاء وضع إيقاف الشاشة."
        announce(message)
    }

    /// Announces bookmark addition or removal.
    public func announceBookmark(isBookmarked: Bool, surahName: String = "", ayahNumber: Int32 = 1) {
        let message = isBookmarked
            ? "تم إضافة سورة \(surahName) الآية \(ayahNumber) للإشارات المرجعية"
            : "تم إزالة الآية من الإشارات المرجعية"
        announce(message)
    }

    /// Announces repeat mode (Tarkiz/Hifz) changes.
    public func announceRepeatMode(count: Int) {
        let message: String
        if count > 1 {
            let modeText = (count == 99) ? "تكرار لا نهائي" : "تكرار \(count) مرات"
            message = "تم تفعيل وضع التركيز: \(modeText)"
        } else {
            message = "تم إيقاف وضع التكرار"
        }
        announce(message)
    }

    /// Announces network connection status.
    public func announceNetworkError(isRecovering: Bool) {
        let message = isRecovering
            ? "انقطع الاتصال بالإنترنت، جاري المحاولة مرة أخرى"
            : "تعذّر الاتصال بالإنترنت بعد عدة محاولات. يرجى التحقق من الشبكة والمحاولة لاحقاً."
        announce(message)
    }
}
