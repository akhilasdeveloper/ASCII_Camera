package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.graphics.*
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import com.akhilasdeveloper.asciicamera.util.*
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

    private val runtimeCalculator = RuntimeCalculator()
    fun changeFilter(filters: AsciiFilters) {
        this.filters = filters
        textSize = filters.textCharSize
        textSizeInt = filters.textCharSize.toInt()
        density = filters.density
        fgColor = filters.fgColor
        bgColor = filters.bgColor
        _densityIntArray = listOf()
    }

    private var _densityIntArray: List<ByteArray> = listOf()
    private val densityIntArray: List<ByteArray>
        get() {
            if (_densityIntArray.isEmpty())
                _densityIntArray = generateDensityBytes()

            return _densityIntArray
        }

    private fun generateDensityBytes(): List<ByteArray> {
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
        val byteArray = ByteArray(bitmap.width * bitmap.height)
        bitmap.getAllPixels(array)

        array.forEachIndexed { index, i ->
            byteArray[index] = (i and 0xff).toByte()
        }

        val densityLength = density.length
        val result = mutableListOf<ByteArray>()

        for (index in density.indices) {
            var sliceChar = ByteArray(textSizeInt * textSizeInt)
            if (isNativeLibAvailable) {
                getSubPixelsBytesNative(
                    width = densityLength * textSizeInt,
                    xStart = index * textSizeInt,
                    yStart = 0,
                    destWidth = textSizeInt,
                    destHeight = textSizeInt,
                    array = byteArray,
                    resultArray = sliceChar
                )
            } else {
                sliceChar =
                    byteArray.getSubPixels(
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

    private external fun getSubPixelsBytesNative(
        width: Int,
        xStart: Int,
        yStart: Int,
        destWidth: Int,
        destHeight: Int,
        array: ByteArray,
        resultArray: ByteArray
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
        bgColor: Int,
        fgColor: Int,
        char: ByteArray,
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

    private suspend fun rgbArrayToTextBitmap(intArray: ByteArray, width: Int, height: Int) =
        withContext(Dispatchers.Default) {

            val textBitmapWidth = width / textSizeInt
            val textBitmapHeight = height / textSizeInt

            val resultArray = IntArray(width * height)
            val resultAvgArray = IntArray(textBitmapWidth * textBitmapHeight)
            var index = 0
            val job: ArrayList<Deferred<Int>> = arrayListOf()

            for (y in 0 until textBitmapHeight) {
                for (x in 0 until textBitmapWidth)
                    job.add(async {
                        reducePixelsNative(x, y, width, textSizeInt, intArray)
                    })
            }

            val newBitmap = Bitmap.createBitmap(job.awaitAll().toIntArray(), textBitmapWidth, textBitmapHeight, Bitmap.Config.ARGB_8888)

            newBitmap
        }

    private external fun reducePixelsNative(
        x: Int,
        y: Int,
        width: Int,
        textSizeInt: Int,
        intArray: ByteArray
    ):Int

    private suspend fun fetchRows(
        intArray: ByteArray,
        resultArray: IntArray,
        textBitmapWidth: Int,
        width: Int,
        y: Int
    ) =
        withContext(Dispatchers.Default) {

        }

    private fun addToResultArray(
        x: Int,
        y: Int,
        width: Int,
        textSizeInt: Int,
        char: ByteArray,
        resultArray: IntArray
    ) {
        if (isNativeLibAvailable){
            addToResultArrayNative(x,y,width,textSizeInt,Color.BLACK, Color.WHITE, char,resultArray)
        }else {
            char.forEachIndexed { index, i ->
                val xx = xFromOneD(index, textSizeInt)
                val yy = yFromOneD(index, textSizeInt)
                val mainIndex =
                    TwoDtoOneD((x * textSizeInt) + xx, (y * textSizeInt) + yy, width)
                resultArray[mainIndex] = if (i!=0.toByte()) Color.WHITE else Color.BLACK
            }
        }
    }

    /*private fun getAveragePixel(
        range: Int,
        width: Int,
        xStart: Int,
        yStart: Int,
        pixels: IntArray
    ): Int {


    }*/

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

        runtimeCalculator.start("imageProxyToTextBitmap")

        val planes = imageProxy.planes
        val buffer = planes[0].buffer

        val outPutArray = convertByteBufferToByteArray(buffer)
//        imageProxy.toBitmap()?.getAllPixels(outPutArray)
        /*if (isNativeLibAvailable)
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
            )*/
        imageProxy.close()

        runtimeCalculator.finish("imageProxyToTextBitmap")

        val textBitmap = rgbArrayToTextBitmap(outPutArray, imageProxy.width, imageProxy.height)

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

    private suspend fun filterPixelToCharIntArray(pixel: Int): ByteArray {
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

