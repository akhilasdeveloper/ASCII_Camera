package com.akhilasdeveloper.asciicamera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.camera.core.ImageProxy
import androidx.core.graphics.get
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.akhilasdeveloper.asciicamera.util.Constants.ASCII_DB_NAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer

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

fun Bitmap.getAllPixels(intArray: IntArray){
    getPixels(intArray, 0,width,0,0,width, height)
}

fun Bitmap.getAllPixelsBytes(byteArray: ByteArray){
    val intArray = IntArray(width * height)
    getPixels(intArray, 0,width,0,0,width, height)
    intArray.forEachIndexed { index, i ->
//        byteArray[index] = (i and 0xff).toByte()
        val brightness = (0.299 * Color.red(i) + 0.587 * Color.green(i) + 0.114 * Color.blue(i)).toInt()
        byteArray[index] = if (brightness < 128) 0.toByte() else 1.toByte()
    }
}



fun Float.map(
    startValue: Float,
    endValue: Float,
    mapStartValue: Int,
    mapEndValue: Int
): Int {
    val n = endValue - startValue
    val mapN = mapEndValue - mapStartValue
    val factor = mapN.toFloat() / n
    return ((startValue + this) * factor).toInt().coerceAtLeast(0).coerceAtMost(mapN - 1)
}

fun IntArray.getSubPixels(
    width: Int,
    xStart: Int,
    yStart: Int,
    destWidth: Int,
    destHeight: Int
): IntArray {
    val yEnd = yStart + destHeight
    val xEnd = xStart + destWidth
    val subArray = mutableListOf <Int>()

    for (i in yStart until yEnd) {
        val row: IntArray = sliceArray(i * width + xStart until i * width + xEnd)
        subArray.addAll(row.toList())
    }
    return subArray.toIntArray()
}

fun rotateByteArrayImage(
    outPutArray: IntArray,
    rotatedArray: IntArray,
    width: Int,
    height: Int
) {
    for (x in 0 until width) {
        for (y in 0 until height) {

            val destX = width - y
            val destY = x
            // Calculate the source and destination indices
            val srcIndex = (y * width + x) * 2
            val destIndex = (destY * height + destX) * 2
            Timber.d("image width:height:x:y $width:$height:$x:$y")

            // Copy the pixel values from the source to the destination
            rotatedArray[destIndex] =
                outPutArray[srcIndex]
            rotatedArray[destIndex + 1] = outPutArray[srcIndex + 1]
        }
    }
}

fun ByteArray.getSubPixels(
    width: Int,
    xStart: Int,
    yStart: Int,
    destWidth: Int,
    destHeight: Int
): ByteArray {
    val yEnd = yStart + destHeight
    val xEnd = xStart + destWidth
    val subArray = mutableListOf <Byte>()

    for (i in yStart until yEnd) {
        val row: ByteArray = sliceArray(i * width + xStart until i * width + xEnd)
        subArray.addAll(row.toList())
    }
    return subArray.toByteArray()
}

fun generateResult(
    ascii_index_array: IntArray,
    text_bitmap_width: Int,
    text_size_int: Int,
    density_byte_array: IntArray,
    result_array: IntArray,
    result_size: Int,
    result_width: Int
) {

    for (index in 0 until result_size) { //60

        val x = index % result_width //4
        val y = index / result_width //479

        val asciiIndexX = x / text_size_int //106
        val asciiIndexY = y / text_size_int //79
        val asciiIndexIndex = asciiIndexX + text_bitmap_width * asciiIndexY //9555
        val asciiIndex = ascii_index_array[asciiIndexIndex]

        val asciiArrayIndexX = x % text_size_int
        val asciiArrayIndexY = y % text_size_int
        val asciiArrayIndex = asciiArrayIndexX + text_size_int * asciiArrayIndexY

        val ascii =
            density_byte_array[asciiArrayIndex + (asciiIndex * text_size_int * text_size_int)]

        result_array[index] = ascii

    }

}

fun reducePixels2(
    width: Int,
    textBitmapWidth: Int,
    textSizeInt: Int,
    intArray: IntArray,
    asciiColorArray: IntArray,
    height: Int
) {

    val arraySize: Int = textSizeInt * textSizeInt
    val rowArray = IntArray(textBitmapWidth * 2)

    for (index in 0 until 60) {
        val y = index / width
        val x = index % width
        val col = x / textSizeInt
        val row = y / textSizeInt

        val i1 = index * 2
        val v1 = intArray[i1]
        rowArray[col * 2] += v1
        val i2 = index * 2 + 1
        val v2 = intArray[i2]
        rowArray[col * 2 + 1] += v2

        if ((y + 1) % textSizeInt == 0 && (x + 1) % textSizeInt == 0) {
            val r = rowArray[col * 2]
            val g = rowArray[col * 2 + 1]

            rowArray[col * 2] = 0
            rowArray[col * 2 + 1] = 0

            val ind = col + textBitmapWidth * row
            asciiColorArray[ind] = r + g
        }
    }

}

fun ImageProxy.toBitmap(): Bitmap? {
    val planes = planes
    val buffer: ByteBuffer = planes[0].buffer
    val pixelStride: Int = planes[0].pixelStride
    val rowStride: Int = planes[0].rowStride
    val rowPadding: Int = rowStride - pixelStride * width

    val bitmap = Bitmap.createBitmap(
        width + rowPadding / pixelStride,
        height, Bitmap.Config.ARGB_8888
    )

    bitmap.copyPixelsFromBuffer(buffer)
    return bitmap
}

fun TwoDtoOneD(x: Int, y: Int, width: Int): Int = x + width * y
fun xFromOneD(index: Int, width: Int): Int = index % width
fun yFromOneD(index: Int, width: Int): Int = index / width