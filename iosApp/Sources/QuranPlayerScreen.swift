//
//  QuranPlayerScreen.swift
//  QuranBlind
//
//  100% 1:1 Parity with Android QuranPlayerScreen.kt (v1.1.0)
//  Horizontal Ayah Card Paging with Exact UI/Icon Parity
//

import SwiftUI
import SharedKit

// Drives audio playback via SharedKit AudioEngine and observeAudioEvents (via AudioBridgeIosKt)

/// Full-screen immersive Quran player mirroring Android production release v1.1.0.
/// Enforces RTL layout direction, strict monochrome theming, horizontal ayah paging, and VoiceOver invariants.
public struct QuranPlayerScreen: View {
    @Environment(\.quranTheme) private var theme
    @EnvironmentObject private var themeManager: ThemeManager

    @EnvironmentObject private var player: SurahPlayer
    @Environment(\.accessibilityVoiceOverEnabled) private var isVoiceOverRunning: Bool

    // Surah & Ayah state
    @State private var currentSurahId: Int32 = 1
    @State private var currentSurah: Surah?
    @State private var ayahs: [Ayah] = []
    @State private var currentIndex: Int = 0
    @State private var currentReciterId: String = Reciter.companion.DEFAULT_RECITER.serverIdentifier

    // Two-way synchronization & scroll tracking
    @State private var isProgrammaticScroll: Bool = false

    // Presentation sheets
    @State private var isSurahIndexPresented: Bool = false
    @State private var isReciterSelectorPresented: Bool = false
    @State private var isScreenOff: Bool = false

    // Paging swipe threshold constant for RTL gestures
    private let swipeThreshold: CGFloat = 50

    // Tap gesture disambiguation handled via AyahCardView TapGesture(count: 2) & TapGesture()
    // Audio Equalizer visual progress bars rendered via AudioEqualizerBars (Equalizer)

    private let repository: QuranRepository = KoinIosKt.iosQuranRepository()

    public init() {}

    /// VoiceOver stealth layout active. DEBUG builds accept `-simulateVoiceOver` so the silent
    /// surface can be audited in Accessibility Inspector on Simulator (VoiceOver can't run there).
    private var isStealthLayoutActive: Bool {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("-simulateVoiceOver") { return true }
        #endif
        return isVoiceOverRunning
    }

    private var displayAyahs: [Ayah] {
        if !player.currentAyahs.isEmpty {
            return player.currentAyahs
        }
        return ayahs
    }

