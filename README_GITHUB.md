# Haron TV - Build APK from phone

هذا المشروع جاهز للبناء السحابي عبر GitHub Actions بدون Android Studio على الهاتف.

1. أنشئ مستودع GitHub جديدًا.
2. ارفع محتويات هذا المجلد إلى المستودع (وليس المجلد الأب).
3. افتح تبويب Actions.
4. اختر **Build Haron TV APK** ثم **Run workflow**.
5. بعد نجاح البناء افتح نتيجة التشغيل، ثم قسم **Artifacts**، ونزّل **HaronTV-debug-apk**.
6. فك الضغط وثبّت ملف APK على هاتف Android.

ملاحظة: التطبيق يعتمد على رابط قائمة M3U الموجود في MainActivity.java، ويحتاج اتصال إنترنت لتحديث القنوات.
