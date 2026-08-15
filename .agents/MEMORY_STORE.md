<div dir="rtl">

# مخزن الذاكرة المركزي لمشروع Blind App (MEMORY_STORE.md)

> [!IMPORTANT]
> هذا الملف هو السجل المركزي للذاكرة المستدامة. يُحدّث تلقائياً بواسطة وكيل `persistent-memory-engine` بعد كل خطاف نجاح أو أمر تسجيل يدوي.

---

## سجل الدروس المستفادة (Lessons Learned)

```yaml
- id: MEM-2026-07-21-001
  type: lesson
  timestamp: "2026-07-21T11:00:00+03:00"
  agents: [agent-optimizer, prompt-engineer]
  context: "تأسيس نظام Autovem وبناء 37 مهارة جديدة"
  content: "عند بناء مهارات جديدة، يجب دائماً اتباع هيكل YAML frontmatter موحد (name + description) والتأكد من تسجيل كل مهارة في sub_agents.yaml"
  tags: [skill-creation, standardization]
  status: active

- id: MEM-2026-07-21-002
  type: lesson
  timestamp: "2026-07-21T11:50:00+03:00"
  agents: [ai-geo-seo-optimizer]
  context: "تحسين محركات البحث التوليدية GEO"
  content: "تحسين المحتوى للظهور في محركات بحث الذكاء الاصطناعي (Perplexity, SearchGPT, Gemini) يتطلب زيادة كثافة الحقائق، الاعتماد على نبرة الخبير، وتقسيم المحتوى لفقرات مستقلة قابلة للاقتباس والاسترجاع عبر RAG."
  tags: [geo, ai-search, seo]
  status: active

- id: MEM-2026-07-21-003
  type: lesson
  timestamp: "2026-07-21T12:15:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "تحميل الملفات الكبيرة برمجياً عبر PowerShell"
  content: "عند أتمتة تحميل الملفات الكبيرة (مثل MSI) من GitHub، استخدام Invoke-WebRequest قد يؤدي إلى انقطاع الاتصال (Connection Forcibly Closed). الحل المستقر هو تفعيل TLS 1.2 واستخدام وحدة BITS (Start-BitsTransfer) لضمان التحميل المستقر ودعم الاستئناف."
  tags: [powershell, automation, bits-transfer, bug-fix]
  status: active

- id: MEM-2026-07-21-004
  type: bug-fix
  timestamp: "2026-07-21T16:50:00+03:00"
  agents: [devops-deployer, persistent-memory-engine]
  context: "نشر ملفات ماركداون ولوحة التحكم على GitHub Pages"
  content: "موقع GitHub Pages يبني المواقع بشكل افتراضي باستخدام Jekyll. هذا يمنع قراءة الملفات التي تحتوي على رموز معينة أو ملفات البنية. لمنع ذلك والحصول على استعراض مباشر وسليم لملفات Markdown الخام، يجب إنشاء ملف فارغ باسم `.nojekyll` في الجذر الرئيسي للمستودع."
  tags: [github-pages, jekyll, static-site]
  status: active

- id: MEM-2026-07-21-005
  type: bug-fix
  timestamp: "2026-07-21T16:50:00+03:00"
  agents: [debugger, persistent-memory-engine]
  context: "معالجة أخطاء الترميز للمخرجات العربية على ويندوز"
  content: "تشغيل سكربتات بايثون في الخلفية على ويندوز تطبع نصوصاً باللغة العربية أو رموزاً تعبيرية (Emojis) يسبب توقف السكربت بخطأ UnicodeEncodeError (ترميز cp1252). الحل هو فرض ترميز UTF-8 لمخرجات الكونسول بإضافة `sys.stdout = codecs.getwriter('utf-8')(sys.stdout.detach())` في بداية السكربت، مع إزالة الرموز غير المدعومة من الطباعة الافتراضية."
  tags: [windows, python, encoding, unicode, bug-fix]
  status: active

- id: MEM-2026-07-21-006
  type: bug-fix
  timestamp: "2026-07-21T16:50:00+03:00"
  agents: [devops-deployer, persistent-memory-engine]
  context: "حل تعارض عمليات البوت النشطة في الخلفية"
  content: "عند تشغيل بوت تليجرام باستخدام getUpdates، فإن أي نسخة قديمة نشطة في الخلفية ستسبب خطأ Conflict (رمز 409). لحلها يجب تحديد معرّف العملية (PID) لـ pythonw.exe أو python.exe وإنهاؤها قسراً. وإذا كانت العملية تعمل بصلاحيات مدير (Elevated) فيجب فتح Terminal كمسؤول (Run as Admin) لتنفيذ أمر `taskkill /F /PID <PID>` بنجاح."
  tags: [process-management, taskkill, telegram-bot, conflict, windows]
  status: active

- id: MEM-2026-08-09-001
  type: preference
  timestamp: "2026-08-09T19:27:00+03:00"
  agents: [persistent-memory-engine]
  context: "سلوك نسخ النصوص في واجهات الويب (Copy Action Behavior)"
  content: "عند استخدام زر النسخ في تطبيق الويب، يجب ألا ينسخ الوصف، بل المحفز (Trigger) فقط لمنع تداخل النصوص ونسخ معلومات إضافية غير مرغوبة."
  tags: [web-app, ux, copy-action, prompt-trigger]
  status: active

- id: MEM-2026-08-09-002
  type: lesson
  timestamp: "2026-08-09T21:05:00+03:00"
  agents: [agent-optimizer, code-reviewer-quality]
  context: "التدقيق المعماري الصارم (Devil's Advocate Audit)"
  content: "الاعتماد الأولي على النماذج قد يولد حلولاً سطحية (Band-aids) مثل الاعتماد على taskkill أو التحايل بالبايثون لحل مشاكل PowerShell. يجب دائماً تفعيل وكلاء الجودة (5، 6، 7) معاً تحت دور المدقق الصارم (محامي الشيطان) لتمزيق الحلول السطحية وفرض حلول هندسية جذرية مثل (Mutex Locks، وتصحيح ترميز الكونسول مباشرة)."
  tags: [quality-audit, best-practices, devil-advocate]
  status: active
- id: MEM-2026-08-10-001
  type: lesson
  timestamp: "2026-08-10T19:30:00+03:00"
  agents: [persistent-memory-engine, agent-optimizer]
  context: "تكرار أخطاء مسجلة مسبقاً (استخدام Fully Qualified Names لدوال الامتداد) بسبب تجاهل قراءة الذاكرة."
  content: "مجرد (تسجيل) الذكريات لا يكفي. يقع الوكيل أحياناً في (Shortcut Anti-Pattern) محاولاً اختصار الوقت. تم إقرار مبدأ (حقن السياق الإجباري Mandatory Context Injection): يجب أن تُمرر الدروس والأخطاء الشائعة ذات الصلة قسرياً للوكيل قبل بدء البرمجة، لضمان عدم استناده فقط لحفظه الخاطئ للغة المترجم."
  tags: [architecture, global, agent-behavior, context-injection, anti-pattern]
  status: active
- id: MEM-2026-08-10-002
  type: lesson
  timestamp: "2026-08-10T21:10:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "نجاح السحب الأفقي الصامت بإصبعين باستخدام TalkBack"
  content: "تم بنجاح استعادة ميزة السحب بإصبعين للتنقل بين الصفحات مع تشغيل TalkBack. تبين أن المشكلة كانت بسبب تغليف الـ Pager بمكونات مخصصة (SilentAccessiblePager) تمنع السلوك الافتراضي. الحل الأمثل: الاعتماد المطلق على (HorizontalPager) القياسي وتطبيق مبدأ المعمارية ثنائية الوضع (Dual-Mode Architecture). ولضمان الصمت التام للـ Pager أثناء السحب، تم إعطاؤه contentDescription بفاصلة فقط `,`."
  tags: [accessibility, talkback, horizontal-pager, dual-mode, bug-fix, success]
  status: active
- id: MEM-2026-08-10-003
  type: lesson
  timestamp: "2026-08-10T23:05:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "حماية الميزات المحذوفة مسبقاً بناءً على طلب العميل (Feature Deletion Memory)"
  content: "ميزة عالمية (Global Rule): يُمنع منعاً باتاً إعادة إضافة أي مكون واجهة مستخدم (UI Component) أو ميزة (Feature) تم حذفها مسبقاً (مثل شريط التشغيل PlayerControlPanel أو الأوامر الصوتية) إلا بطلب صريح ومباشر من المستخدم. يجب دائماً احترام حالة الكود كما هو موجود في آخر Commit وعدم افتراض أن اختفاء المكون هو خطأ برمجي يحتاج للاسترجاع."
  tags: [architecture, global, agent-behavior, ui-components]
  status: active
- id: MEM-2026-08-13-002
  type: lesson
  timestamp: "2026-08-13T12:15:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "الفرض الإجباري لبرومبت 'محامي الشيطان' (Devil's Advocate Persona) على المستوى العالمي"
  content: "لتجنب نسيان حلقة محامي الشيطان وتخطيها، يُفرض قسرياً على الوكيل عند بدء أي ميزة جديدة التوقف فور إنشاء الخطة المبدئية، واستدعاء شخصية 'Devil’s Advocate & Senior Staff Architect'. يُمنع كتابة الكود قبل توليد المخرجات الصارمة: [DEVIL'S ADVOCATE CRITIQUE], [ENGINEERING FIXES], و [MASTER REFINED PLAN CONSTRAINTS] باستخدام معايير التقييم الأربعة (الكمال المعماري، حالات الحافة، الآثار الجانبية، والسطحية). تم حقن هذا القيد في ACTIVE_CONTEXT_INJECTION."
  tags: [architecture, global, devil-advocate, context-injection]
  status: active
```

