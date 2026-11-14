//
//  QuranPlayerScreen.swift
//  QuranBlind
//
//  100% 1:1 Parity with Android QuranPlayerScreen.kt (v1.1.0)
//

import SwiftUI
import SharedKit

// Drives audio playback via SharedKit AudioEngine and observeAudioEvents (via AudioBridgeIosKt)

/// Full-screen immersive Quran player mirroring Android production release v1.1.0.
/// Enforces RTL layout direction, strict monochrome theming, and VoiceOver invariants.
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

    // Interactive Drag Animation State
    @State private var dragOffset: CGFloat = 0

    // Presentation sheets
    @State private var isSurahIndexPresented: Bool = false
    @State private var isReciterSelectorPresented: Bool = false
    @State private var isScreenOff: Bool = false

    private let repository: QuranRepository = KoinIosKt.iosQuranRepository()

    public init() {}

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

                // 2. Surah & Ayah Info Bar (Pill Cards below Header)
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

                // 3. Center Recitation Area (Large Card with Stealth Box Invariant)
                recitationCard
                    .offset(x: dragOffset)
                    .scaleEffect(1.0 - min(abs(dragOffset) / 1200.0, 0.05))
                    .opacity(1.0 - Double(min(abs(dragOffset) / 800.0, 0.15)))
                    .padding(.horizontal, 16)
                    .stealthBox(
                        isPlaying: player.isPlaying,
                        surahName: currentSurah?.nameArabic ?? "الفاتحة",
                        ayahNumber: currentAyahNumber,
                        ayahCount: currentAyahCount,
                        onTogglePlay: { togglePlayback() },
                        onReplay: { replayCurrentAyah(withHaptics: false) },
                        onNextAyah: { playNextAyah() },
                        onPreviousAyah: { playPreviousAyah() }
                    )
                    .gesture(
                        DragGesture(minimumDistance: 20)
                            .onChanged { drag in
                                dragOffset = drag.translation.width
                            }
                            .onEnded { drag in
                                let threshold: CGFloat = 50
                                if abs(drag.translation.width) > threshold {
                                    withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                        if drag.translation.width > 0 {
                                            // In RTL, dragging rightwards advances to next Ayah
                                            playNextAyah()
                                        } else if drag.translation.width < 0 {
                                            // In RTL, dragging leftwards returns to previous Ayah
                                            playPreviousAyah()
                                        }
                                        dragOffset = 0
                                    }
                                } else {
                                    withAnimation(.spring(response: 0.35, dampingFraction: 0.82)) {
                                        dragOffset = 0
                                    }
                                }
                            }
                    )
                    .gesture(
                        TapGesture(count: 2)
                            .exclusively(before: TapGesture(count: 1))
                            .onEnded { value in
                                switch value {
                                case .first:
                                    replayCurrentAyah(withHaptics: true)
                                case .second:
                                    togglePlayback()
                                }
                            }
                    )

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
        .onChange(of: player.currentAyahIndex) { newIndex in
            guard newIndex >= 0 && newIndex < ayahs.count else { return }
            if newIndex != currentIndex {
                currentIndex = newIndex
                if let ayah = currentAyah {
                    A11yAnnouncer.shared.announce("الآية \(ayah.numberInSurah)")
                }
            }
        }
        .onChange(of: player.currentAyah?.numberInSurah) { newAyahNumber in
            guard let newAyahNumber = newAyahNumber else { return }
            if let newIndex = ayahs.firstIndex(where: { $0.numberInSurah == newAyahNumber }) {
                if newIndex != currentIndex {
                    currentIndex = newIndex
                    A11yAnnouncer.shared.announce("الآية \(newAyahNumber)")
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

    // MARK: - Central Recitation Card

    private var recitationCard: some View {
        VStack(spacing: 16) {
            Spacer()

            if let ayah = currentAyah {
                Text(ayah.textArabic)
                    .font(.custom("KFGQPCHAFSUthmanicScript-Regula", size: 30))
                    .multilineTextAlignment(.center)
                    .foregroundColor(theme.textPrimary)
                    .lineSpacing(16)
                    .padding(.horizontal, 20)
            } else {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: theme.accent))
                    .scaleEffect(1.4)
            }

            if player.isPlaying {
                AudioEqualizerBars(isPlaying: true)
                    .padding(.top, 12)
            }

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(.vertical, 24)
        .background(theme.cardSurface)
        .cornerRadius(20)
        .overlay(
            RoundedRectangle(cornerRadius: 20)
                .stroke(player.isPlaying ? theme.cardActiveBorder : theme.cardBorder, lineWidth: 1.5)
        )
        .contentShape(Rectangle())
    }

    // MARK: - Bottom Controls Area

    private var bottomControlsArea: some View {
        HStack(alignment: .center) {
            // Visual balance spacer (matching 52pt width of button on right side in RTL)
            Color.clear
                .frame(width: 52, height: 52)

            Spacer()

            // Center: Bold repeat hint "انقر للتكرار"
            Button(action: {
                replayCurrentAyah()
            }) {
                Text("انقر للتكرار")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(theme.textSecondary)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("انقر للتكرار")
            .accessibilityHint("انقر مرتين لإعادة تلاوة الآية الحالية")

            Spacer()

            // Physical Bottom-Left: 52dp circular Day/Night toggle button
            // In RTL layout direction, the trailing element is on the left side of the screen
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
        guard currentIndex >= 0 && currentIndex < ayahs.count else { return nil }
        return ayahs[currentIndex]
    }

    private var currentAyahNumber: Int32 {
        currentAyah?.numberInSurah ?? 1
    }

    private var currentAyahCount: Int32 {
        currentSurah?.ayahCount ?? Int32(ayahs.count)
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
        player.skipToNextAyah()
    }

    public func playPreviousAyah() {
        player.skipToPreviousAyah()
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
