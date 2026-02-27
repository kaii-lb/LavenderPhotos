package com.kaii.photos.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import com.kaii.lavender.snackbars.LavenderSnackbarController
import com.kaii.lavender.snackbars.LavenderSnackbarEvents
import com.kaii.photos.BuildConfig
import com.kaii.photos.R
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

private const val TAG = "com.kaii.photos.helpers.Updater"

class Updater(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    enum class State {
        Checking,
        Failed,
        Succeeded
    }

    private val currentVersionCode = BuildConfig.VERSION_CODE

    private var githubResponseBody: MutableState<JSONObject?> = mutableStateOf(null)

    val githubVersionName = derivedStateOf {
        if (githubResponseBody.value == null) "" else githubResponseBody.value!!["tag_name"].toString()
    }

    private val githubVersionCode = derivedStateOf {
        if (githubVersionName.value == "") 0
        else githubVersionName.value
            .replace(".", "")
            .removeSuffix("-beta")
            .removeSuffix("-beta-hotfix")
            .removePrefix("v")
            .toInt()
    }

    private val updateFile by derivedStateOf {
        val file = File(context.appStorageDir, "photos_signed_release_${githubVersionName.value}.apk")
        file.parentFile?.mkdirs()
        file
    }

    /** check for updates and return whether there is one */
    val hasUpdates = derivedStateOf {
        Log.d(TAG, "Current version: $currentVersionCode Github available version: ${githubVersionCode.value}")
        githubVersionCode.value > currentVersionCode
    }

    init {
        updateFile.parentFile
            ?.listFiles()
            ?.filter { file ->
                file.name.endsWith(".apk") && file.name != updateFile.name
            }
            ?.forEach { file ->
                file.delete()
            }
    }

    fun refresh(onRefresh: (state: State) -> Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            async {
                onRefresh(State.Checking)
                val url = "https://api.github.com/repos/kaii-lb/LavenderPhotos/releases/latest"

                val body = try {
                    Fuel.get(url).responseJson().third.fold(
                        success = { result ->
                            result.obj()
                        },

                        failure = { error ->
                            Log.e(TAG, error.message.toString())
                            error.printStackTrace()

                            onRefresh(State.Failed)
                            return@async
                        }
                    )
                } catch (e: Throwable) {
                    Log.e(TAG, e.toString())
                    e.printStackTrace()

                    onRefresh(State.Failed)
                    return@async
                }

                githubResponseBody.value = body
                Log.d(TAG, "body of https response is $body")
                Log.d(TAG, "github version name is ${githubVersionName.value}")
                Log.d(TAG, "github version code is ${githubVersionCode.value}")
                Log.d(TAG, "app has updates? ${hasUpdates.value}")

                onRefresh(State.Succeeded)
            }.await()
        }
    }

    /** starts downloading update and returns whether download was successful */
    fun startUpdate(
        progress: (progress: Float) -> Unit,
        onDownloadStopped: (success: Boolean) -> Unit
    ) {
        if (updateFile.exists() && updateFile.length() > 0) {
            installUpdate()
            return
        }

        val url = "https://github.com/kaii-lb/LavenderPhotos/releases/download/${githubVersionName.value}/photos_signed_release.apk"

        try {
            // TODO: switch to fuel-android for better usage
            coroutineScope.launch(Dispatchers.IO) {
                Fuel.download(url)
                    .fileDestination { _, _ ->
                        updateFile
                    }
                    .progress { readBytes, totalBytes ->
                        val percent = readBytes.toFloat() / totalBytes * 100f
                        Log.d(TAG, "Download progress $percent% out of ${totalBytes.toFloat() / 1000000}mb")
                        progress(percent)
                    }
                    .response { result -> // TODO: parse result output | failure, success, etc
                        result.fold(
                            success = {
                                Log.d(TAG, "Download succeeded")
                                onDownloadStopped(true)
                            },

                            failure = {
                                onDownloadStopped(false)
                                Log.d(TAG, "Download failed, aborting...")
                                Log.d(TAG, result.toString())
                            }
                        )
                    }
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.toString())
            e.printStackTrace()
        }
    }

    fun installUpdate() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = FileProvider.getUriForFile(context, LAVENDER_FILE_PROVIDER_AUTHORITY, updateFile)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(intent)
    }

    fun getChangelog(): String =
        githubResponseBody.value?.let {
            it["body"]
                .toString()
                .replace("\n- ", "<br>- ")
                .replace("\'", "'")
                .replace("</u>", "</u><br>")
                .replace("</p>", "</p><br>")
                .replace("<p>\n\n", "<p>")
        } ?: context.resources.getString(R.string.updates_no_changelog)

    fun startupUpdateCheck(navController: NavController) {
        refresh { state ->
            when (state) {
                State.Succeeded -> {
                    if (hasUpdates.value) {
                        Log.d(TAG, "Update found! Notifying user...")

                        coroutineScope.launch {
                            LavenderSnackbarController.pushEvent(
                                LavenderSnackbarEvents.ActionEvent(
                                    message = context.resources.getString(R.string.updates_new_version_available),
                                    icon = R.drawable.error_2,
                                    duration = SnackbarDuration.Short,
                                    actionIcon = R.drawable.download,
                                    action = {
                                        navController.navigate(Screens.Settings.Misc.UpdatePage)
                                    }
                                )
                            )
                        }
                    }
                }

                else -> {
                    Log.d(TAG, "No update found.")
                }
            }
        }
    }
}

@Composable
fun rememberUpdater(): Updater {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return remember {
        Updater(
            context = context,
            coroutineScope = coroutineScope
        )
    }
}