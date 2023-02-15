package com.akhilasdeveloper.asciicamera.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.akhilasdeveloper.asciicamera.util.Constants.ASCII_DB_NAME

internal val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ASCII_DB_NAME
)