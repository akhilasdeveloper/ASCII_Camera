package com.akhilasdeveloper.asciicamera.util

import android.graphics.Color

sealed class TextBitmapFilter {

    abstract val density: String
    abstract fun fgColor(pixel: Int): Int
    abstract fun bgColor(pixel: Int): Int

    object BlackOnWhite : TextBitmapFilter() {
        override val density: String
            get() = "Ñ@#W\$9876543210?!abc;:+=-,.      "

        override fun fgColor(pixel: Int): Int = Color.WHITE

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }

    object WhiteOnBlack : TextBitmapFilter() {
        override val density: String
            get() = "      .,-=+:;cba!?0123456789\$W#@Ñ"

        override fun fgColor(pixel: Int): Int = Color.BLACK

        override fun bgColor(pixel: Int): Int = Color.WHITE
    }

    object OriginalColor : TextBitmapFilter() {
        override val density: String
            get() = "Ñ@#W$9876543210?!abc;:+=-,.      "

        override fun fgColor(pixel: Int): Int = pixel

        override fun bgColor(pixel: Int): Int = Color.BLACK
    }
}

