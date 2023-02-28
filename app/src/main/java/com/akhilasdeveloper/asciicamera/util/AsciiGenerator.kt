package com.akhilasdeveloper.asciicamera.util

import android.graphics.*
import android.text.TextPaint
import androidx.camera.core.ImageProxy
import androidx.core.graphics.ColorUtils
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_CUSTOM_CHARS
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.nio.ByteBuffer

sealed class AsciiGenerator {


    companion object {

        init {
            System.loadLibrary("asciicamera")
        }

        const val COLOR_TYPE_NONE = -1
        const val COLOR_TYPE_ANSI = -2
        const val COLOR_TYPE_ORIGINAL = -3

        data class CharData(
            val char: Char,
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

    external fun convertRgbaToRgb(byteArray: ByteArray,outPutArray: IntArray, width: Int, height: Int)

    /*fun convertRgbaToRgb(rgbaData: ByteArray, width: Int, height: Int): IntArray {
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
    }*/


    private suspend fun Bitmap.scaleToCanvas(): Bitmap = withContext(Dispatchers.Default) {

        val canvasHeight = 64
        val canvasWidth = 64

        /*if (height <= canvasHeight && width <= canvasWidth)
            return this

        var newHeight = canvasHeight
        var newWidth = canvasWidth

        if (height > width) {
            val fact = canvasHeight / height
            newWidth = width * fact

            if (newWidth > canvasWidth) {
                val fact2 = canvasWidth / newWidth
                newWidth = canvasWidth
                newHeight *= fact2
            }

        } else {
            val fact = canvasWidth / width
            newHeight = height * fact

            if (newHeight > canvasHeight) {
                val fact2 = canvasHeight / newHeight
                newHeight = canvasHeight
                newWidth *= fact2
            }
        }*/

        Bitmap.createScaledBitmap(
            this@scaleToCanvas,
            canvasHeight,
            canvasWidth,
            false
        )
    }

    private val _generatedBitmapState = MutableSharedFlow<Bitmap>()
    val generatedBitmapState: SharedFlow<Bitmap> = _generatedBitmapState

    suspend fun bitmapToTextBitmap(intArray: IntArray, width: Int, height: Int) = withContext(Dispatchers.Default) {

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

        canvas.drawColor(Color.RED)

        val charData = arrayListOf<Deferred<CharData>>()

        for (y in 0 until textBitmapHeight)
            for (x in 0 until textBitmapWidth)
                charData.add(async {
                    val pixel = getAveragePixel(textSize, width, x, y, intArray)
                    filterPixelToChar(pixel)
                })


        charData.awaitAll().forEachIndexed { index, char ->

            val x = xFromOneD(index, textBitmapWidth)
            val y = yFromOneD(index, textBitmapWidth)
            val string = char.char.toString()
            textPaint.color = char.colorFg
            canvas.drawText(string, x * textCharSize, y * textCharSize, textPaint)
        }


        newBitmap
    }

    suspend fun bitmapToTextBitmap(bitmap: Bitmap) = withContext(Dispatchers.Default) {

        val textSize = textCharSize.toInt()
        val textBitmapWidth = bitmap.width / textSize
        val textBitmapHeight = bitmap.height / textSize

        val newBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        newBitmap.prepareToDraw()
        val canvas = Canvas(newBitmap)
        val textPaint = TextPaint()
        textPaint.textSize = textCharSize

        canvas.drawColor(Color.RED)

        val charData = arrayListOf<Deferred<CharData>>()

        for (y in 0 until textBitmapHeight)
            for (x in 0 until textBitmapWidth)
                charData.add(async {
                    val pixel = getAveragePixel(textSize, x, y, bitmap)
                    filterPixelToChar(pixel)
                })


        charData.awaitAll().forEachIndexed { index, char ->

            val x = xFromOneD(index, textBitmapWidth)
            val y = yFromOneD(index, textBitmapWidth)
            val string = char.char.toString()
            textPaint.color = char.colorFg
            canvas.drawText(string, x * textCharSize, y * textCharSize, textPaint)
        }


        newBitmap
    }

    private fun getAveragePixel(textSize: Int,width: Int, x: Int, y: Int, intArray: IntArray): Int {
        val start = TwoDtoOneD(width = width, x = x * textSize, y = y * textSize)
        val end = TwoDtoOneD(width = width, x = (x * textSize) + (textSize -1), y = (y * textSize) + (textSize-1))
        val array = intArray.sliceArray(start..end)

        var r = 0
        var g = 0
        var b = 0

        array.forEach {
            r += Color.red(it)
            b += Color.blue(it)
            g += Color.green(it)
        }

        r /= textSize * textSize
        g /= textSize * textSize
        b /= textSize * textSize

        return Color.argb(255, r, g, b)
    }

    private fun getAveragePixel(textSize: Int, x: Int, y: Int, bitmap: Bitmap): Int {
        val intArray = IntArray(textSize * textSize)
        bitmap.getPixels(intArray, 0, textSize, x * textSize, y * textSize, textSize, textSize)
        var r = 0
        var g = 0
        var b = 0

        intArray.forEach {
            r += Color.red(it)
            b += Color.blue(it)
            g += Color.green(it)
        }

        r /= textSize * textSize
        g /= textSize * textSize
        b /= textSize * textSize

        return Color.argb(255, r, g, b)
    }


    val paint = Paint()
    suspend fun imageProxyToTextBitmap(imageProxy: ImageProxy) = withContext(Dispatchers.Default) {

        val startMillis = System.currentTimeMillis()

        val planes = imageProxy.planes
        val buffer = planes[0].buffer
        val pixelStride: Int = planes[0].pixelStride
        val rowStride: Int = planes[0].rowStride
        val rowPadding: Int = rowStride - pixelStride * imageProxy.width

        val width = imageProxy.width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(
            imageProxy.width + rowPadding / pixelStride,
            imageProxy.height, Bitmap.Config.ARGB_8888
        )

        bitmap.prepareToDraw()
        val canvas = Canvas(bitmap)
        val outPutArray = IntArray(width * imageProxy.height)
        convertRgbaToRgb(convertByteBufferToByteArray(buffer),outPutArray, width, imageProxy.height)

        /*outPutArray.forEachIndexed { index, i ->
            val x = xFromOneD(index, imageProxy.width)
            val y = yFromOneD(index, imageProxy.width)
            paint.color = i
//            canvas.drawColor(i)
            canvas.drawPoint(x.toFloat(),y.toFloat(),paint)
//            Timber.d("Testsss $i -- $x:$y -- $index")
        }*/

//        bitmap.copyPixelsFromBuffer(buffer)

        imageProxy.close()

        val textBitmap = bitmapToTextBitmap(outPutArray, width, imageProxy.height)

//        Timber.d("Processed frame in ${System.currentTimeMillis() - startMillis}")

        _generatedBitmapState.emit(textBitmap)

    }

    fun convertByteBufferToByteArray(buffer: ByteBuffer): ByteArray {
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return bytes
    }

    open fun convertToRgb(buffer: ByteBuffer, width: Int, height: Int): IntArray? {
        val rgbData = IntArray(width * height)
        for (i in 0 until (width * height)) {
            val r = buffer.get().toInt() and 0xff
            val g = buffer.get().toInt() and 0xff
            val b = buffer.get().toInt() and 0xff
            buffer.get() // Skip the alpha channel
            rgbData[i] = -0x1000000 or (r shl 16) or (g shl 8) or b
        }
        return rgbData
    }


    private suspend fun filterPixelToChar(pixel: Int): CharData {
        val brightness = ColorUtils.calculateLuminance(pixel)
        val densityLength = density.length
        val charIndex = map(brightness.toFloat(), 0f, 1f, 0, densityLength)
        yield()
        return CharData(
            char = density[densityLength - charIndex - 1],
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
    protected abstract fun fgColors(pixel: Int): Int
    protected abstract fun bgColor(pixel: Int): Int


    object WhiteOnBlack : AsciiGenerator() {

        override val id: Int
            get() = -2
        override val name: String
            get() = "White on black"

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()) {
            specs = filterSpecs
        }

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

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()) {
            specs = filterSpecs
        }

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

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()) {
            specs = filterSpecs
        }

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
        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()) {
            specs = filterSpecs
        }

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

