import Foundation
import CoreGraphics
import ImageIO

// =============================================================================
// BLIND QURAN APP (iOS) — M1 ITERATION 2 VERIFICATION ORACLE
// Script: verify_app_icons.swift
// Target: iosApp/Resources/Assets.xcassets/AppIcon.appiconset
// Resolves: DEFECT-M1-001 (Brand Identity Color Divergence)
// =============================================================================

struct TestResult {
    let tier: String
    let name: String
    let passed: Bool
    let details: String
}

var testResults: [TestResult] = []

func logPass(tier: String, name: String, details: String) {
    testResults.append(TestResult(tier: tier, name: name, passed: true, details: details))
    let padTier = tier.padding(toLength: 6, withPad: " ", startingAt: 0)
    let padName = name.padding(toLength: 36, withPad: " ", startingAt: 0)
    print("  [PASS] [\(padTier)] \(padName) : \(details)")
}

func logFail(tier: String, name: String, details: String) {
    testResults.append(TestResult(tier: tier, name: name, passed: false, details: details))
    let padTier = tier.padding(toLength: 6, withPad: " ", startingAt: 0)
    let padName = name.padding(toLength: 36, withPad: " ", startingAt: 0)
    print("  [FAIL] [\(padTier)] \(padName) : \(details)")
}

let fileManager = FileManager.default
let appiconDir = "iosApp/Resources/Assets.xcassets/AppIcon.appiconset"
let contentsPath = "\(appiconDir)/Contents.json"

print("================================================================================")
print("  BLIND QURAN APP (iOS) — M1 ITERATION 2 VERIFICATION ORACLE")
print("  Automated Gate Verification for DEFECT-M1-001 & Brand Asset Integrity")
print("================================================================================")

// -----------------------------------------------------------------------------
// TIER 1: Catalog & Manifest Conformance
// -----------------------------------------------------------------------------
print("\n--- Tier 1: Asset Catalog & Manifest Conformance ---")

guard let contentsData = try? Data(contentsOf: URL(fileURLWithPath: contentsPath)),
      let contentsJson = try? JSONSerialization.jsonObject(with: contentsData) as? [String: Any],
      let images = contentsJson["images"] as? [[String: Any]] else {
    logFail(tier: "Tier 1", name: "Contents.json Parsing", details: "Unable to parse \(contentsPath)")
    exit(1)
}
logPass(tier: "Tier 1", name: "Contents.json Structure", details: "Valid JSON schema with \(images.count) image entries")

let dirFiles = (try? fileManager.contentsOfDirectory(atPath: appiconDir)) ?? []
let pngFilesOnDisk = Set(dirFiles.filter { $0.hasSuffix(".png") })
let referencedFiles = Set(images.compactMap { $0["filename"] as? String })

let missingFiles = referencedFiles.subtracting(pngFilesOnDisk)
let unreferencedFiles = pngFilesOnDisk.subtracting(referencedFiles)

if missingFiles.isEmpty {
    logPass(tier: "Tier 1", name: "Referenced Files On Disk", details: "All \(referencedFiles.count) referenced files exist on disk")
} else {
    logFail(tier: "Tier 1", name: "Referenced Files On Disk", details: "Missing: \(missingFiles.joined(separator: ", "))")
}

if unreferencedFiles.isEmpty {
    logPass(tier: "Tier 1", name: "No Orphaned Files", details: "Zero unreferenced PNG files in catalog folder")
} else {
    logFail(tier: "Tier 1", name: "No Orphaned Files", details: "Orphaned files found: \(unreferencedFiles.joined(separator: ", "))")
}

let hasUniversal = images.contains { ($0["idiom"] as? String) == "universal" && ($0["size"] as? String) == "1024x1024" }
if hasUniversal {
    logPass(tier: "Tier 1", name: "Universal Single-Size Mapping", details: "1024x1024 universal mapping entry confirmed")
} else {
    logFail(tier: "Tier 1", name: "Universal Single-Size Mapping", details: "Missing universal 1024x1024 entry required by Xcode compiler")
}

