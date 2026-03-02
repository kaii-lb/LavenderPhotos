package com.kaii.photos.models.immich_info_page

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaii.photos.datastore.ImmichBasicInfo
import com.kaii.photos.di.appModule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class ImmichInfoViewModel(
    context: Context
) : ViewModel() {
    private val settings = context.applicationContext.appModule.settings

    val info = settings.immich.getImmichBasicInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = ImmichBasicInfo.Empty
    )

    val alwaysShow = settings.immich.getAlwaysShowUserInfo().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = false
    )

    fun setInfo(info: ImmichBasicInfo) = settings.immich.setImmichBasicInfo(info)

    fun setAlwaysShow(value: Boolean) = settings.immich.setAlwaysShowUserInfo(value)
}