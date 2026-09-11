import SwiftUI

// SharedKit is deliberately NOT imported yet. The framework does not exist until CI
// produces it; importing it now guarantees a red first CI run for no informational gain.
struct ContentView: View {
    var body: some View {
        Text("SharedKit wiring pending")
            .accessibilityLabel("قيد الإعداد")
    }
}
