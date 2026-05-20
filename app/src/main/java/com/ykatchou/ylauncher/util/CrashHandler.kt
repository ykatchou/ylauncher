package com.ykatchou.ylauncher.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_LOG_BYTES = 64 * 1024 // 64 KB

/**
 * Writes uncaught exceptions to <filesDir>/crash_log.txt before delegating
 * to the default handler (so the OS crash dialog / process kill still fires).
 *
 * Retrieve on device (debug builds):
 *   adb shell "run-as com.ykatchou.ylauncher cat files/crash_log.txt"
 */
class CrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            val file = File(context.filesDir, "crash_log.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = buildString {
                append("=== $timestamp [${t.name}] ===\n")
                append(e.stackTraceToString())
                append("\n")
            }
            file.appendText(entry)
            trimLog(file)
        } catch (_: Exception) {
            // Never let the crash handler itself crash
        }
        defaultHandler?.uncaughtException(t, e)
    }

    private fun trimLog(file: File) {
        if (file.length() <= MAX_LOG_BYTES) return
        val content = file.readText()
        // Keep only the last MAX_LOG_BYTES characters
        file.writeText(content.takeLast(MAX_LOG_BYTES))
    }
}
