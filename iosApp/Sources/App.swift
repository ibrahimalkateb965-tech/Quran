import SwiftUI
import AVFoundation
import SharedKit

@main
struct QuranBlindApp: App {
    @StateObject private var themeManager = ThemeManager.shared
    @StateObject private var player = SurahPlayer()

    init() {
        // Starts the shared Koin container (sharedModule + iosModule). Exported by
        // Kotlin/Native as doInitKoinIos because ObjC treats init* as an init family.
        KoinIosKt.doInitKoinIos()

        // Configure AVAudioSession for background audio playback and system integration
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playback, mode: .spokenAudio, policy: .longFormAudio)
        try? session.setActive(true)
    }

    var body: some Scene {
        WindowGroup {
            QuranPlayerScreen()
                .environmentObject(themeManager)
                .environmentObject(player)
                .quranTheme(themeManager.theme)
                .preferredColorScheme(themeManager.isDark ? .dark : .light)
                .environment(\.layoutDirection, .rightToLeft)
        }
    }
}
