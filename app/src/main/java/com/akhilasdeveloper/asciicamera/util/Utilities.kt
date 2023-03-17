package com.akhilasdeveloper.asciicamera.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class Utilities(private val context: Context) {

    suspend fun toImageURI(bitmap: Bitmap?): Uri? {
        bitmap?.let {
            var file: File? = null
            var fos1: FileOutputStream? = null
            var imageUri: Uri? = null
            try {
                val folder = File(
                    context.cacheDir.toString() + File.separator + "ASCII Temp Files"
                )
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val filename = "ascii.png"
                file = File(folder.path, filename)
                withContext(Dispatchers.IO) {
                    fos1 = FileOutputStream(file)
                }

                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos1)
                imageUri = FileProvider.getUriForFile(
                    context.applicationContext,
                    context.applicationContext.packageName.toString() + ".provider",
                    file
                )
            } catch (ex: java.lang.Exception) {
                Timber.d("Exception occurred while converting bitmap to uri : $ex")
            } finally {
                try {
                    withContext(Dispatchers.IO) {
                        fos1?.close()
                    }
                } catch (e: IOException) {
                    Timber.d("Unable to close connection Utilities toImageURI : ${e.toString()}")
                }
            }
            return imageUri
        }
        return null
    }

    suspend fun generateDensityArray(density: String, textSize: Float): ByteArray {
        val canvas = Canvas()
        val textSizeInt = textSize.toInt()
        val bitmap = Bitmap.createBitmap(
            textSizeInt,
            textSizeInt * density.length, Bitmap.Config.ARGB_8888
        )

        val paint = TextPaint()
        val textBounds: Rect = Rect()
        paint.textSize = textSize
        canvas.drawARGB(255, 0, 0, 0)
        paint.color = -0x1
        canvas.setBitmap(bitmap)

        density.toCharArray().forEachIndexed { index, c ->
            paint.getTextBounds(c.toString(), 0, 1, textBounds)
            canvas.drawText(c.toString(),
                (textSize / 2f) - textBounds.exactCenterX(),
                (index * textSize) + ((textSize / 2f) - textBounds.exactCenterY()),
                paint
            )
        }

        val byteArray = ByteArray(bitmap.width * bitmap.height)
        bitmap.getAllPixelsBytes(byteArray)

        return byteArray
    }

}