package com.akhilasdeveloper.asciicamera

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.get
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

    private val emptyColor =
        ResourcesCompat.getColor(context.resources, R.color.can_bg_color, null)
    private val blockColor = ResourcesCompat.getColor(context.resources, R.color.can_fg_color, null)
    private val density = "Ñ@#W$9876543210?!abc;:+=-,.      "

    private var textCharSize = 15f
    private var xStartVal = 0f
    private var yStartVal = 0f
    private var canvasWidth = 0f
    private var canvasHeight = 0f
    private var bitmapWidth = 0
    private var bitmapHeight = 0

    private var paint = Paint().apply {
        color = blockColor
        textSize = textCharSize
    }

    private val drawList = arrayListOf<ArrayList<Char>>()

    private fun draw(list: ArrayList<ArrayList<Char>>) {
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

            val drawListY = arrayListOf<ArrayList<Char>>()

            for (y in 0 until bitmap.height) {
                val drawListX = arrayListOf<Char>()

                for (x in 0 until bitmap.width) {
                    val pixel = bitmap[x, y]
                    val brightness = pixel.brightness()

                    val densityLength = density.length
                    val charIndex = map(brightness.toInt(), 0, 255, 0, densityLength)
                    drawListX.add(density[densityLength - charIndex - 1])
                }
                drawListY.add(drawListX)
            }
            draw(drawListY)
            imageProxy.close()
        }

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
        matrix.preScale(-1f, 1f)
        matrix.postRotate(90f)

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

            setBackgroundColor(emptyColor)

            var xVal = xStartVal
            var yVal = yStartVal
            for (y in 0 until drawList.size) {
                for (x in 0 until drawList[0].size) {
                    drawText(drawList[y][x].toString(), xVal, yVal, paint)
                    xVal += textCharSize
                }
                xVal = xStartVal
                yVal += textCharSize
            }

        }
    }

}


