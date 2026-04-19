package com.kaii.photos.models

sealed interface OperationStatus {
    object Loading : OperationStatus
    object Successful : OperationStatus
    object Failed : OperationStatus
}