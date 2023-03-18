package com.akhilasdeveloper.asciicamera.util.asciigenerator

import android.graphics.Color
import com.akhilasdeveloper.asciicamera.util.Constants

sealed class AsciiFilters {

    abstract val id: Int
    abstract val name: String
    abstract val density: String
    abstract val fgColor: Int
    abstract val bgColor: Int
    abstract val fgColorType: Int
    var textCharSize = 10f

    companion object {
        const val COLOR_TYPE_NONE = -1
        const val COLOR_TYPE_ANSI = -2
        const val COLOR_TYPE_ORIGINAL = -3
        const val ANSI_RATIO = 7f/8

        data class CharData(
            val char: Char,
            val charIntArray: List<Int> = listOf(),
            val colorFg: Int,
            val colorBg: Int
        )

        data class FilterSpecs(
            var id: Long? = null,
            var density: String = Constants.DEFAULT_CUSTOM_CHARS,
            var densityArray: ByteArray = byteArrayOf(),
            var fgColor: Int = Color.WHITE,
            var fgColorType: Int = COLOR_TYPE_NONE,
            var bgColor: Int = Color.BLACK
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as FilterSpecs

                if (id != other.id) return false
                if (density != other.density) return false
                if (!densityArray.contentEquals(other.densityArray)) return false
                if (fgColor != other.fgColor) return false
                if (fgColorType != other.fgColorType) return false
                if (bgColor != other.bgColor) return false

                return true
            }

            override fun hashCode(): Int {
                var result = id?.hashCode() ?: 0
                result = 31 * result + density.hashCode()
                result = 31 * result + densityArray.contentHashCode()
                result = 31 * result + fgColor
                result = 31 * result + fgColorType
                result = 31 * result + bgColor
                return result
            }
        }

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
        override val fgColorType: Int
            get() = COLOR_TYPE_NONE
    }

    object BlackOnWhite : AsciiFilters() {

        override val id: Int
            get() = -12

        override val name: String
            get() = "Black on white"

        override val density: String
            get() = ".:oOB@"

        override val fgColor = Color.BLACK

        override val bgColor = Color.WHITE
        override val fgColorType: Int
            get() = COLOR_TYPE_NONE

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
        override val fgColorType: Int
            get() = COLOR_TYPE_ORIGINAL

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

        override val fgColorType: Int
            get() = COLOR_TYPE_ANSI

    }

    object Custom : AsciiFilters() {

        var specs: FilterSpecs = FilterSpecs()
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

        override val fgColorType: Int
            get() = specs.fgColorType

    }

}