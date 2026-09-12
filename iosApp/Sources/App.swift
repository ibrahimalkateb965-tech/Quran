import SwiftUI
import SharedKit

@main
struct QuranBlindApp: App {
    init() {
        // Starts the shared Koin container (sharedModule + iosModule). Exported by
        // Kotlin/Native as doInitKoinIos because ObjC treats init* as an init family.
        KoinIosKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
