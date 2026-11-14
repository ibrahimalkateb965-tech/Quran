#!/usr/bin/env swift
import Foundation
import AppKit
import CoreGraphics
import ImageIO

// ==============================================================================
// Blind Quran App (iOS) — Complete AppIcon Generator (Vector Master Tooling)
// Milestones: M1 Parity (1:1 with Android Google Play Baseline v1.1.0)
// Brand Identity: Deep Forest Green (#0D281E), Borders (#1F523F, #D4AF37),
//                 Gold (#FFD700) Sound Waves, Crescent Moon, Star, Open Quran.
// Output: 24-bit sRGB opaque PNGs (hasAlpha: no), Contents.json catalog.
// ==============================================================================

struct IconDefinition {
    let idiom: String
    let size: String
    let scale: String
    let pixelWidth: Int
    let filename: String
}

let iconCatalog: [IconDefinition] = [
    // iPhone icons
    IconDefinition(idiom: "iphone", size: "20x20", scale: "2x", pixelWidth: 40, filename: "AppIcon-20x20@2x.png"),
    IconDefinition(idiom: "iphone", size: "20x20", scale: "3x", pixelWidth: 60, filename: "AppIcon-20x20@3x.png"),
    IconDefinition(idiom: "iphone", size: "29x29", scale: "2x", pixelWidth: 58, filename: "AppIcon-29x29@2x.png"),
    IconDefinition(idiom: "iphone", size: "29x29", scale: "3x", pixelWidth: 87, filename: "AppIcon-29x29@3x.png"),
    IconDefinition(idiom: "iphone", size: "40x40", scale: "2x", pixelWidth: 80, filename: "AppIcon-40x40@2x.png"),
    IconDefinition(idiom: "iphone", size: "40x40", scale: "3x", pixelWidth: 120, filename: "AppIcon-40x40@3x.png"),
    IconDefinition(idiom: "iphone", size: "60x60", scale: "2x", pixelWidth: 120, filename: "AppIcon-60x60@2x.png"),
    IconDefinition(idiom: "iphone", size: "60x60", scale: "3x", pixelWidth: 180, filename: "AppIcon-60x60@3x.png"),
    
    // iPad icons
    IconDefinition(idiom: "ipad", size: "20x20", scale: "1x", pixelWidth: 20, filename: "AppIcon-20x20@1x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "20x20", scale: "2x", pixelWidth: 40, filename: "AppIcon-20x20@2x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "29x29", scale: "1x", pixelWidth: 29, filename: "AppIcon-29x29@1x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "29x29", scale: "2x", pixelWidth: 58, filename: "AppIcon-29x29@2x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "40x40", scale: "1x", pixelWidth: 40, filename: "AppIcon-40x40@1x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "40x40", scale: "2x", pixelWidth: 80, filename: "AppIcon-40x40@2x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "76x76", scale: "1x", pixelWidth: 76, filename: "AppIcon-76x76@1x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "76x76", scale: "2x", pixelWidth: 152, filename: "AppIcon-76x76@2x~ipad.png"),
    IconDefinition(idiom: "ipad", size: "83.5x83.5", scale: "2x", pixelWidth: 167, filename: "AppIcon-83.5x83.5@2x~ipad.png"),
    
    // App Store Marketing icon
    IconDefinition(idiom: "ios-marketing", size: "1024x1024", scale: "1x", pixelWidth: 1024, filename: "AppIcon-1024x1024@1x.png")
]

// MARK: - Android Vector XML Parser
class VectorDrawableParser: NSObject, XMLParserDelegate {
    struct VectorPath {
        var pathData: String = ""
        var fillColor: String? = nil
        var strokeColor: String? = nil
        var strokeWidth: String? = nil
        var strokeLineCap: String? = nil
    }
    
    var viewportWidth: String = "108"
    var viewportHeight: String = "108"
    var paths: [VectorPath] = []
    
    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        if elementName == "vector" {
            if let vw = attributeDict["android:viewportWidth"] { viewportWidth = vw }
            if let vh = attributeDict["android:viewportHeight"] { viewportHeight = vh }
        } else if elementName == "path" {
            var vp = VectorPath()
            if let pd = attributeDict["android:pathData"] { vp.pathData = pd }
            if let fc = attributeDict["android:fillColor"] {
                if fc == "#00000000" || fc.lowercased() == "#00ffffff" {
                    vp.fillColor = "none"
                } else if fc.starts(with: "#") {
                    vp.fillColor = fc
                }
            }
            if let sc = attributeDict["android:strokeColor"], sc.starts(with: "#") {
                vp.strokeColor = sc
            }
            if let sw = attributeDict["android:strokeWidth"] {
                vp.strokeWidth = sw
            }
            if let slc = attributeDict["android:strokeLineCap"] {
                vp.strokeLineCap = slc
            }
            paths.append(vp)
        }
    }
}