---

## سجل القرارات المعمارية (Architecture Decisions)

```yaml
- id: ADR-2026-08-01-001
  type: decision
  timestamp: "2026-07-21T11:00:00+03:00"
  agents: [code-architect, agent-optimizer]
  context: "اختيار استراتيجية النماذج لنظام Autovem"
  content: "تم اعتماد النظام الثلاثي: flash للمحتوى والتسويق والمالية والقانون، pro للبرمجة والمراجعة والبناء، thinking للمعمارية والتخطيط والذاكرة"
  tags: [model-strategy, autovem-core]
  status: active

- id: ADR-2026-07-21-002
  type: decision
  timestamp: "2026-07-21T11:00:00+03:00"
  agents: [prompt-engineer]
  context: "استقلالية نظام Autovem"
  content: "حذف جميع الإشارات لأنظمة خارجية من ملفات المهارات والوكلاء. النظام مستقل تماماً ومحايد للنماذج (Model-Agnostic)"
  tags: [independence, cleanup]
  status: active

- id: ADR-2026-07-21-003
  type: decision
  timestamp: "2026-07-21T12:15:00+03:00"
  agents: [code-architect, persistent-memory-engine]
  context: "هيكلية الذاكرة للمشاريع المتعددة (Monorepo Workspace)"
  content: "لمنع تداخل السياق في بيئات العمل متعددة المشاريع، تم اعتماد هيكلية لامركزية للذاكرة عبر تهيئة مجلد `.agents` محلي داخل كل مشروع فرعي يحتوي على ملفات `MEMORY_STORE.md` و `PROJECT_CONTEXT.md` الخاصة به، مع ربطها جميعاً بأدلة الخطافات والمهارات المركزية."
  tags: [memory-architecture, monorepo, context-isolation]
  status: active

- id: ADR-2026-07-21-004
  type: decision
  timestamp: "2026-07-21T13:30:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "توافقية النظام مع المشاريع القديمة (Legacy Support)"
  content: "لتهيئة مشاريع قديمة تمتلك بالفعل مجلدات `.agents` وقواعد `AGENTS.md` منفصلة، نعتمد قاعدة (الإضافة فقط دون استبدال). نضيف ملفي `MEMORY_STORE.md` و `PROJECT_CONTEXT.md` حصرياً، ونبقي قواعد الخطافات القديمة للمشروع سليمة كما هي لضمان عدم تأثر السلوك السابق للمشروع القديم."
  tags: [legacy-support, backward-compatibility, architecture]
  status: active

- id: ADR-2026-07-21-005
  type: decision
  timestamp: "2026-07-21T16:50:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "تخصيص أعمدة لوحة القيادة (Dashboard Columns)"
  content: "لتفعيل دورة إضافات لتطبيق الويب، يجب الالتزام الصارم بتخصيص العمود الثالث ليكون (المحفزات - Triggers فقط) لضمان أن زر النسخ في التطبيق ينسخ المحفز فقط لتشغيل الوكيل، بينما يتم عزل (الوصف والتفاصيل) في عمود مستقل (الأخير) ليتم عرضه للمستخدم كمعلومات دون أن يتداخل مع النص المنسوخ."
  tags: [excel-export, ui-preference, prompt-library, copy-action]
  status: active

- id: ADR-2026-08-09-002
  type: decision
  timestamp: "2026-08-09T21:05:00+03:00"
  agents: [persistent-memory-engine, agent-optimizer]
  context: "اعتماد الخطاف الحارس (Watchdog Hook) لمنع فقدان الذاكرة"
  content: "اكتشفنا ظاهرة (المشاريع فارغة الذاكرة) في تطبيقي (تاج الوقار) و(تيجان النور) بسبب عدم تفعيل المستخدم لخطاف حفظ الذاكرة في نهاية الجلسة. كقرار معماري، تم تعديل قالب (AGENTS_SEED) لإلزام النظام مستقبلاً بتضمين خطاف حارس (Watchdog) يستخرج الـ Diffs آلياً ويحفظها في الذاكرة دون انتظار طلب مباشر."
  tags: [memory-architecture, compliance, watchdog-hook]
  status: active
```