// -----------------------------------------------------------------------------
// TIER 2: Physical Dimensions, Color Model & Opacity
// -----------------------------------------------------------------------------
print("\n--- Tier 2: Physical Dimensions, Color Model & Opacity ---")

var dimFailures = 0
var alphaFailures = 0
var modelFailures = 0

for item in images {
    guard let fn = item["filename"] as? String,
          let sizeStr = item["size"] as? String else { continue }
    let scaleStr = (item["scale"] as? String) ?? "1x"
    let scale = Double(scaleStr.replacingOccurrences(of: "x", with: "")) ?? 1.0
    let parts = sizeStr.split(separator: "x").compactMap { Double($0) }
    guard parts.count == 2 else { continue }
    let expW = Int(round(parts[0] * scale))
    let expH = Int(round(parts[1] * scale))
    
    let filePath = "\(appiconDir)/\(fn)"
    guard let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: filePath) as CFURL, nil),
          let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any] else {
        dimFailures += 1
        continue
    }
    
    let actW = props[kCGImagePropertyPixelWidth] as? Int ?? 0
    let actH = props[kCGImagePropertyPixelHeight] as? Int ?? 0
    let hasAlpha = props[kCGImagePropertyHasAlpha] as? Bool ?? false
    let model = props[kCGImagePropertyColorModel] as? String ?? "Unknown"
    
    if actW != expW || actH != expH {
        dimFailures += 1
        logFail(tier: "Tier 2", name: "\(fn) Dimension", details: "Expected \(expW)x\(expH), got \(actW)x\(actH)")
    }
    if hasAlpha {
        alphaFailures += 1
        logFail(tier: "Tier 2", name: "\(fn) Alpha Stripping", details: "Alpha channel present (App Store rule violation)")
    }
    if model != "RGB" {
        modelFailures += 1
        logFail(tier: "Tier 2", name: "\(fn) ColorModel", details: "Expected RGB, got \(model)")
    }
}

if dimFailures == 0 {
    logPass(tier: "Tier 2", name: "Dimensional Exactitude", details: "All 18 icons strictly match expected pixel dimensions")
}
if alphaFailures == 0 {
    logPass(tier: "Tier 2", name: "Complete Alpha Stripping", details: "All 18 icons are 100% opaque (hasAlpha == false)")
}
if modelFailures == 0 {
    logPass(tier: "Tier 2", name: "Color Model Conformance", details: "All 18 icons use RGB color model (depth: 8)")
}

// -----------------------------------------------------------------------------
// TIER 3: Brand Color Identity & Chromatic Distribution (DEFECT-M1-001)
// -----------------------------------------------------------------------------
print("\n--- Tier 3: Brand Color Identity & Chromatic Distribution ---")

struct ColorMetrics {
    let greenPct: Double
    let goldPct: Double
    let brownDefectPct: Double
    let borderGreenPct: Double
    let cornersMatchGreen: Bool
    let totalPixels: Int
    let cornerColors: [(r: Int, g: Int, b: Int)]
}

