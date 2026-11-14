import SwiftUI
import AVFoundation
import SharedKit
import UIKit

// MARK: - App Delegate Adaptor (App Lifecycle Integration - R3)

public class AppDelegate: NSObject, UIApplicationDelegate {
    public static weak var activeRootHostingController: UIViewController?
    private var observer: NSObjectProtocol?

    public func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        observer = NotificationCenter.default.addObserver(
            forName: Notification.Name("quranblind_root_controller_attached"),
            object: nil,
            queue: .main
        ) { notification in
            Self.activeRootHostingController = notification.object as? UIViewController
        }
        return true
    }

    deinit {
        if let observer = observer {
            NotificationCenter.default.removeObserver(observer)
        }
    }
}

@main
struct QuranBlindApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var themeManager = ThemeManager.shared
    @StateObject private var player = SurahPlayer()
    @StateObject private var voiceOver = VoiceOverObserver.shared

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
            RootHostingView {
                QuranPlayerScreen()
                    .environmentObject(themeManager)
                    .environmentObject(player)
                    .environmentObject(voiceOver)
                    .quranTheme(themeManager.theme)
                    .preferredColorScheme(themeManager.isDark ? .dark : .light)
                    .environment(\.layoutDirection, .rightToLeft)
            }
        }
    }
}
