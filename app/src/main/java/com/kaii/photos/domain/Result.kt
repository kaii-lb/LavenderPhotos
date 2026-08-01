package com.kaii.photos.domain

interface Error

typealias RootError = Error

sealed interface Result<out T, out E : RootError> {
    data class Success<out T, out E : RootError>(val data: T) : Result<T, E>
    data class Error<out T, out E : RootError>(val error: E) : Result<T, E>
}

fun <T, R : Any, E : RootError> Result<T, E>.mapTo(to: Result<R, E>): Result<R, E> = when (this) {
    is Result.Success -> to
    is Result.Error -> Result.Error(error)
}

fun <T, R : Any, E : RootError> Result<T, E>.mapTo(to: (Result.Success<T, E>) -> Result<R, E>): Result<R, E> = when (this) {
    is Result.Success -> to(this)
    is Result.Error -> Result.Error(error)
}