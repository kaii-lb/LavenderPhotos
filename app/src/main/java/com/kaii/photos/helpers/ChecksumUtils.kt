package com.kaii.photos.helpers

import android.os.CancellationSignal
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "com.kaii.photos.helpers.ChecksumUtils"

@OptIn(ExperimentalStdlibApi::class)
fun calculateSha1Checksum(file: File): String {
    if (!file.exists()) {
        Log.d(TAG, "Checksum for ${file.absolutePath} failed, file does not exist!")
        return ""
    }

    val digest = MessageDigest.getInstance("SHA-1")

    val buffer = ByteArray(1024 * 16)
    var bytesRead: Int

    file.inputStream().buffered().use { fis ->
        while (fis.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }

    return digest.digest().toHexString()
}

suspend fun calculateSha1Checksum(
    files: List<File>,
    cancellationSignal: CancellationSignal
): Map<String, String> = coroutineScope {
    var results = ConcurrentHashMap<String, String>()
    val jobs = mutableListOf<Job>()

    files.forEach { file ->
        jobs.add(
            launch(Dispatchers.IO) {
                ensureActive()
                try {
                    val checksum = calculateSha1Checksum(file)
                    results[file.absolutePath] = checksum
                } catch (e: Exception) {
                    Log.d(TAG, "Error calculating checksum for ${file.absolutePath}: ${e.message}")
                }
            }
        )
    }

    jobs.chunked(32).forEach {
        it.joinAll()
    }
    // jobs.joinAll()

    cancellationSignal.setOnCancelListener {
        jobs.forEach {
            it.cancel()
            it.cancelChildren()
        }
        results = ConcurrentHashMap<String, String>()
    }

    return@coroutineScope results
}