package com.akhilasdeveloper.asciicamera.repository.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.akhilasdeveloper.asciicamera.util.userPreferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreFunctionsImpl(private val context: Context) : DataStoreFunctions {
    override suspend fun <T> saveValueToPreferencesStore(key: Preferences.Key<T>, value: T) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    override suspend fun <T> clearPreferencesStore(key: Preferences.Key<T>) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    override fun <T> getValueAsFlowFromPreferencesStore(key: Preferences.Key<T>): Flow<T?> =
        context.userPreferencesDataStore.data.map { preference ->
            preference[key]
        }

    override suspend fun <T> getValueFromPreferencesStore(key: Preferences.Key<T>): T? =
        context.userPreferencesDataStore.data.first()[key]

}