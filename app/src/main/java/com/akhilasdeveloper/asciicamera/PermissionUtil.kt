package com.akhilasdeveloper.asciicamera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private fun MainActivity.checkPermission(
    permission: String,
    context: Activity,
    onResult: (Boolean) -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        onResult(true)
    } else {
        checkPermissionRational(permission, context) {
            onResult(it)
        }
    }
}

private fun MainActivity.checkPermissionRational(
    permission: String,
    context: Activity,
    onResult: (Boolean) -> Unit
) {

    if (ActivityCompat.shouldShowRequestPermissionRationale(
            context,
            permission
        )
    ) {
        AlertDialog.Builder(context)
            .setTitle("Permission needed")
            .setMessage("This permission is needed for the app to work properly")
            .setPositiveButton("OK") { _, _ ->
                requestPermission {
                    onResult(it)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create().show()
    } else {
        requestPermission {
            onResult(it)
        }
    }
}

private fun MainActivity.requestPermission(onResult: (Boolean) -> Unit) {
    registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onResult(true)
        } else {
            onResult(false)
        }
    }.launch(Manifest.permission.CAMERA)
}