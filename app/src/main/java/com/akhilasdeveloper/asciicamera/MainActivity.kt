package com.akhilasdeveloper.asciicamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.get
import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var textCanvasView: TextCanvasView

    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var cameraExecutor: Executor

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
            CameraSelector.DEFAULT_BACK_CAMERA
        else
            CameraSelector.DEFAULT_FRONT_CAMERA
    }

    private fun loadCamera() {
        checkPermission(Manifest.permission.CAMERA) {

            if (it)
                openCamera()
            else
                Toast.makeText(this, "Please allow camera permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun init() {
        initPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
        textCanvasView = TextCanvasView(this)
        binding.gridViewHolder.addView(textCanvasView)

        if (!hasFrontCamera())
            binding.flipCameraButton.visibility = View.GONE
        else
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    }


    private fun openCamera() {

        val cameraProcessProvider = ProcessCameraProvider.getInstance(this)

        cameraProcessProvider.addListener({

            try {
                val cameraProvider: ProcessCameraProvider = cameraProcessProvider.get()

                val analysis = buildImageAnalysisUseCase()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, analysis
                )

            } catch (exc: Exception) {
                Timber.e("Use case binding failed $exc")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun generateTextView(imageProxy: ImageProxy) {
        lifecycleScope.launch {
            textCanvasView.generateTextView(imageProxy){
                binding.imageView.setImageBitmap(it)
            }
        }
    }

    private fun buildImageAnalysisUseCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().apply {
                setAnalyzer(cameraExecutor) { imageProxy ->
                    generateTextView(imageProxy)
                }
            }
    }

    private fun hasFrontCamera(): Boolean = this.packageManager.hasSystemFeature(
        PackageManager.FEATURE_CAMERA_FRONT
    )

}