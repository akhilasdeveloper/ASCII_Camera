package com.akhilasdeveloper.asciicamera.repository.datastore

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface DataStoreFunctions  {
    suspend fun <T> saveValueToPreferencesStore(key: Preferences.Key<T>, value: T)
    suspend fun <T> clearPreferencesStore(key: Preferences.Key<T>)
    fun <T> getValueAsFlowFromPreferencesStore(key: Preferences.Key<T>) : Flow<T?>
    suspend fun <T> getValueFromPreferencesStore(key: Preferences.Key<T>) : T?
}