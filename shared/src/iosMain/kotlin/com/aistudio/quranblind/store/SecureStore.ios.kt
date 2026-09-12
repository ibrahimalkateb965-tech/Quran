package com.aistudio.quranblind.store

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSLog
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class IosSecureStore(private val service: String) : SecureStore {

    override fun getString(key: String): String? = memScoped {
        val serviceCf = CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
        val accountCf = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceCf)
        CFDictionarySetValue(query, kSecAttrAccount, accountCf)
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        CFRelease(query)
        CFRelease(serviceCf)
        CFRelease(accountCf)
        if (status == errSecItemNotFound) return@memScoped null
        if (status != errSecSuccess) {
            NSLog("SecureStore: getString failed with status %d", status)
            return@memScoped null
        }
        val cfData = result.value as CFDataRef?
        if (cfData == null) return@memScoped null
        val length = CFDataGetLength(cfData).toInt()
        if (length == 0) {
            CFRelease(cfData)
            return@memScoped ""
        }
        val bytePtr = CFDataGetBytePtr(cfData)
        if (bytePtr == null) {
            CFRelease(cfData)
            return@memScoped null
        }
        val bytes = bytePtr.readBytes(length)
        CFRelease(cfData)
        bytes.decodeToString()
    }

    override fun putString(key: String, value: String) = memScoped {
        val bytes = value.encodeToByteArray()
        val dataCf = bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        }
        val serviceCf = CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
        val accountCf = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
        val accessibleCf = kSecAttrAccessibleAfterFirstUnlock
        val addQuery = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(addQuery, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(addQuery, kSecAttrService, serviceCf)
        CFDictionarySetValue(addQuery, kSecAttrAccount, accountCf)
        CFDictionarySetValue(addQuery, kSecAttrAccessible, accessibleCf)
        CFDictionarySetValue(addQuery, kSecValueData, dataCf)
        val addStatus = SecItemAdd(addQuery, null)
        CFRelease(addQuery)
        if (addStatus == errSecDuplicateItem) {
            val matchQuery = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(matchQuery, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(matchQuery, kSecAttrService, serviceCf)
            CFDictionarySetValue(matchQuery, kSecAttrAccount, accountCf)
            val updateAttrs = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(updateAttrs, kSecValueData, dataCf)
            val updateStatus = SecItemUpdate(matchQuery, updateAttrs)
            CFRelease(matchQuery)
            CFRelease(updateAttrs)
            if (updateStatus != errSecSuccess) {
                NSLog("SecureStore: SecItemUpdate failed with status %d", updateStatus)
            }
        } else if (addStatus != errSecSuccess) {
            NSLog("SecureStore: SecItemAdd failed with status %d", addStatus)
        }
        CFRelease(dataCf)
        CFRelease(serviceCf)
        CFRelease(accountCf)
    }

    override fun getInt(key: String, default: Int): Int {
        return getString(key)?.toIntOrNull() ?: default
    }

    override fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }

    override fun remove(key: String) = memScoped {
        val serviceCf = CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
        val accountCf = CFStringCreateWithCString(null, key, kCFStringEncodingUTF8)
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceCf)
        CFDictionarySetValue(query, kSecAttrAccount, accountCf)
        val status = SecItemDelete(query)
        CFRelease(query)
        CFRelease(serviceCf)
        CFRelease(accountCf)
        if (status != errSecSuccess && status != errSecItemNotFound) {
            NSLog("SecureStore: SecItemDelete failed with status %d", status)
        }
    }

    override fun clear() = memScoped {
        val serviceCf = CFStringCreateWithCString(null, service, kCFStringEncodingUTF8)
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceCf)
        val status = SecItemDelete(query)
        CFRelease(query)
        CFRelease(serviceCf)
        if (status != errSecSuccess && status != errSecItemNotFound) {
            NSLog("SecureStore: clear failed with status %d", status)
        }
    }
}

actual fun createSecureStore(name: String): SecureStore = IosSecureStore(name)
