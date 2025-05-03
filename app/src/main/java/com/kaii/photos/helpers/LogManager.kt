package com.kaii.photos.helpers

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "LOG_MANAGER"

class LogManager(
	context: Context
) {
	private val previousSuffix = "-previous"
	private val fileType = ".txt"

	private val logPath = "${context.appStorageDir}/log"

	val previousLogPath = logPath + previousSuffix + fileType
	val currentLogPath = logPath + fileType

	fun startRecording() {
		try {
			val currentLog = File(currentLogPath)
			val previousLog = File(previousLogPath)

			if (currentLog.exists()) {
				if (previousLog.exists()) previousLog.delete()
				currentLog.copyTo(previousLog)
			}

		    currentLog.delete()
		    Runtime.getRuntime().exec("logcat -c")
		    Runtime.getRuntime().exec("logcat -f $currentLogPath")
		} catch (e: Throwable) {
		    Log.e(TAG, e.toString())
		    e.printStackTrace()
		}
	}
}