---

## سجل الأخطاء المحلولة والقائمة (Resolved & Pending Bugs)

```yaml
- id: BUG-2026-08-01-001
  type: bug-fix
  timestamp: "2026-08-01T01:25:00+03:00"
  agents: [persistent-memory-engine, debugger]
  context: "مشكلة عدم توافق صوت الـ TTS الداخلي مع صوت الهاتف (صوت أنثوي بدلاً من المألوف)"
  content: "لم يتم حل المشكلة الجذرية المتعلقة بنوع الصوت الداخلي للتطبيق (لا يزال أنثوياً ومختلفاً عن صوت الهاتف الفعلي). يجب إجبار التطبيق على تبني محرك وصوت الـ TTS الافتراضي للنظام بالكامل."
  tags: [tts, accessibility, pending]
  status: pending_investigation

- id: BUG-2026-07-21-007
  type: bug-fix
  timestamp: "2026-07-21T21:23:00+03:00"
  agents: [persistent-memory-engine, debugger]
  context: "فشل إنشاء سجل جديد (Failed to create record) في PocketBase مع بيانات استجابة فارغة (data: {})"
  content: "عند إرسال طلب لإنشاء سجل يحتوي على حقول علاقات (Relations)، يجب التأكد أن قيمة الحقل المُرسلة هي الـ ID الخاص بالعنصر (وهو نص مكون من 15 حرفاً). استخدام الاسم كـ ID يؤدي لرفض السيرفر بـ 400 Bad Request مع رسالة فشل عامة فارغة data. تم تطبيق آلية لاستخراج الـ ID الصحيح، لكن المشكلة لا تزال قائمة (جاري التحقيق لاحقاً في احتمالية أن المشكلة في relation آخر مثل created_by_admin أو مشكلة في الـ Rules)."
  tags: [pocketbase, bug-fix, relations, api, pending]

- id: MEM-2026-08-01-001
  type: bug-fix
  timestamp: "2026-08-01T01:25:00+03:00"
  agents: [persistent-memory-engine, debugger]
  context: "مشكلة عدم توافق صوت الـ TTS الداخلي مع صوت الهاتف (صوت أنثوي بدلاً من المألوف)"
  content: "الدروس المستفادة من هذه المرحلة:\n1. منع الميكروفون من العمل أثناء النطق لتفادي تعارض Audio Focus.\n2. ضرورة إضافة `<queries>` لخدمة `RecognitionService` في أندرويد 11+.\n3. تطبيق تطبيع الحروف العربية للنصوص الملتقطة بالصوت (Normalization).\n\nالمشكلة المتبقية: لم يتم حل المشكلة الجذرية المتعلقة بنوع الصوت الداخلي للتطبيق (لا يزال أنثوياً ومختلفاً عن صوت الهاتف الفعلي). يجب في جلسة العمل القادمة إجبار التطبيق على تبني محرك وصوت الـ TTS الافتراضي للنظام بالكامل."
  tags: [tts, accessibility, speech-recognizer, pending]
  status: pending_investigation

- id: BUG-2026-08-01-002
  type: bug-fix
  timestamp: "2026-08-01T15:34:00+03:00"
  agents: [persistent-memory-engine, debugger, android-testing]
  context: "مشكلة عدم توافق صوت الـ TTS الداخلي مع صوت الهاتف وتعارض الميكروفون عند الاستماع، وفشل اختبارات VoiceCommandManagerTest."
  resolution: "إدارة الـ Audio Focus بشكل صارم في VoiceCommandManager (requestAudioFocus و abandonAudioFocus). تعديل VoiceCommandManager ليعيد النص الخام. وتجاوز اختبارات VoiceCommandManagerTest بعد عمل Mock لـ AudioManager لمنع ClassCastException."

- id: BUG-2026-08-02-001
  type: bug-fix
  timestamp: "2026-08-02T14:22:00+03:00"
  agents: [persistent-memory-engine, debugger]
  context: "إغلاق التطبيق (Crash) والدخول في حلقة لا نهائية عند تمرير واجهة Pager."
  content: "كان التطبيق ينهار أو يتخطى الآيات سريعاً بسبب التقاط حالة قديمة (Stale State) داخل بيئة LaunchedEffect في Compose. تم استبدال القراءة بحالة حية `viewModel.uiState.value.currentIndex` واستخدام أمر انتقال مباشر `goToAyah` لتفادي المشكلة."
  tags: [compose, launched-effect, bug-fix, ui]

- id: ADR-2026-08-09-002
  type: decision
  timestamp: "2026-08-09T21:20:00+03:00"
  agents: [agent-optimizer, persistent-memory-engine]
  context: "تكرار ظاهرة 'المشاريع فارغة الذاكرة' في المشاريع السابقة مثل تاج الوقار."
  content: "تقرر اعتماد مبدأ 'الخطاف الحارس' (Watchdog Hook) لمنع فقدان الذاكرة. لا يمكن الاعتماد على نوايا المستخدم لتفعيل وكيل الذاكرة يدوياً، بل يجب أن يقوم النظام بأخذ لقطة للمتغيرات وحفظها في MEMORY_STORE.md بشكل إجباري قبل الإغلاق."
  tags: [architecture, memory, watchdog, reliability, global]

- id: MEM-2026-08-09-003
  type: lesson
  timestamp: "2026-08-09T21:20:00+03:00"
  agents: [agent-optimizer, code-reviewer-quality]
  context: "مراجعة قوالب التأسيس العالمية واكتشاف ترقيعات (Band-Aids) متراكمة."
  content: "تم إرساء مبدأ دور 'محامي الشيطان' (Devil's Advocate) للتدقيق المعماري الصارم، والذي يمنع رفض أي ترقيع سطحي (مثل استخدام taskkill لمعالجة تعارض العمليات، أو سكربتات بايثون لحل مشكلة ترميز PowerShell) واستبدالها بحلول جذرية مستدامة."
  tags: [architecture, auditing, clean-code, global]

- id: ADR-2026-08-09-004
  type: decision
  timestamp: "2026-08-09T21:44:00+03:00"
  agents: [agent-optimizer, persistent-memory-engine, mcp-tool-builder]
  context: "الحاجة إلى مزامنة الدروس المستفادة عبر كافة المشاريع المحلية في بيئة التطوير (IDE)."
  content: "تم ابتكار وتصميم أول خادم (MCP Server) مخصص لبيئة المحرر باستخدام `FastMCP`. وظيفته فحص الذاكرة المحلية لأي مشروع ومزامنة الدروس العالمية إلى المستودع المركزي. وتم دمج السكربت كإضافة (Plugin) متكاملة في مجلد الإعدادات ليعمل بشكل مركزي مع أي مشروع."
  tags: [mcp, architecture, memory, automation, global]
  status: active

- id: ADR-2026-08-13-003
  type: decision
  timestamp: "2026-08-13T15:38:00+03:00"
  agents: [agent-optimizer, persistent-memory-engine, code-architect]
  context: "منع تضخم نافذة السياق (Context Window Bloat) في ملف القواعد العالمي AGENTS.md"
  content: "تطبيقاً لمبدأ فصل الاهتمامات، يُمنع حشو ملف AGENTS.md المركزي بالتفاصيل التقنية والبرومبتات الطويلة. بدلاً من ذلك، تُعزل هذه التفاصيل في ملف مستقل (ACTIVE_CONTEXT_INJECTION.md) داخل مجلد config العالمي، ويُضاف سطر واحد فقط في AGENTS.md يوجه الوكيل لقراءة هذا الملف قبل أي عملية برمجية. هذا يحافظ على تركيز النماذج ويقلل استهلاك الذاكرة."
  tags: [architecture, global, context-window, optimization]
  status: active
```

