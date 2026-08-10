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

---

# خطة رقم 67: توحيد التنقل بين الآيات عبر HorizontalPager القياسي مع TalkBack

**الحالة:** خطة جاهزة للتنفيذ — لم يُعدَّل أي كود بعد.
**المنفِّذ:** وكيل التطوير.
**النطاق المحصور:** `QuranPlayerScreen.kt` فقط + ملف اختبار جديد. لا تعديل على `AyahCard.kt` ولا `QuranViewModel` ولا أي ملف آخر.

## 1. الهدف

جعل التنقل بين الآيات يعمل مع TalkBack عبر `HorizontalPager` القياسي الموحّد لجميع المستخدمين (مكفوفين ومبصرين)، مع:
- حذف المكوّن المخصص `SilentAccessiblePager` **بالكامل** (لا إعادة تسمية — حذف).
- تفعيل `userScrollEnabled = true` صراحةً.
- تأمين الفوكس عبر `LaunchedEffect(pagerState.settledPage)` بآلية خالية من سباق Recomposition ومن `IllegalStateException`.
- تصميم صامت تماماً: يبقى `contentDescription = ","` كما هو. **ممنوع** إضافة `stateDescription` أو `customActions` أو أي نصوص منطوقة جديدة.

## 2. القرارات المُلزمة (من صاحب المنتج)

| # | القرار |
|---|--------|
| D1 | `HorizontalPager` القياسي هو المكوّن الوحيد للجميع. لا عقد مستقرة وهمية ولا Anchors. |
| D2 | صمت تام: لا `stateDescription`، لا `customActions`، لا تعديل على صمت `AyahCard`. |
| D3 | حذف `SilentAccessiblePager` نهائياً من الكود. |
| D4 | تأمين الفوكس عبر `LaunchedEffect(pagerState.settledPage)`. |

## 3. ملخص سبب الفشل الحالي (للفهم فقط — لا حاجة لإعادة التحقيق)

1. السحب الأفقي بإصبع واحد في TalkBack = تنقّل خطّي بين العقد، ولا يُرسل `scrollBy` أبداً — لذا كان كود `scrollBy` على الـ `Row` ميتاً لهذه الإيماءة.
2. الـ Anchors (عقد 10.dp) تستدعي `goToAyah()` فور وصول الفوكس → تدمير العقدة الحاملة للفوكس أثناء الإيماءة → TalkBack يفقد موضعه ويعود لأعلى الشاشة → يظهر خارجياً كأن "السحب توقف".
3. `FocusRequester.requestFocus()` في الكود القديم كان يسبق اكتمال Recomposition → `IllegalStateException` (انهيارات الفوكس القديمة).
4. **لماذا ينجح الحل الجديد:** `HorizontalPager` يعرض `horizontalScrollAxisRange` + `scrollBy` **بشكل مدمج** على عقدته. عندما يكون الفوكس على `AyahCard` داخله ويمرّر المستخدم بإصبعين (أو إيماءة يمين-ثم-يسار)، يفوّض TalkBack إجراء التمرير لأقرب سلف Scrollable — وهو الـ Pager نفسه → يقلب صفحة واحدة → `settledPage` يتغير → مزامنة الـ ViewModel. الاتجاه RTL يُشتق تلقائياً من `LocalLayoutDirection.Rtl` المفروض أصلاً في الشاشة (~سطر 163).

## 4. التعديلات التفصيلية — `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt`

### الخطوة 4.1 — حذف المكوّن المخصص
- حذف `SilentAccessiblePager` كاملاً (~السطور 438–546).
- حذف التفرع:
  ```kotlin
  if (isTalkBackEnabled) { SilentAccessiblePager(...) } else { ... }
  ```
  واستبداله بمسار واحد موحّد (الخطوة 4.2).

### الخطوة 4.2 — مسار Pager موحّد للجميع
الإبقاء على `pagerState` وتأثيرَي المزامنة الموجودَين (~السطور 277–300) **كما هما مع حارسيهما** (`!pagerState.isScrollInProgress` و `page != currentIndex` و `distinctUntilChanged`) — هذه الحراسات تمنع حلقة التغذية المرتدة بين المزامنتين، ولا يجوز إزالتها.

البنية المستهدفة:

```kotlin
Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = true, // صراحةً وفق القرار D1 (وهو الافتراضي أصلاً)
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val ayah = ayahs.getOrNull(page)
        if (ayah != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                AyahCard(
                    ayah = ayah,
                    isCurrentProvider = { page == currentAyahIndex },
                    isPlayingProvider = { page == currentAyahIndex && isPlaying },
                    isScreenOffModeProvider = { screenModeUiState.isScreenOffMode },
                    onClick = { viewModel.togglePlayback() },
                    onDoubleTap = { viewModel.replayCurrentAyah() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        // ↓↓ تأمين الفوكس — يُضاف في الخطوة 4.3 ↓↓
                        .then(focusSecuringModifier(page))
                )
            }
        }
    }
}
```

