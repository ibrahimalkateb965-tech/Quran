# السياق المعماري لمشروع (Blind App - تطبيق القرآن للمكفوفين)

## 📌 نبذة عن المشروع
التطبيق مخصص لخدمة المكفوفين بشكل أساسي عبر تقديم واجهة صوتية وتفاعلية لقراءة وسماع القرآن الكريم. يعتمد التطبيق على التفاعل الدقيق مع قارئ الشاشة (TalkBack) وإيماءات اللمس المخصصة لتعويض الرؤية.

## 🏗️ المعمارية البرمجية (Architecture)
- **النمط المعماري:** Clean Architecture + MVVM (Model-View-ViewModel).
- **طبقة النطاق (Domain Layer):** عزل واجهات المستودعات (`interface QuranRepository`) ونماذج الأعمال.
- **واجهة المستخدم (UI Layer):** Jetpack Compose بأسلوب Declarative UI، مع التركيز التام على إمكانية الوصول (Accessibility).
- **إدارة الحالة (State Management):** الاعتماد على `StateFlow` في الـ `ViewModel` كمصدر الحقيقة الوحيد (Single Source of Truth)، مع عزل `playbackProgress` لتقليل عمليات الـ Recomposition.
- **حقن الاعتماديات (DI):** **Dagger Hilt** مفعل رسمياً عبر `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, و `AppModule`.
- **تشغيل الصوتيات:** `ExoPlayer` / `Media3` كخدمة في الخلفية (Foreground Service) مع تقييد `onConnect` للجلسات الموثوقة.

## 🛠️ التقنيات والمكتبات الأساسية (Tech Stack)
- **اللغة:** Kotlin
- **واجهة المستخدم:** Jetpack Compose + Material Design 3
- **حقن الاعتماديات:** Dagger Hilt
- **مصادر البيانات:** قاعدة بيانات محلية Room DB + ملف JSON العثماني في Assets مع تخزين مؤقت في الذاكرة (In-Memory Caching).
- **الأمان والترخيص:** `EncryptedSharedPreferences` + `MasterKey AES256` + `TrialManager` بفترة تجريبية 30 يوماً وتجزئة PIN مملّحة (Salted).
- **إمكانية الوصول (Accessibility):** استخدام مكثف لـ `clearAndSetSemantics` و `customActions` مع مكون `blindAccessibleClickable` المخصص لدعم TalkBack بشكل مثالي.

## ⚠️ قواعد ومبادئ صارمة (Strict Conventions)
1. **أداء الواجهة (Recomposition):** يُمنع منعاً باتاً تمرير متغيرات الحالة سريعة التغير كقيم مباشرة في قوائم Lazy/Pager؛ يجب تغليفها كـ Providers/Lambdas `() -> T`.
2. **إمكانية الوصول (TalkBack First):** الإيماءات المباشرة مثل `detectTapGestures` لا يجب أن تلغي أو تتداخل مع دلالات TalkBack. يجب استخدام `SemanticsPropertyReceiver.customActions` لتعريف الأحداث.
3. **الفشل الآمن والاستعادة (Fail-Safe & Restoration):** يجب تخزين موضع القراءة (Surah, Ayah) بشكل آمن واسترجاعه فور فتح التطبيق. يُمنع تهيئة Pager/LazyList يعتمد على الجلسة المحفوظة قبل التأكد من تحميل البيانات `isNotEmpty()`.
4. **الأمان والتعتيم:** تفعيل `isMinifyEnabled = true` في الـ Release مع استثناء ملفات التفضيلات المشفرة من النسخ الاحتياطي للأندرويد.

## 📈 الوضع الحالي للمشروع
- **تم إكمال كافة مراحل خطة التدقيق الشامل (المراحل 0، 1، 2، 3 بنجاح).**
- تم إغلاق كافة الثغرات الأمنية الحرجة وحواجب الإطلاق.
- تم تطبيق Dagger Hilt و Clean Architecture بشكل كامل.
- تم تحسين استهلاك البطارية والأداء وإلغاء عمليات إعادة الرسم المتكررة.
- تم إنشاء اختبارات وحدة حقيقية لطبقات الأمان والمستودع.
