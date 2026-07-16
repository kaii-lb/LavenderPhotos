package com.kaii.photos.models.behaviour

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.PhotosApplication
import com.kaii.photos.data.datasources.BehaviourDataSource

@Suppress("UNCHECKED_CAST")
class BehaviourViewModelFactory : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == BehaviourViewModel::class.java) {
            val dataSource = BehaviourDataSource(
                behaviour = PhotosApplication.appModule.settings.behaviour
            )

            return BehaviourViewModel(dataSource) as T
        }
        throw IllegalArgumentException("${BehaviourViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${BehaviourViewModel::class.simpleName}!! This should never happen!!")
    }
}
