package com.akhilasdeveloper.asciicamera

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

private var onResults: ((Boolean) -> Unit)? = null
private var requestPermission: ActivityResultLauncher<String>? = null

internal fun MainActivity.checkPermission(
    permission: String,
    onResult: (Boolean) -> Unit
) {
    onResults = onResult

    if (ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        Timber.e("Camera permission true")
        onResults?.invoke(true)
    } else {
        Timber.e("Camera permission false")
        checkPermissionRational(permission)
    }
}

private fun MainActivity.checkPermissionRational(
    permission: String
) {

    if (ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            permission
        )
    ) {
        AlertDialog.Builder(this)
            .setTitle("Permission needed")
            .setMessage("This permission is needed for the app to work properly")
            .setPositiveButton("OK") { _, _ ->
                requestPermission?.launch(permission)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create().show()
    } else {
        requestPermission?.launch(permission)
    }
}

internal fun MainActivity.initPermission() {
    requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResults?.invoke(isGranted)
    }
}