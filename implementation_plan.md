# خطة إصلاح مشكلة عدم تشغيل الصوت تلقائياً عند السحب في HorizontalPager

## ملخص المشكلة

عند السحب الأفقي في شاشة `QuranPlayerScreen` على `HorizontalPager`، تتغير الصفحة بصرياً إلى الآية التالية، لكن الصوت لا يبدأ تلقائياً.

الأسباب المحتملة:

1. `goToAyah(...)` في `QuranViewModel` تقرأ لقطة قديمة من الـ State (`val state = _uiState.value`) قبل التحديث، ثم تستدعي `playCurrentAyah()`.
2. `playCurrentAyah()` تستدعي `mediaController?.stop()` قبل `setMediaItem(...)`، وهذا قد يؤدي إلى فقدان حالة التشغيل أو تداخل مع AudioFocus أثناء الانتقال السريع بين الآيات.
3. لا يوجد تعامل مع حالة `mediaController == null` أو غير جاهز عند استدعاء `playCurrentAyah()`.
4. التزامن بين `snapshotFlow { pagerState.currentPage }` و `LaunchedEffect(uiState.currentAyahIndex)` قد يسبب استدعاءات مكررة أو تُignored بسبب التحقق `index != state.currentAyahIndex`.
5. الإعلان الصوتي/الاهتزازي يحدث **قبل** بدء التشغيل، مما قد يتداخل مع AudioFocus ويمنح انطباعاً بأن الصوت لم يعمل.

## الملفات التي تحتاج تعديل

- `app/src/main/java/com/example/ui/viewmodel/QuranViewModel.kt`
- `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt`
- `app/src/main/java/com/example/service/QuranAudioService.kt` (فقط للتأكد من إعدادات AudioFocus)

---

## 1. تعديلات `QuranViewModel.kt`

### 1.1 إضافة خاصية لتخزين آية في انتظار التشغيل

إذا كان `mediaController` غير جاهز بعد، نخزّن رابط الصوت المطلوب تشغيله ونعيد المحاولة فور اتصال المتحكم.

```kotlin
class QuranViewModel(application: Application) : AndroidViewModel(application) {
    ...
    private var pendingAudioUrlToPlay: String? = null
    ...
}
```

### 1.2 تحديث `init` لتشغيل الصوت المعلق فور جاهزية المتحكم

في داخل `controllerFuture.addListener { ... }`، بعد تخزين `mediaController` وإضافة الـ Listener، أضف:

```kotlin
controllerFuture?.addListener({
    if (isControllerReleased) return@addListener

    mediaController = controllerFuture?.get()

    playerListener = object : Player.Listener { ... }
    mediaController?.addListener(playerListener!!)

    // تشغيل الصوت المعلق إذا وجد
    pendingAudioUrlToPlay?.let { url ->
        pendingAudioUrlToPlay = null
        playAudioUrl(url)
    }
}, ContextCompat.getMainExecutor(application))
```

### 1.3 تعديل `goToAyah(...)`

```kotlin
fun goToAyah(index: Int, autoPlay: Boolean = true) {
    val state = playbackUiState.value // أو uiState.value حسب التنفيذ الحالي

    if (index !in state.currentAyahs.indices) return
    if (index == state.currentAyahIndex) return

    // تحديث الحالة أولاً
    _playbackUiState.update {
        it.copy(currentAyahIndex = index, currentLoopCount = 1)
    }

    // بدء التشغيل فوراً إذا كان مطلوباً
    if (autoPlay) {
        playCurrentAyah()
    }

    // الإعلان والاهتزاز بعد بدء التشغيل (وليس قبله)
    val ayah = playbackUiState.value.currentAyahs[index]
    performAction("الآية ${ayah.numberInSurah}", HapticType.CLICK)
}
```

**ملاحظة UX:** بما أن المستخدم مكفوف ويستخدم السحب للانتقال، فالسلوك الأفضل هو `autoPlay = true` دائماً عند السحب.

### 1.4 تعديل `playCurrentAyah()`

```kotlin
private fun playCurrentAyah() {
    val state = playbackUiState.value // أو uiState.value
    val index = state.currentAyahIndex
    val ayahs = state.currentAyahs

    if (index !in ayahs.indices) return

    val activeAyah = ayahs[index]
    if (activeAyah.audioUrl.isBlank()) return

    viewModelScope.launch {
        val bookmarked = repository.isBookmarked(activeAyah.surahId, activeAyah.numberInSurah)
        _bookmarkUiState.update { it.copy(isCurrentAyahBookmarked = bookmarked) }
    }

    val controller = mediaController
    if (controller != null) {
        controller.stop()
        controller.setMediaItem(MediaItem.fromUri(activeAyah.audioUrl), /* startPositionMs= */ 0L)
        controller.prepare()
        controller.play()
    } else {
        // اجعله يلعب تلقائياً عندما يصبح المتحكم جاهزاً
        pendingAudioUrlToPlay = activeAyah.audioUrl
    }
}
```

