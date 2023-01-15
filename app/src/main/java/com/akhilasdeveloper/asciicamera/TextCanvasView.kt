package com.akhilasdeveloper.asciicamera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat

class TextCanvasView(
    context: Context,
    attributeSet: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attributeSet, defStyleAttr, defStyleRes) {

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : this(
        context,
        attributeSet,
        defStyleAttr,
        0
    ) {
        init(attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet) : this(context, attributeSet, 0, 0) {
        init(attributeSet)
    }

    constructor(context: Context) : this(context, null, 0, 0) {
        init(null)
    }

    private fun init(set: AttributeSet?) {

    }

    private val emptyColor =
        ResourcesCompat.getColor(context.resources, R.color.system_border, null)
    private val blockColor = ResourcesCompat.getColor(context.resources, R.color.system_for, null)

    private var paint = Paint().apply {
        color = blockColor
        textSize = 15f
    }

    private var charWidth = 0
    private var charHeight = 0
    private var gap = 0

    private val drawList = arrayListOf<ArrayList<Char>>()

    fun draw(list: ArrayList<ArrayList<Char>>) {
        drawList.clear()
        drawList.addAll(list)
        charWidth = list[0].size
        charHeight = list.size
        gap = 15
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            setBackgroundColor(emptyColor)

            var xVal = 0f
            var yVal = 0f
            for (y in 0 until charHeight) {
                for (x in 0 until charWidth) {
                    drawText(drawList[y][x].toString(), xVal, yVal, paint)
                    xVal += gap
                }
                xVal = 0f
                yVal += gap
            }

        }
    }

}