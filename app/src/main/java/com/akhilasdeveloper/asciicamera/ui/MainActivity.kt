package com.akhilasdeveloper.asciicamera.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.TextGraphicsSorter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var textCanvasView: TextCanvasView
    private lateinit var cameraExecutor: Executor

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    @Inject
    lateinit var textGraphicsSorter: TextGraphicsSorter

    lateinit var viewModel:MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        setClickListeners()
        subscribeToObservers()
    }

    private fun subscribeToObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.lensState.collectLatest {
                cameraSelector = it
                loadCamera()
            }

            viewModel.inverseCanvasState.collectLatest {
                textCanvasView.inverse = it
            }
        }
    }

    private fun setClickListeners() {
        binding.flipCameraButton.setOnClickListener {
            viewModel.toggleCamera()
        }
        binding.filterButton.setOnClickListener {
            when (textCanvasView.filter) {
                is TextBitmapFilter.WhiteOnBlack -> {
                    textCanvasView.filter = TextBitmapFilter.BlackOnWhite
                }
                is TextBitmapFilter.BlackOnWhite -> {
                    textCanvasView.filter = TextBitmapFilter.OriginalColor
                }
                is TextBitmapFilter.OriginalColor -> {
                    textCanvasView.filter = TextBitmapFilter.ANSI
                }
                is TextBitmapFilter.ANSI -> {
                    CoroutineScope(Dispatchers.Default).launch {
                        textCanvasView.filter = TextBitmapFilter.Custom(
                            filterSpecs = FilterSpecs(
                                density = textGraphicsSorter.sortTextByBrightness("!@#$%^&*()_+=-|}{[]\\\":;',.><?/~`"),
                                fgColors = arrayListOf(Color.WHITE),
                                colorBg = Color.BLACK
                            )
                        )
                    }
                }
                is TextBitmapFilter.Custom -> {
                    textCanvasView.filter = TextBitmapFilter.WhiteOnBlack
                }
            }
        }

        binding.captureButton.setOnClickListener {

        }

        /*textGraphicsSorter.bitmapTest = {
            binding.imageView.setImageBitmap(it)
        }*/
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
        viewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        textCanvasView = binding.gridViewHolder
        textCanvasView.rotateDegree = 90f

        if (!hasFrontCamera()) {
            binding.flipCameraButton.visibility = View.GONE
        } else {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            textCanvasView.inverse = true
        }
        viewModel.getLens()
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
            textCanvasView.generateTextView(imageProxy) {
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