**شرح التعديل:**
- نستخدم `controller.stop()` **ثم** `setMediaItem(..., 0L)` لضمان بدء الآية الجديدة من البداية.
- نستدعي `prepare()` ثم `play()` بشكل صريح.
- إذا كان `mediaController` غير متصل بعد، نخزّن الرابط ونلعبه لاحقاً.

### 1.5 إضافة دالة مساعدة `playAudioUrl(url: String)`

```kotlin
private fun playAudioUrl(url: String) {
    mediaController?.let { controller ->
        controller.stop()
        controller.setMediaItem(MediaItem.fromUri(url), 0L)
        controller.prepare()
        controller.play()
    } ?: run {
        pendingAudioUrlToPlay = url
    }
}
```

### 1.6 تعديل `navigateAyah(...)` للحفاظ على التوافق

```kotlin
fun navigateAyah(offset: Int) {
    val state = playbackUiState.value // أو uiState.value
    val newIndex = state.currentAyahIndex + offset

    if (newIndex in state.currentAyahs.indices) {
        goToAyah(newIndex, autoPlay = true)
    } else if (newIndex >= state.currentAyahs.size) {
        val nextSurahId = (state.currentSurah?.id ?: 1) + 1
        if (nextSurahId <= 114) {
            loadSurah(nextSurahId, autoPlay = true)
        }
    } else if (newIndex < 0) {
        val prevSurahId = (state.currentSurah?.id ?: 1) - 1
        if (prevSurahId >= 1) {
            loadSurah(prevSurahId, autoPlay = true)
        }
    }
}
```

### 1.7 تحديث `loadSurah(...)`

```kotlin
fun loadSurah(surahId: Int, targetAyahIndex: Int = 0, autoPlay: Boolean = true) {
    val surah = repository.getSurahById(surahId) ?: return

    viewModelScope.launch {
        _playbackUiState.update {
            it.copy(
                currentSurah = surah,
                currentAyahIndex = targetAyahIndex,
                isLoadingAudio = true,
                currentLoopCount = 1
            )
        }

        val ayahs = repository.fetchAyahsForSurah(surahId, _settingsUiState.value.selectedReciter.serverIdentifier)
        val initialBookmarked = if (ayahs.isNotEmpty()) {
            repository.isBookmarked(surahId, ayahs[targetAyahIndex.coerceIn(0, ayahs.lastIndex)].numberInSurah)
        } else false

        _playbackUiState.update {
            it.copy(
                currentAyahs = ayahs,
                isLoadingAudio = false,
            )
        }
        _bookmarkUiState.update { it.copy(isCurrentAyahBookmarked = initialBookmarked) }

        announce("${surah.translationArabic}. عدد آياتها ${surah.ayahCount}.")

        if (autoPlay && ayahs.isNotEmpty()) {
            playCurrentAyah()
        }
    }
}
```

### 1.8 التعامل مع `onAyahPlaybackEnded()`

```kotlin
private fun onAyahPlaybackEnded() {
    val state = playbackUiState.value // أو uiState.value
    val repeatMode = state.tarkizRepeatMode
    val currentLoop = state.currentLoopCount

    if (repeatMode > 1 && (repeatMode == 99 || currentLoop < repeatMode)) {
        _playbackUiState.update { it.copy(currentLoopCount = currentLoop + 1) }
        playCurrentAyah()
        return
    }

    _playbackUiState.update { it.copy(currentLoopCount = 1) }

    if (state.currentAyahIndex < state.currentAyahs.lastIndex) {
        if (state.isContinuousPlayEnabled) {
            goToAyah(state.currentAyahIndex + 1, autoPlay = true)
        }
    } else {
        haptic.vibrateRepeatOn()
        val nextSurahId = (state.currentSurah?.id ?: 1) + 1
        if (nextSurahId <= 114) {
            announce("انتهت السورة. الانتقال للسورة التالية.")
            loadSurah(nextSurahId, autoPlay = state.isContinuousPlayEnabled)
        } else {
            announce("تم ختام القرآن الكريم.")
        }
    }
}
```

### 1.9 تحديث `handleVoiceCommandResult` للأمر `GoToAyahNumber`

```kotlin
is VoiceCommandResult.GoToAyahNumber -> {
    haptic.vibrateVoiceCommandSuccess()
    val ayahs = playbackUiState.value.currentAyahs
    val targetIndex = (result.ayahNumber - 1).coerceIn(0, (ayahs.size - 1).coerceAtLeast(0))
    if (ayahs.isNotEmpty()) {
        goToAyah(targetIndex, autoPlay = true)
    }
}
```

---

