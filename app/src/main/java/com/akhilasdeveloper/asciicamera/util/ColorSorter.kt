package com.akhilasdeveloper.asciicamera.util

import android.graphics.*
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.get
import kotlin.math.sqrt

class ColorSorter {

    fun sortColorsByBrightness(colors: List<Int>): List<Int> {
        return sortColorsByBrightnessDesc(colors).reversed()
    }

    fun sortColorsByBrightnessDesc(colors: List<Int>): List<Int> {

        val newColors = mutableMapOf<Int,Double>()

        for (i in colors){
            newColors[i] = ColorUtils.calculateLuminance(i)
        }

        val sorted = newColors.toList().sortedBy { (_, value) -> value}.toMap()
        return sorted.keys.toList()

    }


    private operator fun Bitmap.iterator(): Iterator<Int>{
        val arrayList = arrayListOf<Int>()
        for(i in 0 until width){
            for (j in 0 until height)
                arrayList.add(get(i,j))
        }

        return arrayList.iterator()
    }




}