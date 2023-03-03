package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.graphics.*
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import com.akhilasdeveloper.asciicamera.util.*
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.CharData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.nio.ByteBuffer

class AsciiGenerator() {


    companion object {

        var isNativeLibAvailable = false

        init {
            try {
                System.loadLibrary("asciicamera")
                isNativeLibAvailable = true
            } catch (_: java.lang.Exception) {

            }
        }
    }

    private var filters: AsciiFilters = AsciiFilters.WhiteOnBlack
    private var textSize = filters.textCharSize
    private var textSizeInt = filters.textCharSize.toInt()
    private var density = filters.density
    private var fgColor = filters.fgColor
    private var bgColor = filters.bgColor

    fun changeFilter(filters: AsciiFilters) {
        this.filters = filters
        textSize = filters.textCharSize
        textSizeInt = filters.textCharSize.toInt()
        density = filters.density
        fgColor = filters.fgColor
        bgColor = filters.bgColor
        _densityIntArray = listOf()
    }

    private var _densityIntArray: List<IntArray> = listOf()
    private val densityIntArray: List<IntArray>
        get() {
            if (_densityIntArray.isEmpty())
                _densityIntArray = generateDensityBytes()

            return _densityIntArray
        }

    private fun generateDensityBytes(): List<IntArray> {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            textSizeInt * density.length,
            textSizeInt, Bitmap.Config.ARGB_8888
        )
        val paint = Paint()
        val textBounds: Rect = Rect()
        paint.textSize = textSize
        paint.color = Color.WHITE
        canvas.setBitmap(bitmap)
        canvas.drawColor(Color.BLACK)
        paint.getTextBounds("@", 0, 1, textBounds);

        density.toCharArray().forEachIndexed { index, c ->
            canvas.drawText(
                c.toString(),
                (index * textSize) + ((textSize / 2f) - textBounds.exactCenterX()),
                (textSize / 2f) - textBounds.exactCenterY(),
                paint
            )
        }

        val array = IntArray(bitmap.width * bitmap.height)
        bitmap.getAllPixels(array)

        val densityLength = density.length
        val result = mutableListOf<IntArray>()

        for (index in density.indices) {
            var sliceChar = IntArray(textSizeInt * textSizeInt)
            if (isNativeLibAvailable) {
                getSubPixelsNative(
                    width = densityLength * textSizeInt,
                    xStart = index * textSizeInt,
                    yStart = 0,
                    destWidth = textSizeInt,
                    destHeight = textSizeInt,
                    array = array,
                    resultArray = sliceChar
                )
            } else {
                sliceChar =
                    array.getSubPixels(
                        width = densityLength * textSizeInt,
                        xStart = index * textSizeInt,
                        yStart = 0,
                        destWidth = textSizeInt,
                        destHeight = textSizeInt
                    )
            }
            result.add(sliceChar)
        }

