package com.akhilasdeveloper.asciicamera.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.asciicamera.repository.Repository
import com.akhilasdeveloper.asciicamera.repository.room.FilterSpecsTable
import com.akhilasdeveloper.asciicamera.util.Constants
import com.akhilasdeveloper.asciicamera.util.FilterGenerator
import com.akhilasdeveloper.asciicamera.util.Utilities
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executor
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository,
    private val asciiGenerator: AsciiGenerator,
    private val imageAnalysis: ImageAnalysis,
    private val filterGenerator: FilterGenerator,
    private val utilities: Utilities
) : ViewModel() {

    private var capturedTextBitmap: Bitmap? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    init {
        asciiGenerator.setAsciiGeneratedListener(object : AsciiGenerator.OnGeneratedListener {

            override fun onContinue() {
                capturedTextBitmap = null
                revertPanelButtons()
                startCamera()
                Timber.d("Continue")
            }

            override fun onCapture(bitmap: Bitmap?) {
                capturedTextBitmap = bitmap
                setBitmapToImage(bitmap)
                changePanelButtonsToConfirm()
                pauseCamera()
                Timber.d("Capture")
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
            _changePanelButtonToConfirmState.emit(true)
        }
    }

    fun startCamera() {
        viewModelScope.launch {
            _startCameraState.emit(cameraSelector)
        }
    }

    private fun revertPanelButtons() {
        viewModelScope.launch {
            _changePanelButtonToConfirmState.emit(false)
        }
    }

    fun convertImageFromGallery(imageUri: Uri) {
        val bitmap = utilities.bitmapFromUri(imageUri)
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        processBitmap(mutableBitmap)
    }

    private val _shareAsImageState = MutableStateFlow<Uri?>(null)
    val shareAsImageState: StateFlow<Uri?> = _shareAsImageState

    private val _populateCurrentFilterDetailsToAddFilterBottomSheetState = MutableStateFlow(FilterSpecs())
    val populateCurrentFilterDetailsToAddFilterBottomSheetState: StateFlow<FilterSpecs> =
        _populateCurrentFilterDetailsToAddFilterBottomSheetState

    private val _showEditDensityPopupState = MutableSharedFlow<String>()
    val showEditDensityPopupState: SharedFlow<String> = _showEditDensityPopupState

    private val _changePanelButtonToConfirmState = MutableStateFlow(false)
    val changePanelButtonToConfirmState: StateFlow<Boolean> = _changePanelButtonToConfirmState

    private val _setBitmapToImageState = MutableStateFlow<Bitmap?>(null)
    val setBitmapToImageState: StateFlow<Bitmap?> = _setBitmapToImageState

    private val _startCameraState = MutableSharedFlow<CameraSelector>()
    val startCameraState: SharedFlow<CameraSelector> = _startCameraState

    private val _pauseCameraState = MutableSharedFlow<Boolean>()
    val pauseCameraState: SharedFlow<Boolean> = _pauseCameraState

    private val _launchPhotoPickerState = MutableStateFlow(false)
    val launchPhotoPickerState: StateFlow<Boolean> = _launchPhotoPickerState

    private val _customFiltersListState = MutableStateFlow<List<FilterSpecs>>(arrayListOf())
    val customFiltersListState: StateFlow<List<FilterSpecs>> = _customFiltersListState

    private val _filtersCount = MutableStateFlow(0)
    val filtersCount: StateFlow<Int> = _filtersCount

    private suspend fun setLens(lens: CameraSelector) {
        repository.setCurrentLens(lens.toCameraSelectorInt())
    }

    fun getLens() {
        viewModelScope.launch {
            if (getFiltersCountRaw()<=0){
                generateFilters(true)
            }else{
                repository.getCurrentLens().onEach { lens ->
                    cameraSelector = lens.toCameraSelector()
                    startCamera()
                }.launchIn(viewModelScope)
            }
        }
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
                val imageUri: Uri? = utilities.toImageURI(capturedTextBitmap)
                _shareAsImageState.emit(imageUri)
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

    private suspend fun getFiltersCountRaw() = repository.getFiltersCount()

    private fun validateFilterCountAndGenerate(){
        viewModelScope.launch {
            if (getFiltersCountRaw() <= 0)
                generateFilters()
        }
    }

    fun removeCustomFilter(filterSpecs: FilterSpecs) {
        viewModelScope.launch {
            repository.deleteCustomFilter(
                FilterSpecsTable(
                    id = filterSpecs.id,
                    density = filterSpecs.density,
                    fgColor = filterSpecs.fgColor,
                    bgColor = filterSpecs.bgColor,
                    fgColorType = filterSpecs.fgColorType,
                    densityArray = filterSpecs.densityArray
                )
            )
            validateFilterCountAndGenerate()
        }
    }

    fun flipCamera() {
        if (asciiGenerator.isCapturedState) {
            asciiGenerator.continueStream()
        } else
            toggleCamera()
    }

    fun setAsciiGeneratorDispatcher(dispatcher: CoroutineDispatcher) {
        asciiGenerator.setDispatcher(dispatcher)
    }

    private fun processBitmap(mutableBitmap: Bitmap) {
        pauseCamera()
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

    private fun generateTextView(imageProxy: ImageProxy) {
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
            asciiGenerator.densityByteArray = it
        }

        viewModelScope.launch {
            asciiGenerator.reProcessLastFrame()
        }
    }

    fun sortEditTextChars(charEditText: TextInputEditText) {
        utilities.sortEditTextChars(charEditText)
    }

    fun reverseEditTextChars(charEditText: TextInputEditText) {
        utilities.reverseEditTextChars(charEditText)
    }

    fun showEditDensityPopup(density: String) {
        viewModelScope.launch {
            _showEditDensityPopupState.emit(density)
        }

    }

    fun onApplyDensity(charEditText: TextInputEditText) {
        viewModelScope.launch {
            val chars = utilities.getDensityCharsFromEditText(charEditText)

            val array = utilities.generateDensityArray(
                chars,
                AsciiFilters.WhiteOnBlack.textCharSize
            )

            setAsciiGeneratorValues(
                density = chars,
                densityByteArray = array
            )

            populateCurrentFilterDetailsToAddFilterBottomSheet()
        }
    }

    fun setCameraSelector(defaultFrontCamera: CameraSelector) {
        cameraSelector = defaultFrontCamera
    }

    fun buildImageAnalysisUseCase(cameraExecutor: Executor): ImageAnalysis {
        return imageAnalysis.apply {
            setAnalyzer(cameraExecutor) { imageProxy ->
                generateTextView(imageProxy)
            }
        }
    }

    private fun generateFilters(isInit:Boolean = false) {
        viewModelScope.launch {
            val filters = filterGenerator.generateFilters().map {
                FilterSpecsTable(
                    id = it.id,
                    density = it.density,
                    fgColor = it.fgColor,
                    bgColor = it.bgColor,
                    fgColorType = it.fgColorType,
                    densityArray = it.densityArray
                )
            }
            filters.forEach {
                repository.addFilter(it)
            }
            if (isInit)
                getLens()
        }
    }

    fun populateCurrentFilterDetailsToAddFilterBottomSheet() {
        viewModelScope.launch {
            _populateCurrentFilterDetailsToAddFilterBottomSheetState.emit(
                FilterSpecs(
                    fgColor = asciiGenerator.fgColor,
                    bgColor = asciiGenerator.bgColor,
                    density = asciiGenerator.density,
                    densityArray = asciiGenerator.densityByteArray,
                    fgColorType = asciiGenerator.colorType
                )
            )
        }
    }

    fun saveCurrentFilter() {
        viewModelScope.launch {
            repository.addFilter(
                FilterSpecsTable(
                    fgColor = asciiGenerator.fgColor,
                    bgColor = asciiGenerator.bgColor,
                    density = asciiGenerator.density,
                    densityArray = asciiGenerator.densityByteArray,
                    fgColorType = asciiGenerator.colorType
                )
            )
        }
    }

}