func parseVectorDrawable(at path: String) -> VectorDrawableParser? {
    guard let data = FileManager.default.contents(atPath: path) else { return nil }
    let delegate = VectorDrawableParser()
    let parser = XMLParser(data: data)
    parser.delegate = delegate
    guard parser.parse() else { return nil }
    return delegate
}

func synthesizeSVG(bgParser: VectorDrawableParser, fgParser: VectorDrawableParser) -> String {
    var svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
    svg += "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 \(bgParser.viewportWidth) \(bgParser.viewportHeight)\" width=\"1024\" height=\"1024\">\n"
    func writePath(_ p: VectorDrawableParser.VectorPath) {
        svg += "  <path"
        if let fill = p.fillColor { svg += " fill=\"\(fill)\"" }
        else { svg += " fill=\"none\"" }
        if let stroke = p.strokeColor { svg += " stroke=\"\(stroke)\"" }
        if let sw = p.strokeWidth { svg += " stroke-width=\"\(sw)\"" }
        if let cap = p.strokeLineCap { svg += " stroke-linecap=\"\(cap)\"" }
        svg += " d=\"\(p.pathData)\""
        svg += "/>\n"
    }
    svg += "  <!-- Background Layer -->\n"
    for p in bgParser.paths { writePath(p) }
    svg += "  <!-- Foreground Layer -->\n"
    for p in fgParser.paths { writePath(p) }
    svg += "</svg>\n"
    return svg
}

// Built-in verified baseline vector constant (resilient fallback)
let fallbackBrandSVG = """
<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="1024" height="1024">
  <!-- Background -->
  <path fill="#0D281E" d="M0,0h108v108h-108z"/>
  <!-- Borders -->
  <path fill="none" stroke="#1F523F" stroke-width="1.5" d="M12,12h84v84h-84z"/>
  <path fill="none" stroke="#D4AF37" stroke-width="0.8" d="M18,18h72v72h-72z"/>
  <!-- Foreground Sound Waves -->
  <path fill="none" stroke="#FFD700" stroke-width="2.5" stroke-linecap="round" d="M30,54 A24,24 0 0,1 30,36 M78,54 A24,24 0 0,0 78,36"/>
  <path fill="none" stroke="#FFD700" stroke-width="2" stroke-linecap="round" d="M24,60 A32,32 0 0,1 24,30 M84,60 A32,32 0 0,0 84,30"/>
  <!-- Crescent Moon -->
  <path fill="#FFD700" d="M54,26 C48,26 43,30 43,37 C43,44 48,48 54,48 C51,48 47,44 47,37 C47,30 51,26 54,26 Z"/>
  <!-- Star -->
  <path fill="#FFD700" d="M56,33 L57.5,36 L61,36 L58,38 L59,41 L56,39 L53,41 L54,38 L51,36 L54.5,36 Z"/>
  <!-- Open Quran Book -->
  <path fill="#FFD700" d="M34,55 C42,52 50,56 54,58 C58,56 66,52 74,55 L74,75 C66,72 58,76 54,78 C50,76 42,72 34,75 Z"/>
  <path fill="#0D281E" d="M53,58 L55,58 L55,78 L53,78 Z"/>
  <!-- Book Page Accent Lines -->
  <path fill="none" stroke="#0D281E" stroke-width="1.2" d="M38,61 C43,59 48,62 51,63 M38,66 C43,64 48,67 51,68 M57,63 C60,62 65,59 70,61 M57,68 C60,67 65,64 70,66"/>
</svg>
"""

