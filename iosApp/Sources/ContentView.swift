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
                List(ayahs, id: \.globalNumber) { ayah in
                    Text(ayah.textArabic)
                        .font(.title3)
                        .accessibilityLabel("الآية \(ayah.numberInSurah): \(ayah.textArabic)")
                }
            }
        }
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
