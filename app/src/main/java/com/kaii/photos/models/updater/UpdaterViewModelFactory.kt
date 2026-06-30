package com.kaii.photos.models.updater

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.data.datasources.LatestNewsDataSource
import com.kaii.photos.data.parsers.LnmParser
import com.kaii.photos.data.parsers.HTMLToLnmParser
import com.kaii.photos.data.providers.AppVersionProvider
import com.kaii.photos.data.providers.UpdaterParamProvider
import com.kaii.photos.repositories.LatestNewsRepository

@Suppress("UNCHECKED_CAST")
class UpdaterViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == UpdaterViewModel::class.java) {
            val repository = LatestNewsRepository(
                dataSource = LatestNewsDataSource(),
                hTMLToLnmParser = HTMLToLnmParser(),
                lnmParser = LnmParser(),
                versionProvider = AppVersionProvider(context.applicationContext)
            )

            return UpdaterViewModel(
                latestNewsRepository = repository,
                paramProvider = UpdaterParamProvider()
            ) as T
        }
        throw IllegalArgumentException("${UpdaterViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${UpdaterViewModel::class.simpleName}!! This should never happen!!")
    }
}
