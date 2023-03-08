package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.graphics.*
import android.text.TextPaint
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import com.akhilasdeveloper.asciicamera.util.*
import kotlinx.coroutines.*
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

    private var textBitmapWidth = 1
    private var textBitmapHeight = 1

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

    private val densityByteArray: ByteArray
        get() {
            if (_densityIntArray.isEmpty())
                _densityIntArray = generateDensityBytes()

            return _densityIntArray
        }

    private fun setWidthAndHeight(width: Int, height: Int) {

        if (this.width != textBitmapWidth * textSizeInt || this.height != textBitmapHeight * textSizeInt) {

            textBitmapWidth = width / textSizeInt
            textBitmapHeight = height / textSizeInt

            this.width = textBitmapWidth * textSizeInt
            this.height = textBitmapHeight * textSizeInt

            resultArray = IntArray(this.width * this.height)
            asciiIndexArray = IntArray(textBitmapWidth * textBitmapHeight)
            asciiColorArray = IntArray(textBitmapWidth * textBitmapHeight)
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
        paint.textSize = textSize - 1
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

    private suspend fun rgbArrayToTextBitmap(intArray: ByteArray, width: Int) =
        withContext(dispatcher) {

            if (isNativeLibAvailable){
                reducePixelsNative2(
                    width,
                    height,
                    textBitmapWidth,
                    textSizeInt,
                    intArray,
                    asciiColorArray,
                    asciiIndexArray,
                    density.length
                )

                generateResultNative2(
                    asciiIndexArray,
                    asciiIndexArray.size,
                    textBitmapWidth,
                    textBitmapHeight,
                    textSizeInt,
                    densityByteArray,
                    resultArray,
                    width,
                    fgColor,
                    bgColor
                )
            }else{
                reducePixels2(
                    width,
                    height,
                    textBitmapWidth,
                    textSizeInt,
                    intArray,
                    asciiColorArray,
                    asciiIndexArray,
                    density.length
                )

                generateResult(
                    asciiIndexArray,
                    textBitmapWidth,
                    textBitmapHeight,
                    textSizeInt,
                    densityByteArray,
                    resultArray,
                    width,
                    fgColor,
                    bgColor
                )
            }


            val newBitmap = Bitmap.createBitmap(
                resultArray,
                width,
                height,
                Bitmap.Config.ARGB_8888
            )


            /*val matrix = Matrix()
            matrix.postRotate(90f)

            Bitmap.createBitmap(newBitmap, 0, 0, newBitmap.width, newBitmap.height, matrix, true)*/

            newBitmap

        }

    private external fun generateResultNative2(
        asciiIndexArray: IntArray,
        asciiIndexArraySize: Int,
        textBitmapWidth: Int,
        textBitmapHeight: Int,
        textSizeInt: Int,
        densityIntArray: ByteArray,
        resultArray: IntArray,
        resultWidth: Int,
        fgColor: Int,
        bgColor: Int
    )

    /**
     * After getting the pixel index array and color details obtained from the reducePixels2 function, we will populate the result array from the density array template.
     */
    private fun generateResult(
        ascii_index_array: IntArray,
        text_bitmap_width: Int,
        text_bitmap_height: Int,
        text_size_int: Int,
        density_byte_array: ByteArray,
        result_array: IntArray,
        result_width: Int,
        fg_color: Int,
        bg_color: Int
    ) {

        for (index in 0 until (text_size_int * text_size_int * text_bitmap_width * text_bitmap_height)) {

            val x = index % result_width
            val y = index / result_width

            val asciiIndexX = x / text_size_int
            val asciiIndexY = y / text_size_int
            val asciiIndexIndex = asciiIndexX + text_bitmap_width * asciiIndexY
            val asciiIndex = ascii_index_array[asciiIndexIndex]

            val asciiArrayIndexX = x % text_size_int
            val asciiArrayIndexY = y % text_size_int
            val asciiArrayIndex = asciiArrayIndexX + text_size_int * asciiArrayIndexY
            val ascii =
                density_byte_array[asciiArrayIndex + (asciiIndex * text_size_int * text_size_int)]

            result_array[index] = if (ascii != 0.toByte()) fg_color else bg_color

        }

    }

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
        textBitmapWidth: Int,
        textSizeInt: Int,
        intArray: ByteArray,
        asciiColorArray: IntArray,
        asciiIndexArray: IntArray,
        densityLength: Int
    ) {

        val arraySize: Int = textSizeInt * textSizeInt

        /**
         * Byte array will contain 4 fields for each corresponding position of result array rgba. rowArray will calculate store averages of rgba in row wise.
         * eg: first 4 position will store sum of rgba values separately of 1st pixels (width of textIntSize and height of textIntSize)
         */
        val rowArray = IntArray(textBitmapWidth * 4)

        //loops through results array indexes
        for (index in 0 until width*height) {
            //Calculates x and y of the current result array index
            val y = index / width
            val x = index % width

            //Calculates col and row of colorArray/asciiIndexArray
            val col = x / textSizeInt
            val row = y / textSizeInt
            //multiplied by 4 because rgba values are represented.
            val rowArrayCol = col * 4

            rowArray[rowArrayCol] += intArray[index * 4].toInt() and 0xff
            rowArray[rowArrayCol + 1] += intArray[index * 4 + 1].toInt() and 0xff
            rowArray[rowArrayCol + 2] += intArray[index * 4 + 2].toInt() and 0xff
            rowArray[rowArrayCol + 2] += intArray[index * 4 + 3].toInt() and 0xff

            //If y and x reaches multiple of textSizeInt, it means it has the sum of required pixels for that position.
            // Now we can calculate the average of the pixel and reset the rowArray values to 0 for calculating next row.
            if ((y + 1) % textSizeInt == 0 && (x + 1) % textSizeInt == 0) {
                val r = rowArray[rowArrayCol] / arraySize
                val g = rowArray[rowArrayCol + 1] / arraySize
                val b = rowArray[rowArrayCol + 2] / arraySize
                val a = rowArray[rowArrayCol + 3] / arraySize

                rowArray[rowArrayCol] = 0
                rowArray[rowArrayCol + 1] = 0
                rowArray[rowArrayCol + 2] = 0
                rowArray[rowArrayCol + 3] = 0

                val result = a shl 24 or (r shl 16) or (g shl 8) or b
                val densityIndex = calculateDensityIndex(result, densityLength)
                val ind = col + textBitmapWidth * row
                asciiColorArray[ind] = result
                asciiIndexArray[ind] = densityIndex
            }
        }

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

    private external fun reducePixelsNative2(
        width: Int,
        height: Int,
        textBitmapWidth: Int,
        textSizeInt: Int,
        intArray: ByteArray,
        asciiColorArray: IntArray,
        asciiIndexArray: IntArray,
        densityLength: Int
    )


    suspend fun imageProxyToTextBitmap(imageProxy: ImageProxy): Bitmap? = withContext(dispatcher) {

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
        /*val rotatedArray = ByteArray(outPutArray.size)
        rotateByteArrayImage(outPutArray, rotatedArray, width, height)*/

        /*val temp = width
        width = height
        height = temp*/

        val textBitmap = rgbArrayToTextBitmap(croppedArray, width)
        runtimeCalculator.finish("imageProxyToTextBitmap")

        textBitmap

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

    private fun rotateByteArrayImage(
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

                Timber.d("image width:height:x:y $width:$height:$x:$y")

                // Copy the pixel values from the source to the destination
                try {
                    rotatedArray[destIndex] =
                        outPutArray[srcIndex]
                    rotatedArray[destIndex + 1] = outPutArray[srcIndex + 1]
                    rotatedArray[destIndex + 2] = outPutArray[srcIndex + 2]
                    rotatedArray[destIndex + 3] = outPutArray[srcIndex + 3]
                } catch (e: ArrayIndexOutOfBoundsException) {
                    Timber.d("Error image width:height:x:y $width:$height:$x:$y")
                }

            }
        }
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

