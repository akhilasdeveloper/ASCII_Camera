package com.akhilasdeveloper.asciicamera

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        loadCamera()
        setClickListeners()
    }

    private fun setClickListeners() {
        binding.flipCameraButton.setOnClickListener {
            toggleCamera()
            loadCamera()
        }
    }

    private fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA
        else
            CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun loadCamera() {
        checkPermission(Manifest.permission.CAMERA, this) {
            if (it)
                openCamera()
            else
                Toast.makeText(this, "Please allow camera permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun init() {
        textCanvasView = TextCanvasView(this)
        binding.gridViewHolder.addView(textCanvasView)

        if (!hasFrontCamera())
            binding.flipCameraButton.visibility = View.GONE
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
                    generateTextView(imageProxy)
                }
            }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, analysis
                )

            } catch (exc: Exception) {
                Timber.e("Use case binding failed $exc")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun generateTextView(imageProxy: ImageProxy) {
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

    private fun hasFrontCamera(): Boolean = this.packageManager.hasSystemFeature(
        PackageManager.FEATURE_CAMERA_FRONT
    )

}