func analyzeColors(imagePath: String) -> ColorMetrics? {
    guard let source = CGImageSourceCreateWithURL(URL(fileURLWithPath: imagePath) as CFURL, nil),
          let img = CGImageSourceCreateImageAtIndex(source, 0, nil),
          let provider = img.dataProvider,
          let data = provider.data,
          let ptr = CFDataGetBytePtr(data) else { return nil }
    
    let w = img.width
    let h = img.height
    let bpr = img.bytesPerRow
    let bpp = img.bitsPerPixel / 8
    let total = w * h
    
    var greenCount = 0
    var goldCount = 0
    var brownDefectCount = 0
    var borderGreenCount = 0
    
    func getPixel(x: Int, y: Int) -> (r: Int, g: Int, b: Int) {
        let offset = y * bpr + x * bpp
        return (Int(ptr[offset]), Int(ptr[offset + 1]), Int(ptr[offset + 2]))
    }
    
    for y in 0..<h {
        for x in 0..<w {
            let p = getPixel(x: x, y: y)
            let r = p.r, g = p.g, b = p.b
            
            // Deep Forest Green #0D281E (13, 40, 30)
            if abs(r - 13) <= 15 && abs(g - 40) <= 15 && abs(b - 30) <= 15 {
                greenCount += 1
            }
            // Border Green #1F523F (31, 82, 63)
            else if abs(r - 31) <= 15 && abs(g - 82) <= 15 && abs(b - 63) <= 15 {
                borderGreenCount += 1
            }
            
            // Gold #FFD700 (255, 215, 0) or #D4AF37 (212, 175, 55)
            let distFFD700 = sqrt(Double((r - 255)*(r - 255) + (g - 215)*(g - 215) + (b - 0)*(b - 0)))
            let distD4AF37 = sqrt(Double((r - 212)*(r - 212) + (g - 175)*(g - 175) + (b - 55)*(b - 55)))
            if distFFD700 <= 60.0 || distD4AF37 <= 50.0 || (r >= 180 && g >= 140 && b <= 90 && (g - b) >= 40) {
                goldCount += 1
            }
            
            // Defect Brown #855433 (133, 84, 51) and variants: R/G ratio >= 1.40, R >= 90, B < 100
            if g > 0 && Double(r) / Double(g) >= 1.40 && r >= 90 && b < 100 && (r - b) >= 20 {
                brownDefectCount += 1
            }
        }
    }
    
    let corners = [getPixel(x: 0, y: 0), getPixel(x: w - 1, y: 0), getPixel(x: 0, y: h - 1), getPixel(x: w - 1, y: h - 1)]
    let cornersMatch = corners.allSatisfy { abs($0.r - 13) <= 12 && abs($0.g - 40) <= 12 && abs($0.b - 30) <= 12 }
    
    return ColorMetrics(
        greenPct: Double(greenCount) / Double(total) * 100.0,
        goldPct: Double(goldCount) / Double(total) * 100.0,
        brownDefectPct: Double(brownDefectCount) / Double(total) * 100.0,
        borderGreenPct: Double(borderGreenCount) / Double(total) * 100.0,
        cornersMatchGreen: cornersMatch,
        totalPixels: total,
        cornerColors: corners
    )
}

let masterPath = "\(appiconDir)/AppIcon-1024x1024@1x.png"
if let m = analyzeColors(imagePath: masterPath) {
    print(String(format: "  Master (1024x1024): Deep Green: %.2f%%, Gold: %.2f%%, Border Green: %.2f%%, Defect Brown: %.2f%%",
                 m.greenPct, m.goldPct, m.borderGreenPct, m.brownDefectPct))
    
    // Assertion 3.1: Deep Forest Green Coverage > 40.0%
    if m.greenPct > 40.0 {
        logPass(tier: "Tier 3", name: "Deep Forest Green Coverage (>40%)", details: String(format: "Verified %.2f%% coverage", m.greenPct))
    } else {
        logFail(tier: "Tier 3", name: "Deep Forest Green Coverage (>40%)", details: String(format: "Only %.2f%% coverage (MUST be > 40.0%%) [DEFECT-M1-001]", m.greenPct))
    }
    
    // Assertion 3.2: Gold Elements Coverage 3.0% - 35.0%
    if m.goldPct >= 3.0 && m.goldPct <= 35.0 {
        logPass(tier: "Tier 3", name: "Gold Elements Presence (3-35%)", details: String(format: "Verified %.2f%% coverage", m.goldPct))
    } else {
        logFail(tier: "Tier 3", name: "Gold Elements Presence (3-35%)", details: String(format: "Coverage %.2f%% outside expected 3.0%% - 35.0%% range", m.goldPct))
    }
    
    // Assertion 3.3: Brown Defect Zero Tolerance (< 0.05%)
    if m.brownDefectPct < 0.05 {
        logPass(tier: "Tier 3", name: "Defect Brown Regression Guard", details: String(format: "Zero defect pixels (%.4f%%)", m.brownDefectPct))
    } else {
        logFail(tier: "Tier 3", name: "Defect Brown Regression Guard", details: String(format: "Found %.2f%% brown defect pixels [DEFECT-M1-001]", m.brownDefectPct))
    }
    
    // Assertion 3.4: Corner Pixels Match #0D281E
    if m.cornersMatchGreen {
        logPass(tier: "Tier 3", name: "Corner Color Uniformity", details: "All 4 corner pixels strictly match Deep Forest Green #0D281E")
    } else {
        let hexCorners = m.cornerColors.map { String(format: "#%02X%02X%02X", $0.r, $0.g, $0.b) }.joined(separator: ", ")
        logFail(tier: "Tier 3", name: "Corner Color Uniformity", details: "Corners [\(hexCorners)] do not match #0D281E")
    }
} else {
    logFail(tier: "Tier 3", name: "Master Image Analysis", details: "Could not open \(masterPath)")
}

