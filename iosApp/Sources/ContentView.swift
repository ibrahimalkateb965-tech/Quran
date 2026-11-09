import SwiftUI
import SharedKit

// First SwiftUI surface over the shared repository: the 114-surah index and a
// read-only ayah list. Every row carries an explicit VoiceOver label; visual
// styling is deliberately minimal (blind-first, CLAUDE.md section 4.1).
struct ContentView: View {
    private let repository: QuranRepository = KoinIosKt.iosQuranRepository()

    var body: some View {
        NavigationView {
            List(repository.getAllSurahs(), id: \.id) { surah in
                NavigationLink(destination: SurahView(surah: surah, repository: repository)) {
                    HStack {
                        Text("\(surah.id).")
                        Text(surah.nameArabic)
                        Spacer()
                        Text("\(surah.ayahCount)")
                            .foregroundColor(.secondary)
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("سورة \(surah.nameArabic)، \(surah.ayahCount) آية")
                .accessibilityHint("افتح السورة")
            }
            .navigationTitle("السور")
        }
        .environment(\.layoutDirection, .rightToLeft)
    }
}

struct SurahView: View {
    let surah: Surah
    let repository: QuranRepository

    @StateObject private var player = SurahPlayer()
    @State private var ayahs: [Ayah] = []
    @State private var loadError: String?

    var body: some View {
        Group {
            if let loadError = loadError {
                Text(loadError)
                    .accessibilityLabel("تعذر تحميل السورة")
            } else if ayahs.isEmpty {
                ProgressView()
                    .accessibilityLabel("جارٍ التحميل")
            } else {
                // Every row is a "recite from here" button; the ayah being recited is
                // exposed to VoiceOver through its accessibilityValue, not colour alone.
                List(Array(ayahs.enumerated()), id: \.element.globalNumber) { index, ayah in
                    let isCurrent = player.currentAyah?.globalNumber == ayah.globalNumber
                    Button {
                        player.play(ayahs, from: index)
                    } label: {
                        Text(ayah.textArabic)
                            .font(.title3)
                            .fontWeight(isCurrent ? .bold : .regular)
                            .foregroundColor(.primary)
                    }
                    .accessibilityLabel("الآية \(ayah.numberInSurah): \(ayah.textArabic)")
                    .accessibilityValue(isCurrent ? "قيد التلاوة" : "")
                    .accessibilityHint("شغّل التلاوة من هذه الآية")
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            if player.currentAyah != nil || player.failure != nil {
                PlaybackBar(player: player)
            }
        }
        .onDisappear { player.stop() }
        .navigationTitle(surah.nameArabic)
        .task {
            do {
                ayahs = try await QuranBridgeIosKt.loadAyahsOnce(
                    repository: repository,
                    surahId: surah.id,
                    audioBaseUrl: Reciter.companion.DEFAULT_RECITER.serverIdentifier
                )
            } catch {
                loadError = error.localizedDescription
            }
        }
    }
}

// Bottom bar shown while a recitation is queued: what is being recited, pause/resume,
// stop, and any playback failure. Grouped so VoiceOver reads it as one region.
private struct PlaybackBar: View {
    @ObservedObject var player: SurahPlayer

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let failure = player.failure {
                Text(failure)
                    .foregroundColor(.red)
                    .accessibilityLabel("خطأ في التشغيل: \(failure)")
            }
            HStack {
                Text(statusText)
                    .accessibilityLabel(statusText)
                Spacer()
                Button(player.isPlaying ? "إيقاف مؤقت" : "تشغيل") {
                    player.togglePause()
                }
                .accessibilityHint(player.isPlaying ? "يوقف التلاوة مؤقتاً" : "يستأنف التلاوة")
                Button("إيقاف") {
                    player.stop()
                }
                .accessibilityHint("ينهي التلاوة")
            }
        }
        .padding()
        .background(.thinMaterial)
        .accessibilityElement(children: .contain)
    }

    private var statusText: String {
        if player.hasEnded {
            return "انتهت التلاوة"
        }
        if let ayah = player.currentAyah {
            return "يُتلى الآن: الآية \(ayah.numberInSurah)"
        }
        return ""
    }
}
