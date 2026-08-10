<div dir="rtl">

# 🌟 المبدأ المعماري الذهبي: شجرة مكونات واحدة وسلوك متكيف (Dual-Mode Architecture)

**هذا الدليل ملزم بشكل قاطع لوكيل التطوير (Development Agent) في كافة المهام المستقبلية لضمان بقاء التطبيق متوافقاً مع المبصرين والمكفوفين في نفس الوقت.**

## 1. القاعدة الذهبية (The Golden Rule)
**"شجرة مكونات واحدة، وسلوك متكيف — وليس شاشتين منفصلتين."**

يُمنع منعاً باتاً كتابة شرط تفريعي للشاشات (Layout Branching) مثل:
```kotlin
// ❌ خطأ معماري فادح: تدمير شجرة المكونات وتكرار الكود
if (isTalkBackEnabled) {
    AccessibleScreen()
} else {
    NormalScreen()
}
```

**الصحيح هو:** استخدام نفس المكون (`HorizontalPager`، `AyahCard`، `ViewModel`)، وما يتغير هو **طبقة السلوك فقط**.

## 2. طبقات التكيف الخمس (The 5 Layers of Adaptation)

| الطبقة | للمبصر | للمكفوف (TalkBack) |
| :--- | :--- | :--- |
| **1. المكونات (Components)** | نفس المكون تماماً (UI Tree واحدة) | نفس المكون تماماً (UI Tree واحدة) |
| **2. الإيماءات (Gestures)** | نقرة = تنفيذ مباشر | نقرة = نطق الوصف، نقرتان = تنفيذ مباشر |
| **3. التغذية الراجعة (Feedback)** | بصرية (حدود، ألوان، رسوم متحركة) | صوتية (إعلانات Accessibility أو TTS داخلي) |
| **4. التركيز (Focus)** | لا يُدار برمجياً (يعتمد على لمس المستخدم) | تأمين التركيز وتوجيهه برمجياً (Focus Modifiers) |
| **5. الوضعيات الخاصة (Modes)** | واجهة مضيئة كاملة | إمكانية تفعيل وضعيات توفير (مثل Screen-Off) |

## 3. آلية الكشف والحقن الذكي (Detection & Injection)

لتطبيق هذا المبدأ دون تمرير معاملات (`Parameters`) معقدة تلوث المكونات، يجب اتباع نمط الحقن عبر `CompositionLocal`:

1. **الكشف (مصدر واحد للحقيقة في أعلى الشجرة):**
   ```kotlin
   val isTalkBackEnabled by viewModel.speechManager.isTalkBackEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
   ```

2. **الحقن (يسري على الشجرة بالكامل):**
   ```kotlin
   CompositionLocalProvider(LocalTalkBackEnabled provides isTalkBackEnabled) { 
       // جميع المكونات الداخلية ستتفاعل مع حالة TalkBack بذكاء
       QuranPlayerScreenContent() 
   }
   ```

3. **الاستهلاك (في أي مكون فرعي يحتاج سلوكاً متكيفاً):**
   ```kotlin
   val isTalkBackEnabled = LocalTalkBackEnabled.current
   Modifier.pointerInput(isTalkBackEnabled) {
       if (isTalkBackEnabled) return@pointerInput // تعطيل الإيماءات المخصصة وتركها لـ TalkBack
       // ...
   }
   ```

## 4. قواعد صارمة للاستمرار ثنائي الوضع (Strict Dual-Mode Rules)

1. **ممنوع تفريع التصميم (No Layout Branching):** يُسمح فقط بتفريع السلوك (مثل `modifiers`, `focus`, `announcements`).
2. **قاعدة التكافؤ (Equivalence):** كل شيء مرئي وملحوظ للمبصر يجب أن يكون له معادل مسموع للكفيف (والعكس غير إلزامي).
3. **مصفوفة الاختبار الإجبارية (The ×2 Test Matrix):** يجب على وكيل الاختبارات فحص كل ميزة جديدة مرتين:
   - مسار `TalkBack ON`
   - مسار `TalkBack OFF`
4. **المنفعة المتبادلة (Mutual Benefit):** أي تحسين لتجربة الكفيف (مثل تكبير أهداف اللمس، تحسين التباين، أو تبسيط التسلسل) يجب أن يطبق على المبصر لأنه يُعد تحسيناً شاملاً لتجربة المستخدم (UX).

</div>