### الخطوة 4.3 — تأمين الفوكس (التنفيذ الإلزامي للقرار D4)

**المشكلة التي يجب تفاديها:** `FocusRequester.requestFocus()` قبل ارتباط الـ modifier بعقدة مركَّبة = `IllegalStateException`. لذلك الآلية: علم `focusPending` + طلب الفوكس من داخل `onGloballyPositioned` (ضمان أن العقدة مركَّبة والـ focusRequester مرفق بها). لا `delay()` ولا `coroutineScope.launch` مكشوف.

```kotlin
// داخل QuranPlayerScreen، قبل الـ HorizontalPager:
val ayahFocusRequester = remember { FocusRequester() }
var ayahFocusPending by remember { mutableStateOf(false) }

// القرار D4: التأمين مربوط باستقرار الصفحة
LaunchedEffect(pagerState.settledPage, isTalkBackEnabled) {
    if (isTalkBackEnabled && ayahs.isNotEmpty()) {
        ayahFocusPending = true
    }
}
```

ودالة مساعدة خاصة داخل نفس الملف (تُمرَّر لها الحالات عبر معاملات أو إغلاق):

```kotlin
// تُرفق الـ FocusRequester ببطاقة الآية الحالية فقط، وتطلب الفوكس بعد اكتمال التركيب
private fun focusSecuringModifier(page: Int): Modifier {
    return if (page == currentAyahIndex) {
        Modifier
            .focusRequester(ayahFocusRequester)
            .onGloballyPositioned {
                if (ayahFocusPending) {
                    ayahFocusPending = false
                    ayahFocusRequester.requestFocus()
                }
            }
    } else Modifier
}
```

**قيود صارمة على المنفِّذ:**
- الـ `focusRequester` يُرفق **بصفحة واحدة فقط** (صفحة `currentAyahIndex`) — إرفاقه بأكثر من عقدة = `IllegalStateException`.
- طلب الفوكس يحدث **فقط** داخل `onGloballyPositioned` وبشرط العلم — هذا يلغي كامل فئة انهيارات السباق القديمة.
- التأمين يعمل فقط عند `isTalkBackEnabled == true` — تجربة المبصر لا تتغير إطلاقاً.
- **قرار متروك للمنفِّذ (موثّق):** `LaunchedEffect` يعمل عند أول تركيب أيضاً وقد يضبط العلم `true` (فوكس أولي على البطاقة). إن كان ذلك غير مرغوب، تُتخطّى القيمة الأولى عبر `snapshotFlow { pagerState.settledPage }.drop(1)` داخل `LaunchedEffect(Unit)`. يُختبر في البند اليدوي #1.

### الخطوة 4.4 — تنظيف الاستيرادات الميتة
بعد الحذف، تُزال الاستيرادات التي لم يعد لها استخدام (تحقق بالـ Build): `horizontalScrollAxisRange`, `scrollBy`, `ScrollAxisRange`, `onFocusChanged`, `focusable`, `rememberUpdatedState`, `clearAndSetSemantics` (إن لم يبقَ استخدام آخر لها في الملف), `rememberCoroutineScope` (إن لم يبقَ استخدام)، وأي استيراد مكرر (يوجد استيراد مكرر لـ `LocalTalkBackEnabled` حالياً).
**يبقى** `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, LocalTalkBackEnabled provides isTalkBackEnabled)` كما هو — ضروري لاتجاه الـ Pager ولمكوّنات أخرى.
**يُضاف** استيراد `androidx.compose.ui.layout.onGloballyPositioned` و `androidx.compose.ui.focus.focusRequester` (موجود مسبقاً).

### الخطوة 4.5 — ما لا يُمسّ
- `AyahCard.kt`: لا تعديل إطلاقاً. الصمت (`","`) يبقى كما هو (D2).
- `QuranViewModel`, `goToAyah`, `blindAccessibleClickable`, `LocalPendingBlindAction`: لا تعديل.
- نصوص التلميح الموجودة (Header/Hint chips): لا تعديل في هذه المهمة.

## 5. الاختبارات الآلية (تُكتب قبل أو مع التنفيذ)

ملف جديد: `app/src/androidTest/java/com/example/ui/screens/QuranPlayerPagerTalkBackTest.kt`.

ملاحظة للمنفِّذ: إن كان إنشاء `QuranViewModel` وهمياً مكلفاً، يُبنى **harness مصغّر** يكرّر نفس تسلكيب الـ Pager + المزامنة + تأمين الفوكس حرفياً، مع ViewModel مزيّف يسجّل الاستدعاءات:

