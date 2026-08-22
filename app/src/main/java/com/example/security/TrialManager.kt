package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.util.SntpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * مدير آمن للفترة التجريبية.
 *
 * يعتمد على:
 * 1. EncryptedSharedPreferences لمنع التلاعب بالتواريخ محلياً.
 * 2. SNTP (time.google.com) للحصول على وقت موثوق من الشبكة.
 * 3. SystemClock.elapsedRealtime() للكشف عن إعادة ضبط وقت الجهاز إلى الوراء.
 */
class TrialManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createSecurePrefs(appContext)

    init {
        if (!prefs.contains(KEY_INSTALL_WALL_TIME)) {
            recordFirstInstall()
        }
    }

    /**
     * يُرجع true إذا انتهت الفترة التجريبية (30 يوماً).
     * يحاول الحصول على الوقت من SNTP أولاً، وإن فشل يستخدم elapsedRealtime مع كشف التراجع.
     */
    suspend fun isTrialExpired(): Boolean = withContext(Dispatchers.IO) {
        if (prefs.getBoolean(KEY_IS_PERMANENTLY_UNLOCKED, false)) {
            return@withContext false
        }

        val installWallTime = prefs.getLong(KEY_INSTALL_WALL_TIME, 0L)
        val installElapsed = prefs.getLong(KEY_INSTALL_ELAPSED_REALTIME, 0L)

        if (installWallTime == 0L || installElapsed == 0L) {
            recordFirstInstall()
            return@withContext false
        }

        val nowWallTime = resolveTrustedWallTime(installWallTime, installElapsed)
        if (nowWallTime == null) {
            Log.e(TAG, "تنبيه أمني خطير: تعذّر تحديد الوقت الموثوق أو تم رصد تلاعب صريح. سيتم إغلاق التطبيق (Fail-Closed).")
            return@withContext true
        }

        updateLastKnownWallTime(nowWallTime)

        val expiryTime = installWallTime + TRIAL_DURATION_MILLIS
        nowWallTime > expiryTime
    }

    /**
     * يُرجع عدد الأيام المتبقية (0 إذا انتهت).
     */
    suspend fun getDaysRemaining(): Int = withContext(Dispatchers.IO) {
        if (prefs.getBoolean(KEY_IS_PERMANENTLY_UNLOCKED, false)) {
            return@withContext Int.MAX_VALUE
        }

        val installWallTime = prefs.getLong(KEY_INSTALL_WALL_TIME, 0L)
        val installElapsed = prefs.getLong(KEY_INSTALL_ELAPSED_REALTIME, 0L)

        if (installWallTime == 0L || installElapsed == 0L) {
            return@withContext 30
        }

        val nowWallTime = resolveTrustedWallTime(installWallTime, installElapsed) ?: return@withContext 0
        val remaining = TRIAL_DURATION_MILLIS - (nowWallTime - installWallTime)
        val days = (remaining / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        days
    }

    /**
     * يتحقق من الرمز السري ويقوم بالتمديد إذا كان صحيحاً وغير مستخدم من قبل.
     */
    fun submitUnlockPin(pin: String): Boolean {
        val usedPins = prefs.getStringSet(KEY_USED_PINS, emptySet()) ?: emptySet()
        if (usedPins.contains(pin)) {
            Log.w(TAG, "هذا الرمز تم استخدامه مسبقاً.")
            return false
        }

        val hashedPin = hashPin(pin)

        return when {
            VALID_PERMANENT_PINS.contains(hashedPin) -> {
                unlockPermanently()
                prefs.edit { putStringSet(KEY_USED_PINS, usedPins + pin) }
                true
            }
            VALID_EXTENSION_PINS.contains(hashedPin) -> {
                extendTrial(7)
                prefs.edit { putStringSet(KEY_USED_PINS, usedPins + pin) }
                true
            }
            else -> false
        }
    }

    private fun extendTrial(daysToAdd: Int) {
        val currentInstallWallTime = prefs.getLong(KEY_INSTALL_WALL_TIME, System.currentTimeMillis())
        val addedMillis = daysToAdd * 24 * 60 * 60 * 1000L
        // تحريك وقت التثبيت للأمام لإعطاء أيام إضافية
        prefs.edit { putLong(KEY_INSTALL_WALL_TIME, currentInstallWallTime + addedMillis) }
        Log.i(TAG, "تم تمديد الفترة التجريبية بمقدار $daysToAdd أيام.")
    }

    private fun unlockPermanently() {
        prefs.edit { putBoolean(KEY_IS_PERMANENTLY_UNLOCKED, true) }
        Log.i(TAG, "تم فتح التطبيق بالكامل بشكل دائم.")
    }

    /**
     * يحاول الحصول على وقت جدار موثوق.
     * إذا نجح SNTP يُرجع قيمته.
     * إذا فشل، يُقدّر الوقت باستخدام elapsedRealtime ويكشف تراجع تاريخ الجهاز.
     */
    private suspend fun resolveTrustedWallTime(
        installWallTime: Long,
        installElapsed: Long
    ): Long? {
        // المحاولة الأولى: SNTP
        val sntpTime = SntpClient.requestTime()
        if (sntpTime != null) {
            prefs.edit { putLong(KEY_FIRST_SNTP_TIME, sntpTime) }
            return sntpTime
        }

        // المحاولة الثانية: الكشف عن إعادة التشغيل وتتبع الوقت الجداري
        val elapsedNow = SystemClock.elapsedRealtime()
        val lastKnownElapsed = prefs.getLong(KEY_LAST_KNOWN_ELAPSED_REALTIME, installElapsed)
        val lastKnownWallTime = prefs.getLong(KEY_LAST_KNOWN_WALL_TIME, installWallTime)
        val systemWallTime = System.currentTimeMillis()

        if (elapsedNow < lastKnownElapsed) {
            // الجهاز أُعيد تشغيله (Reboot) — قيمة elapsedRealtime عادت للصفر.
            Log.i(TAG, "تم رصد إعادة تشغيل للجهاز. سيتم الاعتماد على آخر وقت جداري موثوق.")
            // نتحقق من أن تاريخ الجهاز لم يتم التلاعب به وإرجاعه للخلف أثناء فترة انقطاع الإنترنت.
            if (systemWallTime < lastKnownWallTime - ROLLBACK_TOLERANCE_MILLIS) {
                Log.w(TAG, "تم اكتشاف تراجع في وقت الجهاز بعد إعادة التشغيل.")
                return null
            }
            // بما أننا فقدنا التتبع المستمر، نثق بوقت النظام الحالي طالما أنه لا يقل عن آخر وقت موثوق.
            return systemWallTime
        }

        // لم يحدث Reboot منذ آخر فحص
        val elapsedSinceLastCheck = elapsedNow - lastKnownElapsed
        val estimatedWallTime = lastKnownWallTime + elapsedSinceLastCheck

        // كشف تراجع تاريخ الجهاز إلى الوراء بأكثر من التفاوت المسموح.
        if (systemWallTime < estimatedWallTime - ROLLBACK_TOLERANCE_MILLIS) {
            Log.w(TAG, "تم اكتشاف تراجع صريح في وقت الجهاز.")
            return null
        }

        return estimatedWallTime
    }

    private fun recordFirstInstall() {
        val wallTime = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime()
        prefs.edit {
            putLong(KEY_INSTALL_WALL_TIME, wallTime)
            putLong(KEY_INSTALL_ELAPSED_REALTIME, elapsed)
            putLong(KEY_LAST_KNOWN_WALL_TIME, wallTime)
            putLong(KEY_LAST_KNOWN_ELAPSED_REALTIME, elapsed)
        }
        Log.i(TAG, "تم تسجيل وقت التثبيت الأول: wallTime=$wallTime elapsed=$elapsed")
    }

    private fun updateLastKnownWallTime(wallTime: Long) {
        prefs.edit {
            putLong(KEY_LAST_KNOWN_WALL_TIME, wallTime)
            putLong(KEY_LAST_KNOWN_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
        }
    }

    private fun createSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "فشل إنشاء EncryptedSharedPreferences؛ مسح التفضيلات الفاسدة وإعادة المحاولة", e)
            try {
                context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE).edit().clear().apply()
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e2: Exception) {
                Log.e(TAG, "فشل التهيئة الآمنة بالكامل. النظام سيمنع الدخول (Fail-Closed).", e2)
                throw IllegalStateException("Critical Security Error: Cannot initialize secure storage.", e2)
            }
        }
    }

    companion object {
        private const val TAG = "TrialManager"
        private const val PREFS_FILE_NAME = "mueen_trial_secure_prefs"
        private const val PREFS_FILE_NAME_FALLBACK = "mueen_trial_prefs"

        private const val KEY_INSTALL_WALL_TIME = "install_wall_time_millis"
        private const val KEY_INSTALL_ELAPSED_REALTIME = "install_elapsed_realtime_millis"
        private const val KEY_FIRST_SNTP_TIME = "first_sntp_time_millis"
        private const val KEY_LAST_KNOWN_WALL_TIME = "last_known_wall_time_millis"
        private const val KEY_LAST_KNOWN_ELAPSED_REALTIME = "last_known_elapsed_realtime_millis"
        private const val KEY_IS_PERMANENTLY_UNLOCKED = "is_permanently_unlocked"
        private const val KEY_USED_PINS = "used_pins_set"

        private const val TRIAL_DURATION_MILLIS = 30L * 24 * 60 * 60 * 1000
        private const val ROLLBACK_TOLERANCE_MILLIS = 5L * 60 * 1000 // 5 دقائق

        private const val PIN_SALT = "mueen_quran_secure_salt_2026_"

        private fun hashPin(pin: String): String {
            val saltedPin = PIN_SALT + pin.trim()
            val bytes = MessageDigest.getInstance("SHA-256").digest(saltedPin.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        val VALID_EXTENSION_PINS = setOf("ed517fee81146b87d0e913e7e44e914aca65911c3ec3e9421f975b4a9451e21b")
        val VALID_PERMANENT_PINS = setOf("bc85a02e0ec24a6c59fb58bba9698d012c7fc8b241c8e925ce048bcd22600ef0")

        @Volatile
        private var INSTANCE: TrialManager? = null

        fun getInstance(context: Context): TrialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrialManager(context).also { INSTANCE = it }
            }
        }
    }
}
