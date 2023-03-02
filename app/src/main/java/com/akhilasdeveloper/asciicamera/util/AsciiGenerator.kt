package com.akhilasdeveloper.asciicamera.util

import android.graphics.*
import android.text.TextPaint
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_CUSTOM_CHARS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import java.nio.ByteBuffer

sealed class AsciiGenerator {


    companion object {

        var isNativeLibAvailable = false

        init {
            try {
                System.loadLibrary("asciicamera")
                isNativeLibAvailable = true
            } catch (_: java.lang.Exception) {

            }
        }

        const val COLOR_TYPE_NONE = -1
        const val COLOR_TYPE_ANSI = -2
        const val COLOR_TYPE_ORIGINAL = -3

        data class CharData(
            val char: Char,
            val charIntArray: ArrayList<Int> = arrayListOf(),
            val colorFg: Int,
            val colorBg: Int
        )

        data class FilterSpecs(
            var id: Long? = null,
            var density: String = DEFAULT_CUSTOM_CHARS,
            var fgColor: Int = Color.WHITE,
            var fgColorType: Int = COLOR_TYPE_NONE,
            var bgColor: Int = Color.BLACK
        )

        fun getFilterByID(id: Int): AsciiGenerator = when (id) {
            BlackOnWhite.id -> {
                BlackOnWhite
            }
            WhiteOnBlack.id -> {
                WhiteOnBlack
            }
            OriginalColor.id -> {
                OriginalColor
            }
            ANSI.id -> {
                ANSI
            }
            Custom.id -> {
                Custom
            }
            else -> {
                BlackOnWhite
            }
        }

        val listOfFilters: ArrayList<AsciiGenerator> =
            arrayListOf(WhiteOnBlack, BlackOnWhite, OriginalColor, ANSI)

        var textCharSize = 10f

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

    private fun convertRgbaToRgb(rgbaData: ByteArray, width: Int, height: Int): IntArray {
        val rgbData = IntArray(width * height)
        var pixelIndex = 0
        for (i in rgbaData.indices step 4) {
            val r = rgbaData[i].toInt() and 0xff
            val g = rgbaData[i + 1].toInt() and 0xff
            val b = rgbaData[i + 2].toInt() and 0xff
            val a = rgbaData[i + 3].toInt() and 0xff
            rgbData[pixelIndex++] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        return rgbData
    }

    private val _generatedBitmapState = MutableSharedFlow<Bitmap>()
    val generatedBitmapState: SharedFlow<Bitmap> = _generatedBitmapState

    private suspend fun rgbArrayToTextBitmap(intArray: IntArray, width: Int, height: Int) =
        withContext(Dispatchers.Default) {

            val textSize = textCharSize.toInt()
            val textBitmapWidth = width / textSize
            val textBitmapHeight = height / textSize

            val newBitmap = Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
            newBitmap.prepareToDraw()
            val canvas = Canvas(newBitmap)
            val textPaint = TextPaint()
            textPaint.textSize = textCharSize

            /*for (y in 0 until textBitmapHeight)
                for (x in 0 until textBitmapWidth) {

                    val pixel = getAveragePixel(textSize, width, x * textCharSize.toInt(), y * textCharSize.toInt(), intArray)
                    textPaint.color = pixel
                    canvas.drawRect( x * textCharSize, y * textCharSize, (x * textCharSize) + textCharSize, (y * textCharSize) + textCharSize, textPaint)
                }*/

            val charData = arrayListOf<Deferred<CharData>>()

            for (y in 0 until textBitmapHeight)
                for (x in 0 until textBitmapWidth)
                    charData.add(async {
                        val pixel = getAveragePixel(
                            textSize,
                            width,
                            x * textCharSize.toInt(),
                            y * textCharSize.toInt(),
                            intArray
                        )
                        filterPixelToChar(pixel)
                    })

            var isColored = false
            charData.awaitAll().forEachIndexed { index, char ->

                if (!isColored) {
                    canvas.drawColor(char.colorBg)
                    isColored = true
                }

                val x = xFromOneD(index, textBitmapWidth)
                val y = yFromOneD(index, textBitmapWidth)
                val string = char.char.toString()
                textPaint.color = char.colorFg
                canvas.drawText(string, x * textCharSize, y * textCharSize, textPaint)
            }


            newBitmap
        }

    private fun getAveragePixel(
        textSize: Int,
        width: Int,
        x: Int,
        y: Int,
        intArray: IntArray
    ): Int {
        var array = IntArray(textSize * textSize)

        return if (isNativeLibAvailable) {
            getSubPixelsNative(width, x, y, textSize, textSize, intArray, array)
            calculateAvgColorNative(array, array.size)
        } else {
            array = intArray.getSubPixels(width, x, y, textSize, textSize)
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

    private suspend fun filterPixelToChar(pixel: Int): CharData {
        val brightness = ColorUtils.calculateLuminance(pixel)
        val densityLength = density.length
        val charIndex = map(brightness.toFloat(), 0f, 1f, 0, densityLength)
        val index = densityLength - charIndex - 1
        val sliceChar = densityIntArray.getSubPixels(densityLength * textCharSize.toInt(), index * textCharSize.toInt(), 0, textCharSize.toInt(), textCharSize.toInt())
        yield()
        return CharData(
            char = density[index],

            colorFg = fgColors(pixel),
            colorBg = bgColor(pixel)
        )
    }

    private fun map(
        value: Float,
        startValue: Float,
        endValue: Float,
        mapStartValue: Int,
        mapEndValue: Int
    ): Int {
        val n = endValue - startValue
        val mapN = mapEndValue - mapStartValue
        val factor = mapN.toFloat() / n
        return ((startValue + value) * factor).toInt().coerceAtLeast(0).coerceAtMost(mapN - 1)
    }

    abstract val id: Int

    abstract val name: String

    protected abstract val density: String

    protected val densityIntArray: IntArray
        get() {
            if (_densityIntArray.isEmpty())
                _densityIntArray = generateDensityBytes()

            return _densityIntArray
        }

    private fun generateDensityBytes(): IntArray {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            textCharSize.toInt() * density.length,
            textCharSize.toInt(), Bitmap.Config.ARGB_8888
        )
        val paint = Paint()
        val textBounds: Rect = Rect()
        paint.textSize = textCharSize
        paint.color = Color.WHITE
        canvas.setBitmap(bitmap)
        canvas.drawColor(Color.BLACK)
        paint.getTextBounds("@", 0, 1, textBounds);

        density.toCharArray().forEachIndexed { index, c ->
            canvas.drawText(
                c.toString(),
                (index * textCharSize) + ((textCharSize / 2f) - textBounds.exactCenterX()),
                (textCharSize / 2f) - textBounds.exactCenterY(),
                paint
            )
        }

        val array = IntArray(bitmap.width * bitmap.height)
        bitmap.getAllPixels(array)
        return array
    }

    private var _densityIntArray: IntArray = intArrayOf()

    protected abstract fun fgColors(pixel: Int): Int
    protected abstract fun bgColor(pixel: Int): Int


    object WhiteOnBlack : AsciiGenerator() {

        override val id: Int
            get() = -2
        override val name: String
            get() = "White on black"

        override val density: String
            get() = "@BOo:..  "

        override fun fgColors(pixel: Int): Int = Color.WHITE

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object BlackOnWhite : AsciiGenerator() {

        override val id: Int
            get() = -12

        override val name: String
            get() = "Black on white"

        override val density: String
            get() = "  ..:oOB@"

        override fun fgColors(pixel: Int): Int = Color.BLACK

        override fun bgColor(pixel: Int): Int = Color.WHITE
    }

    object OriginalColor : AsciiGenerator() {

        override val id: Int
            get() = -3

        override val name: String
            get() = "Original Color"

        override val density: String
            get() = "Ñ@#"

        override fun fgColors(pixel: Int): Int = pixel

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object ANSI : AsciiGenerator() {


        private const val ansiiFact = .9f

        override val id: Int
            get() = -4
        override val name: String
            get() = "ANSII"

        override val density: String
            get() = "@BOo."

        override fun fgColors(pixel: Int): Int = toANSI(pixel)

        fun toANSI(pixel: Int): Int {

            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)

            val maxRG: Int = if (red > green) red else green
            val maxColor = if (blue > maxRG) blue else maxRG

            val fact = maxColor * ansiiFact

            val r = if (red >= fact) 255 else 0
            val g = if (green >= fact) 255 else 0
            val b = if (blue >= fact) 255 else 0

            return Color.argb(255, r, g, b)
        }

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object Custom : AsciiGenerator() {

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()): AsciiGenerator {
            specs = filterSpecs

            return this
        }

        override val id: Int
            get() = -5
        override val name: String
            get() = "Custom"

        override val density: String
            get() = specs.density

        override fun fgColors(pixel: Int): Int = when (specs.fgColorType) {
            COLOR_TYPE_NONE -> {
                specs.fgColor
            }
            COLOR_TYPE_ANSI -> {
                ANSI.toANSI(pixel)
            }
            COLOR_TYPE_ORIGINAL -> {
                pixel
            }
            else -> {
                specs.fgColor
            }
        }

        override fun bgColor(pixel: Int): Int = specs.bgColor
    }
}

