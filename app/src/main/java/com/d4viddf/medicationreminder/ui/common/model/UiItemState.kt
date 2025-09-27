package com.d4viddf.medicationreminder.ui.common.model

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class UiItemState<out T> {
    data class Success<out T>(val data: T) : UiItemState<T>()
    data object Loading : UiItemState<Nothing>()
    data class Error(val exception: Throwable) : UiItemState<Nothing>()
}