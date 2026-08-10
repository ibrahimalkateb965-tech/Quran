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

---

# خطة رقم 68: تطبيق معمارية Dual-Mode بصرامة — تكييف التلميحات وسد فجوات التكافؤ

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**الحالة:** خطة جاهزة للتنفيذ — لم يُعدَّل أي كود.
**المنفِّذ:** وكيل التطوير.
**المرجع الملزم:** `.agents/DUAL_MODE_ARCHITECTURE.md` (القاعدة الذهبية + طبقات التكيف الخمس + قاعدة التكافؤ + مصفوفة الاختبار ×2).
**الاعتمادية:** خطة 67 منفَّذة ومؤكَّدة (تم التحقق: لا وجود لـ `SilentAccessiblePager`، و`userScrollEnabled = true` و`ayahFocusPending` موجودان في `QuranPlayerScreen.kt`).

**Goal:** جعل كل نص تلميحي وكل عنصر مرئي في التطبيق متوافقاً مع قاعدة التكافؤ (لكل مرئي معادل مسموع) مع تصحيح نصوص الإيماءات التي تصف سلوك المبصر وتظهر خطأً لمستخدم TalkBack.

**Architecture:** تفريع سلوك فقط (Behavior Branching) عبر دوال نقية قابلة لاختبار الوحدة (`playerGestureHints`, `buildPlayerStatusDescription`) تُستهلك في مواضع الاستدعاء بناءً على `isTalkBackEnabled` الموجود أصلاً في نطاق الشاشة. لا تفريع Layouts، لا مكونات جديدة للعرض، لا تغيير في شجرة المكونات.

**Tech Stack:** Jetpack Compose (BOM 2025.01.00)، JUnit4 لاختبارات الوحدة.

## Global Constraints

- ممنوع تفريع التصميم (`if (isTalkBackEnabled) { LayoutA } else { LayoutB }`) — يُسمح فقط بتفريع النصوص/السلوك (دليل المعمارية، قاعدة 1).
- الصمت التام لمحتوى الآيات يبقى كما هو (خطة 67، القرار D2): `contentDescription = ","` لا يُمسّ.
- لا تعديل على `QuranViewModel` ولا `AyahCard.kt` (باستثناء لا شيء — `AyahCard` خارج النطاق كلياً).
- كل النصوص الجديدة بالعربية الفصحى المبسطة، وتصف إيماءات TalkBack **الحقيقية** (السحب بإصبعين للتنقل بين الآيات — السحب بإصبع واحد محجوز للتنقل الخطي بين العناصر).
- النطاق: `QuranPlayerScreen.kt` + ملف جديد `PlayerHints.kt` + `AudioEqualizerBars.kt` (سطر واحد) + ملفات اختبار جديدة.

---

## نتائج المسح التدقيقي (الطلب رقم 2 — موثَّق ومعتمد من المهندس المعماري)

تم فحص كامل شجرة `ui/` مقابل طبقات التكيف الخمس. النتائج:

| المكوّن | الطبقة المعنية | الحكم | الإجراء |
|---|---|---|---|
| `GestureHintChip` ×2 (QuranPlayerScreen ~سطر 360-361) | 3 (تغذية راجعة) | ❌ **فجوة**: "سحب أفقي" وصف خاطئ تحت TalkBack، والنصوص تُقرأ بصوت عالٍ وتشتّت | المهمة 1 + 2 |
| وصف الحالة الحي (`talkBackDescription`, ~سطر 204) | 3 | ❌ **فجوة**: "اسحب يميناً ويساراً للتنقل" غير صحيحة لمستخدم TalkBack | المهمة 3 |
| `AudioEqualizerBars` | 3 | ⚠️ زخرفي بحت؛ مخفي حالياً **بالصدفة** عبر `clearAndSetSemantics` الخاص بـ `AyahCard` — غير محميّ من إعادة الاستخدام المستقبلية | المهمة 4 (تحصين) |
| `ScreenOffSaverOverlay` | 5 | ✅ متوافق: وصف منطوق صحيح + `BackHandler` + اندماج Semantics | لا إجراء |
| `TrialExpiredScreen` | 2/3 | ✅ متوافق: التفريع في قناة الإعلان فقط (سلوك مسموح)، التصميم موحّد | لا إجراء |
| `HeaderBar` / `HeaderAccessibleButton` | 2/3 | ✅ متوافق: `contentDescription` عربي + نطق عند النقرة الواحدة | لا إجراء |
| `ListeningVoiceBanner` | 3 | ✅ متوافق: `liveRegion Assertive` + وصف منطوق | لا إجراء |
| `AyahNumberCard` + نص اسم السورة + شريط "القارئ/الوضع" | 3 | ✅ متوافق: نصوص قابلة للقراءة آلياً من TalkBack | لا إجراء |
| `ControlPanel` / `BigVoiceMicrophoneButton` | — | ℹ️ معرَّفة لكن **بلا مواضع استدعاء** حالياً (كود غير مستخدم)؛ إن أُعيد إدخالها فهي متوافقة كما هي | لا إجراء (توثيق فقط) |
| `GlobalBlindGestureModifier` / `blindAccessibleClickable` | 2 | ✅ متوافق: تفريع سلوك نموذجي عبر `LocalTalkBackEnabled` | لا إجراء |