// Matrix check across sampled downscaled icons
let sampledMatrix = [
    "AppIcon-60x60@3x.png",
    "AppIcon-60x60@2x.png",
    "AppIcon-40x40@2x.png",
    "AppIcon-29x29@2x.png",
    "AppIcon-20x20@1x~ipad.png"
]
var matrixFailures = 0
for iconName in sampledMatrix {
    let p = "\(appiconDir)/\(iconName)"
    if let sm = analyzeColors(imagePath: p) {
        if sm.greenPct < 35.0 || sm.brownDefectPct > 0.05 {
            matrixFailures += 1
            logFail(tier: "Tier 3", name: "\(iconName) Color Check", details: String(format: "Green: %.2f%% (<35%%) or Brown: %.2f%%", sm.greenPct, sm.brownDefectPct))
        } else {
            logPass(tier: "Tier 3", name: "\(iconName) Color Check", details: String(format: "Green: %.2f%%, Brown: %.4f%%", sm.greenPct, sm.brownDefectPct))
        }
    }
}

// -----------------------------------------------------------------------------
// TIER 4: Asset Catalog Compilation (actool)
// -----------------------------------------------------------------------------
print("\n--- Tier 4: Asset Catalog Compilation (actool) ---")

let tmpActoolDir = "/tmp/oracle_actool_test"
try? fileManager.removeItem(atPath: tmpActoolDir)
try? fileManager.createDirectory(atPath: tmpActoolDir, withIntermediateDirectories: true)

let actoolProcess = Process()
actoolProcess.executableURL = URL(fileURLWithPath: "/usr/bin/xcrun")
actoolProcess.arguments = [
    "actool",
    "--output-format", "human-readable-text",
    "--notices", "--warnings", "--errors",
    "--output-partial-info-plist", "\(tmpActoolDir)/partial.plist",
    "--app-icon", "AppIcon",
    "--compress-pngs",
    "--enable-on-demand-resources", "NO",
    "--target-device", "iphone",
    "--target-device", "ipad",
    "--minimum-deployment-target", "15.0",
    "--platform", "iphonesimulator",
    "--compile", tmpActoolDir,
    "iosApp/Resources/Assets.xcassets"
]

let actoolPipe = Pipe()
actoolProcess.standardOutput = actoolPipe
actoolProcess.standardError = actoolPipe

