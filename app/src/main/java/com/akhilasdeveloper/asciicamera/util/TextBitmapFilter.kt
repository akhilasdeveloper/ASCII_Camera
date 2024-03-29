package com.akhilasdeveloper.asciicamera.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_CUSTOM_CHARS

sealed class TextBitmapFilter {


    companion object {

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

        fun getFilterByID(id: Int): TextBitmapFilter = when (id) {
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

        val listOfFilters: ArrayList<TextBitmapFilter> =
            arrayListOf(WhiteOnBlack, BlackOnWhite, OriginalColor, ANSI)

    }

    fun bitmapToText(bitmap: Bitmap): ArrayList<ArrayList<CharData>> {

        val drawListY = arrayListOf<ArrayList<CharData>>()

        for (y in 0 until bitmap.height) {
            val drawListX = arrayListOf<CharData>()

            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]

                drawListX.add(
                    filterPixelToChar(pixel)
                )
            }
            drawListY.add(drawListX)
        }

        return drawListY
    }

    private fun filterPixelToChar(pixel: Int): CharData {
        val brightness = ColorUtils.calculateLuminance(pixel)
        val densityLength = density.length
        val charIndex = map(brightness.toFloat(), 0f, 1f, 0, densityLength)
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


    object WhiteOnBlack : TextBitmapFilter() {

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

    object BlackOnWhite : TextBitmapFilter() {

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

    object OriginalColor : TextBitmapFilter() {

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

    object ANSI : TextBitmapFilter() {


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

    object Custom : TextBitmapFilter() {

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()): TextBitmapFilter {
            specs = filterSpecs

            return this
        }

        override val id: Int
            get() = -5
        override val name: String
            get() = "Custom"

        override val density: String
            get() = specs.density


        override fun fgColors(pixel: Int): Int = when(specs.fgColorType){
            COLOR_TYPE_NONE -> {
                specs.fgColor
            }
            COLOR_TYPE_ANSI -> {
                ANSI.toANSI(pixel)
            }
            COLOR_TYPE_ORIGINAL -> {
                pixel
            }
            else ->{
                specs.fgColor
            }
        }

        override fun bgColor(pixel: Int): Int = specs.bgColor
    }
}

