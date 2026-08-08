# خطة رقم 66: إصلاح عدم تشغيل الصوت تلقائياً عند السحب في HorizontalPager

## المشكلة

عند السحب الأفقي (Swipe) للانتقال للآية التالية/السابقة في شاشة `QuranPlayerScreen` عبر `HorizontalPager`، تتغير الصفحة بصرياً ولكن الصوت لا يبدأ تلقائياً للآية الجديدة.

## التحليل الجذري

1. **سباق أوامر ExoPlayer**: `playCurrentAyah()` تستدعي `controller.stop()` قبل `setMediaItem(...)` و `prepare()` و `play()`. الـ `stop()` ينقل اللاعب إلى `STATE_IDLE` ويفقد AudioFocus مؤقتاً، مما قد يؤدي إلى تجاهل أمر `play()` اللاحق أثناء الانتقال السريع.

2. **تداخل TTS مع AudioFocus**: `performAction(...)` تُطلق TTS فوراً بعد `playCurrentAyah()`. TTS يأخذ AudioFocus وقد يمنع ExoPlayer من بدء الآية الجديدة فعلياً.

3. **مشكلة في `snapshotFlow`**: استخدام `pagerState.isScrollInProgress` كشرط للاستماع قد يفوت بعض انبعاثات `currentPage` إذا تغيّرت بعد انتهاء الإيماءة.

4. **تعارض الإيماءة الأفقية الخارجية**: كان هناك `detectHorizontalDragGestures` إضافي على الـ `Box` الخارجي يتعارض مع `HorizontalPager` ويُسبب استدعاءات مزدوجة.

## الملفات المطلوب تعديلها

1. `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt`
2. `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt`

---

## 1. تعديلات `QuranViewModel.kt`

### 1.1 إضافة متغير للإعلان المعلق

```kotlin
private var pendingAudioUrlToPlay: String? = null
private var pendingAyahAnnouncement: String? = null
```

### 1.2 تعديل `playAudioUrl()` لإزالة `stop()`

```kotlin
private fun playAudioUrl(url: String) {
    mediaController?.let { controller ->
        controller.setMediaItem(MediaItem.fromUri(url), 0L)
        controller.prepare()
        controller.play()
    } ?: run {
        pendingAudioUrlToPlay = url
    }
}
```

> لم يعد `stop()` ضرورياً؛ `setMediaItem(..., 0L)` يستبدل المقطع الحالي ويبدأ من البداية.

### 1.3 تعديل `goToAyah()` لتأجيل الإعلان بعد بدء التشغيل

```kotlin
fun goToAyah(index: Int, autoPlay: Boolean = true) {
    val state = _playbackUiState.value
    if (index !in state.currentAyahs.indices || index == state.currentAyahIndex) return

    _playbackUiState.update { it.copy(currentAyahIndex = index, currentLoopCount = 1) }
    val ayah = state.currentAyahs[index]
    haptic.vibrateClick()

    if (autoPlay) {
        pendingAyahAnnouncement = "الآية ${ayah.numberInSurah}"
        playCurrentAyah()
    } else {
        announce("الآية ${ayah.numberInSurah}")
    }
}
```

### 1.4 إعلان رقم الآية بعد أن يبدأ الصوت فعلياً

في `onIsPlayingChanged()` داخل `Player.Listener`:

```kotlin
override fun onIsPlayingChanged(isPlaying: Boolean) {
    _playbackUiState.update { it.copy(isPlaying = isPlaying) }
    if (isPlaying) {
        pendingAyahAnnouncement?.let { msg ->
            pendingAyahAnnouncement = null
            viewModelScope.launch { delay(400); announce(msg) }
        }
        if (isAwaitingNetworkRecovery) { ... }
    }
}
```

### 1.5 تنظيف الإعلان المعلق عند حدوث خطأ

```kotlin
override fun onPlayerError(error: PlaybackException) {
    pendingAyahAnnouncement = null
    if (isNetworkRelatedError(error)) {
        handleNetworkPlaybackError()
    } else {
        announce("حدث خطأ في تشغيل الصوت")
    }
}
```

