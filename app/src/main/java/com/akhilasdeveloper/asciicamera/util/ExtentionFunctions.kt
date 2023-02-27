package com.akhilasdeveloper.asciicamera.util

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.get
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
        collectLatest {
            function.invoke(it)
        }
    }
}

operator fun Bitmap.iterator(): Iterator<Int> {
    val list = arrayListOf<Int>()
    for (x in 0 until width)
        for (y in 0 until height)
            list.add(get(x, y))
    return list.iterator()
}

inline fun Bitmap.forEachIndexed(action: (x: Int, y: Int, pixel: Int) -> Unit): Unit {
    for (x in 0 until width)
        for (y in 0 until height)
            action(x, y, get(x, y))
}

fun TwoDtoOneD(x: Int, y: Int, width: Int): Int = x + width * y
fun xFromOneD(index: Int, width: Int): Int = index % width
fun yFromOneD(index: Int, width: Int): Int = index / width