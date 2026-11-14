//
//  AXTreeDumper.swift
//  QuranBlind
//
//  DEBUG-only: prints the live UIKit accessibility tree (label, value, hint, traits, speech
//  language) when launched with `-dumpAXTree`, so VoiceOver output can be audited on Simulator.
//

#if DEBUG
import UIKit

enum AXTreeDumper {
    static var isRequested: Bool {
        ProcessInfo.processInfo.arguments.contains("-dumpAXTree")
    }

    static func dump(from root: NSObject) {
        var lines: [String] = []
        walk(root, depth: 0, into: &lines)
        print("AXDUMP BEGIN (\(lines.count) elements)")
        lines.forEach { print("AXDUMP \($0)") }
        print("AXDUMP END")
    }

    private static func children(of object: NSObject) -> [NSObject] {
        if let elements = object.accessibilityElements as? [NSObject], !elements.isEmpty {
            return elements
        }
        let count = object.accessibilityElementCount()
        if count != NSNotFound, count > 0 {
            return (0..<count).compactMap { object.accessibilityElement(at: $0) as? NSObject }
        }
        if let view = object as? UIView {
            return view.subviews
        }
        return []
    }

    private static func walk(_ object: NSObject, depth: Int, into lines: inout [String]) {
        guard depth < 60 else { return }
        if let view = object as? UIView, view.isHidden || view.accessibilityElementsHidden { return }
        if object.accessibilityElementsHidden { return }
        if object.isAccessibilityElement {
            lines.append(describe(object))
            return
        }
        children(of: object).forEach { walk($0, depth: depth + 1, into: &lines) }
    }

    private static func describe(_ object: NSObject) -> String {
        var speechLanguages = Set<String>()
        if let attributed = object.accessibilityAttributedLabel, attributed.length > 0 {
            attributed.enumerateAttribute(.accessibilitySpeechLanguage, in: NSRange(location: 0, length: attributed.length)) { value, _, _ in
                if let value = value as? String { speechLanguages.insert(value) }
            }
        }
        let label = object.accessibilityLabel ?? ""
        let value = object.accessibilityValue ?? ""
        let hint = object.accessibilityHint ?? ""
        return "label=\"\(label)\" value=\"\(value)\" hint=\"\(hint)\" traits=\(object.accessibilityTraits.rawValue)"
            + " language=\(object.accessibilityLanguage ?? "nil") speechLanguage=\(speechLanguages.sorted())"
            + " class=\(type(of: object))"
    }
}
#endif