// MARK: - Core Rendering Function
func renderOpaquePNG(from vectorImage: NSImage?, or rasterImage: CGImage?, pixelWidth: Int, targetPath: String, colorSpace: CGColorSpace) -> Bool {
    guard let ctx = CGContext(
        data: nil,
        width: pixelWidth,
        height: pixelWidth,
        bitsPerComponent: 8,
        bytesPerRow: pixelWidth * 4,
        space: colorSpace,
        bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue // 24-bit sRGB opaque (hasAlpha: no)
    ) else {
        fputs("Error: Could not allocate CGContext for \(targetPath)\n", stderr)
        return false
    }
    
    if let vectorImage = vectorImage {
        // Direct crisp vector rendering at native pixel scale
        let gc = NSGraphicsContext(cgContext: ctx, flipped: false)
        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = gc
        vectorImage.draw(in: NSRect(x: 0, y: 0, width: pixelWidth, height: pixelWidth), from: .zero, operation: .copy, fraction: 1.0)
        NSGraphicsContext.restoreGraphicsState()
    } else if let rasterImage = rasterImage {
        // High-quality downsampling fallback for raster sources
        ctx.interpolationQuality = .high
        ctx.draw(rasterImage, in: CGRect(x: 0, y: 0, width: pixelWidth, height: pixelWidth))
    } else {
        return false
    }
    
    guard let cgImage = ctx.makeImage(),
          let dest = CGImageDestinationCreateWithURL(URL(fileURLWithPath: targetPath) as CFURL, "public.png" as CFString, 1, nil) else {
        fputs("Error: Could not create CGImageDestination for \(targetPath)\n", stderr)
        return false
    }
    
    CGImageDestinationAddImage(dest, cgImage, nil)
    return CGImageDestinationFinalize(dest)
}

