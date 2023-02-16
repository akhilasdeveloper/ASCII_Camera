package com.akhilasdeveloper.asciicamera.repository

import androidx.datastore.preferences.core.intPreferencesKey
import com.akhilasdeveloper.asciicamera.repository.datastore.DataStoreFunctions
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_BACK_CAMERA
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class Repository
@Inject constructor(
    private val dataStore: DataStoreFunctions
) {

    private val LENS_VALUE = intPreferencesKey("LENS_VALUE")

    suspend fun setCurrentLens(lens: Int) {
        dataStore.saveValueToPreferencesStore(LENS_VALUE, lens)
    }

    fun getCurrentLens() =
        dataStore.getValueAsFlowFromPreferencesStore(LENS_VALUE).map { it ?: DEFAULT_BACK_CAMERA }

    suspend fun getCurrentLensRaw() =
        dataStore.getValueFromPreferencesStore(LENS_VALUE) ?: DEFAULT_BACK_CAMERA
}