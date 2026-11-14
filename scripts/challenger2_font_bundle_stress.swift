import Foundation
import CoreText
import CoreGraphics
import CommonCrypto

// ============================================================================
// CHALLENGER 2 EMPIRICAL STRESS TEST HARNESS: FONT ASSETS & RUNTIME BUNDLE
// ============================================================================

var passedChecks = 0
var failedChecks = 0

func assertCheck(_ condition: Bool, _ title: String, _ details: String = "") {
    if condition {
        print("  [PASS] \(title)\(details.isEmpty ? "" : " — " + details)")
        passedChecks += 1
    } else {
        print("  [FAIL] \(title)\(details.isEmpty ? "" : " — " + details)")
        failedChecks += 1
    }
}

func sha256(url: URL) -> String? {
    guard let data = try? Data(contentsOf: url) else { return nil }
    var hash = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
    data.withUnsafeBytes {
        _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
    }
    return hash.map { String(format: "%02x", $0) }.joined()
}

print("================================================================================")
print("  BLIND QURAN APP (iOS) — CHALLENGER 2 EMPIRICAL STRESS TEST SUITE")
print("  Focus: Font Asset Integrity, CoreText Shaping, and Runtime App Bundle")
print("================================================================================")

let expectedHash = "5e3eef8f67d99b522587aafef689f2bd8df61e050798ad388afa10dff045d65b"
let expectedByteSize = 241148
let expectedPostScriptName = "KFGQPCHAFSUthmanicScript-Regula"
let fontPath = "iosApp/Resources/Fonts/uthman_taha.ttf"
let androidFontPath = "app/src/main/res/font/uthman_taha.ttf"

// -----------------------------------------------------------------------------
// SECTION 1: BIT-FOR-BIT SHA-256 HASH & PARITY VERIFICATION
// -----------------------------------------------------------------------------
print("\n--- Section 1: Font File Integrity & Hash Parity ---")
let fontURL = URL(fileURLWithPath: fontPath)
let fontHash = sha256(url: fontURL)
assertCheck(fontHash == expectedHash, "iOS Font SHA-256 matches baseline", "Got: \(fontHash ?? "nil")")

let androidURL = URL(fileURLWithPath: androidFontPath)
let androidHash = sha256(url: androidURL)
assertCheck(androidHash == expectedHash, "Android Baseline Font SHA-256 verified", "Got: \(androidHash ?? "nil")")
assertCheck(fontHash == androidHash, "Bit-for-bit parity between iOS and Android fonts")

if let attrs = try? FileManager.default.attributesOfItem(atPath: fontPath),
   let size = attrs[.size] as? Int {
    assertCheck(size == expectedByteSize, "Font exact byte size matches \(expectedByteSize) bytes", "Size: \(size)")
} else {
    assertCheck(false, "Failed to read font file size")
}

// -----------------------------------------------------------------------------
// SECTION 2: CORETEXT REGISTRATION, METRICS & GLYPH RESOLUTION
// -----------------------------------------------------------------------------
print("\n--- Section 2: CoreText Registration, Metrics & Glyph Resolution ---")
guard let dataProvider = CGDataProvider(url: fontURL as CFURL),
      let cgFont = CGFont(dataProvider) else {
    fatalError("Failed to create CGFont from \(fontPath)")
}

var regError: Unmanaged<CFError>?
let registered = CTFontManagerRegisterGraphicsFont(cgFont, &regError)
if !registered, let err = regError?.takeRetainedValue() {
    let code = CFErrorGetCode(err)
    assertCheck(code == 105, "CTFontManager registration: already registered (code 105) or success")
} else {
    assertCheck(registered, "CTFontManagerRegisterGraphicsFont succeeded directly")
}