---

## فهرس المهارات والقدرات (Skill Capability Index)

| القسم | عدد المهارات | المهارات |
|:------|:---:|:---------|
| **النواة والذاكرة** | 1 | persistent-memory-engine |
| **أدوات التطوير** | 4 | skill-forge-builder, docs-fetcher-context, mcp-tool-builder, webapp-qa-tester |
| **التصميم والعلامة** | 6 | ui-ux-design-lead, taste-design-critic, motion-transitions-pro, frontend-design-builder, web-artifacts-prototyper, brand-kit-keeper |
| **التسويق والنمو** | 5 | copywriting-lead, ai-geo-seo-optimizer, cro-conversion-lead, ad-creative-maker, customer-research-voice |
| **صناعة المحتوى** | 3 | post-content-writer, script-hook-generator, profile-optimizer |
| **المالية** | 6 | financial-statements-builder, journal-entry-keeper, reconciliation-auditor, variance-analyst, audit-support-prep, close-management-lead |
| **الأعمال** | 6 | cash-flow-watcher, invoice-chaser, payroll-planner, margin-analyst, tax-prepper, campaign-runner |
| **القانون** | 6 | contract-reviewer, nda-triage, compliance-officer, legal-risk-assessor, vendor-vetter, signature-wrangler |
| **الوكلاء الأصليون** | 23 | (راجع sub_agents.yaml للقائمة الكاملة) |


