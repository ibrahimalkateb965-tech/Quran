package com.aistudio.quranblind.network

import android.util.Log

actual object PlatformLogger {
    actual fun warn(tag: String, message: String) {
        Log.w(tag, message)
    }
}
