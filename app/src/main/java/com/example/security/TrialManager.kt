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
     * يُرجع true إذا انتهت الفترة التجريبية (7 أيام).
     * يحاول الحصول على الوقت من SNTP أولاً، وإن فشل يستخدم elapsedRealtime مع كشف التراجع.
     */
    suspend fun isTrialExpired(): Boolean = withContext(Dispatchers.IO) {
        val installWallTime = prefs.getLong(KEY_INSTALL_WALL_TIME, 0L)
        val installElapsed = prefs.getLong(KEY_INSTALL_ELAPSED_REALTIME, 0L)

        if (installWallTime == 0L || installElapsed == 0L) {
            recordFirstInstall()
            return@withContext false
        }

        val nowWallTime = resolveTrustedWallTime(installWallTime, installElapsed)
        if (nowWallTime == null) {
            // لا يمكن تحديد الوقت الموثوق — نفترض السماح بالاستخدام تجنباً لمنع المستخدم عن الخطأ.
            // ملاحظة: هذا قرار تجربة مستخدم؛ لمنع التلاعب يمكن إرجاع true هنا.
            Log.w(TAG, "تعذّر تحديد الوقت الموثوق؛ يتم السماح بالاستخدام مؤقتاً")
            return@withContext false
        }

        updateLastKnownWallTime(nowWallTime)

        val expiryTime = installWallTime + TRIAL_DURATION_MILLIS
        nowWallTime > expiryTime
    }

    /**
     * يُرجع عدد الأيام المتبقية (0 إذا انتهت).
     */
    suspend fun getDaysRemaining(): Int = withContext(Dispatchers.IO) {
        val installWallTime = prefs.getLong(KEY_INSTALL_WALL_TIME, 0L)
        val installElapsed = prefs.getLong(KEY_INSTALL_ELAPSED_REALTIME, 0L)

        if (installWallTime == 0L || installElapsed == 0L) {
            return@withContext 7
        }

        val nowWallTime = resolveTrustedWallTime(installWallTime, installElapsed) ?: return@withContext 7
        val remaining = TRIAL_DURATION_MILLIS - (nowWallTime - installWallTime)
        val days = (remaining / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        days
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

        // المحاولة الثانية: تقدير بالوقت المنقضي منذ التثبيت.
        val elapsedNow = SystemClock.elapsedRealtime()
        val elapsedSinceInstall = elapsedNow - installElapsed
        if (elapsedSinceInstall < 0) {
            // الجهاز أُعيد تشغيله أو elapsedRealtime تراجع (لا يحدث عادة) — علامة تلاعب.
            Log.w(TAG, "elapsedRealtime أقل من قيمة التثبيت — احتمال تلاعب")
            return null
        }

        val estimatedWallTime = installWallTime + elapsedSinceInstall
        val systemWallTime = System.currentTimeMillis()

        // كشف تراجع تاريخ الجهاز إلى الوراء بأكثر من 5 دقائق.
        if (systemWallTime < estimatedWallTime - ROLLBACK_TOLERANCE_MILLIS) {
            Log.w(TAG, "تم اكتشاف تراجع في وقت الجهاز")
            return null
        }

        // نستخدم التقدير المحسوب من elapsedRealtime (أكثر أماناً من systemWallTime وحده).
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
            Log.e(TAG, "فشل إنشاء EncryptedSharedPreferences؛ يتم الاستعاضة بالتفضيلات العادية", e)
            context.getSharedPreferences(PREFS_FILE_NAME_FALLBACK, Context.MODE_PRIVATE)
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

        private const val TRIAL_DURATION_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val ROLLBACK_TOLERANCE_MILLIS = 5L * 60 * 1000 // 5 دقائق

        @Volatile
        private var INSTANCE: TrialManager? = null

        fun getInstance(context: Context): TrialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TrialManager(context).also { INSTANCE = it }
            }
        }
    }
}
