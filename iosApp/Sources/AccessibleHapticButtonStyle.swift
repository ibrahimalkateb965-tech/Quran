import SwiftUI

public struct AccessibleHapticButtonStyle: ButtonStyle {
    public var haptic: HapticStyle = .click
    public var scaleDown: CGFloat = 0.94

    public enum HapticStyle {
        case click, doubleTap, toggleOn, toggleOff, selection, heavy, none
    }

    public init(haptic: HapticStyle = .click, scaleDown: CGFloat = 0.94) {
        self.haptic = haptic
        self.scaleDown = scaleDown
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scaleDown : 1.0)
            .animation(.spring(response: 0.22, dampingFraction: 0.6), value: configuration.isPressed)
            .onChange(of: configuration.isPressed) { isPressed in
                if isPressed {
                    switch haptic {
                    case .click: HapticManager.shared.click()
                    case .doubleTap: HapticManager.shared.doubleTap()
                    case .toggleOn: HapticManager.shared.toggleOn()
                    case .toggleOff: HapticManager.shared.toggleOff()
                    case .selection: HapticManager.shared.selection()
                    case .heavy: HapticManager.shared.heavy()
                    case .none: break
                    }
                }
            }
    }
}