### 1.6 التعامل مع `mediaController` غير الجاهز

في `init` داخل `controllerFuture.addListener { ... }`:

```kotlin
pendingAudioUrlToPlay?.let { url ->
    pendingAudioUrlToPlay = null
    playAudioUrl(url)
}
```

---

## 2. تعديلات `QuranPlayerScreen.kt`

### 2.1 إزالة الإيماءة الأفقية الخارجية المتعارضة

احذف بلوك `.pointerInput(Unit) { detectHorizontalDragGestures(...) }` من الـ `Box` الخارجي. `HorizontalPager` يكفي للتنقل بالسحب.

### 2.2 تحسين تزامن `snapshotFlow`

أضف علامة `isProgrammaticScroll` لتجنب الحلقة بين التمرير البرمجي والمستخدم:

```kotlin
val pagerState = rememberPagerState(
    initialPage = playbackUiState.currentAyahIndex,
    pageCount = { ayahs.size }
)
var isProgrammaticScroll by remember { mutableStateOf(false) }

// ViewModel -> Pager
LaunchedEffect(playbackUiState.currentAyahIndex) {
    val target = playbackUiState.currentAyahIndex
    if (target != pagerState.currentPage && target in ayahs.indices && !pagerState.isScrollInProgress) {
        isProgrammaticScroll = true
        try {
            pagerState.animateScrollToPage(target)
        } finally {
            isProgrammaticScroll = false
        }
    }
}

// Pager -> ViewModel
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
        .distinctUntilChanged()
        .collect { page ->
            if (isProgrammaticScroll) return@collect
            val currentIndex = viewModel.playbackUiState.value.currentAyahIndex
            if (page != currentIndex && page in ayahs.indices) {
                viewModel.goToAyah(page, autoPlay = true)
            }
        }
}
```

---

## السلوك المتوقع بعد التطبيق

1. المستخدم يشغل الآية الحالية.
2. عند السحب للآية التالية/السابقة:
   - تتغير الصفحة بصرياً.
   - يبدأ تشغيل الآية الجديدة **فوراً**.
   - يحدث اهتزاز تأكيد.
   - بعد ~400 ملي ثانية من بدء التشغيل، يُعلن رقم الآية الجديدة دون مقاطعة بداية التلاوة.

---

## قائمة التحقق

- [ ] `./gradlew :app:compileDebugKotlin` يمر بنجاح.
- [ ] `./gradlew :app:testDebugUnitTest` يمر بنجاح.
- [ ] السحب للآية التالية أثناء التشغيل يبدأ الصوت فوراً.
- [ ] السحب للآية التالية أثناء الإيقاف المؤقت يبدأ الصوت فوراً.
- [ ] الانتقال عبر الأزرار "التالي/السابق" يعمل كالمعتاد.
- [ ] الأوامر الصوتية للانتقال لآية محددة تعمل كالمعتاد.
- [ ] عند انتهاء الآية في وضع التشغيل المتواصل، تنتقل للآية التالية وتعمل تلقائياً.

---

## إضافة: إصلاح القارئ عبد الرشيد صوفي

### المشكلة

القارئ **عبد الرشيد صوفي** لا يعمل حالياً في التطبيق. عند اختياره، لا يُشغّل الصوت.

### التحليل الجذري

1. `Reciter.kt` يستخدم `serverIdentifier = "ar.abdulrashidsufi"`.
2. `api.alquran.cloud/v1/surah/{id}/ar.abdulrashidsufi` يُرجع **نص الآيات فقط** بدون حقل `audio`؛ لذلك لا يحصل التطبيق على رابط صوتي.
3. عند عدم وجود `audio`، يُنشئ `generateFallbackAyahs()` روابط من `cdn.islamic.network/quran/audio/128/ar.abdulrashidsufi/{globalNumber}.mp3`، لكن هذا القارئ غير موجود على الـ CDN (يُرجع HTTP 403).
4. المصادر المتاحة لعبد الرشيد صوفي (مثل `mp3quran.net`) توفر **سورة كاملة فقط**، وليس **آية آية**.
5. مصادر per-ayah معروفة (EveryAyah، Quran.com) لا تحتوي على تلاوة عبد الرشيد صوفي.

