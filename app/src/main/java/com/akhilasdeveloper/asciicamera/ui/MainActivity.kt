package com.akhilasdeveloper.asciicamera.ui

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.Surface
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
import com.akhilasdeveloper.asciicamera.databinding.LayoutDensityEditorBinding
import com.akhilasdeveloper.asciicamera.ui.recyclerview.CustomFiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.FiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerCustomFiltersClickListener
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerFiltersClickListener
import com.akhilasdeveloper.asciicamera.util.*
import com.akhilasdeveloper.asciicamera.util.Constants.BITMAP_PATH
import com.akhilasdeveloper.asciicamera.util.Constants.DEFAULT_CUSTOM_CHARS
import com.akhilasdeveloper.asciicamera.util.TextBitmapFilter.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.OnColorSelectedListener
import com.flask.colorpicker.builder.ColorPickerClickListener
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), RecyclerFiltersClickListener,
    RecyclerCustomFiltersClickListener {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraProcessProvider: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: Executor

    private lateinit var filtersBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var addFilterBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    @Inject
    lateinit var utilities: Utilities
    lateinit var customFiltersRecyclerAdapter: CustomFiltersRecyclerAdapter
    private var capturedTextBitmap: Bitmap? = null
    private var capturedRawBitmap: Bitmap? = null

    private lateinit var viewModel: MainViewModel
    private var sampleBitmap: Bitmap? = null

    private val asciiGenerator: AsciiGenerator = AsciiGenerator()

    private var requestGallery: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { imageUri ->
            convertImageFromGallery(imageUri)
        }
    }

    private fun convertImageFromGallery(imageUri: Uri) {
        val bitmap = utilities.bitmapFromUri(imageUri)
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        capturedRawBitmap = mutableBitmap
        processBitmap(mutableBitmap)
    }

    private fun reprocessLastBitmap(){
        capturedRawBitmap?.let {
            processBitmap(it)
        }
    }
    private fun processBitmap(mutableBitmap: Bitmap) {
        lifecycleScope.launch {
            pauseCamera()
            val result = asciiGenerator.imageBitmapToTextBitmap(mutableBitmap)
            setBitmapToImage(result)
            asciiGenerator.capture()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        }

        viewModel.bottomSheetAddCustomFilterState.observe(lifecycleScope) {
            asciiGenerator.fgColor = it.fgColor
            asciiGenerator.bgColor = it.bgColor
            asciiGenerator.colorType = it.fgColorType
        }

        viewModel.customFiltersListState.observe(lifecycleScope) {
            Timber.d("Rovers :$it")
            customFiltersRecyclerAdapter.submitList(it)
        }

    }

    private fun editDensityPopup(
        onApply: ((density: String, densityArray: ByteArray) -> Unit)?,
        onDismiss: (() -> Unit)?
    ) {
        val dialogView: LayoutDensityEditorBinding =
            LayoutDensityEditorBinding.inflate(LayoutInflater.from(this))
        val charEditText = dialogView.charactersInput

        dialogView.apply {

            sortChars.setOnClickListener {
                utilities.sortEditTextChars(charEditText)
            }

            reversChars.setOnClickListener {
                utilities.reverseEditTextChars(charEditText)
            }

        }

        utilities.basicAlertDialog(
            view = dialogView.root,
            title = "Edit Density",
            negativeText = "Cancel",
            positiveText = "Apply",
            onApply = {
                lifecycleScope.launch {
                    val chars = utilities.getDensityCharsFromEditText(charEditText)

                    val array = utilities.generateDensityArray(
                        chars,
                        AsciiFilters.WhiteOnBlack.textCharSize
                    )

                    onApply?.invoke(chars, array)
                }
            },
            onDismiss = {
                onDismiss?.invoke()
            }
        )

    }

    private fun setClickListeners() {
        binding.flipCameraButton.setOnClickListener {
            onFlipCameraButtonClicked()
        }

        binding.filterButton.setOnClickListener {
            onFilterButtonClicked()
        }

        binding.galleryButton.setOnClickListener {
            onGalleryButtonClicked()
        }

        binding.captureButton.setOnClickListener {
            asciiGenerator.capture()
        }

        val bottomSheetCallBack = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN)
                    hideKeyboard(binding.root)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        }

        setFiltersBottomSheetListeners(bottomSheetCallBack)
        setAddFilterBottomSheetListeners(bottomSheetCallBack)

        asciiGenerator.setAsciiGeneratedListener(object : AsciiGenerator.OnGeneratedListener {

            override fun onContinue() {
                capturedTextBitmap = null
                revertPanelButtons()
                startCamera()
            }

            override fun onCapture(bitmap: Bitmap?, lastRawBitmap: Bitmap) {
                capturedTextBitmap = bitmap
                capturedRawBitmap = lastRawBitmap
                changePanelButtonsToConfirm()
                pauseCamera()
            }
        })

    }

    private fun setAddFilterBottomSheetListeners(bottomSheetCallBack: BottomSheetBehavior.BottomSheetCallback) {
        binding.layoutAddFilterBottomSheet.apply {

            closeFilterSheet.setOnClickListener {
                addFilterBottomSheetBehavior.hide()
            }

            charactersInput.addTextChangedListener {
                applyCustomFilterEnteredDetails()
            }

            editChars.setOnClickListener {
                editDensityPopup(onApply = { density, densityArray ->
                    asciiGenerator.density = density
                    asciiGenerator._densityIntArray = densityArray
                }, onDismiss = {

                })
            }

            bgColorDisp.setOnClickListener {
                onColorSelectButtonClicked(it)
            }

            fgColorDisp.setOnClickListener {
                onColorSelectButtonClicked(it)
            }

            radioGroup.setOnCheckedChangeListener { _, _ ->
                applyCustomFilterEnteredDetails()
            }

            add.setOnClickListener {
                viewModel.addCustomFilters(viewModel.bottomSheetAddCustomFilterState.value)
                addFilterBottomSheetBehavior.hide()
            }
        }

        addFilterBottomSheetBehavior = BottomSheetBehavior.from(binding.filterBottomSheet)
        addFilterBottomSheetBehavior.isGestureInsetBottomIgnored = true
        addFilterBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)
    }

    private fun setFiltersBottomSheetListeners(bottomSheetCallBack: BottomSheetBehavior.BottomSheetCallback) {
        binding.layoutFilterBottomSheet.apply {

            closeFilterSheet.setOnClickListener {
                filtersBottomSheetBehavior.hide()
            }

            addCustomFilter.setOnClickListener {
                addFilterBottomSheetBehavior.toggle(expanded = {
                    initAddFilterSheet()
                })
            }
        }
        filtersBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        filtersBottomSheetBehavior.isGestureInsetBottomIgnored = true
        filtersBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)
    }

    private fun onColorSelectButtonClicked(it: View) {
        val currColor = (it.background as ColorDrawable).color
        fetchColor(currColor, onColorSelect = { col ->
            it.setBackgroundColor(col)
            applyCustomFilterEnteredDetails()
        }, onCancel = {
            it.setBackgroundColor(currColor)
            applyCustomFilterEnteredDetails()
        })
    }

    private fun onGalleryButtonClicked() {
        if (asciiGenerator.isCapturedState) {
            showShareOption()
        } else {
            launchPhotoPicker()
        }
    }

    private fun launchPhotoPicker() {
        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        requestGallery.launch(galleryIntent)
    }

    private fun onFilterButtonClicked() {
        if (filtersBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
            filtersBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        else {
            filtersBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    private fun onFlipCameraButtonClicked() {
        if (asciiGenerator.isCapturedState) {
            asciiGenerator.continueStream()
        } else
            viewModel.toggleCamera()
    }

    private fun showShareOption() {
        shareAsImage()
    }

    private fun shareAsImage() {
        lifecycleScope.launch(Dispatchers.IO) {
            val imageUri: Uri? = utilities.toImageURI(capturedTextBitmap)
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
        binding.captureButton.visibility = View.INVISIBLE
        binding.galleryButton.setImageDrawable(
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
        binding.captureButton.visibility = View.VISIBLE
        binding.galleryButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.baseline_add_photo_alternate_24
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
//            binding.layoutAddFilterBottomSheet.filterItemImage.generateTextViewFromBitmap(bitmap = bitmap)
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
                startCamera()
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
        cameraProcessProvider = ProcessCameraProvider.getInstance(this)

        asciiGenerator.changeFilter(AsciiFilters.WhiteOnBlack)
        asciiGenerator.setDispatcher(cameraExecutor.asCoroutineDispatcher())

        if (!hasFrontCamera()) {
            binding.flipCameraButton.visibility = View.GONE
        } else {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }

        viewModel.getLens()

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

    private fun startCamera() {

        cameraProcessProvider.addListener({

            try {
                cameraProvider = cameraProcessProvider.get()

                val analysis = buildImageAnalysisUseCase()

                cameraProvider?.unbindAll()

                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, analysis
                )

            } catch (exc: Exception) {
                Timber.e("Use case binding failed $exc")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun generateTextView(imageProxy: ImageProxy) {
        lifecycleScope.launch {
            val bitmap = asciiGenerator.imageProxyToTextBitmap(imageProxy)
            setBitmapToImage(bitmap)
        }
    }

    private fun setBitmapToImage(bitmap: Bitmap?) {
        binding.image.setImageBitmap(bitmap)
    }

    private fun pauseCamera() {
        cameraProvider?.unbindAll()
    }

    private fun buildImageAnalysisUseCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(Surface.ROTATION_90)
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
//        textCanvasView.filter = textBitmapFilter
    }

    override fun onCustomItemClicked(filterSpecs: FilterSpecs) {
//        textCanvasView.filter = TextBitmapFilter.Custom(filterSpecs)
    }

    override fun onCustomDeleteClicked(filterSpecs: FilterSpecs) {
        viewModel.removeCustomFilter(filterSpecs)
    }

}

