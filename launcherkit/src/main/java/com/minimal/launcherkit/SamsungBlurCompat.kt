package com.minimal.launcherkit

import android.os.Build
import android.util.Log
import android.view.Window
import android.view.WindowManager

/** Best-effort Samsung blur for pre-Android-12 One UI builds. No Samsung classes are linked. */
internal object SamsungBlurCompat {
    private const val TAG = "SamsungBlurCompat"
    private const val SAMSUNG_BLUR_EXTENSION_FLAG = 0x10

    fun setEnabled(window: Window, enabled: Boolean): Boolean {
        if (!isSamsung() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return false

        val params = window.attributes
        return try {
            if (enabled) {
                val add = findMethod(params.javaClass, "semAddExtensionFlags") ?: return false
                add.invoke(params, SAMSUNG_BLUR_EXTENSION_FLAG)
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                window.attributes = params
                true
            } else {
                val clear = findMethod(params.javaClass, "semClearExtensionFlags")
                if (clear != null) {
                    clear.invoke(params, SAMSUNG_BLUR_EXTENSION_FLAG)
                } else {
                    val flags = findField(params.javaClass, "semExtensionFlags") ?: run {
                        params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                        window.attributes = params
                        return false
                    }
                    flags.setInt(params, flags.getInt(params) and SAMSUNG_BLUR_EXTENSION_FLAG.inv())
                }
                params.flags = params.flags and WindowManager.LayoutParams.FLAG_BLUR_BEHIND.inv()
                window.attributes = params
                true
            }
        } catch (failure: Throwable) {
            Log.w(TAG, "Samsung pre-Android-12 blur unavailable; using launcher fallback", failure)
            false
        }
    }

    private fun isSamsung(): Boolean =
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
            Build.BRAND.equals("samsung", ignoreCase = true)

    private fun findMethod(type: Class<*>, name: String) = runCatching {
        type.getMethod(name, Int::class.javaPrimitiveType!!).apply { isAccessible = true }
    }.recoverCatching {
        type.getDeclaredMethod(name, Int::class.javaPrimitiveType!!).apply { isAccessible = true }
    }.getOrNull()

    private fun findField(type: Class<*>, name: String) = runCatching {
        type.getField(name).apply { isAccessible = true }
    }.recoverCatching {
        type.getDeclaredField(name).apply { isAccessible = true }
    }.getOrNull()
}