### الخيارات المقترحة

#### الخيار 1: استبدال القارئ المعطّل بقارئ يعمل (السريع والموصى به)

استبدل `Reciter("sufi", ...)` في `Reciter.kt` بقارئ مدعوم per-ayah، مثل **ماهر المعيقلي**:

```kotlin
Reciter(
    "maher",
    "الشيخ ماهر المعيقلي",
    "Maher Al-Muaiqly",
    "ar.mahermuaiqly"
)
```

- `VoiceCommandParser` يدعم بالفعل "ماهر" و "المعيقلي".
- يعمل فوراً عبر `api.alquran.cloud` و `cdn.islamic.network`.
- يبقى الاعتراف الصوتي "صوت صوفي" ممكنًا إذا أردت إضافة مرادف يوجه للقارئ الجديد.

#### الخيار 2: إبقاء اسم صوفي مع مصدر سورة كاملة (متوسط)

في `QuranRepository.kt`، أضف حالة خاصة لـ Sufi عند توليد الروابط:

```kotlin
val finalAudioUrl = when (reciterIdentifier) {
    "ar.husary" -> { ... }
    "ar.abdulrashidsufi" -> {
        val formattedSurah = surahId.toString().padStart(3, '0')
        "https://server16.mp3quran.net/soufi/Rewayat-Hafs-A-n-Assem/$formattedSurah.mp3"
    }
    audioUrl.isNotBlank() -> audioUrl
    else -> "https://cdn.islamic.network/quran/audio/128/$reciterIdentifier/$globalNumber.mp3"
}
```

ثم في `QuranViewModel.playCurrentAyah()`، تجنب إعادة تحميل المقطع إذا كانت URL الآية الجديدة مطابقة للمقطع الحالي (كل آيات نفس السورة تشترك الرابط):

```kotlin
private fun playCurrentAyah() {
    ...
    val currentMediaItem = mediaController?.currentMediaItem
    val newMediaItem = MediaItem.fromUri(activeAyah.audioUrl)

    if (currentMediaItem?.localConfiguration?.uri.toString() == activeAyah.audioUrl) {
        // نفس ملف السورة مستمر؛ لا نعيد التحميل
        return
    }

    playAudioUrl(activeAyah.audioUrl)
}
```

- **الإيجابية**: يبقى اسم القارئ "صوفي".
- **السلبية**: الصوت سورة كاملة؛ عند الانتقال بين الآيات لن يبدأ الصوت من بداية كل آية، بل يستمر المقطع.

#### الخيار 3: دعم تشغيل السور الكاملة مع أزمنة الآيات (طويل)

- تحميل جدول أزمنة كل آية للقارئ (مطلوب مصدر موثوق للأزمنة).
- عند الانتقال لآية جديدة داخل نفس السورة، استخدم `mediaController?.seekTo(timestampMs)` بدلاً من `setMediaItem`.
- يتطلب تحديث نموذج `Ayah` أو `PlaybackUiState` لتخزين `audioStartTimeMs`.

### التوصية

**الخيار 1** هو الأفضل للإصلاح الفوري؛ يضمن تجربة صوتية كاملة per-ayah. إذا أصررت على الاحتفاظ بصوت عبد الرشيد صوفي فعلياً، فالخيار 2 يعمل كحل وسط لكنه يغير طبيعة التنقل بين الآيات.

### الملفات المعنية

- `app/src/main/java/com/example/data/model/Reciter.kt` (للخيار 1)
- `app/src/main/java/com/example/data/repository/QuranRepository.kt` (للخيار 2)
- `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt` (للخيار 2 أو 3)

### قائمة التحقق لإصلاح القارئ

- [ ] اختيار القارئ الجديد (ماهر المعيقلي) يشغل الصوت فوراً.
- [ ] الأوامر الصوتية "صوت ماهر" / "المعيقلي" تعمل.
- [ ] الانتقال بين الآيات يعمل بسلاسة.
- [ ] إذا تم تطبيق الخيار 2، يجب اختبار عدم إعادة تحميل ملف السورة عند تغيير الآية.