let psName = cgFont.postScriptName as String? ?? ""
assertCheck(psName == expectedPostScriptName, "PostScript Name matches", psName)
assertCheck(cgFont.fullName as String? == "KFGQPC HAFS Uthmanic Script Regular", "Full Name verified", cgFont.fullName as String? ?? "")
assertCheck(cgFont.numberOfGlyphs == 1412, "Glyph count strictly 1412", "\(cgFont.numberOfGlyphs)")
assertCheck(cgFont.unitsPerEm == 2048, "Units per Em strictly 2048", "\(cgFont.unitsPerEm)")

let ctFont = CTFontCreateWithName(expectedPostScriptName as CFString, 28.0, nil)
let createdPS = CTFontCopyPostScriptName(ctFont) as String
assertCheck(createdPS == expectedPostScriptName, "CTFont instantiated cleanly with correct PostScript name")
assertCheck(CTFontGetSize(ctFont) == 28.0, "CTFont point size 28.0")
assertCheck(CTFontGetAscent(ctFont) > 0, "Font ascent positive", "\(CTFontGetAscent(ctFont))")
assertCheck(CTFontGetDescent(ctFont) > 0, "Font descent positive", "\(CTFontGetDescent(ctFont))")

// -----------------------------------------------------------------------------
// SECTION 3: CANONICAL QURANIC AYAH STRESS & GLYPH COVERAGE
// -----------------------------------------------------------------------------
print("\n--- Section 3: Canonical Quranic Text & Glyph Mapping ---")
let testPassages: [(title: String, text: String)] = [
    ("Al-Fatiha Basmalah", "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۝١"),
    ("Al-Fatiha 1:7", "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ ۝٧"),
    ("Al-Ikhlas 112:1-4", "قُلْ هُوَ اللَّهُ أَحَدٌ ۝١ اللَّهُ الصَّمَدُ ۝٢ لَمْ يَلِدْ وَلَمْ يُولَدْ ۝٣ وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ ۝٤"),
    ("Ayat Al-Kursi (2:255 excerpt)", "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ ۝٢٥٥"),
    ("Quranic Waqf & Sajdah Symbols", "ۖ ۗ ۘ ۙ ۚ ۛ ۜ ۞ ۟ ۠ ۡ ۢ ۣ ۤ ۥ ۦ ۧ ۨ ۩ ۪ ۫ ۬ ۭ")
]

for passage in testPassages {
    let utf16 = Array(passage.text.utf16)
    var glyphs = [CGGlyph](repeating: 0, count: utf16.count)
    let ok = CTFontGetGlyphsForCharacters(ctFont, utf16, &glyphs, utf16.count)
    let missingCount = glyphs.filter { $0 == 0 }.count
    assertCheck(ok && missingCount == 0, "Glyph resolution: \(passage.title)", "Total chars: \(utf16.count), missing: \(missingCount)")
}

// -----------------------------------------------------------------------------
// SECTION 4: TYPESETTING & BITMAP RENDERING (ACTUAL PIXELS ON SCREEN)
// -----------------------------------------------------------------------------
print("\n--- Section 4: Typesetting & CGBitmapContext Pixel Drawing ---")

func renderTextToBitmap(text: String, font: CTFont, width: Int, height: Int) -> (renderedPixels: Int, durationMs: Double) {
    let start = DispatchTime.now()
    let colorSpace = CGColorSpaceCreateDeviceRGB()
    let bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue
    guard let ctx = CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4, space: colorSpace, bitmapInfo: bitmapInfo) else {
        return (0, 0)
    }

    ctx.setFillColor(CGColor(red: 0, green: 0, blue: 0, alpha: 1))
    ctx.fill(CGRect(x: 0, y: 0, width: width, height: height))

    let attrString = NSAttributedString(string: text, attributes: [
        kCTFontAttributeName as NSAttributedString.Key: font,
        kCTForegroundColorAttributeName as NSAttributedString.Key: CGColor(red: 1, green: 1, blue: 1, alpha: 1)
    ])
    let line = CTLineCreateWithAttributedString(attrString)
    ctx.textPosition = CGPoint(x: 10, y: 20)
    CTLineDraw(line, ctx)

    let end = DispatchTime.now()
    let durationMs = Double(end.uptimeNanoseconds - start.uptimeNanoseconds) / 1_000_000.0

    guard let pixelData = ctx.data else { return (0, durationMs) }
    let ptr = pixelData.bindMemory(to: UInt32.self, capacity: width * height)
    var nonZero = 0
    for i in 0..<(width * height) {
        if ptr[i] != 0xFF000000 && ptr[i] != 0 { nonZero += 1 }
    }
    return (nonZero, durationMs)
}