**خلاصة المسح:** فجوتان حقيقيتان (تلميحات + وصف الحالة) وتحصين واحد (Equalizer). كل ما عداها متوافق.

---

### Task 1: دالة نصوص التلميحات النقية + اختبار وحدات فاشل

**Files:**
- Create: `app/src/main/java/com/example/ui/components/player/PlayerHints.kt`
- Test: `app/src/test/java/com/example/ui/components/player/PlayerHintsTest.kt`

**Interfaces:**
- Produces: `data class PlayerGestureHints(val playPauseHint: String, val navigationHint: String)` و `fun playerGestureHints(isTalkBackEnabled: Boolean): PlayerGestureHints` — تستهلكها المهمة 2 في `QuranPlayerScreen.kt`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.ui.components.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerHintsTest {

    @Test
    fun `talkback on - navigation hint describes two-finger swipe`() {
        val hints = playerGestureHints(isTalkBackEnabled = true)
        assertEquals("سحب بإصبعين: آية آية", hints.navigationHint)
        assertEquals("نقرتان: تشغيل/إيقاف", hints.playPauseHint)
    }

    @Test
    fun `talkback off - sighted hints unchanged`() {
        val hints = playerGestureHints(isTalkBackEnabled = false)
        assertEquals("سحب أفقي: آية آية", hints.navigationHint)
        assertEquals("نقرتين: تشغيل/إيقاف", hints.playPauseHint)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.components.player.PlayerHintsTest"`
Expected: FAIL — `unresolved reference: playerGestureHints`

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.example.ui.components.player

/**
 * نصوص تلميحات الإيماءات لشاشة المشغل — مكيّفة حسب وضع TalkBack.
 * تفريع سلوك (نصوص) وليس تفريع تصميم — وفق .agents/DUAL_MODE_ARCHITECTURE.md
 */
data class PlayerGestureHints(
    val playPauseHint: String,
    val navigationHint: String
)

fun playerGestureHints(isTalkBackEnabled: Boolean): PlayerGestureHints =
    if (isTalkBackEnabled) {
        PlayerGestureHints(
            playPauseHint = "نقرتان: تشغيل/إيقاف",
            navigationHint = "سحب بإصبعين: آية آية"
        )
    } else {
        PlayerGestureHints(
            playPauseHint = "نقرتين: تشغيل/إيقاف",
            navigationHint = "سحب أفقي: آية آية"
        )
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.components.player.PlayerHintsTest"`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/ui/components/player/PlayerHints.kt app/src/test/java/com/example/ui/components/player/PlayerHintsTest.kt
git commit -m "feat(a11y): add dual-mode gesture hint texts resolver"
```

---

### Task 2: تطبيق التلميحات المكيّفة في موضع الاستدعاء

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt` (~سطر 360-361)

**Interfaces:**
- Consumes: `playerGestureHints(isTalkBackEnabled)` من المهمة 1؛ `isTalkBackEnabled` موجود أصلاً في نطاق `QuranPlayerScreen` (~سطر 141) — لا حاجة لقراءة `LocalTalkBackEnabled` من جديد.

- [ ] **Step 1: Apply the call-site change**

قبل الـ `Row` الحاوي للتلميحات مباشرة (أو ضمنه)، استبدل:

```kotlin
GestureHintChip(label = "نقرتين: تشغيل/إيقاف")
GestureHintChip(label = "سحب أفقي: آية آية")
```

بـ:

```kotlin
val gestureHints = playerGestureHints(isTalkBackEnabled)
GestureHintChip(label = gestureHints.playPauseHint)
GestureHintChip(label = gestureHints.navigationHint)
```

وأضف الاستيراد: `import com.example.ui.components.player.playerGestureHints`

**ملاحظة إلزامية:** الرقائق تبقى **ظاهرة في الوضعين** (قاعدة المنفعة المتبادلة) — التكييف في النص فقط. ممنوع إخفاء الـ `Row` بشرط.

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt
git commit -m "feat(a11y): adapt player gesture hint chips for TalkBack users"
```

---

### Task 3: وصف الحالة الحي المكيّف (تصحيح تعليمات السحب المنطوقة)

**Files:**
- Modify: `app/src/main/java/com/example/ui/components/player/PlayerHints.kt`
- Modify: `app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt` (~سطر 204)
- Test: `app/src/test/java/com/example/ui/components/player/PlayerHintsTest.kt` (إضافة اختبارات)

**Interfaces:**
- Produces: `fun buildPlayerStatusDescription(surahName: String?, ayahNumber: Int, ayahCount: Int?, isPlaying: Boolean, isRepeatModeActive: Boolean, isTalkBackEnabled: Boolean): String` — تستهلكها `QuranPlayerScreen`.

- [ ] **Step 1: Add failing tests**

```kotlin
@Test
fun `status description - talkback on uses two-finger instruction`() {
    val desc = buildPlayerStatusDescription(
        surahName = "الفاتحة", ayahNumber = 3, ayahCount = 7,
        isPlaying = true, isRepeatModeActive = false, isTalkBackEnabled = true
    )
    assert(desc.contains("اسحب بإصبعين يميناً أو يساراً للتنقل بين الآيات"))
    assert(desc.contains("سورة الفاتحة، الآية 3 من أصل 7"))
    assert(desc.contains("جاري التشغيل"))
}

@Test
fun `status description - talkback off keeps original instruction`() {
    val desc = buildPlayerStatusDescription(
        surahName = null, ayahNumber = 1, ayahCount = null,
        isPlaying = false, isRepeatModeActive = true, isTalkBackEnabled = false
    )
    assert(desc.contains("اسحب يميناً ويساراً للتنقل"))
    assert(!desc.contains("بإصبعين"))
    assert(!desc.contains("سورة"))
    assert(desc.contains("متوقف مؤقتاً"))
    assert(desc.contains("وضع التكرار مفعّل"))
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.components.player.PlayerHintsTest"`
Expected: FAIL — `unresolved reference: buildPlayerStatusDescription`

- [ ] **Step 3: Implement in `PlayerHints.kt`**

```kotlin
fun buildPlayerStatusDescription(
    surahName: String?,
    ayahNumber: Int,
    ayahCount: Int?,
    isPlaying: Boolean,
    isRepeatModeActive: Boolean,
    isTalkBackEnabled: Boolean
): String = buildString {
    append("تطبيق القرآن الكريم للمكفوفين. ")
    if (surahName != null && ayahCount != null) {
        append("سورة $surahName، الآية $ayahNumber من أصل $ayahCount. ")
    }
    append(if (isPlaying) "جاري التشغيل. " else "متوقف مؤقتاً. ")
    if (isRepeatModeActive) append("وضع التكرار مفعّل. ")
    if (isTalkBackEnabled) {
        append("انقر مرتين للتشغيل أو الإيقاف. اسحب بإصبعين يميناً أو يساراً للتنقل بين الآيات.")
    } else {
        append("انقر مرتين للتشغيل أو الإيقاف. اسحب يميناً ويساراً للتنقل.")
    }
}
```

- [ ] **Step 4: Replace the inline buildString in `QuranPlayerScreen.kt`**

استبدل كتلة `val talkBackDescription = buildString { ... }` (~سطر 204-213) بـ:

```kotlin
val talkBackDescription = buildPlayerStatusDescription(
    surahName = activeSurah?.nameArabic,
    ayahNumber = currentAyah?.numberInSurah ?: 1,
    ayahCount = activeSurah?.ayahCount,
    isPlaying = isPlaying,
    isRepeatModeActive = settingsUiState.tarkizRepeatMode > 1,
    isTalkBackEnabled = isTalkBackEnabled
)
```

وأضف الاستيراد: `import com.example.ui.components.player.buildPlayerStatusDescription`

**تحقق سلوكي إلزامي:** الناتج في وضع `TalkBack OFF` يجب أن يطابق النص القديم حرفياً (حرفاً بحرف) — أي اختلاف يعني خطأ في الاستخراج.

- [ ] **Step 5: Run tests + compile**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.ui.components.player.PlayerHintsTest"` ثم `./gradlew :app:compileDebugKotlin`
Expected: 4 tests PASS + BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/ui/components/player/PlayerHints.kt app/src/main/java/com/example/ui/screens/QuranPlayerScreen.kt app/src/test/java/com/example/ui/components/player/PlayerHintsTest.kt
git commit -m "feat(a11y): correct spoken swipe instructions for TalkBack in status description"
```

---

### Task 4: تحصين `AudioEqualizerBars` (إخفاء صريح من شجرة الوصول)

**Files:**
- Modify: `app/src/main/java/com/example/ui/components/player/AudioEqualizerBars.kt:31`

**Interfaces:**
- لا واجهات جديدة. تغيير modifier داخلي فقط.

- [ ] **Step 1: Apply the one-line hardening**

العنصر زخرفي بحت (حركة أشرطة)، ومعادله المسموع ("جاري التشغيل") موجود عبر وصف الحالة والإعلانات الصوتية — لذا وفق قاعدة التكافؤ يُخفى صراحةً بدل الاعتماد على إخفاء عرضي من الأب:

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.Bottom,
    modifier = Modifier
        .height(24.dp)
        .clearAndSetSemantics { } // زخرفي بحت — لا قيمة معلوماتية لقارئ الشاشة
)
```

مع الاستيراد: `import androidx.compose.ui.semantics.clearAndSetSemantics`

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/ui/components/player/AudioEqualizerBars.kt
git commit -m "fix(a11y): explicitly hide decorative equalizer bars from accessibility tree"
```

---

### Task 5: مصفوفة التحقق اليدوية ×2 (إلزامية — قاعدة 3 في الدليل)

- [ ] **Step 1: TalkBack ON**
  - الرقاقتان تُقرآن: "نقرتان: تشغيل/إيقاف" و"سحب بإصبعين: آية آية".
  - وصف الحالة يقول "اسحب بإصبعين يميناً أو يساراً للتنقل بين الآيات".
  - السحب الفعلي بإصبعين يقلب الآية (تطابق النص مع السلوك).
  - أشرطة الـ Equalizer لا تظهر كعقدة قابلة للتركيز أثناء التنقل الخطي.

- [ ] **Step 2: TalkBack OFF**
  - الرقاقتان تظهران بالنصوص الأصلية: "نقرتين: تشغيل/إيقاف" و"سحب أفقي: آية آية".
  - المساعد الداخلي ينطق وصف الحالة بالصيغة القديمة ("اسحب يميناً ويساراً").
  - السحب اللمسي الأفقي بإصبع واحد يقلب الآية.
  - لا تغيّر بصري إطلاقاً في الواجهة (نفس الرقائق، نفس المواضع).

- [ ] **Step 3: Full test suite + commit**

Run: `./gradlew :app:testDebugUnitTest` و `./gradlew :app:compileDebugKotlin`
Expected: الكل أخضر، ثم لا شيء للـ commit إن كانت المهام السابقة قد سُجّلت.

---

## تعريف الإنجاز (Definition of Done)

- [ ] نص "سحب أفقي" لا يظهر أبداً لمستخدم TalkBack — ونص "بإصبعين" لا يظهر أبداً للمبصر.
- [ ] وصف الحالة المنطوق يطابق الإيماءات الحقيقية في كلا الوضعين.
- [ ] صفر تفريع Layouts جديد (الرقائق ظاهرة للجميع — النص فقط يتكيّف).
- [ ] `AudioEqualizerBars` مخفي صراحةً من شجرة الوصول.
- [ ] 4 اختبارات وحدات خضراء + بناء نظيف.
- [ ] مصفوفة ×2 اليدوية موقّعة على جهاز حقيقي.
- [ ] جدول المسح التدقيقي أعلاه يبقى مرجعاً — أي مكوّن مرئي جديد مستقبلاً يُضاف إليه قبل الدمج.

---

# ملاحظات المراجعة المعمارية — خطة 69 (ملزمة لوكيل التطوير)

**الحالة:** مراجعة معتمدة بشروط — يُمنع بدء تنفيذ خطة 69 قبل استيفاء البنود المعدّلة أدناه.
**المرجع:** خطة 69 (دعم وضع المبصرين القياسي وإصلاح الرجوع) كما سلّمها صاحب المنتج.
**نطاق المراجعة:** `AccessibleBottomSheet.kt`، `SurahIndexSheet.kt`، `BlindAccessibleClickable.kt`، `BlindAccessibleButtons.kt`، `GlobalBlindGestureModifier.kt`، `MainActivity.kt`، `TalkBackCompositionLocal.kt`.

## 1. أحكام المراجعة على الأسئلة المعمارية

| البند | الحكم |
|---|---|
| فصل `BackHandler` + تمرير `onBackPress` من الابن للأب (State Hoisting) | ✅ **معتمد** — بشرط الصيغة المعدّلة في §2 |
| التفريع `if (isTalkBackEnabled)` داخل `blindAccessibleClickable` لتفريغ النقرة المفردة | ✅ **آمن على شجرة الوصول** — معتمد |
| تعطيل `interceptBlindDoubleTap` العالمي | ⚠️ **ليس اختيارياً — إلزامي الحذف الكامل** (§3 / W2) |
| إلغاء القارئ الداخلي لغير مستخدمي TalkBack | ⚠️ **موقوف على حسم قرار W1** (§3) قبل التنفيذ |
| خطة التحقق اليدوي | ⚠️ **ناقصة** — تُستكمل ببنود §4 (W5) |

### مبررات الحكم (للفهم — لا إعادة تحقيق)

- **لماذا الفصل معتمد:** هيمنة `BackHandler` في Compose تتبع ترتيب تسجيل الـ callbacks (LIFO) — ترتيب هش يعتمد على ترتيب التركيب وقد ينقلب مع أي إعادة تركيب شرطية. مالك واحد للرجوع في الغلاف (`AccessibleBottomSheet`) مع منطق مرفوع من الابن يلغي فئة الاختطاف كلياً. المعامل الاختياري `onBackPress: (() -> Unit)? = null` متوافق رجعياً — تم التحقق أن `BookmarksSheet` و`ReciterSelectorSheet` و`VoiceCommandGuideSheet` لا تحتوي BackHandlers داخلية (الوحيدات في المشروع: `AccessibleBottomSheet` و`SurahIndexSheet` و`ScreenOffSaverOverlay`).
- **لماذا التفريع آمن:** يحدث في طبقة مؤشرات الإدخال فقط؛ الفرعان ينتجان نفس عقدة Semantics (إجراء `onClick` + `Role` + دمج الأبناء) → لا تغيّر في شجرة الوصول، ومتوافق مع القاعدة الذهبية (تفريع سلوك لا تصميم). التبديل الحي أثناء التشغيل آمن عبر إعادة التركيب، وخدمات الوصول الأخرى (Voice Access / Switch Access) تبقى قادرة على التفعيل في الفرعين.

## 2. التعديل الإلزامي على بند إصلاح الرجوع (خطة 69 §3)

الـ lambda المقترح في خطة 69 يحذف بصمت وظيفتين موجودتين في المعالج الحالي (`SurahIndexSheet.kt:102-110`) — هذا انحدار ممنوع:

1. `keyboardController?.hide()` — إخفاء لوحة المفاتيح عند الرجوع (يوجد بحث في الفهرس).
2. `onAnnounce("تم العودة لقائمة السور")` — المعادل المسموع للعودة البصرية (قاعدة التكافؤ في `.agents/DUAL_MODE_ARCHITECTURE.md`).

**الصيغة الإلزامية البديلة:**

```kotlin
onBackPress = {
    keyboardController?.hide()
    if (selectedSurahForAyahs != null) {
        selectedSurahForAyahs = null
        onAnnounce("تم العودة لقائمة السور") // معادل مسموع — إلزامي
    } else {
        onDismiss()
    }
}
```

**قيد إضافي:** الحفاظ على ترتيب التركيب الحالي في `QuranPlayerScreen.kt` (الـ `ScreenOffSaverOverlay` قبل النوافذ) حتى لا يختطف الـ Overlay زر الرجوع من نافذة مفتوحة فوقه.

## 3. التحذيرات الملزمة (مرتبة حسب الخطورة)

### 🔴 W1 — قرار منتجي يجب حسمه قبل التنفيذ: TalkBack OFF ≠ مبصر

القارئ الداخلي (نقرة = نطق، نقرتان = تنفيذ، النقرة المزدوجة العالمية لإعادة الآية) بُني **للكفيف الذي لا يشغّل TalkBack**، لا للمبصر. إلغاؤه بالكامل يجعل التطبيق غير قابل للاستخدام لتلك الفئة.

**الخياران المسموحان (يوثَّق أحدهما في الخطة قبل التنفيذ):**
- **(أ)** تأكيد منتجي صريح موثّق: "جميع المستخدمين الكفوف يعتمدون TalkBack حصراً" → يُسمح بالإلغاء الكامل.
- **(ب)** التحول لكشف **ثلاثي الأوضاع**: `TalkBack (تلقائي) / مساعد داخلي / مبصر` عبر مفتاح إعدادات أو منتقي وضع عند أول تشغيل، مع بقاء المساعد الداخلي خياراً.

### 🔴 W2 — `interceptBlindDoubleTap`: الحذف الكامل إلزامي وليس اختيارياً

آلية الخلل الشبحي إن تُرك فعّالاً مع النقرة القياسية الجديدة:
1. المبصر ينقر "تشغيل" → يعمل فوراً.
2. ينقر مجدداً خلال `doubleTapTimeoutMillis` → المعترض العالمي (`MainActivity.kt:100`) يلتقطها كنقرة مزدوجة، **يستهلكها**، ويستدعي `pendingActionManager.execute()`.
3. وضع المبصر الجديد لا يسجّل إجراءات معلّقة → يُنفَّذ الـ fallback `replayCurrentAyah()` (`QuranPlayerScreen.kt:156`) → **إعادة تشغيل الآية شبحياً مع كل نقر سريع متتالي**.

**النطاق الإلزامي للحذف (يُضاف لخطة 69 صراحة):**
- موضع الاستدعاء في `MainActivity.kt` (~سطر 100) وما يتعلق به من `pendingActionManager.execute()`.
- `Modifier.interceptBlindDoubleTap` كاملاً في `GlobalBlindGestureModifier.kt`.
- `GlobalBlindGestureState.lastDoubleTapTime` وحاجز الـ 500ms داخل فرع else في `blindAccessibleClickable`.
- الاستيرادات اليتيمة الناتجة.

### 🟡 W3 — موت منظومة `PendingBlindActionManager`

بعد W2 يصبح `LocalPendingBlindAction` / `PendingBlindActionManager` (`TalkBackCompositionLocal.kt:17`) و`setFallback`/`clear` بلا منتجين — كود ميت. يُوثَّق في الخطة: **حذف المنظومة كاملة** إن حُسم W1 بالخيار (أ)، أو إبقاؤها إن حُسم بالخيار (ب). تركها معلقة ممنوع.

### 🟡 W4 — قرارات صغيرة تُحسم داخل الخطة لا أثناء التنفيذ

- **الضغط المطوّل على `AyahCard` للمبصر** (`onLongClick = replayCurrentAyah` حالياً): يبقى أم يُحذف؟ يُحدَّد صراحة.
- **معامل `onSingleTap`** في `blindAccessibleClickable`/`AyahCard` يصبح بلا مستهلك في وضع المبصر: يُوثَّق كـ TalkBack-only أو يُنظَّف — قرار صريح.
- **`TrialExpiredScreen`** (سطر 47-51): هل يبقى النطق عبر TTS الداخلي في وضع المبصر أم يُقتصر على TalkBack؟
- **تباين النص الجديد** "الوضع: مبصرين" بـ `TextMutedZinc` على `DarkImmersiveBg`: يُتحقق من نسبة تباين ≥ 4.5:1 (WCAG) — المبصر ضعيف البصر مستفيد أيضاً.

### 🟡 W5 — استكمال خطة التحقق (تُضاف لخطة 69)

1. **اختبار آلي للرجوع:** Compose Test يركّب `SurahIndexSheet` ويؤكد أن الرجوع من قائمة الآيات يعود لقائمة السور، ومن قائمة السور يغلق النافذة — دون جهاز.
2. **تبديل TalkBack أثناء التشغيل** والشاشة مفتوحة → تحوّل حي للسلوك دون انهيار.
3. **نقر سريع متتالٍ** (3-4 نقرات) للمبصر على التشغيل → صفر إعادة تشغيل شبحية (انحدار W2).
4. **الرجوع ولوحة المفاتيح مفتوحة** في بحث الفهرس → تُخفى اللوحة ولا يُغلق التطبيق.
5. **بعد الرجوع من الآيات للسور مع TalkBack:** التركيز يعود لموضع معقول داخل النافذة، ويُسمع إعلان "تم العودة لقائمة السور".

## 4. شرط البدء

لا يبدأ وكيل التطوير تنفيذ خطة 69 إلا بعد: (1) توثيق حسم W1 من صاحب المنتج، (2) اعتماد الصيغة المعدّلة في §2، (3) إدراج نطاق W2 الكامل ضمن مهام التنفيذ.


