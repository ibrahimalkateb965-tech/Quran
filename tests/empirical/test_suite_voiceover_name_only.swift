//
//  test_suite_voiceover_name_only.swift
//  Empirical Suite 8: VoiceOver Name-Only Source Audit
//
//  Statically audits iosApp/Sources so that every control speaks ONLY its clean
//  accessibilityLabel (+ state value where essential): no accessibilityHint, no
//  "tap/double-tap to…" tutorial text in labels, no spoken button traits, and no
//  named custom actions on the screen-off overlay. Activation relies purely on the
//  universal VoiceOver model: single tap focuses/inspects, double-tap anywhere activates.
//
//  Also compiles iosApp/Sources/Accessibility/A11yLabels.swift and checks that every label is
//  fully-vocalized Classical Arabic spoken with the `ar-SA` voice.
//
//  Runs on the host (not the simulator): `suite8 <repo-root>`. The runner compiles this file as
//  main.swift together with A11yLabels.swift.
//

import Foundation

var passedCount = 0
var failedCount = 0

func assertTrue(_ condition: Bool, _ name: String, detail: String = "") {
    if condition {
        passedCount += 1
        print("  [PASS] \(name)")
    } else {
        failedCount += 1
        print("  [FAIL] \(name)\(detail.isEmpty ? "" : " - \(detail)")")
    }
}

print("================================================================================")
print("  EMPIRICAL SUITE 8: VoiceOver Name-Only Source Audit")
print("================================================================================")

let repoRoot = CommandLine.arguments.count > 1
    ? CommandLine.arguments[1]
    : FileManager.default.currentDirectoryPath
let sourcesURL = URL(fileURLWithPath: repoRoot).appendingPathComponent("iosApp/Sources")

var sources: [String: String] = [:]  // relative path -> contents
if let enumerator = FileManager.default.enumerator(at: sourcesURL, includingPropertiesForKeys: nil) {
    for case let url as URL in enumerator where url.pathExtension == "swift" {
        let rel = String(url.path.dropFirst(sourcesURL.path.count + 1))
        sources[rel] = (try? String(contentsOf: url, encoding: .utf8)) ?? ""
    }
}

func source(_ rel: String) -> String { sources[rel] ?? "" }

/// Lines (file:line: text) matching a predicate, ignoring comment-only lines.
func offendingLines(_ predicate: (String) -> Bool) -> [String] {
    var hits: [String] = []
    for (rel, text) in sources.sorted(by: { $0.key < $1.key }) {
        for (i, raw) in text.components(separatedBy: "\n").enumerated() {
            let line = raw.trimmingCharacters(in: .whitespaces)
            if line.hasPrefix("//") || line.hasPrefix("///") { continue }
            if predicate(line) { hits.append("\(rel):\(i + 1): \(line)") }
        }
    }
    return hits
}

// -----------------------------------------------------------------------------
print("Test Group 1: Source discovery")
assertTrue(sources.count > 10, "Sources_Discovered", detail: "found \(sources.count) files under \(sourcesURL.path)")

// -----------------------------------------------------------------------------
print("Test Group 2: No accessibilityHint anywhere")
let hintCalls = offendingLines { $0.contains(".accessibilityHint(") }
assertTrue(hintCalls.isEmpty, "NoAccessibilityHintModifiers", detail: hintCalls.joined(separator: " | "))

