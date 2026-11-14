//
//  A11yLabels.swift
//  QuranBlind
//
//  Single source of truth for every VoiceOver label/value: fully-vocalized Classical Arabic
//  (فصحى مضبوطة بالشكل التام), spoken with the `ar-SA` voice. Name-only policy: no hints,
//  no "tap to…" tutorials. Dynamic data (surah / reciter names) is inserted as-is.
//

import Foundation

public enum A11yLabels {

    /// VoiceOver speech language for every control.
    public static let speechLanguage = "ar-SA"

    // MARK: Header controls
    public static let continuousPlay = "الِاسْتِمَاعُ الْمُتَوَاصِلُ"
    public static let enabled = "مُفَعَّلٌ"
    public static let disabled = "مُعَطَّلٌ"
    public static let chooseSurah = "اِخْتِيَارُ السُّورَةِ"
    public static let chooseReciter = "اِخْتِيَارُ الْقَارِئِ"

    // MARK: Player screen
    public static let replay = "إِعَادَةُ التِّلَاوَةِ"
    /// Repeat button: speaks its visible text ("انقر للتكرار") — explicit user decision, the one
    /// label allowed to contain "انقر".
    public static let tapToRepeat = "اُنْقُرْ لِلتَّكْرَارِ"
    public static let switchToDayMode = "التَّبْدِيلُ إِلَى الْوَضْعِ النَّهَارِيِّ"
    public static let switchToNightMode = "التَّبْدِيلُ إِلَى الْوَضْعِ اللَّيْلِيِّ"
    public static let screenOffActive = "شَاشَةُ تَوْفِيرِ الطَّاقَةِ نَشِطَةٌ"

    // MARK: Recitation state (stealth box)
    public static let reciting = "قَيْدَ التِّلَاوَةِ"
    public static let stopped = "مُتَوَقِّفٌ"
    public static let quranRecitation = "تِلَاوَةُ الْقُرْآنِ الْكَرِيمِ"
    public static let nextAyah = "الْآيَةُ التَّالِيَةُ"
    public static let previousAyah = "الْآيَةُ السَّابِقَةُ"

    // MARK: Sheets
    public static let close = "إِغْلَاقٌ"
    public static let surahs = "السُّوَرُ"
    public static let chooseAyah = "اِخْتِيَارُ الْآيَةِ"
    public static let openNow = "مَفْتُوحَةٌ الْآنَ"
    public static let currentlySelectedAyah = "مُحَدَّدَةٌ حَالِيًّا"
    public static let currentlySelectedReciter = "مُخْتَارٌ حَالِيًّا"

    // MARK: Dynamic labels

    public static func dayNightToggle(isDark: Bool) -> String {
        isDark ? switchToDayMode : switchToNightMode
    }

    public static func surah(_ name: String) -> String {
        "سُورَةُ \(name)"
    }

    public static func ayah<N: BinaryInteger>(_ number: N) -> String {
        "الْآيَةُ \(number)"
    }

    public static func continuousPlayValue(isEnabled: Bool) -> String {
        isEnabled ? enabled : disabled
    }

    public static func surahRow<N: BinaryInteger, M: BinaryInteger>(name: String, number: N, ayahCount: M, isActive: Bool) -> String {
        "سُورَةُ \(name)، رَقْمُهَا \(number)، آيَاتُهَا \(ayahCount)" + (isActive ? "، \(openNow)" : "")
    }

    public static func ayahRow<N: BinaryInteger>(_ number: N, isCurrent: Bool) -> String {
        ayah(number) + (isCurrent ? "، \(currentlySelectedAyah)" : "")
    }

    public static func reciterRow(arabicName: String, englishName: String, isSelected: Bool) -> String {
        "\(arabicName)، \(englishName)" + (isSelected ? "، \(currentlySelectedReciter)" : "")
    }

    public static func surahSheetTitle<N: BinaryInteger>(surahCount: N) -> String {
        "\(chooseSurah)، \(surahCount) سُورَةً"
    }

    public static func ayahSheetTitle(surahName: String) -> String {
        "\(surah(surahName))، \(chooseAyah)"
    }
}
