package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.content.res.Resources
import android.graphics.*
import android.text.TextPaint
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import com.akhilasdeveloper.asciicamera.util.RuntimeCalculator
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.ANSI_RATIO
import com.akhilasdeveloper.asciicamera.util.getAllPixelsBytes
import com.akhilasdeveloper.asciicamera.util.map
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    private var filters: AsciiFilters = AsciiFilters.OriginalColor
    private var textSize = filters.textCharSize
    private var textSizeInt = filters.textCharSize.toInt()
    private var pixelAvgSize = 6 //
    private var density = filters.density
    private var fgColor = filters.fgColor
    private var bgColor = filters.bgColor
    private var colorType = AsciiFilters.COLOR_TYPE_NONE

    private var width = 0
    private var height = 0

    private var resWidth = 0 //
    private var resHeight = 0 //

    private var textBitmapWidth = 1
    private var textBitmapHeight = 1

    private var avgBitmapWidth = 1 //
    private var avgBitmapHeight = 1 //

    private var resultArray = IntArray(width * height)
    private var asciiIndexArray = IntArray(avgBitmapWidth * avgBitmapHeight)
    private var asciiColorArray = IntArray(avgBitmapWidth * avgBitmapHeight)

    private val runtimeCalculator = RuntimeCalculator()
    private var dispatcher = Dispatchers.Default

    private var mListener: OnGeneratedListener? = null
    private var lastBitmap: Bitmap? = null
    var isCapturedState = false
        private set

    fun continueStream() {
        isCapturedState = false
        mListener?.onContinue()
    }

    fun capture() {
        isCapturedState = true
        mListener?.onCapture(lastBitmap)
    }

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

        colorType = when (filters) {
            is AsciiFilters.Custom -> {
                filters.specs.fgColorType
            }
            is AsciiFilters.ANSI -> {
                AsciiFilters.COLOR_TYPE_ANSI
            }
            is AsciiFilters.OriginalColor -> {
                AsciiFilters.COLOR_TYPE_ORIGINAL
            }
            else -> {
                AsciiFilters.COLOR_TYPE_NONE
            }
        }

        _densityIntArray = byteArrayOf()
    }

    private var _densityIntArray: ByteArray = byteArrayOf()

    private val densityByteArray: ByteArray
        get() {
            if (_densityIntArray.isEmpty())
                _densityIntArray = generateDensityBytes()

            return _densityIntArray
        }

    private fun setWidthAndHeight(width: Int, height: Int) {

        calculatePixelAvg(width, height)

        if (this.width != avgBitmapWidth * pixelAvgSize || this.height != avgBitmapHeight * pixelAvgSize) {

            textBitmapWidth = width / textSizeInt
            textBitmapHeight = height / textSizeInt

            avgBitmapWidth = width / pixelAvgSize
            avgBitmapHeight = height / pixelAvgSize

            this.width = avgBitmapWidth * pixelAvgSize
            this.height = avgBitmapHeight * pixelAvgSize

            this.resWidth = avgBitmapWidth * textSizeInt
            this.resHeight = avgBitmapHeight * textSizeInt

            resultArray = IntArray(this.resWidth * this.resHeight)
            asciiIndexArray = IntArray(avgBitmapWidth * avgBitmapHeight)
            asciiColorArray = IntArray(avgBitmapWidth * avgBitmapHeight)
        }
    }

    /**
     * This function converts string density into byte array. First it draws all chars into a vertical canvas. It will be white on black background.
     * Then it can be converted into a byte array. byte 1 representing white pixel and byte 0 representing black.
     * Using this template we can generate chars in efficient way but sacrifices char quality.
     */
    private fun generateDensityBytes(): ByteArray {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            textSizeInt,
            textSizeInt * density.length, Bitmap.Config.ARGB_8888
        )

        val paint = TextPaint()
        val textBounds: Rect = Rect()
        paint.textSize = textSize
        canvas.drawARGB(255, 0, 0, 0)
        paint.color = -0x1
        canvas.setBitmap(bitmap)

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


    private suspend fun rgbArrayToTextBitmap(intArray: ByteArray, width: Int) =
        withContext(dispatcher) {

            if (isNativeLibAvailable) {
                reducePixelsNative2(
                    width,
                    height,
                    avgBitmapWidth,
                    pixelAvgSize,
                    intArray,
                    asciiColorArray,
                    asciiIndexArray,
                    density.length,
                    colorType,
                    ANSI_RATIO,
                    fgColor
                )

                generateResultNative2(
                    asciiIndexArray,
                    asciiColorArray,
                    avgBitmapWidth,
                    avgBitmapHeight,
                    textSizeInt,
                    densityByteArray,
                    resultArray,
                    resWidth,
                    bgColor
                )
            } else {

                reducePixels2(
                    width,
                    height,
                    avgBitmapWidth,
                    pixelAvgSize,
                    intArray,
                    asciiColorArray,
                    asciiIndexArray,
                    density.length,
                    colorType,
                    fgColor
                )

                generateResult(
                    asciiIndexArray,
                    asciiColorArray,
                    avgBitmapWidth,
                    avgBitmapHeight,
                    textSizeInt,
                    densityByteArray,
                    resultArray,
                    resWidth,
                    bgColor
                )
            }

//            val rotatedArray = rotateArray(resultArray, height, width)

            val newBitmap = Bitmap.createBitmap(
                resultArray,
                resWidth,
                resHeight,
                Bitmap.Config.ARGB_8888
            )

            newBitmap

        }

    private external fun generateResultNative2(
        asciiIndexArray: IntArray,
        asciiColorArray: IntArray,
        avgBitmapWidth: Int,
        avgBitmapHeight: Int,
        textSizeInt: Int,
        densityIntArray: ByteArray,
        resultArray: IntArray,
        resultWidth: Int,
        bgColor: Int
    )

    /**
     * After getting the pixel index array and color details obtained from the reducePixels2 function, we will populate the result array from the density array template.
     */
    private fun generateResult(
        ascii_index_array: IntArray,
        ascii_color_array: IntArray,
        avg_bitmap_width: Int,
        avg_bitmap_height: Int,
        text_size_int: Int,
        density_byte_array: ByteArray,
        result_array: IntArray,
        result_width: Int,
        bg_color: Int
    ) {

        for (index in 0 until (text_size_int * text_size_int * avg_bitmap_width * avg_bitmap_height)) {

            val x = index % result_width
            val y = index / result_width

            val asciiIndexX = x / text_size_int
            val asciiIndexY = y / text_size_int
            val asciiIndexIndex = asciiIndexX + avg_bitmap_width * asciiIndexY
            val asciiIndex = ascii_index_array[asciiIndexIndex]

            val asciiArrayIndexX = x % text_size_int
            val asciiArrayIndexY = y % text_size_int
            val asciiArrayIndex = asciiArrayIndexX + text_size_int * asciiArrayIndexY
            val ascii =
                density_byte_array[asciiArrayIndex + (asciiIndex * text_size_int * text_size_int)]

            result_array[index] =
                if (ascii != 0.toByte()) ascii_color_array[asciiIndexIndex] else bg_color

        }

    }

    private external fun reducePixelsNative2(
        width: Int,
        height: Int,
        avgBitmapWidth: Int,
        pixelAvgSize: Int,
        intArray: ByteArray,
        asciiColorArray: IntArray,
        asciiIndexArray: IntArray,
        densityLength: Int,
        colorType: Int,
        ansiRatio: Float,
        fgColor: Int
    )

    /**
     * This function averages the pixels from byte array within the size of textSizeInt. using that result pixel, we will determine the density character.
     *
     * Density character is not drawn directly. We will first create a bitmap of all the characters arranged in vertical. And convert the bitmap into byte array.
     * Means the character is drawn as black on white. we can consider byte 1 as white and byte 0 as black.
     * If we convert the bitmap into byte array, we can store the pixels like this as 1 bit value. This will result better performance but will lose character quality.
     *
     * We will iterate through each positions of bytearray. each position will contain 4 values. rgba. We will add all the r,g,b,a values individually and take average.
     * Using the average pixel, will will find the density index and stores to asciiIndexArray. Color value will be stored to asciiColorArray.
     */
    private fun reducePixels2(
        width: Int,
        height: Int,
        avgBitmapWidth: Int,
        pixelAvgSize: Int,
        intArray: ByteArray,
        asciiColorArray: IntArray,
        asciiIndexArray: IntArray,
        densityLength: Int,
        colorType: Int,
        fgColor: Int
    ) {

        val arraySize: Int = pixelAvgSize * pixelAvgSize

        /**
         * Byte array will contain 4 fields for each corresponding position of result array rgba. rowArray will calculate store averages of rgba in row wise.
         * eg: first 4 position will store sum of rgba values separately of 1st pixels (width of textIntSize and height of textIntSize)
         */
        val rowArray = IntArray(avgBitmapWidth * 4)

        //loops through results array indexes
        for (index in 0 until width * height) {
            //Calculates x and y of the current result array index
            val y = index / width
            val x = index % width

            //Calculates col and row of colorArray/asciiIndexArray
            val col = x / pixelAvgSize
            val row = y / pixelAvgSize
            //multiplied by 4 because rgba values are represented.
            val rowArrayCol = col * 4

            rowArray[rowArrayCol] += intArray[index * 4].toInt() and 0xff
            rowArray[rowArrayCol + 1] += intArray[index * 4 + 1].toInt() and 0xff
            rowArray[rowArrayCol + 2] += intArray[index * 4 + 2].toInt() and 0xff
            rowArray[rowArrayCol + 3] += intArray[index * 4 + 3].toInt() and 0xff

            //If y and x reaches multiple of textSizeInt, it means it has the sum of required pixels for that position.
            // Now we can calculate the average of the pixel and reset the rowArray values to 0 for calculating next row.
            if ((y + 1) % pixelAvgSize == 0 && (x + 1) % pixelAvgSize == 0) {
                var r = rowArray[rowArrayCol] / arraySize
                var g = rowArray[rowArrayCol + 1] / arraySize
                var b = rowArray[rowArrayCol + 2] / arraySize
                val a = rowArray[rowArrayCol + 3] / arraySize

                rowArray[rowArrayCol] = 0
                rowArray[rowArrayCol + 1] = 0
                rowArray[rowArrayCol + 2] = 0
                rowArray[rowArrayCol + 3] = 0

                val result = a shl 24 or (r shl 16) or (g shl 8) or b
                val densityIndex = calculateDensityIndex(result, densityLength)
                val ind = col + avgBitmapWidth * row
                asciiIndexArray[ind] = densityIndex

                if (colorType == AsciiFilters.COLOR_TYPE_NONE) {
                    asciiColorArray[ind] = fgColor
                    continue
                }

                if (colorType == AsciiFilters.COLOR_TYPE_ORIGINAL) {
                    asciiColorArray[ind] = result
                    continue
                }

                if (colorType == AsciiFilters.COLOR_TYPE_ANSI) {

                    val maxRG: Int = if (r > g) r else g
                    val maxColor = if (b > maxRG) b else maxRG
                    if (maxColor > 0) {
                        val threshold: Int = (maxColor * ANSI_RATIO).toInt()
                        r = if (r >= threshold) r else 0
                        g = if (g >= threshold) g else 0
                        b = if (b >= threshold) b else 0
                    }
                    val ansiResult = a shl 24 or (r shl 16) or (g shl 8) or b
                    asciiColorArray[ind] = ansiResult
                }

            }
        }

    }

    suspend fun imageBitmapToTextBitmap(imageProxy: Bitmap): Bitmap? = withContext(dispatcher) {

        if (!isCapturedState) {

            setWidthAndHeight(imageProxy.width, imageProxy.height)

            runtimeCalculator.start("imageProxyToTextBitmap")

            val outPutArray = imageProxy.getAllPixelsByteArray()

            val croppedArray = ByteArray(width * height * 4)
            if (isNativeLibAvailable)
                cropArrayNative(
                    outPutArray,
                    outPutArray.size,
                    imageProxy.width,
                    croppedArray,
                    width,
                    height
                )
            else
                cropArray(outPutArray, imageProxy.width, croppedArray, width, height)

            Timber.d("image width:height $width:$height")
            Timber.d("image avgBitmapWidth:avgBitmapHeight $avgBitmapWidth:$avgBitmapHeight")
            Timber.d("image avgPixels $pixelAvgSize")

            lastBitmap = rgbArrayToTextBitmap(croppedArray, width)
            runtimeCalculator.finish("imageProxyToTextBitmap")
        }
        lastBitmap
    }

    private fun calculatePixelAvg(sWidth: Int, sHeight: Int) {
        val dWidth = getScreenWidth()
        val dHeight = getScreenHeight()

        val possibleMaxWidth = dWidth / textSizeInt
        val possibleMaxHeight = dHeight / textSizeInt

        val avgSizeHeight = sHeight / possibleMaxHeight
        val avgSizeWidth = sWidth / possibleMaxWidth

        pixelAvgSize = if (avgSizeHeight > avgSizeWidth) avgSizeHeight else avgSizeWidth
        if (pixelAvgSize <= 0) pixelAvgSize = 1
    }

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    suspend fun imageProxyToTextBitmap(imageProxy: ImageProxy): Bitmap? = withContext(dispatcher) {

        if (!isCapturedState) {

            setWidthAndHeight(imageProxy.width, imageProxy.height)

            runtimeCalculator.start("imageProxyToTextBitmap")

            val planes = imageProxy.planes
            val buffer = planes[0].buffer

            val outPutArray = convertByteBufferToByteArray(buffer)
            imageProxy.close()
            val croppedArray = ByteArray(width * height * 4)
            if (isNativeLibAvailable)
                cropArrayNative(
                    outPutArray,
                    outPutArray.size,
                    imageProxy.width,
                    croppedArray,
                    width,
                    height
                )
            else
                cropArray(outPutArray, imageProxy.width, croppedArray, width, height)

            Timber.d("image width:height $width:$height")
            Timber.d("image avgBitmapWidth:avgBitmapHeight $avgBitmapWidth:$avgBitmapHeight")

            lastBitmap = rgbArrayToTextBitmap(croppedArray, width)
            runtimeCalculator.finish("imageProxyToTextBitmap")
        }
        lastBitmap
    }

    private external fun cropArrayNative(
        outPutArray: ByteArray,
        outPutArraySize: Int,
        width: Int,
        croppedArray: ByteArray,
        width1: Int,
        height1: Int
    )

    private fun cropArray(
        outPutArray: ByteArray,
        width: Int,
        croppedArray: ByteArray,
        width1: Int,
        height1: Int
    ) {

        for ((index, byte) in outPutArray.withIndex()) {
            val x = index % (width * 4)
            val y = index / (width * 4)
            val ind = x + width1 * 4 * y

            if (x >= width1 * 4 || y >= height1)
                continue

            croppedArray[ind] = byte
        }

    }

    fun rotateByteArrayImage(
        outPutArray: ByteArray,
        rotatedArray: ByteArray,
        width: Int,
        height: Int
    ) {
        for (x in 0 until width) {
            for (y in 0 until height) {

                val destX = width - y
                val destY = x
                // Calculate the source and destination indices
                val srcIndex = (y * width + x) * 4
                val destIndex = (destY * height + destX) * 4


                // Copy the pixel values from the source to the destination
                rotatedArray[destIndex] =
                    outPutArray[srcIndex]
                rotatedArray[destIndex + 1] = outPutArray[srcIndex + 1]
                rotatedArray[destIndex + 2] = outPutArray[srcIndex + 2]
                rotatedArray[destIndex + 3] = outPutArray[srcIndex + 3]

            }
        }
    }

    fun rotateArray(array: IntArray, rows: Int, cols: Int): IntArray {
        val result = IntArray(rows * cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                // Compute the index of the current element in the rotated array
                val rotatedIndex = j * rows + (rows - i - 1)
                // Copy the element to the rotated array
                result[rotatedIndex] = array[i * cols + j]
            }
        }
        return result
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

    fun setAsciiGeneratedListener(eventListener: OnGeneratedListener) {
        mListener = eventListener
    }

    interface OnGeneratedListener {
        fun onContinue()
        fun onCapture(bitmap: Bitmap?)
    }

    private fun Bitmap.getAllPixelsByteArray(): ByteArray {
        val width: Int = this.getWidth()
        val height: Int = this.getHeight()
        val pixels = IntArray(width * height)
        this.getPixels(pixels, 0, width, 0, 0, width, height)
        val argb8888Bytes = ByteArray(width * height * 4)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            argb8888Bytes[i * 4] = (pixel shr 16 and 0xFF).toByte() // alpha
            argb8888Bytes[i * 4 + 1] = (pixel shr 8 and 0xFF).toByte() // red
            argb8888Bytes[i * 4 + 2] = (pixel and 0xFF).toByte() // green
            argb8888Bytes[i * 4 + 3] = (pixel shr 24 and 0xFF).toByte() // blue
        }

        return argb8888Bytes
    }

}



