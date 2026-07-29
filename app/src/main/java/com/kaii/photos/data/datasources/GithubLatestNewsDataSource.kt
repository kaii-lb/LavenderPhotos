package com.kaii.photos.data.datasources

import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface LatestNewsDataSource {
    suspend fun fetch(): String?
    suspend fun getLatestVersion(): String?
}

class GithubLatestNewsDataSource @Inject constructor() : LatestNewsDataSource {
    companion object {
        private val TAG = GithubLatestNewsDataSource::class.qualifiedName
        private const val RELEASE_URL = "https://api.github.com/repos/kaii-lb/LavenderPhotos/releases/latest"
    }

    override suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        val info = Fuel.get(RELEASE_URL).responseJson().third.fold(
            success = { result ->
                result.obj()
            },

            failure = { error ->
                Log.e(TAG, "Failed to fetch latest news. ${error.message}")
                error.printStackTrace()

                return@withContext null
            }
        )

        val title = info.getString("name")
        val date = info.getString("created_at").substringBefore("T")
        val body = info.getString("body")

        return@withContext "$title $date\n$body"
    }

    override suspend fun getLatestVersion(): String? = withContext(Dispatchers.IO) {
        Fuel.get(path = RELEASE_URL).responseJson().third.fold(
            success = { result ->
                result.obj().getString("tag_name")
            },

            failure = { error ->
                Log.e(TAG, "Failed to fetch latest version. ${error.message}")
                error.printStackTrace()

                return@withContext null
            }
        )
    }
}