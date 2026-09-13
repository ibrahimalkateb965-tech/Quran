package com.aistudio.quranblind.network

import platform.Foundation.NSLog

actual object PlatformLogger {
    actual fun warn(tag: String, message: String) {
        NSLog("%@: %@", tag, message)
    }
}
