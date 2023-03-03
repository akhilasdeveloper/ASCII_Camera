package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.graphics.Color
import com.akhilasdeveloper.asciicamera.util.Constants

sealed class AsciiFilters {

    abstract val id: Int
    abstract val name: String
    abstract val density: String
    abstract val fgColor: Int
    abstract val bgColor: Int
    var textCharSize = 5f

    companion object {
        const val COLOR_TYPE_NONE = -1
        const val COLOR_TYPE_ANSI = -2
        const val COLOR_TYPE_ORIGINAL = -3

        data class CharData(
            val char: Char,
            val charIntArray: List<Int> = listOf(),
            val colorFg: Int,
            val colorBg: Int
        )

        data class FilterSpecs(
            var id: Long? = null,
            var density: String = Constants.DEFAULT_CUSTOM_CHARS,
            var fgColor: Int = Color.WHITE,
            var fgColorType: Int = COLOR_TYPE_NONE,
            var bgColor: Int = Color.BLACK
        )

        fun getFilterByID(id: Int): AsciiFilters = when (id) {
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

        val listOfFilters: ArrayList<AsciiFilters> =
            arrayListOf(WhiteOnBlack, BlackOnWhite, OriginalColor, ANSI)

    }

    object WhiteOnBlack : AsciiFilters() {

        override val id: Int
            get() = -2
        override val name: String
            get() = "White on black"

        override val density: String
            get() = "@BOo:."

        override val fgColor = Color.WHITE

        override val bgColor = Color.BLACK
    }

    object BlackOnWhite : AsciiFilters() {

        override val id: Int
            get() = -12

        override val name: String
            get() = "Black on white"

        override val density: String
            get() = ":oOB@"

        override val fgColor = Color.BLACK

        override val bgColor = Color.WHITE
    }

    object OriginalColor : AsciiFilters() {

        override val id: Int
            get() = -3

        override val name: String
            get() = "Original Color"

        override val density: String
            get() = "Ñ@#"

        override val fgColor = Color.WHITE

        override val bgColor = Color.BLACK
    }

    object ANSI : AsciiFilters() {

        override val id: Int
            get() = -4
        override val name: String
            get() = "ANSII"

        override val density: String
            get() = "@BOo."

        override val fgColor = Color.WHITE

        override val bgColor = Color.BLACK
    }

    object Custom : AsciiFilters() {

        private var specs: FilterSpecs = FilterSpecs()
        operator fun invoke(filterSpecs: FilterSpecs = FilterSpecs()): AsciiFilters {
            specs = filterSpecs
            return this
        }

        override val id: Int
            get() = -5
        override val name: String
            get() = "Custom"

        override val density: String
            get() = specs.density

        override val fgColor = specs.fgColor

        override val bgColor = specs.bgColor
    }

}