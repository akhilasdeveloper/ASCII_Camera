package com.akhilasdeveloper.asciicamera.ui

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.asciicamera.R
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import com.akhilasdeveloper.asciicamera.ui.recyclerview.CustomFiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.FiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerCustomFiltersClickListener
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerFiltersClickListener
import com.akhilasdeveloper.asciicamera.ui.views.TextCanvasView
import com.akhilasdeveloper.asciicamera.util.*
import com.akhilasdeveloper.asciicamera.util.Constants.BITMAP_PATH
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_CUSTOM_CHARS
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.OnColorSelectedListener
import com.flask.colorpicker.builder.ColorPickerClickListener
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), RecyclerFiltersClickListener,
    RecyclerCustomFiltersClickListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var textCanvasView: TextCanvasView
    private lateinit var cameraExecutor: Executor

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var createFilterBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    @Inject
    lateinit var textGraphicsSorter: TextGraphicsSorter

    @Inject
    lateinit var utilities: Utilities
    lateinit var customFiltersRecyclerAdapter: CustomFiltersRecyclerAdapter
    private var capturedBitmap: Bitmap? = null
    private var capturedChars: ArrayList<ArrayList<TextBitmapFilter.Companion.CharData>> =
        arrayListOf()

    private lateinit var viewModel: MainViewModel
    private var sampleBitmap: Bitmap? = null

    private val asciiGenerator: AsciiGenerator = AsciiGenerator()

    private var requestGallery: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { imageUri ->

            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(
                    this.contentResolver,
                    imageUri
                )
            } else {
                val source = ImageDecoder.createSource(this.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            }

            textCanvasView.generateTextViewFromBitmap(
                bitmap.copy(Bitmap.Config.ARGB_8888, true)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*asciiGenerator.generatedBitmapState.observe(lifecycleScope) {
            binding.image.setImageBitmap(it)
        }*/

        init()
        setClickListeners()
        subscribeToObservers()
    }

    private fun subscribeToObservers() {

        viewModel.lensState.observe(lifecycleScope) {
            cameraSelector = it
            loadCamera()
        }

        viewModel.inverseCanvasState.observe(lifecycleScope) {
            textCanvasView.inverse = it
        }

        viewModel.bottomSheetAddCustomFilterState.observe(lifecycleScope) {
            binding.layoutAddFilterBottomSheet.filterItemImage.filter = TextBitmapFilter.Custom(it)

        }

        viewModel.customFiltersListState.observe(lifecycleScope) {
            Timber.d("Rovers :$it")
            customFiltersRecyclerAdapter.submitList(it)
        }

    }

    private fun setClickListeners() {
        binding.flipCameraButton.setOnClickListener {
            if (textCanvasView.isCapturedState) {
                textCanvasView.continueStream()
            } else
                viewModel.toggleCamera()
        }
        binding.filterButton.setOnClickListener {
            if (textCanvasView.isCapturedState) {
                showShareOption()
            } else {
                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                else {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

        }

        binding.layoutFilterBottomSheet.closeFilterSheet.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.layoutFilterBottomSheet.addCustomFilter.setOnClickListener {
            if (createFilterBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                createFilterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            else {
                createFilterBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                initAddFilterSheet()
            }
        }

        binding.layoutAddFilterBottomSheet.closeFilterSheet.setOnClickListener {
            if (createFilterBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
                createFilterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        binding.captureButton.setOnClickListener {
            textCanvasView.capture()
        }

        binding.layoutAddFilterBottomSheet.charactersInput.addTextChangedListener {
            applyCustomFilterEnteredDetails()
        }

        binding.layoutAddFilterBottomSheet.sortChars.setOnClickListener {
            binding.layoutAddFilterBottomSheet.charactersInput.setText(
                textGraphicsSorter.sortTextByBrightness(
                    getDensityFromInput()
                )
            )
        }

        binding.layoutAddFilterBottomSheet.reversChars.setOnClickListener {
            binding.layoutAddFilterBottomSheet.charactersInput.setText(getDensityFromInput().reversed())
        }

        binding.layoutAddFilterBottomSheet.bgColorDisp.setOnClickListener {
            val currColor =
                (binding.layoutAddFilterBottomSheet.bgColorDisp.background as ColorDrawable).color
            fetchColor(currColor, onColorSelect = {
                binding.layoutAddFilterBottomSheet.bgColorDisp.setBackgroundColor(it)
                applyCustomFilterEnteredDetails()
            }, onCancel = {
                binding.layoutAddFilterBottomSheet.bgColorDisp.setBackgroundColor(currColor)
                applyCustomFilterEnteredDetails()
            })
        }

        binding.layoutAddFilterBottomSheet.fgColorDisp.setOnClickListener {
            val currColor =
                (binding.layoutAddFilterBottomSheet.fgColorDisp.background as ColorDrawable).color
            fetchColor(currColor, onColorSelect = {
                binding.layoutAddFilterBottomSheet.fgColorDisp.setBackgroundColor(it)
                applyCustomFilterEnteredDetails()
            }, onCancel = {
                binding.layoutAddFilterBottomSheet.fgColorDisp.setBackgroundColor(currColor)
                applyCustomFilterEnteredDetails()
            })
        }

        binding.layoutAddFilterBottomSheet.radioGroup.setOnCheckedChangeListener { _, _ ->
            applyCustomFilterEnteredDetails()
        }

        binding.layoutAddFilterBottomSheet.add.setOnClickListener {
            viewModel.addCustomFilters(viewModel.bottomSheetAddCustomFilterState.value)
            createFilterBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }

        val bottomSheetCallBack = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    hideKeyboard(binding.root)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

        }

        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)
        createFilterBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)

        textCanvasView.setOnTextCaptureListener(object : TextCanvasView.OnTextCaptureListener {
            override fun continueStream() {
                capturedChars.clear()
                capturedBitmap = null
                revertPanelButtons()
            }

            override fun onCapture(
                drawList: ArrayList<ArrayList<TextBitmapFilter.Companion.CharData>>,
                bitmap: Bitmap?
            ) {
                capturedChars.clear()
                capturedChars.addAll(drawList)
                capturedBitmap = bitmap
                changePanelButtonsToConfirm()

            }
        })

        binding.galleryButton.setOnClickListener {
            val galleryIntent = Intent(Intent.ACTION_PICK)
            galleryIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            requestGallery.launch(galleryIntent)
        }

    }

    private fun showShareOption() {
        shareAsImage()
    }

    private fun shareAsImage() {
        lifecycleScope.launch(Dispatchers.IO) {
            val imageUri: Uri? = utilities.toImageURI(capturedBitmap)
            withContext(Dispatchers.Main) {
                val shareIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    type = "image/jpeg"
                }
                startActivity(Intent.createChooser(shareIntent, "Share image via"))
            }
        }
    }

    private fun changePanelButtonsToConfirm() {
        binding.flipCameraButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.round_close_24
            )
        )
        binding.filterButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.baseline_share_24
            )
        )
    }

    private fun revertPanelButtons() {
        binding.flipCameraButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.baseline_flip_camera_android_24
            )
        )
        binding.filterButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.baseline_filter_24
            )
        )
    }

    private fun hideKeyboard(v: View) {
        val inputMethodManager: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(v.applicationWindowToken, 0)
    }

    private fun initAddFilterSheet() {
        getSampleBitmap()?.let { bitmap ->
            binding.layoutAddFilterBottomSheet.filterItemImage.generateTextViewFromBitmap(bitmap = bitmap)
            applyCustomFilterEnteredDetails()
        }
    }

    private fun applyCustomFilterEnteredDetails() {
        viewModel.bottomSheetAddCustomFilterState(FilterSpecs().apply {
            val density = getDensityFromInput()

            binding.layoutAddFilterBottomSheet.let {
                this.density = density
                this.fgColor = (it.fgColorDisp.background as ColorDrawable).color
                this.bgColor = (it.bgColorDisp.background as ColorDrawable).color

                this.fgColorType = when (it.radioGroup.checkedRadioButtonId) {
                    R.id.radio_button_none -> {
                        it.fgColorDisp.visibility = View.VISIBLE
                        TextBitmapFilter.COLOR_TYPE_NONE
                    }
                    R.id.radio_button_ansi -> {
                        it.fgColorDisp.visibility = View.INVISIBLE
                        TextBitmapFilter.COLOR_TYPE_ANSI
                    }
                    R.id.radio_button_org -> {
                        it.fgColorDisp.visibility = View.INVISIBLE
                        TextBitmapFilter.COLOR_TYPE_ORIGINAL
                    }
                    else -> {
                        it.fgColorDisp.visibility = View.VISIBLE
                        TextBitmapFilter.COLOR_TYPE_NONE
                    }
                }
            }
        })
    }

    private fun getDensityFromInput(): String =
        binding.layoutAddFilterBottomSheet.charactersInput.text?.let {
            if (it.isNotEmpty())
                it.toString()
            else
                null
        } ?: DEFAULT_CUSTOM_CHARS


    private fun loadCamera() {
        checkPermission(Manifest.permission.CAMERA) {

            if (it)
                openCamera()
            else
                Toast.makeText(this, "Please allow camera permission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchColor(
        currentColor: Int,
        onColorSelect: (color: Int) -> Unit,
        onCancel: () -> Unit
    ) {
        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose color")
            .initialColor(currentColor)
            .wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
            .density(12)
            .setOnColorSelectedListener(OnColorSelectedListener { _ ->

            })
            .setPositiveButton("OK",
                ColorPickerClickListener { _, selectedColor, _ ->
                    onColorSelect(selectedColor)
                })
            .setNegativeButton("cancel", DialogInterface.OnClickListener { dialog, which ->
                onCancel()
                dialog.cancel()
            })
            .build()
            .show()
    }

    private fun init() {
        initPermission()
        viewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        asciiGenerator.setDispatcher(cameraExecutor.asCoroutineDispatcher())
        textCanvasView = binding.gridViewHolder
        textCanvasView.rotateDegree = 90f

        if (!hasFrontCamera()) {
            binding.flipCameraButton.visibility = View.GONE
        } else {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            textCanvasView.inverse = true
        }
        viewModel.getLens()

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.isGestureInsetBottomIgnored = true

        createFilterBottomSheetBehavior = BottomSheetBehavior.from(binding.filterBottomSheet)
        createFilterBottomSheetBehavior.isGestureInsetBottomIgnored = true

        binding.layoutFilterBottomSheet.filterItems.layoutManager = LinearLayoutManager(
            this@MainActivity,
            LinearLayoutManager.HORIZONTAL, false
        )

        binding.layoutFilterBottomSheet.customFilterItems.layoutManager = LinearLayoutManager(
            this@MainActivity,
            LinearLayoutManager.HORIZONTAL, false
        )


        getSampleBitmap()?.let { bitmap ->

            customFiltersRecyclerAdapter = CustomFiltersRecyclerAdapter(this, bitmap)
            binding.layoutFilterBottomSheet.customFilterItems.adapter = customFiltersRecyclerAdapter
            binding.layoutFilterBottomSheet.filterItems.adapter =
                FiltersRecyclerAdapter(this, bitmap).also {
                    it.submitList(TextBitmapFilter.listOfFilters)
                }
            viewModel.getCustomFilters()
        }

    }

    private fun getSampleBitmap(): Bitmap? {
        sampleBitmap = sampleBitmap ?: getBitmapFromAsset(this, BITMAP_PATH)
        return sampleBitmap
    }

    private fun getBitmapFromAsset(context: Context, filePath: String?): Bitmap? {
        val assetManager: AssetManager = context.assets
        val str: InputStream
        var bitmap: Bitmap? = null
        try {
            str = assetManager.open(filePath!!)
            bitmap = BitmapFactory.decodeStream(str)
        } catch (e: IOException) {
            // handle exception
        }
        return bitmap
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

    var isWidthAndHeightSet = false

    private fun generateTextView(imageProxy: ImageProxy) {
        lifecycleScope.launch {
            if (!isWidthAndHeightSet) {
                asciiGenerator.setWidthAndHeight(imageProxy.width, imageProxy.height)
                isWidthAndHeightSet = true
            }
            val bitmap = asciiGenerator.imageProxyToTextBitmap(imageProxy)
            binding.image.setImageBitmap(
                bitmap
            )
        }
        /*lifecycleScope.launch {
            asciiGenerator.filterPixelToCharIntArrayTest(Color.DKGRAY).let {
                binding.image.setImageBitmap(it)
            }
        }*/
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

    override fun onItemClicked(textBitmapFilter: TextBitmapFilter) {
        textCanvasView.filter = textBitmapFilter
    }

    override fun onCustomItemClicked(filterSpecs: FilterSpecs) {
        textCanvasView.filter = TextBitmapFilter.Custom(filterSpecs)
    }

    override fun onCustomDeleteClicked(filterSpecs: FilterSpecs) {
        viewModel.removeCustomFilter(filterSpecs)
    }

    private fun ImageProxy.toByteArray(): ByteArray? {
        val planes = planes
        val buffer: ByteBuffer = planes[0].buffer
        val byteArray = ByteArray(width * (height + height / 2))
        buffer.get(byteArray, 0, width * height)

        return byteArray
    }

}

