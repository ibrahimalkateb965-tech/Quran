# Memory Store - سجل الذكريات والدروس المستفادة

يحتفظ هذا الملف بالدروس المستفادة، والقرارات التقنية لتفادي تكرار الأخطاء ولتحسين الأداء.

## [lesson] حل مشكلة 401 عند ربط Claude Code مع Kimi API
- **التاريخ:** 2026-08-04
- **الوكلاء المساهمون:** `[agent-optimizer]`, `[code-architect]`
- **المشكلة:** ظهور خطأ فشل المصادقة (401 Invalid Authentication) عند إعطاء مفتاح Kimi لسطر الأوامر `Claude Code CLI`.
- **السبب:** أداة Claude مبرمجة حصرياً للاتصال بخوادم Anthropic (وبصيغتها الخاصة)، بينما Kimi يعتمد صيغة `OpenAI Compatible`.
- **الحل المعتمد:**
  1. تشغيل `LiteLLM` في الخلفية ليعمل كمترجم محلي (Proxy) يحول الطلبات من صيغة Anthropic إلى OpenAI Compatible ويوجهها لـ Moonshot.
  2. توجيه المتغير `ANTHROPIC_BASE_URL` ليقرأ من الخادم المحلي (`http://localhost:4000`).
  3. إنهاء عملية `LiteLLM` آلياً فور خروج المستخدم من أداة Claude.