## الدروس المستفادة من مشروع تطبيق المكفوفين (TTS & STT)
1. **احترام تفضيلات النطق للمستخدم الكفيف (Accessibility UX)**
   - المشكلة: محاولة فرض محرك نطق محدد أو فرض أصوات سحابية عالية الجودة تشتت المستخدم.
   - الحل: ترك تهيئة TextToSpeech افتراضية تماماً بدون تمرير اسم المحرك ليعتمد التطبيق فوراً على تفضيلات النظام.
2. **منع تداخل النطق بين التطبيق و TalkBack**
   - المشكلة: المساعد الداخلي ينطق الوصف في نفس وقت TalkBack.
   - الحل: منع النطق الداخلي إذا كان isTalkBackEnabled صحيحاً. توفير contentDescription بدلاً من ذلك.
3. **أمان أندرويد 11+ في خدمة التعرف الصوتي (SpeechRecognizer)**
   - المشكلة: فشل التعرف الصوتي مباشرة بسبب حجب الخدمة أمنياً.
   - الحل: إضافة queries في AndroidManifest.xml للسماح بالوصول لخدمة جوجل.
4. **تجنب تعارض الـ TTS مع الميكروفون (Audio Focus Conflict)**
   - المشكلة: الميكروفون يلتقط صوت المساعد ويغلق.
   - الحل: عدم تشغيل TTS.speak قبل فتح الميكروفون مباشرة. الاعتماد على الرنة الافتراضية القصيرة (Beep).
5. **معالجة الاختلافات في تحويل الصوت إلى نص (Arabic STT Normalization)**
   - المشكلة: عدم التعرف على الأوامر بسبب اختلاف كتابة الهمزات والتاء المربوطة.
   - الحل: تطبيق نظام تطبيع (Normalization) لتوحيد الحروف قبل المقارنة.
6. **إدارة تنازع الصوت (Audio Focus Management) بشكل جذري**
   - المشكلة: تعارض مع قوارئ الشاشة رغم الإجراءات السابقة.
   - الحل: استدعاء requestAudioFocus مع AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE والتخلي عنه بـ abandonAudioFocus لضمان توجيه الصوت للتطبيق فقط أثناء الاستماع.
7. **محاكاة خدمات النظام في اختبارات الوحدة**
   - المشكلة: حقن Context يحمل getSystemService يسبب ClassCastException مع MockK.
   - الحل: عمل Mock صريح للخدمة وإرجاعها عند طلبها من الـ Context.
8. **تعارض التقاط الحالة القديمة في Compose (Stale State Capture in LaunchedEffect)**
   - المشكلة: الاعتماد على قيمة مقروءة من الـ `uiState` داخل كتلة `snapshotFlow` يؤدي لالتقاط قيمة قديمة (Stale Value)، مما قد يسبب حلقة لا نهائية (Infinite Loop) من التنقل العشوائي أو توقف التطبيق (Crash).
   - الحل: قراءة القيمة الحية مباشرة من المرجع داخل كتلة الـ `collect` مثل `viewModel.uiState.value.currentIndex` وتجنب استخدام المتغيرات الملتقطة، وكذلك الاعتماد على القفز المباشر `goToAyah(index)` بدلاً من حسابات الزيادة/النقصان النسبية.
9. **تطبيق مبدأ DRY (لا تكرر نفسك) على الروابط المركزية**
   - المشكلة: تكرار الرابط الأساسي (`https://verse.mp3quran.net/data/`) في كل عنصر ضمن قائمة القراء يرفع نسبة الخطأ ويجعل الصيانة صعبة.
   - الحل: استخراج الرابط كمتغير ثابت (`private const val BASE_URL`) وتمريره برمجياً لكل عنصر.
10. **توظيف أدوات Android Studio لحل قيود التعديل الخارجي**
   - المشكلة: الحاجة لقص وتعديل مقاسات الأيقونات (Image Processing) مع وجود قيد صارم يمنع تشغيل سكريبتات أو أوامر طرفية خارجية (No Shell Commands).
   - الحل: توجيه المستخدم لاستخدام الأداة المدمجة `Image Asset Studio` كخيار قياسي وأكثر أماناً لتوليد (Adaptive Icons) لجميع مقاسات الشاشات.
11. **التشغيل المتصل الديناميكي ومراعاة نمط المستخدم**
    - المشكلة: فرض قائمة تشغيل (Playlist) مستمرة يكسر النمط الافتراضي للتطبيق المبني على السحب اليدوي (Swipe-to-Read).
    - الحل: حقن قائمة الآيات المترابطة في `ExoPlayer` فقط عند تفعيل (الاستماع المتواصل)، واستخدام مستمع `onMediaItemTransition` لتحديث الواجهة، بينما يُترك النمط الفردي لتجربة السحب اليدوي الافتراضية.
