package com.kaii.photos.models.news

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.datasources.NewsDataSource
import com.kaii.photos.repositories.NewsRepository

@Suppress("UNCHECKED_CAST")
class NewsViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == NewsViewModel::class.java) {
            val dataSource = NewsDataSource(context.applicationContext)
            val repository = NewsRepository(dataSource)

            return NewsViewModel(repository) as T
        }
        throw IllegalArgumentException("${NewsViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${NewsViewModel::class.simpleName}!! This should never happen!!")
    }
}