1. **T1 — أفعال التمرير موجودة:** عقدة الـ Pager تملك `SemanticsActions.ScrollBy` (مدمجة من مكتبة foundation).
2. **T2 — مزامنة الاتجاهين:** تغيير `settledPage` يستدعي `goToAyah(page, autoPlay = true)` مرة واحدة فقط؛ وتغيير `currentAyahIndex` من الـ ViewModel يحرّك الـ Pager دون استدعاء `goToAyah` عكسياً (لا حلقة مرتدة).
3. **T3 — لا انهيار فوكس:** مع محاكاة TalkBack مفعّل، 10 تقليبات صفحات متتالية سريعة → لا استثناءات (تحديداً لا `IllegalStateException` من FocusRequester).
4. **T4 — ثبات الصمت:** كل بطاقات الآيات المكشوفة في شجرة Semantics تحمل `contentDescription == ","` ولا تحمل `stateDescription`.

أمر التشغيل: `./gradlew :app:compileDebugKotlin` ثم `./gradlew :app:connectedDebugAndroidTest` على جهاز/محاكٍ.

## 6. بروتوكول التحقق اليدوي على الجهاز (معيار القبول)

| # | السيناريو | النتيجة المطلوبة |
|---|-----------|------------------|
| 1 | فتح الشاشة مع TalkBack | لا فوكس عشوائي مسروق؛ لا انهيار (راجع القرار الموثّق في 4.3) |
| 2 | سحب بإصبعين يساراً | الانتقال للآية التالية + تشغيل الصوت وفق السلوك الحالي |
| 3 | سحب بإصبعين يميناً | العودة للآية السابقة |
| 4 | 10 تقليبات سريعة متتالية | صفر انهيارات في logcat (ابحث عن `IllegalStateException` و `FocusRequester`) |
| 5 | أول آية + سحب بإصبعين يميناً / آخر آية + يساراً | لا تمرير، لا انهيار |
| 6 | سحب بإصبع واحد يمين/يسار | تنقّل خطّي هادئ بين العناصر (أزرار التحكم...) بلا قفز ولا إعادة فوكس لأعلى الشاشة |
| 7 | تفعيل التقدّم التلقائي بالصوت (Continuous Play) مع TalkBack | الـ Pager يتبع `currentAyahIndex` بلا تعليق ولا حلقة |
| 8 | إيقاف TalkBack | السحب اللمسي العادي يعمل (`userScrollEnabled = true`) والنقر المزدوج للتشغيل سليم |

## 7. المخاطر الموثّقة (إلزامية القراءة للمنفِّذ)

| # | الخطر | التخفيف |
|---|-------|---------|
| R1 | طلب System Focus قد لا يسحب معه Accessibility Focus الخاص بـ TalkBack على كل إصدارات أندرويد/TalkBack؛ عندها يعيد TalkBack الاستقرار على موضع افتراضي بعد قلب الصفحة | آلية `onGloballyPositioned` تضمن عدم الانهيار على الأقل؛ يُقيَّم السلوك فعلياً في البند اليدوي #2/#4. إن كان غير مقبول منتجياً، يُرفع تقرير لصاحب المنتج — **ممنوع إضافة حل بديل (customActions/إعلانات/Anchors) دون موافقته** |
| R2 | السحب بإصبع واحد لا يقلب الصفحات (الصفحات المجاورة غير مُركَّبة افتراضياً) — سلوك متوقَّع ومقبول وفق D1؛ التقليب عبر إيماءات التمرير (إصبعان / يمين-ثم-يسار) | موثّق هنا فقط؛ ممنوع "إصلاحه" بإعادة Anchors أو تغيير `beyondViewportPageCount` دون موافقة |
| R3 | حلقة تغذية مرتدة بين مزامنتي الـ Pager والـ ViewModel | الحارسان الموجودان يبقيان؛ T2 يتحقق آلياً |
| R4 | `goToAyah` قد لا يكون آمناً للدخول المتكرر أثناء تقليبات سريعة مع تحميل صوت | يُتحقق في البند اليدوي #4 وT3؛ إن وُجد خلل في الـ ViewModel يُوثَّق ويُرفع — **لا يُعدَّل الـ ViewModel ضمن هذه المهمة** |

## 8. تعريف الإنجاز (Definition of Done)

- [ ] `SilentAccessiblePager` محذوف كلياً ولا يوجد أي مرجع له.
- [ ] مسار واحد: `HorizontalPager` بـ `userScrollEnabled = true` لجميع المستخدمين.
- [ ] تأمين الفوكس عبر `settledPage` + `onGloballyPositioned`، TalkBack فقط.
- [ ] صفر `stateDescription`/`customActions`/نصوص منطوقة جديدة.
- [ ] `compileDebugKotlin` نظيف + صفر استيرادات ميتة.
- [ ] T1–T4 خضراء.
- [ ] بنود التحقق اليدوي 1–8 موقّعة على جهاز حقيقي مع TalkBack.