// MARK: - Main Catalog Generation
func generateCatalog(outputDirectory: String, bgPath: String?, fgPath: String?, rasterSourcePath: String?) -> Bool {
    let fileManager = FileManager.default
    do {
        try fileManager.createDirectory(atPath: outputDirectory, withIntermediateDirectories: true)
    } catch {
        fputs("Error: Could not create output catalog directory: \(error)\n", stderr)
        return false
    }
    
    let sRGBColorSpace = CGColorSpace(name: CGColorSpace.sRGB)!
    var vectorImage: NSImage? = nil
    var rasterSourceImage: CGImage? = nil
    
    // Check if vector sources are available
    if let bgPath = bgPath, let fgPath = fgPath,
       let bgParser = parseVectorDrawable(at: bgPath),
       let fgParser = parseVectorDrawable(at: fgPath) {
        print("Successfully parsed vector drawables from \(bgPath) and \(fgPath).")
        let svgString = synthesizeSVG(bgParser: bgParser, fgParser: fgParser)
        if let data = svgString.data(using: .utf8), let img = NSImage(data: data) {
            vectorImage = img
            print("Synthesized vector NSImage loaded successfully from parsed XML.")
        }
    }
    
    // If no vector yet, check if a raster source was explicitly requested
    if vectorImage == nil, let rasterSourcePath = rasterSourcePath, !rasterSourcePath.isEmpty {
        let sourceURL = URL(fileURLWithPath: rasterSourcePath) as CFURL
        if let imageSource = CGImageSourceCreateWithURL(sourceURL, nil),
           let sourceImage = CGImageSourceCreateImageAtIndex(imageSource, 0, nil) {
            rasterSourceImage = sourceImage
            print("Loaded raster source image: \(sourceImage.width)x\(sourceImage.height)px from \(rasterSourcePath)")
        }
    }
    
    // If still nil, fall back to embedded verified brand SVG constant
    if vectorImage == nil && rasterSourceImage == nil {
        print("Using embedded verified baseline brand SVG vector.")
        if let data = fallbackBrandSVG.data(using: .utf8), let img = NSImage(data: data) {
            vectorImage = img
        } else {
            fputs("Critical Error: Unable to initialize vector rendering.\n", stderr)
            return false
        }
    }
    
    var imagesMetadata: [[String: String]] = []
    
    for def in iconCatalog {
        let targetPath = (outputDirectory as NSString).appendingPathComponent(def.filename)
        let success = renderOpaquePNG(
            from: vectorImage,
            or: rasterSourceImage,
            pixelWidth: def.pixelWidth,
            targetPath: targetPath,
            colorSpace: sRGBColorSpace
        )
        guard success else {
            fputs("Error: Failed to render icon \(def.filename)\n", stderr)
            return false
        }
        
        imagesMetadata.append([
            "filename": def.filename,
            "idiom": def.idiom,
            "scale": def.scale,
            "size": def.size
        ])
    }
    
    // Modern Xcode universal single-size entry
    imagesMetadata.append([
        "filename": "AppIcon-1024x1024@1x.png",
        "idiom": "universal",
        "platform": "ios",
        "size": "1024x1024"
    ])
    
    let contentsDict: [String: Any] = [
        "images": imagesMetadata,
        "info": [
            "author": "xcode",
            "version": 1
        ]
    ]
    
    do {
        let jsonData = try JSONSerialization.data(withJSONObject: contentsDict, options: [.prettyPrinted, .sortedKeys])
        let contentsPath = (outputDirectory as NSString).appendingPathComponent("Contents.json")
        try jsonData.write(to: URL(fileURLWithPath: contentsPath))
        print("Generated Contents.json with \(imagesMetadata.count) image entries.")
    } catch {
        fputs("Error writing Contents.json: \(error)\n", stderr)
        return false
    }
    
    // Also update app/src/main/ic_launcher-playstore.png to fix stale Android master asset
    let playstorePath = "app/src/main/ic_launcher-playstore.png"
    if fileManager.fileExists(atPath: playstorePath) {
        print("Updating stale raster master asset at \(playstorePath) (512x512)...")
        _ = renderOpaquePNG(
            from: vectorImage,
            or: rasterSourceImage,
            pixelWidth: 512,
            targetPath: playstorePath,
            colorSpace: sRGBColorSpace
        )
    }
    
    return true
}

// MARK: - CLI Invocation Handling
let args = CommandLine.arguments
var bgFile: String? = "app/src/main/res/drawable/ic_launcher_background.xml"
var fgFile: String? = "app/src/main/res/drawable/ic_launcher_foreground.xml"
var destDir: String = "iosApp/Resources/Assets.xcassets/AppIcon.appiconset"
var rasterFile: String? = nil

if args.count == 2 {
    if args[1].hasSuffix(".png") {
        rasterFile = args[1]
        bgFile = nil
        fgFile = nil
    } else {
        destDir = args[1]
    }
} else if args.count == 3 {
    if args[1].hasSuffix(".png") {
        rasterFile = args[1]
        destDir = args[2]
        bgFile = nil
        fgFile = nil
    } else {
        bgFile = args[1]
        fgFile = args[2]
    }
} else if args.count >= 4 {
    bgFile = args[1]
    fgFile = args[2]
    destDir = args[3]
}

print("Running iOS AppIcon generator...")
print("  Background vector: \(bgFile ?? "none")")
print("  Foreground vector: \(fgFile ?? "none")")
print("  Raster source:     \(rasterFile ?? "none")")
print("  Output catalog:    \(destDir)")

if generateCatalog(outputDirectory: destDir, bgPath: bgFile, fgPath: fgFile, rasterSourcePath: rasterFile) {
    print("\nSUCCESS: iOS AppIcon catalog generated cleanly at '\(destDir)'")
    exit(0)
} else {
    print("\nFAILURE: Could not generate iOS AppIcon catalog.")
    exit(1)
}
