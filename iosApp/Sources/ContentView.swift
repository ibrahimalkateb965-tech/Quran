import SwiftUI
import SharedKit

final class BookmarksViewModel: ObservableObject {
    @Published private(set) var bookmarks: [Bookmark] = []

    private let store: BookmarkStore = KoinIosKt.iosBookmarkStore()
    private var subscription: BookmarkSubscription?

    init() {
        subscription = BookmarkBridgeIosKt.observeBookmarks(store: store) { [weak self] items in
            self?.bookmarks = items
        }
    }

    deinit {
        subscription?.cancel()
    }

    func isBookmarked(surahId: Int32, ayahNumber: Int32) -> Bool {
        store.isBookmarked(surahId: surahId, ayahNumber: ayahNumber)
    }

    @discardableResult
    func toggle(surahId: Int32, surahNameAr: String, ayahNumber: Int32) -> Bool {
        store.toggle(surahId: surahId, surahNameAr: surahNameAr, ayahNumber: ayahNumber)
    }
}

// First SwiftUI surface over the shared repository: the 114-surah index,
// bookmark storage and a read-only ayah list. Every row carries an explicit VoiceOver label;
// visual styling is deliberately minimal (blind-first, CLAUDE.md section 4.1).
struct ContentView: View {
    private let repository: QuranRepository = KoinIosKt.iosQuranRepository()
    @StateObject private var bookmarksViewModel = BookmarksViewModel()

    var body: some View {
        TabView {
            NavigationView {
                List(repository.getAllSurahs(), id: \.id) { surah in
                    NavigationLink(destination: SurahView(surah: surah, repository: repository, bookmarksViewModel: bookmarksViewModel)) {
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
            .tabItem {
                Label("السور", systemImage: "book")
            }

            BookmarksView(bookmarksViewModel: bookmarksViewModel, repository: repository)
                .tabItem {
                    Label("العلامات", systemImage: "bookmark")
                }
        }
        .environment(\.layoutDirection, .rightToLeft)
    }
}

struct BookmarksView: View {
    @ObservedObject var bookmarksViewModel: BookmarksViewModel
    let repository: QuranRepository

    var body: some View {
        NavigationView {
            Group {
                if bookmarksViewModel.bookmarks.isEmpty {
                    VStack(spacing: 12) {
                        Image(systemName: "bookmark")
                            .font(.largeTitle)
                            .foregroundColor(.secondary)
                        Text("لا توجد علامات مرجعية محفوظة")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("لا توجد علامات مرجعية محفوظة حالياً")
                } else {
                    List(bookmarksViewModel.bookmarks, id: \.createdAtEpochMillis) { bookmark in
                        if let surah = repository.getSurahById(id: bookmark.surahId) {
                            NavigationLink(destination: SurahView(surah: surah, repository: repository, bookmarksViewModel: bookmarksViewModel)) {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("سورة \(bookmark.surahNameAr)")
                                            .font(.headline)
                                        Text("الآية \(bookmark.ayahNumber)")
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                    }
                                    Spacer()
                                }
                            }
                            .accessibilityElement(children: .ignore)
                            .accessibilityLabel("علامة مرجعية: سورة \(bookmark.surahNameAr)، الآية \(bookmark.ayahNumber)")
                            .accessibilityHint("افتح السورة عند هذه الآية")
                        }
                    }
                }
            }
            .navigationTitle("العلامات المرجعية")
        }
    }
}

struct SurahView: View {
    let surah: Surah
    let repository: QuranRepository
    @ObservedObject var bookmarksViewModel: BookmarksViewModel

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
                List(Array(ayahs.enumerated()), id: \.element.globalNumber) { index, ayah in
                    let isCurrent = player.currentAyah?.globalNumber == ayah.globalNumber
                    let isSaved = bookmarksViewModel.isBookmarked(surahId: surah.id, ayahNumber: ayah.numberInSurah)
                    HStack {
                        Button {
                            player.play(ayahs, from: index)
                        } label: {
                            Text(ayah.textArabic)
                                .font(.title3)
                                .fontWeight(isCurrent ? .bold : .regular)
                                .foregroundColor(.primary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("الآية \(ayah.numberInSurah): \(ayah.textArabic)")
                        .accessibilityValue(isCurrent ? "قيد التلاوة" : "")
                        .accessibilityHint("شغّل التلاوة من هذه الآية")

                        Spacer()

                        Button {
                            bookmarksViewModel.toggle(surahId: surah.id, surahNameAr: surah.nameArabic, ayahNumber: ayah.numberInSurah)
                        } label: {
                            Image(systemName: isSaved ? "bookmark.fill" : "bookmark")
                                .foregroundColor(isSaved ? .accentColor : .secondary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(isSaved ? "إزالة العلامة المرجعية للآية \(ayah.numberInSurah)" : "حفظ العلامة المرجعية للآية \(ayah.numberInSurah)")
                        .accessibilityHint("تبديل حفظ الآية في العلامات المرجعية")
                    }
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
