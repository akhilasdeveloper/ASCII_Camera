package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.graphics.*
import android.text.TextPaint
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

    private var width = 0
    private var height = 0

    private var textBitmapWidth = 0
    private var textBitmapHeight = 0

    private var resultArray = IntArray(width * height)
    private var asciiIndexArray = IntArray(textBitmapWidth * textBitmapHeight)
    private var asciiColorArray = IntArray(textBitmapWidth * textBitmapHeight)

    private val runtimeCalculator = RuntimeCalculator()
    private var dispatcher = Dispatchers.Default

    fun setDispatcher(dispatcher: CoroutineDispatcher) {
        this.dispatcher = dispatcher
    }

    fun changeFilter(filters: AsciiFilters) {
        this.filters = filters
        textSize = filters.textCharSize
        textSizeInt = filters.textCharSize.toInt()
        density = filters.density
        fgColor = filters.fgColor
        bgColor = filters.bgColor
        _densityIntArray = ByteArray(1)
    }

    private var _densityIntArray: ByteArray = byteArrayOf()

    private val densityIntArray: ByteArray
        get() {
            if (_densityIntArray.isEmpty())
                _densityIntArray = generateDensityBytes()

            return _densityIntArray
        }

    fun setWidthAndHeight(width: Int, height: Int) {
        this.width = width
        this.height = height

        textBitmapWidth = width / textSizeInt
        textBitmapHeight = height / textSizeInt

        resultArray = IntArray(width * height)
        asciiIndexArray = IntArray(textBitmapWidth * textBitmapHeight)
        asciiColorArray = IntArray(textBitmapWidth * textBitmapHeight)
    }

    private fun generateDensityBytes(): ByteArray {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            textSizeInt,
            textSizeInt * density.length, Bitmap.Config.ARGB_8888
        )
        val paint = TextPaint()
        val textBounds: Rect = Rect()
        paint.textSize = textSize
        paint.isAntiAlias = false
        paint.color = Color.WHITE
        canvas.setBitmap(bitmap)
        canvas.drawColor(Color.BLACK)

        density.toCharArray().forEachIndexed { index, c ->
            paint.getTextBounds(c.toString(), 0, 1, textBounds)
            canvas.drawText(
                c.toString(),
                ((textSize / 2f) - textBounds.exactCenterX()),
                (index * textSize) + ((textSize / 2f) - textBounds.exactCenterY()),
                paint
            )
        }

        val byteArray = ByteArray(bitmap.width * bitmap.height)
        bitmap.getAllPixelsBytes(byteArray)

        return byteArray
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

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun rgbArrayToTextBitmap(intArray: ByteArray, width: Int) =
        withContext(dispatcher) {

            val job: ArrayList<Deferred<Unit>> = arrayListOf()

            for (y in 0 until textBitmapHeight) {
                for (x in 0 until textBitmapWidth)
                    job.add(GlobalScope.async {
                        if (isNativeLibAvailable)
                            reducePixelsNative(
                                x,
                                y,
                                width,
                                textBitmapWidth,
                                textSizeInt,
                                intArray,
                                asciiColorArray,
                                asciiIndexArray,
                                density.length
                            )
                        else
                            reducePixels(
                                x,
                                y,
                                width,
                                textBitmapWidth,
                                textSizeInt,
                                intArray,
                                asciiColorArray,
                                asciiIndexArray,
                                density.length
                            )
                    })
            }

            job.awaitAll()

            Timber.d("textBitmapWidth:textBitmapHeight $textBitmapWidth:$textBitmapHeight")

            asciiIndexArray.forEachIndexed { index, i ->
                val x = xFromOneD(index, textBitmapWidth)
                val y = yFromOneD(index, textBitmapWidth)
                val sliceStart = i * textSizeInt * textSizeInt
                val sliceEnd = sliceStart + (textSizeInt * textSizeInt)
                addToResultArray(
                    x,
                    y,
                    width,
                    textSizeInt,
                    densityIntArray.sliceArray(sliceStart until sliceEnd),
                    resultArray
                )
            }

            val newBitmap = Bitmap.createBitmap(
                resultArray,
                width,
                height,
                Bitmap.Config.ARGB_8888
            )

            newBitmap
        }

    private fun reducePixels(
        x: Int,
        y: Int,
        width: Int,
        textBitmapWidth: Int,
        textSizeInt: Int,
        intArray: ByteArray,
        asciiColorArray: IntArray,
        asciiIndexArray: IntArray,
        densityLength: Int
    ) {
        val arraySize: Int = textSizeInt * textSizeInt
        val xStart: Int = x * textSizeInt
        val yStart: Int = y * textSizeInt

        val yEnd: Int = yStart + textSizeInt
        val xEnd: Int = xStart + textSizeInt

        var a = 0
        var r = 0
        var g = 0
        var b = 0

        val offset: Int = 4

        for (i in yStart until yEnd) {
            for (j in i * width + xStart until i * width + xEnd) {
                val index: Int = offset * j
                r += intArray[index].toInt() and 0xff
                g += intArray[index + 1].toInt() and 0xff
                b += intArray[index + 2].toInt() and 0xff
                a += intArray[index + 3].toInt() and 0xff
            }
        }

        a /= arraySize
        r /= arraySize
        g /= arraySize
        b /= arraySize

        val result = a shl 24 or (r shl 16) or (g shl 8) or b
        val densityIndex = calculateDensityIndex(result, densityLength)
        val index = x + textBitmapWidth * y

        asciiIndexArray[index] = densityIndex
        asciiColorArray[index] = result
    }

    private external fun reducePixelsNative(
        x: Int,
        y: Int,
        width: Int,
        textBitmapWidth: Int,
        textSizeInt: Int,
        intArray: ByteArray,
        asciiColorArray: IntArray,
        asciiIndexArray: IntArray,
        densityLength: Int
    )

    private fun addToResultArray(
        x: Int,
        y: Int,
        width: Int,
        textSizeInt: Int,
        char: ByteArray,
        resultArray: IntArray
    ) {
        if (isNativeLibAvailable) {
            addToResultArrayNative(
                x,
                y,
                width,
                textSizeInt,
                Color.BLACK,
                Color.WHITE,
                char,
                resultArray
            )
        } else {
            char.forEachIndexed { index, i ->
                val xx = xFromOneD(index, textSizeInt)
                val yy = yFromOneD(index, textSizeInt)
                val mainIndex =
                    TwoDtoOneD((x * textSizeInt) + xx, (y * textSizeInt) + yy, width)
                resultArray[mainIndex] = if (i != 0.toByte()) Color.WHITE else Color.BLACK
            }
        }
    }

    suspend fun imageProxyToTextBitmap(imageProxy: ImageProxy) = withContext(dispatcher) {

        runtimeCalculator.start("imageProxyToTextBitmap")

        val planes = imageProxy.planes
        val buffer = planes[0].buffer

        val outPutArray = convertByteBufferToByteArray(buffer)
        imageProxy.close()

        val textBitmap = rgbArrayToTextBitmap(outPutArray, width)
        runtimeCalculator.finish("imageProxyToTextBitmap")

        textBitmap

    }

    private fun convertByteBufferToByteArray(buffer: ByteBuffer): ByteArray {
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    private fun calculateDensityIndex(pixel: Int, densityLength: Int): Int {
        val brightness = ColorUtils.calculateLuminance(pixel)
        val charIndex = brightness.toFloat().map(0f, 1f, 0, densityLength)
        return densityLength - charIndex - 1
    }

}