let renderBasmalah = renderTextToBitmap(text: "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", font: ctFont, width: 600, height: 60)
assertCheck(renderBasmalah.renderedPixels > 100, "Rendered Basmalah produced actual visible glyph pixels", "Pixels: \(renderBasmalah.renderedPixels), Time: \(String(format: "%.2f", renderBasmalah.durationMs)) ms")

// Stress Test: Empty String
let emptyRender = renderTextToBitmap(text: "", font: ctFont, width: 100, height: 40)
assertCheck(emptyRender.renderedPixels == 0, "Empty string rendered safely without crash or spurious pixels")

// Stress Test: 5,000 character repeated Ayah string (Throughput & memory stress)
let repeatedText = String(repeating: "اللَّهُ نُورُ السَّمَاوَاتِ وَالْأَرْضِ ۚ ", count: 150)
let stressRender = renderTextToBitmap(text: repeatedText, font: ctFont, width: 4000, height: 100)
assertCheck(stressRender.renderedPixels > 1000, "5,000-char extreme Arabic stress string rendered successfully", "Pixels: \(stressRender.renderedPixels), Time: \(String(format: "%.2f", stressRender.durationMs)) ms")
assertCheck(stressRender.durationMs < 500.0, "Stress rendering completed within performance budget (< 500ms)", "\(String(format: "%.2f", stressRender.durationMs)) ms")

// Stress Test: RTL and Bidi control characters
let bidiStress = "\u{200E}English: \u{200F}سُورَةُ الفَاتِحَةِ\u{200E} (1)\u{061C}"
let bidiRender = renderTextToBitmap(text: bidiStress, font: ctFont, width: 600, height: 60)
assertCheck(bidiRender.renderedPixels > 50, "Bidi control sequences (\u{200E}, \u{200F}, \u{061C}) rendered cleanly without crash")

// -----------------------------------------------------------------------------
// SECTION 5: RUNTIME APP BUNDLE VALIDATION
// -----------------------------------------------------------------------------
print("\n--- Section 5: Built Application Bundle (QuranBlind.app) ---")

let fm = FileManager.default
let derivedDataPath = ("~/Library/Developer/Xcode/DerivedData" as NSString).expandingTildeInPath

// Find newest QuranBlind.app in DerivedData
var appBundleURL: URL?
if let subpaths = try? fm.subpathsOfDirectory(atPath: derivedDataPath) {
    let appPaths = subpaths.filter { $0.hasSuffix("Debug-iphonesimulator/QuranBlind.app") }
    if let newest = appPaths.sorted(by: { p1, p2 in
        let d1 = (try? fm.attributesOfItem(atPath: "\(derivedDataPath)/\(p1)")[.modificationDate] as? Date) ?? Date.distantPast
        let d2 = (try? fm.attributesOfItem(atPath: "\(derivedDataPath)/\(p2)")[.modificationDate] as? Date) ?? Date.distantPast
        return d1 > d2
    }).first {
        appBundleURL = URL(fileURLWithPath: "\(derivedDataPath)/\(newest)")
    }
}

guard let bundleURL = appBundleURL else {
    fatalError("QuranBlind.app bundle not found in DerivedData!")
}
print("Found app bundle at: \(bundleURL.path)")

// Check Info.plist inside app bundle
let bundlePlistURL = bundleURL.appendingPathComponent("Info.plist")
assertCheck(fm.fileExists(atPath: bundlePlistURL.path), "Info.plist present inside QuranBlind.app bundle")

