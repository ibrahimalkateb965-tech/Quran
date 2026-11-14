import UIKit

public final class HapticManager {
    public static let shared = HapticManager()
    private init() {}

    /// Standard button click: HeaderBar buttons, theme toggle, sheet row select
    public func click() {
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.prepare()
        generator.impactOccurred()
    }

    /// Double-tap confirmation: Replay current Ayah, Screen-off wake up
    public func doubleTap() {
        let generator = UIImpactFeedbackGenerator(style: .light)
        generator.prepare()
        generator.impactOccurred()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.08) {
            generator.impactOccurred()
        }
    }

    /// Selection tick: Swiping between Ayahs (Horizontal Pager), Picker changes
    public func selection() {
        let generator = UISelectionFeedbackGenerator()
        generator.prepare()
        generator.selectionChanged()
    }

    /// Mode Activated: Continuous Play ON (Two distinct pulses / Success feedback)
    public func toggleOn() {
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.success)
    }

    /// Mode Deactivated: Continuous Play OFF (One rigid pulse)
    public func toggleOff() {
        let generator = UIImpactFeedbackGenerator(style: .rigid)
        generator.prepare()
        generator.impactOccurred()
    }

    /// Heavy Action / Long press / Bookmarking
    public func heavy() {
        let generator = UIImpactFeedbackGenerator(style: .heavy)
        generator.prepare()
        generator.impactOccurred()
    }
}
