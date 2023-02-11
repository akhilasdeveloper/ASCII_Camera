package com.akhilasdeveloper.asciicamera

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Size
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.get
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var textCanvasView: TextCanvasView

    private val density = "Ñ@#W$9876543210?!abc;:+=-,.      "

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
            Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestStoragePermission();
        }
    }

    private fun init() {
        textCanvasView = TextCanvasView(this)
        binding.gridViewHolder.addView(textCanvasView)
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            AlertDialog.Builder(this)
                .setTitle("Permission needed")
                .setMessage("This permission is needed because of this and that")
                .setPositiveButton("ok",
                    DialogInterface.OnClickListener { dialog, which ->
                        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    })
                .setNegativeButton("cancel",
                    DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
                .create().show()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val executor = Executors.newSingleThreadExecutor()

            // analysis
            val analysis = buildImageAnalysisUseCase().apply {
                setAnalyzer(executor) { imageProxy ->

                    CoroutineScope(Dispatchers.Main).launch {
                        imageProxy.toBitmap()?.let { bitmap: Bitmap ->
                            binding.imageView.setImageBitmap(bitmap)

                            val drawListY = arrayListOf<ArrayList<Char>>()

                            for (y in 0 until bitmap.height) {
                                val drawListX = arrayListOf<Char>()

                                for (x in 0 until bitmap.width) {
                                    val pixel = bitmap[x, y]
                                    val brightness = pixel.brightness()

                                    val densityLength = density.length
                                    val charIndex =
                                        map(brightness.toInt(), 0, 255, 0, densityLength)
                                    drawListX.add(density[densityLength - charIndex - 1])
                                }
                                drawListY.add(drawListX)
                            }
                            textCanvasView.draw(drawListY)
                            imageProxy.close()
                        }
                    }

                }
            }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val cam = cameraProvider.bindToLifecycle(
                    this, cameraSelector, analysis
                )


            } catch (exc: Exception) {
                Timber.e("Use case binding failed $exc")
            }

        }, ContextCompat.getMainExecutor(this))
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
        matrix.postRotate(-90f)

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createScaledBitmap(
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true),
            64,
            64,
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

    private fun buildImageAnalysisUseCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setTargetResolution(Size(48, 48))
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }
}