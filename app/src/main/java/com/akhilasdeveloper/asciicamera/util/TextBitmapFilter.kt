package com.akhilasdeveloper.asciicamera.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import kotlin.math.sqrt

sealed class TextBitmapFilter {


    companion object {
        data class CharData(
            val char: Char,
            val colorFg: Int,
            val colorBg: Int
        )

        data class FilterSpecs(
            val density: String = "",
            val fgColors: ArrayList<Int> = arrayListOf(),
            val colorBg: Int = Color.BLACK
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
        val brightness = pixel.brightness()
        val fgColors = fgColors(pixel)
        val fgColorsLength = fgColors.size
        val densityLength = density.length
        val charIndex = map(brightness.toInt(), 0, 255, 0, densityLength)
        val fgColorIndex = map(brightness.toInt(), 0, 255, 0, fgColorsLength)
        return CharData(
            char = density[densityLength - charIndex - 1],
            colorFg = fgColors[fgColorsLength - fgColorIndex - 1],
            colorBg = bgColor(pixel)
        )
    }

    private fun map(
        value: Int,
        startValue: Int,
        endValue: Int,
        mapStartValue: Int,
        mapEndValue: Int
    ): Int {
        val n = endValue - startValue
        val mapN = mapEndValue - mapStartValue
        val factor = mapN.toFloat() / n.toFloat()
        return ((startValue + value) * factor).toInt().coerceAtLeast(0).coerceAtMost(mapN - 1)
    }

    private fun Int.brightness(): Double {

        val r = Color.red(this)
        val g = Color.red(this)
        val b = Color.red(this)

        return sqrt(
            r * r * .241 + g
                    * g * .691 + b * b * .068
        )
    }

    abstract val id:Int

    abstract val name:String

    protected abstract val density: String
    protected abstract fun fgColors(pixel: Int): ArrayList<Int>
    protected abstract fun bgColor(pixel: Int): Int


    object WhiteOnBlack : TextBitmapFilter() {

        override val id: Int
            get() = -2
        override val name: String
            get() = "White on black"

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()){
            specs = filterSpecs
        }

        override val density: String
            get() = "@BOo:."

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(Color.WHITE)

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object BlackOnWhite : TextBitmapFilter() {

        override val id: Int
            get() = -12

        override val name: String
            get() = "Black on white"

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()){
            specs = filterSpecs
        }

        override val density: String
            get() = ".:oOB@"

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(Color.BLACK)

        override fun bgColor(pixel: Int): Int = Color.WHITE
    }

    object OriginalColor : TextBitmapFilter() {

        override val id: Int
            get() = -3

        override val name: String
            get() = "Original Color"

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()){
            specs = filterSpecs
        }

        override val density: String
            get() = "Ñ@#"

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(pixel)

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object ANSI : TextBitmapFilter() {

        override val id: Int
            get() = -4
        override val name: String
            get() = "ANSII"
        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()){
            specs = filterSpecs
        }

        override val density: String
            get() = "Ñ@#"

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(
            Color.WHITE,
            Color.CYAN,
            Color.MAGENTA,
            Color.BLUE,
            Color.YELLOW,
            Color.GREEN,
            Color.RED,
            Color.BLACK
        )

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object Custom : TextBitmapFilter() {

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()):TextBitmapFilter{
            specs = filterSpecs

            return this
        }

        override val id: Int
            get() = -5
        override val name: String
            get() = "Custom"

        override val density: String
            get() = specs.density


        override fun fgColors(pixel: Int): ArrayList<Int> =
            if (specs.fgColors.isEmpty()) arrayListOf(pixel) else specs.fgColors

        override fun bgColor(pixel: Int): Int = specs.colorBg
    }
}