12. **معالجة تعارض الصوت التلقائي عبر ContentType**
    - المشكلة: محاولة إدارة التركيز الصوتي (Audio Focus) يدوياً للإيقاف عند نطق TalkBack معقدة.
    - الحل: تغيير `ContentType` لمشغل `ExoPlayer` إلى `C.AUDIO_CONTENT_TYPE_SPEECH`. هذه الميزة المدمجة تجبر المشغل على الإيقاف المؤقت (Pause) تلقائياً عندما يتحدث المساعد (Ducking)، واستئناف التشغيل فور سكوته، دون الحاجة لمستمع صوتي يدوي.
13. **التدقيق المعماري الصارم ومنع الترقيعات السطحية (No Band-Aids)**
    - المشكلة: تراكم ديون تقنية بسبب استخدام حلول مؤقتة مثل `taskkill` لإنهاء العمليات المعلقة أو سكربتات خارجية لحل مشاكل ترميز.
    - الحل: تفعيل وكيل بدور "محامي الشيطان" لرفض هذه الترقيعات وفرض حلول هندسية صلبة (مثل Mutex Locks ووضع ترميز `UTF-8` محلي في `PowerShell`).
14. **ضمان استدامة الذاكرة عبر الخطاف الحارس (Watchdog Hook)**
    - المشكلة: مشاريع عملاقة (مثل تاج الوقار) لم تُسجل فيها أي ذكريات بسبب الاعتماد على تذكر المستخدم لاستدعاء وكيل الذاكرة.
    - الحل: بناء خطاف حارس مستقل يجبر النظام على استخراج الـ Diffs وتخزينها تلقائياً عند اقتراب انتهاء المهام.
15. **تنسيق عرض ماركداون للغة العربية في بيئة التطوير (IDE RTL Rendering)**
    - المشكلة: ملفات التوثيق العربية (`AGENTS.md` وغيرها) تظهر بشكل مشوه ومعكوس (الأرقام والرموز متداخلة) في واجهة العرض داخل المحرر.
    - الحل: تغليف جميع مستندات الماركداون والتقارير بـ `<div dir="rtl">` في بدايتها و `</div>` في نهايتها بشكل إجباري لضمان محاذاة صحيحة.
16. **معالجة انهيار التطبيق (NPE) داخل LazyColumn عند تغيير الحالة (Snapshot Capture)**
    - المشكلة: انهيار التطبيق بـ NullPointerException عند الضغط على زر الرجوع من قائمة الآيات، بسبب تعيين الحالة إلى null بينما لا يزال `LazyColumn` يحاول إعادة بناء عناصره بناءً على حالة قديمة. (تسريب مرجعي).
    - الحل: تطبيق نمط (Snapshot Capture) بالتقاط قيمة المتغير الثابتة قبل الـ if-condition (`val currentSurah = selectedSurahForAyahs`) واستخدامه لتهيئة الـ LazyColumn وتجنب أي استخدام لـ `!!` مع State متغيرة داخل دالة التكوين.

- id: MEM-2026-08-10-004
  type: lesson
  timestamp: "2026-08-10T20:30:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "إصلاحات قارئ الشاشة والسحب الأفقي"
  content: "تم إزالة تعديل الفاصلة (,) من AyahCard لمنع TalkBack من قراءة 'comma page'. كما تم إعادة الاعتماد على currentPage بدلاً من settledPage في مكون HorizontalPager مع إضافة متغير isProgrammaticScroll لتجنب تأخير تزامن الصوت عند التمرير الأفقي."
  tags: [accessibility, talkback, bug-fix, horizontal-pager]
  status: active

- id: MEM-2026-08-11-001
  type: lesson
  timestamp: "2026-08-11T00:05:00+03:00"
  agents: [persistent-memory-engine, debugger]
  context: "إصلاح انهيار التطبيق (NPE) داخل LazyColumn"
  content: "تم تطبيق حل Snapshot Capture لحماية القوائم الكسولة في Jetpack Compose من استثناءات NullPointer الناتجة عن التفريغ السريع للمتغيرات المشتركة."
  tags: [compose, lazycolumn, bug-fix, state]
  status: active

- id: MEM-2026-08-11-002
  type: lesson
  timestamp: "2026-08-11T00:25:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "تخصيص ترتيب البيانات الثابتة وتجاوز الفرز الافتراضي (Custom Sorting Override)"
  content: "لتطبيق فرز مخصص يطلبه المستخدم ويخالف الترتيب الأبجدي أو الافتراضي، الأفضل تطبيق نظام (أوزان مخصصة Custom Weights) باستخدام قائمة `indexOf` واسترجاع رقم أولوية (Priority). وللحفاظ على أي بيانات إضافية خارج القائمة المخصصة، يتم إعطاؤها وزناً كبيراً (مثل 999) لضمان إلحاقها دائماً في النهاية دون حذفها."
  tags: [kotlin, sorting, architecture, custom-logic]
  status: active
- id: MEM-2026-08-11-003
  type: lesson
  timestamp: "2026-08-11T12:00:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "إسكات حاويات التمرير (Pagers) تماماً مع الاحتفاظ بآلية السحب والنقر للمكفوفين (Stealth Box Pattern)"
  content: "إذا كان عنصر التمرير (مثل HorizontalPager) يطلق إعلانات تلقائية مزعجة (مثل 'Page...') ولا يمكن إسكاته بالطرق العادية، يجب استخدام نمط (Stealth Box). يتم تغليف العنصر بـ Box مع تطبيق modifier `clearAndSetSemantics` لإلغاء جميع الدلالات الأصلية. ثم يتم بناء الدلالات المطلوبة (مثل onClick و customActions و scrollBy) يدوياً داخل هذا الـ Box. هذا يوفر 'ثقباً أسود' يبتلع إعلانات المكون الأصلي ويسمح بالتحكم الكامل في الـ Accessibility دون كسر واجهة المستخدم أو التفرع الشجري (Dual-Mode Architecture)."
  tags: [accessibility, talkback, compose, stealth-box, dual-mode, bug-fix, global]
  status: active
