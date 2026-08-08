<div dir="rtl">

# 🛠️ دليل الأدوات المحلية للذكاء الاصطناعي والملاءمة مع MCP & Maestro

يمثل هذا المستند المرجع المحلي لتهيئة وتشغيل أدوات الذكاء الاصطناعي المتقدمة داخل مشروع **Blind App / معين**.

---

## 1. ⚙️ تهيئة خوادم Model Context Protocol (MCP)
توجد إعدادات الخوادم المحلية في الملف [`mcp_config.json`](file:///f:/AI%20PROJECTS/Blind%20App/mcp_config.json)، وتشتمل على:
- **Filesystem MCP Server:** يتيح للوكلاء قراءة وإدارة ملفات المشروع بأعلى سرعة وأمان.
- **Playwright MCP Server:** يتيح لوكلاء الـ Web فحص واختبار الواجهات برمجياً عبر متصفح تلقائي.
- **GitHub MCP Server:** يربط الوكلاء بحسابك في GitHub لإدارة الـ Pull Requests والـ Issues.

### طريقة التشغيل والتفعيل:
يمكن استدعاؤها مباشرة في الـ IDE أو تشغيل أي سيرفر بـ `npx` مثل:
```bash
npx -y @modelcontextprotocol/server-filesystem "f:/AI PROJECTS/Blind App"
```

---

## 2. 📱 أتمتة اختبارات الواجهة مع Maestro
توجد سيناريوهات فحص واجهة تطبيق أندرويد في المجلد [`.maestro/`](file:///f:/AI%20PROJECTS/Blind%20App/.maestro/).

### الملف الحالي:
- [`quran_app_flow.yaml`](file:///f:/AI%20PROJECTS/Blind%20App/.maestro/quran_app_flow.yaml): فحص فتح التطبيق، تشغيل الصوت، والسحب بين الآيات.

### أمر التشغيل السريع:
```bash
maestro test .maestro/quran_app_flow.yaml
```

---

## 3. 🕵️‍♂️ آلية عمل `[github-talent-scout]`
عند الحاجة إلى مكتبة خارجية جديدة أو أداة أتمتة إضافية، يقوم الوكيل تلقائياً بـ:
1. البحث في GitHub عن المستودع القياسي الأكثر نشاطاً.
2. تقييم ترخيص المستودع ومعايير الأمان.
3. إنشاء ملف التوجيه البرمجي وضمه تلقائياً لبيئة المشروع.

</div>
