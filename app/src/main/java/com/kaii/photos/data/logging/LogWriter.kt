package com.kaii.photos.data.logging

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

object LogWriter {
    private var logFile: File? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var channel: Channel<LogEntry>? = null
    private var consumerJob: Job? = null

    private enum class LogLevel {
        Debug,
        Error
    }

    private data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val message: String,
        val throwable: Throwable?
    )

    fun init(context: Context) {
        val logDir = context.getExternalFilesDir("logs") ?: return

        if (!logDir.exists()) logDir.mkdirs()

        logFile = File(logDir, "app-only-log.txt")
    }

    fun start() {
        if (logFile == null) return

        val channel = Channel<LogEntry>(capacity = Channel.UNLIMITED)
        this.channel = channel

        consumerJob = scope.launch {
            logFile!!.bufferedWriter().use { out ->
                for (entry in channel) {
                    out.appendLine("${entry.timestamp} ${entry.level}/${entry.tag}: ${entry.message}")
                    entry.throwable?.let { out.appendLine(it.stackTraceToString()) }
                    out.flush()
                }
            }
        }
    }

    fun d(tag: String?, message: String, throwable: Throwable? = null) = write(LogLevel.Debug, tag, message, throwable)
    fun e(tag: String?, message: String, throwable: Throwable? = null) = write(LogLevel.Error, tag, message, throwable)

    private fun write(level: LogLevel, tag: String?, message: String, throwable: Throwable? = null) {
        when (level) {
            LogLevel.Debug -> Log.d(tag, message, throwable)
            LogLevel.Error -> Log.e(tag, message, throwable)
        }

        channel?.trySend(LogEntry(System.currentTimeMillis(), level, tag ?: "com.kaii.photos", message, throwable))
    }

    fun stop() {
        scope.launch {
            channel?.close()
            consumerJob?.join()
            channel = null
            consumerJob = null
        }
    }

    fun getLogFile() = logFile
}