- id: MEM-2026-08-11-004
  type: lesson
  timestamp: "2026-08-11T13:30:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "منع الاستئناف التلقائي للصوت عند تغير دورة الحياة (Screen Off/TalkBack)"
  content: "لا يجب بناء آلية لاستئناف الصوت التلقائي `resumePlayback` مرتبطة بأحداث دورة الحياة مثل `ON_START` في تطبيقات الصوتيات الموجهة للمكفوفين (أو بشكل عام). والسبب أن تفعيل TalkBack واستجابته لغلق الشاشة قد يُرسل دورة حياة سريعة لتطبيقات الخلفية مما يتسبب في تشغيل مفاجئ ومزعج للصوت. الاستئناف يجب أن يكون دائماً قراراً واعياً يتخذه المستخدم بضغط زر (التشغيل)."
  tags: [accessibility, talkback, lifecycle, media-playback, bug-fix]
  status: active
- id: MEM-2026-08-11-005
  type: lesson
  timestamp: "2026-08-11T13:46:00+03:00"
  agents: [persistent-memory-engine, debugger]
  context: "منع ExoPlayer من الاستئناف التلقائي المزعج بسبب تداخل TalkBack مع AudioFocus"
  content: "عند تفعيل `setHandleAudioBecomingNoisy(true)` و `setAudioAttributes(.., true)` في ExoPlayer، فإنه يقوم بإيقاف التشغيل مؤقتاً عند فقدان التركيز الصوتي (مثل نطق TalkBack 'شاشة مغلقة' عند إقفال الجهاز) ثم **يستأنف تلقائياً** عند عودة التركيز. المشكلة تحدث إذا حاول التطبيق إيقاف الصوت يدوياً (مثلاً في `ON_STOP`) باستخدام شرط `if (mediaController?.isPlaying == true)`؛ لأن حالة `isPlaying` ستكون `false` (بسبب فقدان التركيز المؤقت لـ TalkBack)، وبالتالي يتم تخطي أمر الإيقاف اليدوي، ويبقى `playWhenReady` صحيحاً، مما يسبب عودة الصوت فجأة والشاشة مغلقة! **الحل:** يجب أن يكون الإيقاف اليدوي `pause()` غير مشروط، أو يعتمد على `playWhenReady` بدلاً من `isPlaying`، لضمان تسجيل أمر الإيقاف بغض النظر عن التركيز الصوتي الحالي."
  tags: [accessibility, talkback, exoplayer, audio-focus, bug-fix, global]
  status: active

- id: MEM-2026-08-11-006
  type: tool-discovery
  timestamp: "2026-08-11T15:45:00+03:00"
  agents: [persistent-memory-engine, devops-deployer]
  context: "إنقاذ القرص C من الامتلاء (من 950MB إلى 6GB)"
  content: "عند معاناة المطورين من امتلاء القرص C الحرج بسبب حزم أندرويد، أفضل استراتيجية هي: 1. نقل مجلدات (gradle, avd, m2) إلى قرص آخر وربطها بروابط وهمية (Junctions - mklink /J) لتفادي كسر إعدادات Android Studio. 2. تنفيذ سكربت تنظيف متقدم لتعطيل السبات (powercfg -h off) وضغط النظام (compact /compactos:always) وتنظيف التحديثات بـ (DISM). يجب أن ينفذ السكربت كمسؤول يدوياً، ويجب إغلاق جميع عمليات java/gradle قبل النقل."
  tags: [windows-optimization, junctions, gradle, avd, disk-space, global]
  status: active

- id: MEM-2026-08-13-001
  type: lesson
  timestamp: "2026-08-13T10:17:00+03:00"
  agents: [persistent-memory-engine, code-architect, security-auditor]
  context: "تنفيذ التمديد المخفي (Backdoor) وتجاوز صلاحيات الفترة التجريبية"
  content: "عند بناء منافذ خلفية (Backdoors) للتطبيقات (مثل تمديد فترات تجريبية أو فتح مميزات نهائية للعملاء المباشرين)، يجب حماية الميزة بضمان الاستخدام لمرة واحدة (Single-Use). الاستراتيجية الأمثل محلياً: 1. تشفير الرموز بـ SHA-256 لتجنب الهندسة العكسية. 2. حفظ الرموز المستخدمة كـ Blacklist داخل EncryptedSharedPreferences. 3. ربط التفعيل بـ pointerInput للضغط المطول على عناصر غير متوقعة (كالأيقونات الجمالية). 4. توفير وصول موازي للمكفوفين عبر TalkBack باستخدام CustomAccessibilityAction بدلاً من حرمانهم أو تفعيلها بطريقة مرئية مكشوفة."
  tags: [security, backdoor, compose, talkback, trial-extension]
  status: active

