package com.akhilasdeveloper.asciicamera.ui

import androidx.camera.core.CameraSelector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilasdeveloper.asciicamera.repository.Repository
import com.akhilasdeveloper.asciicamera.repository.room.FilterSpecsTable
import com.akhilasdeveloper.asciicamera.util.Constants
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.FilterSpecs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private val _bottomSheetAddCustomFilterState = MutableStateFlow<FilterSpecs>(FilterSpecs())
    val bottomSheetAddCustomFilterState: StateFlow<FilterSpecs> = _bottomSheetAddCustomFilterState

    private val _lensState = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val lensState: StateFlow<CameraSelector> = _lensState

    private val _inverseCanvasState = MutableStateFlow(false)
    val inverseCanvasState: StateFlow<Boolean> = _inverseCanvasState

    private val _customFiltersListState = MutableStateFlow<List<FilterSpecs>>(arrayListOf())
    val customFiltersListState: StateFlow<List<FilterSpecs>> = _customFiltersListState

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
        _bottomSheetAddCustomFilterState.value = filterSpecs
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

    fun addCustomFilters(filterSpecs: FilterSpecs) {
        viewModelScope.launch {
            repository.addCustomFilter(filterSpecsTableFromData(filterSpecs))
        }
    }

    fun removeCustomFilter(filterSpecs: FilterSpecs) {
        viewModelScope.launch {
            repository.deleteCustomFilter(filterSpecsTableFromData(filterSpecs))
        }
    }

    private fun filterSpecsFromTable(filterSpecsTable: FilterSpecsTable): FilterSpecs = FilterSpecs(
        id = filterSpecsTable.id,
        density = filterSpecsTable.density,
        fgColor = filterSpecsTable.fgColor,
        bgColor = filterSpecsTable.bgColor,
        fgColorType = filterSpecsTable.fgColorType,
        )

    private fun filterSpecsTableFromData(filterSpecs: FilterSpecs): FilterSpecsTable = FilterSpecsTable(
        id = filterSpecs.id,
        density = filterSpecs.density,
        fgColor = filterSpecs.fgColor,
        bgColor = filterSpecs.bgColor,
        fgColorType = filterSpecs.fgColorType,
    )

}



