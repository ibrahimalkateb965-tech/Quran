import Foundation
import CoreText
import CoreGraphics

func runTests(fontPath: String) -> Bool {
    print("==================================================")
    print("Testing Font Asset at path: \(fontPath)")
    print("==================================================")

    guard FileManager.default.fileExists(atPath: fontPath) else {
        print("FAIL: Font file does not exist at path: \(fontPath)")
        return false
    }

    let url = URL(fileURLWithPath: fontPath)
    guard let dataProvider = CGDataProvider(url: url as CFURL) else {
        print("FAIL: Unable to create CGDataProvider for \(url)")
        return false
    }

    guard let cgFont = CGFont(dataProvider) else {
        print("FAIL: Unable to instantiate CGFont from dataProvider")
        return false
    }

    var regError: Unmanaged<CFError>?
    let registered = CTFontManagerRegisterGraphicsFont(cgFont, &regError)
    if !registered {
        if let err = regError?.takeRetainedValue() {
            let desc = CFErrorCopyDescription(err) as String
            let code = CFErrorGetCode(err)
            // Error code 105 means already registered, which is fine
            if code == 105 || desc.contains("already registered") {
                print("INFO: Font already registered (\(code)): \(desc)")
            } else {
                print("WARN: CTFontManagerRegisterGraphicsFont failed with code \(code): \(desc)")
            }
        }
    } else {
        print("PASS: CTFontManagerRegisterGraphicsFont succeeded.")
    }

    guard let postScriptName = cgFont.postScriptName as String? else {
        print("FAIL: cgFont has no postScriptName")
        return false
    }
    print("PostScript Name: \(postScriptName)")

    let expectedPSName = "KFGQPCHAFSUthmanicScript-Regula"
    if postScriptName != expectedPSName {
        print("FAIL: PostScript Name mismatch! Expected '\(expectedPSName)', got '\(postScriptName)'")
        return false
    }
    print("PASS: PostScript Name matches '\(expectedPSName)'")

    if let fullName = cgFont.fullName as String? {
        print("Full Name: \(fullName)")
    }
    let glyphCount = cgFont.numberOfGlyphs
    let unitsPerEm = cgFont.unitsPerEm
    print("Glyph Count: \(glyphCount)")
    print("Units Per Em: \(unitsPerEm)")

    if glyphCount <= 0 {
        print("FAIL: Font has zero glyphs")
        return false
    }

    // Now test CTFont instantiation
    let ctFont = CTFontCreateWithName(postScriptName as CFString, 24.0, nil)
    let createdPSName = CTFontCopyPostScriptName(ctFont) as String
    print("CTFont instantiated: \(createdPSName), size: \(CTFontGetSize(ctFont))")
    if createdPSName != expectedPSName {
        print("FAIL: CTFont PostScriptName mismatch. Expected \(expectedPSName), got \(createdPSName)")
        return false
    }
    print("PASS: CTFont instantiated cleanly.")

    // Test specific Quranic characters and glyph resolution
    let testCharacters: [(name: String, scalar: UnicodeScalar, expectedToExist: Bool)] = [
        ("Beh (ب)", UnicodeScalar(0x0628)!, true),
        ("Seen (س)", UnicodeScalar(0x0633)!, true),
        ("Meem (م)", UnicodeScalar(0x0645)!, true),
        ("Alef (ا)", UnicodeScalar(0x0627)!, true),
        ("Lam (ل)", UnicodeScalar(0x0644)!, true),
        ("Heh (ه)", UnicodeScalar(0x0647)!, true),
        ("Reh (ر)", UnicodeScalar(0x0631)!, true),
        ("Hah (ح)", UnicodeScalar(0x062D)!, true),
        ("Noon (ن)", UnicodeScalar(0x0646)!, true),
        ("Fatha (َ)", UnicodeScalar(0x064E)!, true),
        ("Kasra (ِ)", UnicodeScalar(0x0650)!, true),
        ("Damma (ُ)", UnicodeScalar(0x064F)!, true),
        ("Shadda (ّ)", UnicodeScalar(0x0651)!, true),
        ("Sukun (ْ)", UnicodeScalar(0x0652)!, true),
        ("Superscript Alef (ٰ)", UnicodeScalar(0x0670)!, true),
        ("Alef Wasla (ٱ)", UnicodeScalar(0x0671)!, true),
        ("End of Ayah (۝)", UnicodeScalar(0x06DD)!, true),
        ("Small High Sad-Lam-Alef (ۖ)", UnicodeScalar(0x06D6)!, true),
        ("Small High Qaf-Lam-Alef (ۗ)", UnicodeScalar(0x06D7)!, true),
        ("Small High Meem (ۘ)", UnicodeScalar(0x06D8)!, true),
        ("Small High Lam-Alef (ۙ)", UnicodeScalar(0x06D9)!, true),
        ("Small High Jeem (ۚ)", UnicodeScalar(0x06DA)!, true),
        ("Small High Three Dots (ۛ)", UnicodeScalar(0x06DB)!, true),
        ("Small High Seen (ۜ)", UnicodeScalar(0x06DC)!, true),
        ("Small High Rect Zero (۠)", UnicodeScalar(0x06E0)!, true),
        ("Small High Meem Isolated (ۢ)", UnicodeScalar(0x06E2)!, true),
        ("Small Waw (ۥ)", UnicodeScalar(0x06E5)!, true),
        ("Small Ya (ۦ)", UnicodeScalar(0x06E6)!, true),
        ("Arabic Indic Digit 1 (١)", UnicodeScalar(0x0661)!, true),
        ("Negative Test - Chinese Hanzi (中)", UnicodeScalar(0x4E2D)!, false)
    ]

    var allTestsPassed = true
    print("\n--- Testing Individual Character Glyphs ---")
    for item in testCharacters {
        let utf16 = Array(String(item.scalar).utf16)
        var glyphs = [CGGlyph](repeating: 0, count: utf16.count)
        let success = CTFontGetGlyphsForCharacters(ctFont, utf16, &glyphs, utf16.count)

        if item.expectedToExist {
            if !success || glyphs.contains(0) {
                print("FAIL: Expected glyph for \(item.name) [U+\(String(item.scalar.value, radix: 16, uppercase: true))], but got glyphs: \(glyphs)")
                allTestsPassed = false
            } else {
                print("PASS: \(item.name) [U+\(String(item.scalar.value, radix: 16, uppercase: true))] -> Glyph ID(s): \(glyphs)")
            }
        } else {
            // Negative test: should NOT have glyph in this specific font
            if success && !glyphs.contains(0) {
                print("WARN: Negative test character \(item.name) unexpectedly mapped to glyph \(glyphs)")
            } else {
                print("PASS (Negative Test): \(item.name) correctly unmapped (glyph 0 / not found)")
            }
        }
    }

    // Test a real Quranic text string (Surah Al-Fatiha Ayah 1)
    let basmalah = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ۝١"
    print("\n--- Testing Full Basmalah Recitation String ---")
    print("Text: \(basmalah)")
    let utf16Chars = Array(basmalah.utf16)
    var basmalahGlyphs = [CGGlyph](repeating: 0, count: utf16Chars.count)
    let bSuccess = CTFontGetGlyphsForCharacters(ctFont, utf16Chars, &basmalahGlyphs, utf16Chars.count)
    print("CTFontGetGlyphsForCharacters returned: \(bSuccess)")
    print("Glyphs count: \(basmalahGlyphs.count), Zero (missing) glyph count: \(basmalahGlyphs.filter { $0 == 0 }.count)")
    if basmalahGlyphs.filter({ $0 == 0 }).count > 0 {
        print("FAIL: Some characters in Basmalah failed glyph mapping!")
        for (i, char) in utf16Chars.enumerated() {
            if basmalahGlyphs[i] == 0 {
                print("  Missing glyph at index \(i): char 0x\(String(char, radix: 16))")
            }
        }
        allTestsPassed = false
    } else {
        print("PASS: Every character in Basmalah successfully resolved to a non-zero glyph!")
    }

    return allTestsPassed
}

let fontPath = CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "iosApp/Resources/Fonts/uthman_taha.ttf"
let result = runTests(fontPath: fontPath)
if result {
    print("\nALL FONT & CORETEXT TESTS PASSED SUCCESSFULLY!")
    exit(0)
} else {
    print("\nSOME FONT TESTS FAILED!")
    exit(1)
}