guard let plistData = try? Data(contentsOf: bundlePlistURL),
      let plist = try? PropertyListSerialization.propertyList(from: plistData, options: [], format: nil) as? [String: Any] else {
    fatalError("Failed to parse Info.plist in app bundle")
}

let displayName = plist["CFBundleDisplayName"] as? String ?? ""
assertCheck(displayName == "القرآن", "CFBundleDisplayName strictly 'القرآن'", displayName)

let version = plist["CFBundleShortVersionString"] as? String ?? ""
assertCheck(version == "1.1.0", "CFBundleShortVersionString strictly '1.1.0'", version)

let buildNumber = plist["CFBundleVersion"] as? String ?? ""
assertCheck(buildNumber == "3", "CFBundleVersion strictly '3'", buildNumber)

let uiAppFonts = plist["UIAppFonts"] as? [String] ?? []
assertCheck(uiAppFonts.contains("uthman_taha.ttf"), "UIAppFonts contains 'uthman_taha.ttf'", "\(uiAppFonts)")

if let icons = plist["CFBundleIcons"] as? [String: Any],
   let primaryIcon = icons["CFBundlePrimaryIcon"] as? [String: Any],
   let iconName = primaryIcon["CFBundleIconName"] as? String {
    assertCheck(iconName == "AppIcon", "CFBundleIcons primary icon binds to 'AppIcon'", iconName)
} else {
    assertCheck(false, "CFBundleIcons missing or malformed in Info.plist")
}

let backgroundModes = plist["UIBackgroundModes"] as? [String] ?? []
assertCheck(backgroundModes.contains("audio"), "UIBackgroundModes includes 'audio'", "\(backgroundModes)")

// Check font binary inside app bundle
let bundleFontURL = bundleURL.appendingPathComponent("uthman_taha.ttf")
assertCheck(fm.fileExists(atPath: bundleFontURL.path), "uthman_taha.ttf exists at bundle root")
let bundleFontHash = sha256(url: bundleFontURL)
assertCheck(bundleFontHash == expectedHash, "Bundled uthman_taha.ttf SHA-256 strictly matches baseline", bundleFontHash ?? "nil")

// Check Assets.car
let bundleCarURL = bundleURL.appendingPathComponent("Assets.car")
assertCheck(fm.fileExists(atPath: bundleCarURL.path), "Assets.car exists inside bundle")
if let carAttrs = try? fm.attributesOfItem(atPath: bundleCarURL.path),
   let carSize = carAttrs[.size] as? Int {
    assertCheck(carSize > 50000, "Assets.car size > 50KB (\(carSize) bytes)")
} else {
    assertCheck(false, "Could not read Assets.car size")
}

// Check other mandatory bundle resources
assertCheck(fm.fileExists(atPath: bundleURL.appendingPathComponent("quran_uthmani_tanzil.json").path), "quran_uthmani_tanzil.json present in bundle")
assertCheck(fm.fileExists(atPath: bundleURL.appendingPathComponent("PrivacyInfo.xcprivacy").path), "PrivacyInfo.xcprivacy present in bundle")
assertCheck(fm.fileExists(atPath: bundleURL.appendingPathComponent("QuranBlind").path), "QuranBlind Mach-O executable binary present in bundle")

// -----------------------------------------------------------------------------
// SUMMARY VERDICT
// -----------------------------------------------------------------------------
print("\n================================================================================")
print("  STRESS TEST COMPLETE: \(passedChecks) Passed, \(failedChecks) Failed (Total: \(passedChecks + failedChecks))")
if failedChecks == 0 {
    print("  VERDICT: APPROVE (100% Pass Rate across all stress dimensions)")
} else {
    print("  VERDICT: REQUEST_CHANGES (\(failedChecks) failure(s) detected)")
}
print("================================================================================")

exit(failedChecks == 0 ? 0 : 1)
