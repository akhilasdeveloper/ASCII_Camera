package com.akhilasdeveloper.asciicamera

import android.graphics.Color

sealed class TextBitmapFilter() {
    abstract fun getFilter(pixel:Int): FilterData

    class BlackOnWhite() : TextBitmapFilter() {
        override fun getFilter(pixel:Int): FilterData {
            return FilterData("      .,-=+:;cba!?0123456789\$W#@Ñ", Color.WHITE, Color.BLACK)
        }
    }

    class WhiteOnBlack() : TextBitmapFilter() {
        override fun getFilter(pixel:Int): FilterData {
            return FilterData("Ñ@#W\$9876543210?!abc;:+=-,.      ", Color.BLACK, Color.WHITE)
        }
    }

    class OriginalColor() : TextBitmapFilter() {
        override fun getFilter(pixel:Int): FilterData {
            return FilterData("Ñ@#W$9876543210?!abc;:+=-,.      ", Color.BLACK, pixel)
        }
    }

    data class FilterData(
        val density: String,
        val colorFg: Int,
        val colorBg: Int
    )
}
