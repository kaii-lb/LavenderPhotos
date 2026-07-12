package com.kaii.photos.models.data_and_backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kaii.photos.PhotosApplication
import com.kaii.photos.data.datasources.DataAndBackupDatasource
import com.kaii.photos.datastore.datastore
import com.kaii.photos.repositories.DataAndBackupRepository

@Suppress("UNCHECKED_CAST")
class DataAndBackupViewModelFactory(
    private val context: Context
) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass == DataAndBackupViewModel::class.java) {
            val dataSource = DataAndBackupDatasource(
                context = context.applicationContext,
                datastore = context.datastore,
                customDao = PhotosApplication.appModule.db.customDao()
            )

            return DataAndBackupViewModel(
                repo = DataAndBackupRepository(dataSource)
            ) as T
        }
        throw IllegalArgumentException("${DataAndBackupViewModel::class.simpleName}: Cannot cast ${modelClass.simpleName} as ${DataAndBackupViewModel::class.simpleName}!! This should never happen!!")
    }
}
