//
//  ReciterSelectorSheet.swift
//  QuranBlind
//
//  1:1 Parity with Android ReciterSelectorSheet.kt
//

import SwiftUI
import SharedKit

public struct ReciterSelectorSheet: View {
    @Environment(\.quranTheme) private var theme
    @Environment(\.dismiss) private var dismiss
    @StateObject private var pager = SilentScrollPager()

    public static let defaultReciterId: String = "husary_mujawwad"
    public let currentReciterId: String
    public let onSelectReciter: (Reciter) -> Void
    public let onDismiss: () -> Void

    public init(
        currentReciterId: String = defaultReciterId,
        onSelectReciter: @escaping (Reciter) -> Void,
        onDismiss: @escaping () -> Void = {}
    ) {
        self.currentReciterId = currentReciterId.isEmpty ? Self.defaultReciterId : currentReciterId
        self.onSelectReciter = onSelectReciter
        self.onDismiss = onDismiss
    }

    public var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                reciterList
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                // Custom title: the system title would make VoiceOver append "heading".
                ToolbarItem(placement: .principal) {
                    Text("اختيار القارئ")
                        .font(.headline)
                        .accessibilityLabel(A11yLabels.chooseReciter)
                        .accessibilityRemoveTraits(.isHeader)
                        .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
                }
                ToolbarItem(placement: .cancellationAction) {
                    Button("إغلاق") {
                        dismissSheet()
                    }
                    .buttonStyle(AccessibleHapticButtonStyle(haptic: .none))
                    .accessibilityLabel(A11yLabels.close)
                    .accessibilityRemoveTraits(.isButton)
                    .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
                }
            }
        }
        .environment(\.layoutDirection, .rightToLeft)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape) {
            dismissSheet()
        }
    }

    /// Same structure as the Surah sheet, whose list VoiceOver scrolls correctly.
    /// - The ScrollView is NOT the NavigationView's direct root: as the direct root it was
    ///   exposed to VoiceOver without a scroll container, so vertical swipes (one, two or three
    ///   fingers) did nothing.
    /// - Eager VStack (only 20 rows): a LazyVStack left the last reciters out of the
    ///   accessibility tree, so swiping past the visible rows jumped straight to "Close".
    /// - Scrolls the current reciter into view on open (Android `scrollToItem(index)` parity).
    private var reciterList: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 8) {
                    ForEach(Reciter.companion.DEFAULT_RECITERS, id: \.id) { reciter in
                        let isSelected = (reciter.id == currentReciterId || reciter.serverIdentifier == currentReciterId)

                        Button(action: {
                            onSelectReciter(reciter)
                            A11yAnnouncer.shared.announceReciterChange(name: reciter.nameArabic)
                            dismissSheet()
                        }) {
                            HStack(alignment: .center, spacing: 16) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(reciter.nameArabic)
                                        .font(.headline)
                                        .fontWeight(.bold)
                                        .foregroundColor(theme.textPrimary)

                                    Text(reciter.nameEnglish)
                                        .font(.subheadline)
                                        .foregroundColor(theme.textSecondary)
                                }

                                Spacer()

                                if isSelected {
                                    Image(systemName: "checkmark.circle.fill")
                                        .font(.title2)
                                        .foregroundColor(theme.accent)
                                }
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 14)
                            .frame(minHeight: 64)
                            .contentShape(Rectangle())
                            .background(isSelected ? theme.cardActive : theme.cardSurface)
                            .cornerRadius(12)
                            .overlay(
                                RoundedRectangle(cornerRadius: 12)
                                    .stroke(isSelected ? theme.cardActiveBorder : theme.cardBorder, lineWidth: 1.5)
                            )
                        }
                        .buttonStyle(AccessibleHapticButtonStyle(haptic: .click, scaleDown: 0.98))
                        .padding(.horizontal, 16)
                        .id(reciter.id)
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(A11yLabels.reciterRow(arabicName: reciter.nameArabic, englishName: reciter.nameEnglish, isSelected: isSelected))
                        .accessibilityRemoveTraits([.isButton, .isSelected])
                        .accessibilitySpeechLanguage(A11yLabels.speechLanguage)
                        .silentPageScroll(pager)
                    }
                }
                .padding(.vertical, 12)
                .silentPagingContainer(pager)
            }
            .onAppear {
                guard let selected = Reciter.companion.DEFAULT_RECITERS.first(where: {
                    $0.id == currentReciterId || $0.serverIdentifier == currentReciterId
                }) else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                    withAnimation {
                        proxy.scrollTo(selected.id, anchor: .center)
                    }
                }
            }
        }
    }

    private func dismissSheet() {
        HapticManager.shared.selection()
        onDismiss()
        dismiss()
    }
}
