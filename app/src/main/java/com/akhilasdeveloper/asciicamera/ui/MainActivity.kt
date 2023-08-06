package com.akhilasdeveloper.asciicamera.ui

import android.Manifest
import android.R.attr.mimeType
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
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
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.akhilasdeveloper.asciicamera.R
import com.akhilasdeveloper.asciicamera.databinding.ActivityMainBinding
import com.akhilasdeveloper.asciicamera.databinding.LayoutDensityEditorBinding
import com.akhilasdeveloper.asciicamera.databinding.LayoutTextInputBinding
import com.akhilasdeveloper.asciicamera.repository.data.FilterDownloadDao
import com.akhilasdeveloper.asciicamera.ui.recyclerview.CustomFiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.DownloadsFiltersRecyclerAdapter
import com.akhilasdeveloper.asciicamera.ui.recyclerview.GridAutofitLayoutManager
import com.akhilasdeveloper.asciicamera.ui.recyclerview.RecyclerCustomFiltersClickListener
import com.akhilasdeveloper.asciicamera.util.*
import com.akhilasdeveloper.asciicamera.util.Constants.BITMAP_PATH
import com.akhilasdeveloper.asciicamera.util.Constants.VIEW_TYPE_CUSTOM
import com.akhilasdeveloper.asciicamera.util.Constants.VIEW_TYPE_DOWNLOADED
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters
import com.akhilasdeveloper.asciicamera.util.asciigenerator.AsciiFilters.Companion.FilterSpecs
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.ListenableFuture
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(),
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
    lateinit var downloadsFiltersRecyclerAdapter: DownloadsFiltersRecyclerAdapter

    private lateinit var viewModel: MainViewModel
    private var sampleBitmap: Bitmap? = null

    private var requestGallery: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { imageUri ->
            viewModel.convertImageFromGallery(imageUri)
        }
    }

    private var requestFile: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        it.data?.data?.let { imageUri ->
            importFilter(imageUri, false)
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

        viewModel.customFiltersListState.observe(lifecycleScope) {
            customFiltersRecyclerAdapter.submitList(it)
        }

        viewModel.downloadsFiltersListState.observe(lifecycleScope) {
            downloadsFiltersRecyclerAdapter.submitList(it)
        }

        viewModel.launchPhotoPickerState.observe(lifecycleScope) {
            if (it)
                launchPhotoPicker()
        }

        viewModel.shareMenuState.observe(lifecycleScope) {
            if (it)
                shareMenuPopup()
        }

        viewModel.shareAsTextState.observe(lifecycleScope) {
            it?.let {
                shareAsText(it)
            }
        }

        viewModel.shareAsHtmlState.observe(lifecycleScope) {
            it?.let {
                shareAsHtml(it)
            }
        }

        viewModel.shareAsImageState.observe(lifecycleScope) {
            it?.let {
                shareAsImage(it)
            }
        }

        viewModel.setBitmapToImageState.observe(lifecycleScope) {
            setBitmapToImage(it)
        }

        viewModel.startCameraState.observe(lifecycleScope) { cameraSelector ->
            startCamera(cameraSelector)
        }
        viewModel.pauseCameraState.observe(lifecycleScope) {
            if (it)
                pauseCamera()
        }
        viewModel.changePanelButtonToConfirmState.observe(lifecycleScope) {
            if (it)
                changePanelButtonsToConfirm()
            else
                revertPanelButtons()
        }
        viewModel.showEditDensityPopupState.observe(lifecycleScope) {
            editDensityPopup(it)
        }

        viewModel.populateCurrentFilterDetailsToAddFilterBottomSheetState.observe(lifecycleScope) {
            populateAddFilterBottomSheet(it)
        }


        viewModel.currentFilterIdState.observe(lifecycleScope) { id ->
            id?.let {
                viewModel.getFilterById(id)
                customFiltersRecyclerAdapter.selectedID = id.toLong()
                downloadsFiltersRecyclerAdapter.selectedID = id.toLong()
            }
        }

        viewModel.progressState.observe(lifecycleScope) {
            binding.progress.isVisible = it
        }

    }

    private fun shareAsHtml(it: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share"))
    }

    private fun shareAsText(it: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share"))
    }

    private fun shareMenuPopup() {
        val items = arrayOf("Share as image", "Share as text file", "Share as html file")
        android.app.AlertDialog.Builder(this)
            .setTitle("Share")
            .setItems(items) { dialog, which ->
                when (which) {
                    0 -> {
                        viewModel.shareAsImage()
                    }
                    1 -> {
                        viewModel.shareAsciiAsText()
                    }
                    2 -> {
                        viewModel.shareAsHtml()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun populateAddFilterBottomSheet(it: FilterSpecs) {
        binding.layoutAddFilterBottomSheet.apply {
            this.charactersInput.setText(it.density)
            this.bgColorDisp.setBackgroundColor(it.bgColor)
            this.fgColorDisp.setBackgroundColor(it.fgColor)
            this.radioGroup.check(
                when (it.fgColorType) {
                    AsciiFilters.COLOR_TYPE_NONE -> {
                        this.fgColorContainer.visibility = View.VISIBLE
                        R.id.radio_button_none
                    }
                    AsciiFilters.COLOR_TYPE_ANSI -> {
                        this.fgColorContainer.visibility = View.INVISIBLE
                        R.id.radio_button_ansi
                    }
                    AsciiFilters.COLOR_TYPE_ORIGINAL -> {
                        this.fgColorContainer.visibility = View.INVISIBLE
                        R.id.radio_button_org
                    }
                    else -> {
                        this.fgColorContainer.visibility = View.VISIBLE
                        R.id.radio_button_none
                    }
                }
            )
        }
    }

    private fun editDensityPopup(density: String) {
        val dialogView: LayoutDensityEditorBinding =
            LayoutDensityEditorBinding.inflate(LayoutInflater.from(this))
        val charEditText = dialogView.charactersInput
        charEditText.setText(density)
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

    private fun inputNamePopup() {
        val dialogView: LayoutTextInputBinding =
            LayoutTextInputBinding.inflate(LayoutInflater.from(this))
        val charEditText = dialogView.charactersInput


        basicAlertDialog(
            view = dialogView.root,
            title = "Enter name to save filter",
            negativeText = "Cancel",
            positiveText = "Apply",
            onApply = {
                val name = charEditText.text?.toString()
                if (name == null || name.isEmpty()) {
                    Toast.makeText(this, "Please enter a name to save filter", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    viewModel.saveCurrentFilter(name)
                }
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

        binding.infoButton.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("Developer Contact: \nakhilasdeveloper@gmail.com")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .create().show()
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

            outlinedTextField.setEndIconOnClickListener {
                viewModel.showEditDensityPopup(charactersInput.text.toString())
            }

            bgColorDisp.setOnClickListener {
                onColorSelectButtonClicked(it) {
                    viewModel.setAsciiGeneratorValues(bgColor = it)
                }
            }

            fgColorDisp.setOnClickListener {
                onColorSelectButtonClicked(it) {
                    viewModel.setAsciiGeneratorValues(fgColor = it)
                }
            }

            radioGroup.setOnCheckedChangeListener { _, _ ->
                onRadioButtonSelected()
            }

            radioButtonNone.setOnClickListener {
                callColorSelectorFg()
            }

            add.setOnClickListener {
                addFilterBottomSheetBehavior.hide()
                inputNamePopup()
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

            importFilter.setOnClickListener {
                openFile(null)
            }
        }
        filtersBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        filtersBottomSheetBehavior.isGestureInsetBottomIgnored = true
        filtersBottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)

        binding.layoutFilterBottomSheet.filterItems.layoutManager = GridAutofitLayoutManager(
            this@MainActivity, 4
        )
        binding.layoutFilterBottomSheet.filterDownloadItems.layoutManager =
            GridAutofitLayoutManager(
                this@MainActivity, 4
            )

    }

    private fun manageFileAssociation() {


        if (intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND) {
            val fileUri: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)
            fileUri?.let { uri ->

                val cr: ContentResolver = this.contentResolver
                val mimeType = cr.getType(uri)

                try {
                    if (mimeType?.contains("image") == true)
                        viewModel.convertImageFromGallery(uri)
                    else
                        importFilter(uri, true)
                } catch (e: java.lang.Exception) {
                    Timber.e(e)
                    Toast.makeText(
                        this@MainActivity,
                        "Error reading file",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            if (fileUri == null) {
                Timber.e("Null uri")
                Toast.makeText(
                    this@MainActivity,
                    "Error reading file",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    }

    private fun importFilter(uri: Uri, finish: Boolean) {
        var inputStream: InputStream? = null

        try {
            inputStream = contentResolver.openInputStream(uri)
            val r = BufferedReader(InputStreamReader(inputStream))
            val total: StringBuilder = StringBuilder()

            var line: String?
            while (r.readLine().also { line = it } != null) {
                total.append(line).append("\n")
            }

            val content = total.toString()
            val data = Gson().fromJson(content, FilterDownloadDao::class.java)

            android.app.AlertDialog.Builder(this)
                .setTitle("Import Filter")
                .setMessage("Do you want to import \"${data.name}\" Filter?")
                .setPositiveButton("OK") { _, _ ->
                    viewModel.importFilter(data)

                    Toast.makeText(
                        this@MainActivity,
                        "\"${data.name}\" Filter imported",
                        Toast.LENGTH_SHORT
                    ).show()

                    if (finish)
                        finish()

                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    if (finish)
                        finish()
                }
                .create().show()

        } catch (e: java.lang.Exception) {
            Timber.e(e)
            Toast.makeText(
                this@MainActivity,
                "Error importing file",
                Toast.LENGTH_SHORT
            ).show()
            if (finish)
                finish()
        } finally {
            inputStream?.close()
        }

    }

    private fun openFile(pickerInitialUri: Uri?) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
            }
        }

        Toast.makeText(
            this@MainActivity,
            "Select .asc file",
            Toast.LENGTH_SHORT
        ).show()

        requestFile.launch(intent)
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
        filtersBottomSheetBehavior.toggle()
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
        viewModel.populateCurrentFilterDetailsToAddFilterBottomSheet()
    }

    private fun onRadioButtonSelected() {
        binding.layoutAddFilterBottomSheet.let {
            val fgColorType = when (it.radioGroup.checkedRadioButtonId) {
                R.id.radio_button_none -> {
                    it.fgColorContainer.visibility = View.VISIBLE
                    TextBitmapFilter.COLOR_TYPE_NONE
                }
                R.id.radio_button_ansi -> {
                    it.fgColorContainer.visibility = View.INVISIBLE
                    TextBitmapFilter.COLOR_TYPE_ANSI
                }
                R.id.radio_button_org -> {
                    it.fgColorContainer.visibility = View.INVISIBLE
                    TextBitmapFilter.COLOR_TYPE_ORIGINAL
                }
                else -> {
                    it.fgColorContainer.visibility = View.VISIBLE
                    TextBitmapFilter.COLOR_TYPE_NONE
                }
            }

            viewModel.setAsciiGeneratorValues(colorType = fgColorType)
        }
    }

    private fun callColorSelectorFg() {
        val view = binding.layoutAddFilterBottomSheet.fgColorDisp
        onColorSelectButtonClicked(view) {
            viewModel.setAsciiGeneratorValues(fgColor = it)
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
            .setPositiveButton(
                "OK"
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

        viewModel.setAsciiGeneratorDispatcher(cameraExecutor.asCoroutineDispatcher())
        viewModel.getFilter()
        viewModel.getLens()

        if (!hasFrontCamera()) {
            binding.flipCameraButton.visibility = View.GONE
        } else {
            viewModel.setCameraSelector(CameraSelector.DEFAULT_FRONT_CAMERA)

        }

        getSampleBitmap()?.let { bitmap ->

            customFiltersRecyclerAdapter =
                CustomFiltersRecyclerAdapter(this, bitmap, lifecycleScope, resources)
            downloadsFiltersRecyclerAdapter =
                DownloadsFiltersRecyclerAdapter(this, bitmap, lifecycleScope, resources)
            binding.layoutFilterBottomSheet.filterItems.adapter = customFiltersRecyclerAdapter
            binding.layoutFilterBottomSheet.filterDownloadItems.adapter =
                downloadsFiltersRecyclerAdapter

            viewModel.getCustomFilters()
            viewModel.getDownloadedFilters()
        }
        manageFileAssociation()

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
            if (it) {
                cameraProcessProvider.addListener({

                    try {

                        val analysis = viewModel.buildImageAnalysisUseCase(cameraExecutor)

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(this, cameraSelector, analysis)

                    } catch (exc: Exception) {
                        Timber.e("Use case binding failed $exc")
                    }

                }, ContextCompat.getMainExecutor(this))
            } else
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

    override fun onCustomItemClicked(filterSpecs: FilterSpecs, viewType: Int) {
        filterSpecs.id?.let {
            viewModel.setFilter(it.toInt())
        }

        if (viewType == VIEW_TYPE_CUSTOM)
            downloadsFiltersRecyclerAdapter.selectedID = -1
        if (viewType == VIEW_TYPE_DOWNLOADED)
            customFiltersRecyclerAdapter.selectedID = -1
    }

    override fun onCustomDeleteClicked(filterSpecs: FilterSpecs) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Filter")
            .setMessage("Do you want to delete this filter?")
            .setPositiveButton("OK") { _, _ ->
                viewModel.removeCustomFilter(filterSpecs)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    override fun onCustomShareClicked(filterSpecs: FilterSpecs) {
        val data = FilterDownloadDao(
            name = filterSpecs.name,
            density = filterSpecs.density,
            fgColor = filterSpecs.fgColor.toHex(),
            bgColor = filterSpecs.bgColor.toHex(),
            fgColorType = filterSpecs.fgColorType
        )

        val name = "${filterSpecs.name}.asc"
        val dataJson = Gson().toJson(data)
        viewModel.shareAsText(dataJson, name)
    }

    private fun Int.toHex(): String = String.format("#%06X", (0xFFFFFF and this))

}