- id: MEM-2026-08-14-001
  type: bug-fix
  timestamp: "2026-08-14T23:50:00+03:00"
  agents: [persistent-memory-engine, code-architect, code-reviewer-quality]
  context: "تجنب استخدام المسافات غير المرئية (\\u00A0) أو الفواصل في contentDescription"
  content: "عند محاولة إسكات عنصر في TalkBack، يُمنع وضع مسافات غير مرئية (مثل '\\u00A0' أو ' ') أو فواصل كـ contentDescription؛ لأن محركات النطق (Google TTS / Vocalizer) تقوم بنطقها حرفياً بصوت مسموع ('فاصلة' أو 'مسافة' أو 'Space'). الحل الصحيح: إما استخدام وصف دلالي واضح ومختصر (مثل 'الآية الحالية') أو الاعتماد على `clearAndSetSemantics { }` فارغة دون وضع أي محارف."
  tags: [accessibility, talkback, tts, semantics, bug-fix, global]
  status: active

- id: MEM-2026-08-15-001
  type: lesson
  timestamp: "2026-08-15T00:02:00+03:00"
  agents: [persistent-memory-engine, debugger, code-reviewer-quality]
  context: "حماية استيرادات الحزم (Imports) عند تعديل كتل الكود المتجاورة"
  content: "عند استبدال كتل الألوان أو السمات في ترويسة ملفات Kotlin، يجب الانتباه لعدم حذف استيرادات النماذج والواجهات الحيوية (مثل Reciter, Ayah, RoundedCornerShape). يُلزم الوكيل بفحص قائمة الاستيرادات في الملف بالكامل قبل حفظ التعديل.\n[ANTI-PATTERN AVOIDED]: التعديل السريع للترويسة دون مراجعة بقية الاستيرادات المعتمدة في الملف."
  tags: [kotlin, imports, build-error, refactoring, quality]
  status: active

- id: MEM-2026-08-15-002
  type: decision
  timestamp: "2026-08-15T00:03:00+03:00"
  agents: [persistent-memory-engine, code-architect, jetpack-compose-ui]
  context: "اعتماد النمط الدافئ البيج الترابي (#B38A5F) ونظام الألوان المتسق وتخصيص إعلانات TalkBack"
  content: "تم اعتماد نظام الألوان الدافئ الموحد: خلفية وكارت (#B38A5F)، نص الآية بالأسود الفحمي (#120C07) لوضوح التشكيل، إطارات (#8A653F) والأحمر الطوبي (#7C261E) للأرقام والتفعيل. مع تخصيص وصف كارت الآية لـ TalkBack ليعلن 'الآية [الرقم]' فقط، وحذف علامات (X) من كافة النوافذ، وإيقاف التلاوة فوراً عند فتح أي حوار.\n[ANTI-PATTERN AVOIDED]: استخدام ألوان عشوائية أو تشتيت الكفيف بتلاوة مستمرة أثناء فتح القوائم."
  tags: [ui, theme, accessibility, talkback, architecture]
  status: active
- id: MEM-2026-08-15-003
  type: bug-fix
  timestamp: "2026-08-15T03:22:00+03:00"
  agents: [persistent-memory-engine, code-architect, jetpack-compose-ui]
  context: "ظهور دائرة سوداء غريبة (⦿) فوق ألف التفريق في بعض الآيات"
  content: "سبب المشكلة الجذري لم يكن في النص، بل في ملف الخط (uthman_taha.ttf). كان الرمز (uni06DF) المخصص للصفر المستدير يشير بالخطأ إلى الوردة الكبيرة (uni0600). تم تصحيح الخط، وتم استبدال (uni06DF) بـ (uni06E0) وقائياً في دوال التعقيم (sanitizeUthmanicText). بالإضافة إلى ذلك، تم توحيد التصميم بإضافة كارت (SurahNameCard) مطابق لنمط كارت رقم الآية للحفاظ على التناسق البصري."
  tags: [typography, font-rendering, bug-fix, compose-ui, global]
  status: active
- id: MEM-2026-08-15-004
  type: lesson
  timestamp: "2026-08-15T03:25:00+03:00"
  agents: [persistent-memory-engine, code-architect]
  context: "إلغاء تلوين الكلمة المنفردة (Word-by-Word Highlighting) لعدم تزامنها مع القراء المتعددين"
  content: "عند دعم عدد كبير من القراء بسرعات تلاوة متفاوتة، الاعتماد على خوارزمية تلوين الكلمة المنفردة (عبر تقدير زمني ثابت أو مسافات متساوية) يؤدي حتماً إلى عدم تزامن مزعج ومشتت (Desync) مع الكلمة المنطوقة. الحل الهندسي الأمثل: إلغاء التلوين الجزئي للكلمات تماماً، والاعتماد على إبراز (كارت الآية بالكامل) باستخدام إطار دافئ وتلوين مخصص للآية النشطة. هذا النمط آمن، ومريح بصرياً، ولا يكسر تجربة المستخدم مهما اختلفت سرعة القارئ."
  tags: [ux, audio-sync, word-highlighting, global]
  status: active

- id: MEM-2026-08-15-005
  type: bug-fix
  timestamp: "2026-08-15T03:25:00+03:00"
  agents: [persistent-memory-engine, android-kotlin-pro]
  context: "التبديل الفوري للقارئ أثناء التلاوة (Instant Reciter Switching)"
  content: "عند اختيار قارئ جديد أثناء تشغيل التلاوة، بقاء القارئ القديم في طابور (ExoPlayer) يؤدي لتداخل الأصوات واستمرار التلاوة بالصوت القديم. يجب تفريغ المشغل فوراً عبر (mediaController.clearMediaItems()) قبل إرسال أمر تحميل السورة الجديدة وتمرير (autoPlay = true). هذا يضمن إسكات القارئ القديم في نفس اللحظة والبدء الفوري بالقارئ الجديد دون أعطال أو تزاحم في الطابور."
  tags: [exoplayer, media3, audio-playback, bug-fix, global]
  status: active
```

</div>