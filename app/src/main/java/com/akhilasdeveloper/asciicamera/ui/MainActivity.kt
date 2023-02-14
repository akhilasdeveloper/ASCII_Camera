package com.akhilasdeveloper.asciicamera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors


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
        binding.filterButton.setOnClickListener {
            when(textCanvasView.filter){
                is TextBitmapFilter.WhiteOnBlack -> {
                    textCanvasView.filter = TextBitmapFilter.BlackOnWhite
                }
                is TextBitmapFilter.BlackOnWhite -> {
                    textCanvasView.filter = TextBitmapFilter.OriginalColor
                }
                is TextBitmapFilter.OriginalColor -> {
                    textCanvasView.filter = TextBitmapFilter.WhiteOnBlack
                }
            }
        }
    }

    private fun toggleCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            textCanvasView.inverse = false
            CameraSelector.DEFAULT_BACK_CAMERA
        }else {
            textCanvasView.inverse = true
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
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
        textCanvasView = binding.gridViewHolder
        textCanvasView.rotateDegree = 90f

        if (!hasFrontCamera()) {
            binding.flipCameraButton.visibility = View.GONE
        }else {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            textCanvasView.inverse = true
        }
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