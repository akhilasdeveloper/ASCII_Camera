package com.akhilasdeveloper.asciicamera.ui.views

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.camera.core.ImageProxy
import com.akhilasdeveloper.asciicamera.R
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.CharData
import com.akhilasdeveloper.asciicamera.util.TextGraphicsSorter
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
        set?.let { attrSet ->

            val ta = context.obtainStyledAttributes(attrSet, R.styleable.TextCanvasView)

            textCharSize =
                ta.getDimension(R.styleable.TextCanvasView_textSize, textCharSize).spToPx()

            ta.recycle()

        }
    }

    private var paint = Paint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
    }

    var textCharSize = 15f
        set(value) {
            field = value
            canvasWidth = width / textCharSize
            canvasHeight = height / textCharSize
            paint.textSize = textCharSize
            scaleBitmap()
        }
    private var xStartVal = 0f
    private var yStartVal = 0f
    private var canvasWidth = 1f
        set(value) {
            field = if (value < 1) 1f else value
        }
    private var canvasHeight = 1f
        set(value) {
            field = if (value < 1) 1f else value
        }
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    var inverse = false
    var rotateDegree: Float? = null
    private val textBounds: Rect = Rect()

    private var bitmap: Bitmap? = null
        set(value) {
            field = value
            scaleBitmap()
        }

    private var scaledBitmap: Bitmap? = null
        set(value) {
            field = value
            draw()
        }

    private val drawList = arrayListOf<ArrayList<CharData>>()
    private var bgColor = Color.BLACK
    var filter: TextBitmapFilter? = null
        set(value) {
            field = value
            draw()
        }

    private fun draw() {
        scaledBitmap?.let {
            drawList.clear()
            drawList.addAll((filter?:TextBitmapFilter.WhiteOnBlack).bitmapToText(it))
            calculateStartVal()
            bgColor = drawList.first().first().colorBg
            postInvalidate()
        }
    }

    private fun scaleBitmap() {
        scaledBitmap = bitmap?.scaleToCanvas()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        canvasWidth = width / textCharSize
        canvasHeight = height / textCharSize

        scaleBitmap()
    }

    private fun calculateStartVal() {

        xStartVal = (width - (bitmapWidth * textCharSize)) / 2f
        yStartVal = (height - (bitmapHeight * textCharSize)) / 2f

        Timber.d("xStartVal:yStartVal ${(width - (bitmapWidth * textCharSize)) / 2f}:${(height - (bitmapHeight * textCharSize)) / 2f}")
    }

    fun generateTextViewFromImageProxy(imageProxy: ImageProxy) {
        bitmap = imageProxy.toBitmap()
        imageProxy.close()
    }

    fun generateTextViewFromBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
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

    private fun Bitmap.scaleToCanvas(): Bitmap {

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
            if (drawList.isNotEmpty())
                paint.getTextBounds(drawList.first().first().toString(), 0, 1, textBounds);
            for (y in 0 until drawList.size) {
                for (x in 0 until drawList[0].size) {
                    drawList[y][x].let {
                        val string = it.char.toString()
                        paint.color = it.colorFg
                        canvas.drawText(
                            string,
                            (textCharSize / 2f) - textBounds.exactCenterX() + xVal,
                            (textCharSize / 2f) - textBounds.exactCenterY() + yVal,
                            paint
                        )
                    }
                    xVal += textCharSize
                }
                xVal = xStartVal
                yVal += textCharSize
            }

        }
    }

    private fun Float.spToPx(): Float = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics
    )

}



