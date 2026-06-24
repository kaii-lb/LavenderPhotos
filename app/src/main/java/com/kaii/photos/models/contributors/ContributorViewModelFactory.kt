package com.kaii.photos.models.contributors

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.data.datasources.ContributorDataSource
import com.kaii.photos.data.providers.AppVersionProvider
import com.kaii.photos.repositories.ContributorRepository

@Suppress("UNCHECKED_CAST")
class ContributorViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == ContributorViewModel::class.java) {
            val contributorDataSource = ContributorDataSource(
                context = context.applicationContext
            )

            val contributorRepository = ContributorRepository(contributorDataSource)
            val appVersionProvider = AppVersionProvider(context.applicationContext)

            return ContributorViewModel(contributorRepository, appVersionProvider) as T
        }
        throw IllegalArgumentException("${ContributorViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${ContributorViewModel::class.simpleName}!! This should never happen!!")
    }
}
