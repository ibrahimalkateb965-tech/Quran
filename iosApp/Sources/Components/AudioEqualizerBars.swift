import SwiftUI

/// 5-bar animated audio equalizer visualizer, hidden from VoiceOver accessibility tree.
/// Renders 5 vertical bars with total container height of 24pt, bar width 5pt, spacing 4pt, corner radius 2pt.
/// When isPlaying is false, bars rest at a static minimum height of 6pt (0.25 * 24pt).
public struct AudioEqualizerBars: View {
    @Environment(\.quranTheme) private var theme

    public let isPlaying: Bool

    @State private var animating = false

    private let minScales: [CGFloat] = [0.25, 0.25, 0.25, 0.25, 0.25]
    private let targetScales: [CGFloat] = [0.9, 0.35, 0.95, 0.45, 0.85]

    public init(isPlaying: Bool) {
        self.isPlaying = isPlaying
    }

    public var body: some View {
        HStack(alignment: .bottom, spacing: 4) {
            ForEach(0..<5, id: \.self) { index in
                RoundedRectangle(cornerRadius: 2)
                    .fill(theme.accent)
                    .frame(
                        width: 5,
                        height: 24 * (isPlaying && animating ? targetScales[index] : minScales[index])
                    )
                    .animation(
                        isPlaying
                            ? Animation.easeInOut(duration: 0.35 + Double(index) * 0.08)
                                .repeatForever(autoreverses: true)
                            : .default,
                        value: animating
                    )
            }
        }
        .frame(height: 24)
        .onAppear {
            if isPlaying {
                animating = true
            }
        }
        .onChange(of: isPlaying) { newValue in
            animating = newValue
        }
        .accessibilityHidden(true)
        .accessibilityElement(children: .ignore)
    }
}