        return result
    }


    private external fun getSubPixelsNative(
        width: Int,
        xStart: Int,
        yStart: Int,
        destWidth: Int,
        destHeight: Int,
        array: IntArray,
        resultArray: IntArray
    )

    private external fun calculateAvgColorNative(array: IntArray, size: Int): Int

    private external fun convertByteArrayToRgbNative(
        byteArray: ByteArray,
        outPutArray: IntArray,
        width: Int,
        height: Int
    )

    private external fun calculateDensityIndexNative(pixel: Int, densityLength: Int): Int

    private external fun addToResultArrayNative(
        x: Int,
        y: Int,
        width: Int,
        textSizeInt: Int,
        char: IntArray,
        resultArray: IntArray
    )
    private fun convertRgbaToRgb(rgbaData: ByteArray, width: Int, height: Int): IntArray {
        val rgbData = IntArray(width * height)
        for ((pixelIndex, i) in (rgbaData.indices step 4).withIndex()) {
            val r = rgbaData[i].toInt() and 0xff
            val g = rgbaData[i + 1].toInt() and 0xff
            val b = rgbaData[i + 2].toInt() and 0xff
            val a = rgbaData[i + 3].toInt() and 0xff
            rgbData[pixelIndex] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return rgbData
    }

    private val _generatedBitmapState = MutableSharedFlow<Bitmap>()
    val generatedBitmapState: SharedFlow<Bitmap> = _generatedBitmapState

    private suspend fun rgbArrayToTextBitmap(intArray: IntArray, width: Int, height: Int) =
        withContext(Dispatchers.Default) {

            val textBitmapWidth = width / textSizeInt
            val textBitmapHeight = height / textSizeInt

            val resultArray = IntArray(width * height)
            val job: ArrayList<Deferred<List<Unit>>> = arrayListOf()

            for (y in 0 until textBitmapHeight) {
                job.add(async { fetchRows(intArray, resultArray, textBitmapWidth, width, y) })
            }
            job.awaitAll()
            val newBitmap = Bitmap.createBitmap(resultArray, width, height, Bitmap.Config.ARGB_8888)

            newBitmap
        }

    private suspend fun fetchRows(
        intArray: IntArray,
        resultArray: IntArray,
        textBitmapWidth: Int,
        width: Int,
        y: Int
    ) =
        withContext(Dispatchers.Default) {
            val job: ArrayList<Deferred<Unit>> = arrayListOf()
            for (x in 0 until textBitmapWidth)
                job.add(async {
                    val pixel = getAveragePixel(
                        range = textSizeInt,
                        width = width,
                        xStart = x * textSizeInt,
                        yStart = y * textSizeInt,
                        pixels = intArray
                    )
                    val char = filterPixelToCharIntArray(pixel)
                    addToResultArray(x, y, width, textSizeInt, char, resultArray)
                })
            job.awaitAll()
        }

    private fun addToResultArray(
        x: Int,
        y: Int,
        width: Int,
        textSizeInt: Int,
        char: IntArray,
        resultArray: IntArray
    ) {
        if (isNativeLibAvailable){
            addToResultArrayNative(x,y,width,textSizeInt,char,resultArray)
        }else {
            char.forEachIndexed { index, i ->
                val xx = xFromOneD(index, textSizeInt)
                val yy = yFromOneD(index, textSizeInt)
                val mainIndex =
                    TwoDtoOneD((x * textSizeInt) + xx, (y * textSizeInt) + yy, width)
                resultArray[mainIndex] = i
            }
        }
    }

    private fun getAveragePixel(
        range: Int,
        width: Int,
        xStart: Int,
        yStart: Int,
        pixels: IntArray
    ): Int {
        var array = IntArray(range * range)

        return if (isNativeLibAvailable) {
            getSubPixelsNative(width, xStart, yStart, range, range, pixels, array)
            calculateAvgColorNative(array, array.size)
        } else {
            array = pixels.getSubPixels(width, xStart, yStart, range, range)
            calculateAvgColor(array)
        }

    }

    private fun calculateAvgColor(array: IntArray): Int {
        var r = 0
        var g = 0
        var b = 0

        array.forEach {
            r += Color.red(it)
            b += Color.blue(it)
            g += Color.green(it)
        }

        r /= array.size
        g /= array.size
        b /= array.size

        return Color.argb(255, r, g, b)
    }

    suspend fun imageProxyToTextBitmap(imageProxy: ImageProxy) = withContext(Dispatchers.Default) {

        val startMillis = System.currentTimeMillis()

        val planes = imageProxy.planes
        val buffer = planes[0].buffer

        var outPutArray = IntArray(imageProxy.width * imageProxy.height)

        if (isNativeLibAvailable)
            convertByteArrayToRgbNative(
                convertByteBufferToByteArray(buffer),
                outPutArray,
                imageProxy.width,
                imageProxy.height
            ) else
            outPutArray = convertRgbaToRgb(
                convertByteBufferToByteArray(buffer),
                imageProxy.width,
                imageProxy.height
            )

        imageProxy.close()

        val textBitmap = rgbArrayToTextBitmap(outPutArray, imageProxy.width, imageProxy.height)

        Timber.d("Processed frame in ${System.currentTimeMillis() - startMillis}")

        _generatedBitmapState.emit(textBitmap)

    }

    private fun convertByteBufferToByteArray(buffer: ByteBuffer): ByteArray {
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    /*private suspend fun filterPixelToChar(pixel: Int): CharData {
        val brightness = ColorUtils.calculateLuminance(pixel)
        val densityLength = density.length
        val charIndex = brightness.toFloat().map(0f, 1f, 0, densityLength)
        val index = densityLength - charIndex - 1
        val sliceChar = densityIntArray.getSubPixels(
            densityLength * textSizeInt,
            index * textSizeInt,
            0,
            textSizeInt,
            textSizeInt
        )
        yield()
        return CharData(
            char = density[index],
            charIntArray = sliceChar.toList(),
            colorFg = fgColor,
            colorBg = bgColor
        )
    }*/

    private suspend fun filterPixelToCharIntArray(pixel: Int): IntArray {
        val densityLength = density.length
        val index: Int = if (isNativeLibAvailable) {
            calculateDensityIndexNative(pixel, densityLength)
        } else {
            calculateDensityIndex(pixel, densityLength)
        }
        yield()
        return densityIntArray[index]
    }

    private fun calculateDensityIndex(pixel: Int, densityLength: Int): Int {
        val brightness = ColorUtils.calculateLuminance(pixel)
        val charIndex = brightness.toFloat().map(0f, 1f, 0, densityLength)
        return densityLength - charIndex - 1
    }

}

