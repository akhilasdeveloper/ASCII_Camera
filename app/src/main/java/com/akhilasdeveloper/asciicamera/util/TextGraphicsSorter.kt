package com.akhilasdeveloper.asciicamera.util

import android.graphics.*
import androidx.core.graphics.get
import timber.log.Timber
import kotlin.math.sqrt

class TextGraphicsSorter {

    companion object{
        const val TEXT_WIDTH = 15
    }

    private val canvas = Canvas()
    private val bitmap = Bitmap.createBitmap(TEXT_WIDTH, TEXT_WIDTH, Bitmap.Config.ARGB_8888)
    private val paint = Paint()
    private val textBounds: Rect = Rect()

    var bitmapTest: ((Bitmap) -> Unit)? = null

    init {
        paint.textSize = TEXT_WIDTH.toFloat()
        paint.color = Color.WHITE
        canvas.setBitmap(bitmap)
    }

    suspend fun sortTextByBrightness(text: String): String {
        return sortTextByBrightnessDesc(text).reversed()
    }

    suspend fun sortTextByBrightnessDesc(text: String): String{

        val newString = mutableMapOf<Char,Double>()
        for (i in text.toCharArray()){
            newString[i] = getCharBrightness(i)
        }
        Timber.d("Sorting map $newString")
        val sorted = newString.toList().sortedBy { (_, value) -> value}.toMap()
        return String(sorted.keys.toCharArray())

    }

    private fun getCharBrightness(i: Char): Double {
        canvas.drawColor(Color.BLACK)
        paint.getTextBounds(i.toString(), 0, 1, textBounds);
        canvas.drawText(i.toString(), (TEXT_WIDTH/2f) - textBounds.exactCenterX(), (TEXT_WIDTH/2f) - textBounds.exactCenterY(), paint)
        var brightness = 0.0
        bitmapTest?.invoke(bitmap)
        for (pixel in bitmap){
            Timber.d("Pixels: $pixel")
            brightness += pixel.brightness()
        }

        return brightness
    }


    private operator fun Bitmap.iterator(): Iterator<Int>{
        val arrayList = arrayListOf<Int>()
        for(i in 0 until width){
            for (j in 0 until height)
                arrayList.add(get(i,j))
        }

        return arrayList.iterator()
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



}