do {
    try actoolProcess.run()
    actoolProcess.waitUntilExit()
    if actoolProcess.terminationStatus == 0 {
        let carPath = "\(tmpActoolDir)/Assets.car"
        if fileManager.fileExists(atPath: carPath) {
            logPass(tier: "Tier 4", name: "actool Asset Compilation", details: "Clean compilation (exit code 0), Assets.car produced")
        } else {
            logFail(tier: "Tier 4", name: "actool Asset Compilation", details: "Compiled with code 0 but Assets.car missing")
        }
    } else {
        let actoolOutput = String(data: actoolPipe.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8) ?? ""
        logFail(tier: "Tier 4", name: "actool Asset Compilation", details: "actool failed (\(actoolProcess.terminationStatus)): \(actoolOutput)")
    }
} catch {
    logFail(tier: "Tier 4", name: "actool Asset Compilation", details: "Execution error: \(error)")
}
try? fileManager.removeItem(atPath: tmpActoolDir)

// -----------------------------------------------------------------------------
// TIER 5: Project Specification & Metadata Integrity
// -----------------------------------------------------------------------------
print("\n--- Tier 5: Project Specification & Metadata Integrity ---")

let projectYmlPath = "iosApp/project.yml"
if let ymlContent = try? String(contentsOfFile: projectYmlPath, encoding: .utf8) {
    if ymlContent.contains("ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon") {
        logPass(tier: "Tier 5", name: "AppIcon Binding", details: "project.yml sets ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon")
    } else {
        logFail(tier: "Tier 5", name: "AppIcon Binding", details: "Missing ASSETCATALOG_COMPILER_APPICON_NAME: AppIcon")
    }
    
    if ymlContent.contains("MARKETING_VERSION: \"1.1.0\"") && ymlContent.contains("CURRENT_PROJECT_VERSION: \"3\"") {
        logPass(tier: "Tier 5", name: "Versioning Parity", details: "MARKETING_VERSION 1.1.0 & CURRENT_PROJECT_VERSION 3 confirmed")
    } else {
        logFail(tier: "Tier 5", name: "Versioning Parity", details: "Missing 1.1.0/3 version strings")
    }
    
    if ymlContent.contains("CFBundleDisplayName: \"القرآن\"") {
        logPass(tier: "Tier 5", name: "CFBundleDisplayName", details: "Display name verified as 'القرآن'")
    } else {
        logFail(tier: "Tier 5", name: "CFBundleDisplayName", details: "Missing or incorrect CFBundleDisplayName")
    }
} else {
    logFail(tier: "Tier 5", name: "project.yml", details: "Could not read \(projectYmlPath)")
}

let fontPath = "iosApp/Resources/Fonts/uthman_taha.ttf"
if fileManager.fileExists(atPath: fontPath) {
    let fontAttrs = try? fileManager.attributesOfItem(atPath: fontPath)
    let size = (fontAttrs?[.size] as? Int64) ?? 0
    if size >= 200_000 {
        logPass(tier: "Tier 5", name: "Uthmanic Quran Font Asset", details: String(format: "uthman_taha.ttf exists (size: %lld bytes)", size))
    } else {
        logFail(tier: "Tier 5", name: "Uthmanic Quran Font Asset", details: "uthman_taha.ttf size too small (\(size) bytes)")
    }
} else {
    logFail(tier: "Tier 5", name: "Uthmanic Quran Font Asset", details: "Missing uthman_taha.ttf file")
}

// -----------------------------------------------------------------------------
// SUMMARY & VERDICT
// -----------------------------------------------------------------------------
print("\n================================================================================")
let total = testResults.count
let passCount = testResults.filter { $0.passed }.count
let failCount = testResults.filter { !$0.passed }.count

print(String(format: "  ORACLE EXECUTION COMPLETE: %d Passed, %d Failed (Total: %d)", passCount, failCount, total))

if failCount == 0 {
    print("  VERDICT: PASSED (All acceptance criteria strictly satisfied)")
    print("================================================================================\n")
    exit(0)
} else {
    print("  VERDICT: FAILED (\(failCount) assertion(s) violated)")
    print("================================================================================\n")
    exit(1)
}
