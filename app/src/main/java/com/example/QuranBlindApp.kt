package com.example

import android.app.Application
import com.aistudio.quranblind.store.SecureStoreAndroid
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuranBlindApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureStoreAndroid.init(this)
    }
}