## 2. تعديلات `QuranPlayerScreen.kt`

### 2.1 تحسين التزامن بين Pager و ViewModel

استخدم `distinctUntilChanged()` وتأكد من عدم استدعاء `goToAyah` أثناء تمرير برمجي.

```kotlin
val pagerState = rememberPagerState(
    initialPage = playback.currentAyahIndex,
    pageCount = { playback.currentAyahs.size }
)

// Sync ViewModel -> Pager
LaunchedEffect(playback.currentAyahIndex) {
    if (pagerState.currentPage != playback.currentAyahIndex &&
        playback.currentAyahIndex in playback.currentAyahs.indices
    ) {
        pagerState.animateScrollToPage(playback.currentAyahIndex)
    }
}

// Sync Pager -> ViewModel
LaunchedEffect(pagerState) {
    snapshotFlow { pagerState.currentPage }
        .distinctUntilChanged()
        .collect { page ->
            if (pagerState.isScrollInProgress) {
                val currentIndex = viewModel.playbackUiState.value.currentAyahIndex
                if (page != currentIndex && page in playback.currentAyahs.indices) {
                    viewModel.goToAyah(page, autoPlay = true)
                }
            }
        }
}
```

**ملاحظة:** التحقق `pagerState.isScrollInProgress` يضمن أن التغيير جاء من تفاعل المستخدم (السحب)، وليس من الـ `animateScrollToPage` البرمجي.

### 2.2 تحسين `HorizontalPager` باستخدام `derivedStateOf`

```kotlin
val currentAyahIndex by remember { derivedStateOf { playback.currentAyahIndex } }
val isPlaying by remember { derivedStateOf { playback.isPlaying } }

HorizontalPager(
    state = pagerState,
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
) { page ->
    val ayah = playback.currentAyahs.getOrNull(page)
    if (ayah != null) {
        val isCurrentPage by remember(page) {
            derivedStateOf { page == currentAyahIndex }
        }
        val isPagePlaying by remember(page) {
            derivedStateOf { isCurrentPage && isPlaying }
        }

        AyahCard(
            ayah = ayah,
            isCurrent = isCurrentPage,
            isPlaying = isPagePlaying,
            onClick = { },
            onDoubleTap = { viewModel.togglePlayback() },
            onSingleTap = { },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        ...
    }
}
```

### 2.3 إزالة الاعتماد على `uiState.value.currentAyahIndex` القديم

في كل أماكن الشاشة، استخدم `playback.currentAyahIndex` بدلاً من `uiState.currentAyahIndex`.

---

## 3. تعديلات `QuranAudioService.kt` (اختياري ولكن مُستحسن)

تأكد من أن الخدمة تحافظ على AudioFocus عند الانتقال السريع بين الآيات.

في `onCreate`:

```kotlin
val audioAttributes = AudioAttributes.Builder()
    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
    .setUsage(C.USAGE_MEDIA)
    .build()

player = ExoPlayer.Builder(this)
    .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
    .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
    .setHandleAudioBecomingNoisy(true)
    .setWakeMode(C.WAKE_MODE_NETWORK)
    .build()
```

**تأكد من أن `handleAudioFocus` = `true` (موجود حالياً).**

إذا كان التطبيق يستخدم TTS داخلياً أثناء التشغيل، فمن المستحسن جعل TTS يستخدم `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` لتجنب إيقاف ExoPlayer بالكامل.

---

## 4. ملاحظات UX للمكفوفين

1. **السحب للانتقال = نية تشغيل:** عندما يسحب المستخدم إلى آية جديدة، يجب أن تبدأ الآية بالعمل فوراً (`autoPlay = true`).
2. **التغذية الراجعة:** الاهتزاز والإعلان الصوتي يجب أن يحدثا **بعد** بدء التشغيل، أو على الأقل لا يتسببان بتأخيره.
3. **الحالة المتوقفة:** إذا كان المستخدم قد أوقف التشغيل مؤقتاً ثم سحب، من الأفضل لأجل UX للمكفوفين أن يبدأ الصوت بالعمل، لأن السحب إيماءة واعية للانتقال.

---

## 5. قائمة التحقق بعد التنفيذ

بعد تنفيذ التعديلات، شغّل:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

ثم اختبر يدوياً:

1. افتح السورة وشغّل الآية.
2. اسحب يميناً/يساراً للانتقال إلى آية أخرى.
3. تأكد أن الصوت يبدأ فوراً في الآية الجديدة.
4. أوقف التشغيل مؤقتاً، ثم اسحب — يجب أن تبدأ الآية الجديدة بالعمل.
5. اختبر الأوامر الصوتية "الآية التالية" و "الآية السابقة" و "اذهب للآية X".
6. اختبر الانتقال البرمجي عند انتهاء الآية في وضع التشغيل المتواصل.
