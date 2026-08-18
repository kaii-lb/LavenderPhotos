package com.kaii.photos.data.logging

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

class CrashHandler(
    context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    companion object {
        fun getLogFile(context: Context): File {
            val logDir = context.getExternalFilesDir("logs")
            return File(logDir, "crash-log.txt")
        }
    }

    private val crashFile = getLogFile(context)

    init {
        // remove crash files older than a day
        if (crashFile.exists() && crashFile.lastModified() < Clock.System.now().minus(1.days).toEpochMilliseconds()) {
            crashFile.delete()
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            if (crashFile.exists()) crashFile.delete()
            crashFile.bufferedWriter().use { writer ->
                writer.append("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} ")
                writer.append("FATAL/${thread.name}: ")
                writer.append(Log.getStackTraceString(throwable))
                writer.flush()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}