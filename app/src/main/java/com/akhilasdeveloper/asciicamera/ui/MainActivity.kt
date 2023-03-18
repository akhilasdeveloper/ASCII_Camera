package com.akhilasdeveloper.asciicamera.ui

import android.Manifest
import android.content.Context
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
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.akhilasdeveloper.asciicamera.R
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import com.akhilasdeveloper.asciicamera.databinding.LayoutDensityEditorBinding
import com.akhilasdeveloper.asciicamera.ui.recyclerview.CustomFiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerCustomFiltersClickListener
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerFiltersClickListener
import com.akhilasdeveloper.asciicamera.util.*
import com.akhilasdeveloper.asciicamera.util.Constants.BITMAP_PATH
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.FilterSpecs
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiGenerator
import com.flask.colorpicker.ColorPickerView
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

    @Inject
    lateinit var cameraProvider: ProcessCameraProvider

    @Inject
    lateinit var cameraProcessProvider: ListenableFuture<ProcessCameraProvider>

    @Inject
    lateinit var imageAnalysis: ImageAnalysis

    private lateinit var cameraExecutor: Executor

    private lateinit var filtersBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private lateinit var addFilterBottomSheetBehavior: BottomSheetBehavior<NestedScrollView>

    lateinit var customFiltersRecyclerAdapter: CustomFiltersRecyclerAdapter

    private lateinit var viewModel: MainViewModel
    private var sampleBitmap: Bitmap? = null

    private var requestGallery: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { imageUri ->
            viewModel.convertImageFromGallery(imageUri)
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

        viewModel.inverseCanvasState.observe(lifecycleScope) {

        }

        viewModel.customFiltersListState.observe(lifecycleScope) {
            customFiltersRecyclerAdapter.submitList(it)
        }

        viewModel.launchPhotoPickerState.observe(lifecycleScope) {
            if (it)
                launchPhotoPicker()
        }

        viewModel.shareAsImageState.observe(lifecycleScope) {
            it?.let {
                shareAsImage(it)
            }
        }

        viewModel.setBitmapToImageState.observe(lifecycleScope) {
            setBitmapToImage(it)
        }

        viewModel.startCameraState.observe(lifecycleScope) {cameraSelector->
            startCamera(cameraSelector)
        }
        viewModel.pauseCameraState.observe(lifecycleScope) {
            if (it)
                pauseCamera()
        }
        viewModel.changePanelButtonToConfirmState.observe(lifecycleScope){
            if (it)
                changePanelButtonsToConfirm()
            else
                revertPanelButtons()
        }
        viewModel.showEditDensityPopupState.observe(lifecycleScope) {
            if (it)
                editDensityPopup()
        }

        viewModel.filtersCount.observe(lifecycleScope){
            Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
            if (it<4)
                viewModel.generateFilters()
        }

    }

    private fun editDensityPopup() {
        val dialogView: LayoutDensityEditorBinding =
            LayoutDensityEditorBinding.inflate(LayoutInflater.from(this))
        val charEditText = dialogView.charactersInput

        dialogView.apply {

            sortChars.setOnClickListener {
                viewModel.sortEditTextChars(charEditText)
            }

            reversChars.setOnClickListener {
                viewModel.reverseEditTextChars(charEditText)
            }

        }

        basicAlertDialog(
            view = dialogView.root,
            title = "Edit Density",
            negativeText = "Cancel",
            positiveText = "Apply",
            onApply = {
                viewModel.onApplyDensity(charEditText)
            },
            onDismiss = {
            }
        )

    }

    private fun basicAlertDialog(
        view: View,
        title: String,
        negativeText: String,
        positiveText: String,
        onApply: (() -> Unit)?,
        onDismiss: (() -> Unit)?
    ) {
        val builder: AlertDialog.Builder =
            AlertDialog.Builder(this)
                .setView(view)
                .setTitle(title)
                .setNegativeButton(negativeText) { dialog, _ ->
                    dialog.dismiss()
                    onDismiss?.invoke()
                }
                .setPositiveButton(positiveText) { dialog, _ ->

                    onApply?.invoke()

                    dialog.dismiss()
                }

        val alertDialog: AlertDialog = builder.create()
        alertDialog.show()
    }

    private fun setClickListeners() {
        binding.flipCameraButton.setOnClickListener {
            onFlipCameraButtonClicked()
        }

        binding.filterButton.setOnClickListener {
            onFilterButtonClicked()
        }

        binding.galleryButton.setOnClickListener {
            viewModel.onGalleryButtonClicked()
        }

        binding.captureButton.setOnClickListener {
            viewModel.asciiGeneratorCapture()
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

    }

    private fun setAddFilterBottomSheetListeners(bottomSheetCallBack: BottomSheetBehavior.BottomSheetCallback) {
        binding.layoutAddFilterBottomSheet.apply {

            closeFilterSheet.setOnClickListener {
                addFilterBottomSheetBehavior.hide()
            }

            editChars.setOnClickListener {
                viewModel.showEditDensityPopup()
            }

            bgColorDisp.setOnClickListener {
                onColorSelectButtonClicked(it){
                    viewModel.setAsciiGeneratorValues(bgColor = it)
                }
            }

            fgColorDisp.setOnClickListener {
                onColorSelectButtonClicked(it){
                    viewModel.setAsciiGeneratorValues(bgColor = it)
                }
            }

            radioGroup.setOnCheckedChangeListener { _, _ ->
                onRadioButtonSelected()
            }

            add.setOnClickListener {
                addFilterBottomSheetBehavior.hide()
                viewModel.saveCurrentFilter()
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

        binding.layoutFilterBottomSheet.filterItems.layoutManager = LinearLayoutManager(
            this@MainActivity,
            LinearLayoutManager.HORIZONTAL, false
        )

    }

    private fun onColorSelectButtonClicked(it: View, onResult: (color: Int) -> Unit) {
        val currColor = (it.background as ColorDrawable).color
        fetchColor(currColor, onColorSelect = { col ->
            it.setBackgroundColor(col)
            onResult(col)
        }, onCancel = {
            it.setBackgroundColor(currColor)
            onResult(currColor)
        })
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
        viewModel.flipCamera()
    }

    private fun shareAsImage(imageUri: Uri?) {
        lifecycleScope.launch(Dispatchers.IO) {
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
            onRadioButtonSelected()
        }
    }

    private fun onRadioButtonSelected() {
        binding.layoutAddFilterBottomSheet.let {
            val fgColorType = when (it.radioGroup.checkedRadioButtonId) {
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

            viewModel.setAsciiGeneratorValues(colorType = fgColorType)
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
            .setOnColorSelectedListener {

            }
            .setPositiveButton("OK"
            ) { _, selectedColor, _ ->
                onColorSelect(selectedColor)
            }
            .setNegativeButton("cancel") { dialog, _ ->
                onCancel()
                dialog.cancel()
            }
            .build()
            .show()
    }

    private fun init() {

        initPermission()

        viewModel = ViewModelProvider(this@MainActivity)[MainViewModel::class.java]

        cameraExecutor = Executors.newSingleThreadExecutor()

        viewModel.asciiGeneratorChangeFilter(AsciiFilters.WhiteOnBlack)
        viewModel.setAsciiGeneratorDispatcher(cameraExecutor.asCoroutineDispatcher())

        if (!hasFrontCamera()) {
            binding.flipCameraButton.visibility = View.GONE
        } else {
            viewModel.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)

        }

        viewModel.getLens()

        getSampleBitmap()?.let { bitmap ->

            customFiltersRecyclerAdapter = CustomFiltersRecyclerAdapter(this, bitmap, AsciiGenerator(), lifecycleScope)
            binding.layoutFilterBottomSheet.filterItems.adapter = customFiltersRecyclerAdapter

            viewModel.getCustomFilters()
        }

        viewModel.getFiltersCount()
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
            Timber.e("Unable to load image asset")
        }
        return bitmap
    }

    private fun startCamera(cameraSelector: CameraSelector) {

        checkPermission(Manifest.permission.CAMERA) {
            if (it){
                cameraProcessProvider.addListener({

                    try {

                        val analysis = viewModel.buildImageAnalysisUseCase(cameraExecutor)

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(this, cameraSelector, analysis)

                    } catch (exc: Exception) {
                        Timber.e("Use case binding failed $exc")
                    }

                }, ContextCompat.getMainExecutor(this))
            }
            else
                Toast.makeText(this, "Please allow camera permission", Toast.LENGTH_SHORT).show()
        }

    }

    private fun setBitmapToImage(bitmap: Bitmap?) {
        binding.image.setImageBitmap(bitmap)
    }

    private fun pauseCamera() {
        cameraProvider.unbindAll()
    }

    private fun hasFrontCamera(): Boolean = this.packageManager.hasSystemFeature(
        PackageManager.FEATURE_CAMERA_FRONT
    )

    override fun onItemClicked(textBitmapFilter: TextBitmapFilter) {

    }

    override fun onCustomItemClicked(filterSpecs: FilterSpecs) {
        viewModel.setAsciiGeneratorValues(
            fgColor = filterSpecs.fgColor,
            bgColor = filterSpecs.bgColor,
            density = filterSpecs.density,
            densityByteArray = filterSpecs.densityArray,
            colorType = filterSpecs.fgColorType
        )
    }

    override fun onCustomDeleteClicked(filterSpecs: FilterSpecs) {
        viewModel.removeCustomFilter(filterSpecs)
    }

}

