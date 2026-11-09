package com.aistudio.quranblind.data

import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile

class BundleQuranJsonSource(
    private val bundle: NSBundle = NSBundle.mainBundle,
) : QuranJsonSource {
    override fun readQuranJson(): String {
        val path = bundle.pathForResource(RESOURCE_NAME, ofType = RESOURCE_TYPE)
            ?: error("$RESOURCE_NAME.$RESOURCE_TYPE is not in the app bundle; check iosApp/project.yml")
        val data = NSData.dataWithContentsOfFile(path)
            ?: error("$RESOURCE_NAME.$RESOURCE_TYPE could not be read from $path")
        return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
            ?: error("$RESOURCE_NAME.$RESOURCE_TYPE is not valid UTF-8")
    }

    private companion object {
        const val RESOURCE_NAME = "quran_uthmani_tanzil"
        const val RESOURCE_TYPE = "json"
    }
}
