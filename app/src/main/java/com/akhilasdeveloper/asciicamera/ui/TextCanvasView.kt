package com.akhilasdeveloper.asciicamera.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.CharData
import timber.log.Timber
import java.nio.ByteBuffer

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

    private var textCharSize = 15f
    private var xStartVal = 0f
    private var yStartVal = 0f
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    var inverse = false
    var rotateDegree: Float? = null

    private var paint = Paint().apply {
        textSize = textCharSize
    }

    private val drawList = arrayListOf<ArrayList<CharData>>()
    private var bgColor = Color.BLACK
    var filter: TextBitmapFilter = TextBitmapFilter.BlackOnWhite

    private fun draw(list: ArrayList<ArrayList<CharData>>) {
        drawList.clear()
        drawList.addAll(list)
        calculateStartVal()
        bgColor = list.first().first().colorBg
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        canvasWidth = width / textCharSize
        canvasHeight = height / textCharSize

        Timber.d("canvasHeight:canvasWidth $canvasHeight:$canvasWidth")

    }

    private fun calculateStartVal() {

        xStartVal = (width - (bitmapWidth * textCharSize)) / 2f
        yStartVal = (height - (bitmapHeight * textCharSize)) / 2f

        Timber.d("xStartVal:yStartVal ${(width - (bitmapWidth * textCharSize)) / 2f}:${(height - (bitmapHeight * textCharSize)) / 2f}")
    }

    fun generateTextView(imageProxy: ImageProxy, toBitmap: (Bitmap) -> Unit) {
        imageProxy.toBitmap()?.scaleDownToCanvas()?.let { bitmap: Bitmap ->

            toBitmap(bitmap)
            draw(filter.bitmapToText(bitmap))
            imageProxy.close()
        }

    }


    private fun ImageProxy.toBitmap(): Bitmap? {
        val planes = planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride: Int = planes[0].pixelStride
        val rowStride: Int = planes[0].rowStride
        val rowPadding: Int = rowStride - pixelStride * width

        val matrix = Matrix()
        if (inverse)
            matrix.preScale(-1f, 1f)
        rotateDegree?.let {
            matrix.postRotate(it)
        }

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height, Bitmap.Config.ARGB_8888
        )

        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun Bitmap.scaleDownToCanvas(): Bitmap {

        if (height <= canvasHeight && width <= canvasWidth)
            return this

        var newHeight = canvasHeight
        var newWidth = canvasWidth

        if (height > width) {
            val fact = canvasHeight / height
            newWidth = width * fact

            if (newWidth > canvasWidth) {
                val fact2 = canvasWidth / newWidth
                newWidth = canvasWidth
                newHeight *= fact2
            }

        } else {
            val fact = canvasWidth / width
            newHeight = height * fact

            if (newHeight > canvasHeight) {
                val fact2 = canvasHeight / newHeight
                newHeight = canvasHeight
                newWidth *= fact2
            }
        }

        bitmapWidth = newWidth.toInt()
        bitmapHeight = newHeight.toInt()

        return Bitmap.createScaledBitmap(
            this,
            bitmapWidth,
            bitmapHeight,
            false
        )
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.apply {

            setBackgroundColor(bgColor)

            var xVal = xStartVal
            var yVal = yStartVal
            for (y in 0 until drawList.size) {
                for (x in 0 until drawList[0].size) {
                    drawList[y][x].let {
                        paint.color = it.colorFg
                        drawText(it.char.toString(), xVal, yVal, paint)
                    }
                    xVal += textCharSize
                }
                xVal = xStartVal
                yVal += textCharSize
            }

        }
    }

}