// Init/function parameters only — the silent surface's `override var accessibilityHint` (pinned nil) is allowed.
let hintParams = offendingLines {
    !$0.contains("var ") && $0.range(of: #"\b(accessibilityHint|hint)\s*:\s*String"#, options: .regularExpression) != nil
}
assertTrue(hintParams.isEmpty, "NoHintInitParameters", detail: hintParams.joined(separator: " | "))

let hintTextConstants = offendingLines { $0.contains("accessibilityHintText") }
assertTrue(hintTextConstants.isEmpty, "NoHintTextConstants", detail: hintTextConstants.joined(separator: " | "))

// The silent recitation surface must keep its hint pinned to nil.
assertTrue(
    source("Accessibility/StealthBoxModifier.swift")
        .contains("override var accessibilityHint: String? { get { nil } set {} }"),
    "SilentSurface_HintPinnedNil"
)

// -----------------------------------------------------------------------------
print("Test Group 3: Labels are clean names (no tap/double-tap tutorials)")
let tutorialWords = ["انقر", "مرتين", "مزدوج", "اضغط", "اسحب"]
// Exception: the repeat button speaks its visible text "انقر للتكرار" (A11yLabels.tapToRepeat).
let tutorialLabels = offendingLines { line in
    (line.contains(".accessibilityLabel(") || line.contains("accessibilityLabelText =")
        || line.contains(".accessibilityValue("))
        && tutorialWords.contains(where: { line.contains($0) })
}
assertTrue(tutorialLabels.isEmpty, "NoTutorialTextInLabelsOrValues", detail: tutorialLabels.joined(separator: " | "))

let header = source("Components/PlayerHeaderBar.swift")
assertTrue(header.contains(".accessibilityLabel(A11yLabels.chooseSurah)"), "Header_SurahButton_LabelOnly")
assertTrue(header.contains(".accessibilityLabel(A11yLabels.chooseReciter)"), "Header_ReciterButton_LabelOnly")
assertTrue(header.contains(".accessibilityLabel(A11yLabels.continuousPlay)"), "Header_ContinuousPlay_Label")
assertTrue(
    header.contains(".accessibilityValue(A11yLabels.continuousPlayValue(isEnabled: isContinuousPlayEnabled))"),
    "Header_ContinuousPlay_StateValue"
)

// Repeat button speaks its visible text, vocalized, and is a normal VoiceOver element.
let playerScreen = source("QuranPlayerScreen.swift")
if let repeatRange = playerScreen.range(of: ".accessibilityLabel(A11yLabels.tapToRepeat)") {
    let repeatBlock = String(playerScreen[playerScreen.index(repeatRange.lowerBound, offsetBy: -700)...].prefix(1000))
    assertTrue(repeatBlock.contains(#"Text("انقر للتكرار")"#), "RepeatButton_VisibleTextUnchanged")
    assertTrue(repeatBlock.contains("replayCurrentAyah"), "RepeatButton_DoubleTapReplays")
    assertTrue(!repeatBlock.contains("SilentRecitationSurface("), "RepeatButton_NotSilent")
    assertTrue(!repeatBlock.contains(".accessibilityHidden("), "RepeatButton_NotHidden")
} else {
    assertTrue(false, "RepeatButton_Found")
}
assertTrue(
    source("Components/AyahNumberCard.swift").contains(".accessibilityLabel(A11yLabels.ayah(number))"),
    "AyahCard_CleanLabel"
)

// -----------------------------------------------------------------------------
print("Test Group 4: No spoken button traits on controls")
for rel in [
    "Components/BlindAccessibleIconButton.swift",
    "Components/HeaderAccessibleButton.swift",
    "Components/AyahNumberCard.swift",
    "Components/SurahNameCard.swift",
    "Components/PlayerHeaderBar.swift",
    "QuranPlayerScreen.swift",
] {
    assertTrue(source(rel).contains(".accessibilityRemoveTraits(.isButton)"), "RemovesButtonTrait_\(rel)")
}
let addedButtonTraits = offendingLines { $0.contains("accessibilityAddTraits(.isButton)") }
assertTrue(addedButtonTraits.isEmpty, "NoAddedButtonTraits", detail: addedButtonTraits.joined(separator: " | "))

// -----------------------------------------------------------------------------
print("Test Group 5: Universal activation (double-tap) without named custom actions")
let overlay = source("Components/ScreenOffSaverOverlay.swift")
assertTrue(!overlay.contains("accessibilityAction(named:"), "ScreenOffOverlay_NoNamedActions")
assertTrue(overlay.contains(".accessibilityAction {"), "ScreenOffOverlay_DefaultActivationWakes")
let screen = source("QuranPlayerScreen.swift")
assertTrue(!screen.contains("accessibilityAction(named:"), "PlayerScreen_NoNamedActions")

// -----------------------------------------------------------------------------
print("Test Group 6: One-finger VoiceOver model only (no direct touch, no custom multi-finger recognizers)")
let stealth = source("Accessibility/StealthBoxModifier.swift")
let directTouch = offendingLines { $0.contains(".allowsDirectInteraction") && !$0.contains("No traits") }
assertTrue(directTouch.isEmpty, "NoDirectTouchArea", detail: directTouch.joined(separator: " | "))
let multiFinger = offendingLines { $0.contains("numberOfTouchesRequired") }
assertTrue(multiFinger.isEmpty, "NoMultiFingerRecognizers", detail: multiFinger.joined(separator: " | "))
assertTrue(
    stealth.contains("override var accessibilityTraits: UIAccessibilityTraits { get { .none } set {} }"),
    "SilentSurface_TraitsPinnedEmpty"
)
if let scrollRange = stealth.range(of: "override func accessibilityScroll(") {
    let body = String(stealth[scrollRange.lowerBound...].prefix(400))
    assertTrue(body.contains("case .right, .next:\n            onNext?()"), "ThreeFingerSwipeRight_NextAyah")
    assertTrue(body.contains("case .left, .previous:\n            onPrevious?()"), "ThreeFingerSwipeLeft_PreviousAyah")
} else {
    assertTrue(false, "ThreeFingerScrollOverridePresent")
}
assertTrue(stealth.contains("override func accessibilityActivate() -> Bool"), "DoubleTapAnywhereActivates")

// -----------------------------------------------------------------------------
print("Test Group 7: Fully-vocalized Classical Arabic labels (فصحى مضبوطة بالشكل التام)")

// Exact strings requested for the main controls.
assertTrue(A11yLabels.chooseSurah == "اِخْتِيَارُ السُّورَةِ", "Vocalized_ChooseSurah")
assertTrue(A11yLabels.chooseReciter == "اِخْتِيَارُ الْقَارِئِ", "Vocalized_ChooseReciter")
assertTrue(A11yLabels.continuousPlay == "الِاسْتِمَاعُ الْمُتَوَاصِلُ", "Vocalized_ContinuousPlay")
assertTrue(A11yLabels.continuousPlayValue(isEnabled: true) == "مُفَعَّلٌ", "Vocalized_ContinuousPlay_Enabled")
assertTrue(A11yLabels.continuousPlayValue(isEnabled: false) == "مُعَطَّلٌ", "Vocalized_ContinuousPlay_Disabled")
assertTrue(A11yLabels.replay == "إِعَادَةُ التِّلَاوَةِ", "Vocalized_Replay")
assertTrue(A11yLabels.tapToRepeat == "اُنْقُرْ لِلتَّكْرَارِ", "Vocalized_RepeatButton_MatchesVisibleText")
assertTrue(A11yLabels.ayah(7) == "الْآيَةُ 7", "Vocalized_Ayah_WesternDigits")
assertTrue(A11yLabels.surah("الفاتحة") == "سُورَةُ الفاتحة", "Vocalized_Surah")
assertTrue(A11yLabels.speechLanguage == "ar-SA", "SpeechLanguage_IsArSA")

/// Every Arabic word must carry at least one diacritic (U+064B–U+0652, U+0670).
/// `dataWords` are inserted runtime data (surah / reciter names), exempt from the rule.
func unvocalizedWords(_ text: String, dataWords: Set<String> = []) -> [String] {
    let arabicLetters = CharacterSet(charactersIn: "\u{0621}"..."\u{064A}")
    let diacritics = CharacterSet(charactersIn: "\u{064B}"..."\u{0652}").union(CharacterSet(charactersIn: "\u{0670}"))
    return text.components(separatedBy: CharacterSet(charactersIn: " ،:,.()\n"))
        .filter { word in
            !word.isEmpty && !dataWords.contains(word)
                && word.unicodeScalars.contains(where: { arabicLetters.contains($0) })
                && !word.unicodeScalars.contains(where: { diacritics.contains($0) })
        }
}

let sampleName = "الفاتحة"
let samples: [(String, String)] = [
    ("continuousPlay", A11yLabels.continuousPlay),
    ("enabled", A11yLabels.enabled),
    ("disabled", A11yLabels.disabled),
    ("chooseSurah", A11yLabels.chooseSurah),
    ("chooseReciter", A11yLabels.chooseReciter),
    ("replay", A11yLabels.replay),
    ("tapToRepeat", A11yLabels.tapToRepeat),
    ("switchToDayMode", A11yLabels.switchToDayMode),
    ("switchToNightMode", A11yLabels.switchToNightMode),
    ("screenOffActive", A11yLabels.screenOffActive),
    ("reciting", A11yLabels.reciting),
    ("stopped", A11yLabels.stopped),
    ("quranRecitation", A11yLabels.quranRecitation),
    ("nextAyah", A11yLabels.nextAyah),
    ("previousAyah", A11yLabels.previousAyah),
    ("close", A11yLabels.close),
    ("surahs", A11yLabels.surahs),
    ("chooseAyah", A11yLabels.chooseAyah),
    ("openNow", A11yLabels.openNow),
    ("currentlySelectedAyah", A11yLabels.currentlySelectedAyah),
    ("currentlySelectedReciter", A11yLabels.currentlySelectedReciter),
    ("surah()", A11yLabels.surah(sampleName)),
    ("ayah()", A11yLabels.ayah(3)),
    ("surahRow(active)", A11yLabels.surahRow(name: sampleName, number: 1, ayahCount: 7, isActive: true)),
    ("ayahRow(current)", A11yLabels.ayahRow(3, isCurrent: true)),
    ("reciterRow(selected)", A11yLabels.reciterRow(arabicName: "مشاري العفاسي", englishName: "Mishary Alafasy", isSelected: true)),
    ("surahSheetTitle", A11yLabels.surahSheetTitle(surahCount: 114)),
    ("ayahSheetTitle", A11yLabels.ayahSheetTitle(surahName: sampleName)),
]
let dataWords: Set<String> = [sampleName, "مشاري", "العفاسي"]
for (name, text) in samples {
    let bare = unvocalizedWords(text, dataWords: dataWords)
    assertTrue(bare.isEmpty, "FullyVocalized_\(name)", detail: "unvocalized: \(bare)")
}
let tutorialHits = samples.filter { name, text in
    name != "tapToRepeat" && tutorialWords.contains(where: { text.contains($0) })
}
assertTrue(tutorialHits.isEmpty, "A11yLabels_NoTutorialText", detail: "\(tutorialHits.map { $0.0 })")

// -----------------------------------------------------------------------------
print("Test Group 8: Every labelled control routes through A11yLabels and speaks ar-SA")

// No raw Arabic literals left in labels / values / action names: all come from A11yLabels.
let rawArabicLabels = offendingLines { line in
    (line.contains(".accessibilityLabel(\"") || line.contains(".accessibilityValue(\"")
        || line.contains("accessibilityAction(named: \""))
        && line.unicodeScalars.contains(where: { (0x0621...0x064A).contains($0.value) })
}
assertTrue(rawArabicLabels.isEmpty, "NoRawArabicLabelLiterals", detail: rawArabicLabels.joined(separator: " | "))

// Each file: at least one ar-SA speech-language modifier per accessibilityLabel.
for (rel, text) in sources.sorted(by: { $0.key < $1.key }) where rel != "Accessibility/AXTreeDumper.swift" {
    let labels = text.components(separatedBy: ".accessibilityLabel(").count - 1
    guard labels > 0 else { continue }
    let languages = text.components(separatedBy: ".accessibilitySpeechLanguage(A11yLabels.speechLanguage)").count - 1
    assertTrue(languages >= labels, "SpeechLanguage_\(rel)", detail: "\(labels) labels, \(languages) ar-SA modifiers")
}

let speechModifier = source("Accessibility/AccessibilitySpeechLanguage.swift")
assertTrue(speechModifier.contains("func accessibilitySpeechLanguage(_ languageCode: String) -> some View"), "SpeechLanguageModifier_Defined")
assertTrue(speechModifier.contains("-u-nu-latn"), "SpeechLanguageModifier_KeepsWesternDigits")
assertTrue(
    stealth.contains("override var accessibilityLanguage: String? { get { A11yLabels.speechLanguage } set {} }"),
    "SilentSurface_LanguageArSA"
)

print("\nSUITE 8 RESULTS: \(passedCount) passed, \(failedCount) failed.")
exit(failedCount == 0 ? 0 : 1)
