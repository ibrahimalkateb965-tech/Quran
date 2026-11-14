//
//  SurahIndexSheet.swift
//  QuranBlind
//
//  Milestone 3: Accessibility Engine & VoiceOver Infrastructure
//  Feature 9: RTL Layout Direction & Feature 11: Surah Index Modal Sheet
//
//  Full rewrite: auto-scroll, live playback tracking via SurahPlayer,
//  active Surah/Ayah highlighting, and complete VoiceOver semantics.
//

import SwiftUI
import SharedKit

/// 2-level modal Surah Index Sheet enforcing RTL layout direction:
/// - Level 1: List of 114 Surahs with auto-scroll to active Surah and "مفتوحة الآن" badge.
/// - Level 2: List of Ayahs with auto-scroll to active Ayah and "محددة حالياً" VoiceOver label.
/// - Minimum 64pt row touch target height for blind accessibility.
public struct SurahIndexSheet: View {
    @Environment(\.quranTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var player: SurahPlayer
    let onSelectSurahAndAyah: (Int32, Int32) -> Void
    let onDismiss: () -> Void

    @State private var surahs: [Surah] = []
    @State private var selectedSurah: Surah? = nil
    private let repository: QuranRepository = KoinIosKt.iosQuranRepository()

    init(
        player: SurahPlayer,
        onSelectSurahAndAyah: @escaping (Int32, Int32) -> Void = { _, _ in },
        onDismiss: @escaping () -> Void = {}
    ) {
        self.player = player
        self.onSelectSurahAndAyah = onSelectSurahAndAyah
        self.onDismiss = onDismiss
    }

    // MARK: - Body

    public var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                if let surah = selectedSurah {
                    ayahListView(for: surah)
                } else {
                    surahListView
                }
            }
            .navigationTitle(
                selectedSurah != nil
                    ? "سورة \(selectedSurah!.nameArabic) ( اختيار الآية )"
                    : "اختيار السورة (114 سورة)"
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    if selectedSurah != nil {
                        Button("السور") {
                            backToSurahs()
                        }
                    } else {
                        Button("إغلاق") {
                            dismissSheet()
                        }
                    }
                }
            }
        }
        .environment(\.layoutDirection, .rightToLeft)
        .accessibilityAddTraits(.isModal)
        .onAppear {
            loadSurahs()
        }
        .accessibilityAction(.escape) {
            if selectedSurah != nil {
                backToSurahs()
            } else {
                dismissSheet()
            }
        }
    }

    // MARK: - Navigation helpers

    private func backToSurahs() {
        selectedSurah = nil
        A11yAnnouncer.shared.announce("تم العودة لقائمة السور")
    }

    private func dismissSheet() {
        onDismiss()
        dismiss()
    }

    // MARK: - Data loading

    private func loadSurahs() {
        let all = repository.getAllSurahs()
        if !all.isEmpty {
            surahs = all
        }
        if ProcessInfo.processInfo.arguments.contains("-showAyahSheet") {
            if let rad = all.first(where: { $0.id == 13 }) ?? all.first {
                selectedSurah = rad
            }
        }
    }

    // MARK: - Stage A: Surah List

    private var surahListView: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(surahs.isEmpty ? repository.getAllSurahs() : surahs, id: \.id) { surah in
                        let isActive = surah.id == player.activeSurahId

                        Button(action: {
                            selectedSurah = surah
                        }) {
                            HStack(alignment: .center, spacing: 12) {
                                // Number badge
                                Text("\(surah.id)")
                                    .font(.headline)
                                    .foregroundColor(theme.textOnFill)
                                    .frame(width: 44, height: 44)
                                    .background(
                                        Circle()
                                            .fill(isActive ? theme.accentContainer : theme.cardBorder)
                                    )

                                // Title & subtitle
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(surah.nameArabic)
                                        .font(.title3)
                                        .fontWeight(.bold)
                                        .foregroundColor(theme.textPrimary)

                                    Text("\(surah.revelationType) • \(surah.ayahCount) آية")
                                        .font(.subheadline)
                                        .foregroundColor(theme.textSecondary)
                                }

                                Spacer()

                                // Active badge
                                if isActive {
                                    Text("مفتوحة الآن")
                                        .font(.caption)
                                        .fontWeight(.bold)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(theme.accent)
                                        .foregroundColor(theme.textOnFill)
                                        .cornerRadius(6)
                                }
                            }
                            .padding(.horizontal, 16)
                            .frame(minHeight: 64)
                            .contentShape(Rectangle())
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(isActive ? theme.cardActive : Color.clear)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(isActive ? theme.cardActiveBorder : Color.clear, lineWidth: isActive ? 1.5 : 0)
                            )
                        }
                        .buttonStyle(.plain)
                        .id(surah.id)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel("سورة \(surah.nameArabic)، رقمها \(surah.id)، آياتها \(surah.ayahCount). \(isActive ? "مفتوحة الآن. " : "")انقر مرتين لاختيار الآية.")
                        .if(isActive) { view in
                            view.accessibilityAddTraits(.isSelected)
                        }

                        Divider()
                    }
                }
            }
            .onAppear {
                if let activeId = player.activeSurahId {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        withAnimation {
                            proxy.scrollTo(activeId, anchor: .center)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Stage B: Ayah List

    private func ayahListView(for surah: Surah) -> some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 0) {
                    ForEach(1...Int(surah.ayahCount), id: \.self) { ayahNumber in
                        let index = ayahNumber - 1
                        let isCurrent = surah.id == player.activeSurahId && index == player.activeAyahIndex

                        Button(action: {
                            onSelectSurahAndAyah(surah.id, Int32(ayahNumber))
                            dismissSheet()
                        }) {
                            HStack(alignment: .center) {
                                Text("الآية \(ayahNumber)")
                                    .font(.headline)
                                    .foregroundColor(theme.textPrimary)

                                Spacer()
                            }
                            .padding(.horizontal, 16)
                            .frame(minHeight: 64)
                            .contentShape(Rectangle())
                            .background(
                                RoundedRectangle(cornerRadius: 8)
                                    .fill(isCurrent ? theme.cardActive : Color.clear)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke(isCurrent ? theme.cardActiveBorder : Color.clear, lineWidth: isCurrent ? 1.5 : 0)
                            )
                        }
                        .buttonStyle(.plain)
                        .id(index)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel("الآية \(ayahNumber). \(isCurrent ? "محددة حالياً. " : "")انقر مرتين للتشغيل من هذه الآية.")
                        .if(isCurrent) { view in
                            view.accessibilityAddTraits(.isSelected)
                        }

                        Divider()
                    }
                }
            }
            .onAppear {
                A11yAnnouncer.shared.announce("اختيار الآية")
                if surah.id == player.activeSurahId, let ayahIdx = player.activeAyahIndex {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                        withAnimation {
                            proxy.scrollTo(ayahIdx, anchor: .center)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Conditional modifier helper

private extension View {
    @ViewBuilder
    func `if`<Content: View>(_ condition: Bool, transform: (Self) -> Content) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}
