package com.kaii.photos.data.datasources

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LatestNewsDataSource {
    companion object {
        private val TAG = LatestNewsDataSource::class.qualifiedName
        private const val RELEASE_URL = "https://api.github.com/repos/kaii-lb/LavenderPhotos/releases/latest"
    }

    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        val info = Fuel.get(RELEASE_URL).responseJson().third.fold(
            success = { result ->
                result.obj()
            },

            failure = { error ->
                Log.e(TAG, error.message.toString())
                error.printStackTrace()

                return@withContext null
            }
        )

        val title = info.getString("name")
        val date = info.getString("created_at").substringBefore("T")
        val body = info.getString("body")

        return@withContext "$title $date\n$body"
    }

    suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
        Fuel.get(path = RELEASE_URL).responseJson().third.fold(
            success = { result ->
                result.obj().getString("tag_name")
            },

            failure = { error ->
                Log.e(TAG, error.message.toString())
                error.printStackTrace()

                return@withContext null
            }
        )
    }
}