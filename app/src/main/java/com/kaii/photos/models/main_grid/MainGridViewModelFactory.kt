package com.kaii.photos.models.main_grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.data.datasources.LatestNewsDataSource
import com.kaii.photos.data.parsers.HTMLToLnmParser
import com.kaii.photos.data.parsers.LnmParser
import com.kaii.photos.data.providers.AppVersionProvider
import com.kaii.photos.repositories.LatestNewsRepository

@Suppress("UNCHECKED_CAST")
class MainGridViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == MainGridViewModel::class.java) {
            val latestNewsRepository = LatestNewsRepository(
                dataSource = LatestNewsDataSource(),
                hTMLToLnmParser = HTMLToLnmParser(),
                lnmParser = LnmParser(),
                versionProvider = AppVersionProvider(context)
            )

            return MainGridViewModel(
                context = context,
                latestNewsRepository = latestNewsRepository
            ) as T
        }
        throw IllegalArgumentException("${MainGridViewModelFactory::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${MainGridViewModel::class.simpleName}!! This should never happen!!")
    }
}