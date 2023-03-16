package com.akhilasdeveloper.asciicamera.ui.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class PixelsView(
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

    var bitmap: Bitmap? = null
    val paint = Paint()

    fun drawBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let { can ->
            bitmap?.let {
                can.drawBitmap(it, 0f, 0f, paint)
            }
        }
    }
}