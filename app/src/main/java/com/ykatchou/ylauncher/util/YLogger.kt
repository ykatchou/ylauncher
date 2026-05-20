package com.ykatchou.ylauncher.util

import android.util.Log

/**
 * Centralized logger. All calls go through here so a single-line swap to Crashlytics
 * (or any other backend) is possible in the future.
 */
object YLogger {
    private const val TAG = "yLauncher"

    fun e(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) Log.e(TAG, "[$tag] $msg", t) else Log.e(TAG, "[$tag] $msg")
    }

    fun w(tag: String, msg: String, t: Throwable? = null) {
        if (t != null) Log.w(TAG, "[$tag] $msg", t) else Log.w(TAG, "[$tag] $msg")
    }

    fun d(tag: String, msg: String) {
        Log.d(TAG, "[$tag] $msg")
    }
}
