package com.akhilasdeveloper.asciicamera.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.get
import kotlin.math.sqrt

sealed class TextBitmapFilter {


    companion object{
        data class CharData(
            val char: Char,
            val colorFg: Int,
            val colorBg: Int
        )

        data class FilterSpecs(
            val density: String,
            val fgColors: ArrayList<Int>,
            val colorBg: Int
        )

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


    protected abstract val density: String
    protected abstract fun fgColors(pixel: Int): ArrayList<Int>
    protected abstract fun bgColor(pixel: Int): Int

    object BlackOnWhite : TextBitmapFilter() {
        override val density: String
            get() = "Ñ@#W\$9876543210?!abc;:+=-,.      "

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(Color.WHITE)

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object WhiteOnBlack : TextBitmapFilter() {
        override val density: String
            get() = "      .,-=+:;cba!?0123456789\$W#@Ñ"

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(Color.BLACK)

        override fun bgColor(pixel: Int): Int = Color.WHITE
    }

    object OriginalColor : TextBitmapFilter() {
        override val density: String
            get() = "Ñ@#"

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(pixel)

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object ANSI : TextBitmapFilter() {
        override val density: String
            get() = "Ñ@#"

        override fun fgColors(pixel: Int): ArrayList<Int> = arrayListOf(Color.WHITE, Color.CYAN, Color.MAGENTA, Color.BLUE, Color.YELLOW, Color.GREEN, Color.RED, Color.BLACK)

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    class Custom(private val filterSpecs: FilterSpecs) : TextBitmapFilter() {
        override val density: String
            get() = filterSpecs.density

        override fun fgColors(pixel: Int): ArrayList<Int> = if (filterSpecs.fgColors.isEmpty()) arrayListOf(pixel) else filterSpecs.fgColors

        override fun bgColor(pixel: Int): Int = filterSpecs.colorBg
    }
}

