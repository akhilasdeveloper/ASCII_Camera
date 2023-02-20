package com.akhilasdeveloper.asciicamera.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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

}