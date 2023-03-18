package com.akhilasdeveloper.asciicamera.ui

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.asciicamera.repository.Repository
import com.akhilasdeveloper.asciicamera.repository.room.FilterSpecsTable
import com.akhilasdeveloper.asciicamera.util.Constants
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    private val asciiGenerator: AsciiGenerator
) : ViewModel() {

    private var capturedTextBitmap: Bitmap? = null
    private var capturedRawBitmap: Bitmap? = null

    init {
        asciiGenerator.setAsciiGeneratedListener(object : AsciiGenerator.OnGeneratedListener {

            override fun onContinue() {
                capturedTextBitmap = null
                revertPanelButtons()
                startCamera()
            }

            override fun onCapture(bitmap: Bitmap?, lastRawBitmap: Bitmap) {
                capturedTextBitmap = bitmap
                capturedRawBitmap = lastRawBitmap
                changePanelButtonsToConfirm()
                pauseCamera()
            }
        })
    }

    private fun pauseCamera() {
        viewModelScope.launch {
            _pauseCameraState.emit(true)
        }
    }

    private fun changePanelButtonsToConfirm() {
        viewModelScope.launch {
            _changePanelButtonsToConfirmState.emit(true)
        }
    }

    private fun startCamera() {
        viewModelScope.launch {
            _startCameraState.emit(true)
        }
    }

    private fun revertPanelButtons() {
        viewModelScope.launch {
            _revertPanelButtonState.emit(true)
        }
    }

    private val _lensState = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val lensState: StateFlow<CameraSelector> = _lensState

    private val _shareAsImageState = MutableStateFlow<Bitmap?>(null)
    val shareAsImageState: StateFlow<Bitmap?> = _shareAsImageState

    private val _setBitmapToImageState = MutableStateFlow<Bitmap?>(null)
    val setBitmapToImageState: StateFlow<Bitmap?> = _setBitmapToImageState

    private val _revertPanelButtonState = MutableStateFlow(false)
    val revertPanelButtonState: StateFlow<Boolean> = _revertPanelButtonState

    private val _changePanelButtonsToConfirmState = MutableStateFlow(false)
    val changePanelButtonsToConfirmState: StateFlow<Boolean> = _changePanelButtonsToConfirmState

    private val _startCameraState = MutableStateFlow(false)
    val startCameraState: StateFlow<Boolean> = _startCameraState

    private val _pauseCameraState = MutableStateFlow(false)
    val pauseCameraState: StateFlow<Boolean> = _pauseCameraState

    private val _launchPhotoPickerState = MutableStateFlow(false)
    val launchPhotoPickerState: StateFlow<Boolean> = _launchPhotoPickerState

    private val _inverseCanvasState = MutableStateFlow(false)
    val inverseCanvasState: StateFlow<Boolean> = _inverseCanvasState

    private val _customFiltersListState = MutableStateFlow<List<FilterSpecs>>(arrayListOf())
    val customFiltersListState: StateFlow<List<FilterSpecs>> = _customFiltersListState

    private val _filtersCount = MutableStateFlow<Int>(0)
    val filtersCount: StateFlow<Int> = _filtersCount

    private suspend fun setLens(lens: CameraSelector) {
        repository.setCurrentLens(lens.toCameraSelectorInt())
    }

    fun getLens() {
        repository.getCurrentLens().onEach { lens ->
            isCanvasNeedsToBeInverse(lens)
            _lensState.emit(lens.toCameraSelector())
        }.launchIn(viewModelScope)
    }

    fun bottomSheetAddCustomFilterState(filterSpecs: FilterSpecs) {
        setAsciiGeneratorValues(
            fgColor = filterSpecs.fgColor,
            bgColor = filterSpecs.bgColor,
            colorType = filterSpecs.fgColorType
        )
    }

    private fun toggleCamera() {
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

    fun onGalleryButtonClicked() {
        viewModelScope.launch {
            if (asciiGenerator.isCapturedState) {
                _shareAsImageState.emit(capturedTextBitmap)
            } else {
                launchPhotoPicker()
            }
        }
    }

    private fun launchPhotoPicker() {
        viewModelScope.launch {
            _launchPhotoPickerState.emit(true)
        }
    }

    private fun isCanvasNeedsToBeInverse(lens: Int) {
        _inverseCanvasState.value = lens == Constants.DEFAULT_FRONT_CAMERA
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

    fun getCustomFilters() {
        viewModelScope.launch {
            repository.getCustomFilters().collect { rover ->
                _customFiltersListState.value = rover.map { filterSpecsFromTable(it) }
            }
        }
    }

    fun getFiltersCount() {
        viewModelScope.launch {
            _filtersCount.value = repository.getFiltersCount()
        }
    }

    fun addCustomFilters(filterSpecs: FilterSpecs) {
        viewModelScope.launch {
//            repository.addCustomFilter(filterSpecsTableFromData(filterSpecs))
        }
    }

    fun removeCustomFilter(filterSpecs: FilterSpecs) {
        viewModelScope.launch {
//            repository.deleteCustomFilter(filterSpecsTableFromData(filterSpecs))
        }
    }

    fun flipCamera(){
        if (asciiGenerator.isCapturedState) {
            asciiGenerator.continueStream()
        } else
            toggleCamera()
    }

    fun asciiGeneratorChangeFilter(filter: AsciiFilters){
        asciiGenerator.changeFilter(filter)
    }

    fun setAsciiGeneratorDispatcher(dispatcher: CoroutineDispatcher){
        asciiGenerator.setDispatcher(dispatcher)
    }

    fun processBitmap(mutableBitmap: Bitmap){
        viewModelScope.launch {
            val result = asciiGenerator.imageBitmapToTextBitmap(mutableBitmap)
            setBitmapToImage(result)
            asciiGenerator.capture()
        }
    }

    private fun setBitmapToImage(result: Bitmap?) {
        viewModelScope.launch {
            _setBitmapToImageState.emit(result)
        }
    }

    fun generateTextView(imageProxy: ImageProxy) {
        viewModelScope.launch {
            val bitmap = asciiGenerator.imageProxyToTextBitmap(imageProxy)
            setBitmapToImage(bitmap)
        }
    }

    private fun filterSpecsFromTable(filterSpecsTable: FilterSpecsTable): FilterSpecs = FilterSpecs(
        id = filterSpecsTable.id,
        density = filterSpecsTable.density,
        fgColor = filterSpecsTable.fgColor,
        bgColor = filterSpecsTable.bgColor,
        fgColorType = filterSpecsTable.fgColorType,
    )

    /*private fun filterSpecsTableFromData(filterSpecs: FilterSpecs): FilterSpecsTable = FilterSpecsTable(
        id = filterSpecs.id,
        density = filterSpecs.density,
        fgColor = filterSpecs.fgColor,
        bgColor = filterSpecs.bgColor,
        fgColorType = filterSpecs.fgColorType,
    )*/

    fun asciiGeneratorCapture() {
        asciiGenerator.capture()
    }

    fun setAsciiGeneratorValues(
        fgColor: Int? = null,
        bgColor: Int? = null,
        colorType: Int? = null,
        density: String? = null,
        densityByteArray: ByteArray? = null,
    ) {
        fgColor?.let {
            asciiGenerator.fgColor = it
        }
        bgColor?.let {
            asciiGenerator.bgColor = it
        }
        colorType?.let {
            asciiGenerator.colorType = it
        }
        density?.let {
            asciiGenerator.density = it
        }
        densityByteArray?.let {
            asciiGenerator._densityIntArray = it
        }

        viewModelScope.launch {
            asciiGenerator.reProcessLastFrame()
        }
    }

}



