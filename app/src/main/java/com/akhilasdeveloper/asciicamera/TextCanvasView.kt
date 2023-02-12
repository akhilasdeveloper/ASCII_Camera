package com.akhilasdeveloper.asciicamera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.core.graphics.*
import timber.log.Timber
import java.nio.ByteBuffer
import kotlin.math.sqrt

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

//    private val density = "@8Oo:."

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
    var filter:TextBitmapFilter = TextBitmapFilter.BlackOnWhite()

    private fun draw(list: ArrayList<ArrayList<CharData>>) {
        drawList.clear()
        drawList.addAll(list)
        calculateStartVal()
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
            draw(bitmapToText(bitmap))
            imageProxy.close()
        }

    }

    private fun bitmapToText(bitmap: Bitmap): ArrayList<ArrayList<CharData>> {

        val drawListY = arrayListOf<ArrayList<CharData>>()

        for (y in 0 until bitmap.height) {
            val drawListX = arrayListOf<CharData>()

            for (x in 0 until bitmap.width) {
                val pixel = bitmap[x, y]

                drawListX.add(
                    filterPixelToChar(pixel)
                )
            }
            drawListY.add(drawListX)
        }

        return drawListY
    }

    private fun filterPixelToChar(pixel: Int): CharData {
        val brightness = pixel.brightness()
        val densityLength = filter.density.length
        val charIndex = map(brightness.toInt(), 0, 255, 0, densityLength)
        bgColor = filter.colorBg
        return CharData(
            char = filter.density[densityLength - charIndex - 1],
            colorFg = filter.colorFg,
            colorBg = filter.colorBg
        )
    }

    private fun map(
        value: Int,
        startValue: Int,
        endValue: Int,
        mapStartValue: Int,
        mapEndValue: Int
    ): Int {
        val n = endValue - startValue
        val mapN = mapEndValue - mapStartValue
        val factor = mapN.toFloat() / n.toFloat()
        return ((startValue + value) * factor).toInt().coerceAtLeast(0).coerceAtMost(mapN - 1)
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

    private fun Int.brightness(): Double {

        val r = Color.red(this)
        val g = Color.red(this)
        val b = Color.red(this)

        return sqrt(
            r * r * .241 + g
                    * g * .691 + b * b * .068
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

    data class CharData(
        val char: Char,
        val colorFg: Int,
        val colorBg: Int
    )

}


