package com.kaii.photos.data.logging

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.kaii.photos.R
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.concurrent.thread

class LogManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val logDir = context.getExternalFilesDir("logs")
    private val logFile = File(logDir, "current-log.txt")
    private val previousLogFile = File(logDir, "previous-log.txt")

    private var process: Process? = null
    private var readerThread: Thread? = null

    fun startRecording() {
        stopRecording()

        if (logDir == null) {
            Log.d(LogManager::class.qualifiedName, "Failed to record logs, logDir does not exist")

            return
        }

        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        if (logFile.exists()) {
            previousLogFile.delete()
            logFile.renameTo(previousLogFile)
        } else {
            logFile.createNewFile()
        }

        Log.d(LogManager::class.qualifiedName, "Initializing logging service for files $logFile $previousLogFile ${logFile.exists()} ${previousLogFile.exists()}")

        try {
            process = ProcessBuilder(
                "logcat",
                "-v", "threadtime",
                "-f", logFile.absolutePath,
                "--pid", android.os.Process.myPid().toString()
            ).redirectErrorStream(true).start()

            readerThread = thread(name = "lavender-photos-logcat-writer") {
                try {
                    logFile.bufferedWriter().use { out ->
                        process!!.inputStream.bufferedReader().forEachLine { line ->
                            out.appendLine(line)
                            out.flush() // so process being killed doesn't lose the tail
                        }
                    }
                } catch (e: Throwable) {
                    Log.e(LogManager::class.qualifiedName, "Error while logging", e)
                    e.printStackTrace()
                }
            }
        } catch (e: Throwable) {
            Log.e(LogManager::class.qualifiedName, "Failed to start logging service", e)
        }
    }

    fun stopRecording() {
        process?.destroy()
        process = null
        readerThread?.interrupt()
        readerThread = null
    }

    fun getShareIntent(): Intent? {
        val files = listOf(logFile, previousLogFile, CrashHandler.getLogFile(context))
        val fileUris = ArrayList<Uri>(2)

        files.forEach { file ->
            if (!file.exists()) return@forEach

            fileUris.add(
                FileProvider.getUriForFile(
                    context,
                    LAVENDER_FILE_PROVIDER_AUTHORITY,
                    file
                )
            )
        }

        if (fileUris.isEmpty()) return null

        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            type = "text/plain"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
        }

        return Intent.createChooser(
            intent,
            context.resources.getString(R.string.logs_share)
        )
    }
}