    public var body: some View {
        ZStack {
            theme.background.ignoresSafeArea()

            VStack(spacing: 0) {
                // 1. Top HeaderBar (RTL Layout: Continuous Play, Surah Sheet, Reciter Sheet)
                PlayerHeaderBar(
                    isContinuousPlayEnabled: player.isContinuousPlayEnabled,
                    onToggleContinuousPlay: {
                        player.toggleContinuousPlay()
                    },
                    onOpenSurahIndex: {
                        isSurahIndexPresented = true
                    },
                    onOpenReciters: {
                        isReciterSelectorPresented = true
                    }
                )
                .padding(.horizontal, 16)
                .padding(.top, 8)

                // 2. Surah & Ayah Info Bar (Pill Cards below Header: 12pt cornerRadius, 1.5pt border, 24/12 padding)
                HStack(spacing: 16) {
                    SurahNameCard(name: currentSurah?.nameArabic ?? "الفاتحة") {
                        A11yAnnouncer.shared.announce("سورة \(currentSurah?.nameArabic ?? "الفاتحة")")
                    }

                    AyahNumberCard(number: currentAyahNumber) {
                        A11yAnnouncer.shared.announce("الآية \(currentAyahNumber)")
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 12)
                .padding(.bottom, 8)
                // Two separate focus stops (Android Row of two clickable pills): these are the
                // ONLY places on screen that speak the surah name / ayah number.
                .accessibilityElement(children: .contain)

                Spacer()

                // 3. Center Recitation Area (Horizontal Paging TabView of AyahCardView)
                if displayAyahs.isEmpty {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: theme.accent))
                        .scaleEffect(1.4)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    TabView(selection: $player.currentAyahIndex) {
                        ForEach(Array(displayAyahs.enumerated()), id: \.offset) { index, ayah in
                            AyahCardView(
                                ayah: ayah,
                                surahName: player.currentSurah?.nameArabic ?? currentSurah?.nameArabic,
                                isCurrent: index == player.currentAyahIndex,
                                isPlaying: player.isPlaying,
                                onTap: {
                                    togglePlayback(withHaptics: false)
                                },
                                onDoubleTap: {
                                    replayCurrentAyah(withHaptics: false)
                                },
                                onNext: {
                                    playNextAyah()
                                },
                                onPrevious: {
                                    playPreviousAyah()
                                }
                            )
                            .tag(index)
                            .padding(.horizontal, 16)
                            // Must be applied per page: the paged TabView hosts pages in UIKit
                            // cells, so a TabView-level accessibilityHidden does not reach them.
                            .accessibilityHidden(isStealthLayoutActive)
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                    .environment(\.layoutDirection, .rightToLeft)
                    .id(player.currentSurah?.id ?? currentSurahId)
                    // Stealth Box (Android QuranPlayerScreen.kt:296-302): with VoiceOver on, the pager
                    // and its cards (hidden per page above) are replaced by ONE silent element.
                    .accessibilityHidden(isStealthLayoutActive)
                    .overlay {
                        if isStealthLayoutActive {
                            SilentRecitationSurface(
                                onActivate: { togglePlayback(withHaptics: true, announce: false) },
                                onNext: { onScrollRight() },
                                onPrevious: { onScrollLeft() },
                                onMagicTap: { togglePlayback(withHaptics: true, announce: false) }
                            )
                        }
                    }
                }

                Spacer()

                // 4. Bottom Controls Area (Repeat Hint + Physical Bottom-Left 52dp Theme Toggle Button)
                bottomControlsArea
                    .padding(.horizontal, 16)
                    .padding(.bottom, 12)
            }
            .accessibilityHidden(isScreenOff)

            // 5. Full-Screen OLED Screen-Off Battery Saver Overlay
            if isScreenOff {
                ScreenOffSaverOverlay(onWakeUp: {
                    isScreenOff = false
                })
            }
        }
        .environment(\.layoutDirection, .rightToLeft)
        .accessibilityAction(.magicTap) {
            togglePlayback(withHaptics: true)
        }
        .accessibilityAction(.escape) {
            handleEscape()
        }
        // No screen-level named actions: SwiftUI copies them onto every child element, which made
        // VoiceOver append "Actions available" to every button. Replay stays on the bottom button.
        .onReceive(NotificationCenter.default.publisher(for: .magicTapTriggered)) { _ in
            togglePlayback(withHaptics: true)
        }
        .onReceive(NotificationCenter.default.publisher(for: .escapeTriggered)) { _ in
            handleEscape()
        }
        // Two-way synchronization: detect user-initiated swipe vs audio auto-advance
        .onChange(of: player.currentAyahIndex) { newIndex in
            guard newIndex >= 0 && newIndex < displayAyahs.count else { return }
            if isProgrammaticScroll || player.isProgrammaticScroll {
                // Audio engine auto-advance or programmatic transition
                // Silent by design (Android parity): the ayah number is spoken ONLY when the
                // user taps the top AyahNumberCard pill — never on page change.
                currentIndex = newIndex
            } else {
                // User-initiated manual swipe
                if newIndex != currentIndex {
                    HapticManager.shared.selection()
                    currentIndex = newIndex
                    player.goToAyah(newIndex, autoPlay: true)
                }
            }
        }
        .onChange(of: player.currentAyah?.numberInSurah) { newAyahNumber in
            guard let newAyahNumber = newAyahNumber else { return }
            if let newIndex = displayAyahs.firstIndex(where: { $0.numberInSurah == newAyahNumber }) {
                if newIndex != player.currentAyahIndex {
                    isProgrammaticScroll = true
                    withAnimation(.easeInOut(duration: 0.35)) {
                        player.currentAyahIndex = newIndex
                    }
                    currentIndex = newIndex
                    DispatchQueue.main.async {
                        isProgrammaticScroll = false
                    }
                }
            }
        }
        .onChange(of: player.currentSurah?.id) { newSurahId in
            guard let newSurahId = newSurahId, newSurahId != currentSurahId else { return }
            currentSurahId = newSurahId
            currentSurah = player.currentSurah
            ayahs = player.currentAyahs
            currentIndex = player.currentAyahIndex
        }
        .onChange(of: player.hasEnded) { ended in
            if ended {
                player.stop()
            }
        }
        .sheet(isPresented: $isSurahIndexPresented) {
            SurahIndexSheet(
                player: player,
                onSelectSurahAndAyah: { surahId, ayahNumber in
                    isSurahIndexPresented = false
                    onSelectSurahAndAyah(surahId: surahId, ayahNumber: ayahNumber)
                },
                onDismiss: {
                    isSurahIndexPresented = false
                }
            )
        }
        .sheet(isPresented: $isReciterSelectorPresented) {
            ReciterSelectorSheet(
                currentReciterId: currentReciterId,
                onSelectReciter: { reciter in
                    isReciterSelectorPresented = false
                    onSelectReciter(reciter: reciter)
                },
                onDismiss: {
                    isReciterSelectorPresented = false
                }
            )
        }
        .onAppear {
            loadInitialSurah()
            let args = ProcessInfo.processInfo.arguments
            if args.contains("-showSurahSheet") {
                isSurahIndexPresented = true
            } else if args.contains("-showAyahSheet") {
                isSurahIndexPresented = true
            } else if args.contains("-showReciterSheet") {
                isReciterSelectorPresented = true
            } else if args.contains("-showScreenOff") {
                isScreenOff = true
            }
        }
        .onDisappear {
            player.stop()
        }
    }

    // MARK: - Bottom Controls Area

    private var bottomControlsArea: some View {
        HStack(alignment: .center) {
            // Visual balance spacer (matching 52pt width of button on right side in RTL)
            Color.clear
                .frame(width: 52, height: 52)

            Spacer()

            // Center: Semibold repeat hint "انقر للتكرار"
            Button(action: {
                HapticManager.shared.doubleTap()
                replayCurrentAyah(withHaptics: false)
            }) {
                Text("انقر للتكرار")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(theme.textSecondary)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
            }
            .buttonStyle(AccessibleHapticButtonStyle(haptic: .none))
            .accessibilityLabel("انقر للتكرار")
            .accessibilityRemoveTraits(.isButton)

            Spacer()

            // Physical Bottom-Left: 52dp circular Day/Night toggle button
            // In RTL layout direction, trailing element is on the physical left of the screen
            // Displays sun.max.fill in dark mode and moon.stars.fill in light mode
            BlindAccessibleIconButton(
                systemName: themeManager.isDark ? "sun.max.fill" : "moon.stars.fill",
                accessibilityLabel: themeManager.isDark ? "التبديل إلى الوضع النهاري" : "التبديل إلى الوضع الليلي"
            ) {
                themeManager.toggleTheme()
            }
        }
    }

    // MARK: - State & Navigation Helpers

    private var currentAyah: Ayah? {
        if let playerAyah = player.currentAyah {
            return playerAyah
        }
        guard currentIndex >= 0 && currentIndex < displayAyahs.count else { return nil }
        return displayAyahs[currentIndex]
    }

    private var currentAyahNumber: Int32 {
        currentAyah?.numberInSurah ?? 1
    }

    private var currentAyahCount: Int32 {
        currentSurah?.ayahCount ?? Int32(displayAyahs.count)
    }

    /// - Parameter announce: false for the silent recitation surface (zero speech on the card).
    public func togglePlayback(withHaptics: Bool = false, announce: Bool = true) {
        if withHaptics {
            HapticManager.shared.click()
        }
        // `isPlaying` is published asynchronously, so announce the state we are switching TO,
        // derived from the state before the toggle (reading it after announced the stale state).
        let wasPlaying = player.isPlaying
        player.togglePause()
        if announce {
            A11yAnnouncer.shared.announcePlaybackState(isPlaying: !wasPlaying)
        }
    }

    public func replayCurrentAyah(withHaptics: Bool = false) {
        if withHaptics {
            HapticManager.shared.doubleTap()
        }
        let targetAyahs = player.currentAyahs.isEmpty ? ayahs : player.currentAyahs
        if !targetAyahs.isEmpty {
            player.play(
                targetAyahs,
                surah: currentSurah,
                from: currentIndex,
                reciterName: player.currentReciterName,
                reciterId: currentReciterId
            )
        }
    }

    public func playNextAyah() {
        // When isContinuousPlayEnabled is true, advancing beyond the last ayah advances surahId + 1
        player.skipToNextAyah()
    }

    public func playPreviousAyah() {
        if currentIndex > 0 {
            player.skipToPreviousAyah()
        } else {
            player.skipToPreviousAyah()
        }
    }

    // RTL Swipe & Navigation Support (edge == .leading corresponds to right drag / next Ayah)
    public func onScrollRight() {
        HapticManager.shared.selection()
        playNextAyah()
    }

    public func onScrollLeft() {
        HapticManager.shared.selection()
        playPreviousAyah()
    }

    private func handleEscape() {
        if isScreenOff {
            isScreenOff = false
            HapticManager.shared.doubleTap()
            A11yAnnouncer.shared.announceScreenOff(enabled: false)
        } else if isSurahIndexPresented {
            isSurahIndexPresented = false
        } else if isReciterSelectorPresented {
            isReciterSelectorPresented = false
        }
    }

    public func onSelectSurahAndAyah(surahId: Int32, ayahNumber: Int32) {
        loadSurah(id: surahId, startAyahNumber: ayahNumber)
    }

    public func onSelectReciter(reciter: Reciter) {
        currentReciterId = reciter.serverIdentifier
        player.currentReciterId = reciter.serverIdentifier
        player.currentReciterName = reciter.nameArabic
        A11yAnnouncer.shared.announceReciterChange(name: reciter.nameArabic)
        if !ayahs.isEmpty {
            loadSurah(id: currentSurahId, startAyahNumber: currentAyahNumber)
        }
    }

    private func loadInitialSurah() {
        loadSurah(id: currentSurahId, startAyahNumber: 1)
    }

    private func loadSurah(id: Int32, startAyahNumber: Int32) {
        currentSurahId = id
        let all = repository.getAllSurahs()
        currentSurah = all.first { $0.id == id }

        Task { @MainActor in
            do {
                let loaded = try await QuranBridgeIosKt.loadAyahsOnce(
                    repository: repository,
                    surahId: id,
                    audioBaseUrl: currentReciterId
                )
                self.ayahs = loaded
                let targetIndex = max(0, min(Int(startAyahNumber - 1), loaded.count - 1))
                self.currentIndex = targetIndex
                if !loaded.isEmpty {
                    self.player.play(
                        loaded,
                        surah: self.currentSurah,
                        from: targetIndex,
                        reciterName: self.player.currentReciterName,
                        reciterId: self.currentReciterId
                    )
                    if let surah = self.currentSurah {
                        A11yAnnouncer.shared.announceSurahChange(name: surah.nameArabic, ayahCount: surah.ayahCount)
                    }
                }
            } catch {
                print("Failed to load ayahs: \(error)")
            }
        }
    }
}
