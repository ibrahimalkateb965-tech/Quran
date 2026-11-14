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
                .accessibilityElement(children: .combine)

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
                                isCurrent: index == player.currentAyahIndex,
                                isPlaying: player.isPlaying,
                                onTap: {
                                    togglePlayback()
                                },
                                onDoubleTap: {
                                    replayCurrentAyah(withHaptics: true)
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
                        }
                    }
                    .tabViewStyle(.page(indexDisplayMode: .never))
                    .environment(\.layoutDirection, .rightToLeft)
                    .id(player.currentSurah?.id ?? currentSurahId)
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
                    A11yAnnouncer.shared.announceScreenOff(enabled: false)
                })
            }
        }
        .environment(\.layoutDirection, .rightToLeft)
        .accessibilityAction(.magicTap) {
            togglePlayback()
        }
        .accessibilityAction(named: "تفعيل شاشة توفير الطاقة") {
            isScreenOff = true
            A11yAnnouncer.shared.announceScreenOff(enabled: true)
        }
        .accessibilityAction(named: "إعادة التلاوة") {
            replayCurrentAyah()
        }
        // Two-way synchronization: detect user-initiated swipe vs audio auto-advance
        .onChange(of: player.currentAyahIndex) { newIndex in
            guard newIndex >= 0 && newIndex < displayAyahs.count else { return }
            if isProgrammaticScroll || player.isProgrammaticScroll {
                // Audio engine auto-advance or programmatic transition
                currentIndex = newIndex
                if let ayah = currentAyah {
                    A11yAnnouncer.shared.announce("الآية \(ayah.numberInSurah)")
                }
            } else {
                // User-initiated manual swipe
                if newIndex != currentIndex {
                    currentIndex = newIndex
                    player.goToAyah(newIndex, autoPlay: true)
                    if let ayah = currentAyah {
                        A11yAnnouncer.shared.announce("الآية \(ayah.numberInSurah)")
                    }
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
                replayCurrentAyah()
            }) {
                Text("انقر للتكرار")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(theme.textSecondary)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("انقر للتكرار")
            .accessibilityHint("انقر مرتين لإعادة تلاوة الآية الحالية")

            Spacer()

            // Physical Bottom-Left: 52dp circular Day/Night toggle button
            // In RTL layout direction, trailing element is on the physical left of the screen
            // Displays sun.max.fill in dark mode and moon.stars.fill in light mode
            BlindAccessibleIconButton(
                systemName: themeManager.isDark ? "sun.max.fill" : "moon.stars.fill",
                accessibilityLabel: themeManager.isDark ? "التبديل إلى الوضع النهاري" : "التبديل إلى الوضع الليلي",
                accessibilityHint: "انقر للتبديل بين الوضعين النهاري والليلي"
            ) {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
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

    public func togglePlayback() {
        player.togglePause()
        A11yAnnouncer.shared.announcePlaybackState(isPlaying: player.isPlaying)
    }

    public func replayCurrentAyah(withHaptics: Bool = true) {
        if withHaptics {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
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
        playNextAyah()
    }

    public func onScrollLeft() {
        playPreviousAyah()
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
