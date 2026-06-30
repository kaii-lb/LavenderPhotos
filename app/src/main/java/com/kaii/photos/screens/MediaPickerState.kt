package com.kaii.photos.screens

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastMap
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.kaii.photos.PhotosApplication
import com.kaii.photos.database.MediaDatabase
import com.kaii.photos.helpers.grid_management.SelectionManager
import com.kaii.photos.mediastore.LAVENDER_FILE_PROVIDER_AUTHORITY
import io.github.kaii_lb.lavender.immichintegration.Auth
import io.github.kaii_lb.lavender.immichintegration.UriWriteChannel
import io.github.kaii_lb.lavender.immichintegration.clients.AssetsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MediaPickerState(
    private val context: Context,
    private val incomingIntent: Intent,
    coroutineScope: CoroutineScope
) {
    private val dao = MediaDatabase.getInstance(context).mediaDao()

    private lateinit var endpoint: String
    private lateinit var auth: Auth

    var isLoading by mutableStateOf(false)
        private set

    private val _processedCount = MutableStateFlow(0)
    val processedCount = _processedCount.asStateFlow()

    init {
        coroutineScope.launch {
            PhotosApplication.appModule.settings.immich.getImmichBasicInfo().collect {
                endpoint = it.endpoint
                auth = it.auth
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun parseUris(
        items: List<SelectionManager.SelectedItem>
    ): List<Uri> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()

        val media = dao.getMedia(ids = items.fastMap { it.id })

        val parsed = mutableListOf<Uri>()
        val cacheDir = context.cacheDir

        val assetClient = AssetsClient(
            client = PhotosApplication.appModule.apiClient,
            endpoint = endpoint,
            auth = auth
        )

        media.forEach { item ->
            _processedCount.value += 1

            if (!item.isCloud) {
                parsed.add(item.uri.toUri())
                return@forEach
            }

            val file = File(cacheDir, "${item.id}-${item.displayName}")

            val success = (file.exists() && file.length() == item.size) ||
                    assetClient.download(
                        id = Uuid.parse(item.immichId!!),
                        channel = UriWriteChannel(
                            uri = file.toUri(),
                            context
                        )
                    )

            if (success) {
                parsed.add(
                    FileProvider.getUriForFile(
                        context,
                        LAVENDER_FILE_PROVIDER_AUTHORITY,
                        file
                    )
                )
            }
        }

        return@withContext parsed
    }

    private fun send(
        incomingIntent: Intent,
        uris: List<Uri>
    ) {
        if (uris.isEmpty()) return

        val activity = context as Activity

        if (incomingIntent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            || incomingIntent.action == Intent.ACTION_OPEN_DOCUMENT
        ) {
            val resultIntent = Intent().apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val clipData = ClipData.newUri(context.contentResolver, "Media", uris.first())
                for (i in 1 until uris.size) {
                    clipData.addItem(ClipData.Item(uris[i]))
                }
                setClipData(clipData)
            }

            activity.setResult(RESULT_OK, resultIntent)
        } else {
            val resultIntent = Intent().apply {
                data = uris.first()
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            activity.setResult(RESULT_OK, resultIntent)
        }
    }

    suspend fun shareWithApp(
        items: List<SelectionManager.SelectedItem>
    ) {
        isLoading = true
        _processedCount.value = 0

        val uris = parseUris(items = items)
        send(
            incomingIntent = incomingIntent,
            uris = uris
        )

        isLoading = false
    }
}

@Composable
fun retainMediaPickerState(incomingIntent: Intent): MediaPickerState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    return retain(incomingIntent) {
        MediaPickerState(
            context = context,
            coroutineScope = coroutineScope,
            incomingIntent = incomingIntent
        )
    }
}