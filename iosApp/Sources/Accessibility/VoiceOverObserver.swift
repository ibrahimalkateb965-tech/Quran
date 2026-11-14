//
//  VoiceOverObserver.swift
//  QuranBlind
//
//  Dynamic VoiceOver Detection (Invariant I3).
//  Observes UIAccessibility.voiceOverStatusDidChangeNotification and provides
//  reactive state so the app and UI adapt dynamically when VoiceOver is turned ON or OFF
//  mid-session without requiring an app restart.
//

import Foundation
import UIKit
import SwiftUI

/// Observable object monitoring iOS VoiceOver state reactively (Invariant I3).
public final class VoiceOverObserver: ObservableObject {
    public static let shared = VoiceOverObserver()

    @Published public var isVoiceOverRunning: Bool

    private var statusObserver: NSObjectProtocol?

    public init() {
        self.isVoiceOverRunning = UIAccessibility.isVoiceOverRunning
        setupObserver()
    }

    deinit {
        if let statusObserver = statusObserver {
            NotificationCenter.default.removeObserver(statusObserver)
        }
    }

    private func setupObserver() {
        statusObserver = NotificationCenter.default.addObserver(
            forName: UIAccessibility.voiceOverStatusDidChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.isVoiceOverRunning = UIAccessibility.isVoiceOverRunning
        }
    }
}
