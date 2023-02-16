package com.akhilasdeveloper.asciicamera.ui

import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.asciicamera.repository.Repository
import com.akhilasdeveloper.asciicamera.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _lensState = MutableSharedFlow<CameraSelector>()
    val lensState: SharedFlow<CameraSelector> = _lensState

    private val _inverseCanvasState = MutableSharedFlow<Boolean>()
    val inverseCanvasState: SharedFlow<Boolean> = _inverseCanvasState

    private suspend fun setLens(lens: CameraSelector) {
        repository.setCurrentLens(lens.toCameraSelectorInt())
    }

    fun getLens() {
        repository.getCurrentLens().onEach { lens ->
            isCanvasNeedsToBeInverse(lens)
            _lensState.emit(lens.toCameraSelector())
        }.launchIn(viewModelScope)
    }

    fun toggleCamera() {
        viewModelScope.launch {
            setLens(
                if (repository.getCurrentLensRaw() == Constants.DEFAULT_FRONT_CAMERA) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
            )
        }
    }

    private suspend fun isCanvasNeedsToBeInverse(lens: Int) {
        _inverseCanvasState.emit(lens != Constants.DEFAULT_FRONT_CAMERA)
    }

    private fun Int.toCameraSelector(): CameraSelector =
        if (this == Constants.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_BACK_CAMERA
        else
            CameraSelector.DEFAULT_FRONT_CAMERA

    private fun CameraSelector.toCameraSelectorInt(): Int =
        if (this == CameraSelector.DEFAULT_BACK_CAMERA)
            Constants.DEFAULT_BACK_CAMERA
        else
            Constants.DEFAULT_FRONT_CAMERA

}



