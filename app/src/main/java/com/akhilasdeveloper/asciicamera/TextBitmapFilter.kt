package com.akhilasdeveloper.asciicamera

import android.graphics.Color

sealed class TextBitmapFilter(
    val density: String,
    val colorBg: Int,
    val colorFg: Int
){
    class BlackOnWhite():TextBitmapFilter("      .,-=+:;cba!?0123456789\$W#@Ñ", Color.WHITE, Color.BLACK)
    class WhiteOnBlack():TextBitmapFilter("Ñ@#W\$9876543210?!abc;:+=-,.      ", Color.BLACK, Color.WHITE)
    class OriginalColor():TextBitmapFilter("Ñ@#W$9876543210?!abc;:+=-,.      ", Color.BLACK, Color.GREEN)
}
