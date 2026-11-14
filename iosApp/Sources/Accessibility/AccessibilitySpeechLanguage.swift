//
//  AccessibilitySpeechLanguage.swift
//  QuranBlind
//

import SwiftUI

public extension View {
    /// Makes VoiceOver speak this control with the given language's voice (e.g. `ar-SA`),
    /// even when the system VoiceOver voice is English.
    ///
    /// SwiftUI has no speech-language modifier; it derives each element's UIKit
    /// `accessibilityLanguage` from the environment locale (verified with the `-dumpAXTree`
    /// DEBUG dump: `language=ar-Arab-SA`). `-u-nu-latn` keeps on-screen digits Western (0-9).
    func accessibilitySpeechLanguage(_ languageCode: String) -> some View {
        environment(\.locale, Locale(identifier: "\(languageCode)-u-nu-latn"))
    }
}
