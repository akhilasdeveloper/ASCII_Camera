package com.akhilasdeveloper.asciicamera.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.akhilasdeveloper.asciicamera.util.Constants.ASCII_DB_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

internal val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ASCII_DB_NAME
)

fun <T> Flow<T>.observe(lifecycleScope: CoroutineScope, function: (T) -> Unit) {
    lifecycleScope.launch {
        collectLatest{
            function.invoke(it)
        }
    }
}

fun <T> Flow<T>.observeOnEach(lifecycleScope: CoroutineScope, function: (T) -> Unit) {
    lifecycleScope.launch {
        onEach{
            function.invoke(it)
        }
    }
}